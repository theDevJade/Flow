import 'package:flutter/material.dart';
import '../graph_editor/graph_editor.dart';
import '../graph_editor/models.dart';
import '../services/graph_project_service.dart';
import '../services/graph_persistence_service.dart';
import '../state/page_manager.dart';

class GraphEditorScreen extends StatefulWidget {
  final String? pageId;

  const GraphEditorScreen({super.key, this.pageId});

  @override
  State<GraphEditorScreen> createState() => _GraphEditorScreenState();
}

class _GraphEditorScreenState extends State<GraphEditorScreen> {
  GraphProject? currentProject;
  final GraphProjectService _projectService = GraphProjectService.instance;

  List<GraphNode> _nodes = [];
  List<GraphConnection> _connections = [];
  String _currentPageId = 'default'; // fallback page ID
  bool _isLoading = false;
  bool _isSaving = false;
  String? _loadingMessage;

  @override
  void initState() {
    super.initState();
    _currentPageId =
        widget.pageId ?? PageManager.instance.activePageId ?? 'default';

    debugPrint('GraphEditorScreen: Initializing with pageId: $_currentPageId');


    if (widget.pageId != null) {
      PageManager.instance.setActivePage(widget.pageId!, 'graphEditor');
    }


    Future.microtask(() {
      if (mounted) {
        _loadPageState();
      }
    });
  }

  @override
  void didUpdateWidget(GraphEditorScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.pageId != widget.pageId) {
      debugPrint(
        'GraphEditorScreen: Page changed from ${oldWidget.pageId} to ${widget.pageId}',
      );


      setState(() {
        _isLoading = true;
        _loadingMessage = 'Switching to new page...';
      });


      _saveCurrentPageState();


      _currentPageId =
          widget.pageId ?? PageManager.instance.activePageId ?? 'default';


      if (widget.pageId != null) {
        PageManager.instance.setActivePage(widget.pageId!, 'graphEditor');
      }


      _loadPageState();
    }
  }

  @override
  void dispose() {

    if (mounted) {
      try {
        _saveCurrentPageState();
      } catch (e) {
        debugPrint('Error saving page state during disposal: $e');
      }
    }
    super.dispose();
  }

  void _loadPageState() {
    if (!mounted) return;

    setState(() {
      _isLoading = true;
      _loadingMessage = 'Loading page data...';
    });


    Future.microtask(() async {
      if (!mounted) return;

      try {
        final pageManager = PageManager.instance;
        final workspaceId =
            pageManager.currentWorkspaceId ?? 'default_workspace';

        debugPrint(
          'GraphEditorScreen: Loading page state for $_currentPageId in workspace $workspaceId',
        );


        final graphIdForLoading = workspaceId.startsWith('workspace_')
            ? '$workspaceId-$_currentPageId'
            : _currentPageId;
        debugPrint(
          'GraphEditorScreen: Using graphId: $graphIdForLoading for loading data',
        );


        Map<String, dynamic> pageState = {};
        try {
          pageState = Map<String, dynamic>.from(
            pageManager.getPageStateWithoutNotify(
                  _currentPageId,
                  'graphEditor',
                ) ??
                {},
          );
        } catch (e) {
          debugPrint('Error getting page state: $e');
          pageState = {}; // Use empty state as fallback
        }

        debugPrint(
          'GraphEditorScreen: Nodes: ${pageState['nodes']?.length ?? 0}, Connections: ${pageState['connections']?.length ?? 0}',
        );

        if (mounted) {
          setState(() {
            _nodes = _parseNodesFromState(
              pageState['nodes'] as List<dynamic>? ?? [],
            );
            _connections = _parseConnectionsFromState(
              pageState['connections'] as List<dynamic>? ?? [],
            );
          });
        }

        // Ensure the graph persistence service knows about the correct graph ID
        // and has the graph data initialized
        final graphId = workspaceId.startsWith('workspace_')
            ? '$workspaceId-$_currentPageId'
            : _currentPageId;

        final graphPersistenceService = GraphPersistenceService.instance;

        // First ensure the service knows what graph we're working with
        graphPersistenceService.setCurrentGraph(graphId, workspaceId);

        // Try to get cached graph data or load from server
        final cachedData = graphPersistenceService.getCachedGraph(graphId);
        if (cachedData == null) {
          // No cached data, explicitly create the graph
          debugPrint(
            'GraphEditorScreen: No cached data for graph $graphId, creating new graph',
          );
          graphPersistenceService.createGraph(
            graphId,
            workspaceId: workspaceId,
          );
        }

        debugPrint(
          'GraphEditorScreen: Setting graph persistence service to use graphId: $graphId',
        );
      } catch (e) {
        debugPrint('GraphEditorScreen: Error loading page state: $e');
        // Reset to empty state
        if (mounted) {
          setState(() {
            _nodes = [];
            _connections = [];
          });
        }

        // Try to create a default graph in case the error was due to missing graph
        try {
          final workspaceId =
              PageManager.instance.currentWorkspaceId ?? 'default_workspace';
          final graphId = workspaceId.startsWith('workspace_')
              ? '$workspaceId-$_currentPageId'
              : _currentPageId;

          debugPrint(
            'GraphEditorScreen: Creating default graph due to error: $graphId',
          );
          GraphPersistenceService.instance.createGraph(
            graphId,
            workspaceId: workspaceId,
          );
        } catch (graphError) {
          debugPrint(
            'GraphEditorScreen: Error creating default graph: $graphError',
          );
        }
      } finally {
        // Only update state if widget is still mounted
        if (mounted) {
          setState(() {
            _isLoading = false;
            _loadingMessage = null;
          });
        }
      }
    });
  }

  void _saveCurrentPageState() {
    // Safety check to prevent calling during disposal or when tree might be locked
    if (!mounted) {
      debugPrint(
        'GraphEditorScreen: Not saving page state - widget not mounted',
      );
      return;
    }

    try {
      final pageManager = PageManager.instance;
      final pageState = {
        'nodes': _nodes.map((node) => _nodeToMap(node)).toList(),
        'connections': _connections
            .map((conn) => _connectionToMap(conn))
            .toList(),
        'panOffset': {
          'dx': 0.0,
          'dy': 0.0,
        }, // Will be updated by GraphEditor callbacks
        'scale': 1.0, // Will be updated by GraphEditor callbacks
        'selectedNodeId': null, // Will be updated by GraphEditor callbacks
      };

      // Use a microtask to ensure we don't update state during build/layout
      Future.microtask(() {
        if (mounted) {
          try {
            pageManager.updatePageState(_currentPageId, pageState);

            // Also save to graph persistence service
            final workspaceId =
                pageManager.currentWorkspaceId ?? 'default_workspace';
            final graphId = workspaceId.startsWith('workspace_')
                ? '$workspaceId-$_currentPageId'
                : _currentPageId;

            // Prepare graph data in the format expected by persistence service
            final graphData = {
              'nodes': pageState['nodes'],
              'connections': pageState['connections'],
              'metadata': {
                'lastSaved': DateTime.now().toIso8601String(),
                'pageId': _currentPageId,
              },
            };

            GraphPersistenceService.instance.updateGraphDataCache(
              graphId,
              graphData,
            );
          } catch (e) {
            debugPrint('Error in microtask during page state update: $e');
          }
        }
      });
    } catch (e) {
      debugPrint('GraphEditorScreen: Error saving page state: $e');
    }
  }

  List<GraphNode> _parseNodesFromState(List<dynamic> nodeData) {
    if (nodeData.isEmpty) {
      return []; // Each page starts with an empty graph
    }

    return nodeData
        .map((data) => _nodeFromMap(data as Map<String, dynamic>))
        .toList();
  }

  List<GraphConnection> _parseConnectionsFromState(
    List<dynamic> connectionData,
  ) {
    if (connectionData.isEmpty) {
      return []; // Each page starts with no connections
    }

    return connectionData
        .map((data) => _connectionFromMap(data as Map<String, dynamic>))
        .toList();
  }

  GraphNode _nodeFromMap(Map<String, dynamic> data) {
    return GraphNode(
      id: data['id'] ?? '',
      name: data['name'] ?? 'Unnamed Node',
      inputs: (data['inputs'] as List<dynamic>? ?? [])
          .map(
            (portData) => GraphPort(
              id: portData['id'] ?? '',
              name: portData['name'] ?? '',
              isInput: portData['isInput'] ?? true,
              color: Color(portData['color'] ?? Colors.grey.value),
            ),
          )
          .toList(),
      outputs: (data['outputs'] as List<dynamic>? ?? [])
          .map(
            (portData) => GraphPort(
              id: portData['id'] ?? '',
              name: portData['name'] ?? '',
              isInput: portData['isInput'] ?? false,
              color: Color(portData['color'] ?? Colors.grey.value),
            ),
          )
          .toList(),
      color: Color(data['color'] ?? Colors.grey.value),
      position: Offset(
        (data['position']?['dx'] ?? 0.0).toDouble(),
        (data['position']?['dy'] ?? 0.0).toDouble(),
      ),
      size: Size(
        (data['size']?['width'] ?? 150.0).toDouble(),
        (data['size']?['height'] ?? 80.0).toDouble(),
      ),
    );
  }

  GraphConnection _connectionFromMap(Map<String, dynamic> data) {
    return GraphConnection(
      id: data['id'] ?? '',
      fromNodeId: data['fromNodeId'] ?? '',
      fromPortId: data['fromPortId'] ?? '',
      toNodeId: data['toNodeId'] ?? '',
      toPortId: data['toPortId'] ?? '',
      color: Color(data['color'] ?? Colors.grey.value),
    );
  }

  Map<String, dynamic> _nodeToMap(GraphNode node) {
    return {
      'id': node.id,
      'name': node.name,
      'inputs': node.inputs
          .map(
            (port) => {
              'id': port.id,
              'name': port.name,
              'isInput': port.isInput,
              'color': port.color.value,
            },
          )
          .toList(),
      'outputs': node.outputs
          .map(
            (port) => {
              'id': port.id,
              'name': port.name,
              'isInput': port.isInput,
              'color': port.color.value,
            },
          )
          .toList(),
      'color': node.color.value,
      'position': {'dx': node.position.dx, 'dy': node.position.dy},
      'size': {
        'width': node.size?.width ?? 150.0,
        'height': node.size?.height ?? 80.0,
      },
    };
  }

  Map<String, dynamic> _connectionToMap(GraphConnection connection) {
    return {
      'id': connection.id,
      'fromNodeId': connection.fromNodeId,
      'fromPortId': connection.fromPortId,
      'toNodeId': connection.toNodeId,
      'toPortId': connection.toPortId,
      'color': connection.color.value,
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Row(
          children: [
            Text(
              currentProject != null ? currentProject!.name : 'Graph Editor',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
            ),
            if (_isLoading) ...[
              const SizedBox(width: 12),
              const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
            ],
            if (_loadingMessage != null && !_isLoading) ...[
              const SizedBox(width: 12),
              Text(
                _loadingMessage!,
                style: TextStyle(
                  fontSize: 12,
                  color: Theme.of(
                    context,
                  ).colorScheme.onSurface.withOpacity(0.6),
                ),
              ),
            ],
          ],
        ),
        actions: [
          if (currentProject != null) ...[
            IconButton(
              icon: _isSaving
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.save),
              onPressed: _isSaving ? null : _saveCurrentProject,
              tooltip: _isSaving ? 'Saving...' : 'Save Project',
            ),
          ],
        ],
      ),
      body: Stack(
        children: [
          GraphEditor(
            initialNodes: _nodes,
            initialConnections: _connections,
            workspaceId: PageManager.instance.currentWorkspaceId,
            pageId: _currentPageId,
            onNodeAdded: (node) {
              print('Node added: ${node.name}');
              setState(() {
                _nodes.add(node);
              });
              _saveCurrentPageState();
            },
            onNodeDeleted: (nodeId) {
              print('Node deleted: $nodeId');
              setState(() {
                _nodes.removeWhere((node) => node.id == nodeId);
                _connections.removeWhere(
                  (conn) =>
                      conn.fromNodeId == nodeId || conn.toNodeId == nodeId,
                );
              });
              _saveCurrentPageState();
            },
            onConnectionAdded: (connection) {
              print('Connection added: ${connection.id}');
              setState(() {
                _connections.add(connection);
              });
              _saveCurrentPageState();
            },
            onConnectionDeleted: (connectionId) {
              print('Connection deleted: $connectionId');
              setState(() {
                _connections.removeWhere((conn) => conn.id == connectionId);
              });
              _saveCurrentPageState();
            },
            onGraphSaved: (jsonString) {
              _onGraphSaved(jsonString);
            },
            onGraphLoaded: (graphData) {
              // Handle different types of graphData (Map or GraphData object)
              if (graphData is Map<String, dynamic>) {
                final nodesLength = graphData['nodes'] is List
                    ? (graphData['nodes'] as List).length
                    : 0;
                final connectionsLength = graphData['connections'] is List
                    ? (graphData['connections'] as List).length
                    : 0;
                print(
                  'Graph loaded: $nodesLength nodes, $connectionsLength connections',
                );
              } else if (graphData != null) {
                try {
                  // Try to access properties safely
                  final nodesLength = graphData.nodes?.length ?? 0;
                  final connectionsLength = graphData.connections?.length ?? 0;
                  print(
                    'Graph loaded: $nodesLength nodes, $connectionsLength connections',
                  );
                } catch (e) {
                  print('Graph loaded but couldn\'t determine size: $e');
                }
              } else {
                print('Graph loaded with null data');
              }
            },
          ),
          if (_isLoading)
            Container(
              color: Colors.black.withOpacity(0.5),
              child: Center(
                child: Card(
                  elevation: 8,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const SizedBox(
                          width: 40,
                          height: 40,
                          child: CircularProgressIndicator(strokeWidth: 3),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _loadingMessage ?? 'Loading...',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  void _onGraphSaved(String jsonString) {
    print('Graph saved to JSON (${jsonString.length} characters)');
    if (currentProject != null) {
      _saveCurrentProject();
    }
  }

  Future<void> _saveCurrentProject() async {
    if (currentProject == null) return;

    setState(() {
      _isSaving = true;
    });

    try {
      final graphData = {
        'nodes': _nodes
            .map(
              (n) => {
                'id': n.id,
                'name': n.name,
                'position': {'x': n.position.dx, 'y': n.position.dy},
              },
            )
            .toList(),
        'connections': _connections
            .map((c) => {'id': c.id, 'from': c.fromNodeId, 'to': c.toNodeId})
            .toList(),
      };

      await _projectService.updateProject(
        currentProject!.id,
        graphData: graphData,
      );

      setState(() {
        _isSaving = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Project saved successfully'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      setState(() {
        _isSaving = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving project: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}
