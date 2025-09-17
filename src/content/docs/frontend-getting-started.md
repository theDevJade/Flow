---
title: Getting Started
description: Setup and installation guide for Flow Frontend
---

# Getting Started with Flow Frontend

This guide will help you set up and run Flow Frontend on your development machine.

## Prerequisites

### Required Software

- **Flutter SDK**: Version 3.9.2 or higher
- **Dart SDK**: Included with Flutter
- **Development Environment**: 
  - macOS (for desktop app development)
  - Windows (for desktop app development)
  - Linux (for desktop app development)
  - Web browser (for web development)

### System Requirements

#### macOS
- macOS 10.14 (Mojave) or later
- Xcode 12 or later (for iOS development)
- 8GB RAM minimum, 16GB recommended
- 2GB free disk space

#### Windows
- Windows 10 version 1903 or later
- Visual Studio 2019 or later with C++ tools
- 8GB RAM minimum, 16GB recommended
- 2GB free disk space

#### Linux
- Ubuntu 18.04 LTS or later
- 8GB RAM minimum, 16GB recommended
- 2GB free disk space

## Installation

### Step 1: Install Flutter

#### macOS
```bash
# Download Flutter SDK
cd ~/development
curl -O https://storage.googleapis.com/flutter_infra_release/releases/stable/macos/flutter_macos_3.9.2-stable.zip

# Extract Flutter
unzip flutter_macos_3.9.2-stable.zip

# Add Flutter to PATH
echo 'export PATH="$PATH:$HOME/development/flutter/bin"' >> ~/.zshrc
source ~/.zshrc
```

#### Windows
1. Download Flutter SDK from [flutter.dev](https://flutter.dev/docs/get-started/install/windows)
2. Extract to `C:\flutter`
3. Add `C:\flutter\bin` to your PATH environment variable

#### Linux
```bash
# Download Flutter SDK
cd ~/development
wget https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.9.2-stable.tar.xz

# Extract Flutter
tar xf flutter_linux_3.9.2-stable.tar.xz

# Add Flutter to PATH
echo 'export PATH="$PATH:$HOME/development/flutter/bin"' >> ~/.bashrc
source ~/.bashrc
```

### Step 2: Verify Flutter Installation

```bash
flutter doctor
```

This command will check your Flutter installation and show any missing dependencies.

### Step 3: Install Dependencies

```bash
# Navigate to project directory
cd flow-frontend

# Install Flutter dependencies
flutter pub get

# Install additional dependencies (if any)
flutter pub upgrade
```

### Step 4: Configure Development Environment

#### For Desktop Development
```bash
# Enable desktop support
flutter config --enable-macos-desktop
flutter config --enable-windows-desktop
flutter config --enable-linux-desktop
```

#### For Web Development
```bash
# Enable web support
flutter config --enable-web
```

## Running the Application

### Desktop Application

#### macOS
```bash
flutter run -d macos
```

#### Windows
```bash
flutter run -d windows
```

#### Linux
```bash
flutter run -d linux
```

### Web Application

```bash
flutter run -d chrome
```

### Mobile Application (if supported)

#### iOS Simulator
```bash
flutter run -d ios
```

#### Android Emulator
```bash
flutter run -d android
```

## Development Setup

### IDE Configuration

#### Visual Studio Code
1. Install Flutter extension
2. Install Dart extension
3. Configure launch.json for debugging

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Flow Frontend (macOS)",
      "type": "dart",
      "request": "launch",
      "program": "lib/main.dart",
      "deviceId": "macos",
      "args": ["--dart-define=ENVIRONMENT=development"]
    },
    {
      "name": "Flow Frontend (Web)",
      "type": "dart",
      "request": "launch",
      "program": "lib/main.dart",
      "deviceId": "chrome",
      "args": ["--dart-define=ENVIRONMENT=development"]
    }
  ]
}
```

#### Android Studio
1. Install Flutter plugin
2. Install Dart plugin
3. Configure run configurations

### Environment Configuration

Create a `.env` file in the project root:

```env
# Development environment
ENVIRONMENT=development

# WebSocket server URL
WEBSOCKET_URL=ws://localhost:8080/ws

# API base URL
API_BASE_URL=http://localhost:8080/api

# Debug mode
DEBUG_MODE=true

# Log level
LOG_LEVEL=debug
```

### Build Configuration

#### Debug Build
```bash
# Desktop
flutter build macos --debug
flutter build windows --debug
flutter build linux --debug

# Web
flutter build web --debug
```

#### Release Build
```bash
# Desktop
flutter build macos --release
flutter build windows --release
flutter build linux --release

# Web
flutter build web --release
```

## Project Structure

```
flow-frontend/
├── lib/
│   ├── main.dart                 # Application entry point
│   ├── app.dart                  # Main app widget
│   ├── state/                    # State management
│   │   ├── app_state.dart
│   │   ├── workspace_state.dart
│   │   ├── file_system_state.dart
│   │   └── auth_state.dart
│   ├── services/                 # Business logic services
│   │   ├── auth_service.dart
│   │   ├── websocket_service.dart
│   │   ├── file_service.dart
│   │   └── graph_service.dart
│   ├── screens/                  # Main UI screens
│   │   ├── main_screen.dart
│   │   ├── login_screen.dart
│   │   ├── code_editor_screen.dart
│   │   └── graph_editor_screen.dart
│   ├── widgets/                  # Reusable UI components
│   │   ├── workspace_top_bar.dart
│   │   ├── file_tree_view.dart
│   │   ├── tabbed_code_editor.dart
│   │   └── status_bar.dart
│   ├── models/                   # Data models
│   │   ├── user.dart
│   │   ├── file_tab.dart
│   │   └── websocket_message.dart
│   ├── utils/                    # Utility functions
│   │   ├── constants.dart
│   │   ├── helpers.dart
│   │   └── validators.dart
│   └── themes/                   # Styling and themes
│       ├── app_theme.dart
│       ├── colors.dart
│       └── text_styles.dart
├── test/                         # Test files
│   ├── unit/
│   ├── widget/
│   └── integration/
├── web/                          # Web-specific files
│   ├── index.html
│   └── favicon.ico
├── macos/                        # macOS-specific files
├── windows/                      # Windows-specific files
├── linux/                        # Linux-specific files
├── android/                      # Android-specific files
├── ios/                          # iOS-specific files
├── pubspec.yaml                  # Dependencies and metadata
├── pubspec.lock                  # Locked dependency versions
└── README.md                     # Project documentation
```

## Dependencies

### Core Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # State management
  provider: ^6.0.5
  
  # UI components
  cupertino_icons: ^1.0.2
  flutter_animate: ^4.2.0
  
  # Code editing
  code_text_field: ^1.0.0
  
  # Networking
  web_socket_channel: ^2.4.0
  http: ^1.1.0
  
  # Storage
  shared_preferences: ^2.2.0
  path_provider: ^2.1.0
  
  # Utilities
  json_annotation: ^4.8.1
  intl: ^0.18.1
```

### Dev Dependencies

```yaml
dev_dependencies:
  flutter_test:
    sdk: flutter
  
  # Testing
  mockito: ^5.4.2
  integration_test:
    sdk: flutter
  
  # Code generation
  build_runner: ^2.4.6
  json_serializable: ^6.7.1
  
  # Linting
  flutter_lints: ^3.0.0
```

## Configuration

### Flutter Configuration

```bash
# Check Flutter configuration
flutter config

# Enable specific platforms
flutter config --enable-macos-desktop
flutter config --enable-windows-desktop
flutter config --enable-linux-desktop
flutter config --enable-web

# Set up Android SDK (for Android development)
flutter config --android-sdk /path/to/android/sdk

# Set up iOS development (macOS only)
flutter config --ios-sdk /path/to/ios/sdk
```

### IDE Configuration

#### VS Code Settings
Create `.vscode/settings.json`:

```json
{
  "dart.flutterSdkPath": "/path/to/flutter",
  "dart.lineLength": 120,
  "editor.rulers": [120],
  "editor.formatOnSave": true,
  "dart.enableSdkFormatter": true,
  "dart.insertArgumentPlaceholders": false
}
```

#### Android Studio Settings
1. Go to File → Settings → Languages & Frameworks → Flutter
2. Set Flutter SDK path
3. Enable Dart SDK
4. Configure code style and formatting

## Troubleshooting

### Common Issues

#### Flutter Doctor Issues
```bash
# Run Flutter doctor to identify issues
flutter doctor -v

# Fix specific issues
flutter doctor --android-licenses
flutter config --android-sdk /path/to/android/sdk
```

#### Build Issues
```bash
# Clean build cache
flutter clean
flutter pub get

# Rebuild
flutter build macos --debug
```

#### Web Issues
```bash
# Clear web build cache
flutter clean
flutter build web --web-renderer html
```

#### Desktop Issues
```bash
# Enable desktop support
flutter config --enable-macos-desktop
flutter config --enable-windows-desktop
flutter config --enable-linux-desktop

# Rebuild
flutter build macos --debug
```

### Performance Issues

#### Slow Build Times
```bash
# Use build cache
flutter build macos --debug --build-cache

# Parallel builds
flutter build macos --debug --build-cache --parallel
```

#### Memory Issues
```bash
# Increase memory limit
export FLUTTER_BUILD_MEMORY_LIMIT=8192
flutter build macos --debug
```

## Next Steps

Once you have Flow Frontend running:

1. **Explore the Interface**: Familiarize yourself with the dual workspace architecture
2. **Try the Features**: Test file operations, code editing, and real-time features
3. **Read the Documentation**: Check out the [Features](/frontend/features) and [Architecture](/frontend/architecture) guides
4. **Start Developing**: Follow the [Development](/frontend/development) guide for contributing

## Support

If you encounter issues:

1. Check the [Troubleshooting](#troubleshooting) section above
2. Review the Flutter documentation
3. Check the project's issue tracker
4. Ask for help in the community forums
