import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/gestures.dart';
import 'dart:math' as math;
import 'dart:async';
import 'models.dart';
import 'graph_painter.dart';
import 'context_menu.dart';
import 'command_palette.dart';
import 'keyboard_shortcuts.dart';
import 'coordinate_system.dart';
import 'graph_serializer.dart';
import 'node_template.dart';
import 'node_template_service.dart';
import 'property_inspector.dart';
import '../services/websocket_service.dart';
import '../services/persistence_service.dart';
import '../services/graph_persistence_service.dart';

class GraphEditor extends StatefulWidget {
  final List<GraphNode> initialNodes;
  final List<GraphConnection> initialConnections;
  final Function(GraphNode)? onNodeAdded;
  final Function(String)? onNodeDeleted;
  final Function(GraphConnection)? onConnectionAdded;
  final Function(String)? onConnectionDeleted;
  final Function(String)? onGraphSaved; // Callback when graph is saved
  final Function(dynamic)? onGraphLoaded; // Callback when graph is loaded
  final String? workspaceId;
  final String? pageId;

  const GraphEditor({
    super.key,
    this.initialNodes = const [],
    this.initialConnections = const [],
    this.onNodeAdded,
    this.onNodeDeleted,
    this.onConnectionAdded,
    this.onConnectionDeleted,
    this.onGraphSaved,
    this.onGraphLoaded,
    this.workspaceId,
    this.pageId,
  });

  @override
  State<GraphEditor> createState() => _GraphEditorState();

  // Static method to access the state
  static Map<String, dynamic>? getExecutableFlowchart(BuildContext context) {
    final state = context.findAncestorStateOfType<_GraphEditorState>();
    return state?.getExecutableFlowchart();
  }
}

class _GraphEditorState extends State<GraphEditor>
    with TickerProviderStateMixin {
  List<GraphNode> nodes = [];
  List<GraphConnection> connections = [];
  PendingConnection? pendingConnection;

  // WebSocket service for auto-saving
  WebSocketService? _webSocketService;

  double scale = 1.0;
  Offset panOffset = Offset.zero;
  final double minScale = 0.1;
  final double maxScale = 3.0;

  // Graph bounds constraints
  Size? _canvasSize;
  static const double _boundsPadding = 50.0; // Extra padding for boundaries

  // Coordinate system helper
  CoordinateSystem get coordinateSystem =>
      CoordinateSystem(scale: scale, panOffset: panOffset);

  String? selectedNodeId;
  String? selectedConnectionId;
  String? hoveredNodeId;
  Offset? lastFocalPoint;

  bool isDraggingNode = false;
  String? draggingNodeId;
  Offset? dragStartOffset;

  // Connection system state
  bool isConnecting = false;
  String? connectingFromNodeId;
  String? connectingFromPortId;
  Offset? currentMousePosition;

  // UI State
  bool showCommandPalette = false;
  bool showContextMenu = false;
  Offset contextMenuPosition = Offset.zero;
  ContextMenuType contextMenuType = ContextMenuType.canvas;

  // Clipboard state
  GraphNode? _clipboardNode;

  // Animation controllers for smooth interactions
  AnimationController? _scaleAnimationController;
  Animation<double>? _scaleAnimation;
  AnimationController? _panAnimationController;
  Animation<Offset>? _panAnimation;

  // Performance tracking
  DateTime? _lastPanUpdate;

  // Keyboard state
  final Set<LogicalKeyboardKey> _pressedKeys = {};
  final FocusNode _focusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    nodes = List.from(widget.initialNodes);
    connections = List.from(widget.initialConnections);

    // Initialize animation controllers for smooth interactions
    _scaleAnimationController = AnimationController(
      duration: const Duration(
        milliseconds: 150,
      ), // Reduced from 300ms for snappier feel
      vsync: this,
    );
    _panAnimationController = AnimationController(
      duration: const Duration(
        milliseconds: 150,
      ), // Reduced from 300ms for snappier feel
      vsync: this,
    );

    // Initialize WebSocket service for real-time updates
    _webSocketService = WebSocketService.instance;

    // Listen for WebSocket status changes
    _webSocketService!.status.listen(_handleWebSocketStatusChange);

    // Set the current graph for persistence service and listen for updates
    final persistenceService = GraphPersistenceService.instance;
    final workspaceId = widget.workspaceId ?? 'default_workspace';
    final pageId = widget.pageId ?? 'default';
    // Combine workspace ID and page ID to create a unique graph ID
    final graphId = '${workspaceId}-${pageId}';
    persistenceService.setCurrentGraph(graphId, workspaceId);
    persistenceService.addListener(_handleGraphDataUpdate);

    // Load node templates
    NodeTemplateService.instance.loadTemplates();

    // Load graph data from persistence service (which handles WebSocket loading)
    _loadGraphFromPersistenceService();

    // Load view state from local persistence (but don't apply if we have nodes)
    _loadViewState();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
      // Auto-fit to show all nodes if we have any
      if (nodes.isNotEmpty) {
        _frameAll();
      }
    });
  }

  @override
  void didUpdateWidget(GraphEditor oldWidget) {
    super.didUpdateWidget(oldWidget);

    // Check if pageId or workspaceId has changed
    if (oldWidget.pageId != widget.pageId ||
        oldWidget.workspaceId != widget.workspaceId) {
      debugPrint(
        'GraphEditor: Page/Workspace changed from ${oldWidget.workspaceId}-${oldWidget.pageId} to ${widget.workspaceId}-${widget.pageId}',
      );

      // Update persistence service to use the new graph ID
      final persistenceService = GraphPersistenceService.instance;
      final workspaceId = widget.workspaceId ?? 'default_workspace';
      final pageId = widget.pageId ?? 'default';
      final graphId = '$workspaceId-$pageId';

      // Reset current graph state
      setState(() {
        nodes = List.from(widget.initialNodes);
        connections = List.from(widget.initialConnections);
        scale = 1.0;
        panOffset = Offset.zero;
        selectedNodeId = null;
        selectedConnectionId = null;
      });

      // Configure persistence service for the new graph and load it
      persistenceService.setCurrentGraph(graphId, workspaceId);
      _loadGraphFromPersistenceService();
    }
  }

  void _handleGraphDataUpdate() {
    // Called when GraphPersistenceService has new data available
    if (!mounted) return;

    final persistenceService = GraphPersistenceService.instance;
    final workspaceId = widget.workspaceId ?? 'default_workspace';
    final pageId = widget.pageId ?? 'default';
    final graphId = '$workspaceId-$pageId';
    final graphData = persistenceService.getCachedGraph(graphId);

    if (graphData != null) {
      debugPrint(
        'GraphEditor: Received updated graph data from persistence service',
      );
      _applyLoadedGraphData(graphData);
    }
  }

  void _loadGraphFromPersistenceService() {
    final persistenceService = GraphPersistenceService.instance;
    final workspaceId = widget.workspaceId ?? 'default_workspace';
    final pageId = widget.pageId ?? 'default';

    // Construct the graph ID according to the expected format
    String graphId;
    if (workspaceId.startsWith('workspace_')) {
      graphId = '$workspaceId-$pageId';
    } else {
      graphId = pageId;
    }

    debugPrint('GraphEditor: Loading graph with ID: $graphId');

    // Try to get cached data first
    final cachedData = persistenceService.getCachedGraph(graphId);
    if (cachedData != null) {
      debugPrint('GraphEditor: Using cached graph data');
      _applyLoadedGraphData(cachedData);
    } else {
      // Request fresh data from WebSocket
      debugPrint('GraphEditor: Requesting fresh graph data');
      persistenceService.loadGraph(graphId);

      // Set the current graph in the persistence service
      persistenceService.setCurrentGraph(graphId, workspaceId);

      // If WebSocket fails, fallback to local persistence
      Timer(const Duration(seconds: 2), () {
        if (mounted && nodes.isEmpty) {
          debugPrint(
            'GraphEditor: WebSocket load timeout, using local persistence',
          );
          _loadGraphFromLocalPersistence();
        }
      });
    }
  }

  void _handleWebSocketStatusChange(WebSocketConnectionStatus status) {
    debugPrint('WebSocket status changed to: $status');

    // If we just reconnected, ask persistence service to reload
    if (status == WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket reconnected, loading graph...');
      final workspaceId = widget.workspaceId ?? 'default_workspace';
      final pageId = widget.pageId ?? 'default';
      final graphId = '$workspaceId-$pageId';
      GraphPersistenceService.instance.loadGraph(graphId);
    }
  }

  void _handleNodeMove(GraphNode node) {
    // Use the comprehensive graph persistence service for real-time updates
    GraphPersistenceService.instance.updateNodePosition(node.id, node.position);
  }

  // Load view state from local persistence
  void _loadViewState() async {
    final viewState = await PersistenceService.instance.loadGraphViewState();
    if (viewState != null) {
      // Only apply viewport state if there are no nodes (empty graph)
      if (nodes.isEmpty) {
        setState(() {
          scale = (viewState['scale'] as double?) ?? 1.0;
          final panData = viewState['panOffset'] as Map<String, dynamic>?;
          if (panData != null) {
            panOffset = Offset(
              (panData['dx'] as double?) ?? 0.0,
              (panData['dy'] as double?) ?? 0.0,
            );
          }
        });
      }

      // Always load selection state
      setState(() {
        selectedNodeId = viewState['selectedNodeId'] as String?;
        selectedConnectionId = viewState['selectedConnectionId'] as String?;
      });
    }
  }

  // Load graph data from WebSocket service
  // This method is not currently used but kept for reference
  /*
  void _loadGraphFromWebSocket() {
    if (_webSocketService == null ||
        _webSocketService!.currentStatus !=
            WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, loading from local persistence');
      _loadGraphFromLocalPersistence();
      return;
    }

    debugPrint('Loading graph from WebSocket...');
    _webSocketService!.sendMessage('graph_load', {'graphId': 'default'});
  }
  */

  // This method is not currently used but kept for reference
  /*
  void _handleGraphLoadResponse(WebSocketMessage message) {
    if (message.type == 'graph_load_response') {
      debugPrint(
        'Received graph load response: success=${message.data['success']}',
      );
      if (message.data['success'] == true) {
        final graphData = message.data['graphData'];
        if (graphData != null) {
          debugPrint(
            'Applying loaded graph data with ${graphData['nodes']?.length ?? 0} nodes',
          );
          _applyLoadedGraphData(graphData);
        } else {
          debugPrint('No graph data received, loading from local persistence');
          _loadGraphFromLocalPersistence();
        }
      } else {
        debugPrint('Graph load failed, loading from local persistence');
        _loadGraphFromLocalPersistence();
      }
    }
  }
  */

  // This method is not currently used but kept for reference
  /*
  void _handleBatchUpdate(WebSocketMessage message) {
    // Check if widget is still mounted before processing updates
    if (!mounted) {
      debugPrint('Widget not mounted, skipping batch update');
      return;
    }

    final updates = message.data['updates'] as List<dynamic>?;
    if (updates != null) {
      setState(() {
        for (final update in updates) {
          final updateData = update as Map<String, dynamic>;
          final updateType = updateData['updateType'] as String?;
          final nodeData = updateData['node'] as Map<String, dynamic>?;
          final connectionData =
              updateData['connection'] as Map<String, dynamic>?;

          if (nodeData != null) {
            final nodeId = nodeData['id'] as String?;
            if (nodeId != null) {
              final nodeIndex = nodes.indexWhere((n) => n.id == nodeId);
              if (updateType == 'node_add' && nodeIndex == -1) {
                nodes.add(GraphNode.fromJson(nodeData));
              } else if (updateType == 'node_update' && nodeIndex != -1) {
                nodes[nodeIndex] = GraphNode.fromJson(nodeData);
              }
            }
          }

          if (connectionData != null) {
            final connectionId = connectionData['id'] as String?;
            if (connectionId != null) {
              final connectionIndex = connections.indexWhere(
                (c) => c.id == connectionId,
              );
              if (updateType == 'connection_add' && connectionIndex == -1) {
                connections.add(GraphConnection.fromJson(connectionData));
              } else if (updateType == 'connection_update' &&
                  connectionIndex != -1) {
                connections[connectionIndex] = GraphConnection.fromJson(
                  connectionData,
                );
              }
            }
          }
        }
      });
    }
  }
  */

  // Viewport updates are now local only - this method is no longer needed

  // Load graph data from local persistence
  void _loadGraphFromLocalPersistence() async {
    String graphId = 'default';

    // Format graph ID properly if we have workspace and page IDs
    if (widget.workspaceId != null && widget.pageId != null) {
      if (widget.workspaceId!.startsWith('workspace_')) {
        graphId = '${widget.workspaceId}-${widget.pageId}';
      } else {
        graphId = widget.pageId!;
      }
      debugPrint('Loading graph with ID: $graphId from local persistence');
    }

    // Try to get cached data from GraphPersistenceService
    final graphPersistenceService = GraphPersistenceService.instance;
    final cachedData = graphPersistenceService.getCachedGraph(graphId);

    if (cachedData != null) {
      debugPrint('Found cached graph data for $graphId');
      _applyLoadedGraphData(cachedData);
    } else {
      debugPrint('No cached data found, attempting to load from server');
      graphPersistenceService.loadGraph(graphId);
    }
  }

  // Apply loaded graph data to the editor
  void _applyLoadedGraphData(Map<String, dynamic> graphData) {
    // Check if widget is still mounted before updating state
    if (!mounted) {
      debugPrint('Widget not mounted, skipping graph data application');
      return;
    }

    // Safely access nodes and connections with null handling
    final nodesData = graphData['nodes'];
    final connectionsData = graphData['connections'];

    debugPrint(
      'Applying graph data: nodes=${nodesData is List ? nodesData.length : 0}, '
      'connections=${connectionsData is List ? connectionsData.length : 0}',
    );

    // Clear existing nodes and connections first to avoid state sharing between pages
    List<GraphNode> loadedNodes = [];
    List<GraphConnection> loadedConnections = [];

    if (nodesData is List) {
      try {
        loadedNodes = nodesData
            .map((nodeData) {
              if (nodeData is Map<String, dynamic>) {
                final node = GraphNode.fromJson(nodeData);
                debugPrint(
                  'Loaded node: ${node.name} at position (${node.position.dx}, ${node.position.dy})',
                );
                return node;
              } else if (nodeData != null) {
                // Try to convert to Map<String, dynamic> if it's another Map type
                try {
                  final nodeMap = Map<String, dynamic>.from(nodeData as Map);
                  return GraphNode.fromJson(nodeMap);
                } catch (e) {
                  debugPrint('Error converting node data: $e');
                  return null;
                }
              }
              return null;
            })
            .whereType<GraphNode>()
            .toList();
      } catch (e) {
        debugPrint('Error parsing nodes: $e');
        loadedNodes = [];
      }
    }

    if (connectionsData is List) {
      try {
        loadedConnections = connectionsData
            .map((connData) {
              if (connData is Map<String, dynamic>) {
                return GraphConnection.fromJson(connData);
              } else if (connData != null) {
                // Try to convert to Map<String, dynamic> if it's another Map type
                try {
                  final connMap = Map<String, dynamic>.from(connData as Map);
                  return GraphConnection.fromJson(connMap);
                } catch (e) {
                  debugPrint('Error converting connection data: $e');
                  return null;
                }
              }
              return null;
            })
            .whereType<GraphConnection>()
            .toList();
      } catch (e) {
        debugPrint('Error parsing connections: $e');
        loadedConnections = [];
      }
    }

    // Only update state if the widget is still mounted
    if (mounted) {
      setState(() {
        nodes = loadedNodes;
        connections = loadedConnections;

        // Clean up invalid selections
        if (selectedNodeId != null &&
            !loadedNodes.any((node) => node.id == selectedNodeId)) {
          selectedNodeId = null;
        }
        if (selectedConnectionId != null &&
            !loadedConnections.any((conn) => conn.id == selectedConnectionId)) {
          selectedConnectionId = null;
        }
      });
    }

    debugPrint(
      'Applied ${loadedNodes.length} nodes and ${loadedConnections.length} connections',
    );

    // Notify parent about loaded graph if callback is provided
    if (mounted && widget.onGraphLoaded != null) {
      try {
        // Create a safe data structure for the callback
        final callbackData = {
          'nodes': loadedNodes,
          'connections': loadedConnections,
          'metadata': <String, dynamic>{},
          'version': '1.0',
        };
        widget.onGraphLoaded!(callbackData);
      } catch (e) {
        debugPrint('Error in onGraphLoaded callback: $e');
      }
    }

    // Auto-fit to show all nodes after loading
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _frameAll();
      }
    });
  }

  // Auto-save graph data to WebSocket service and local persistence
  void _autoSaveGraph() {
    debugPrint(
      'Auto-saving graph with ${nodes.length} nodes and ${connections.length} connections',
    );

    final graphData = {
      'nodes': nodes
          .map(
            (node) => {
              'id': node.id,
              'name': node.name,
              'position': {'x': node.position.dx, 'y': node.position.dy},
              'inputs': node.inputs.map((port) => port.toJson()).toList(),
              'outputs': node.outputs.map((port) => port.toJson()).toList(),
              'color': node.color.value,
              'size': node.size != null
                  ? {'width': node.size!.width, 'height': node.size!.height}
                  : null,
              'templateId': node.templateId,
              'properties': node.properties,
            },
          )
          .toList(),
      'connections': connections
          .map(
            (conn) => {
              'id': conn.id,
              'fromNodeId': conn.fromNodeId,
              'fromPortId': conn.fromPortId,
              'toNodeId': conn.toNodeId,
              'toPortId': conn.toPortId,
              'color': conn.color.value,
            },
          )
          .toList(),
    };

    // Get the current graph ID (workspaceId-pageId format)
    final workspaceId = widget.workspaceId ?? 'default_workspace';
    final pageId = widget.pageId ?? 'default';
    final graphId = '$workspaceId-$pageId';

    // Use the comprehensive graph persistence service
    GraphPersistenceService.instance.updateGraphDataCache(graphId, graphData);
    GraphPersistenceService.instance.markGraphAsModified();
    GraphPersistenceService.instance.forceSave(graphData: graphData);

    // Save view state
    _saveViewState();
  }

  // This method is not currently used but kept for reference
  /*
  void _sendViewportUpdate() {
    // Viewport updates are now local only - no WebSocket communication needed
    // This method is kept for potential future use but doesn't send WebSocket messages
  }
  */

  // Save the current view state (zoom, pan, selection)
  void _saveViewState() {
    PersistenceService.instance.saveGraphViewState(
      scale: scale,
      panOffset: panOffset,
      selectedNodeId: selectedNodeId,
      selectedConnectionId: selectedConnectionId,
    );

    // Viewport state is now local only - no WebSocket sync needed
  }

  @override
  void dispose() {
    _scaleAnimationController?.dispose();
    _panAnimationController?.dispose();
    _focusNode.dispose();
    // Don't dispose the singleton WebSocket service - it should persist across workspace switches
    // _webSocketService?.dispose();
    super.dispose();
  }

  // Graph serialization methods
  String saveGraphToJson({Map<String, dynamic>? metadata}) {
    final jsonString = GraphSerializer.serializeGraph(
      nodes: nodes,
      connections: connections,
      metadata: metadata,
    );
    widget.onGraphSaved?.call(jsonString);
    return jsonString;
  }

  String saveGraphToJsonPretty({Map<String, dynamic>? metadata}) {
    return GraphSerializer.serializeGraphPretty(
      nodes: nodes,
      connections: connections,
      metadata: metadata,
    );
  }

  // WebSocket-ready JSON for flowchart execution
  Map<String, dynamic> getExecutableFlowchart() {
    return {
      'type': 'flowchart_execution',
      'timestamp': DateTime.now().toIso8601String(),
      'graph': {
        'nodes': nodes
            .map(
              (node) => {
                'id': node.id,
                'type': node.templateId ?? 'unknown',
                'name': node.name,
                'properties': node.properties,
                'position': {'x': node.position.dx, 'y': node.position.dy},
              },
            )
            .toList(),
        'connections': connections
            .map(
              (conn) => {
                'id': conn.id,
                'from': {'nodeId': conn.fromNodeId, 'portId': conn.fromPortId},
                'to': {'nodeId': conn.toNodeId, 'portId': conn.toPortId},
              },
            )
            .toList(),
      },
      'metadata': {
        'version': '1.0',
        'platform': 'flutter_web',
        'nodeCount': nodes.length,
        'connectionCount': connections.length,
      },
    };
  }

  void loadGraphFromJson(String jsonString) {
    try {
      final graphData = GraphSerializer.deserializeGraph(jsonString);
      setState(() {
        nodes = graphData.nodes;
        connections = graphData.connections;
        // Reset UI state
        selectedNodeId = null;
        hoveredNodeId = null;
        pendingConnection = null;
        isConnecting = false;
        connectingFromNodeId = null;
        connectingFromPortId = null;
      });
      widget.onGraphLoaded?.call(graphData);
    } catch (e) {
      debugPrint('Failed to load graph: $e');
      // You could show a snackbar or dialog here
    }
  }

  void clearGraph() {
    setState(() {
      nodes.clear();
      connections.clear();
      selectedNodeId = null;
      hoveredNodeId = null;
      pendingConnection = null;
      isConnecting = false;
      connectingFromNodeId = null;
      connectingFromPortId = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Focus(
      focusNode: _focusNode,
      autofocus: true,
      child: KeyboardListener(
        focusNode: FocusNode(),
        onKeyEvent: _handleKeyEvent,
        child: GestureDetector(
          onTap: () => _focusNode.requestFocus(),
          child: Container(
            color: const Color(0xFF1A1A1A), // Darker background like Blender
            child: Row(
              children: [
                // Main graph area
                Expanded(
                  child: LayoutBuilder(
                    builder: (context, constraints) {
                      // Update canvas size for bounds calculations
                      _canvasSize = Size(
                        constraints.maxWidth,
                        constraints.maxHeight,
                      );

                      return Stack(
                        children: [
                          // Graph canvas
                          GestureDetector(
                            onPanStart: _handlePanStart,
                            onPanUpdate: _handlePanUpdate,
                            onPanEnd: _handlePanEnd,
                            child: Listener(
                              onPointerDown: _handlePointerDown,
                              onPointerMove: _handlePointerMove,
                              onPointerUp: _handlePointerUp,
                              onPointerSignal: _handlePointerSignal,
                              child: GestureDetector(
                                onTapDown: _handleTapDown,
                                onTapUp: _handleTapUp,
                                onScaleStart: _handleScaleStart,
                                onScaleUpdate: _handleScaleUpdate,
                                onScaleEnd: _handleScaleEnd,
                                child: CustomPaint(
                                  painter: GraphPainter(
                                    nodes: nodes,
                                    connections: connections,
                                    pendingConnection: pendingConnection,
                                    scale: scale,
                                    offset: panOffset,
                                  ),
                                  size: Size.infinite,
                                ),
                              ),
                            ),
                          ),

                          // Context menu
                          if (showContextMenu)
                            ContextMenu(
                              items: _getContextMenuItems(),
                              position: contextMenuPosition,
                              onDismiss: () =>
                                  setState(() => showContextMenu = false),
                            ),

                          // Command palette
                          if (showCommandPalette)
                            CommandPalette(
                              onNodeSelected: _addNodeFromTemplate,
                              onDismiss: () =>
                                  setState(() => showCommandPalette = false),
                              spawnPosition: _getSpawnPosition(),
                            ),

                          // Info overlay (shortcuts, etc.)
                          Positioned(
                            bottom: 16,
                            right: 16,
                            child: _buildInfoPanel(),
                          ),
                        ],
                      );
                    },
                  ),
                ),

                // Property inspector panel
                SizedBox(
                  width: 300,
                  child: PropertyInspector(
                    selectedNode: selectedNodeId != null
                        ? nodes
                              .where((node) => node.id == selectedNodeId)
                              .firstOrNull
                        : null,
                    onNodeUpdated: _updateNode,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoPanel() {
    return Container(
      constraints: const BoxConstraints(maxWidth: 400),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(0.8),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text(
            'Keyboard Shortcuts',
            style: TextStyle(
              color: Colors.white,
              fontSize: 12,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            KeyboardShortcuts.getShortcutTooltip(),
            style: const TextStyle(
              color: Colors.white70,
              fontSize: 10,
              height: 1.3,
            ),
          ),
        ],
      ),
    );
  }

  List<ContextMenuItem> _getContextMenuItems() {
    if (contextMenuType == ContextMenuType.node && selectedNodeId != null) {
      return [
        ContextMenuItem(
          label: 'Delete',
          icon: Icons.delete,
          shortcut: KeyboardShortcuts.getShortcutDisplay('delete'),
          onTap: () => _deleteNode(selectedNodeId!),
          color: Colors.red,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Duplicate',
          icon: Icons.copy,
          shortcut: 'Shift+D',
          onTap: _duplicateSelectedNode,
        ),
        ContextMenuItem(
          label: 'Copy',
          icon: Icons.content_copy,
          shortcut: KeyboardShortcuts.getShortcutDisplay('copy'),
          onTap: _copySelectedNode,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Bring to Front',
          icon: Icons.flip_to_front,
          onTap: _bringNodeToFront,
        ),
        ContextMenuItem(
          label: 'Send to Back',
          icon: Icons.flip_to_back,
          onTap: _sendNodeToBack,
        ),
      ];
    } else if (contextMenuType == ContextMenuType.connection &&
        selectedConnectionId != null) {
      return [
        ContextMenuItem(
          label: 'Delete Connection',
          icon: Icons.delete,
          shortcut: KeyboardShortcuts.getShortcutDisplay('delete'),
          onTap: () => _deleteConnection(selectedConnectionId!),
          color: Colors.red,
        ),
      ];
    } else {
      return [
        ContextMenuItem(
          label: 'Add Node',
          icon: Icons.add,
          shortcut: KeyboardShortcuts.getShortcutDisplay('addNode'),
          onTap: _showCommandPalette,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Paste',
          icon: Icons.content_paste,
          shortcut: KeyboardShortcuts.getShortcutDisplay('paste'),
          onTap: _pasteNode,
          enabled: _clipboardNode != null,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Select All',
          icon: Icons.select_all,
          shortcut: KeyboardShortcuts.getShortcutDisplay('selectAll'),
          onTap: _selectAll,
        ),
        ContextMenuItem(
          label: 'Clear All',
          icon: Icons.clear_all,
          onTap: _confirmClearAll,
          color: Colors.orange,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Save Graph',
          icon: Icons.save,
          shortcut: KeyboardShortcuts.getShortcutDisplay('save'),
          onTap: _saveGraph,
        ),
        ContextMenuItem(
          label: 'Load Graph',
          icon: Icons.folder_open,
          shortcut: KeyboardShortcuts.getShortcutDisplay('open'),
          onTap: _loadGraph,
        ),
        const ContextMenuItem.divider(),
        ContextMenuItem(
          label: 'Reset View',
          icon: Icons.center_focus_strong,
          shortcut: KeyboardShortcuts.getShortcutDisplay('resetZoom'),
          onTap: _resetView,
        ),
        ContextMenuItem(
          label: 'Frame All',
          icon: Icons.fit_screen,
          shortcut: KeyboardShortcuts.getShortcutDisplay('home'),
          onTap: _frameAll,
        ),
      ];
    }
  }

  void _handleKeyEvent(KeyEvent event) {
    if (event is KeyDownEvent) {
      _pressedKeys.add(event.logicalKey);
      _processKeyboardShortcuts();
    } else if (event is KeyUpEvent) {
      _pressedKeys.remove(event.logicalKey);
    }
  }

  void _processKeyboardShortcuts() {
    // Debug print to see what keys are pressed
    if (_pressedKeys.isNotEmpty) {
      print('Pressed keys: ${_pressedKeys.map((k) => k.keyLabel).join(', ')}');
    }

    if (KeyboardShortcuts.isPressed('delete', _pressedKeys)) {
      print('Delete shortcut detected');
      if (selectedNodeId != null) {
        _deleteNode(selectedNodeId!);
      }
    } else if (KeyboardShortcuts.isPressed('deselect', _pressedKeys)) {
      print('Deselect shortcut detected');
      _deselectAll();
    } else if (KeyboardShortcuts.isPressed('copy', _pressedKeys)) {
      print('Copy shortcut detected');
      _copySelectedNode();
    } else if (KeyboardShortcuts.isPressed('paste', _pressedKeys)) {
      print('Paste shortcut detected');
      _pasteNode();
    } else if (KeyboardShortcuts.isPressed('duplicate', _pressedKeys)) {
      print('Duplicate shortcut detected');
      _duplicateSelectedNode();
    } else if (KeyboardShortcuts.isPressed('save', _pressedKeys)) {
      print('Save shortcut detected');
      _saveGraph();
    } else if (KeyboardShortcuts.isPressed('addNode', _pressedKeys)) {
      print('Add node shortcut detected');
      _showCommandPalette();
    } else if (KeyboardShortcuts.isPressed('zoomIn', _pressedKeys)) {
      print('Zoom in shortcut detected');
      _zoomIn();
    } else if (KeyboardShortcuts.isPressed('zoomOut', _pressedKeys)) {
      print('Zoom out shortcut detected');
      _zoomOut();
    } else if (KeyboardShortcuts.isPressed('resetZoom', _pressedKeys)) {
      print('Reset zoom shortcut detected');
      _resetView();
    } else if (KeyboardShortcuts.isPressed('home', _pressedKeys)) {
      print('Home shortcut detected');
      _frameAll();
    } else if (KeyboardShortcuts.isPressed('commandPalette', _pressedKeys)) {
      print('Command palette shortcut detected');
      _showCommandPalette();
    } else if (KeyboardShortcuts.isPressed('selectAll', _pressedKeys)) {
      print('Select all shortcut detected');
      _selectAll();
    }
  }

  void _handlePointerDown(PointerDownEvent event) {
    // Right click detection - button 2 is right mouse button
    if (event.buttons == 2) {
      if (showContextMenu) {
        // If context menu is already showing, hide it
        setState(() {
          showContextMenu = false;
        });
      } else {
        // Show context menu
        _showContextMenuAt(event.localPosition);
      }
    }
  }

  void _handlePointerMove(PointerMoveEvent event) {
    // Update current mouse position for connection preview without setState
    currentMousePosition = event.localPosition;

    // Update pending connection if we're connecting
    if (isConnecting &&
        connectingFromNodeId != null &&
        connectingFromPortId != null) {
      final graphPosition = _screenToGraph(event.localPosition);
      pendingConnection = PendingConnection(
        fromNodeId: connectingFromNodeId!,
        fromPortId: connectingFromPortId!,
        currentPosition: graphPosition,
      );

      // Only call setState for connection updates to trigger repaint
      setState(() {});
    }
  }

  void _handlePointerUp(PointerUpEvent event) {
    // Clean up any temporary states
  }

  void _handlePointerSignal(PointerSignalEvent event) {
    if (event is PointerScrollEvent) {
      // Handle mouse wheel zoom with more responsive scaling
      final zoomDelta = event.scrollDelta.dy;
      // Use a more moderate zoom factor for better control
      final zoomFactor = zoomDelta > 0 ? 1.0 / 1.1 : 1.1;

      // Zoom towards the mouse cursor position with immediate response
      final targetScale = (scale * zoomFactor).clamp(minScale, maxScale);
      if ((targetScale - scale).abs() > 0.001) {
        // Calculate the point to zoom towards
        final graphPoint = _screenToGraph(event.localPosition);
        final newPanOffset = event.localPosition - (graphPoint * targetScale);

        setState(() {
          scale = targetScale;
          panOffset = newPanOffset;
        });
        _saveViewState();
      }
    }
  }

  void _showContextMenuAt(Offset position) {
    final graphPosition = _screenToGraph(position);

    // Check if we clicked on a connection first
    final clickedConnection = _getConnectionAt(graphPosition);
    if (clickedConnection != null) {
      setState(() {
        selectedConnectionId = clickedConnection.id;
        selectedNodeId = null; // Deselect nodes when selecting connections
        contextMenuType = ContextMenuType.connection;
        contextMenuPosition = position;
        showContextMenu = true;
      });
      return;
    }

    // Check if clicking on a node
    for (final node in nodes.reversed) {
      if (node.rect.contains(graphPosition)) {
        setState(() {
          selectedNodeId = node.id;
          selectedConnectionId = null; // Deselect connections
          contextMenuType = ContextMenuType.node;
          contextMenuPosition = position;
          showContextMenu = true;
        });
        return;
      }
    }

    // Clicking on empty space
    setState(() {
      selectedNodeId = null;
      selectedConnectionId = null;
      contextMenuType = ContextMenuType.canvas;
      contextMenuPosition = position;
      showContextMenu = true;
    });
  }

  void _showCommandPalette() {
    setState(() {
      showCommandPalette = true;
      showContextMenu = false;
    });
  }

  Offset _getSpawnPosition() {
    // Return center of visible area
    final center = MediaQuery.of(context).size.center(Offset.zero);
    return _screenToGraph(center);
  }

  void _addNodeFromTemplate(NodeTemplate template, Offset position) {
    final newNode = GraphNode.fromTemplate(
      template,
      nodeId: '${template.id}_${DateTime.now().millisecondsSinceEpoch}',
      position: position,
    );

    setState(() {
      nodes.add(newNode);
      showCommandPalette = false;
    });

    widget.onNodeAdded?.call(newNode);

    // Auto-save after adding node
    _autoSaveGraph();
  }

  void _updateNode(GraphNode updatedNode) {
    setState(() {
      final index = nodes.indexWhere((node) => node.id == updatedNode.id);
      if (index != -1) {
        nodes[index] = updatedNode;
      }
    });
  }

  void _duplicateSelectedNode() {
    if (selectedNodeId == null) return;

    final node = nodes.where((n) => n.id == selectedNodeId).firstOrNull;
    if (node == null) return;
    final newNode = node.copyWith(
      id: '${node.id}_copy_${DateTime.now().millisecondsSinceEpoch}',
      position: node.position + const Offset(20, 20),
    );

    setState(() {
      nodes.add(newNode);
      selectedNodeId = newNode.id;
    });

    widget.onNodeAdded?.call(newNode);
  }

  void _selectAll() {
    // For now, just select the first node if any exist
    // In a full implementation, you'd support multi-selection
    if (nodes.isNotEmpty) {
      setState(() {
        selectedNodeId = nodes.first.id;
      });
    }
  }

  void _deselectAll() {
    setState(() {
      selectedNodeId = null;
      hoveredNodeId = null;
      // Also clear connection state if deselecting
      if (isConnecting) {
        isConnecting = false;
        connectingFromNodeId = null;
        connectingFromPortId = null;
        pendingConnection = null;
      }
    });
  }

  void _resetView() {
    setState(() {
      scale = 1.0;
      panOffset = Offset.zero;
    });
  }

  void _frameAll() {
    if (nodes.isEmpty) return;

    // Calculate bounding box of all nodes
    double minX = nodes.first.position.dx;
    double minY = nodes.first.position.dy;
    double maxX = nodes.first.position.dx + 150;
    double maxY = nodes.first.position.dy + 80;

    for (final node in nodes) {
      final nodeSize = node.size ?? const Size(150, 80);
      minX = math.min(minX, node.position.dx);
      minY = math.min(minY, node.position.dy);
      maxX = math.max(maxX, node.position.dx + nodeSize.width);
      maxY = math.max(maxY, node.position.dy + nodeSize.height);
    }

    // Add padding
    const padding = 50.0;
    minX -= padding;
    minY -= padding;
    maxX += padding;
    maxY += padding;

    // Calculate scale and offset to fit all nodes
    final boundingWidth = maxX - minX;
    final boundingHeight = maxY - minY;

    // Use the actual canvas size instead of screen size for better accuracy
    final canvasSize = _canvasSize ?? MediaQuery.of(context).size;
    final scaleX = canvasSize.width / boundingWidth;
    final scaleY = canvasSize.height / boundingHeight;
    final newScale = math.min(scaleX, scaleY).clamp(minScale, maxScale);

    final centerX = (minX + maxX) / 2;
    final centerY = (minY + maxY) / 2;

    setState(() {
      scale = newScale;
      panOffset = Offset(
        canvasSize.width / 2 - centerX * scale,
        canvasSize.height / 2 - centerY * scale,
      );
    });
  }

  void _completeConnection(String toNodeId, String toPortId) {
    if (!isConnecting ||
        connectingFromNodeId == null ||
        connectingFromPortId == null) {
      return;
    }

    final fromNode = nodes
        .where((n) => n.id == connectingFromNodeId)
        .firstOrNull;
    final toNode = nodes.where((n) => n.id == toNodeId).firstOrNull;

    if (fromNode == null || toNode == null) return;

    final fromPort = [
      ...fromNode.inputs,
      ...fromNode.outputs,
    ].where((p) => p.id == connectingFromPortId).firstOrNull;
    final toPort = [
      ...toNode.inputs,
      ...toNode.outputs,
    ].where((p) => p.id == toPortId).firstOrNull;

    if (fromPort == null || toPort == null) return;

    // Validate connection (output to input, different nodes, etc.)
    if (!fromPort.isInput && toPort.isInput && fromNode.id != toNode.id) {
      // Check if connection already exists
      final existingConnection = connections.any(
        (c) =>
            c.fromNodeId == fromNode.id &&
            c.fromPortId == fromPort.id &&
            c.toNodeId == toNode.id &&
            c.toPortId == toPort.id,
      );

      if (!existingConnection) {
        setState(() {
          connections.add(
            GraphConnection(
              id: 'connection_${DateTime.now().millisecondsSinceEpoch}',
              fromNodeId: fromNode.id,
              fromPortId: fromPort.id,
              toNodeId: toNode.id,
              toPortId: toPort.id,
            ),
          );
        });

        // Auto-save after creating connection
        _autoSaveGraph();
      }
    }

    // Reset connection state
    setState(() {
      isConnecting = false;
      connectingFromNodeId = null;
      connectingFromPortId = null;
      pendingConnection = null;
    });
  }

  void _handleTapDown(TapDownDetails details) {
    final localPosition = _screenToGraph(details.localPosition);
    print(
      '🔍 Tap: screen=${details.localPosition} → graph=$localPosition (connecting: $isConnecting)',
    );

    // Check if tapping on a node
    for (final node in nodes.reversed) {
      print('\nChecking node: ${node.name} (${node.id})');
      print('Node rect: ${node.rect}');
      print('Node position: ${node.position}');
      print('Contains point: ${node.rect.contains(localPosition)}');

      if (node.rect.contains(localPosition)) {
        final nodeLocalPos = localPosition - node.position;
        print('Node local position: $nodeLocalPos');

        final portId = node.getPortAt(nodeLocalPos);
        print('Port ID found: $portId');

        // Debug all ports
        print(
          'Input ports: ${node.inputs.map((p) => '${p.id}(${p.name})').join(', ')}',
        );
        print(
          'Output ports: ${node.outputs.map((p) => '${p.id}(${p.name})').join(', ')}',
        );

        if (portId != null) {
          // Find the port and check if it's input or output
          GraphPort? foundPort;
          bool isInputPort = false;

          // Check in inputs first
          for (final port in node.inputs) {
            if (port.id == portId) {
              foundPort = port;
              isInputPort = true;
              break;
            }
          }

          // If not found in inputs, check outputs
          if (foundPort == null) {
            for (final port in node.outputs) {
              if (port.id == portId) {
                foundPort = port;
                isInputPort = false;
                break;
              }
            }
          }

          print('Found port: ${foundPort?.name} (ID: ${foundPort?.id})');
          print('Is input port: $isInputPort');

          if (foundPort != null) {
            if (!isInputPort) {
              // Starting a connection from output port
              print('STARTING CONNECTION from output port');
              setState(() {
                isConnecting = true;
                connectingFromNodeId = node.id;
                connectingFromPortId = portId;
                currentMousePosition = details.localPosition;

                pendingConnection = PendingConnection(
                  fromNodeId: node.id,
                  fromPortId: portId,
                  currentPosition: localPosition,
                );
              });
              print('Connection state set - isConnecting: $isConnecting');
              return;
            } else {
              // If we're already connecting and clicked on input port, try to complete connection
              print('Clicked on input port - isConnecting: $isConnecting');
              if (isConnecting &&
                  connectingFromNodeId != null &&
                  connectingFromPortId != null) {
                print('COMPLETING CONNECTION to input port');
                _completeConnection(node.id, portId);
                return;
              }
            }
          }
        } else {
          // Clicked on node body (not on a port)
          print('Clicked on node body (no port detected)');
          if (!isConnecting) {
            print('Starting node drag');
            // Start dragging node
            setState(() {
              selectedNodeId = node.id;
              draggingNodeId = node.id;
              dragStartOffset = nodeLocalPos;
              isDraggingNode = true;
            });
            return;
          }
        }
      }
    }

    // Clicked on empty space - deselect and cancel operations
    _deselectAll();
    setState(() {
      showContextMenu = false;
    });
    print('=== END TAP DOWN DEBUG ===\n');
  }

  void _handleTapUp(TapUpDetails details) {
    if (isDraggingNode) {
      setState(() {
        isDraggingNode = false;
        draggingNodeId = null;
      });
    }

    // Don't cancel connections on tap up - let them stay active until explicitly cancelled
    // or completed by connecting to another port
  }

  void _handleScaleStart(ScaleStartDetails details) {
    lastFocalPoint = details.localFocalPoint;
  }

  void _handleScaleUpdate(ScaleUpdateDetails details) {
    // Validate input scale value
    if (!details.scale.isFinite || details.scale <= 0) {
      return;
    }

    if (details.scale != 1.0) {
      // Handle zoom with reduced sensitivity for better control
      // Apply a dampening factor to make zooming less aggressive
      final dampenedScale = 1.0 + (details.scale - 1.0) * 0.3;
      final calculatedScale = scale * dampenedScale;

      // Validate calculated scale before clamping
      if (!calculatedScale.isFinite) {
        return;
      }

      final newScale = calculatedScale.clamp(minScale, maxScale);

      // Update scale immediately for responsive zoom
      if ((newScale - scale).abs() > 0.001) {
        setState(() {
          scale = newScale;
        });
        _saveViewState();
      }
    } else if (lastFocalPoint != null) {
      // Handle pan/drag
      if (isDraggingNode && draggingNodeId != null && dragStartOffset != null) {
        // Drag node with improved constraint handling
        final nodeIndex = nodes.indexWhere((n) => n.id == draggingNodeId);
        if (nodeIndex != -1) {
          final graphPoint = _screenToGraph(details.localFocalPoint);
          final uncConstrainedPosition = graphPoint - dragStartOffset!;
          final nodeSize = nodes[nodeIndex].size ?? const Size(150.0, 80.0);
          final constrainedPosition = _constrainNodePosition(
            uncConstrainedPosition,
            nodeSize,
          );

          // Only update if position changed significantly
          if ((constrainedPosition - nodes[nodeIndex].position).distance >
              0.5) {
            setState(() {
              nodes[nodeIndex] = nodes[nodeIndex].copyWith(
                position: constrainedPosition,
              );
            });
            _handleNodeMove(nodes[nodeIndex]);
          }
        }
      } else if (isConnecting && pendingConnection != null) {
        // Update pending connection with real-time preview
        final localPosition = _screenToGraph(details.localFocalPoint);
        currentMousePosition = details.localFocalPoint;
        pendingConnection = pendingConnection!.copyWith(
          currentPosition: localPosition,
        );
        // Only call setState to trigger repaint
        setState(() {});
      } else {
        // Pan the canvas with minimal throttling for better responsiveness
        final now = DateTime.now();
        if (_lastPanUpdate == null ||
            now.difference(_lastPanUpdate!) >=
                const Duration(milliseconds: 16)) {
          // Restored to 16ms for smoother panning
          _lastPanUpdate = now;

          final delta = details.localFocalPoint - lastFocalPoint!;

          // Validate delta before using it
          if (!delta.dx.isFinite || !delta.dy.isFinite) {
            return;
          }

          final uncConstrainedPanOffset = panOffset + delta;
          final constrainedPanOffset = _constrainPanOffset(
            uncConstrainedPanOffset,
          );

          // Update pan offset immediately for responsive panning
          if ((constrainedPanOffset - panOffset).distance > 0.5) {
            // Restored to 0.5 for smoother panning
            setState(() {
              panOffset = constrainedPanOffset;
            });
            _saveViewState();
          }
        }
      }
      lastFocalPoint = details.localFocalPoint;
    }
  }

  void _handleScaleEnd(ScaleEndDetails details) {
    // If we were connecting, try to complete the connection
    if (isConnecting && currentMousePosition != null) {
      final localPosition = _screenToGraph(currentMousePosition!);

      // Check if ending on a valid input port
      for (final node in nodes) {
        if (node.rect.contains(localPosition)) {
          final nodeLocalPos = localPosition - node.position;
          final portId = node.getPortAt(nodeLocalPos);

          if (portId != null) {
            _completeConnection(node.id, portId);
            lastFocalPoint = null;
            return;
          }
        }
      }

      // Connection failed, cancel it
      setState(() {
        isConnecting = false;
        connectingFromNodeId = null;
        connectingFromPortId = null;
        pendingConnection = null;
      });
    }

    // End dragging
    final wasDraggingNode = isDraggingNode;
    setState(() {
      isDraggingNode = false;
      draggingNodeId = null;
      dragStartOffset = null;
    });

    // Auto-save if we were dragging a node
    if (wasDraggingNode) {
      _autoSaveGraph();
    }

    lastFocalPoint = null;
  }

  // Pan handling methods for dragging on empty space
  void _handlePanStart(DragStartDetails details) {
    // Only start panning if we're not already doing something else
    if (!isDraggingNode && !isConnecting) {
      lastFocalPoint = details.localPosition;
    }
  }

  void _handlePanUpdate(DragUpdateDetails details) {
    // Only pan if we're not dragging a node or connecting
    if (!isDraggingNode && !isConnecting && lastFocalPoint != null) {
      final delta = details.localPosition - lastFocalPoint!;

      // Validate delta before using it
      if (!delta.dx.isFinite || !delta.dy.isFinite) {
        return;
      }

      final uncConstrainedPanOffset = panOffset + delta;
      final constrainedPanOffset = _constrainPanOffset(uncConstrainedPanOffset);

      // Update pan offset for smooth panning
      if ((constrainedPanOffset - panOffset).distance > 0.5) {
        setState(() {
          panOffset = constrainedPanOffset;
        });
        _saveViewState();
      }

      lastFocalPoint = details.localPosition;
    }
  }

  void _handlePanEnd(DragEndDetails details) {
    lastFocalPoint = null;
  }

  // Bounds constraint methods
  Offset _constrainPanOffset(Offset newPanOffset) {
    if (_canvasSize == null) return newPanOffset;

    // Validate input values
    if (!newPanOffset.dx.isFinite || !newPanOffset.dy.isFinite) {
      return Offset.zero;
    }

    // Allow panning across the full canvas area (including behind sidebar)
    // The graph should render behind the sidebar, so we use the full canvas width
    final visibleWidth = _canvasSize!.width;
    final visibleHeight = _canvasSize!.height;

    // Validate canvas size
    if (!visibleWidth.isFinite ||
        !visibleHeight.isFinite ||
        visibleWidth <= 0 ||
        visibleHeight <= 0) {
      return newPanOffset;
    }

    // Calculate the bounds of all nodes to determine content area
    if (nodes.isEmpty) return newPanOffset;

    double minX = double.infinity;
    double maxX = double.negativeInfinity;
    double minY = double.infinity;
    double maxY = double.negativeInfinity;

    for (final node in nodes) {
      minX = math.min(minX, node.position.dx);
      maxX = math.max(maxX, node.position.dx + (node.size?.width ?? 150.0));
      minY = math.min(minY, node.position.dy);
      maxY = math.max(maxY, node.position.dy + (node.size?.height ?? 80.0));
    }

    // Add padding around content
    minX -= _boundsPadding;
    maxX += _boundsPadding;
    minY -= _boundsPadding;
    maxY += _boundsPadding;

    // Validate scale value
    if (!scale.isFinite || scale <= 0) {
      return newPanOffset;
    }

    // Calculate the scaled content dimensions
    final contentWidth = (maxX - minX) * scale;
    final contentHeight = (maxY - minY) * scale;

    // Validate calculated dimensions
    if (!contentWidth.isFinite || !contentHeight.isFinite) {
      return newPanOffset;
    }

    // Constrain pan offset to keep content visible within the canvas
    double constrainedX = newPanOffset.dx;
    double constrainedY = newPanOffset.dy;

    // Horizontal constraints
    if (contentWidth > visibleWidth) {
      // Content is larger than canvas, allow panning within bounds
      final maxPanX = (minX * scale) + _boundsPadding;
      final minPanX = visibleWidth - (maxX * scale) - _boundsPadding;

      // Validate clamp values before using them
      if (minPanX.isFinite && maxPanX.isFinite && minPanX <= maxPanX) {
        constrainedX = constrainedX.clamp(minPanX, maxPanX);
      }
    } else {
      // Content fits in canvas, center it
      final centeredX = (visibleWidth - contentWidth) / 2 - (minX * scale);
      if (centeredX.isFinite) {
        constrainedX = centeredX;
      }
    }

    // Vertical constraints
    if (contentHeight > visibleHeight) {
      // Content is larger than canvas, allow panning within bounds
      final maxPanY = (minY * scale) + _boundsPadding;
      final minPanY = visibleHeight - (maxY * scale) - _boundsPadding;

      // Validate clamp values before using them
      if (minPanY.isFinite && maxPanY.isFinite && minPanY <= maxPanY) {
        constrainedY = constrainedY.clamp(minPanY, maxPanY);
      }
    } else {
      // Content fits in canvas, center it
      final centeredY = (visibleHeight - contentHeight) / 2 - (minY * scale);
      if (centeredY.isFinite) {
        constrainedY = centeredY;
      }
    }

    // Validate the constrained values before creating Offset
    final validX = constrainedX.isFinite ? constrainedX : 0.0;
    final validY = constrainedY.isFinite ? constrainedY : 0.0;

    return Offset(validX, validY);
  }

  Offset _constrainNodePosition(Offset newPosition, Size nodeSize) {
    // Allow nodes to move freely, including behind the sidebar
    // The z-ordering will be handled in the rendering layer
    return newPosition;

    // Old constrained approach (commented out):
    // if (_canvasSize == null) return newPosition;
    //
    // // Calculate the visible graph area (excluding right sidebar)
    // final visibleGraphArea = Rect.fromLTWH(
    //   -panOffset.dx / scale,
    //   -panOffset.dy / scale,
    //   (_canvasSize!.width - _sidebarWidth) / scale,
    //   _canvasSize!.height / scale,
    // );
    //
    // // Add some margin to prevent nodes from being completely hidden
    // final margin = _boundsPadding / scale;
    // final constrainedBounds = visibleGraphArea.deflate(margin);
    //
    // // Constrain the node position to stay within bounds
    // final constrainedX = newPosition.dx.clamp(
    //   constrainedBounds.left,
    //   constrainedBounds.right - nodeSize.width,
    // );
    // final constrainedY = newPosition.dy.clamp(
    //   constrainedBounds.top,
    //   constrainedBounds.bottom - nodeSize.height,
    // );
    //
    // return Offset(constrainedX, constrainedY);
  }

  Offset _screenToGraph(Offset screenPosition) {
    final result = coordinateSystem.screenToGraph(screenPosition);
    print(
      '_screenToGraph: screen=$screenPosition, panOffset=$panOffset, scale=$scale, result=$result',
    );
    return result;
  }

  GraphConnection? _getConnectionAt(Offset position) {
    const hitTestRadius = 10.0; // Tolerance for connection hit testing

    for (final connection in connections) {
      // Get the positions of the connected ports
      final fromNode = nodes
          .where((n) => n.id == connection.fromNodeId)
          .firstOrNull;
      final toNode = nodes
          .where((n) => n.id == connection.toNodeId)
          .firstOrNull;

      if (fromNode == null || toNode == null) continue;

      final fromPort = fromNode.outputs
          .where((p) => p.id == connection.fromPortId)
          .firstOrNull;
      final toPort = toNode.inputs
          .where((p) => p.id == connection.toPortId)
          .firstOrNull;

      if (fromPort == null || toPort == null) continue;

      final fromPosition = fromNode.getPortPosition(fromPort.id);
      final toPosition = toNode.getPortPosition(toPort.id);

      // Test if the point is close enough to the connection line
      if (_isPointNearLine(position, fromPosition, toPosition, hitTestRadius)) {
        return connection;
      }
    }

    return null;
  }

  bool _isPointNearLine(
    Offset point,
    Offset start,
    Offset end,
    double tolerance,
  ) {
    // Calculate distance from point to line segment
    final A = point.dx - start.dx;
    final B = point.dy - start.dy;
    final C = end.dx - start.dx;
    final D = end.dy - start.dy;

    final dot = A * C + B * D;
    final lenSq = C * C + D * D;

    if (lenSq == 0) {
      // Start and end are the same point
      return (point - start).distance <= tolerance;
    }

    final t = (dot / lenSq).clamp(0.0, 1.0);
    final projection = Offset(start.dx + t * C, start.dy + t * D);

    return (point - projection).distance <= tolerance;
  }

  void _deleteNode(String nodeId) {
    setState(() {
      nodes.removeWhere((n) => n.id == nodeId);
      connections.removeWhere(
        (c) => c.fromNodeId == nodeId || c.toNodeId == nodeId,
      );
      selectedNodeId = null;
    });

    widget.onNodeDeleted?.call(nodeId);

    // Auto-save after deleting node
    _autoSaveGraph();
  }

  void _deleteConnection(String connectionId) {
    setState(() {
      connections.removeWhere((c) => c.id == connectionId);
      selectedConnectionId = null;
      showContextMenu = false;
    });

    widget.onConnectionDeleted?.call(connectionId);

    // Auto-save after deleting connection
    _autoSaveGraph();
  }

  void _zoomIn([Offset? zoomCenter]) {
    _animateZoom(scale * 1.15, zoomCenter); // More moderate zoom factor
  }

  void _zoomOut([Offset? zoomCenter]) {
    _animateZoom(scale / 1.15, zoomCenter); // More moderate zoom factor
  }

  void _animateZoom(double targetScale, [Offset? zoomCenter]) {
    final clampedScale = targetScale.clamp(minScale, maxScale);
    if ((clampedScale - scale).abs() < 0.001) return;

    // If zoom center is provided, adjust pan offset to zoom towards that point
    if (zoomCenter != null) {
      final graphPoint = _screenToGraph(zoomCenter);

      _scaleAnimation = Tween<double>(begin: scale, end: clampedScale).animate(
        CurvedAnimation(
          parent: _scaleAnimationController!,
          curve: Curves
              .easeOut, // Changed from easeOutCubic to easeOut for snappier feel
        ),
      );

      final newPanOffset = zoomCenter - (graphPoint * clampedScale);
      _panAnimation = Tween<Offset>(begin: panOffset, end: newPanOffset).animate(
        CurvedAnimation(
          parent: _panAnimationController!,
          curve: Curves
              .easeOut, // Changed from easeOutCubic to easeOut for snappier feel
        ),
      );

      _scaleAnimation!.addListener(() {
        setState(() {
          scale = _scaleAnimation!.value;
        });
      });

      _panAnimation!.addListener(() {
        setState(() {
          panOffset = _panAnimation!.value;
        });
      });

      _scaleAnimationController!.forward(from: 0).then((_) {
        _saveViewState();
      });
      _panAnimationController!.forward(from: 0);
    } else {
      // Simple center zoom
      _scaleAnimation = Tween<double>(begin: scale, end: clampedScale).animate(
        CurvedAnimation(
          parent: _scaleAnimationController!,
          curve: Curves
              .easeOut, // Changed from easeOutCubic to easeOut for snappier feel
        ),
      );

      _scaleAnimation!.addListener(() {
        setState(() {
          scale = _scaleAnimation!.value;
        });
      });

      _scaleAnimationController!.forward(from: 0).then((_) {
        _saveViewState();
      });
    }
  }

  // Enhanced context menu operations
  void _copySelectedNode() {
    if (selectedNodeId != null) {
      final node = nodes.where((n) => n.id == selectedNodeId).firstOrNull;
      if (node != null) {
        _clipboardNode = node;
      }
    }
  }

  void _pasteNode() {
    if (_clipboardNode != null) {
      final newNode = _clipboardNode!.copyWith(
        id: 'node_${DateTime.now().millisecondsSinceEpoch}',
        position: contextMenuPosition + const Offset(20, 20),
      );
      setState(() {
        nodes.add(newNode);
        selectedNodeId = newNode.id;
      });
      widget.onNodeAdded?.call(newNode);
    }
  }

  void _bringNodeToFront() {
    if (selectedNodeId != null) {
      setState(() {
        final nodeIndex = nodes.indexWhere((n) => n.id == selectedNodeId);
        if (nodeIndex != -1) {
          final node = nodes.removeAt(nodeIndex);
          nodes.add(node); // Move to end (front)
        }
      });
    }
  }

  void _sendNodeToBack() {
    if (selectedNodeId != null) {
      setState(() {
        final nodeIndex = nodes.indexWhere((n) => n.id == selectedNodeId);
        if (nodeIndex != -1) {
          final node = nodes.removeAt(nodeIndex);
          nodes.insert(0, node); // Move to beginning (back)
        }
      });
    }
  }

  void _confirmClearAll() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear All'),
        content: const Text(
          'Are you sure you want to delete all nodes and connections?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              clearGraph();
            },
            child: const Text('Clear All', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  void _saveGraph() {
    saveGraphToJsonPretty(
      metadata: {
        'saved_at': DateTime.now().toIso8601String(),
        'version': '1.0',
      },
    );

    // For now, just copy to clipboard or print
    // In a real app, you'd show a file dialog
    debugPrint('Graph JSON saved');

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Graph saved to console (check debug output)'),
      ),
    );
  }

  void _loadGraph() {
    // For now, just show a placeholder dialog
    // In a real app, you'd show a file picker
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Load Graph'),
        content: const Text(
          'Graph loading from file not implemented yet.\nFor now, use the loadGraphFromJson() method programmatically.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }
}
