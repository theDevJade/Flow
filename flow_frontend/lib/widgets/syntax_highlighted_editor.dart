import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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
  final FocusNode _focusNode = FocusNode();

  // Consistent styling constants
  static const double fontSize = 14.0;
  static const double lineHeight = 1.4;
  static const double actualLineHeight = fontSize * lineHeight;
  static const EdgeInsets editorPadding = EdgeInsets.symmetric(
    horizontal: 16,
    vertical: 8,
  );

  @override
  void initState() {
    super.initState();
    debugPrint(
      '📝 SyntaxHighlightedEditor: Init with content length: ${widget.content.length}',
    );
    debugPrint('📝 SyntaxHighlightedEditor: Language: "${widget.language}"');

    _controller = TextEditingController(text: widget.content);

    _controller.addListener(() {
      if (mounted) {
        widget.onChanged(_controller.text);
      }
    });

    // Sync scroll controllers
    _scrollController.addListener(() {
      if (_lineNumberScrollController.hasClients) {
        _lineNumberScrollController.jumpTo(_scrollController.offset);
      }
    });
  }

  @override
  void didUpdateWidget(SyntaxHighlightedEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    debugPrint(
      '📝 SyntaxHighlightedEditor: didUpdateWidget - new content length: ${widget.content.length}',
    );

    if (widget.content != oldWidget.content &&
        _controller.text != widget.content) {
      debugPrint(
        '📝 SyntaxHighlightedEditor: Content changed externally, updating text',
      );
      _controller.text = widget.content;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    _lineNumberScrollController.dispose();
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
          // Editor area - Simple stable TextField
          Expanded(
            child: Container(
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
                  color: Colors.white,
                ),
                decoration: const InputDecoration(
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.zero,
                  isDense: true,
                ),
                cursorColor: Colors.white,
                cursorWidth: 2,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLineNumbers() {
    final lines = _controller.text.split('\n');
    final lineCount = lines.length;

    return Container(
      height: double.infinity,
      child: SingleChildScrollView(
        controller: _lineNumberScrollController,
        physics:
            const NeverScrollableScrollPhysics(), // Only scroll via main editor
        child: Container(
          padding: EdgeInsets.only(
            top: editorPadding.top,
            left: 8,
            right: 8,
            bottom: editorPadding.bottom,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            mainAxisSize: MainAxisSize.min,
            children: List.generate(lineCount, (index) {
              return SizedBox(
                height: actualLineHeight,
                child: Align(
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
                ),
              );
            }),
          ),
        ),
      ),
    );
  }
}
