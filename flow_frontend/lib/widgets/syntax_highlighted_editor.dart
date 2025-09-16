import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_highlight/flutter_highlight.dart';
import 'package:flutter_highlight/themes/monokai-sublime.dart';

class SyntaxHighlightedEditor extends StatefulWidget {
  final String content;
  final String language;
  final Function(String) onChanged;

  const SyntaxHighlightedEditor({
    super.key,
    required this.content,
    required this.language,
    required this.onChanged,
  });

  @override
  State<SyntaxHighlightedEditor> createState() =>
      _SyntaxHighlightedEditorState();
}

class _SyntaxHighlightedEditorState extends State<SyntaxHighlightedEditor> {
  late TextEditingController _controller;
  final ScrollController _scrollController = ScrollController();
  final ScrollController _lineNumberScrollController = ScrollController();
  final ScrollController _backgroundScrollController = ScrollController();
  final FocusNode _focusNode = FocusNode();

  // Consistent styling constants
  static const double fontSize = 14.0;
  static const double lineHeight = 1.4;
  static const double actualLineHeight = fontSize * lineHeight;
  static const EdgeInsets editorPadding = EdgeInsets.all(16);

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.content);

    // Sync all scroll controllers
    _scrollController.addListener(() {
      if (_lineNumberScrollController.hasClients) {
        _lineNumberScrollController.jumpTo(_scrollController.offset);
      }
      if (_backgroundScrollController.hasClients) {
        _backgroundScrollController.jumpTo(_scrollController.offset);
      }
    });
  }

  @override
  void didUpdateWidget(SyntaxHighlightedEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.content != oldWidget.content &&
        _controller.text != widget.content) {
      _controller.text = widget.content;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    _lineNumberScrollController.dispose();
    _backgroundScrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF272822), // Monokai background
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Line numbers
          Container(
            width: 60,
            decoration: const BoxDecoration(
              color: Color(0xFF3C3C3C),
              border: Border(
                right: BorderSide(color: Color(0xFF555555), width: 1),
              ),
            ),
            child: _buildLineNumbers(),
          ),
          // Editor area
          Expanded(
            child: Stack(
              children: [
                // Syntax highlighted text (background)
                Positioned.fill(
                  child: Container(
                    padding: editorPadding,
                    child: SingleChildScrollView(
                      controller: _backgroundScrollController,
                      child: Container(
                        padding: const EdgeInsets.only(
                          bottom: 50,
                        ), // Extra bottom padding
                        child: HighlightView(
                          _controller.text,
                          language: widget.language,
                          theme: monokaiSublimeTheme,
                          padding: EdgeInsets.zero,
                          textStyle: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: fontSize,
                            height: lineHeight,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                // Transparent text field (foreground) for input
                Container(
                  padding: editorPadding,
                  child: TextField(
                    controller: _controller,
                    focusNode: _focusNode,
                    maxLines: null,
                    scrollController: _scrollController,
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: fontSize,
                      height: lineHeight,
                      color: Colors.transparent,
                    ),
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.zero,
                      isDense: true,
                    ),
                    onChanged: widget.onChanged,
                    cursorColor: Colors.white,
                    cursorWidth: 2,
                    cursorHeight: fontSize + 2, // Set explicit cursor height
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLineNumbers() {
    final lines = _controller.text.split('\n');
    return SingleChildScrollView(
      controller: _lineNumberScrollController,
      child: Container(
        padding: EdgeInsets.only(
          top: editorPadding.top,
          left: 8,
          right: 8,
          bottom: editorPadding.bottom + 50, // Match the extra bottom padding
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: List.generate(lines.length, (index) {
            return Container(
              height: actualLineHeight,
              alignment: Alignment.centerRight,
              child: Text(
                '${index + 1}',
                style: const TextStyle(
                  color: Color(0xFF75715E),
                  fontSize: fontSize,
                  height: lineHeight,
                  fontFamily: 'monospace',
                ),
              ),
            );
          }),
        ),
      ),
    );
  }
}
