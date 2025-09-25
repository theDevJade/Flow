# Flow - AI Coding Agent Instructions

## Project Overview
Flow is a modern dual-workspace application combining visual graph editing with a full-featured code editor. The architecture consists of a Flutter frontend (`flow_frontend/`) and a Kotlin backend with multiple modules, communicating via WebSocket and REST APIs.

## Architecture & Module Structure

### Multi-Module Gradle Setup
- **Root**: Multi-project Gradle build with shared configuration
- **`:webserver`**: Ktor-based HTTP/WebSocket server (main backend)
- **`:flow`**: Core Flow API and graph management logic
- **`:common`**: Shared data models and configuration
- **`:plugin`**: Plugin system architecture
- **`:app`**: Application entry point wrapper
- **`flow_frontend/`**: Flutter desktop/web client (separate from Gradle)

### Key Integration Patterns
- WebSocket communication on port 9090 (`FlowConfiguration.webserverConfig.websocketPort`)
- REST API on port 8080 for authentication and file operations
- SQLite database with Exposed ORM (`data/flow.db`)
- Local persistence in Flutter via SharedPreferences
- Cross-platform file system access through `FileSystemAccessImpl`

## Essential Development Commands

### Backend (Kotlin/Gradle)
```bash
# Build and run WebSocket server (primary development server)
./gradlew :webserver:runWebSocket

# Test build all modules without running tests
./gradlew testBuild

# Build specific module
./gradlew :webserver:build
```

### Frontend (Flutter)
```bash
cd flow_frontend/
flutter pub get
flutter run -d macos    # Desktop
flutter run -d chrome   # Web
flutter analyze         # Lint check
```

### Full Stack Development
```bash
# Complete test build (backend + frontend analysis)
./gradlew fullTestBuild

# Root-level WebSocket server (delegates to webserver module)
./gradlew runWebSocketServer
```

## Core Data Models & Communication

### Graph Data Flow
1. **Flutter Frontend**: `GraphNode`/`GraphConnection` models with visual editing
2. **WebSocket Protocol**: JSON message format via `WebSocketMessage` class
3. **Backend Processing**: `FlowGraph` models in `:flow` module via `GraphManager`
4. **Database Persistence**: `GraphDataTable` with Exposed ORM in `:webserver`

### WebSocket Message Types
- `graph_save`: Persist graph data with auto-versioning
- `graph_load`: Retrieve graph by ID
- `auth_token`: Authentication validation
- `file_sync`: File system synchronization
- `workspace_update`: Workspace state changes

### Database Schema Patterns
All tables use `IntIdTable` from Exposed with timestamp fields:
- `GraphDataTable`: Graph JSON storage with versioning
- `WorkspaceDataTable`: User workspace state
- `UsersTable`/`AuthTokensTable`: Authentication system
- `FileSystemTable`: File metadata and content

## Project-Specific Conventions

### State Management (Flutter)
- **Provider pattern** for app-level state
- **`PageStateManager`** for cross-tab state persistence
- **`PersistenceService`** for local data storage
- **`WebSocketService`** singleton for real-time communication

### Backend Architecture
- **Ktor Application modules**: `module()` for HTTP, `moduleSocket()` for WebSocket
- **`FlowCore.initialize()`** pattern for service initialization
- **Event-driven updates** via `EventManager` and `GraphEvent` system
- **Mutex-protected operations** for concurrent graph modifications

### Configuration Management
- **`FlowConfiguration`** object in `:common` for shared settings
- **Gradle version catalogs** (`gradle/libs.versions.toml`)
- **Environment-specific configs** via `application.conf` in `:webserver`

## Development Workflow Patterns

### Adding New Graph Features
1. Define data models in `:common/src/main/kotlin/models/`
2. Implement backend logic in `:flow` GraphManager
3. Add WebSocket message handlers in `:webserver/websocket/`
4. Create Flutter UI components in `flow_frontend/lib/graph_editor/`
5. Update database schema in `DatabaseTables.kt` if needed

### WebSocket Development
- Server: Extend `WebSocketMessageHandler` in `:webserver`
- Client: Use `WebSocketService.instance` singleton in Flutter
- Message format: Always include `type`, `data`, and `timestamp` fields
- Connection management: Handle reconnection via `ConnectionStatus` enum

### Database Changes
- Use Exposed migrations pattern in `DatabaseManager.initialize()`
- Repository pattern: `*Repository` objects for data access
- Transaction wrapping for all database operations
- SQLite WAL mode enabled for concurrent access

## Testing & Quality Patterns
- **Backend**: JUnit tests in `src/test/kotlin/` directories
- **Frontend**: Flutter widget tests in `test/` directory
- **CI/CD**: GitHub Actions with change detection for selective building
- **Code quality**: Flutter analyzer + Kotlin linting via Gradle

## Key Files for Context
- **Architecture**: `settings.gradle.kts`, `build.gradle.kts`, `flow_frontend/pubspec.yaml`
- **WebSocket**: `webserver/src/main/kotlin/Sockets.kt`, `flow_frontend/lib/services/websocket_service.dart`
- **Data Models**: `common/src/main/kotlin/models/`, `flow_frontend/lib/models/`
- **Database**: `webserver/src/main/kotlin/database/DatabaseTables.kt`
- **Graph Logic**: `flow/src/main/kotlin/com/thedevjade/flow/api/graph/GraphManager.kt`