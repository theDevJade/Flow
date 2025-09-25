import 'package:flutter/material.dart';
import 'editor_document.dart';
import 'editor_syntax_highlighter.dart';

class EditorLine extends StatelessWidget {
  const EditorLine({
    super.key,
    this.lineNumber = 0,
    this.text = '',
    required this.document,
    required this.highlighter,
  });

  final int lineNumber;
  final String text;
  final EditorDocument document;
  final EditorSyntaxHighlighter highlighter;

  @override
  Widget build(BuildContext context) {
    // Skip syntax highlighting for empty lines
    if (text.isEmpty) {
      return Container(
        color: background,
        height: fontSize * 1.4,
        child: const SizedBox.shrink(),
      );
    }

    List<InlineSpan> spans;
    try {
      spans = highlighter.run(text, lineNumber, document);
    } catch (e) {
      // Fallback to simple text if syntax highlighting fails
      spans = [
        TextSpan(
          text: text,
          style: TextStyle(
            fontFamily: 'monospace',
            fontSize: fontSize,
            color: foreground,
            height: 1.4,
          ),
        ),
      ];
    }

    final gutterStyle = TextStyle(
      fontFamily: 'monospace',
      fontSize: gutterFontSize,
      color: comment,
      height: 1.4,
    );

    double gutterWidth = getTextExtents(
      ' ${document.lines.length} ',
      gutterStyle,
    ).width;

    return Container(
      color: background,
      child: Stack(
        children: [
          Padding(
            padding: EdgeInsets.only(left: gutterWidth),
            child: RichText(text: TextSpan(children: spans), softWrap: false),
          ),
          Container(
            width: gutterWidth,
            alignment: Alignment.centerRight,
            decoration: BoxDecoration(
              color: const Color(0xFF3C3C3C),
              border: Border(
                right: BorderSide(color: const Color(0xFF555555), width: 1),
              ),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8.0),
              child: Text('${lineNumber + 1} ', style: gutterStyle),
            ),
          ),
        ],
      ),
    );
  }
}

class EditorView extends StatefulWidget {
  const EditorView({
    super.key,
    required this.document,
    required this.highlighter,
  });

  final EditorDocument document;
  final EditorSyntaxHighlighter highlighter;

  @override
  State<EditorView> createState() => _EditorViewState();
}

class _EditorViewState extends State<EditorView> {
  late ScrollController scrollController;

  @override
  void initState() {
    super.initState();
    scrollController = ScrollController();
  }

  @override
  void dispose() {
    scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: background,
      child: ListView.builder(
        controller: scrollController,
        itemCount: widget.document.lines.length,
        itemBuilder: (BuildContext context, int index) {
          return EditorLine(
            lineNumber: index,
            text: widget.document.lines[index],
            document: widget.document,
            highlighter: widget.highlighter,
          );
        },
      ),
    );
  }
}
