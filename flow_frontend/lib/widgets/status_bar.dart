import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';
import '../state/file_system_state.dart' as fs;
import '../services/websocket_service.dart';
import '../screens/login_screen.dart';

class StatusBar extends StatelessWidget {
  const StatusBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        return Container(
          height: 24,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            border: Border(
              top: BorderSide(
                color: Theme.of(context).dividerColor,
                width: 0.5,
              ),
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12.0),
            child: Row(
              children: [
                _buildConnectionStatus(context, appState.webSocketService),
                const SizedBox(width: 16),
                _buildWorkspaceIndicator(context, appState.workspaceState),
                const SizedBox(width: 16),
                _buildWebSocketMessage(context, appState.webSocketService),
                const Spacer(),
                _buildFileInfo(context, appState.fileSystemState),
              ],
            ),
          ),
        );
      },
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
        String statusText;
        String? statusDetail;

        switch (status) {
          case WebSocketConnectionStatus.connected:
            statusColor = Colors.green;
            statusText = 'Connected';
            break;
          case WebSocketConnectionStatus.connecting:
            statusColor = Colors.orange;
            statusText = 'Connecting';
            break;
          case WebSocketConnectionStatus.reconnecting:
            statusColor = Colors.orange;
            statusText = 'Reconnecting';
            // Show reconnection attempt count
            final logs = webSocketService.logs;
            final reconnectLogs =
                logs.where((log) => log.type == 'RECONNECT').length;
            if (reconnectLogs > 0) {
              statusDetail =
                  'Attempt $reconnectLogs/${WebSocketService.maxReconnectAttempts}';
            }
            break;
          case WebSocketConnectionStatus.error:
            statusColor = Colors.red;
            statusText = 'Error';
            // Show last error message
            final logs = webSocketService.logs;
            final errorLogs = logs.where((log) => log.isError).toList();
            if (errorLogs.isNotEmpty) {
              final lastError = errorLogs.last.message;
              statusDetail = lastError.length > 30
                  ? '${lastError.substring(0, 27)}...'
                  : lastError;
            }
            break;
          case WebSocketConnectionStatus.disconnected:
            statusColor = Colors.grey;
            statusText = 'Disconnected';
            break;
        }

        return GestureDetector(
          onTap: () => _showConnectionDetails(context, webSocketService),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildStatusIndicator(context, status, statusColor),
              const SizedBox(width: 6),
              Flexible(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      statusText,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontSize: 11,
                            color: Theme.of(
                              context,
                            ).colorScheme.onSurface.withOpacity(0.7),
                          ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (statusDetail != null)
                      Text(
                        statusDetail,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              fontSize: 9,
                              color: Theme.of(
                                context,
                              ).colorScheme.onSurface.withOpacity(0.5),
                            ),
                        overflow: TextOverflow.ellipsis,
                      ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildWorkspaceIndicator(
    BuildContext context,
    WorkspaceState workspaceState,
  ) {
    return Consumer<WorkspaceState>(
      builder: (context, state, child) {
        final workspaceName =
            state.currentWorkspace == WorkspaceType.graphEditor
                ? 'Graph Editor'
                : 'Code Editor';

        return Text(
          workspaceName,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                fontSize: 11,
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                fontWeight: FontWeight.w500,
              ),
        );
      },
    );
  }

  Widget _buildFileInfo(
    BuildContext context,
    fs.FileSystemState fileSystemState,
  ) {
    return Consumer<fs.FileSystemState>(
      builder: (context, state, child) {
        final activeFile = state.activeFile;
        final openFilesCount = state.openFiles.length;

        if (activeFile == null) {
          return Text(
            openFilesCount == 0 ? 'No files' : '$openFilesCount files',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  fontSize: 11,
                  color:
                      Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                ),
          );
        }

        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              activeFile.path.split('/').last,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    fontSize: 11,
                    color: Theme.of(context)
                        .colorScheme
                        .onSurface
                        .withOpacity(0.7),
                    fontWeight: FontWeight.w500,
                  ),
            ),
            if (activeFile.isModified) ...[
              const SizedBox(width: 4),
              Container(
                width: 4,
                height: 4,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary,
                  shape: BoxShape.circle,
                ),
              ),
            ],
            const SizedBox(width: 8),
            Text(
              '$openFilesCount files',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    fontSize: 11,
                    color: Theme.of(context)
                        .colorScheme
                        .onSurface
                        .withOpacity(0.5),
                  ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildWebSocketMessage(
    BuildContext context,
    WebSocketService webSocketService,
  ) {
    return StreamBuilder<WebSocketMessage>(
      stream: webSocketService.messages,
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const SizedBox.shrink();
        }

        final message = snapshot.data!;
        final timestamp =
            DateTime.now().difference(message.timestamp).inSeconds;

        String timeText;
        if (timestamp < 60) {
          timeText = '${timestamp}s ago';
        } else {
          timeText = '${timestamp ~/ 60}m ago';
        }

        return Container(
          constraints: const BoxConstraints(maxWidth: 300),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                _getMessageIcon(message.type),
                size: 12,
                color: _getMessageColor(message.type),
              ),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  '${message.type}: ${_getMessageSummary(message)}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        fontSize: 10,
                        color: Theme.of(
                          context,
                        ).colorScheme.onSurface.withOpacity(0.7),
                      ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 6),
              Text(
                timeText,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      fontSize: 9,
                      color: Theme.of(
                        context,
                      ).colorScheme.onSurface.withOpacity(0.4),
                    ),
              ),
            ],
          ),
        );
      },
    );
  }

  IconData _getMessageIcon(String messageType) {
    switch (messageType) {
      case 'graph_save':
      case 'graph_save_response':
        return Icons.save;
      case 'graph_load':
      case 'graph_load_response':
        return Icons.folder_open;
      case 'file_save':
      case 'file_save_response':
        return Icons.description;
      case 'auth_login':
      case 'auth_response':
        return Icons.person;
      default:
        return Icons.message;
    }
  }

  Color _getMessageColor(String messageType) {
    switch (messageType) {
      case 'graph_save':
      case 'graph_save_response':
        return Colors.green;
      case 'graph_load':
      case 'graph_load_response':
        return Colors.blue;
      case 'file_save':
      case 'file_save_response':
        return Colors.orange;
      case 'auth_login':
      case 'auth_response':
        return Colors.purple;
      default:
        return Colors.grey;
    }
  }

  String _getMessageSummary(WebSocketMessage message) {
    final data = message.data;
    switch (message.type) {
      case 'graph_save':
        final nodeCount = data['graph_data']?['nodes']?.length ?? 0;
        return '$nodeCount nodes saved';
      case 'graph_save_response':
        return data['success'] == true ? 'saved successfully' : 'save failed';
      case 'graph_load_response':
        final nodeCount = data['graph_data']?['nodes']?.length ?? 0;
        return data['success'] == true
            ? '$nodeCount nodes loaded'
            : 'load failed';
      case 'file_save':
        return 'file: ${data['path'] ?? 'unknown'}';
      case 'auth_response':
        return data['success'] == true ? 'authenticated' : 'auth failed';
      default:
        return data.toString().length > 50
            ? '${data.toString().substring(0, 47)}...'
            : data.toString();
    }
  }

  Widget _buildStatusIndicator(
    BuildContext context,
    WebSocketConnectionStatus status,
    Color color,
  ) {
    if (status == WebSocketConnectionStatus.connecting ||
        status == WebSocketConnectionStatus.reconnecting) {
      return Container(
        width: 8,
        height: 8,
        decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        child: Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: color.withOpacity(0.3),
            shape: BoxShape.circle,
          ),
        )
            .animate(
              onPlay: (controller) => controller.repeat(reverse: true),
            )
            .fadeIn(duration: 500.milliseconds)
            .fadeOut(duration: 500.milliseconds),
      );
    }

    return Container(
      width: 8,
      height: 8,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }

  void _showConnectionDetails(
    BuildContext context,
    WebSocketService webSocketService,
  ) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('WebSocket Connection Details'),
        content: SizedBox(
          width: 400,
          height: 300,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildStatusInfo(context, webSocketService),
              const SizedBox(height: 16),
              const Text(
                'Recent Logs:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Expanded(child: _buildLogsList(context, webSocketService)),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              webSocketService.clearLogs();
              Navigator.of(context).pop();
            },
            child: const Text('Clear Logs'),
          ),
          if (webSocketService.currentStatus !=
              WebSocketConnectionStatus.connected)
            TextButton(
              onPressed: () {
                webSocketService.reconnect();
                Navigator.of(context).pop();
              },
              child: const Text('Reconnect'),
            ),
          if (webSocketService.currentStatus ==
                  WebSocketConnectionStatus.error ||
              webSocketService.hasExhaustedReconnectAttempts)
            TextButton(
              onPressed: () => _showReauthDialog(context),
              child: const Text('Reauthenticate'),
            ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusInfo(
    BuildContext context,
    WebSocketService webSocketService,
  ) {
    final status = webSocketService.currentStatus;
    final logs = webSocketService.logs;

    final reconnectCount = logs.where((log) => log.type == 'RECONNECT').length;
    final errorCount = logs.where((log) => log.isError).length;
    final lastError = logs.where((log) => log.isError).lastOrNull;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: _getStatusColor(status),
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 8),
            Text(
              'Status: ${_getStatusText(status)}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text('Reconnection attempts: $reconnectCount'),
        Text('Error count: $errorCount'),
        if (lastError != null) ...[
          const SizedBox(height: 4),
          Text(
            'Last error: ${lastError.message}',
            style: const TextStyle(fontSize: 12, color: Colors.red),
          ),
        ],
      ],
    );
  }

  Widget _buildLogsList(
    BuildContext context,
    WebSocketService webSocketService,
  ) {
    final logs = webSocketService.logs.reversed.take(20).toList();

    if (logs.isEmpty) {
      return const Center(child: Text('No logs available'));
    }

    return ListView.builder(
      itemCount: logs.length,
      itemBuilder: (context, index) {
        final log = logs[index];
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 4,
                decoration: BoxDecoration(
                  color: log.isError ? Colors.red : Colors.green,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '${log.type}: ${log.message}',
                  style: TextStyle(
                    fontSize: 11,
                    color: log.isError ? Colors.red : Colors.grey[600],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Color _getStatusColor(WebSocketConnectionStatus status) {
    switch (status) {
      case WebSocketConnectionStatus.connected:
        return Colors.green;
      case WebSocketConnectionStatus.connecting:
      case WebSocketConnectionStatus.reconnecting:
        return Colors.orange;
      case WebSocketConnectionStatus.error:
        return Colors.red;
      case WebSocketConnectionStatus.disconnected:
        return Colors.grey;
    }
  }

  String _getStatusText(WebSocketConnectionStatus status) {
    switch (status) {
      case WebSocketConnectionStatus.connected:
        return 'Connected';
      case WebSocketConnectionStatus.connecting:
        return 'Connecting';
      case WebSocketConnectionStatus.reconnecting:
        return 'Reconnecting';
      case WebSocketConnectionStatus.error:
        return 'Error';
      case WebSocketConnectionStatus.disconnected:
        return 'Disconnected';
    }
  }

  void _showReauthDialog(BuildContext context) {
    final webSocketService = Provider.of<WebSocketService>(
      context,
      listen: false,
    );

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reauthenticate'),
        content: Text(
          webSocketService.hasExhaustedReconnectAttempts
              ? 'Maximum reconnection attempts reached. This may be due to authentication issues. '
                  'Would you like to reauthenticate?'
              : 'WebSocket connection failed. This may be due to authentication issues. '
                  'Would you like to reauthenticate?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop(); // Close dialog
              webSocketService.clearAuthAndReconnect();
            },
            child: const Text('Reauthenticate'),
          ),
        ],
      ),
    );
  }
}
