import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';
import '../state/workspace_manager.dart';
import '../widgets/workspace_topbar.dart';
import '../widgets/workspace_sidebar.dart';
import '../widgets/animated_loading_screen.dart';
import '../widgets/global_keyboard_shortcuts.dart';
import '../widgets/status_bar.dart';
import '../screens/graph_editor_screen.dart';
import '../screens/code_editor_screen.dart';
import '../screens/terminal_screen.dart';

class MainAppShell extends StatefulWidget {
  const MainAppShell({super.key});

  @override
  State<MainAppShell> createState() => _MainAppShellState();
}

class _MainAppShellState extends State<MainAppShell>
    with SingleTickerProviderStateMixin {
  late AnimationController _transitionController;

  @override
  void initState() {
    super.initState();
    _transitionController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
  }

  @override
  void dispose() {
    _transitionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer2<AppState, WorkspaceManager>(
      builder: (context, appState, workspaceManager, child) {
        if (!appState.isInitialized) {
          return _buildLoadingScreen();
        }

        if (appState.error != null) {
          return _buildErrorScreen(appState.error!);
        }


        if (workspaceManager.isSwitchingWorkspace) {
          return _buildWorkspaceSwitchingState(appState.workspaceState.currentWorkspace);
        }

        return Scaffold(
          body: GlobalKeyboardShortcuts(
            child: Column(
              children: [
                const WorkspaceTopBar(),
                Expanded(
                  child: Row(
                    children: [
                      const WorkspaceSidebar(),
                      Expanded(
                        child: Consumer<WorkspaceState>(
                          builder: (context, workspaceState, child) {
                            return AnimatedSwitcher(
                              duration: const Duration(milliseconds: 300),
                              switchInCurve: Curves.easeInOut,
                              switchOutCurve: Curves.easeInOut,
                              transitionBuilder: (child, animation) {
                                return SlideTransition(
                                  position: animation.drive(
                                    Tween(
                                      begin: const Offset(0.05, 0),
                                      end: Offset.zero,
                                    ),
                                  ),
                                  child: FadeTransition(
                                    opacity: animation,
                                    child: child,
                                  ),
                                );
                              },
                              child: _buildCurrentWorkspace(
                                workspaceState.currentWorkspace,
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
                const StatusBar(),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildCurrentWorkspace(WorkspaceType workspace) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        final pageManager = appState.pageManager;
        final activePageId = pageManager.activePageId;
        final activePageType = pageManager.activePageType;

        if (activePageId == null || activePageType == null) {

          return _buildEmptyStateWithLoading(workspace);
        }


        switch (activePageType) {
          case 'graphEditor':
            return GraphEditorScreen(
              key: ValueKey('graph_editor_$activePageId'),
              pageId: activePageId,
            );
          case 'codeEditor':
            return CodeEditorScreen(
              key: ValueKey('code_editor_$activePageId'),
              pageId: activePageId,
            );
          case 'terminal':
            return TerminalScreen(
              key: ValueKey('terminal_$activePageId'),
              pageId: activePageId,
            );
          default:

            return _buildFallbackWorkspace(workspace);
        }
      },
    );
  }

  Widget _buildEmptyStateWithLoading(WorkspaceType workspace) {
    return Container(
      color: Theme.of(context).colorScheme.surface,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              _getIconForWorkspaceType(workspace),
              size: 64,
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3),
            ),
            const SizedBox(height: 16),
            Text(
              'No page loaded',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Select a page from the sidebar to get started',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildWorkspaceSwitchingState(WorkspaceType workspace) {
    return Stack(
      children: [

        Container(
          width: double.infinity,
          height: double.infinity,
          color: Theme.of(context).colorScheme.surface,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  _getIconForWorkspaceType(workspace),
                  size: 64,
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3),
                ),
                const SizedBox(height: 16),
                Text(
                  'Switching workspace...',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                  ),
                ),
              ],
            ),
          ),
        ),

        Container(
          width: double.infinity,
          height: double.infinity,
          color: Colors.black.withOpacity(0.5),
          child: Center(
            child: Card(
              elevation: 8,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const SizedBox(
                      width: 40,
                      height: 40,
                      child: CircularProgressIndicator(strokeWidth: 3),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Switching workspace...',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }


  IconData _getIconForWorkspaceType(WorkspaceType workspace) {
    switch (workspace) {
      case WorkspaceType.graphEditor:
        return Icons.account_tree;
      case WorkspaceType.codeEditor:
        return Icons.code;
      case WorkspaceType.terminal:
        return Icons.terminal;
    }
  }

  Widget _buildFallbackWorkspace(WorkspaceType workspace) {
    switch (workspace) {
      case WorkspaceType.graphEditor:
        return const GraphEditorScreen(key: ValueKey('graph_editor'));
      case WorkspaceType.codeEditor:
        return const CodeEditorScreen(key: ValueKey('code_editor'));
      case WorkspaceType.terminal:
        return const TerminalScreen(
          key: ValueKey('terminal'),
          pageId: 'main_terminal',
        );
    }
  }

  Widget _buildLoadingScreen() {
    return AnimatedLoadingScreen(
      onLoadingComplete: () {


      },
    );
  }

  Widget _buildErrorScreen(String error) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surface,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                    Icons.error_outline,
                    size: 64,
                    color: Theme.of(context).colorScheme.error,
                  )
                  .animate()
                  .scale(duration: 300.ms)
                  .shake(hz: 2, curve: Curves.easeInOut),
              const SizedBox(height: 24),
              Text(
                'Initialization Error',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  color: Theme.of(context).colorScheme.error,
                  fontWeight: FontWeight.bold,
                ),
              ).animate().fadeIn(duration: 400.ms, delay: 100.ms),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.errorContainer.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: Theme.of(context).colorScheme.error.withOpacity(0.3),
                  ),
                ),
                child: Column(
                  children: [
                    Text(
                      'Error Details:',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      error,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.9),
                        fontFamily: 'monospace',
                      ),
                      textAlign: TextAlign.left,
                    ),
                  ],
                ),
              ).animate().fadeIn(duration: 400.ms, delay: 200.ms),
              const SizedBox(height: 32),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ElevatedButton.icon(
                    onPressed: () {
                      Provider.of<AppState>(
                        context,
                        listen: false,
                      ).initialize();
                    },
                    icon: const Icon(Icons.refresh),
                    label: const Text('Retry'),
                  ).animate()
                    .fadeIn(duration: 400.ms, delay: 300.ms)
                    .slideY(begin: 0.2, end: 0),
                  const SizedBox(width: 16),
                  ElevatedButton.icon(
                    onPressed: () {
                      _showLogsDialog(context);
                    },
                    icon: const Icon(Icons.bug_report),
                    label: const Text('View Logs'),
                  ).animate()
                    .fadeIn(duration: 400.ms, delay: 400.ms)
                    .slideY(begin: 0.2, end: 0),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showLogsDialog(BuildContext context) {
    final appState = Provider.of<AppState>(context, listen: false);
    final logs = appState.flutterLogService.recentLogs;
    final errorLogs = appState.flutterLogService.getRecentErrorLogs(count: 10);

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: [
            const Icon(Icons.bug_report),
            const SizedBox(width: 8),
            const Text('Debug Logs'),
            const Spacer(),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: errorLogs.isNotEmpty ? Colors.red.shade100 : Colors.green.shade100,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                '${errorLogs.length} errors',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: errorLogs.isNotEmpty ? Colors.red.shade700 : Colors.green.shade700,
                ),
              ),
            ),
          ],
        ),
        content: SizedBox(
          width: 700,
          height: 500,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    'Recent logs (last ${logs.length} entries):',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const Spacer(),
                  TextButton.icon(
                    onPressed: () {
                      appState.flutterLogService.clearLogs();
                      Navigator.of(context).pop();
                      _showLogsDialog(context);
                    },
                    icon: const Icon(Icons.clear_all, size: 16),
                    label: const Text('Clear All'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.black87,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.grey.shade600),
                  ),
                  child: SingleChildScrollView(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: logs.map((log) => Container(
                        margin: const EdgeInsets.symmetric(vertical: 1),
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: log.isError ? Colors.red.shade900.withOpacity(0.3) : Colors.transparent,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 4,
                              height: 16,
                              decoration: BoxDecoration(
                                color: log.isError ? Colors.red.shade400 : Colors.green.shade400,
                                borderRadius: BorderRadius.circular(2),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                log.toString(),
                                style: TextStyle(
                                  fontFamily: 'monospace',
                                  fontSize: 11,
                                  color: log.isError ? Colors.red.shade300 : Colors.green.shade300,
                                ),
                              ),
                            ),
                          ],
                        ),
                      )).toList(),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton.icon(
            onPressed: () {

              final logText = logs.map((log) => log.toString()).join('\n');
              // @todo actually copy to clickboard?
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Logs copied to clipboard (feature not implemented)')),
              );
            },
            icon: const Icon(Icons.copy, size: 16),
            label: const Text('Copy'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }
}
