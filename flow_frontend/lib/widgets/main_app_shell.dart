import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';
import '../widgets/workspace_topbar.dart';
import '../widgets/workspace_sidebar.dart';
import '../widgets/animated_loading_screen.dart';
import '../widgets/global_keyboard_shortcuts.dart';
import '../widgets/status_bar.dart';
import '../screens/graph_editor_screen.dart';
import '../screens/code_editor_screen.dart';
import '../screens/terminal_screen.dart';
import '../screens/connection_config_screen.dart';

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
    return Consumer<AppState>(
      builder: (context, appState, child) {












        if (!appState.isInitialized) {
          return _buildLoadingScreen();
        }

        if (appState.error != null) {
          return _buildErrorScreen(appState.error!);
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
        // Trigger app initialization completion
        // Provider.of<AppState>(context, listen: false).markInitialized();
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
              Text(
                error,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(
                    context,
                  ).colorScheme.onSurface.withOpacity(0.8),
                ),
                textAlign: TextAlign.center,
              ).animate().fadeIn(duration: 400.ms, delay: 200.ms),
              const SizedBox(height: 32),
              ElevatedButton.icon(
                    onPressed: () {
                      Provider.of<AppState>(
                        context,
                        listen: false,
                      ).initialize();
                    },
                    icon: const Icon(Icons.refresh),
                    label: const Text('Retry'),
                  )
                  .animate()
                  .fadeIn(duration: 400.ms, delay: 300.ms)
                  .slideY(begin: 0.2, end: 0),
            ],
          ),
        ),
      ),
    );
  }
}
