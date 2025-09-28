import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/status.dart' as socket_status;

enum WebSocketConnectionStatus {
  disconnected,
  connecting,
  connected,
  reconnecting,
  error,
}

class WebSocketMessage {
  final String type;
  final String? id;
  final Map<String, dynamic> data;
  final DateTime timestamp;
  final String? userId;

  WebSocketMessage({
    required this.type,
    this.id,
    required this.data,
    DateTime? timestamp,
    this.userId,
  }) : timestamp = timestamp ?? DateTime.now();

  factory WebSocketMessage.fromJson(Map<String, dynamic> json) {
    return WebSocketMessage(
      type: json['type'],
      id: json['id'],
      data: json['data'] ?? {},
      timestamp: DateTime.tryParse(json['timestamp']) ?? DateTime.now(),
      userId: json['userId'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'id': id,
      'data': data,
      'timestamp': timestamp.toIso8601String(),
      'userId': userId,
    };
  }
}

class WebSocketLogEntry {
  final DateTime timestamp;
  final String type;
  final String message;
  final Map<String, dynamic>? data;
  final bool isError;

  WebSocketLogEntry({
    required this.type,
    required this.message,
    this.data,
    this.isError = false,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  String get formattedTime {
    final time = timestamp;
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:${time.second.toString().padLeft(2, '0')}.${time.millisecond.toString().padLeft(3, '0')}';
  }
}

class WebSocketService with ChangeNotifier {
  static WebSocketService? _instance;
  static WebSocketService get instance {

    if (_instance != null && _instance!._isDisposed) {
      _instance = null;
    }
    return _instance ??= WebSocketService._();
  }

  WebSocketService._();

  WebSocketChannel? _channel;
  Timer? _heartbeatTimer;
  Timer? _reconnectTimer;

  final StreamController<WebSocketConnectionStatus> _statusController =
      StreamController<WebSocketConnectionStatus>.broadcast();
  final StreamController<WebSocketMessage> _messageController =
      StreamController<WebSocketMessage>.broadcast();

  Stream<WebSocketConnectionStatus> get status => _statusController.stream;
  Stream<WebSocketMessage> get messages => _messageController.stream;

  WebSocketConnectionStatus _currentStatus =
      WebSocketConnectionStatus.disconnected;
  WebSocketConnectionStatus get currentStatus => _currentStatus;


  final List<WebSocketLogEntry> _logs = [];
  static const int maxLogEntries = 1000;


  final List<WebSocketMessage> _messageQueue = [];
  static const int maxQueuedMessages = 50;

  List<WebSocketLogEntry> get logs => List.unmodifiable(_logs);

  String? _token;
  String? _serverUrl;
  bool _isDisposing = false;
  bool _isDisposed = false;
  bool _isConnecting = false;
  int _reconnectAttempts = 0;
  static const int maxReconnectAttempts = 5;
  static const Duration reconnectDelay = Duration(seconds: 2);
  static const Duration heartbeatInterval = Duration(seconds: 30);
  static const Duration connectionTimeout = Duration(seconds: 30);

  void _addLog(
    String type,
    String message, {
    Map<String, dynamic>? data,
    bool isError = false,
  }) {

    if (_isDisposing) return;

    final logEntry = WebSocketLogEntry(
      type: type,
      message: message,
      data: data,
      isError: isError,
    );

    _logs.add(logEntry);


    if (_logs.length > maxLogEntries) {
      _logs.removeAt(0);
    }

    debugPrint('WebSocket[$type]: $message');

    if (!_isDisposing) {
      notifyListeners();
    }
  }

  void clearLogs() {
    _logs.clear();
    _addLog('SYSTEM', 'Logs cleared');
  }

  void configureServer(String host, int port) {
    _serverUrl = 'ws://$host:$port/ws';
    _addLog('CONFIG', 'Server configured: $_serverUrl');
    debugPrint('WebSocket configured for: $_serverUrl');
  }

  Future<void> connect(String? token) async {
    if (_isDisposing || _isConnecting) return;


    if (_currentStatus == WebSocketConnectionStatus.connected) {
      if (_token == token) return;

      disconnect();
    }

    _isConnecting = true;
    _token = token;
    _addLog('CONNECTION', 'Attempting to connect...');
    _setStatus(WebSocketConnectionStatus.connecting);

    try {
      await _connectToRealServer();
      _reconnectAttempts = 0;
    } catch (e) {
      _addLog('CONNECTION', 'Connection failed: $e', isError: true);
      debugPrint('WebSocket connection error: $e');
      _setStatus(WebSocketConnectionStatus.error);
      _scheduleReconnect();
    } finally {
      _isConnecting = false;
    }
  }

  Future<void> _connectToRealServer() async {
    if (_serverUrl == null) {
      throw Exception('Server URL not configured');
    }

    try {
      await _cleanup();

      String wsUrl = _serverUrl!;
      if (_token != null && _token!.isNotEmpty) {
        final uri = Uri.parse(_serverUrl!);
        final newUri = uri.replace(queryParameters: {'token': _token!});
        wsUrl = newUri.toString();
      }

      _addLog(
        'CONNECTION',
        'Connecting to $wsUrl',
        data: {'hasToken': _token != null},
      );
      debugPrint(
        'Connecting to WebSocket server: $wsUrl (token: ${_token != null ? 'provided' : 'none'})',
      );

      _channel = WebSocketChannel.connect(Uri.parse(wsUrl));

      _channel!.stream.listen(
        _handleRealMessage,
        onError: (error) {
          _addLog('CONNECTION', 'Stream error: $error', isError: true);
          debugPrint('WebSocket stream error: $error');
          _handleConnectionError(error);
        },
        onDone: () {
          _addLog('CONNECTION', 'Connection closed by server');
          debugPrint('WebSocket connection closed');
          _handleConnectionClosed();
        },
      );

      // Send authentication message if token is available
      if (_token != null && _token!.isNotEmpty) {
        final authMessage = WebSocketMessage(
          type: 'auth',
          data: {
            'token': _token!,
            'userId': 'user_${DateTime.now().millisecondsSinceEpoch}',
            'username': 'user',
          },
        );

        _addLog('AUTH', 'Sending authentication message');
        _channel!.sink.add(jsonEncode(authMessage.toJson()));
      }

      await Future.delayed(const Duration(milliseconds: 500)).timeout(
        connectionTimeout,
        onTimeout: () {
          throw TimeoutException('Connection timeout', connectionTimeout);
        },
      );

      _setStatus(WebSocketConnectionStatus.connected);
      _reconnectAttempts = 0;

      _addLog('CONNECTION', 'Connected successfully');
      debugPrint('WebSocket connected successfully');
      _startHeartbeat();

      // Process any queued messages
      _processMessageQueue();
    } catch (e) {
      _addLog('CONNECTION', 'Failed to connect: $e', isError: true);
      debugPrint('Failed to connect to WebSocket server: $e');
      rethrow;
    }
  }

  void _handleRealMessage(dynamic message) {
    try {
      final Map<String, dynamic> json = jsonDecode(message.toString());
      final wsMessage = WebSocketMessage.fromJson(json);
      _addLog('MESSAGE_IN', '${wsMessage.type}', data: wsMessage.data);
      _messageController.add(wsMessage);
    } catch (e) {
      _addLog('MESSAGE_IN', 'Parse error: $e', isError: true);
      debugPrint('Error parsing WebSocket message: $e');
    }
  }

  void _handleConnectionError(dynamic error) {
    _addLog('CONNECTION', 'Connection error: $error', isError: true);
    debugPrint('WebSocket connection error: $error');
    _setStatus(WebSocketConnectionStatus.error);
    _scheduleReconnect();
  }

  void _handleConnectionClosed() {
    if (_currentStatus != WebSocketConnectionStatus.disconnected &&
        !_isDisposing) {
      _addLog('CONNECTION', 'Connection unexpectedly closed, reconnecting...');
      debugPrint(
        'WebSocket connection unexpectedly closed, attempting to reconnect',
      );
      _setStatus(WebSocketConnectionStatus.reconnecting);
      _scheduleReconnect();
    }
  }

  void sendMessage(String type, Map<String, dynamic> data) {
    final message = WebSocketMessage(type: type, data: data);
    send(message);
  }

  void send(WebSocketMessage message) {
    if (_currentStatus == WebSocketConnectionStatus.connected) {
      try {
        if (_channel?.sink != null) {
          final jsonMessage = jsonEncode(message.toJson());
          _channel!.sink.add(jsonMessage);
          _addLog('MESSAGE_OUT', '${message.type}', data: message.data);
          debugPrint('Sent WebSocket message: ${message.type}');
        } else {
          _addLog(
            'MESSAGE_OUT',
            'Failed to send ${message.type}: Channel not available',
            isError: true,
          );
          debugPrint('Cannot send message: WebSocket channel not available');

          // Trigger reconnection when channel is not available
          _setStatus(WebSocketConnectionStatus.error);
          _scheduleReconnect();
        }
      } catch (e) {
        _addLog(
          'MESSAGE_OUT',
          'Failed to send ${message.type}: $e',
          isError: true,
        );
        debugPrint('Error sending WebSocket message: $e');
      }
    } else {
      _addLog(
        'MESSAGE_OUT',
        'Cannot send ${message.type}: Not connected (${_currentStatus.name})',
        data: message.data,
        isError: true,
      );
      debugPrint(
        'Cannot send message type: ${message.type} with data: ${message.data}. WebSocket not connected (status: $_currentStatus)',
      );

      // Store message to be sent once reconnected
      if (_messageQueue.length < maxQueuedMessages) {
        _messageQueue.add(message);
        debugPrint(
          'Added message to queue for later delivery (${_messageQueue.length} pending messages)',
        );
      } else {
        debugPrint('Message queue full, dropping message: ${message.type}');
      }

      // Attempt to reconnect if we have a token and are not already reconnecting
      if (_token != null &&
          _currentStatus != WebSocketConnectionStatus.connecting &&
          _currentStatus != WebSocketConnectionStatus.reconnecting) {
        _addLog('RECONNECT', 'Attempting to reconnect due to send failure');
        _setStatus(WebSocketConnectionStatus.reconnecting);
        _scheduleReconnect();
      }
    }
  }

  void _startHeartbeat() {
    _heartbeatTimer?.cancel();
    _addLog('HEARTBEAT', 'Starting heartbeat timer');
    _heartbeatTimer = Timer.periodic(heartbeatInterval, (timer) {
      if (_currentStatus == WebSocketConnectionStatus.connected) {
        try {
          if (_channel?.sink != null) {
            _channel!.sink.add(
              '{"type":"ping","data":{"timestamp":"${DateTime.now().toIso8601String()}"}}',
            );
          }

          send(
            WebSocketMessage(
              type: 'heartbeat',
              data: {'timestamp': DateTime.now().toIso8601String()},
            ),
          );
          _addLog('HEARTBEAT', 'Heartbeat sent');
        } catch (e) {
          _addLog('HEARTBEAT', 'Heartbeat failed: $e', isError: true);
          debugPrint('Error sending heartbeat: $e');
          _handleConnectionError(e);
        }
      } else {
        _addLog('HEARTBEAT', 'Stopping heartbeat: Not connected');
        timer.cancel();
      }
    });
  }

  void _setStatus(WebSocketConnectionStatus status) {
    if (_currentStatus != status) {
      final oldStatus = _currentStatus;
      _currentStatus = status;
      _addLog('STATUS', 'Status changed: ${oldStatus.name} → ${status.name}');
      _statusController.add(status);
      notifyListeners();
    }
  }

  void _scheduleReconnect() {
    if (_isDisposing || _reconnectAttempts >= maxReconnectAttempts) {
      _addLog(
        'RECONNECT',
        'Max reconnection attempts reached or service is disposing',
        isError: true,
      );
      debugPrint('Max reconnection attempts reached or service is disposing');
      _setStatus(WebSocketConnectionStatus.error);

      // Notify listeners that reconnection attempts are exhausted
      if (_reconnectAttempts >= maxReconnectAttempts) {
        _addLog(
          'AUTH',
          'Reconnection attempts exhausted - authentication may be required',
          isError: true,
        );
      }
      return;
    }

    _reconnectTimer?.cancel();
    _reconnectAttempts++;

    _addLog(
      'RECONNECT',
      'Scheduling reconnect attempt $_reconnectAttempts/$maxReconnectAttempts',
    );

    final baseDelay = Duration(seconds: (2 * _reconnectAttempts).clamp(2, 30));
    final jitter = Duration(
      milliseconds: (1000 * (0.5 + 0.5 * DateTime.now().millisecond / 1000))
          .round(),
    );
    final delay = baseDelay + jitter;

    debugPrint(
      'Scheduling reconnect attempt $_reconnectAttempts in ${delay.inSeconds}s',
    );

    _reconnectTimer = Timer(delay, () async {
      if (!_isDisposing &&
          !_isConnecting &&
          _currentStatus != WebSocketConnectionStatus.connected) {
        debugPrint(
          'Attempting reconnection $_reconnectAttempts/$maxReconnectAttempts',
        );
        _setStatus(WebSocketConnectionStatus.reconnecting);
        try {
          await connect(_token);
          // If successful, reset reconnect attempts
          _reconnectAttempts = 0;
        } catch (e) {
          _addLog(
            'RECONNECT',
            'Reconnection attempt failed: $e',
            isError: true,
          );
          debugPrint('Reconnection attempt failed: $e');
          _setStatus(WebSocketConnectionStatus.error);
          // Don't call _scheduleReconnect() recursively - let the connect() method handle it
        }
      }
    });
  }

  Future<void> _cleanup() async {
    _heartbeatTimer?.cancel();
    _reconnectTimer?.cancel();

    if (_channel != null) {
      try {
        await _channel!.sink.close(socket_status.normalClosure);
      } catch (e) {
        debugPrint('Error closing WebSocket channel: $e');
      }
      _channel = null;
    }
  }

  void disconnect() {
    _isDisposing = false;
    _setStatus(WebSocketConnectionStatus.disconnected);
    _cleanup();
  }

  /// Manually trigger a reconnection attempt
  void reconnect() {
    if (_isDisposing) return;

    _addLog('RECONNECT', 'Manual reconnection triggered');
    debugPrint('Manual WebSocket reconnection triggered');

    if (_token != null) {
      _setStatus(WebSocketConnectionStatus.reconnecting);
      _scheduleReconnect();
    } else {
      _addLog(
        'RECONNECT',
        'Cannot reconnect: No authentication token',
        isError: true,
      );
      debugPrint('Cannot reconnect: No authentication token available');
    }
  }

  void _processMessageQueue() {
    if (_messageQueue.isEmpty) {
      debugPrint('No queued messages to process');
      return;
    }

    _addLog('QUEUE', 'Processing ${_messageQueue.length} queued messages');

    // Create a copy of the queue to avoid modification issues during iteration
    final messagesToProcess = List<WebSocketMessage>.from(_messageQueue);
    _messageQueue.clear();

    // Process each message
    for (final message in messagesToProcess) {
      _addLog(
        'QUEUE',
        'Sending queued message: ${message.type}',
        data: message.data,
      );

      // Call send again - this will either send or requeue if still not connected
      send(message);
    }
  }

  /// Clear authentication token and trigger reauthentication
  void clearAuthAndReconnect() {
    _addLog('AUTH', 'Clearing authentication token for reauth');
    debugPrint('Clearing authentication token for reauthentication');

    _token = null;
    _setStatus(WebSocketConnectionStatus.disconnected);
    _cleanup();

    // This will trigger the app to show login screen
    notifyListeners();
  }

  /// Check if reconnection attempts have been exhausted
  bool get hasExhaustedReconnectAttempts {
    return _reconnectAttempts >= maxReconnectAttempts;
  }

  /// Reset reconnection attempts (useful after successful auth)
  void resetReconnectAttempts() {
    _reconnectAttempts = 0;
    _addLog('RECONNECT', 'Reconnection attempts reset');
  }

  @override
  void dispose() {
    _isDisposing = true;
    _isDisposed = true;
    _cleanup();
    _statusController.close();
    _messageController.close();
    super.dispose();
  }
}
