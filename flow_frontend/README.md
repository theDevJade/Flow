# Flow - Graph & Code Editor

A modern Flutter application that combines visual graph editing with a full-featured code editor, designed with an Atom-style interface and dark theme.

## Features

### 🎛️ Dual Workspace Architecture
- **Graph Editor**: Visual node-based editing with drag-and-drop functionality
- **Code Editor**: Full-featured IDE with syntax highlighting, tabs, and file management
- Seamless switching between workspaces with animated transitions

### 🔐 Authentication & Real-time Communication
- Mock authentication system with session management
- WebSocket-based real-time communication
- Connection status indicators and automatic reconnection

### 💾 File System Integration
- Tree-based file explorer with folder expansion/collapse
- Multi-tab code editing with syntax highlighting for Dart, JSON, YAML, Markdown
- File modification tracking with visual indicators
- Save/close functionality with keyboard shortcuts

### 🎨 Modern UI/UX
- Dark theme with professional color scheme
- Smooth animations and transitions powered by Flutter Animate
- Responsive layout with resizable panels
- Status bar with connection and file information

### ⌨️ Keyboard Shortcuts
- `Cmd/Ctrl + 1`: Switch to Graph Editor
- `Cmd/Ctrl + 2`: Switch to Code Editor  
- `Cmd/Ctrl + S`: Save current file
- `Cmd/Ctrl + W`: Close current tab

## Architecture

### State Management
- **Provider**: Used for reactive state management across the application
- **AppState**: Central application state coordination
- **WorkspaceState**: Manages workspace switching and transitions
- **FileSystemState**: Handles file operations and editor tabs

### Services
- **AuthService**: Handles user authentication with persistent sessions
- **WebSocketService**: Manages real-time communication with mock server responses

### Components
- **WorkspaceTopBar**: Contains workspace switcher, connection status, and user menu
- **FileTreeView**: File system navigation with icon-based file type recognition
- **TabbedCodeEditor**: Multi-tab editor with syntax highlighting
- **StatusBar**: Bottom status information display
- **GlobalKeyboardShortcuts**: Application-wide keyboard shortcut handling

## Demo Accounts

The application includes mock authentication with these test accounts:
- `admin@flow.dev` / `password123`
- `user@flow.dev` / `userpass`  
- `dev@flow.dev` / `devpass`

## Getting Started

### Prerequisites
- Flutter SDK (^3.9.2)
- Dart SDK
- macOS development environment (for desktop app)

### Installation
1. Clone the repository
2. Run `flutter pub get` to install dependencies
3. Run `flutter run -d macos` for desktop or `flutter run -d chrome` for web

### Dependencies
- `provider`: State management
- `web_socket_channel`: WebSocket communication
- `code_text_field`: Code editor with syntax highlighting  
- `flutter_animate`: Smooth animations
- `shared_preferences`: Local data persistence
- `path_provider`: File system access

## Mock WebSocket Features

The application simulates real-time features including:
- File system updates and synchronization
- Graph data persistence and loading
- Authentication token management
- Connection status monitoring
- Server-side file content management

## Future Enhancements

- Command palette for quick actions
- Plugin/extension system
- Real WebSocket server integration
- Advanced graph node types and templates
- Collaborative editing features
- Project management and workspaces
- Git integration
- Advanced code analysis and IntelliSense

## Development

The codebase is structured for modularity and extensibility:
- `/lib/state/`: State management classes
- `/lib/services/`: External service integrations
- `/lib/widgets/`: Reusable UI components  
- `/lib/screens/`: Main application screens
- `/lib/graph_editor/`: Graph editing functionality

Built with Flutter's modern Material 3 design system and optimized for both desktop and web deployment.
