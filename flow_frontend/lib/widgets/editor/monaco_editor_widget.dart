import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_monaco/flutter_monaco.dart';

class MonacoEditorWidget extends StatefulWidget {
  final String content;
  final String language;
  final Function(String) onChanged;
  final Function(bool)? onInitialized; // Callback for initialization status
  final Function(MonacoController?)? onControllerReady; // Callback when controller is ready

  const MonacoEditorWidget({
    super.key,
    required this.content,
    required this.language,
    required this.onChanged,
    this.onInitialized,
    this.onControllerReady,
  });

  @override
  State<MonacoEditorWidget> createState() => _MonacoEditorWidgetState();
}

class _MonacoEditorWidgetState extends State<MonacoEditorWidget> {
  MonacoController? _controller;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _initializeEditor();
  }

  Future<void> _initializeEditor() async {
    try {
      // Ensure Monaco assets are ready
      await MonacoAssets.ensureReady();
      
      // Create the Monaco controller with error handling
      _controller = await MonacoController.create(
        options: EditorOptions(
          language: _getMonacoLanguage(widget.language),
          theme: _getTheme(),
          fontSize: 14,
          minimap: true,
          lineNumbers: true,
          wordWrap: false,
          automaticLayout: true,
          scrollBeyondLastLine: false,
          cursorBlinking: CursorBlinking.smooth,
          cursorStyle: CursorStyle.line,
          renderWhitespace: RenderWhitespace.boundary,
          bracketPairColorization: true,
          formatOnPaste: true,
          formatOnType: true,
          quickSuggestions: true,
          parameterHints: true,
          hover: true,
          contextMenu: true,
          mouseWheelZoom: false,
          readOnly: false,
        ),
      );

      // Monaco editor should handle Cmd+S automatically
      // The onSave callback will be triggered by the parent widget

      // Set initial content with error handling
      try {
        await _controller!.setValue(widget.content);
      } catch (e) {
        debugPrint('Warning: Could not set initial content: $e');
        // Continue anyway, the editor will still work
      }

      // Listen to content changes with error handling
      try {
        _controller!.onContentChanged.listen((isFlush) {
          if (!isFlush) {
            _getContentAndNotify();
          }
        });
      } catch (e) {
        debugPrint('Warning: Could not set up content change listener: $e');
      }

      // Listen to focus events for potential save operations
      try {
        _controller!.onFocus.listen((_) {
          // Could add auto-save logic here if needed
        });
      } catch (e) {
        debugPrint('Warning: Could not set up focus listener: $e');
      }


      setState(() {
        _isInitialized = true;
      });
      
      // Provide the controller to parent
      widget.onControllerReady?.call(_controller);
      
      widget.onInitialized?.call(true);
    } catch (e) {
      debugPrint('Error initializing Monaco editor: $e');
      // Don't set _isInitialized to false, show error UI instead
      setState(() {
        _isInitialized = true; // Show error UI
      });
      widget.onInitialized?.call(false);
    }
  }

  Future<void> _getContentAndNotify() async {
    if (_controller != null) {
      try {
        final content = await _controller!.getValue();
        debugPrint('📝 MonacoEditor: Content changed, length: ${content.length}');
        widget.onChanged(content);
      } catch (e) {
        debugPrint('Error getting content from Monaco editor: $e');
      }
    }
  }

  // Method to force content synchronization
  Future<void> forceContentSync() async {
    if (_controller != null) {
      try {
        final content = await _controller!.getValue();
        debugPrint('🔄 MonacoEditor: Force content sync, length: ${content.length}');
        widget.onChanged(content);
      } catch (e) {
        debugPrint('Error force syncing content from Monaco editor: $e');
      }
    }
  }

  // Method to get current content from editor
  Future<String> getCurrentContent() async {
    if (_controller != null) {
      try {
        return await _controller!.getValue();
      } catch (e) {
        debugPrint('Error getting content from Monaco editor: $e');
        return widget.content;
      }
    }
    return widget.content;
  }

  @override
  void didUpdateWidget(MonacoEditorWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    // Update content if it changed from outside
    if (oldWidget.content != widget.content && _controller != null) {
      _controller!.setValue(widget.content);
    }

    // Update language if it changed
    if (oldWidget.language != widget.language && _controller != null) {
      _controller!.setLanguage(_getMonacoLanguage(widget.language));
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  MonacoLanguage _getMonacoLanguage(String language) {
    switch (language.toLowerCase()) {
      case 'dart':
        return MonacoLanguage.dart;
      case 'flowlang':
        return MonacoLanguage.javascript; // Use JS as fallback for FlowLang
      case 'kotlin':
      case 'kt':
      case 'kts':
        return MonacoLanguage.kotlin;
      case 'java':
        return MonacoLanguage.java;
      case 'json':
        return MonacoLanguage.json;
      case 'yaml':
      case 'yml':
        return MonacoLanguage.yaml;
      case 'markdown':
      case 'md':
        return MonacoLanguage.markdown;
      case 'html':
      case 'htm':
        return MonacoLanguage.html;
      case 'css':
        return MonacoLanguage.css;
      case 'javascript':
      case 'js':
        return MonacoLanguage.javascript;
      case 'typescript':
      case 'ts':
        return MonacoLanguage.typescript;
      case 'python':
      case 'py':
        return MonacoLanguage.python;
      case 'bash':
      case 'sh':
        return MonacoLanguage.shell;
      case 'xml':
        return MonacoLanguage.xml;
      case 'sql':
        return MonacoLanguage.sql;
      case 'dockerfile':
        return MonacoLanguage.dockerfile;
      case 'go':
        return MonacoLanguage.go;
      case 'rust':
        return MonacoLanguage.rust;
      case 'swift':
        return MonacoLanguage.swift;
      case 'csharp':
      case 'cs':
        return MonacoLanguage.csharp;
      case 'cpp':
      case 'c++':
        return MonacoLanguage.cpp;
      case 'c':
        return MonacoLanguage.c;
      case 'php':
        return MonacoLanguage.php;
      case 'ruby':
        return MonacoLanguage.ruby;
      case 'scala':
        return MonacoLanguage.scala;
      case 'plaintext':
      default:
        return MonacoLanguage.plaintext;
    }
  }

  MonacoTheme _getTheme() {
    // You can make this dynamic based on app theme
    return MonacoTheme.vsDark;
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return Container(
        color: Theme.of(context).colorScheme.surface,
        child: const Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_controller == null) {
      return Container(
        color: Theme.of(context).colorScheme.surface,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.error_outline,
                size: 64,
                color: Theme.of(context).colorScheme.error,
              ),
              const SizedBox(height: 16),
              Text(
                'Editor Failed to Load',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  color: Theme.of(context).colorScheme.error,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'The Monaco Editor could not be initialized.\nThis may be due to WebView compatibility issues.',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                ),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () {
                  setState(() {
                    _isInitialized = false;
                    _controller = null;
                  });
                  _initializeEditor();
                },
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    return Container(
      color: Theme.of(context).colorScheme.surface,
      child: _controller!.webViewWidget,
    );
  }
}
