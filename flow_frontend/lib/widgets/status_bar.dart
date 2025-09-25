import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../state/workspace_state.dart';
import '../state/file_system_state.dart' as fs;
import '../services/websocket_service.dart';
import '../services/flutter_log_service.dart';

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
      builder: (context) => _ConnectionDetailsDialog(
        webSocketService: webSocketService,
      ),
    );
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
              Navigator.of(context).pop();
              webSocketService.clearAuthAndReconnect();
            },
            child: const Text('Reauthenticate'),
          ),
        ],
      ),
    );
  }
}

class _ConnectionDetailsDialog extends StatefulWidget {
  final WebSocketService webSocketService;

  const _ConnectionDetailsDialog({
    required this.webSocketService,
  });

  @override
  State<_ConnectionDetailsDialog> createState() => _ConnectionDetailsDialogState();
}

class _ConnectionDetailsDialogState extends State<_ConnectionDetailsDialog>
    with TickerProviderStateMixin {
  late TabController _tabController;
  bool _autoScroll = true;
  bool _showErrorsOnly = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      child: Container(
        width: 800,
        height: 600,
        child: Column(
          children: [
            _buildHeader(context),
            _buildTabBar(context),
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildWebSocketLogs(context),
                  _buildFlutterLogs(context),
                  _buildCombinedLogs(context),
                ],
              ),
            ),
            _buildActions(context),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final status = widget.webSocketService.currentStatus;
    final logs = widget.webSocketService.logs;
    final errorCount = logs.where((log) => log.isError).length;
    final reconnectCount = logs.where((log) => log.type == 'RECONNECT').length;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 1,
          ),
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.wifi,
            color: _getStatusColor(status),
            size: 24,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Connection Details',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: _getStatusColor(status),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text('Status: ${_getStatusText(status)}'),
                    const SizedBox(width: 16),
                    Text('Errors: $errorCount'),
                    const SizedBox(width: 16),
                    Text('Reconnects: $reconnectCount'),
                  ],
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.close),
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 1,
          ),
        ),
      ),
      child: TabBar(
        controller: _tabController,
        tabs: const [
          Tab(text: 'WebSocket Logs', icon: Icon(Icons.wifi, size: 16)),
          Tab(text: 'Flutter Logs', icon: Icon(Icons.bug_report, size: 16)),
          Tab(text: 'Combined', icon: Icon(Icons.merge, size: 16)),
        ],
      ),
    );
  }

  Widget _buildWebSocketLogs(BuildContext context) {
    return Consumer<WebSocketService>(
      builder: (context, webSocketService, child) {
        final logs = _showErrorsOnly
            ? webSocketService.logs.where((log) => log.isError).toList()
            : webSocketService.logs;

        return _buildLogViewer(
          context,
          logs.reversed.take(100).toList(),
          'WebSocket',
          () => webSocketService.clearLogs(),
        );
      },
    );
  }

  Widget _buildFlutterLogs(BuildContext context) {
    return Consumer<FlutterLogService>(
      builder: (context, flutterLogService, child) {
        final logs = _showErrorsOnly
            ? flutterLogService.getErrorLogs()
            : flutterLogService.recentLogs;

        return _buildLogViewer(
          context,
          logs,
          'Flutter',
          () => flutterLogService.clearLogs(),
        );
      },
    );
  }

  Widget _buildCombinedLogs(BuildContext context) {
    return Consumer2<WebSocketService, FlutterLogService>(
      builder: (context, webSocketService, flutterLogService, child) {
        final wsLogs = webSocketService.logs.map((log) => _CombinedLogEntry(
          timestamp: log.timestamp,
          type: log.type,
          message: log.message,
          isError: log.isError,
          source: 'WebSocket',
        )).toList();

        final flutterLogs = flutterLogService.logs.map((log) => _CombinedLogEntry(
          timestamp: log.timestamp,
          type: log.type,
          message: log.message,
          isError: log.isError,
          source: 'Flutter',
        )).toList();

        final allLogs = [...wsLogs, ...flutterLogs]
          ..sort((a, b) => b.timestamp.compareTo(a.timestamp));

        final filteredLogs = _showErrorsOnly
            ? allLogs.where((log) => log.isError).toList()
            : allLogs;

        return _buildLogViewer(
          context,
          filteredLogs.take(100).toList(),
          'Combined',
          () {
            webSocketService.clearLogs();
            flutterLogService.clearLogs();
          },
        );
      },
    );
  }

  Widget _buildLogViewer(
    BuildContext context,
    List<dynamic> logs,
    String source,
    VoidCallback onClear,
  ) {
    return Column(
      children: [
        _buildLogControls(context, onClear),
        Expanded(
          child: Container(
            margin: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.black87,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey.shade600),
            ),
            child: logs.isEmpty
                ? const Center(
                    child: Text(
                      'No logs available',
                      style: TextStyle(color: Colors.grey),
                    ),
                  )
                : ListView.builder(
                    itemCount: logs.length,
                    itemBuilder: (context, index) {
                      final log = logs[index];
                      return _buildLogEntry(context, log);
                    },
                  ),
          ),
        ),
      ],
    );
  }

  Widget _buildLogControls(BuildContext context, VoidCallback onClear) {
    return Container(
      padding: const EdgeInsets.all(8),
      child: Row(
        children: [
          Checkbox(
            value: _autoScroll,
            onChanged: (value) {
              setState(() {
                _autoScroll = value ?? false;
              });
            },
          ),
          const Text('Auto-scroll'),
          const SizedBox(width: 16),
          Checkbox(
            value: _showErrorsOnly,
            onChanged: (value) {
              setState(() {
                _showErrorsOnly = value ?? false;
              });
            },
          ),
          const Text('Errors only'),
          const Spacer(),
          TextButton.icon(
            onPressed: onClear,
            icon: const Icon(Icons.clear_all, size: 16),
            label: const Text('Clear'),
          ),
        ],
      ),
    );
  }

  Widget _buildLogEntry(BuildContext context, dynamic log) {
    final isError = log.isError;
    final timestamp = log.timestamp;
    final type = log.type;
    final message = log.message;


    String source;
    if (log is _CombinedLogEntry) {
      source = log.source;
    } else if (log is WebSocketLogEntry) {
      source = 'WebSocket';
    } else if (log is FlutterLogEntry) {
      source = 'Flutter';
    } else {
      source = 'Unknown';
    }

    final timeStr = '${timestamp.hour.toString().padLeft(2, '0')}:'
        '${timestamp.minute.toString().padLeft(2, '0')}:'
        '${timestamp.second.toString().padLeft(2, '0')}';

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 1, horizontal: 4),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isError ? Colors.red.shade900.withOpacity(0.3) : Colors.transparent,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 4,
            height: 16,
            decoration: BoxDecoration(
              color: isError ? Colors.red.shade400 : Colors.green.shade400,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            '[$timeStr]',
            style: const TextStyle(
              fontFamily: 'monospace',
              fontSize: 10,
              color: Colors.grey,
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: source == 'WebSocket' ? Colors.blue.shade900 : Colors.purple.shade900,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              source,
              style: const TextStyle(
                fontFamily: 'monospace',
                fontSize: 9,
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            '[$type]',
            style: TextStyle(
              fontFamily: 'monospace',
              fontSize: 10,
              color: isError ? Colors.red.shade300 : Colors.grey.shade400,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontFamily: 'monospace',
                fontSize: 11,
                color: isError ? Colors.red.shade300 : Colors.green.shade300,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActions(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 1,
          ),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          if (widget.webSocketService.currentStatus !=
              WebSocketConnectionStatus.connected)
            TextButton.icon(
              onPressed: () {
                widget.webSocketService.reconnect();
                Navigator.of(context).pop();
              },
              icon: const Icon(Icons.refresh, size: 16),
              label: const Text('Reconnect'),
            ),
          if (widget.webSocketService.currentStatus ==
                  WebSocketConnectionStatus.error ||
              widget.webSocketService.hasExhaustedReconnectAttempts)
            TextButton.icon(
              onPressed: () => _showReauthDialog(context),
              icon: const Icon(Icons.login, size: 16),
              label: const Text('Reauthenticate'),
            ),
          const SizedBox(width: 8),
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  void _showReauthDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reauthenticate'),
        content: Text(
          widget.webSocketService.hasExhaustedReconnectAttempts
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
              Navigator.of(context).pop();
              widget.webSocketService.clearAuthAndReconnect();
            },
            child: const Text('Reauthenticate'),
          ),
        ],
      ),
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
}

class _CombinedLogEntry {
  final DateTime timestamp;
  final String type;
  final String message;
  final bool isError;
  final String source;

  _CombinedLogEntry({
    required this.timestamp,
    required this.type,
    required this.message,
    required this.isError,
    required this.source,
  });
}
