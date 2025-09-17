import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../services/websocket_service.dart';

class Workspace {
  final String id;
  final String name;
  final String? currentPage;
  final Map<String, dynamic> data;
  final Map<String, dynamic> settings;
  final DateTime createdAt;
  final DateTime updatedAt;

  Workspace({
    required this.id,
    required this.name,
    this.currentPage,
    required this.data,
    required this.settings,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Workspace.fromJson(Map<String, dynamic> json) {
    debugPrint('WorkspaceManager: Parsing workspace JSON: $json');

    // Parse data field - it might be a JSON string or already a Map
    Map<String, dynamic> parsedData = {};
    final dataField = json['data'];
    debugPrint(
      'WorkspaceManager: Data field type: ${dataField.runtimeType}, value: $dataField',
    );

    if (dataField is String) {
      try {
        parsedData = Map<String, dynamic>.from(jsonDecode(dataField));
      } catch (e) {
        debugPrint('WorkspaceManager: Failed to parse data JSON string: $e');
      }
    } else if (dataField is Map) {
      try {
        parsedData = Map<String, dynamic>.from(dataField);
      } catch (e) {
        debugPrint('WorkspaceManager: Failed to convert data Map: $e');
      }
    } else if (dataField != null) {
      debugPrint(
        'WorkspaceManager: Unexpected data field type: ${dataField.runtimeType}',
      );
    }

    // Parse settings field - it might be a JSON string or already a Map
    Map<String, dynamic> parsedSettings = {};
    final settingsField = json['settings'];
    debugPrint(
      'WorkspaceManager: Settings field type: ${settingsField.runtimeType}, value: $settingsField',
    );

    if (settingsField is String) {
      try {
        parsedSettings = Map<String, dynamic>.from(jsonDecode(settingsField));
      } catch (e) {
        debugPrint(
          'WorkspaceManager: Failed to parse settings JSON string: $e',
        );
      }
    } else if (settingsField is Map) {
      try {
        parsedSettings = Map<String, dynamic>.from(settingsField);
      } catch (e) {
        debugPrint('WorkspaceManager: Failed to convert settings Map: $e');
      }
    } else if (settingsField != null) {
      debugPrint(
        'WorkspaceManager: Unexpected settings field type: ${settingsField.runtimeType}',
      );
    }

    return Workspace(
      id: json['workspaceId'] ?? json['id'] ?? '',
      name: json['name'] ?? 'Untitled Workspace',
      currentPage: json['currentPage'],
      data: parsedData,
      settings: parsedSettings,
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
      updatedAt: DateTime.tryParse(json['updatedAt'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'workspaceId': id,
      'name': name,
      'currentPage': currentPage,
      'data': data,
      'settings': settings,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  Workspace copyWith({
    String? id,
    String? name,
    String? currentPage,
    Map<String, dynamic>? data,
    Map<String, dynamic>? settings,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return Workspace(
      id: id ?? this.id,
      name: name ?? this.name,
      currentPage: currentPage ?? this.currentPage,
      data: data ?? this.data,
      settings: settings ?? this.settings,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

class WorkspaceManager with ChangeNotifier {
  final WebSocketService _webSocketService = WebSocketService.instance;

  List<Workspace> _workspaces = [];
  String? _currentWorkspaceId;
  bool _isLoading = false;
  String? _error;

  List<Workspace> get workspaces => List.unmodifiable(_workspaces);
  String? get currentWorkspaceId => _currentWorkspaceId;
  Workspace? get currentWorkspace {
    if (_currentWorkspaceId != null) {
      try {
        return _workspaces.firstWhere((w) => w.id == _currentWorkspaceId);
      } catch (e) {
        return _workspaces.isNotEmpty ? _workspaces.first : null;
      }
    }
    return _workspaces.isNotEmpty ? _workspaces.first : null;
  }

  bool get isLoading => _isLoading;
  String? get error => _error;

  WorkspaceManager() {
    _webSocketService.messages.listen(_handleWebSocketMessage);
  }

  void _handleWebSocketMessage(WebSocketMessage message) {
    switch (message.type) {
      case 'workspace_list_response':
        _handleWorkspaceListResponse(message.data);
        break;
      case 'workspace_created':
        _handleWorkspaceCreated(message.data);
        break;
      case 'workspace_updated':
        _handleWorkspaceUpdated(message.data);
        break;
      case 'workspace_deleted':
        _handleWorkspaceDeleted(message.data);
        break;
      case 'error':
        if (message.data['request_type'] == 'workspace_list' ||
            message.data['request_type'] == 'create_workspace' ||
            message.data['request_type'] == 'update_workspace' ||
            message.data['request_type'] == 'delete_workspace') {
          _error = message.data['message'] ?? 'Unknown error';
          _isLoading = false;
          notifyListeners();
        }
        break;
    }
  }

  void _handleWorkspaceListResponse(Map<String, dynamic> data) {
    if (data['success'] == true) {
      final workspacesList = data['workspaces'] as List<dynamic>? ?? [];
      _workspaces = workspacesList
          .map((w) => Workspace.fromJson(w as Map<String, dynamic>))
          .toList();

      // Set current workspace if not set
      if (_currentWorkspaceId == null && _workspaces.isNotEmpty) {
        _currentWorkspaceId = _workspaces.first.id;
      }

      _isLoading = false;
      _error = null;
      notifyListeners();
    } else {
      _error = data['error'] ?? 'Failed to load workspaces';
      _isLoading = false;
      notifyListeners();
    }
  }

  void _handleWorkspaceCreated(Map<String, dynamic> data) {
    if (data['success'] == true) {
      final workspace = Workspace.fromJson(data['workspace']);
      _workspaces.add(workspace);
      _currentWorkspaceId = workspace.id;
      _isLoading = false;
      _error = null;
      notifyListeners();
    } else {
      _error = data['error'] ?? 'Failed to create workspace';
      _isLoading = false;
      notifyListeners();
    }
  }

  void _handleWorkspaceUpdated(Map<String, dynamic> data) {
    if (data['success'] == true) {
      final workspace = Workspace.fromJson(data['workspace']);
      final index = _workspaces.indexWhere((w) => w.id == workspace.id);
      if (index != -1) {
        _workspaces[index] = workspace;
        notifyListeners();
      }
    }
  }

  void _handleWorkspaceDeleted(Map<String, dynamic> data) {
    if (data['success'] == true) {
      final workspaceId = data['workspaceId'] as String?;
      if (workspaceId != null) {
        _workspaces.removeWhere((w) => w.id == workspaceId);
        if (_currentWorkspaceId == workspaceId) {
          _currentWorkspaceId =
              _workspaces.isNotEmpty ? _workspaces.first.id : null;
        }
        notifyListeners();
      }
    }
  }

  Future<void> loadWorkspaces() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    _webSocketService.sendMessage('workspace_list', {
      'request_type': 'workspace_list',
    });
  }

  Future<void> createWorkspace(String name) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    _webSocketService.sendMessage('create_workspace', {
      'request_type': 'create_workspace',
      'name': name,
      'data': {},
      'settings': {},
    });
  }

  Future<void> updateWorkspace(
    String workspaceId, {
    String? name,
    Map<String, dynamic>? data,
    Map<String, dynamic>? settings,
  }) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    _webSocketService.sendMessage('update_workspace', {
      'request_type': 'update_workspace',
      'workspaceId': workspaceId,
      'name': name,
      'data': data,
      'settings': settings,
    });
  }

  Future<void> deleteWorkspace(String workspaceId) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    _webSocketService.sendMessage('delete_workspace', {
      'request_type': 'delete_workspace',
      'workspaceId': workspaceId,
    });
  }

  void setCurrentWorkspace(String workspaceId) {
    if (_workspaces.any((w) => w.id == workspaceId)) {
      _currentWorkspaceId = workspaceId;
      notifyListeners();
    }
  }

  void clearError() {
    _error = null;
    notifyListeners();
  }
}
