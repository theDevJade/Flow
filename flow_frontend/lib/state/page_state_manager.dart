import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../services/persistence_service.dart';

class PageState {
  final String pageId;
  final Map<String, dynamic> _data = {};

  PageState(this.pageId);

  // Generic data storage
  void setValue(String key, dynamic value) {
    _data[key] = value;
  }

  T? getValue<T>(String key) {
    return _data[key] as T?;
  }

  Map<String, dynamic> get data => Map.unmodifiable(_data);

  void clearData() {
    _data.clear();
  }
}

class GraphEditorPageState extends PageState {
  GraphEditorPageState(super.pageId);

  // Graph-specific state
  Offset get panOffset => getValue<Offset>('panOffset') ?? Offset.zero;
  set panOffset(Offset value) => setValue('panOffset', value);

  double get scale => getValue<double>('scale') ?? 1.0;
  set scale(double value) => setValue('scale', value);

  List<Map<String, dynamic>> get nodes =>
      getValue<List<Map<String, dynamic>>>('nodes') ?? [];
  set nodes(List<Map<String, dynamic>> value) => setValue('nodes', value);

  List<Map<String, dynamic>> get connections =>
      getValue<List<Map<String, dynamic>>>('connections') ?? [];
  set connections(List<Map<String, dynamic>> value) =>
      setValue('connections', value);

  String? get selectedNodeId => getValue<String?>('selectedNodeId');
  set selectedNodeId(String? value) => setValue('selectedNodeId', value);
}

class CodeEditorPageState extends PageState {
  CodeEditorPageState(super.pageId);

  // Code editor specific state
  List<String> get openFiles => getValue<List<String>>('openFiles') ?? [];
  set openFiles(List<String> value) => setValue('openFiles', value);

  String? get activeFile => getValue<String?>('activeFile');
  set activeFile(String? value) => setValue('activeFile', value);

  Map<String, String> get fileContents =>
      getValue<Map<String, String>>('fileContents') ?? {};
  set fileContents(Map<String, String> value) =>
      setValue('fileContents', value);
}

class TerminalPageState extends PageState {
  TerminalPageState(super.pageId);

  // Terminal specific state
  List<String> get history => getValue<List<String>>('history') ?? [];
  set history(List<String> value) => setValue('history', value);

  String get currentDirectory => getValue<String>('currentDirectory') ?? '/';
  set currentDirectory(String value) => setValue('currentDirectory', value);

  List<String> get commandHistory =>
      getValue<List<String>>('commandHistory') ?? [];
  set commandHistory(List<String> value) => setValue('commandHistory', value);

  int get historyIndex => getValue<int>('historyIndex') ?? -1;
  set historyIndex(int value) => setValue('historyIndex', value);
}

class PageStateManager with ChangeNotifier {
  static PageStateManager? _instance;
  static PageStateManager get instance {
    _instance ??= PageStateManager._internal();
    return _instance!;
  }

  PageStateManager._internal();

  final Map<String, PageState> _pageStates = {};

  PageState getOrCreatePageState(String pageId, String pageType) {
    if (_pageStates.containsKey(pageId)) {
      return _pageStates[pageId]!;
    }

    PageState pageState;
    switch (pageType) {
      case 'graphEditor':
        pageState = GraphEditorPageState(pageId);
        break;
      case 'codeEditor':
        pageState = CodeEditorPageState(pageId);
        break;
      case 'terminal':
        pageState = TerminalPageState(pageId);
        // Try to restore terminal state from persistence
        _restoreTerminalState(pageState as TerminalPageState);
        break;
      default:
        pageState = PageState(pageId);
        break;
    }

    _pageStates[pageId] = pageState;
    notifyListeners();
    return pageState;
  }

  void _restoreTerminalState(TerminalPageState terminalState) async {
    try {
      final history = await PersistenceService.instance.loadTerminalHistory(
        terminalState.pageId,
      );
      final commandHistory = await PersistenceService.instance
          .loadTerminalCommandHistory(terminalState.pageId);

      if (history.isNotEmpty) {
        terminalState.history = history;
      }
      if (commandHistory.isNotEmpty) {
        terminalState.commandHistory = commandHistory;
      }

      notifyListeners();
    } catch (e) {
      debugPrint(
        'Error restoring terminal state for ${terminalState.pageId}: $e',
      );
    }
  }

  void saveTerminalState(String pageId) async {
    try {
      final pageState = _pageStates[pageId];
      if (pageState is TerminalPageState) {
        await PersistenceService.instance.saveTerminalHistory(
          pageId,
          pageState.history,
        );
        await PersistenceService.instance.saveTerminalCommandHistory(
          pageId,
          pageState.commandHistory,
        );
      }
    } catch (e) {
      debugPrint('Error saving terminal state for $pageId: $e');
    }
  }

  T getPageState<T extends PageState>(String pageId) {
    return _pageStates[pageId] as T;
  }

  void removePageState(String pageId) {
    _pageStates.remove(pageId);
    notifyListeners();
  }

  void clearAllStates() {
    _pageStates.clear();
    notifyListeners();
  }

  // Save/restore functionality for persistence
  Map<String, Map<String, dynamic>> exportStates() {
    return _pageStates.map((key, value) => MapEntry(key, value.data));
  }

  void importStates(Map<String, Map<String, dynamic>> states) {
    _pageStates.clear();
    states.forEach((pageId, data) {
      final pageState = PageState(pageId);
      data.forEach((key, value) {
        pageState.setValue(key, value);
      });
      _pageStates[pageId] = pageState;
    });
    notifyListeners();
  }
}
