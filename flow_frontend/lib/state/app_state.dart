import 'package:flutter/foundation.dart';
import '../services/auth_service.dart';
import '../services/websocket_service.dart';
import '../services/file_system_service.dart';
import '../services/persistence_service.dart';
import '../services/graph_persistence_service.dart';
import '../services/flutter_log_service.dart';
import '../graph_editor/node_template_service.dart';
import '../services/graph_project_service.dart';
import 'package:flow_frontend/state/file_system_state.dart' as fs;
import 'package:flow_frontend/state/workspace_state.dart';
import 'package:flow_frontend/state/workspace_manager.dart';
import 'package:flow_frontend/state/page_manager.dart';

class AppState with ChangeNotifier {
  final AuthService _authService = AuthService.instance;
  final WebSocketService _webSocketService = WebSocketService.instance;
  final FileSystemService _fileSystemService = FileSystemService.instance;
  final FlutterLogService _flutterLogService = FlutterLogService.instance;
  final WorkspaceState _workspaceState = WorkspaceState();
  final WorkspaceManager _workspaceManager = WorkspaceManager();
  final fs.FileSystemState _fileSystemState = fs.FileSystemState();
  final PageManager _pageManager = PageManager.instance;

  bool _isInitialized = false;
  String? _error;
  bool _isProcessingGraphNotFound = false;

  AuthService get authService => _authService;
  WebSocketService get webSocketService => _webSocketService;
  FileSystemService get fileSystemService => _fileSystemService;
  FlutterLogService get flutterLogService => _flutterLogService;
  WorkspaceState get workspaceState => _workspaceState;
  WorkspaceManager get workspaceManager => _workspaceManager;
  fs.FileSystemState get fileSystemState => _fileSystemState;
  PageManager get pageManager => _pageManager;

  bool get isInitialized => _isInitialized;
  String? get error => _error;

  Future<void> initialize() async {
    try {
      debugPrint('AppState: Starting initialization...');
      _error = null;
      notifyListeners();

      debugPrint('AppState: Initializing auth service...');
      await _authService.initialize();

      debugPrint('AppState: Loading WebSocket config...');
      final config = await PersistenceService.instance.loadWebSocketConfig();
      if (config != null) {
        _webSocketService.configureServer(config['host'], config['port']);
      } else {
        debugPrint('No WebSocket config found, defaulting to localhost:9090');
        _webSocketService.configureServer('localhost', 9090);
        await PersistenceService.instance.saveWebSocketConfig(
          'localhost',
          9090,
        );
      }


      debugPrint('AppState: Connecting to WebSocket...');
      try {
        await _connectWebSocketWithAuth().timeout(
          const Duration(seconds: 10),
          onTimeout: () {
            debugPrint(
              'AppState: WebSocket connection timed out, continuing anyway...',
            );
          },
        );
      } catch (e) {
        debugPrint(
          'AppState: WebSocket connection failed: $e, continuing anyway...',
        );
      }

      if (_authService.isAuthenticated) {
        debugPrint('AppState: Loading workspaces and syncing data...');
        try {

          await _workspaceManager.loadWorkspaces();

          await _syncDataFromServer().timeout(
            const Duration(seconds: 5),
            onTimeout: () {
              debugPrint('AppState: Data sync timed out, continuing anyway...');
            },
          );
        } catch (e) {
          debugPrint(
            'AppState: Error during workspace/sync operations: $e, continuing anyway...',
          );
        }
      }

      debugPrint('AppState: Restoring workspace state...');
      await _restoreWorkspaceState();

      debugPrint('AppState: Setting up listeners and services...');
      _pageManager.initialize(_workspaceManager);
      _authService.addListener(_onAuthStateChanged);
      _webSocketService.messages.listen(_handleWebSocketMessage);

      NodeTemplateService.instance.initialize(_webSocketService);

      try {
        await NodeTemplateService.instance.loadTemplates();
      } catch (e) {
        debugPrint('AppState: Error loading node templates: $e, continuing anyway...');
      }
      await _fileSystemService.initialize();

      try {
        await GraphProjectService.instance.initialize();
      } catch (e) {
        debugPrint(
          'AppState: GraphProjectService initialization failed: $e, continuing anyway...',
        );
      }

      _workspaceState.addListener(_saveWorkspaceState);
      _fileSystemState.addListener(_saveFileSystemState);
      _fileSystemState.addListener(_onFileSystemStateChanged);

      debugPrint('AppState: Initialization completed successfully!');
      _isInitialized = true;
      notifyListeners();
    } catch (e) {
      debugPrint('AppState: Initialization error: $e');
      _error = e.toString();
      notifyListeners();
    }
  }

  void _onAuthStateChanged() {
    if (_authService.isAuthenticated) {
      _connectWebSocketWithAuth().then((_) {

        _syncDataFromServer();
      });
    } else {
      _webSocketService.disconnect();
    }
    notifyListeners();
  }

  Future<void> _connectWebSocketWithAuth() async {

    final token = _authService.isAuthenticated ? _authService.authToken : null;
    await _webSocketService.connect(token);
  }

  void _handleWebSocketMessage(WebSocketMessage message) {
    try {
      switch (message.type) {
        case 'auth_response':
          if (message.data['success'] == true) {
            debugPrint('AppState: Authentication successful');
          } else {
            debugPrint(
              'AppState: Authentication failed: ${message.data['error']}',
            );
          }
          break;
        case 'file_tree':

          break;
        case 'file_content':

          break;
        case 'workspace_list_response':
        case 'workspace_created':
        case 'workspace_updated':
        case 'workspace_deleted':

          break;
        case 'graph_data':
        case 'graph_list_response':
        case 'graph_load_response':

          break;
        case 'node_templates_response':

          break;
        case 'connection_established':
          debugPrint('AppState: WebSocket connection established');
          break;
        case 'user_left':
          debugPrint('AppState: User left session: ${message.data}');
          break;
        case 'pong':
        case 'heartbeat_ack':

          break;
        case 'success':
          debugPrint(
            'AppState: Operation completed successfully: ${message.data}',
          );
          break;
        case 'terminal_response':
          debugPrint('AppState: Terminal response: ${message.data}');
          break;
        case 'graph_sync_error':
          debugPrint('AppState: Graph sync error: ${message.data}');
          break;
        case 'error':
          final errorMessage = message.data['message'] ?? message.data['error'] ?? 'WebSocket error';
          final correlationId = message.data['correlationId'] as String?;

          debugPrint(
            'AppState: WebSocket error: $errorMessage',
          );


          if (errorMessage == 'Graph not found' && correlationId != null) {

            if (!_isProcessingGraphNotFound) {
              _isProcessingGraphNotFound = true;
              _handleGraphNotFoundError(correlationId);

              Future.delayed(const Duration(seconds: 2), () {
                _isProcessingGraphNotFound = false;
              });
            }
          } else {
            _error = errorMessage;
            notifyListeners();
          }
          break;
        case 'file_saved':

          break;
        default:
          debugPrint('AppState: Unhandled message type: ${message.type}');
          break;
      }
    } catch (e) {
      debugPrint('AppState: Error handling WebSocket message: $e');
    }
  }


  Future<void> _syncDataFromServer() async {
    try {
      debugPrint('AppState: Starting data synchronization from server...');


      await Future.delayed(const Duration(milliseconds: 500));

      if (_webSocketService.currentStatus !=
          WebSocketConnectionStatus.connected) {
        debugPrint(
          'AppState: WebSocket not connected, attempting to reconnect...',
        );
        _webSocketService.reconnect();
        return;
      }


      _webSocketService.sendMessage('get_file_tree', {});


      _webSocketService.sendMessage('workspace_list', {});


      _webSocketService.sendMessage('graph_list', {});


      _webSocketService.sendMessage('graph_load', {'graphId': 'default'});

      debugPrint('AppState: Data synchronization requests sent');
    } catch (e) {
      debugPrint('AppState: Error during data sync: $e');
    }
  }

  @override
  void dispose() {
    _workspaceState.removeListener(_saveWorkspaceState);
    _fileSystemState.removeListener(_saveFileSystemState);
    _fileSystemState.removeListener(_onFileSystemStateChanged);
    _authService.removeListener(_onAuthStateChanged);


    GraphPersistenceService.instance.dispose();


    _webSocketService.disconnect();
    super.dispose();
  }

  Future<void> _restoreWorkspaceState() async {
    try {
      final persistenceService = PersistenceService.instance;


      final workspaceType = await persistenceService.loadWorkspaceType();
      if (workspaceType != null) {
        _workspaceState.setWorkspace(workspaceType);
      }


      final openFiles = await persistenceService.loadOpenFiles();
      if (openFiles.isNotEmpty) {
        for (final openFile in openFiles) {
          _fileSystemState.openFile(openFile.path, openFile.content);
        }
      }


      final activeFilePath = await persistenceService.loadActiveFile();
      if (activeFilePath != null &&
          _fileSystemState.openFiles.any((f) => f.path == activeFilePath)) {
        _fileSystemState.switchToFile(activeFilePath);
      }

      debugPrint('AppState: Workspace state restored successfully');
    } catch (e) {
      debugPrint('AppState: Error restoring workspace state: $e');
    }
  }

  void _saveWorkspaceState() {
    try {
      final persistenceService = PersistenceService.instance;


      persistenceService.saveWorkspaceType(_workspaceState.currentWorkspace);


      persistenceService.saveOpenFiles(_fileSystemState.openFiles);


      if (_fileSystemState.activeFile != null) {
        persistenceService.saveActiveFile(_fileSystemState.activeFile!.path);
      }

      debugPrint('AppState: Workspace state saved');
    } catch (e) {
      debugPrint('AppState: Error saving workspace state: $e');
    }
  }

  void _saveFileSystemState() {
    try {
      final persistenceService = PersistenceService.instance;


      persistenceService.saveOpenFiles(_fileSystemState.openFiles);

      debugPrint('AppState: File system state saved');
    } catch (e) {
      debugPrint('AppState: Error saving file system state: $e');
    }
  }

  void _onFileSystemStateChanged() {
    debugPrint(
      'AppState: FileSystemState changed, notifying AppState listeners',
    );
    notifyListeners();
  }

  void _handleGraphNotFoundError(String correlationId) {
    debugPrint('AppState: Handling graph not found error for correlationId: $correlationId');


    final workspaceId = _workspaceManager.currentWorkspace?.id;
    if (workspaceId == null) {
      debugPrint('AppState: No workspace available, skipping graph creation');
      return;
    }


    final graphId = 'default';

    debugPrint('AppState: Attempting to create missing graph: $graphId in workspace: $workspaceId');

    try {

      GraphPersistenceService.instance.createGraph(graphId, workspaceId: workspaceId);

      // Add a log entry for this action
      _flutterLogService.addCustomLog(
        'GRAPH_CREATION',
        'Auto-created missing graph: $graphId in workspace: $workspaceId (correlationId: $correlationId)',
        isError: false,
      );
    } catch (e) {
      debugPrint('AppState: Error creating graph: $e');
      _flutterLogService.addCustomLog(
        'GRAPH_CREATION_ERROR',
        'Failed to create graph: $e',
        isError: true,
      );
    }
  }
}
