import 'package:flutter/foundation.dart';
import 'workspace_manager.dart';



class PageManager with ChangeNotifier {
  static PageManager? _instance;
  static PageManager get instance => _instance ??= PageManager._();
  PageManager._();

  String? _activePageId;
  String? _activePageType;
  WorkspaceManager? _workspaceManager;

  String? get activePageId => _activePageId;
  String? get activePageType => _activePageType;
  String? get currentWorkspaceId => _workspaceManager?.currentWorkspaceId;

  void initialize(WorkspaceManager workspaceManager) {
    _workspaceManager = workspaceManager;
  }


  void setActivePage(String pageId, String pageType) {
    if (_activePageId != pageId || _activePageType != pageType) {
      debugPrint('PageManager: Setting active page to $pageId ($pageType)');
      _activePageId = pageId;
      _activePageType = pageType;
      _saveActivePageToWorkspace();
      notifyListeners();


      _updateWorkspacePageActiveStatus(pageId);
    }
  }


  void _updateWorkspacePageActiveStatus(String activePageId) {
    final workspace = _workspaceManager?.currentWorkspace;
    if (workspace == null) return;

    try {

      final data = Map<String, dynamic>.from(workspace.data);
      final List<dynamic> pages = (data['pages'] as List<dynamic>?) ?? [];


      final updatedPages = pages.map((page) {
        if (page is Map<String, dynamic>) {
          final pageData = Map<String, dynamic>.from(page);
          pageData['isActive'] = pageData['id'] == activePageId;
          return pageData;
        }
        return page;
      }).toList();


      data['pages'] = updatedPages;
      _workspaceManager?.updateWorkspace(workspace.id, data: data);
    } catch (e) {
      debugPrint('PageManager: Error updating page active status: $e');
    }
  }


  Map<String, dynamic> getPageState(String pageId, String pageType) {
    final workspace = _workspaceManager?.currentWorkspace;
    if (workspace == null) return {};

    final pageStates =
        workspace.data['pageStates'] as Map<String, dynamic>? ?? {};

    if (!pageStates.containsKey(pageId)) {

      pageStates[pageId] = _createDefaultPageState(pageType);
      _savePageStatesToWorkspace();
    }

    return Map<String, dynamic>.from(
      pageStates[pageId] as Map<String, dynamic>? ?? {},
    );
  }



  Map<String, dynamic>? getPageStateWithoutNotify(
    String pageId,
    String pageType,
  ) {
    final workspace = _workspaceManager?.currentWorkspace;
    if (workspace == null) return {};

    try {
      final pageStates =
          workspace.data['pageStates'] as Map<String, dynamic>? ?? {};

      if (!pageStates.containsKey(pageId)) {

        return _createDefaultPageState(pageType);
      }

      return Map<String, dynamic>.from(
        pageStates[pageId] as Map<String, dynamic>? ?? {},
      );
    } catch (e) {
      debugPrint('PageManager: Error getting page state without notify: $e');
      return _createDefaultPageState(pageType);
    }
  }


  void updatePageState(String pageId, Map<String, dynamic> state) {
    final workspace = _workspaceManager?.currentWorkspace;
    if (workspace == null) return;

    final pageStates =
        workspace.data['pageStates'] as Map<String, dynamic>? ?? {};
    pageStates[pageId] = state;

    _savePageStatesToWorkspace();
    notifyListeners();
  }


  void updatePageStateKey(String pageId, String key, dynamic value) {
    final currentState = getPageState(pageId, '');
    currentState[key] = value;
    updatePageState(pageId, currentState);
  }

  void _saveActivePageToWorkspace() {
    if (_workspaceManager == null || _activePageId == null) return;

    final workspace = _workspaceManager!.currentWorkspace;
    if (workspace != null) {
      final updatedData = Map<String, dynamic>.from(workspace.data);
      updatedData['activePageId'] = _activePageId;
      updatedData['activePageType'] = _activePageType;

      _workspaceManager!.updateWorkspace(workspace.id, data: updatedData);
    }
  }

  void _savePageStatesToWorkspace() {
    if (_workspaceManager == null) return;

    final workspace = _workspaceManager!.currentWorkspace;
    if (workspace != null) {


      _workspaceManager!.updateWorkspace(workspace.id, data: workspace.data);
    }
  }

  Map<String, dynamic> _createDefaultPageState(String pageType) {
    switch (pageType) {
      case 'graphEditor':
        return {
          'nodes': <Map<String, dynamic>>[],
          'connections': <Map<String, dynamic>>[],
          'panOffset': {'dx': 0.0, 'dy': 0.0},
          'scale': 1.0,
          'selectedNodeId': null,
        };
      case 'codeEditor':
        return {
          'openFiles': <String>[],
          'activeFile': null,
          'fileContents': <String, String>{},
          'scrollPositions': <String, double>{},
        };
      case 'terminal':
        return {
          'history': <String>[],
          'currentDirectory': '/',
          'commandHistory': <String>[],
          'historyIndex': -1,
        };
      default:
        return {};
    }
  }


  void loadActivePageFromWorkspace() {
    final workspace = _workspaceManager?.currentWorkspace;
    if (workspace != null) {
      debugPrint(
        'PageManager: Loading active page from workspace: ${workspace.id}',
      );


      String? newActivePageId;
      String? newActivePageType;


      newActivePageId = workspace.data['activePageId'] as String?;
      newActivePageType = workspace.data['activePageType'] as String?;


      if (newActivePageId == null || newActivePageType == null) {
        final pagesList = workspace.data['pages'] as List<dynamic>?;
        if (pagesList != null) {
          for (final pageData in pagesList) {
            if (pageData is Map<String, dynamic> &&
                pageData['isActive'] == true) {
              newActivePageId = pageData['id'] as String?;
              newActivePageType = pageData['type'] as String?;
              break;
            }
          }
        }
      }


      if (newActivePageId != null && newActivePageType != null) {
        _activePageId = newActivePageId;
        _activePageType = newActivePageType;
        debugPrint(
          'PageManager: Active page set to $_activePageId ($_activePageType)',
        );
      } else {
        // Default to first page in the list or create defaults
        final pagesList = workspace.data['pages'] as List<dynamic>?;
        if (pagesList != null && pagesList.isNotEmpty) {
          final firstPage = pagesList.first;
          if (firstPage is Map<String, dynamic>) {
            _activePageId = firstPage['id'] as String?;
            _activePageType = firstPage['type'] as String?;
            debugPrint(
              'PageManager: Defaulting to first page: $_activePageId ($_activePageType)',
            );
          }
        }
      }

      notifyListeners();
    } else {
      debugPrint(
        'PageManager: No workspace available, cannot load active page',
      );
    }
  }

  /// Clears the active page (e.g., when no workspace is selected)
  void clearActivePage() {
    _activePageId = null;
    _activePageType = null;
    notifyListeners();
  }
}
