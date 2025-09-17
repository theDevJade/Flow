---
title: Development
description: Development guidelines and best practices for Flow Frontend
---

# Flow Frontend Development

This guide covers development guidelines, best practices, and contribution guidelines for Flow Frontend.

## Development Guidelines

### Code Style

#### Dart/Flutter Style Guide
Follow the official Dart style guide and Flutter conventions:

```dart
// Use camelCase for variables and methods
String userName = 'john_doe';
void updateUserProfile() {}

// Use PascalCase for classes
class UserProfileWidget extends StatelessWidget {}

// Use SCREAMING_SNAKE_CASE for constants
const String API_BASE_URL = 'https://api.example.com';

// Use descriptive names
final List<FileTab> openTabs = [];
final bool isUserAuthenticated = false;

// Use meaningful comments
/// Updates the user profile with new information
/// 
/// [userData] contains the updated user information
/// Returns true if the update was successful
Future<bool> updateUserProfile(Map<String, dynamic> userData) async {
  // Implementation here
}
```

#### File Organization
- One class per file
- Group related functionality
- Use meaningful file names
- Follow the established directory structure

#### Import Organization
```dart
// Dart SDK imports
import 'dart:async';
import 'dart:convert';

// Flutter imports
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// Third-party package imports
import 'package:provider/provider.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

// Local imports
import '../models/user.dart';
import '../services/auth_service.dart';
import '../widgets/user_profile_widget.dart';
```

### State Management Best Practices

#### Provider Usage
```dart
// Use Consumer for specific state changes
Consumer<FileSystemState>(
  builder: (context, fileSystemState, child) {
    return ListView.builder(
      itemCount: fileSystemState.openTabs.length,
      itemBuilder: (context, index) {
        return TabWidget(
          tab: fileSystemState.openTabs[index],
          onClose: () => fileSystemState.closeTab(index),
        );
      },
    );
  },
);

// Use Selector for specific property changes
Selector<FileSystemState, String?>(
  selector: (context, state) => state.activeTabId,
  builder: (context, activeTabId, child) {
    return Text('Active tab: $activeTabId');
  },
);
```

#### State Updates
```dart
class FileSystemState extends ChangeNotifier {
  List<FileTab> _openTabs = [];
  
  // Use private setters with notifyListeners()
  void _setOpenTabs(List<FileTab> tabs) {
    _openTabs = tabs;
    notifyListeners();
  }
  
  // Batch multiple updates
  void openMultipleFiles(List<FileTreeItem> files) {
    final newTabs = files.map((file) => FileTab.fromFile(file)).toList();
    _setOpenTabs([..._openTabs, ...newTabs]);
  }
  
  // Use async methods properly
  Future<void> loadFileContent(String filePath) async {
    try {
      final content = await FileService.readFile(filePath);
      _updateFileContent(filePath, content);
    } catch (e) {
      _handleError('Failed to load file: $e');
    }
  }
}
```

### Widget Development

#### StatelessWidget Guidelines
```dart
class UserProfileWidget extends StatelessWidget {
  final User user;
  final VoidCallback? onEdit;
  final bool showActions;
  
  const UserProfileWidget({
    Key? key,
    required this.user,
    this.onEdit,
    this.showActions = true,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              user.displayName,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(
              user.email,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            if (showActions) ...[
              const SizedBox(height: 16),
              Row(
                children: [
                  ElevatedButton(
                    onPressed: onEdit,
                    child: const Text('Edit'),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
```

#### StatefulWidget Guidelines
```dart
class FileEditorWidget extends StatefulWidget {
  final FileTab fileTab;
  final ValueChanged<String>? onContentChanged;
  
  const FileEditorWidget({
    Key? key,
    required this.fileTab,
    this.onContentChanged,
  }) : super(key: key);
  
  @override
  State<FileEditorWidget> createState() => _FileEditorWidgetState();
}

class _FileEditorWidgetState extends State<FileEditorWidget> {
  late TextEditingController _controller;
  bool _isModified = false;
  
  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.fileTab.content);
    _controller.addListener(_onTextChanged);
  }
  
  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
  
  void _onTextChanged() {
    if (_controller.text != widget.fileTab.content) {
      setState(() {
        _isModified = true;
      });
      widget.onContentChanged?.call(_controller.text);
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (_isModified)
          Container(
            color: Colors.orange,
            child: const Text('Unsaved changes'),
          ),
        Expanded(
          child: TextField(
            controller: _controller,
            maxLines: null,
            expands: true,
            decoration: const InputDecoration(
              border: InputBorder.none,
            ),
          ),
        ),
      ],
    );
  }
}
```

### Service Layer Development

#### Service Interface
```dart
abstract class AuthService {
  Future<AuthResult> login(String email, String password);
  Future<void> logout();
  bool isAuthenticated();
  User? getCurrentUser();
}

class AuthServiceImpl implements AuthService {
  final SharedPreferences _prefs;
  final WebSocketService _webSocketService;
  
  AuthServiceImpl(this._prefs, this._webSocketService);
  
  @override
  Future<AuthResult> login(String email, String password) async {
    // Implementation
  }
  
  // Other methods...
}
```

#### Error Handling
```dart
class FileService {
  static const String _errorPrefix = 'FileService';
  
  Future<String> readFile(String filePath) async {
    try {
      final file = File(filePath);
      if (!await file.exists()) {
        throw FileNotFoundException('File not found: $filePath');
      }
      
      final content = await file.readAsString();
      return content;
    } on FileSystemException catch (e) {
      throw FileServiceException(
        'Failed to read file: ${e.message}',
        originalError: e,
      );
    } catch (e) {
      throw FileServiceException(
        'Unexpected error reading file: $e',
        originalError: e,
      );
    }
  }
}

class FileServiceException implements Exception {
  final String message;
  final dynamic originalError;
  
  FileServiceException(this.message, {this.originalError});
  
  @override
  String toString() => 'FileServiceException: $message';
}
```

### Testing Guidelines

#### Unit Tests
```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:mockito/annotations.dart';

import 'package:flow_frontend/services/auth_service.dart';
import 'package:flow_frontend/models/user.dart';

@GenerateMocks([SharedPreferences, WebSocketService])
void main() {
  group('AuthService', () {
    late AuthService authService;
    late MockSharedPreferences mockPrefs;
    late MockWebSocketService mockWebSocket;
    
    setUp(() {
      mockPrefs = MockSharedPreferences();
      mockWebSocket = MockWebSocketService();
      authService = AuthServiceImpl(mockPrefs, mockWebSocket);
    });
    
    test('should login successfully with valid credentials', () async {
      // Arrange
      when(mockPrefs.setString(any, any)).thenAnswer((_) async => true);
      
      // Act
      final result = await authService.login('test@example.com', 'password');
      
      // Assert
      expect(result.isSuccess, true);
      expect(result.user?.email, 'test@example.com');
    });
    
    test('should fail login with invalid credentials', () async {
      // Act
      final result = await authService.login('invalid@example.com', 'wrong');
      
      // Assert
      expect(result.isSuccess, false);
      expect(result.error, 'Invalid credentials');
    });
  });
}
```

#### Widget Tests
```dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:flow_frontend/widgets/user_profile_widget.dart';
import 'package:flow_frontend/models/user.dart';

void main() {
  group('UserProfileWidget', () {
    testWidgets('should display user information', (WidgetTester tester) async {
      // Arrange
      final user = User(
        id: '1',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'user',
        createdAt: DateTime.now(),
      );
      
      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: UserProfileWidget(user: user),
        ),
      );
      
      // Assert
      expect(find.text('Test User'), findsOneWidget);
      expect(find.text('test@example.com'), findsOneWidget);
    });
    
    testWidgets('should call onEdit when edit button is pressed', (WidgetTester tester) async {
      // Arrange
      final user = User(
        id: '1',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'user',
        createdAt: DateTime.now(),
      );
      bool editCalled = false;
      
      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: UserProfileWidget(
            user: user,
            onEdit: () => editCalled = true,
          ),
        ),
      );
      
      await tester.tap(find.text('Edit'));
      await tester.pump();
      
      // Assert
      expect(editCalled, true);
    });
  });
}
```

#### Integration Tests
```dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:flow_frontend/main.dart' as app;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  
  group('Flow Frontend Integration Tests', () {
    testWidgets('should complete login flow', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();
      
      // Navigate to login
      expect(find.text('Login'), findsOneWidget);
      
      // Enter credentials
      await tester.enterText(find.byKey(Key('email_field')), 'admin@flow.dev');
      await tester.enterText(find.byKey(Key('password_field')), 'password123');
      
      // Tap login button
      await tester.tap(find.byKey(Key('login_button')));
      await tester.pumpAndSettle();
      
      // Verify successful login
      expect(find.text('Welcome'), findsOneWidget);
    });
  });
}
```

### Performance Best Practices

#### Widget Optimization
```dart
// Use const constructors
const Text('Hello World');

// Use RepaintBoundary for expensive widgets
RepaintBoundary(
  child: ExpensiveWidget(),
);

// Use ListView.builder for large lists
ListView.builder(
  itemCount: items.length,
  itemBuilder: (context, index) {
    return ListTile(
      title: Text(items[index].title),
    );
  },
);

// Use AutomaticKeepAliveClientMixin for stateful widgets
class _ExpensiveWidgetState extends State<ExpensiveWidget>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;
  
  @override
  Widget build(BuildContext context) {
    super.build(context);
    return ExpensiveContent();
  }
}
```

#### Memory Management
```dart
class FileEditorWidget extends StatefulWidget {
  @override
  State<FileEditorWidget> createState() => _FileEditorWidgetState();
}

class _FileEditorWidgetState extends State<FileEditorWidget> {
  late TextEditingController _controller;
  late StreamSubscription _subscription;
  
  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
    _subscription = _controller.stream.listen(_onTextChanged);
  }
  
  @override
  void dispose() {
    _controller.dispose();
    _subscription.cancel();
    super.dispose();
  }
  
  void _onTextChanged(String text) {
    // Handle text changes
  }
}
```

### Security Best Practices

#### Input Validation
```dart
class InputValidator {
  static String? validateEmail(String? value) {
    if (value == null || value.isEmpty) {
      return 'Email is required';
    }
    
    final emailRegex = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
    if (!emailRegex.hasMatch(value)) {
      return 'Please enter a valid email';
    }
    
    return null;
  }
  
  static String? validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Password is required';
    }
    
    if (value.length < 8) {
      return 'Password must be at least 8 characters';
    }
    
    return null;
  }
}
```

#### Secure Storage
```dart
class SecureStorageService {
  static const String _tokenKey = 'auth_token';
  
  Future<void> storeToken(String token) async {
    // Use secure storage for sensitive data
    await FlutterSecureStorage().write(key: _tokenKey, value: token);
  }
  
  Future<String?> getToken() async {
    return await FlutterSecureStorage().read(key: _tokenKey);
  }
  
  Future<void> deleteToken() async {
    await FlutterSecureStorage().delete(key: _tokenKey);
  }
}
```

## Contribution Guidelines

### Pull Request Process

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Write tests** for your changes
5. **Run tests** to ensure they pass
   ```bash
   flutter test
   flutter test integration_test/
   ```
6. **Commit your changes**
   ```bash
   git commit -m "Add your feature description"
   ```
7. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
8. **Create a Pull Request**

### Code Review Guidelines

#### For Reviewers
- Check code style and conventions
- Verify tests are included and passing
- Ensure documentation is updated
- Test the changes manually
- Provide constructive feedback

#### For Authors
- Respond to feedback promptly
- Make requested changes
- Update documentation as needed
- Ensure all tests pass
- Address all review comments

### Issue Reporting

#### Bug Reports
Include the following information:
- **Description**: Clear description of the bug
- **Steps to Reproduce**: Detailed steps to reproduce
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**: OS, Flutter version, device
- **Screenshots**: If applicable

#### Feature Requests
Include the following information:
- **Description**: Clear description of the feature
- **Use Case**: Why this feature is needed
- **Proposed Solution**: How you think it should work
- **Alternatives**: Other solutions considered

### Development Workflow

#### Daily Workflow
1. **Pull latest changes**
   ```bash
   git pull origin main
   ```
2. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature
   ```
3. **Make changes and test**
4. **Commit changes**
   ```bash
   git add .
   git commit -m "Your commit message"
   ```
5. **Push and create PR**

#### Release Workflow
1. **Update version numbers**
2. **Update CHANGELOG.md**
3. **Run full test suite**
4. **Create release branch**
5. **Tag release**
6. **Deploy to production**

## Documentation

### Code Documentation
- Use Dart doc comments for public APIs
- Include examples in documentation
- Keep documentation up to date
- Use meaningful parameter names

### README Updates
- Update installation instructions
- Add new features to feature list
- Update dependencies
- Include troubleshooting information

### API Documentation
- Document all public methods
- Include parameter descriptions
- Provide usage examples
- Document return values

## Troubleshooting

### Common Development Issues

#### Build Issues
```bash
# Clean and rebuild
flutter clean
flutter pub get
flutter build macos --debug
```

#### Test Issues
```bash
# Run specific tests
flutter test test/unit/auth_service_test.dart

# Run with verbose output
flutter test --verbose
```

#### Performance Issues
```bash
# Profile the app
flutter run --profile

# Analyze performance
flutter analyze
```

### Debugging Tips

#### Flutter Inspector
- Use Flutter Inspector to debug widget tree
- Check widget properties and state
- Identify performance bottlenecks

#### Logging
```dart
import 'package:flutter/foundation.dart';

void debugLog(String message) {
  if (kDebugMode) {
    print('DEBUG: $message');
  }
}
```

#### Error Handling
```dart
try {
  // Risky operation
} catch (e, stackTrace) {
  debugLog('Error: $e');
  debugLog('Stack trace: $stackTrace');
  // Handle error appropriately
}
```

This development guide provides comprehensive guidelines for contributing to Flow Frontend. Follow these practices to ensure code quality, maintainability, and consistency across the project.
