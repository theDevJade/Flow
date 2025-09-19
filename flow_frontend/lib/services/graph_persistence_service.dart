import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../services/websocket_service.dart';

/// Service responsible for persisting graph state including position, status,
/// and node data when moving between tabs or making changes
class GraphPersistenceService with ChangeNotifier {
  static GraphPersistenceService? _instance;
  static GraphPersistenceService get instance =>
      _instance ??= GraphPersistenceService._();

  GraphPersistenceService._() {
    _initializeMessageListener();
  }

  final WebSocketService _webSocketService = WebSocketService.instance;
  late StreamSubscription _messageSubscription;

  // Auto-save functionality
  Timer? _autoSaveTimer;
  bool _hasUnsavedChanges = false;
  String? _currentGraphId;
  String? _currentWorkspaceId;

  // State tracking
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

          // Notify listeners that new graph data is available
          notifyListeners();
        } catch (e) {
          debugPrint('Error caching loaded graph data: $e');
        }
      }
    } else {
      debugPrint('Graph load failed for $graphId, error: $error');

      // If graph not found, attempt to create it automatically
      if (error == 'Graph not found' && graphId != null) {
        debugPrint('Attempting to create missing graph: $graphId');

        // Extract workspaceId from graphId if possible
        String workspaceId = 'default_workspace';
        if (graphId.contains('-')) {
          final parts = graphId.split('-');
          if (parts.length > 1 && parts[0].startsWith('workspace_')) {
            workspaceId = parts[0];
          }
        }

        // Create a new empty graph
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

  /// Set the current active graph
  void setCurrentGraph(String graphId, String workspaceId) {
    // Save current graph if there are unsaved changes
    if (_hasUnsavedChanges && _currentGraphId != null) {
      saveCurrentGraph();
    }

    // Ensure proper formatting of graph ID with workspace prefix if needed
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

    // Start auto-save timer
    _startAutoSaveTimer();
  }

  /// Mark that the graph has unsaved changes
  void markGraphAsModified() {
    _hasUnsavedChanges = true;
    _restartDebounceTimer();
  }

  /// Update node position and mark as modified
  void updateNodePosition(String nodeId, Offset position) {
    markGraphAsModified();
    // Send real-time update to other clients
    _sendNodeMoveUpdate(nodeId, position);
  }

  /// Update node data and mark as modified
  void updateNodeData(String nodeId, Map<String, dynamic> data) {
    markGraphAsModified();
    // Send real-time update to other clients
    _sendNodeUpdate(nodeId, data);
  }

  /// Save the current graph
  Future<void> saveCurrentGraph({Map<String, dynamic>? graphData}) async {
    if (_currentGraphId == null || _currentWorkspaceId == null) {
      return;
    }

    // Attempt to reconnect if not connected
    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();
      return;
    }

    try {
      // Ensure graphData has the required nodes and connections fields
      final dataToSend = graphData ?? _graphDataCache[_currentGraphId] ?? {};

      // Make sure we have nodes and connections arrays (even if empty)
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

  /// Load a graph by ID
  Future<void> loadGraph(String graphId) async {
    // Attempt to reconnect if not connected
    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();
      return;
    }

    try {
      // Check if we need to construct a proper graph ID
      String finalGraphId = graphId;
      String? workspaceId;

      if (_currentWorkspaceId != null &&
          !graphId.contains('-') &&
          graphId != 'default') {
        finalGraphId = '$_currentWorkspaceId-$graphId';
        workspaceId = _currentWorkspaceId;
        debugPrint('Constructing full graph ID: $finalGraphId');
      } else if (graphId.contains('-')) {
        // Extract workspace ID from graph ID
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
              true, // Tell server to create if it doesn't exist
          'workspaceId': workspaceId,
        },
      );

      _webSocketService.send(message);
      debugPrint('Loading graph: $finalGraphId');
    } catch (e) {
      debugPrint('Error loading graph: $e');
    }
  }

  /// Create a new graph with default data
  Future<void> createGraph(String graphId, {String? workspaceId}) async {
    // Attempt to reconnect if not connected
    if (_webSocketService.currentStatus !=
        WebSocketConnectionStatus.connected) {
      debugPrint('WebSocket not connected, attempting to reconnect...');
      _webSocketService.reconnect();

      // Save this for later using custom send method which will queue
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
