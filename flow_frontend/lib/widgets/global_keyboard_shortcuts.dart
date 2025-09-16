import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';

class GlobalKeyboardShortcuts extends StatefulWidget {
  final Widget child;

  const GlobalKeyboardShortcuts({super.key, required this.child});

  @override
  State<GlobalKeyboardShortcuts> createState() =>
      _GlobalKeyboardShortcutsState();
}

class _GlobalKeyboardShortcutsState extends State<GlobalKeyboardShortcuts> {
  @override
  Widget build(BuildContext context) {
    return Shortcuts(
      shortcuts: <LogicalKeySet, Intent>{
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.digit1):
            SwitchWorkspaceIntent(WorkspaceType.graphEditor),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.digit1):
            SwitchWorkspaceIntent(WorkspaceType.graphEditor),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.digit2):
            SwitchWorkspaceIntent(WorkspaceType.codeEditor),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.digit2):
            SwitchWorkspaceIntent(WorkspaceType.codeEditor),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyS):
            const SaveFileIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyS):
            const SaveFileIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyW):
            const CloseFileIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyW):
            const CloseFileIntent(),
      },
      child: Actions(
        actions: <Type, Action<Intent>>{
          SwitchWorkspaceIntent: SwitchWorkspaceAction(),
          SaveFileIntent: SaveFileAction(),
          CloseFileIntent: CloseFileAction(),
        },
        child: Focus(autofocus: true, child: widget.child),
      ),
    );
  }
}

class SwitchWorkspaceIntent extends Intent {
  final WorkspaceType workspaceType;
  const SwitchWorkspaceIntent(this.workspaceType);
}

class SaveFileIntent extends Intent {
  const SaveFileIntent();
}

class CloseFileIntent extends Intent {
  const CloseFileIntent();
}

class SwitchWorkspaceAction extends Action<SwitchWorkspaceIntent> {
  @override
  Object? invoke(SwitchWorkspaceIntent intent) {
    final context = primaryFocus?.context;
    if (context != null) {
      context.read<AppState>().workspaceState.switchToWorkspace(
        intent.workspaceType,
      );
    }
    return null;
  }
}

class SaveFileAction extends Action<SaveFileIntent> {
  @override
  Object? invoke(SaveFileIntent intent) {
    final context = primaryFocus?.context;
    if (context != null) {
      final appState = context.read<AppState>();
      final activeFile = appState.fileSystemState.activeFile;
      if (activeFile != null && activeFile.isModified) {
        appState.fileSystemService
            .writeFile(activeFile.path, activeFile.content)
            .then((success) {
              if (success) {
                appState.fileSystemState.markFileSaved(activeFile.path);
              }
            });
      }
    }
    return null;
  }
}

class CloseFileAction extends Action<CloseFileIntent> {
  @override
  Object? invoke(CloseFileIntent intent) {
    final context = primaryFocus?.context;
    if (context != null) {
      final fileSystemState = context.read<AppState>().fileSystemState;
      final activeFile = fileSystemState.activeFile;
      if (activeFile != null) {
        fileSystemState.closeFile(activeFile.path);
      }
    }
    return null;
  }
}
