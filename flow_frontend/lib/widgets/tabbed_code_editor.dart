import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/scheduler.dart';
import 'package:provider/provider.dart';
import '../models/open_file.dart';
import '../state/app_state.dart';
import 'syntax_highlighted_editor.dart';

class TabbedCodeEditor extends StatefulWidget {
  const TabbedCodeEditor({super.key});

  @override
  State<TabbedCodeEditor> createState() => _TabbedCodeEditorState();
}

class _TabbedCodeEditorState extends State<TabbedCodeEditor> {
  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
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
          ],
        );
      },
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
          Text(
            file.fileName,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
            ),
          ),
          const Spacer(),
          IconButton(
            icon: Icon(
              Icons.save,
              size: 16,
              color: file.isModified
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
            ),
            tooltip: 'Save (Cmd+S)',
            onPressed: () => _saveFile(file),
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
    debugPrint(
      '📄 TabbedCodeEditor: Content preview: "${file.content.length > 50 ? file.content.substring(0, 50) + "..." : file.content}"',
    );

    final language = _getLanguageNameFromExtension(file.fileExtension);
    debugPrint(
      '📄 TabbedCodeEditor: Using language "$language" for extension "${file.fileExtension}"',
    );

    return RawKeyboardListener(
      focusNode: FocusNode(),
      onKey: (RawKeyEvent event) {
        if (event is RawKeyDownEvent) {
          // Handle Ctrl+S (or Cmd+S on Mac)
          if ((event.logicalKey == LogicalKeyboardKey.keyS) &&
              (event.isControlPressed || event.isMetaPressed)) {
            _saveFile(file);
          }
        }
      },
      child: Container(
        color: Theme.of(context).colorScheme.background,
        child: SyntaxHighlightedEditor(
          content: file.content,
          language: language,
          onChanged: (text) {
            debugPrint(
              '📝 TabbedCodeEditor: Content changed for ${file.path}, new length: ${text.length}',
            );
            // Defer state update to avoid setState during build
            SchedulerBinding.instance.addPostFrameCallback((_) {
              if (mounted) {
                context.read<AppState>().fileSystemState.updateFileContent(
                  file.path,
                  text,
                );
              }
            });
          },
        ),
      ),
    );
  }

  void _saveFile(OpenFile file) {
    debugPrint('💾 Saving file: ${file.path}');

    // Send save request via WebSocket
    final webSocketService = context.read<AppState>().webSocketService;
    webSocketService.sendMessage('write_file', {
      'path': file.path,
      'content': file.content,
    });

    // Mark as saved locally (the server response will confirm)
    context.read<AppState>().fileSystemState.markFileSaved(file.path);
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
            'Open a file from the explorer to start editing',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
            ),
          ),
        ],
      ),
    );
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
      default:
        return Icons.insert_drive_file;
    }
  }

  String _getLanguageNameFromExtension(String extension) {
    switch (extension.toLowerCase()) {
      case 'dart':
        return 'dart';
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
