import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:path_provider/path_provider.dart';
import '../models/open_file.dart';
import '../state/workspace_state.dart';

class PersistenceService {
  static const String _terminalHistoryKey = 'terminal_history';
  static const String _terminalCommandHistoryKey = 'terminal_command_history';
  static const String _openFilesKey = 'open_files';
  static const String _activeFileKey = 'active_file';
  static const String _workspaceTypeKey = 'workspace_type';
  static const String _terminalStateFileName = 'terminal_states.json';
  static const String _websocketHostKey = 'websocket_host';
  static const String _websocketPortKey = 'websocket_port';
  static const String _websocketConfiguredKey = 'websocket_configured';

  static PersistenceService? _instance;
  static PersistenceService get instance =>
      _instance ??= PersistenceService._();

  PersistenceService._();

  SharedPreferences? _prefs;
  Directory? _appDir;

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    if (!kIsWeb) {
      _appDir = await getApplicationDocumentsDirectory();
    }
    debugPrint('PersistenceService: Initialized');
  }


  Future<void> saveTerminalHistory(String pageId, List<String> history) async {
    if (_prefs == null) return;

    try {
      final key = '${_terminalHistoryKey}_$pageId';
      await _prefs!.setStringList(key, history);
    } catch (e) {
      debugPrint('Error saving terminal history: $e');
    }
  }

  Future<List<String>> loadTerminalHistory(String pageId) async {
    if (_prefs == null) return [];

    try {
      final key = '${_terminalHistoryKey}_$pageId';
      return _prefs!.getStringList(key) ?? [];
    } catch (e) {
      debugPrint('Error loading terminal history: $e');
      return [];
    }
  }

  Future<void> saveTerminalCommandHistory(
    String pageId,
    List<String> commandHistory,
  ) async {
    if (_prefs == null) return;

    try {
      final key = '${_terminalCommandHistoryKey}_$pageId';
      await _prefs!.setStringList(key, commandHistory);
    } catch (e) {
      debugPrint('Error saving terminal command history: $e');
    }
  }

  Future<List<String>> loadTerminalCommandHistory(String pageId) async {
    if (_prefs == null) return [];

    try {
      final key = '${_terminalCommandHistoryKey}_$pageId';
      return _prefs!.getStringList(key) ?? [];
    } catch (e) {
      debugPrint('Error loading terminal command history: $e');
      return [];
    }
  }


  Future<void> saveOpenFiles(List<OpenFile> openFiles) async {
    if (_prefs == null) return;

    try {
      final List<Map<String, dynamic>> serializedFiles = openFiles
          .map(
            (file) => {
              'path': file.path,
              'content': file.content,
              'isModified': file.isModified,
            },
          )
          .toList();

      final String jsonString = jsonEncode(serializedFiles);
      await _prefs!.setString(_openFilesKey, jsonString);
    } catch (e) {
      debugPrint('Error saving open files: $e');
    }
  }

  Future<List<OpenFile>> loadOpenFiles() async {
    if (_prefs == null) return [];

    try {
      final String? jsonString = _prefs!.getString(_openFilesKey);
      if (jsonString == null) return [];

      final List<dynamic> jsonList = jsonDecode(jsonString);
      return jsonList
          .map(
            (json) => OpenFile(
              path: json['path'] as String,
              content: json['content'] as String,
              isModified: json['isModified'] as bool? ?? false,
            ),
          )
          .toList();
    } catch (e) {
      debugPrint('Error loading open files: $e');
      return [];
    }
  }

  Future<void> saveActiveFile(String? filePath) async {
    if (_prefs == null) return;

    try {
      if (filePath != null) {
        await _prefs!.setString(_activeFileKey, filePath);
      } else {
        await _prefs!.remove(_activeFileKey);
      }
    } catch (e) {
      debugPrint('Error saving active file: $e');
    }
  }

  Future<String?> loadActiveFile() async {
    if (_prefs == null) return null;

    try {
      return _prefs!.getString(_activeFileKey);
    } catch (e) {
      debugPrint('Error loading active file: $e');
      return null;
    }
  }


  Future<void> saveWorkspaceType(WorkspaceType workspaceType) async {
    if (_prefs == null) return;

    try {
      await _prefs!.setString(_workspaceTypeKey, workspaceType.toString());
    } catch (e) {
      debugPrint('Error saving workspace type: $e');
    }
  }

  Future<WorkspaceType?> loadWorkspaceType() async {
    if (_prefs == null) return null;

    try {
      final String? typeString = _prefs!.getString(_workspaceTypeKey);
      if (typeString == null) return null;

      return WorkspaceType.values.firstWhere(
        (type) => type.toString() == typeString,
        orElse: () => WorkspaceType.graphEditor,
      );
    } catch (e) {
      debugPrint('Error loading workspace type: $e');
      return null;
    }
  }


  Future<void> saveTerminalPageStates(
    Map<String, Map<String, dynamic>> states,
  ) async {
    if (_appDir == null) return;

    try {
      final file = File('${_appDir!.path}/$_terminalStateFileName');
      final jsonString = jsonEncode(states);
      await file.writeAsString(jsonString);
    } catch (e) {
      debugPrint('Error saving terminal page states: $e');
    }
  }

  Future<Map<String, Map<String, dynamic>>> loadTerminalPageStates() async {
    if (_appDir == null) return {};

    try {
      final file = File('${_appDir!.path}/$_terminalStateFileName');
      if (!await file.exists()) return {};

      final jsonString = await file.readAsString();
      final Map<String, dynamic> json = jsonDecode(jsonString);

      return json.map(
        (key, value) => MapEntry(key, Map<String, dynamic>.from(value)),
      );
    } catch (e) {
      debugPrint('Error loading terminal page states: $e');
      return {};
    }
  }


  Future<void> clearAllData() async {
    if (_prefs != null) {
      await _prefs!.clear();
    }

    if (_appDir != null) {
      try {
        final file = File('${_appDir!.path}/$_terminalStateFileName');
        if (await file.exists()) {
          await file.delete();
        }
      } catch (e) {
        debugPrint('Error clearing terminal state file: $e');
      }
    }

    debugPrint('PersistenceService: All data cleared');
  }


  Future<void> saveWebSocketConfig(String host, int port) async {
    if (_prefs == null) return;

    try {
      await _prefs!.setString(_websocketHostKey, host);
      await _prefs!.setInt(_websocketPortKey, port);
      await _prefs!.setBool(_websocketConfiguredKey, true);
      debugPrint('WebSocket config saved: $host:$port');
    } catch (e) {
      debugPrint('Error saving WebSocket config: $e');
    }
  }

  Future<Map<String, dynamic>?> loadWebSocketConfig() async {
    if (_prefs == null) return null;

    try {
      final isConfigured = _prefs!.getBool(_websocketConfiguredKey) ?? false;
      if (!isConfigured) return null;

      final host = _prefs!.getString(_websocketHostKey);
      final port = _prefs!.getInt(_websocketPortKey);

      if (host != null && port != null) {
        return {'host': host, 'port': port};
      }
    } catch (e) {
      debugPrint('Error loading WebSocket config: $e');
    }

    return null;
  }

  Future<void> clearWebSocketConfig() async {
    if (_prefs == null) return;

    try {
      await _prefs!.remove(_websocketHostKey);
      await _prefs!.remove(_websocketPortKey);
      await _prefs!.remove(_websocketConfiguredKey);
      debugPrint('WebSocket config cleared');
    } catch (e) {
      debugPrint('Error clearing WebSocket config: $e');
    }
  }


  String getWebSocketHost() {
    if (_prefs == null) return 'localhost';
    return _prefs!.getString(_websocketHostKey) ?? 'localhost';
  }

  int getWebSocketPort() {
    if (_prefs == null) return 8080;
    return _prefs!.getInt(_websocketPortKey) ?? 8080;
  }

  bool isWebSocketConfigured() {
    if (_prefs == null) return false;
    return _prefs!.getBool(_websocketConfiguredKey) ?? false;
  }


  static const String _showIntroSplashKey = 'show_intro_splash';

  Future<void> saveAppSettings(Map<String, dynamic> settings) async {
    if (_prefs == null) return;

    try {
      for (final entry in settings.entries) {
        switch (entry.key) {
          case 'showIntroSplash':
            if (entry.value is bool) {
              await _prefs!.setBool(_showIntroSplashKey, entry.value as bool);
            }
            break;
          default:
            debugPrint('Unknown setting key: ${entry.key}');
        }
      }
      debugPrint('App settings saved: $settings');
    } catch (e) {
      debugPrint('Error saving app settings: $e');
    }
  }

  Future<Map<String, dynamic>> loadAppSettings() async {
    if (_prefs == null) return {};

    try {
      return {'showIntroSplash': _prefs!.getBool(_showIntroSplashKey) ?? true};
    } catch (e) {
      debugPrint('Error loading app settings: $e');
      return {};
    }
  }

  Future<bool> shouldShowIntroSplash() async {
    if (_prefs == null) return true;

    try {
      return _prefs!.getBool(_showIntroSplashKey) ?? true;
    } catch (e) {
      debugPrint('Error loading intro splash setting: $e');
      return true;
    }
  }


  static const String _graphDataKey = 'graph_editor_data';
  static const String _graphViewStateKey = 'graph_editor_view_state';

  Future<void> saveGraphData(Map<String, dynamic> graphData) async {
    if (_prefs == null) return;

    try {
      final jsonString = jsonEncode(graphData);
      await _prefs!.setString(_graphDataKey, jsonString);
      debugPrint('Graph data saved to local storage');
    } catch (e) {
      debugPrint('Error saving graph data: $e');
    }
  }

  Future<Map<String, dynamic>?> loadGraphData() async {
    if (_prefs == null) return null;

    try {
      final jsonString = _prefs!.getString(_graphDataKey);
      if (jsonString == null) return null;

      final data = jsonDecode(jsonString) as Map<String, dynamic>;
      debugPrint('Graph data loaded from local storage');
      return data;
    } catch (e) {
      debugPrint('Error loading graph data: $e');
      return null;
    }
  }

  Future<void> saveGraphViewState({
    required double scale,
    required Offset panOffset,
    String? selectedNodeId,
    String? selectedConnectionId,
  }) async {
    if (_prefs == null) return;

    try {
      final viewState = {
        'scale': scale,
        'panOffset': {'dx': panOffset.dx, 'dy': panOffset.dy},
        'selectedNodeId': selectedNodeId,
        'selectedConnectionId': selectedConnectionId,
        'timestamp': DateTime.now().toIso8601String(),
      };

      final jsonString = jsonEncode(viewState);
      await _prefs!.setString(_graphViewStateKey, jsonString);
      debugPrint('Graph view state saved');
    } catch (e) {
      debugPrint('Error saving graph view state: $e');
    }
  }

  Future<Map<String, dynamic>?> loadGraphViewState() async {
    if (_prefs == null) return null;

    try {
      final jsonString = _prefs!.getString(_graphViewStateKey);
      if (jsonString == null) return null;

      final data = jsonDecode(jsonString) as Map<String, dynamic>;
      debugPrint('Graph view state loaded');
      return data;
    } catch (e) {
      debugPrint('Error loading graph view state: $e');
      return null;
    }
  }

  Future<void> clearGraphData() async {
    if (_prefs == null) return;

    try {
      await _prefs!.remove(_graphDataKey);
      await _prefs!.remove(_graphViewStateKey);
      debugPrint('Graph data cleared');
    } catch (e) {
      debugPrint('Error clearing graph data: $e');
    }
  }
}
