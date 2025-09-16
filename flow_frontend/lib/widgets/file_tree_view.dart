import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../services/websocket_service.dart';
import '../services/file_system_service.dart';

class FileTreeView extends StatefulWidget {
  const FileTreeView({super.key});

  @override
  State<FileTreeView> createState() => _FileTreeViewState();
}

class _FileTreeViewState extends State<FileTreeView> {
  final Set<String> _expandedNodes = {};
  final Set<String> _loadingFiles =
      {}; // Track which files are currently loading

  @override
  void initState() {
    super.initState();
    // Load file tree on initialization, but only if authenticated and connected
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _tryLoadFileTree();
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Auto-expand root directory when file tree is loaded
    final appState = context.read<AppState>();
    if (appState.fileSystemState.fileTree != null && _expandedNodes.isEmpty) {
      setState(() {
        _expandedNodes.add(appState.fileSystemState.fileTree!.fullPath);
      });
    }
  }

  void _tryLoadFileTree() {
    final appState = context.read<AppState>();
    if (appState.webSocketService.currentStatus ==
        WebSocketConnectionStatus.connected) {
      appState.fileSystemService.requestFileTree();
    }
  }

  void _loadFile(String filePath) {
    debugPrint('🔍 FileTreeView: Loading file: $filePath');

    setState(() {
      _loadingFiles.add(filePath);
    });

    final appState = context.read<AppState>();

    // Listen to WebSocket messages to detect when file loading completes
    late StreamSubscription subscription;
    subscription = appState.webSocketService.messages.listen((message) {
      if (!mounted) {
        subscription.cancel();
        return;
      }

      if (message.type == 'file_content' && message.data['path'] == filePath) {
        debugPrint(
          '✅ FileTreeView: File load response received for: $filePath',
        );
        setState(() {
          _loadingFiles.remove(filePath);
        });
        subscription.cancel();

        appState.fileSystemState.openFile(filePath, message.data['content']);
      } else if (message.type == 'error' &&
          message.data['request_id'] == 'read_file_$filePath') {
        debugPrint('❌ FileTreeView: Failed to load: $filePath');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to load file: ${filePath.split('/').last}'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
        setState(() {
          _loadingFiles.remove(filePath);
        });
        subscription.cancel();
      }
    });

    // Send the read file request
    appState.fileSystemService.readFile(filePath).catchError((error) {
      debugPrint('❌ FileTreeView: Error reading file: $error');
      if (mounted) {
        setState(() {
          _loadingFiles.remove(filePath);
        });
        subscription.cancel();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error loading file: ${filePath.split('/').last}'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
      return null; // Return null on error
    });

    // Set a timeout to remove loading state if no response comes
    Timer(const Duration(seconds: 5), () {
      if (mounted && _loadingFiles.contains(filePath)) {
        setState(() {
          _loadingFiles.remove(filePath);
        });
        subscription.cancel();
        debugPrint('⏰ FileTreeView: Timeout loading file: $filePath');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Timeout loading file: ${filePath.split('/').last}'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        // Check if user is authenticated
        if (!appState.authService.isAuthenticated) {
          return _buildNotAuthenticatedState();
        }

        // Check WebSocket connection status
        if (appState.webSocketService.currentStatus !=
            WebSocketConnectionStatus.connected) {
          return _buildConnectingState(appState.webSocketService.currentStatus);
        }

        // Auto-load file tree if not loaded yet
        if (appState.fileSystemState.fileTree == null) {
          return _buildLoadingState();
        }

        // Show file tree if loaded
        return _buildFileTreeView(appState.fileSystemState.fileTree!);
      },
    );
  }

  Widget _buildNotAuthenticatedState() {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          right: BorderSide(color: Theme.of(context).dividerColor, width: 1),
        ),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.lock_outline,
              size: 48,
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
            ),
            const SizedBox(height: 16),
            Text(
              'Please login to view files',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConnectingState(WebSocketConnectionStatus status) {
    String message;
    switch (status) {
      case WebSocketConnectionStatus.connecting:
        message = 'Connecting to server...';
        break;
      case WebSocketConnectionStatus.reconnecting:
        message = 'Reconnecting...';
        break;
      case WebSocketConnectionStatus.error:
        message = 'Connection error';
        break;
      default:
        message = 'Not connected';
    }

    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          right: BorderSide(color: Theme.of(context).dividerColor, width: 1),
        ),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (status == WebSocketConnectionStatus.connecting ||
                status == WebSocketConnectionStatus.reconnecting)
              const CircularProgressIndicator()
            else
              Icon(
                Icons.wifi_off,
                size: 48,
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
              ),
            const SizedBox(height: 16),
            Text(
              message,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingState() {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          right: BorderSide(color: Theme.of(context).dividerColor, width: 1),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
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
                  Icons.folder_outlined,
                  size: 20,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  'Explorer',
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ),
          const Expanded(child: Center(child: CircularProgressIndicator())),
        ],
      ),
    );
  }

  Widget _buildFileTreeView(FileNode fileTree) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          right: BorderSide(color: Theme.of(context).dividerColor, width: 1),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
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
                  Icons.folder_outlined,
                  size: 20,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  'Explorer',
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ),
          Expanded(
            child: SingleChildScrollView(child: _buildFileTree(fileTree)),
          ),
        ],
      ),
    );
  }

  Widget _buildFileTree(FileNode node, [int depth = 0]) {
    final isExpanded = _expandedNodes.contains(node.fullPath);

    if (node.isDirectory) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildTreeItem(
            icon: isExpanded ? Icons.folder_open : Icons.folder,
            name: node.name,
            fullPath: node.fullPath,
            isDirectory: true,
            onTap: () {
              setState(() {
                if (isExpanded) {
                  _expandedNodes.remove(node.fullPath);
                } else {
                  _expandedNodes.add(node.fullPath);
                }
              });
            },
            depth: depth,
          ),
          if (isExpanded)
            ...node.children.map((child) => _buildFileTree(child, depth + 1)),
        ],
      );
    } else {
      return _buildTreeItem(
        icon: _getFileIcon(node.name),
        name: node.name,
        fullPath: node.fullPath,
        isDirectory: false,
        isLoading: _loadingFiles.contains(node.fullPath),
        onTap: () {
          _loadFile(node.fullPath);
        },
        depth: depth,
      );
    }
  }

  Widget _buildTreeItem({
    required IconData icon,
    required String name,
    required String fullPath,
    required bool isDirectory,
    required VoidCallback onTap,
    required int depth,
    bool isLoading = false,
  }) {
    return GestureDetector(
      onSecondaryTapUp: (details) {
        _showContextMenu(
          context,
          details.globalPosition,
          fullPath,
          isDirectory,
        );
      },
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: EdgeInsets.only(
            left: 12 + (depth * 16.0),
            right: 12,
            top: 6,
            bottom: 6,
          ),
          child: Row(
            children: [
              Icon(
                icon,
                size: 18,
                color: isDirectory
                    ? Theme.of(context).colorScheme.primary.withOpacity(0.8)
                    : Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  name,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(
                      context,
                    ).colorScheme.onSurface.withOpacity(0.9),
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (isLoading)
                Container(
                  margin: const EdgeInsets.only(left: 8),
                  child: SizedBox(
                    width: 12,
                    height: 12,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  void _showContextMenu(
    BuildContext context,
    Offset position,
    String fullPath,
    bool isDirectory,
  ) {
    showMenu(
      context: context,
      position: RelativeRect.fromLTRB(
        position.dx,
        position.dy,
        position.dx + 1,
        position.dy + 1,
      ),
      items: [
        if (!isDirectory)
          PopupMenuItem(
            value: 'open',
            child: Row(
              children: [
                Icon(Icons.open_in_new, size: 16),
                SizedBox(width: 8),
                Text('Open'),
              ],
            ),
          ),
        PopupMenuItem(
          value: 'rename',
          child: Row(
            children: [
              Icon(Icons.edit, size: 16),
              SizedBox(width: 8),
              Text('Rename'),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'delete',
          child: Row(
            children: [
              Icon(Icons.delete, size: 16),
              SizedBox(width: 8),
              Text('Delete'),
            ],
          ),
        ),
        if (isDirectory)
          PopupMenuItem(
            value: 'new_file',
            child: Row(
              children: [
                Icon(Icons.add, size: 16),
                SizedBox(width: 8),
                Text('New File'),
              ],
            ),
          ),
      ],
    ).then((value) {
      if (value != null) {
        _handleContextMenuAction(value, fullPath, isDirectory);
      }
    });
  }

  void _handleContextMenuAction(
    String action,
    String fullPath,
    bool isDirectory,
  ) {
    debugPrint(
      '🔧 FileTreeView: Context menu action: $action on $fullPath (isDirectory: $isDirectory)',
    );

    switch (action) {
      case 'open':
        if (!isDirectory) {
          _loadFile(fullPath);
        }
        break;
      case 'rename':
        _showRenameDialog(fullPath, isDirectory);
        break;
      case 'delete':
        _showDeleteConfirmation(fullPath, isDirectory);
        break;
      case 'new_file':
        _showNewFileDialog(fullPath);
        break;
    }
  }

  IconData _getFileIcon(String fileName) {
    final extension = fileName.split('.').last.toLowerCase();

    switch (extension) {
      case 'dart':
        return Icons.code;
      case 'json':
        return Icons.data_object;
      case 'yaml':
      case 'yml':
        return Icons.settings;
      case 'md':
        return Icons.description;
      case 'html':
        return Icons.web;
      case 'css':
        return Icons.palette;
      case 'js':
        return Icons.javascript;
      case 'ts':
        return Icons.code;
      case 'png':
      case 'jpg':
      case 'jpeg':
      case 'gif':
        return Icons.image;
      default:
        return Icons.insert_drive_file;
    }
  }

  void _showDeleteConfirmation(String fullPath, bool isDirectory) {
    final fileName = fullPath.split('/').last;
    final itemType = isDirectory ? 'folder' : 'file';

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Delete $itemType'),
          content: Text(
            'Are you sure you want to delete the $itemType "$fileName"?\n\nThis action cannot be undone.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _deleteItem(fullPath, isDirectory);
              },
              style: TextButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.error,
              ),
              child: const Text('Delete'),
            ),
          ],
        );
      },
    );
  }

  void _deleteItem(String fullPath, bool isDirectory) {
    debugPrint(
      '🗑️ FileTreeView: Deleting ${isDirectory ? 'folder' : 'file'}: $fullPath',
    );

    final appState = context.read<AppState>();
    appState.webSocketService.send(
      WebSocketMessage(
        type: isDirectory ? 'folder_delete' : 'file_delete',
        data: {'path': fullPath},
      ),
    );

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Deleting ${isDirectory ? 'folder' : 'file'}: ${fullPath.split('/').last}',
        ),
        duration: const Duration(seconds: 2),
      ),
    );

    // Refresh the file tree after deletion
    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted) {
        appState.fileSystemService.initialize();
      }
    });
  }

  void _showRenameDialog(String fullPath, bool isDirectory) {
    final fileName = fullPath.split('/').last;
    final controller = TextEditingController(text: fileName);

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Rename ${isDirectory ? 'folder' : 'file'}'),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              labelText: 'New name',
              border: OutlineInputBorder(),
            ),
            autofocus: true,
            onSubmitted: (value) {
              if (value.trim().isNotEmpty && value.trim() != fileName) {
                Navigator.of(context).pop();
                _renameItem(fullPath, value.trim(), isDirectory);
              }
            },
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                final newName = controller.text.trim();
                if (newName.isNotEmpty && newName != fileName) {
                  Navigator.of(context).pop();
                  _renameItem(fullPath, newName, isDirectory);
                }
              },
              child: const Text('Rename'),
            ),
          ],
        );
      },
    );
  }

  void _renameItem(String fullPath, String newName, bool isDirectory) {
    debugPrint(
      '✏️ FileTreeView: Renaming ${isDirectory ? 'folder' : 'file'}: $fullPath to $newName',
    );

    final pathSegments = fullPath.split('/');
    pathSegments[pathSegments.length - 1] = newName;
    final newPath = pathSegments.join('/');

    final appState = context.read<AppState>();
    appState.webSocketService.send(
      WebSocketMessage(
        type: isDirectory ? 'folder_rename' : 'file_rename',
        data: {'oldPath': fullPath, 'newPath': newPath},
      ),
    );

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Renaming ${isDirectory ? 'folder' : 'file'} to: $newName',
        ),
        duration: const Duration(seconds: 2),
      ),
    );

    // Refresh the file tree after rename
    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted) {
        appState.fileSystemService.initialize();
      }
    });
  }

  void _showNewFileDialog(String parentPath) {
    final controller = TextEditingController();

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Create new file'),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              labelText: 'File name',
              hintText: 'example.dart',
              border: OutlineInputBorder(),
            ),
            autofocus: true,
            onSubmitted: (value) {
              if (value.trim().isNotEmpty) {
                Navigator.of(context).pop();
                _createNewFile(parentPath, value.trim());
              }
            },
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                final fileName = controller.text.trim();
                if (fileName.isNotEmpty) {
                  Navigator.of(context).pop();
                  _createNewFile(parentPath, fileName);
                }
              },
              child: const Text('Create'),
            ),
          ],
        );
      },
    );
  }

  void _createNewFile(String parentPath, String fileName) {
    debugPrint('📄 FileTreeView: Creating new file: $fileName in $parentPath');

    final fullPath = '$parentPath/$fileName';

    final appState = context.read<AppState>();
    appState.webSocketService.send(
      WebSocketMessage(
        type: 'file_create',
        data: {
          'path': fullPath,
          'content': '', // Empty file content
        },
      ),
    );

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Creating file: $fileName'),
        duration: const Duration(seconds: 2),
      ),
    );

    // Refresh the file tree and expand the parent directory
    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted) {
        setState(() {
          _expandedNodes.add(parentPath);
        });
        appState.fileSystemService.initialize();
      }
    });
  }
}
