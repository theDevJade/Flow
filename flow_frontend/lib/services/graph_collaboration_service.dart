import 'dart:async';
import 'package:flutter/material.dart';
import '../services/websocket_service.dart';
import '../graph_editor/models.dart';

class GraphCollaborationService {
  final WebSocketService _webSocketService;
  final StreamController<CollaborationEvent> _eventController =
      StreamController<CollaborationEvent>.broadcast();

  late StreamSubscription _messageSubscription;

  String? _currentUserId;
  String? _currentGraphId;

  GraphCollaborationService(this._webSocketService) {
    _initializeMessageListener();
  }

  Stream<CollaborationEvent> get eventStream => _eventController.stream;

  String? get currentUserId => _currentUserId;
  String? get currentGraphId => _currentGraphId;

  void _initializeMessageListener() {
    _messageSubscription = _webSocketService.messages.listen((message) {
      _handleWebSocketMessage(message);
    });
  }

  void _handleWebSocketMessage(WebSocketMessage message) {
    switch (message.type) {
      case 'auth_response':
        if (message.data['success'] == true) {
          _currentUserId = message.data['userId']?.toString();
        }
        break;

      case 'user_joined':
        _eventController.add(
          CollaborationEvent.userJoined(
            userId: message.data['userId']?.toString() ?? '',
          ),
        );
        break;

      case 'user_left':
        _eventController.add(
          CollaborationEvent.userLeft(
            userId: message.data['userId']?.toString() ?? '',
          ),
        );
        break;

      case 'node_added':
        _handleNodeAdded(message);
        break;

      case 'node_updated':
        _handleNodeUpdated(message);
        break;

      case 'node_deleted':
        _handleNodeDeleted(message);
        break;

      case 'connection_added':
        _handleConnectionAdded(message);
        break;

      case 'connection_deleted':
        _handleConnectionDeleted(message);
        break;

      case 'user_cursor_update':
        _handleUserCursor(message);
        break;

      case 'user_selection_update':
        _handleUserSelection(message);
        break;

      case 'graph_sync_update':
        _handleGraphSync(message);
        break;

      case 'graph_load_response':
        _handleGraphLoadResponse(message);
        break;
    }
  }

  void _handleNodeAdded(WebSocketMessage message) {
    try {
      final nodeData = message.data['node'];
      if (nodeData != null) {
        final node = GraphNode.fromJson(Map<String, dynamic>.from(nodeData));
        _eventController.add(
          CollaborationEvent.nodeAdded(
            node: node,
            userId: message.userId ?? '',
          ),
        );
      }
    } catch (e) {
      print('Error parsing node added message: $e');
    }
  }

  void _handleNodeUpdated(WebSocketMessage message) {
    try {
      final nodeData = message.data['node'];
      if (nodeData != null) {
        final node = GraphNode.fromJson(Map<String, dynamic>.from(nodeData));
        _eventController.add(
          CollaborationEvent.nodeUpdated(
            node: node,
            userId: message.userId ?? '',
          ),
        );
      }
    } catch (e) {
      print('Error parsing node updated message: $e');
    }
  }

  void _handleNodeDeleted(WebSocketMessage message) {
    final nodeId = message.data['nodeId']?.toString();
    if (nodeId != null) {
      _eventController.add(
        CollaborationEvent.nodeDeleted(
          nodeId: nodeId,
          userId: message.userId ?? '',
        ),
      );
    }
  }

  void _handleConnectionAdded(WebSocketMessage message) {
    try {
      final connectionData = message.data['connection'];
      if (connectionData != null) {
        final connection = GraphConnection.fromJson(
          Map<String, dynamic>.from(connectionData),
        );
        _eventController.add(
          CollaborationEvent.connectionAdded(
            connection: connection,
            userId: message.userId ?? '',
          ),
        );
      }
    } catch (e) {
      print('Error parsing connection added message: $e');
    }
  }

  void _handleConnectionDeleted(WebSocketMessage message) {
    final connectionId = message.data['connectionId']?.toString();
    if (connectionId != null) {
      _eventController.add(
        CollaborationEvent.connectionDeleted(
          connectionId: connectionId,
          userId: message.userId ?? '',
        ),
      );
    }
  }

  void _handleUserCursor(WebSocketMessage message) {
    final x = message.data['x']?.toDouble();
    final y = message.data['y']?.toDouble();
    final userId = message.userId;

    if (x != null && y != null && userId != null) {
      _eventController.add(
        CollaborationEvent.userCursor(userId: userId, position: Offset(x, y)),
      );
    }
  }

  void _handleUserSelection(WebSocketMessage message) {
    final userId = message.userId;
    if (userId != null) {
      final selectedNodes = (message.data['selectedNodes'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          [];
      final selectedConnections =
          (message.data['selectedConnections'] as List<dynamic>?)
                  ?.map((e) => e.toString())
                  .toList() ??
              [];

      _eventController.add(
        CollaborationEvent.userSelection(
          userId: userId,
          selectedNodes: selectedNodes,
          selectedConnections: selectedConnections,
        ),
      );
    }
  }

  void _handleGraphSync(WebSocketMessage message) {

    _eventController.add(
      CollaborationEvent.graphSync(
        data: message.data,
        userId: message.userId ?? '',
      ),
    );
  }

  void _handleGraphLoadResponse(WebSocketMessage message) {
    if (message.data['success'] == true) {
      final graphData = message.data['graphData'];
      if (graphData != null) {
        _eventController.add(
          CollaborationEvent.graphLoaded(
            graphData: Map<String, dynamic>.from(graphData),
          ),
        );
      }
    }
  }


  void addNode(GraphNode node) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'node_add',
        data: {
          'graphId': _currentGraphId ?? 'default',
          'nodeId': node.id,
          'nodeData': node.toJson(),
        },
      ),
    );
  }

  void updateNode(GraphNode node) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'node_update',
        data: {
          'graphId': _currentGraphId ?? 'default',
          'nodeId': node.id,
          'nodeData': node.toJson(),
        },
      ),
    );
  }

  void deleteNode(String nodeId) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'node_delete',
        data: {'graphId': _currentGraphId ?? 'default', 'nodeId': nodeId},
      ),
    );
  }

  void addConnection(GraphConnection connection) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'connection_add',
        data: {
          'graphId': _currentGraphId ?? 'default',
          'connectionId': connection.id,
          'connectionData': connection.toJson(),
        },
      ),
    );
  }

  void deleteConnection(String connectionId) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'connection_delete',
        data: {
          'graphId': _currentGraphId ?? 'default',
          'connectionId': connectionId,
        },
      ),
    );
  }

  void updateCursor(Offset position) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'user_cursor',
        data: {
          'x': position.dx,
          'y': position.dy,
          'graphId': _currentGraphId ?? 'default',
        },
      ),
    );
  }

  void updateSelection(
    List<String> selectedNodes,
    List<String> selectedConnections,
  ) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'user_selection',
        data: {
          'selectedNodes': selectedNodes,
          'selectedConnections': selectedConnections,
          'graphId': _currentGraphId ?? 'default',
        },
      ),
    );
  }

  void saveGraph(String graphId, Map<String, dynamic> graphData) {
    _currentGraphId = graphId;
    _webSocketService.send(
      WebSocketMessage(
        type: 'graph_save',
        data: {'graphId': graphId, 'graphData': graphData},
      ),
    );
  }

  void loadGraph(String graphId) {
    _currentGraphId = graphId;
    _webSocketService.send(
      WebSocketMessage(type: 'graph_load', data: {'graphId': graphId}),
    );
  }

  void syncGraph(Map<String, dynamic> changes) {
    _webSocketService.send(
      WebSocketMessage(
        type: 'graph_update',
        data: {
          'graphId': _currentGraphId ?? 'default',
          'updateType': 'full_sync',
          'changes': changes,
        },
      ),
    );
  }

  void dispose() {
    _messageSubscription.cancel();
    _eventController.close();
  }
}


abstract class CollaborationEvent {
  const CollaborationEvent();

  factory CollaborationEvent.userJoined({required String userId}) =
      UserJoinedEvent;
  factory CollaborationEvent.userLeft({required String userId}) = UserLeftEvent;
  factory CollaborationEvent.nodeAdded({
    required GraphNode node,
    required String userId,
  }) = NodeAddedEvent;
  factory CollaborationEvent.nodeUpdated({
    required GraphNode node,
    required String userId,
  }) = NodeUpdatedEvent;
  factory CollaborationEvent.nodeDeleted({
    required String nodeId,
    required String userId,
  }) = NodeDeletedEvent;
  factory CollaborationEvent.connectionAdded({
    required GraphConnection connection,
    required String userId,
  }) = ConnectionAddedEvent;
  factory CollaborationEvent.connectionDeleted({
    required String connectionId,
    required String userId,
  }) = ConnectionDeletedEvent;
  factory CollaborationEvent.userCursor({
    required String userId,
    required Offset position,
  }) = UserCursorEvent;
  factory CollaborationEvent.userSelection({
    required String userId,
    required List<String> selectedNodes,
    required List<String> selectedConnections,
  }) = UserSelectionEvent;
  factory CollaborationEvent.graphSync({
    required Map<String, dynamic> data,
    required String userId,
  }) = GraphSyncEvent;
  factory CollaborationEvent.graphLoaded({
    required Map<String, dynamic> graphData,
  }) = GraphLoadedEvent;
}

class UserJoinedEvent extends CollaborationEvent {
  final String userId;
  const UserJoinedEvent({required this.userId});
}

class UserLeftEvent extends CollaborationEvent {
  final String userId;
  const UserLeftEvent({required this.userId});
}

class NodeAddedEvent extends CollaborationEvent {
  final GraphNode node;
  final String userId;
  const NodeAddedEvent({required this.node, required this.userId});
}

class NodeUpdatedEvent extends CollaborationEvent {
  final GraphNode node;
  final String userId;
  const NodeUpdatedEvent({required this.node, required this.userId});
}

class NodeDeletedEvent extends CollaborationEvent {
  final String nodeId;
  final String userId;
  const NodeDeletedEvent({required this.nodeId, required this.userId});
}

class ConnectionAddedEvent extends CollaborationEvent {
  final GraphConnection connection;
  final String userId;
  const ConnectionAddedEvent({required this.connection, required this.userId});
}

class ConnectionDeletedEvent extends CollaborationEvent {
  final String connectionId;
  final String userId;
  const ConnectionDeletedEvent({
    required this.connectionId,
    required this.userId,
  });
}

class UserCursorEvent extends CollaborationEvent {
  final String userId;
  final Offset position;
  const UserCursorEvent({required this.userId, required this.position});
}

class UserSelectionEvent extends CollaborationEvent {
  final String userId;
  final List<String> selectedNodes;
  final List<String> selectedConnections;
  const UserSelectionEvent({
    required this.userId,
    required this.selectedNodes,
    required this.selectedConnections,
  });
}

class GraphSyncEvent extends CollaborationEvent {
  final Map<String, dynamic> data;
  final String userId;
  const GraphSyncEvent({required this.data, required this.userId});
}

class GraphLoadedEvent extends CollaborationEvent {
  final Map<String, dynamic> graphData;
  const GraphLoadedEvent({required this.graphData});
}
