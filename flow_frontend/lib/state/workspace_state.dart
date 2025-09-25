import 'package:flutter/foundation.dart';

enum WorkspaceType { graphEditor, codeEditor, terminal }

class WorkspaceState with ChangeNotifier {
  WorkspaceType _currentWorkspace = WorkspaceType.graphEditor;
  bool _isTransitioning = false;

  WorkspaceType get currentWorkspace => _currentWorkspace;
  bool get isTransitioning => _isTransitioning;

  void switchToWorkspace(WorkspaceType workspace) {
    if (_currentWorkspace != workspace && !_isTransitioning) {
      _isTransitioning = true;
      notifyListeners();


      Future.delayed(const Duration(milliseconds: 300), () {
        _currentWorkspace = workspace;
        _isTransitioning = false;
        notifyListeners();
      });
    }
  }

  void setWorkspace(WorkspaceType workspace) {
    if (_currentWorkspace != workspace) {
      _currentWorkspace = workspace;
      notifyListeners();
    }
  }
}
