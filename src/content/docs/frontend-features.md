---
title: Features
description: Detailed overview of Flow Frontend features
---

# Flow Frontend Features

Flow Frontend provides a comprehensive development environment with powerful features for both visual and code-based development.

## Dual Workspace Architecture

### Graph Editor
- **Visual Node-Based Editing**: Create and manipulate nodes with drag-and-drop functionality
- **Connection System**: Link nodes together to create visual workflows
- **Node Types**: Support for various node types and templates
- **Real-time Updates**: Changes are reflected immediately in the visual editor

### Code Editor
- **Multi-tab Interface**: Work with multiple files simultaneously
- **Syntax Highlighting**: Support for Dart, JSON, YAML, Markdown, and more
- **File Management**: Open, save, close, and manage files efficiently
- **Code Navigation**: Jump to definitions, find references, and more

### Workspace Switching
- **Smooth Transitions**: Animated transitions between workspaces
- **Keyboard Shortcuts**: Quick switching with `Cmd/Ctrl + 1/2`
- **State Persistence**: Each workspace maintains its own state
- **Context Awareness**: UI adapts based on current workspace

## Authentication & User Management

### Mock Authentication System
- **User Accounts**: Pre-configured demo accounts for testing
- **Session Management**: Persistent login sessions
- **Role-based Access**: Different permission levels for different users
- **Secure Storage**: Credentials stored securely using Flutter's secure storage

### Demo Accounts
- **Admin Account**: `admin@flow.dev` / `password123`
- **User Account**: `user@flow.dev` / `userpass`
- **Developer Account**: `dev@flow.dev` / `devpass`

### User Interface
- **Login Screen**: Clean, modern login interface
- **User Menu**: Access to user settings and logout
- **Status Indicators**: Visual feedback for authentication state

## Real-time Communication

### WebSocket Integration
- **Live Updates**: Real-time synchronization across the application
- **Connection Status**: Visual indicators for connection health
- **Automatic Reconnection**: Handles network interruptions gracefully
- **Message Queuing**: Ensures no messages are lost during disconnections

### Mock Server Features
- **File System Sync**: Simulates server-side file operations
- **Graph Data Persistence**: Stores and retrieves graph configurations
- **Authentication Tokens**: Manages user sessions server-side
- **Real-time Notifications**: Push updates to connected clients

## File System Integration

### File Explorer
- **Tree Structure**: Hierarchical file and folder navigation
- **Icon Recognition**: Visual file type indicators
- **Expand/Collapse**: Intuitive folder navigation
- **Context Menus**: Right-click actions for files and folders

### File Operations
- **Open Files**: Double-click to open files in editor
- **Create Files**: Right-click to create new files
- **Delete Files**: Safe file deletion with confirmation
- **Rename Files**: In-place file renaming
- **Move Files**: Drag-and-drop file organization

### Multi-tab Editor
- **Tab Management**: Open multiple files in tabs
- **Tab Switching**: Click tabs or use keyboard shortcuts
- **Unsaved Indicators**: Visual markers for modified files
- **Close Tabs**: Individual tab closing with `Cmd/Ctrl + W`

## Code Editing Features

### Syntax Highlighting
- **Dart**: Full Dart language support
- **JSON**: JSON syntax highlighting and validation
- **YAML**: YAML configuration file support
- **Markdown**: Rich markdown editing with preview
- **Custom Languages**: Extensible syntax highlighting system

### Editor Features
- **Line Numbers**: Optional line number display
- **Code Folding**: Collapsible code blocks
- **Bracket Matching**: Visual bracket pair highlighting
- **Auto-indentation**: Smart indentation based on language
- **Search and Replace**: Find and replace functionality

### File Management
- **Auto-save**: Optional automatic file saving
- **Save Indicators**: Visual feedback for save status
- **File Status**: Modified, saved, and error states
- **Keyboard Shortcuts**: `Cmd/Ctrl + S` for save

## User Interface & Design

### Dark Theme
- **Professional Color Scheme**: Carefully chosen dark color palette
- **High Contrast**: Excellent readability in low-light conditions
- **Consistent Styling**: Unified design language throughout
- **Accessibility**: WCAG compliant color choices

### Animations & Transitions
- **Flutter Animate**: Smooth, performant animations
- **Workspace Transitions**: Elegant switching between modes
- **Loading States**: Visual feedback during operations
- **Micro-interactions**: Subtle animations for better UX

### Responsive Layout
- **Resizable Panels**: Adjustable panel sizes
- **Adaptive UI**: Layout adapts to different screen sizes
- **Touch Support**: Full touch interaction support
- **Keyboard Navigation**: Complete keyboard accessibility

## Status Bar & Information

### Connection Status
- **WebSocket Status**: Real-time connection indicator
- **Server Status**: Server availability indicator
- **Sync Status**: File synchronization status
- **Error Notifications**: Clear error messaging

### File Information
- **Current File**: Currently active file name
- **File Path**: Full path to current file
- **File Size**: File size information
- **Last Modified**: Timestamp of last modification

### System Information
- **Memory Usage**: Application memory consumption
- **Performance Metrics**: Real-time performance data
- **Debug Information**: Development mode information

## Keyboard Shortcuts

### Global Shortcuts
- `Cmd/Ctrl + 1`: Switch to Graph Editor
- `Cmd/Ctrl + 2`: Switch to Code Editor
- `Cmd/Ctrl + S`: Save current file
- `Cmd/Ctrl + W`: Close current tab
- `Cmd/Ctrl + T`: Open new tab
- `Cmd/Ctrl + N`: Create new file

### Editor Shortcuts
- `Cmd/Ctrl + F`: Find in file
- `Cmd/Ctrl + H`: Find and replace
- `Cmd/Ctrl + G`: Go to line
- `Cmd/Ctrl + D`: Duplicate line
- `Cmd/Ctrl + /`: Toggle comment
- `Cmd/Ctrl + Z`: Undo
- `Cmd/Ctrl + Y`: Redo

### Navigation Shortcuts
- `Tab`: Switch between panels
- `Shift + Tab`: Reverse panel navigation
- `Escape`: Close dialogs and menus
- `Enter`: Confirm actions
- `Space`: Toggle selections

## Performance Features

### Optimized Rendering
- **Efficient Widgets**: Optimized Flutter widgets for performance
- **Lazy Loading**: Load content only when needed
- **Memory Management**: Efficient memory usage patterns
- **Smooth Animations**: 60fps animations and transitions

### Caching System
- **File Cache**: Cached file contents for quick access
- **Image Cache**: Cached icons and images
- **State Cache**: Cached application state
- **Network Cache**: Cached network responses

### Background Processing
- **Async Operations**: Non-blocking file operations
- **Background Sync**: File synchronization in background
- **Queue Management**: Efficient task queuing
- **Resource Cleanup**: Automatic resource cleanup

## Extensibility

### Plugin System
- **Custom Widgets**: Add custom UI components
- **File Type Support**: Add support for new file types
- **Syntax Highlighting**: Add syntax highlighting for new languages
- **Keyboard Shortcuts**: Define custom keyboard shortcuts

### Configuration
- **User Preferences**: Customizable user settings
- **Theme Customization**: Custom color schemes
- **Layout Options**: Adjustable panel layouts
- **Feature Toggles**: Enable/disable features

### API Integration
- **WebSocket API**: Real-time communication protocol
- **File System API**: File operation interfaces
- **Authentication API**: User management interfaces
- **Plugin API**: Extension development interfaces

## Security Features

### Data Protection
- **Secure Storage**: Encrypted local storage
- **Session Security**: Secure session management
- **Input Validation**: Comprehensive input validation
- **XSS Protection**: Cross-site scripting prevention

### Network Security
- **HTTPS Only**: Secure communication protocols
- **Token Authentication**: Secure API authentication
- **Request Validation**: Server-side request validation
- **Error Handling**: Secure error reporting

## Accessibility

### Screen Reader Support
- **Semantic Labels**: Proper ARIA labels
- **Focus Management**: Logical focus order
- **Screen Reader Announcements**: Audio feedback
- **High Contrast Mode**: Enhanced visibility

### Keyboard Navigation
- **Tab Order**: Logical tab navigation
- **Keyboard Shortcuts**: Comprehensive keyboard support
- **Focus Indicators**: Clear focus visualization
- **Escape Routes**: Easy navigation exit

### Visual Accessibility
- **Color Contrast**: WCAG compliant contrast ratios
- **Font Scaling**: Support for system font scaling
- **High Contrast**: High contrast mode support
- **Reduced Motion**: Respect user motion preferences
