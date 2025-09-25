import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class AuthUser {
  final String id;
  final String username;
  final String token;
  final int expiresAt;

  AuthUser({
    required this.id,
    required this.username,
    required this.token,
    required this.expiresAt,
  });

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: json['userId'] as String,
      username: json['username'] as String,
      token: json['token'] as String,
      expiresAt: json['expiresAt'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'userId': id,
      'username': username,
      'token': token,
      'expiresAt': expiresAt,
    };
  }

  bool get isExpired {
    return DateTime.now().millisecondsSinceEpoch > expiresAt;
  }
}

class AuthService extends ChangeNotifier {
  static AuthService? _instance;
  static AuthService get instance => _instance ??= AuthService._();

  AuthService._();
  static const String _tokenKey = 'auth_token';
  static const String _userDataKey = 'user_data';

  AuthUser? _currentUser;
  final List<Function(AuthUser?)> _authListeners = [];

  AuthUser? get currentUser => _currentUser;
  bool get isAuthenticated => _currentUser != null && !_currentUser!.isExpired;
  String? get authToken => _currentUser?.token;


  void addAuthListener(Function(AuthUser?) listener) {
    _authListeners.add(listener);
  }


  void removeAuthListener(Function(AuthUser?) listener) {
    _authListeners.remove(listener);
  }


  void _notifyAuthListeners() {
    notifyListeners();
    for (final listener in _authListeners) {
      listener(_currentUser);
    }
  }


  Future<void> initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final userData = prefs.getString(_userDataKey);

    if (userData != null) {
      try {
        final userMap = jsonDecode(userData) as Map<String, dynamic>;
        final user = AuthUser.fromJson(userMap);

        if (!user.isExpired) {
          _currentUser = user;
          print('AuthService: Restored user session for ${user.username}');
        } else {
          print('AuthService: Stored token expired, clearing session');
          await logout();
        }
      } catch (e) {
        print('AuthService: Error restoring user session: $e');
        await logout();
      }
    }

    _notifyAuthListeners();
  }


  Future<LoginResult> login(
    String serverUrl,
    String username,
    String password,
  ) async {
    try {
      print('AuthService: Attempting login for user: $username');

      final response = await http.post(
        Uri.parse('$serverUrl/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'username': username, 'password': password}),
      );

      final responseData = jsonDecode(response.body) as Map<String, dynamic>;

      if (response.statusCode == 200 && responseData['success'] == true) {
        final user = AuthUser.fromJson(responseData);
        _currentUser = user;


        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(_tokenKey, user.token);
        await prefs.setString(_userDataKey, jsonEncode(user.toJson()));

        print('AuthService: Login successful for ${user.username}');
        _notifyAuthListeners();

        return LoginResult.success(user);
      } else {
        final message = responseData['message'] as String? ?? 'Login failed';
        print('AuthService: Login failed: $message');
        return LoginResult.failure(message);
      }
    } catch (e) {
      print('AuthService: Login error: $e');
      return LoginResult.failure('Network error: $e');
    }
  }


  Future<void> logout() async {
    if (_currentUser != null) {
      print('AuthService: Logging out user: ${_currentUser!.username}');
    }

    _currentUser = null;

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userDataKey);

    _notifyAuthListeners();
  }


  Future<bool> validateToken(String serverUrl) async {
    if (_currentUser == null || _currentUser!.isExpired) {
      return false;
    }

    try {
      final response = await http.get(
        Uri.parse('$serverUrl/auth/validate'),
        headers: {'Authorization': 'Bearer ${_currentUser!.token}'},
      );

      if (response.statusCode == 200) {
        final responseData = jsonDecode(response.body) as Map<String, dynamic>;
        return responseData['valid'] == true;
      } else {
        print('AuthService: Token validation failed');
        await logout();
        return false;
      }
    } catch (e) {
      print('AuthService: Token validation error: $e');
      return false;
    }
  }


  String? getWebSocketAuthQuery() {
    if (_currentUser == null || _currentUser!.isExpired) {
      return null;
    }
    return 'token=${_currentUser!.token}';
  }


  Map<String, String>? getAuthHeaders() {
    if (_currentUser == null || _currentUser!.isExpired) {
      return null;
    }
    return {'Authorization': 'Bearer ${_currentUser!.token}'};
  }
}

class LoginResult {
  final bool success;
  final String? error;
  final AuthUser? user;

  LoginResult._({required this.success, this.error, this.user});

  factory LoginResult.success(AuthUser user) {
    return LoginResult._(success: true, user: user);
  }

  factory LoginResult.failure(String error) {
    return LoginResult._(success: false, error: error);
  }
}
