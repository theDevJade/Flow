import 'dart:async';
import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';


class FlutterLogService with ChangeNotifier {
  static FlutterLogService? _instance;
  static FlutterLogService get instance => _instance ??= FlutterLogService._();

  FlutterLogService._() {
    _initializeLogCapture();
  }

  final List<FlutterLogEntry> _logs = [];
  static const int maxLogEntries = 1000;

  List<FlutterLogEntry> get logs => List.unmodifiable(_logs);
  List<FlutterLogEntry> get recentLogs => _logs.length > 50
      ? _logs.sublist(_logs.length - 50)
      : _logs;

  void _initializeLogCapture() {

    debugPrint = (String? message, {int? wrapWidth}) {
      _addLog('DEBUG', message ?? '', isError: false);

      developer.log(message ?? '', name: 'DEBUG');
    };


    FlutterError.onError = (FlutterErrorDetails details) {
      _addLog('FLUTTER_ERROR', details.toString(), isError: true);

      FlutterError.presentError(details);
    };
  }

  void _addLog(String type, String message, {bool isError = false}) {
    final logEntry = FlutterLogEntry(
      type: type,
      message: message,
      timestamp: DateTime.now(),
      isError: isError,
    );

    _logs.add(logEntry);


    if (_logs.length > maxLogEntries) {
      _logs.removeAt(0);
    }

    notifyListeners();
  }

  void addCustomLog(String type, String message, {bool isError = false}) {
    _addLog(type, message, isError: isError);
  }

  void clearLogs() {
    _logs.clear();
    notifyListeners();
  }

  List<FlutterLogEntry> getLogsByType(String type) {
    return _logs.where((log) => log.type == type).toList();
  }

  List<FlutterLogEntry> getErrorLogs() {
    return _logs.where((log) => log.isError).toList();
  }

  List<FlutterLogEntry> getRecentErrorLogs({int count = 20}) {
    final errorLogs = getErrorLogs();
    return errorLogs.length > count
        ? errorLogs.sublist(errorLogs.length - count)
        : errorLogs;
  }
}

class FlutterLogEntry {
  final String type;
  final String message;
  final DateTime timestamp;
  final bool isError;

  FlutterLogEntry({
    required this.type,
    required this.message,
    required this.timestamp,
    required this.isError,
  });

  @override
  String toString() {
    final timeStr = '${timestamp.hour.toString().padLeft(2, '0')}:'
        '${timestamp.minute.toString().padLeft(2, '0')}:'
        '${timestamp.second.toString().padLeft(2, '0')}';
    return '[$timeStr] [$type] $message';
  }
}
