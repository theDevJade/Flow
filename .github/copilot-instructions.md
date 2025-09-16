# Flow - AI Agent Development Guide

## Project Architecture

Flow is a **multi-platform visual graph and code editor** with a hybrid Kotlin-Flutter architecture:

- **Flutter Frontend** (`flow_frontend/`): Cross-platform desktop app with dual workspace (graph editor + code editor)
- **Kotlin WebSocket Server** (`webserver/`): Real-time communication backend using Ktor
- **Kotlin Core Engine** (`flow/`): Graph processing and language extension framework  
- **Minecraft Plugin** (`plugin/`): PaperMC plugin integration
- **Common Module** (`common/`): Shared data models and configuration

## Key Development Patterns

### Multi-Module Gradle Structure
```bash
# Build entire project
./gradlew build

# Run WebSocket server only
./gradlew :webserver:runWebSocket

# Run full application (includes WebSocket + web server)
./gradlew :webserver:run

# Run Minecraft plugin in test environment
./gradlew :plugin:runServer
```

### Flutter State Management Architecture
- **Provider-based**: All state management through `provider` package
- **Hierarchical State**: `AppState` → `WorkspaceState` + `FileSystemState`
- **Service Layer**: Singleton services injected via Provider (`AuthService`, `WebSocketService`, etc.)
- **Real-time Communication**: WebSocket service with automatic reconnection and message queuing

**State Update Pattern**:
```dart
// Services notify state changes via notifyListeners()
class WebSocketService with ChangeNotifier {
  void _handleMessage(WebSocketMessage message) {
    // Process message
    notifyListeners(); // Triggers UI rebuild
  }
}
```

### WebSocket Communication Protocol
- **Message Format**: JSON with `type`, `id`, `data`, `timestamp`, `userId` fields
- **Authentication**: Token-based auth via query param or Authorization header
- **Session Management**: Server maintains user sessions with automatic cleanup
- **Mock Responses**: Development mode includes simulated server responses

### Database Integration
- **Exposed ORM**: Kotlin SQL framework with SQLite backend
- **Connection Management**: Pool-based with WAL mode enabled
- **Data Location**: `./data/flow.db` (configurable via `FlowConfiguration`)

### Flutter Development Workflows

**Running the Frontend**:
```bash
cd flow_frontend
flutter pub get
flutter run -d macos  # or windows/linux
```

**Key Keyboard Shortcuts**:
- `Cmd/Ctrl + 1`: Switch to Graph Editor
- `Cmd/Ctrl + 2`: Switch to Code Editor  
- `Cmd/Ctrl + S`: Save current file
- `Cmd/Ctrl + W`: Close current tab

### File System Integration
- **Mock File System**: `LocalFileService` provides development file tree
- **Multi-tab Editor**: Syntax highlighting for Dart, JSON, YAML, Markdown
- **File State Tracking**: Modified indicators, save/close operations

### Plugin Development (Minecraft Integration)
- **PaperMC Framework**: Built on Paper API with Kotlin
- **Resource Factory**: Auto-generates `plugin.yml` from Gradle config
- **Shadow JAR**: Single JAR deployment with all dependencies

### Testing Approach
- **Python WebSocket Test**: `test_websocket.py` for connection validation
- **Flutter Widget Tests**: Standard Flutter test framework
- **Manual Testing**: Run servers locally and test integration

## Critical Configuration Files

- **Gradle Version Catalog**: `gradle/libs.versions.toml` - Central dependency management
- **Flutter Dependencies**: `flow_frontend/pubspec.yaml` - UI/state management libs
- **WebSocket Config**: `common/src/.../FlowConfiguration.kt` - Ports and database paths
- **Build Logic**: Root `settings.gradle.kts` defines multi-module structure

## Development Server Setup

1. **Start WebSocket Server**: `./gradlew :webserver:runWebSocket` (port 9090)
2. **Start Web Server** (optional): `./gradlew :webserver:run` (port 8080)  
3. **Launch Flutter App**: `cd flow_frontend && flutter run`
4. **Test Connection**: `python3 test_websocket.py`

## Code Style Conventions

- **Kotlin**: Standard Kotlin conventions, coroutines for async operations
- **Flutter**: Provider pattern for state, services as singletons
- **File Organization**: Feature-based modules, shared utilities in `common/`
- **Naming**: WebSocket message types are lowercase strings (`"auth"`, `"graph_update"`)

## Integration Points

- **Flutter ↔ WebSocket**: Real-time bidirectional communication via `web_socket_channel`
- **Kotlin Modules**: Project dependencies defined in individual `build.gradle.kts`
- **Database Access**: Centralized through `DatabaseManager` singleton
- **Cross-Platform**: Flutter handles UI, Kotlin handles business logic and external integrations