import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter_monaco/flutter_monaco.dart';
import 'package:provider/provider.dart';
import '../models/open_file.dart';
import '../state/app_state.dart';
import '../services/websocket_service.dart';
import 'editor/monaco_editor_widget.dart';
import 'flowlang_execution_panel.dart';

class TabbedCodeEditor extends StatefulWidget {
  const TabbedCodeEditor({super.key});

  @override
  State<TabbedCodeEditor> createState() => _TabbedCodeEditorState();
}

class _TabbedCodeEditorState extends State<TabbedCodeEditor> {
  bool _showExecutionPanel = false;
  final Set<String> _savingFiles = {};
  final Map<String, MonacoController?> _monacoControllers = {}; // Store Monaco controllers for each file

  @override
  Widget build(BuildContext context) {
    return KeyboardListener(
      focusNode: FocusNode(),
      onKeyEvent: (KeyEvent event) {
        // Handle Cmd+S (macOS) or Ctrl+S (Windows/Linux)
        if (event is KeyDownEvent) {
          final isCmdOrCtrl = HardwareKeyboard.instance.isMetaPressed || HardwareKeyboard.instance.isControlPressed;
          if (isCmdOrCtrl && event.logicalKey == LogicalKeyboardKey.keyS) {
            final appState = context.read<AppState>();
            final activeFile = appState.fileSystemState.activeFile;
            if (activeFile != null) {
              debugPrint('💾 Keyboard shortcut triggered - getting content from Monaco editor');
              _triggerSaveWithMonacoContent(activeFile);
            }
          }
        }
      },
      child: Consumer<AppState>(
        builder: (context, appState, child) {
        final fileSystemState = appState.fileSystemState;
        final openFiles = fileSystemState.openFiles;
        final activeFile = fileSystemState.activeFile;

        debugPrint(
          '🗂️ TabbedCodeEditor: Building with ${openFiles.length} open files, active: ${activeFile?.path}',
        );

        if (openFiles.isEmpty) {
          debugPrint(
            '🗂️ TabbedCodeEditor: No open files, showing empty state',
          );
          return _buildEmptyState();
        }

        return Column(
          children: [
            // Tab bar
            _buildTabBar(openFiles, activeFile),
            // Editor toolbar
            if (activeFile != null) _buildEditorToolbar(activeFile),
            // Editor area
            Expanded(
              child: activeFile != null
                  ? _buildCodeEditor(activeFile)
                  : _buildEmptyState(),
            ),
            // FlowLang execution panel
            if (activeFile != null && _isFlowLangFile(activeFile.fileExtension))
              _buildFlowLangExecutionPanel(activeFile),
          ],
        );
        },
      ),
    );
  }

  bool _isFlowLangFile(String extension) {
    return extension.toLowerCase() == 'flowlang';
  }

  Widget _buildFlowLangExecutionPanel(OpenFile file) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      height: _showExecutionPanel ? 300 : 0,
      decoration: _showExecutionPanel ? BoxDecoration(
        border: Border(
          top: BorderSide(
            color: Theme.of(context).colorScheme.primary.withOpacity(0.3),
            width: 1,
          ),
        ),
      ) : null,
      child: _showExecutionPanel
          ? FlowLangExecutionPanel(
              code: file.content,
              fileName: file.fileName,
              getCurrentCode: _monacoControllers[file.path] != null 
                  ? () => _monacoControllers[file.path]!.getValue()
                  : null,
            )
          : const SizedBox.shrink(),
    );
  }

  Widget _buildEditorToolbar(OpenFile file) {
    return Container(
      height: 32,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface.withOpacity(0.5),
        border: Border(
          bottom: BorderSide(color: Theme.of(context).dividerColor, width: 0.5),
        ),
      ),
      child: Row(
        children: [
          const SizedBox(width: 8),
          Icon(
            _getFileIcon(file.fileName),
            size: 16,
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
          ),
          const SizedBox(width: 8),
          Text(
            file.fileName,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
            ),
          ),
          const Spacer(),
          // FlowLang execution toggle button
          if (_isFlowLangFile(file.fileExtension))
            Container(
              margin: const EdgeInsets.only(right: 8),
              child: ElevatedButton.icon(
                onPressed: () {
                  setState(() {
                    _showExecutionPanel = !_showExecutionPanel;
                  });
                },
                icon: Icon(
                  _showExecutionPanel ? Icons.keyboard_arrow_down : Icons.keyboard_arrow_up,
                  size: 16,
                ),
                label: Text(_showExecutionPanel ? 'Hide Console' : 'Show Console'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _showExecutionPanel 
                      ? Theme.of(context).colorScheme.primary
                      : Theme.of(context).colorScheme.surface,
                  foregroundColor: _showExecutionPanel
                      ? Theme.of(context).colorScheme.onPrimary
                      : Theme.of(context).colorScheme.onSurface,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  minimumSize: const Size(0, 24),
                ),
              ),
            ),
          IconButton(
            icon: _savingFiles.contains(file.path)
                ? SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  )
                : Icon(
                    Icons.save,
                    size: 16,
                    color: file.isModified
                        ? Theme.of(context).colorScheme.primary
                        : Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                  ),
            tooltip: _savingFiles.contains(file.path)
                ? 'Saving...'
                : file.isModified 
                    ? 'Save (Cmd+S) - Modified'
                    : 'Save (Cmd+S)',
            onPressed: file.content.isNotEmpty && !_savingFiles.contains(file.path)
                ? () {
                    debugPrint('💾 Save button clicked - getting content from Monaco editor');
                    _triggerSaveWithMonacoContent(file);
                  }
                : null,
            iconSize: 16,
          ),
          const SizedBox(width: 4),
        ],
      ),
    );
  }

  Widget _buildTabBar(List<OpenFile> openFiles, OpenFile? activeFile) {
    return Container(
      height: 40,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          bottom: BorderSide(color: Theme.of(context).dividerColor, width: 0.5),
        ),
      ),
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: openFiles.length,
        itemBuilder: (context, index) {
          final file = openFiles[index];
          final isActive = activeFile?.path == file.path;

          return _buildTab(file, isActive);
        },
      ),
    );
  }

  Widget _buildTab(OpenFile file, bool isActive) {
    return InkWell(
      onTap: () {
        context.read<AppState>().fileSystemState.switchToFile(file.path);
      },
      child: Container(
        constraints: const BoxConstraints(maxWidth: 200, minWidth: 120),
        decoration: BoxDecoration(
          color: isActive
              ? Theme.of(context).colorScheme.background
              : Colors.transparent,
          border: Border(
            right: BorderSide(
              color: Theme.of(context).dividerColor,
              width: 0.5,
            ),
            top: isActive
                ? BorderSide(
                    color: Theme.of(context).colorScheme.primary,
                    width: 2,
                  )
                : BorderSide.none,
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(
            children: [
              Icon(
                _getFileIcon(file.fileName),
                size: 16,
                color: isActive
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  file.fileName,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: isActive
                        ? Theme.of(context).colorScheme.onBackground
                        : Theme.of(
                            context,
                          ).colorScheme.onSurface.withOpacity(0.8),
                    fontWeight: isActive ? FontWeight.w500 : FontWeight.normal,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (file.isModified)
                Container(
                  width: 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary,
                    shape: BoxShape.circle,
                  ),
                ),
              const SizedBox(width: 4),
              InkWell(
                onTap: () {
                  // Clean up Monaco controller when closing file
                  _monacoControllers.remove(file.path);
                  context.read<AppState>().fileSystemState.closeFile(file.path);
                },
                child: Icon(
                  Icons.close,
                  size: 16,
                  color: Theme.of(
                    context,
                  ).colorScheme.onSurface.withOpacity(0.6),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCodeEditor(OpenFile file) {
    debugPrint(
      '📄 TabbedCodeEditor: Building editor for ${file.path} with content length: ${file.content.length}',
    );

    final language = _getLanguageNameFromExtension(file.fileExtension);
    debugPrint(
      '📄 TabbedCodeEditor: Using language "$language" for extension "${file.fileExtension}"',
    );

    return Container(
      color: Theme.of(context).colorScheme.background,
      child: MonacoEditorWidget(
        key: ValueKey('monaco_${file.path}_${file.content.length}'),
        content: file.content,
        language: language,
        onChanged: (text) {
          // Update content immediately to ensure it's available for saving
          context.read<AppState>().fileSystemState.updateFileContent(
            file.path,
            text,
          );
        },
        onControllerReady: (controller) {
          // Store the Monaco controller for this file
          _monacoControllers[file.path] = controller;
        },
        // onSave removed - save is handled by keyboard shortcut and button
      ),
    );
  }

  void _triggerSaveWithMonacoContent(OpenFile file) async {
    // Set loading state
    setState(() {
      _savingFiles.add(file.path);
    });

    try {
      // Get content directly from Monaco controller
      String contentToSave;
      final controller = _monacoControllers[file.path];
      if (controller != null) {
        contentToSave = await controller.getValue();
        debugPrint('💾 _triggerSaveWithMonacoContent: Got content from Monaco controller, length: ${contentToSave.length}');
      } else {
        contentToSave = file.content;
        debugPrint('💾 _triggerSaveWithMonacoContent: No Monaco controller available, using file.content length: ${file.content.length}');
      }

      _triggerSave(file, contentToSave);
    } catch (e) {
      debugPrint('💾 Error getting content from Monaco controller: $e');
      // Fallback to file content
      _triggerSave(file, file.content);
    }
  }

  void _triggerSave(OpenFile file, [String? content]) {
    final contentToSave = content ?? file.content;
    debugPrint('💾 _triggerSave: content param length: ${content?.length ?? 'null'}, file.content length: ${file.content.length}, using: ${contentToSave.length}');
    
    // Set loading state
    setState(() {
      _savingFiles.add(file.path);
    });

    StreamSubscription? subscription;
    Timer? timeoutTimer;

    try {
      final webSocketService = context.read<AppState>().webSocketService;
      
      // Listen for save response
      subscription = webSocketService.messages.listen((message) {
        if (message.type == 'file_saved' && 
            message.data['path'] == file.path) {
          final success = message.data['success'] as bool? ?? false;
          
          // Clean up
          timeoutTimer?.cancel();
          subscription?.cancel();
          
          // Remove loading state
          if (mounted) {
            setState(() {
              _savingFiles.remove(file.path);
            });
          }
          
          if (success) {
            // Mark as saved locally
            context.read<AppState>().fileSystemState.markFileSaved(file.path);
          }
        }
      });

      // Send save request via WebSocket
      debugPrint('💾 Sending WebSocket write_file - path: ${file.path}, content length: ${contentToSave.length}');
      debugPrint('💾 WebSocket content preview: ${contentToSave.substring(0, contentToSave.length > 100 ? 100 : contentToSave.length)}...');
      webSocketService.sendMessage('write_file', {
        'path': file.path,
        'content': contentToSave,
      });

      // Set up timeout
      timeoutTimer = Timer(const Duration(seconds: 3), () {
        subscription?.cancel();
        if (mounted) {
          setState(() {
            _savingFiles.remove(file.path);
          });
        }
      });
      
    } catch (e) {
      // Clean up
      subscription?.cancel();
      timeoutTimer?.cancel();
      
      // Remove loading state
      if (mounted) {
        setState(() {
          _savingFiles.remove(file.path);
        });
      }
    }
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.code_off,
            size: 64,
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3),
          ),
          const SizedBox(height: 16),
          Text(
            'No files open',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Open a file from the explorer or create a new one',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
            ),
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: _showNewFileDialog,
            icon: const Icon(Icons.add),
            label: const Text('New File'),
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
            ),
          ),
        ],
      ),
    );
  }

  void _showNewFileDialog() {
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
                _createNewFile(value.trim());
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
                  _createNewFile(fileName);
                }
              },
              child: const Text('Create'),
            ),
          ],
        );
      },
    );
  }

  void _createNewFile(String fileName) {
    debugPrint('📄 TabbedCodeEditor: Creating new file: $fileName');

    final fullPath = 'lib/$fileName'; // Default to lib directory

    final appState = context.read<AppState>();
    
    // Listen for file creation response
    late StreamSubscription subscription;
    subscription = appState.webSocketService.messages.listen((message) {
      if (message.type == 'file_created' && 
          message.data['path'] == fullPath && 
          message.data['success'] == true) {
        // File created successfully, open it in the editor
        if (mounted) {
          // Open the new file in the editor
          appState.fileSystemState.openFile(fullPath, '');
          
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('File created and opened: $fileName'),
              duration: const Duration(seconds: 2),
            ),
          );
        }
        subscription.cancel();
      }
    });

    appState.webSocketService.send(
      WebSocketMessage(
        type: 'file_create',
        data: {
          'dirPath': 'lib',
          'fileName': fileName,
        },
      ),
    );

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Creating file: $fileName'),
        duration: const Duration(seconds: 2),
      ),
    );

    // Fallback: open file after timeout if no response received
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) {
        subscription.cancel();
        // Try to open the file anyway
        appState.fileSystemState.openFile(fullPath, '');
      }
    });
  }

  IconData _getFileIcon(String fileName) {
    final extension = fileName.split('.').last.toLowerCase();

    switch (extension) {
      case 'dart':
        return Icons.code;
      case 'flowlang':
        return Icons.auto_fix_high; // Special icon for FlowLang
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
      case 'kt':
      case 'kts':
        return Icons.code;
      case 'py':
        return Icons.code;
      default:
        return Icons.insert_drive_file;
    }
  }

  String _getLanguageNameFromExtension(String extension) {
    switch (extension.toLowerCase()) {
      case 'dart':
        return 'dart';
      case 'flowlang':
        return 'flowlang'; // FlowLang support
      case 'kt':
      case 'kts':
        return 'kotlin';
      case 'java':
        return 'java';
      case 'json':
        return 'json';
      case 'yaml':
      case 'yml':
        return 'yaml';
      case 'md':
        return 'markdown';
      case 'html':
      case 'htm':
        return 'xml'; // HTML is a subset of XML
      case 'css':
        return 'css';
      case 'js':
      case 'javascript':
        return 'javascript';
      case 'ts':
      case 'typescript':
        return 'javascript'; // TypeScript uses JS highlighting as fallback
      case 'py':
        return 'python';
      case 'sh':
      case 'bash':
        return 'bash';
      default:
        return 'plaintext'; // Default fallback
    }
  }
}
