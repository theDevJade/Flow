---
title: Architecture
description: Technical architecture and design patterns of Flow Frontend
---

# Flow Frontend Architecture

Flow Frontend is built with a modular, scalable architecture that separates concerns and promotes maintainability.

## Overall Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────┐
│                    Flow Frontend                        │
├─────────────────────────────────────────────────────────┤
│  Presentation Layer (UI/UX)                            │
│  ├── Screens (Main UI Components)                      │
│  ├── Widgets (Reusable Components)                     │
│  └── Themes (Styling & Design System)                  │
├─────────────────────────────────────────────────────────┤
│  State Management Layer                                 │
│  ├── AppState (Global Application State)               │
│  ├── WorkspaceState (Workspace Management)             │
│  ├── FileSystemState (File Operations)                 │
│  └── AuthState (Authentication)                        │
├─────────────────────────────────────────────────────────┤
│  Service Layer (Business Logic)                        │
│  ├── AuthService (User Authentication)                 │
│  ├── WebSocketService (Real-time Communication)       │
│  ├── FileService (File Operations)                     │
│  └── GraphService (Graph Data Management)              │
├─────────────────────────────────────────────────────────┤
│  Data Layer (Data Management)                          │
│  ├── Local Storage (SharedPreferences)                 │
│  ├── File System (PathProvider)                        │
│  └── Network (WebSocket/HTTP)                          │
└─────────────────────────────────────────────────────────┘
```

## State Management

### Provider Pattern

Flow Frontend uses the Provider pattern for state management, providing a reactive and testable approach to managing application state.

#### AppState
Central application state coordinator that manages global application state.

```dart
class AppState extends ChangeNotifier {
  // Global application state
  bool _isInitialized = false;
  String _currentUser = '';
  ConnectionStatus _connectionStatus = ConnectionStatus.disconnected;
  
  // Getters
  bool get isInitialized => _isInitialized;
  String get currentUser => _currentUser;
  ConnectionStatus get connectionStatus => _connectionStatus;
  
  // Methods
  void initialize() {
    _isInitialized = true;
    notifyListeners();
  }
  
  void setUser(String user) {
    _currentUser = user;
    notifyListeners();
  }
  
  void setConnectionStatus(ConnectionStatus status) {
    _connectionStatus = status;
    notifyListeners();
  }
}
```

#### WorkspaceState
Manages workspace switching and transitions between Graph Editor and Code Editor.

```dart
class WorkspaceState extends ChangeNotifier {
  WorkspaceType _currentWorkspace = WorkspaceType.codeEditor;
  bool _isTransitioning = false;
  
  WorkspaceType get currentWorkspace => _currentWorkspace;
  bool get isTransitioning => _isTransitioning;
  
  Future<void> switchToWorkspace(WorkspaceType workspace) async {
    if (_isTransitioning) return;
    
    _isTransitioning = true;
    notifyListeners();
    
    // Animate transition
    await Future.delayed(Duration(milliseconds: 300));
    
    _currentWorkspace = workspace;
    _isTransitioning = false;
    notifyListeners();
  }
}
```

#### FileSystemState
Handles file operations, editor tabs, and file system navigation.

```dart
class FileSystemState extends ChangeNotifier {
  List<FileTab> _openTabs = [];
  String? _activeTabId;
  FileTreeItem? _selectedFile;
  
  List<FileTab> get openTabs => _openTabs;
  String? get activeTabId => _activeTabId;
  FileTreeItem? get selectedFile => _selectedFile;
  
  void openFile(FileTreeItem file) {
    // Check if file is already open
    final existingTab = _openTabs.firstWhere(
      (tab) => tab.filePath == file.path,
      orElse: () => FileTab.empty(),
    );
    
    if (existingTab.isEmpty) {
      _openTabs.add(FileTab.fromFile(file));
    }
    
    _activeTabId = file.path;
    notifyListeners();
  }
  
  void closeTab(String tabId) {
    _openTabs.removeWhere((tab) => tab.id == tabId);
    
    if (_activeTabId == tabId) {
      _activeTabId = _openTabs.isNotEmpty ? _openTabs.last.id : null;
    }
    
    notifyListeners();
  }
}
```

## Service Layer

### AuthService
Handles user authentication and session management.

```dart
class AuthService {
  static const String _tokenKey = 'auth_token';
  static const String _userKey = 'user_data';
  
  final SharedPreferences _prefs;
  final WebSocketService _webSocketService;
  
  AuthService(this._prefs, this._webSocketService);
  
  Future<AuthResult> login(String email, String password) async {
    try {
      // Mock authentication logic
      if (_isValidCredentials(email, password)) {
        final token = _generateToken(email);
        final user = _getUserData(email);
        
        await _prefs.setString(_tokenKey, token);
        await _prefs.setString(_userKey, jsonEncode(user.toJson()));
        
        // Notify WebSocket service
        _webSocketService.authenticate(token);
        
        return AuthResult.success(user);
      } else {
        return AuthResult.failure('Invalid credentials');
      }
    } catch (e) {
      return AuthResult.failure('Login failed: $e');
    }
  }
  
  Future<void> logout() async {
    await _prefs.remove(_tokenKey);
    await _prefs.remove(_userKey);
    _webSocketService.disconnect();
  }
  
  bool isAuthenticated() {
    return _prefs.getString(_tokenKey) != null;
  }
  
  User? getCurrentUser() {
    final userData = _prefs.getString(_userKey);
    if (userData != null) {
      return User.fromJson(jsonDecode(userData));
    }
    return null;
  }
}
```

### WebSocketService
Manages real-time communication with the server.

```dart
class WebSocketService {
  WebSocketChannel? _channel;
  StreamController<WebSocketMessage> _messageController = StreamController.broadcast();
  Timer? _reconnectTimer;
  
  Stream<WebSocketMessage> get messageStream => _messageController.stream;
  
  Future<void> connect() async {
    try {
      _channel = WebSocketChannel.connect(Uri.parse('ws://localhost:8080/ws'));
      
      _channel!.stream.listen(
        (data) => _handleMessage(data),
        onError: (error) => _handleError(error),
        onDone: () => _handleDisconnection(),
      );
      
      _messageController.add(WebSocketMessage.connected());
    } catch (e) {
      _scheduleReconnect();
    }
  }
  
  void _handleMessage(dynamic data) {
    try {
      final message = WebSocketMessage.fromJson(jsonDecode(data));
      _messageController.add(message);
    } catch (e) {
      print('Error parsing WebSocket message: $e');
    }
  }
  
  void _handleError(dynamic error) {
    print('WebSocket error: $error');
    _scheduleReconnect();
  }
  
  void _handleDisconnection() {
    print('WebSocket disconnected');
    _scheduleReconnect();
  }
  
  void _scheduleReconnect() {
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(Duration(seconds: 5), () {
      connect();
    });
  }
  
  void sendMessage(WebSocketMessage message) {
    if (_channel != null) {
      _channel!.sink.add(jsonEncode(message.toJson()));
    }
  }
  
  void disconnect() {
    _reconnectTimer?.cancel();
    _channel?.sink.close();
    _channel = null;
  }
}
```

## Component Architecture

### Screen Components
Main application screens that compose the overall UI.

#### MainScreen
Root screen that contains the workspace switcher and manages the overall layout.

```dart
class MainScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        return Scaffold(
          body: Column(
            children: [
              WorkspaceTopBar(),
              Expanded(
                child: Consumer<WorkspaceState>(
                  builder: (context, workspaceState, child) {
                    return AnimatedSwitcher(
                      duration: Duration(milliseconds: 300),
                      child: workspaceState.currentWorkspace == WorkspaceType.graphEditor
                          ? GraphEditorScreen()
                          : CodeEditorScreen(),
                    );
                  },
                ),
              ),
              StatusBar(),
            ],
          ),
        );
      },
    );
  }
}
```

#### CodeEditorScreen
Code editor workspace with file tree and multi-tab editor.

```dart
class CodeEditorScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // File tree sidebar
        Container(
          width: 250,
          child: FileTreeView(),
        ),
        // Vertical divider
        VerticalDivider(width: 1),
        // Main editor area
        Expanded(
          child: TabbedCodeEditor(),
        ),
      ],
    );
  }
}
```

### Widget Components
Reusable UI components used throughout the application.

#### FileTreeView
File system navigation component.

```dart
class FileTreeView extends StatefulWidget {
  @override
  _FileTreeViewState createState() => _FileTreeViewState();
}

class _FileTreeViewState extends State<FileTreeView> {
  @override
  Widget build(BuildContext context) {
    return Consumer<FileSystemState>(
      builder: (context, fileSystemState, child) {
        return ListView.builder(
          itemCount: fileSystemState.fileTree.length,
          itemBuilder: (context, index) {
            final item = fileSystemState.fileTree[index];
            return FileTreeItemWidget(
              item: item,
              onTap: () => fileSystemState.openFile(item),
              onExpand: () => fileSystemState.toggleExpansion(item),
            );
          },
        );
      },
    );
  }
}
```

#### TabbedCodeEditor
Multi-tab code editor component.

```dart
class TabbedCodeEditor extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<FileSystemState>(
      builder: (context, fileSystemState, child) {
        if (fileSystemState.openTabs.isEmpty) {
          return Center(
            child: Text('No files open'),
          );
        }
        
        return Column(
          children: [
            // Tab bar
            Container(
              height: 40,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: fileSystemState.openTabs.length,
                itemBuilder: (context, index) {
                  final tab = fileSystemState.openTabs[index];
                  return TabWidget(
                    tab: tab,
                    isActive: tab.id == fileSystemState.activeTabId,
                    onClose: () => fileSystemState.closeTab(tab.id),
                    onSelect: () => fileSystemState.setActiveTab(tab.id),
                  );
                },
              ),
            ),
            // Editor area
            Expanded(
              child: CodeEditor(
                file: fileSystemState.getActiveFile(),
                onContentChanged: (content) {
                  fileSystemState.updateFileContent(
                    fileSystemState.activeTabId!,
                    content,
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}
```

## Data Models

### Core Data Models

#### User
User authentication and profile data.

```dart
class User {
  final String id;
  final String email;
  final String displayName;
  final String role;
  final DateTime createdAt;
  
  User({
    required this.id,
    required this.email,
    required this.displayName,
    required this.role,
    required this.createdAt,
  });
  
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      email: json['email'],
      displayName: json['displayName'],
      role: json['role'],
      createdAt: DateTime.parse(json['createdAt']),
    );
  }
  
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'displayName': displayName,
      'role': role,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}
```

#### FileTab
Represents an open file tab in the editor.

```dart
class FileTab {
  final String id;
  final String filePath;
  final String fileName;
  final String content;
  final bool isModified;
  final FileType fileType;
  
  FileTab({
    required this.id,
    required this.filePath,
    required this.fileName,
    required this.content,
    this.isModified = false,
    required this.fileType,
  });
  
  factory FileTab.fromFile(FileTreeItem file) {
    return FileTab(
      id: file.path,
      filePath: file.path,
      fileName: file.name,
      content: file.content ?? '',
      fileType: _getFileType(file.name),
    );
  }
  
  static FileType _getFileType(String fileName) {
    final extension = fileName.split('.').last.toLowerCase();
    switch (extension) {
      case 'dart':
        return FileType.dart;
      case 'json':
        return FileType.json;
      case 'yaml':
      case 'yml':
        return FileType.yaml;
      case 'md':
        return FileType.markdown;
      default:
        return FileType.text;
    }
  }
}
```

#### WebSocketMessage
Real-time communication message format.

```dart
class WebSocketMessage {
  final String type;
  final Map<String, dynamic> data;
  final DateTime timestamp;
  
  WebSocketMessage({
    required this.type,
    required this.data,
    required this.timestamp,
  });
  
  factory WebSocketMessage.fromJson(Map<String, dynamic> json) {
    return WebSocketMessage(
      type: json['type'],
      data: json['data'] ?? {},
      timestamp: DateTime.parse(json['timestamp']),
    );
  }
  
  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'data': data,
      'timestamp': timestamp.toIso8601String(),
    };
  }
  
  factory WebSocketMessage.connected() {
    return WebSocketMessage(
      type: 'connected',
      data: {},
      timestamp: DateTime.now(),
    );
  }
  
  factory WebSocketMessage.fileChanged(String filePath, String content) {
    return WebSocketMessage(
      type: 'file_changed',
      data: {
        'filePath': filePath,
        'content': content,
      },
      timestamp: DateTime.now(),
    );
  }
}
```

## Design Patterns

### Provider Pattern
Used for state management throughout the application.

### Observer Pattern
Used for reactive UI updates when state changes.

### Factory Pattern
Used for creating different types of file tabs and UI components.

### Singleton Pattern
Used for services that need to be shared across the application.

### Command Pattern
Used for keyboard shortcuts and user actions.

## Performance Considerations

### Widget Optimization
- Use `const` constructors where possible
- Implement `shouldRebuild` for custom widgets
- Use `ListView.builder` for large lists
- Minimize widget rebuilds with proper state management

### Memory Management
- Dispose of controllers and streams properly
- Use weak references where appropriate
- Implement proper cleanup in `dispose()` methods
- Monitor memory usage with Flutter Inspector

### Network Optimization
- Implement connection pooling
- Use efficient serialization formats
- Implement proper error handling and retry logic
- Cache frequently accessed data

## Testing Strategy

### Unit Tests
- Test individual components in isolation
- Mock dependencies for predictable testing
- Test state management logic
- Test service layer functionality

### Widget Tests
- Test UI components and interactions
- Test user input handling
- Test navigation and routing
- Test responsive design

### Integration Tests
- Test complete user workflows
- Test real-time communication
- Test file system operations
- Test authentication flows

## Security Considerations

### Data Protection
- Encrypt sensitive data in local storage
- Use secure communication protocols
- Implement proper input validation
- Sanitize user inputs

### Authentication Security
- Use secure token storage
- Implement proper session management
- Validate tokens on the server side
- Implement proper logout functionality

### Network Security
- Use HTTPS/WSS for all communications
- Implement proper certificate validation
- Use secure WebSocket connections
- Implement proper error handling
