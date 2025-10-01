import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../services/websocket_service.dart';



class GraphPersistenceService with ChangeNotifier {
  static GraphPersistenceService? _instance;
  static GraphPersistenceService get instance =>
      _instance ??= GraphPersistenceService._();

  GraphPersistenceService._() {
    _initializeMessageListener();
  }

  final WebSocketService _webSocketService = WebSocketService.instance;
  late StreamSubscription _messageSubscription;


  Timer? _autoSaveTimer;
  bool _hasUnsavedChanges = false;
  String? _currentGraphId;
  String? _currentWorkspaceId;


  final Map<String, Map<String, dynamic>> _graphDataCache = {};
  final Map<String, DateTime> _lastSaveTime = {};

  static const Duration autoSaveInterval = Duration(seconds: 30);
  static const Duration debounceDelay = Duration(seconds: 2);

  void _initializeMessageListener() {
    _messageSubscription = _webSocketService.messages.listen((message) {
      _handleWebSocketMessage(message);
    });
  }

  void _handleWebSocketMessage(WebSocketMessage message) {
    switch (message.type) {
      case 'graph_save_response':
        _handleGraphSaveResponse(message);
        break;
      case 'graph_load_response':
        _handleGraphLoadResponse(message);
        break;
      case 'graph_updated':
        _handleGraphUpdated(message);
        break;
      case 'node_updated':
        _handleNodeUpdated(message);
        break;
      case 'connection_updated':
        _handleConnectionUpdated(message);
        break;
      case 'graph_update_broadcast':
        _handleGraphUpdateBroadcast(message);
        break;
    }
  }

  void _handleGraphSaveResponse(WebSocketMessage message) {
    final success = message.data['success'] as bool? ?? false;
    final graphId = message.data['graphId'] as String?;

    if (success && graphId != null) {
      _lastSaveTime[graphId] = DateTime.now();
      if (graphId == _currentGraphId) {
        _hasUnsavedChanges = false;
      }
      debugPrint('Graph $graphId saved successfully');
    } else {
      debugPrint('Failed to save graph $graphId');
    }
  }

  void _handleGraphLoadResponse(WebSocketMessage message) {
    final success = message.data['success'] as bool? ?? false;
    final graphId = message.data['graphId'] as String?;
    final error = message.data['error'] as String?;

    if (success && graphId != null) {
      final graphData = message.data['graphData'];
      if (graphData != null) {
        try {
          _graphDataCache[graphId] = Map<String, dynamic>.from(graphData);
          debugPrint(
            'Graph $graphId loaded successfully with ${graphData['nodes']?.length ?? 0} nodes',
          );


          notifyListeners();
        } catch (e) {
          debugPrint('Error caching loaded graph data: $e');
        }
      }
    } else {
      debugPrint('Graph load failed for $graphId, error: $error');


      if (error == 'Graph not found' && graphId != null) {
        debugPrint('Attempting to create missing graph: $graphId');


        String workspaceId = 'default_workspace';
        if (graphId.contains('-')) {
          final parts = graphId.split('-');
          if (parts.length > 1 && parts[0].startsWith('workspace_')) {
            workspaceId = parts[0];
          }
        }


        createGraph(graphId, workspaceId: workspaceId);
      }
    }
  }

  void _handleGraphUpdated(WebSocketMessage message) {
    final graphId = message.data['graphId'] as String?;
    final updatedBy = message.data['updatedBy'] as String?;

    if (graphId != null && graphId == _currentGraphId) {
      debugPrint('Graph $graphId was updated by $updatedBy - refreshing');
      loadGraph(graphId);
    }
  }

  void _handleNodeUpdated(WebSocketMessage message) {
    final graphId = message.data['graphId'] as String?;
    final nodeId = message.data['nodeId'] as String?;
    final nodeData = message.data['nodeData'] as Map<String, dynamic>?;

    if (graphId != null && graphId == _currentGraphId && nodeId != null && nodeData != null) {
      debugPrint('Node $nodeId updated in graph $graphId');


      final cachedData = _graphDataCache[graphId];
      if (cachedData != null) {
        final nodes = List<Map<String, dynamic>>.from(cachedData['nodes'] ?? []);
        final nodeIndex = nodes.indexWhere((node) => node['id'] == nodeId);

        if (nodeIndex != -1) {

          nodes[nodeIndex] = Map<String, dynamic>.from(nodeData);
        } else {

          nodes.add(Map<String, dynamic>.from(nodeData));
        }

        cachedData['nodes'] = nodes;
        _graphDataCache[graphId] = cachedData;


        notifyListeners();
      }
    }
  }

  void _handleConnectionUpdated(WebSocketMessage message) {
    final graphId = message.data['graphId'] as String?;
    final connectionId = message.data['connectionId'] as String?;
    final connectionData = message.data['connectionData'] as Map<String, dynamic>?;

    if (graphId != null && graphId == _currentGraphId && connectionId != null && connectionData != null) {
      debugPrint('Connection $connectionId updated in graph $graphId');


      final cachedData = _graphDataCache[graphId];
      if (cachedData != null) {
        final connections = List<Map<String, dynamic>>.from(cachedData['connections'] ?? []);
        final connectionIndex = connections.indexWhere((conn) => conn['id'] == connectionId);

        if (connectionIndex != -1) {

          connections[connectionIndex] = Map<String, dynamic>.from(connectionData);
        } else {

          connections.add(Map<String, dynamic>.from(connectionData));
        }

        cachedData['connections'] = connections;
        _graphDataCache[graphId] = cachedData;


        notifyListeners();
      }
    }
  }

  void _handleGraphUpdateBroadcast(WebSocketMessage message) {
    final graphId = message.data['graphId'] as String?;
    final updateType = message.data['updateType'] as String?;
    final nodeData = message.data['node'] as Map<String, dynamic>?;
    final connectionData = message.data['connection'] as Map<String, dynamic>?;

    if (graphId != null && graphId == _currentGraphId) {
      debugPrint('Graph update broadcast received for $graphId: $updateType');


      final cachedData = _graphDataCache[graphId];
      if (cachedData != null) {
        bool updated = false;

        if (nodeData != null) {
          final nodes = List<Map<String, dynamic>>.from(cachedData['nodes'] ?? []);
          final nodeId = nodeData['id'] as String?;

          if (nodeId != null) {
            final nodeIndex = nodes.indexWhere((node) => node['id'] == nodeId);

            if (updateType == 'node_add' || nodeIndex == -1) {

              nodes.add(Map<String, dynamic>.from(nodeData));
              updated = true;
            } else if (updateType == 'node_update') {

              nodes[nodeIndex] = Map<String, dynamic>.from(nodeData);
              updated = true;
            }

            cachedData['nodes'] = nodes;
          }
        }

        if (connectionData != null) {
          final connections = List<Map<String, dynamic>>.from(cachedData['connections'] ?? []);
          final connectionId = connectionData['id'] as String?;

          if (connectionId != null) {
            final connectionIndex = connections.indexWhere((conn) => conn['id'] == connectionId);

            if (updateType == 'connection_add' || connectionIndex == -1) {

              connections.add(Map<String, dynamic>.from(connectionData));
              updated = true;
            } else if (updateType == 'connection_update') {

              connections[connectionIndex] = Map<String, dynamic>.from(connectionData);
              updated = true;
            }

            cachedData['connections'] = connections;
          }
        }

        if (updated) {
          _graphDataCache[graphId] = cachedData;

          notifyListeners();
        }
      }
    }
  }


  void setCurrentGraph(String graphId, String workspaceId) {

    if (_hasUnsavedChanges && _currentGraphId != null) {
      saveCurrentGraph();
    }


    String formattedGraphId = graphId;
    if (!graphId.contains('-') &&
        graphId != 'default' &&
        workspaceId.startsWith('workspace_')) {
      formattedGraphId = '$workspaceId-$graphId';
      debugPrint('Formatting graph ID with workspace: $formattedGraphId');
    }

    _currentGraphId = formattedGraphId;
    _currentWorkspaceId = workspaceId;
    _hasUnsavedChanges = false;


    _startAutoSaveTimer();
  }


  void markGraphAsModified() {
    _hasUnsavedChanges = true;
    _restartDebounceTimer();
  }


  void updateNodePosition(String nodeId, Offset position) {
    markGraphAsModified();

    _sendNodeMoveUpdate(nodeId, position);
  }


  void updateNodeData(String nodeId, Map<String, dynamic> data) {
    markGraphAsModified();

    _sendNodeUpdate(nodeId, data);
  }


  Future<void> saveCurrentGraph({Map<String, dynamic>? graphData}) async {
    if (_currentGraphId == null || _currentWorkspaceId == null) {
      return;
    }


    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();
      return;
    }

    try {

      final dataToSend = graphData ?? _graphDataCache[_currentGraphId] ?? {};


      if (!dataToSend.containsKey('nodes')) {
        dataToSend['nodes'] = [];
      }
      if (!dataToSend.containsKey('connections')) {
        dataToSend['connections'] = [];
      }

      final message = WebSocketMessage(
        type: 'graph_save',
        data: {
          'graphId': _currentGraphId!,
          'workspaceId': _currentWorkspaceId!,
          'name': 'Graph ${_currentGraphId}',
          'version': '1.0.0',
          'graphData': dataToSend,
        },
      );

      _webSocketService.send(message);
      debugPrint('Saving graph: $_currentGraphId');
    } catch (e) {
      debugPrint('Error saving graph: $e');
    }
  }


  Future<void> loadGraph(String graphId) async {

    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();
      return;
    }

    try {

      String finalGraphId = graphId;
      String? workspaceId;

      if (_currentWorkspaceId != null &&
          !graphId.contains('-') &&
          graphId != 'default') {
        finalGraphId = '$_currentWorkspaceId-$graphId';
        workspaceId = _currentWorkspaceId;
        debugPrint('Constructing full graph ID: $finalGraphId');
      } else if (graphId.contains('-')) {

        final parts = graphId.split('-');
        if (parts.length > 1 && parts[0].startsWith('workspace_')) {
          workspaceId = parts[0];
        }
      }

      final message = WebSocketMessage(
        type: 'graph_load',
        data: {
          'graphId': finalGraphId,
          'createIfNotExists':
              true,
          'workspaceId': workspaceId,
        },
      );

      _webSocketService.send(message);
      debugPrint('Loading graph: $finalGraphId');
    } catch (e) {
      debugPrint('Error loading graph: $e');
    }
  }


  Future<void> createGraph(String graphId, {String? workspaceId}) async {

    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();


      final wsId = workspaceId ?? _currentWorkspaceId ?? 'default_workspace';
      String finalGraphId = graphId;
      if (!graphId.contains('-') && graphId != 'default') {
        finalGraphId = '$wsId-$graphId';
      }

      // Create a default empty graph structure to cache
      final defaultGraphData = {
        'nodes': [],
        'connections': [],
        'metadata': {
          'created': DateTime.now().toIso8601String(),
          'version': '1.0',
          'description': 'Auto-created graph (cached while offline)',
        },
      };

      // Store in cache even while offline
      _graphDataCache[finalGraphId] = defaultGraphData;

      // Use the send method which will queue if not connected
      final message = WebSocketMessage(
        type: 'graph_save',
        data: {
          'graphId': finalGraphId,
          'workspaceId': wsId,
          'name': 'Graph $graphId',
          'version': '1.0.0',
          'graphData': defaultGraphData,
          'createIfNotExists': true,
        },
      );
      _webSocketService.send(message);

      debugPrint(
        'Queued graph creation for when connection is restored: $graphId',
      );
      return;
    }

    final wsId = workspaceId ?? _currentWorkspaceId ?? 'default_workspace';

    // Check if we need to construct a proper graph ID
    String finalGraphId = graphId;
    if (!graphId.contains('-') && graphId != 'default') {
      finalGraphId = '$wsId-$graphId';
      debugPrint('Constructing full graph ID for creation: $finalGraphId');
    }

    try {
      // Create a default empty graph structure
      final defaultGraphData = {
        'nodes': [],
        'connections': [],
        'metadata': {
          'created': DateTime.now().toIso8601String(),
          'version': '1.0',
          'description': 'Auto-created graph',
        },
      };

      final message = WebSocketMessage(
        type: 'graph_save',
        data: {
          'graphId': finalGraphId,
          'workspaceId': wsId,
          'name': 'Graph $graphId',
          'version': '1.0.0',
          'graphData': defaultGraphData,
          'createIfNotExists': true, // Tell server to create if missing
        },
      );

      _webSocketService.send(message);
      debugPrint('Creating new graph: $finalGraphId');

      // Store this data in our cache immediately
      _graphDataCache[finalGraphId] = defaultGraphData;

      // Set this as the current graph if we don't have one
      if (_currentGraphId == null) {
        _currentGraphId = finalGraphId;
        _currentWorkspaceId = wsId;
      }
    } catch (e) {
      debugPrint('Error creating graph: $e');
    }
  }

  /// Get cached graph data
  Map<String, dynamic>? getCachedGraph(String graphId) {
    return _graphDataCache[graphId];
  }

  /// Check if graph has unsaved changes
  bool hasUnsavedChanges() {
    return _hasUnsavedChanges;
  }

  /// Get time of last save
  DateTime? getLastSaveTime(String graphId) {
    return _lastSaveTime[graphId];
  }

  void _sendNodeMoveUpdate(String nodeId, Offset position) {
    // Attempt to reconnect if not connected
    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      _webSocketService.reconnect();
      return;
    }

    final message = WebSocketMessage(
      type: 'node_update',
      data: {
        'graphId': _currentGraphId,
        'nodeId': nodeId,
        'nodeData': {
          'id': nodeId,
          'position': {'x': position.dx, 'y': position.dy},
        },
      },
    );
    _webSocketService.send(message);
  }

  void _sendNodeUpdate(String nodeId, Map<String, dynamic> data) {
    // Attempt to reconnect if not connected
    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      _webSocketService.reconnect();
      return;
    }

    final message = WebSocketMessage(
      type: 'node_update',
      data: {'graphId': _currentGraphId, 'nodeId': nodeId, 'data': data},
    );
    _webSocketService.send(message);
  }

  void _startAutoSaveTimer() {
    _autoSaveTimer?.cancel();
    _autoSaveTimer = Timer.periodic(autoSaveInterval, (timer) {
      if (_hasUnsavedChanges) {
        debugPrint('Auto-saving graph due to timer');
        saveCurrentGraph();
      }
    });
  }

  void _restartDebounceTimer() {
    _autoSaveTimer?.cancel();
    _autoSaveTimer = Timer(debounceDelay, () {
      if (_hasUnsavedChanges) {
        debugPrint('Auto-saving graph due to debounce');
        saveCurrentGraph();
      }
      _startAutoSaveTimer();
    });
  }

  /// Force save current graph
  void forceSave({Map<String, dynamic>? graphData}) {
    if (_hasUnsavedChanges) {
      saveCurrentGraph(graphData: graphData);
    }
  }

  /// Update current graph data cache
  void updateGraphDataCache(String graphId, Map<String, dynamic> graphData) {
    _graphDataCache[graphId] = graphData;
    if (graphId == _currentGraphId) {
      markGraphAsModified();
    }
  }

  @override
  void dispose() {
    _autoSaveTimer?.cancel();
    _messageSubscription.cancel();
    super.dispose();
  }
}
