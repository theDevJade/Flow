import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'websocket_service.dart';

class FlowLangExecutionResult {
  final String output;
  final List<String> errors;
  final List<String> logs;
  final bool success;
  final Duration executionTime;
  final DateTime timestamp;

  FlowLangExecutionResult({
    required this.output,
    required this.errors,
    required this.logs,
    required this.success,
    required this.executionTime,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  factory FlowLangExecutionResult.fromJson(Map<String, dynamic> json) {
    return FlowLangExecutionResult(
      output: json['output'] ?? '',
      errors: List<String>.from(json['errors'] ?? []),
      logs: List<String>.from(json['logs'] ?? []),
      success: json['success'] ?? false,
      executionTime: Duration(milliseconds: json['executionTime'] ?? 0),
      timestamp: DateTime.tryParse(json['timestamp'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'output': output,
      'errors': errors,
      'logs': logs,
      'success': success,
      'executionTime': executionTime.inMilliseconds,
      'timestamp': timestamp.toIso8601String(),
    };
  }
}

class FlowLangExecutionService with ChangeNotifier {
  static FlowLangExecutionService? _instance;
  static FlowLangExecutionService get instance {
    if (_instance != null && _instance!._isDisposed) {
      _instance = null;
    }
    return _instance ??= FlowLangExecutionService._();
  }

  FlowLangExecutionService._();

  final List<FlowLangExecutionResult> _executionHistory = [];
  final StreamController<FlowLangExecutionResult> _executionController =
      StreamController<FlowLangExecutionResult>.broadcast();

  bool _isExecuting = false;
  bool _isDisposed = false;

  List<FlowLangExecutionResult> get executionHistory => List.unmodifiable(_executionHistory);
  Stream<FlowLangExecutionResult> get executionStream => _executionController.stream;
  bool get isExecuting => _isExecuting;

  static const int maxHistorySize = 100;

  Future<FlowLangExecutionResult> executeFlowLang(String code, {String? fileName}) async {
    if (_isDisposed) {
      throw Exception('FlowLangExecutionService has been disposed');
    }

    _isExecuting = true;
    notifyListeners();

    final stopwatch = Stopwatch()..start();

    try {
      debugPrint('🚀 FlowLangExecutionService: Executing FlowLang code (${code.length} chars)');


      final result = await _executeViaWebSocket(code, fileName);

      stopwatch.stop();

      final executionResult = FlowLangExecutionResult(
        output: result.output,
        errors: result.errors,
        logs: result.logs,
        success: result.success,
        executionTime: stopwatch.elapsed,
      );


      _addToHistory(executionResult);


      _executionController.add(executionResult);

      debugPrint('✅ FlowLangExecutionService: Execution completed in ${executionResult.executionTime.inMilliseconds}ms');

      return executionResult;
    } catch (e) {
      stopwatch.stop();

      final errorResult = FlowLangExecutionResult(
        output: '',
        errors: ['Execution failed: $e'],
        logs: [],
        success: false,
        executionTime: stopwatch.elapsed,
      );

      _addToHistory(errorResult);
      _executionController.add(errorResult);

      debugPrint('❌ FlowLangExecutionService: Execution failed: $e');

      return errorResult;
    } finally {
      _isExecuting = false;
      notifyListeners();
    }
  }

  Future<FlowLangExecutionResult> _executeViaWebSocket(String code, String? fileName) async {
    final completer = Completer<FlowLangExecutionResult>();
    final timeout = const Duration(seconds: 30);


    Timer(timeout, () {
      if (!completer.isCompleted) {
        completer.complete(FlowLangExecutionResult(
          output: '',
          errors: ['Execution timeout after ${timeout.inSeconds} seconds'],
          logs: [],
          success: false,
          executionTime: timeout,
        ));
      }
    });


    late StreamSubscription subscription;
    subscription = _executionController.stream.listen((result) {
      subscription.cancel();
      if (!completer.isCompleted) {
        completer.complete(result);
      }
    });


    WebSocketService.instance.sendMessage('execute_flowlang', {
      'code': code,
      'fileName': fileName ?? 'untitled.flowlang',
      'timestamp': DateTime.now().toIso8601String(),
    });

    return completer.future;
  }

  void _addToHistory(FlowLangExecutionResult result) {
    _executionHistory.add(result);


    if (_executionHistory.length > maxHistorySize) {
      _executionHistory.removeAt(0);
    }
  }

  void clearHistory() {
    _executionHistory.clear();
    notifyListeners();
  }

  FlowLangExecutionResult? getLastResult() {
    return _executionHistory.isNotEmpty ? _executionHistory.last : null;
  }

  List<FlowLangExecutionResult> getResultsBySuccess(bool success) {
    return _executionHistory.where((result) => result.success == success).toList();
  }


  void handleExecutionResult(dynamic message) {
    try {
      final result = FlowLangExecutionResult.fromJson(message.data);
      _addToHistory(result);
      _executionController.add(result);
      debugPrint('📥 FlowLangExecutionService: Received execution result');
    } catch (e) {
      debugPrint('❌ FlowLangExecutionService: Error handling execution result: $e');
    }
  }


  void handleExecutionError(dynamic message) {
    try {
      final result = FlowLangExecutionResult(
        output: '',
        errors: [message.data['error'] ?? 'Unknown execution error'],
        logs: List<String>.from(message.data['logs'] ?? []),
        success: false,
        executionTime: Duration.zero,
      );
      _addToHistory(result);
      _executionController.add(result);
      debugPrint('📥 FlowLangExecutionService: Received execution error');
    } catch (e) {
      debugPrint('❌ FlowLangExecutionService: Error handling execution error: $e');
    }
  }

  @override
  void dispose() {
    _isDisposed = true;
    _executionController.close();
    super.dispose();
  }
}
