import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';
import '../state/workspace_manager.dart';
import '../services/websocket_service.dart';
import '../services/persistence_service.dart';
import 'settings_dialog.dart';

class WorkspaceTopBar extends StatelessWidget {
  const WorkspaceTopBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        return Container(
          height: 48,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            border: Border(
              bottom: BorderSide(
                color: Theme.of(context).dividerColor,
                width: 0.5,
              ),
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                // Logo/App name
                Text(
                  'Flow',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                ),

                const SizedBox(width: 24),

                // Workspace manager
                _buildWorkspaceManager(context, appState.workspaceManager),

                const Spacer(),

                // Connection status
                _buildConnectionStatus(context, appState.webSocketService),

                const SizedBox(width: 16),

                // Settings button
                _buildSettingsButton(context, appState),

                const SizedBox(width: 16),

                // User menu
                _buildUserMenu(context, appState),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildWorkspaceManager(
    BuildContext context,
    WorkspaceManager workspaceManager,
  ) {
    return Consumer<WorkspaceManager>(
      builder: (context, manager, child) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildWorkspaceDropdown(context, manager),
            const SizedBox(width: 8),
            _buildCreateWorkspaceButton(context, manager),
          ],
        );
      },
    );
  }

  Widget _buildWorkspaceDropdown(
      BuildContext context, WorkspaceManager manager) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceVariant,
        borderRadius: BorderRadius.circular(8),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<Workspace>(
          value: manager.currentWorkspace,
          isDense: true,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          items: manager.workspaces.map((workspace) {
            return DropdownMenuItem<Workspace>(
              value: workspace,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.folder,
                    size: 16,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    workspace.name,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ],
              ),
            );
          }).toList(),
          onChanged: (Workspace? workspace) {
            if (workspace != null) {
              manager.setCurrentWorkspace(workspace.id);
            }
          },
        ),
      ),
    );
  }

  Widget _buildCreateWorkspaceButton(
      BuildContext context, WorkspaceManager manager) {
    return IconButton(
      onPressed: () => _showCreateWorkspaceDialog(context, manager),
      icon: const Icon(Icons.add),
      tooltip: 'Create New Workspace',
      style: IconButton.styleFrom(
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Theme.of(context).colorScheme.onPrimary,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
    );
  }

  Widget _buildConnectionStatus(
    BuildContext context,
    WebSocketService webSocketService,
  ) {
    return StreamBuilder<WebSocketConnectionStatus>(
      stream: webSocketService.status,
      initialData: webSocketService.currentStatus,
      builder: (context, snapshot) {
        final status = snapshot.data ?? WebSocketConnectionStatus.disconnected;

        Color statusColor;
        IconData statusIcon;
        String statusText;

        switch (status) {
          case WebSocketConnectionStatus.connected:
            statusColor = Colors.green;
            statusIcon = Icons.wifi;
            statusText = 'Connected';
            break;
          case WebSocketConnectionStatus.connecting:
            statusColor = Colors.orange;
            statusIcon = Icons.wifi_off;
            statusText = 'Connecting...';
            break;
          case WebSocketConnectionStatus.reconnecting:
            statusColor = Colors.orange;
            statusIcon = Icons.wifi_off;
            statusText = 'Reconnecting...';
            break;
          case WebSocketConnectionStatus.error:
            statusColor = Colors.red;
            statusIcon = Icons.wifi_off;
            statusText = 'Connection Error';
            break;
          case WebSocketConnectionStatus.disconnected:
            statusColor = Colors.grey;
            statusIcon = Icons.wifi_off;
            statusText = 'Disconnected';
            break;
        }

        return Tooltip(
          message: statusText,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: statusColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: statusColor.withOpacity(0.3)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(statusIcon, size: 16, color: statusColor),
                const SizedBox(width: 4),
                Text(
                  statusText,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: statusColor,
                        fontWeight: FontWeight.w500,
                      ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildSettingsButton(BuildContext context, AppState appState) {
    return IconButton(
      icon: const Icon(Icons.settings),
      tooltip: 'Settings',
      onPressed: () => _showSettingsDialog(context, appState),
    );
  }

  void _showSettingsDialog(BuildContext context, AppState appState) async {
    // Load current settings from persistence service
    final wsConfig = await PersistenceService.instance.loadWebSocketConfig();
    final appSettings = await PersistenceService.instance.loadAppSettings();

    showDialog(
      context: context,
      builder: (context) => SettingsDialog(
        currentHost: wsConfig?['host'] as String?,
        currentPort: wsConfig?['port'] as int?,
        showIntroSplash: appSettings['showIntroSplash'] as bool? ?? true,
        onSettingsChanged: (host, port, showIntro) {
          // Update WebSocket configuration
          // appState.configureWebSocketConnection(host, port);

          // Show restart notification if needed
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Settings saved. Restart the app for some changes to take effect.',
              ),
              duration: Duration(seconds: 4),
            ),
          );
        },
      ),
    );
  }

  Widget _buildUserMenu(BuildContext context, AppState appState) {
    return Consumer<AppState>(
      builder: (context, state, child) {
        final user = state.authService.currentUser;

        if (user == null) {
          return TextButton(
            onPressed: () => _showLoginDialog(context, appState),
            child: const Text('Login'),
          );
        }

        return PopupMenuButton<String>(
          child: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: Text(
              user.username.isNotEmpty ? user.username[0].toUpperCase() : 'U',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: Theme.of(context).colorScheme.primary,
                  ),
            ),
          ),
          itemBuilder: (context) => <PopupMenuEntry<String>>[
            PopupMenuItem<String>(
              value: 'user_info',
              enabled: false,
              child: Text(user.username),
            ),
            PopupMenuItem<String>(
              value: 'user_email',
              enabled: false,
              child: Text(user.username),
            ),
            const PopupMenuDivider(),
            PopupMenuItem<String>(value: 'logout', child: const Text('Logout')),
          ],
          onSelected: (value) {
            if (value == 'logout') {
              appState.authService.logout();
            }
          },
        );
      },
    );
  }

  void _showLoginDialog(BuildContext context, AppState appState) {
    showDialog(
      context: context,
      builder: (context) => LoginDialog(appState: appState),
    );
  }

  void _showCreateWorkspaceDialog(
      BuildContext context, WorkspaceManager manager) {
    final nameController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Create New Workspace'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(
                labelText: 'Workspace Name',
                hintText: 'My Project',
                border: OutlineInputBorder(),
              ),
              autofocus: true,
            ),
            if (manager.error != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  manager.error!,
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.error,
                    fontSize: 12,
                  ),
                ),
              ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              manager.clearError();
              Navigator.of(context).pop();
            },
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: manager.isLoading
                ? null
                : () {
                    if (nameController.text.trim().isNotEmpty) {
                      manager.createWorkspace(nameController.text.trim());
                      Navigator.of(context).pop();
                    }
                  },
            child: manager.isLoading
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Create'),
          ),
        ],
      ),
    );
  }
}

class LoginDialog extends StatefulWidget {
  final AppState appState;

  const LoginDialog({super.key, required this.appState});

  @override
  State<LoginDialog> createState() => _LoginDialogState();
}

class _LoginDialogState extends State<LoginDialog> {
  final _passwordController = TextEditingController();
  bool _isLoading = false;
  String? _error;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Login'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _passwordController,
            decoration: const InputDecoration(
              labelText: 'Password',
              hintText: 'Enter your password',
            ),
            obscureText: true,
            enabled: !_isLoading,
            autofocus: true,
            onSubmitted: (_) => _login(),
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: 16),
          Text(
            'Demo passwords:\\n'
            'admin123 (Administrator)\\n'
            'dev456 (Developer)\\n'
            'user789 (User)',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color:
                      Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: _isLoading ? null : () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        ElevatedButton(
          onPressed: _isLoading ? null : _login,
          child: _isLoading
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Login'),
        ),
      ],
    );
  }

  Future<void> _login() async {
    // Close this dialog and redirect to main login screen
    if (mounted) {
      Navigator.pop(context);
      // Trigger logout to show the login screen
      await widget.appState.authService.logout();
    }
  }

  @override
  void dispose() {
    _passwordController.dispose();
    super.dispose();
  }
}
