import 'package:flutter/material.dart';
import 'editor_document.dart';
import 'editor_syntax_highlighter.dart';
import 'editor_input_handler.dart';
import 'editor_view.dart';


class AdvancedCodeEditor extends StatefulWidget {
  final String content;
  final String language;
  final Function(String) onChanged;
  final Function(String)? onSave;

  const AdvancedCodeEditor({
    super.key,
    required this.content,
    required this.language,
    required this.onChanged,
    this.onSave,
  });

  @override
  State<AdvancedCodeEditor> createState() => _AdvancedCodeEditorState();
}

class _AdvancedCodeEditorState extends State<AdvancedCodeEditor> {
  late EditorDocument document;
  late EditorSyntaxHighlighter highlighter;

  @override
  void initState() {
    super.initState();
    document = EditorDocument();
    highlighter = EditorSyntaxHighlighter();
    highlighter.setLanguage(widget.language);
    document.setContent(widget.content);
  }

  @override
  void didUpdateWidget(AdvancedCodeEditor oldWidget) {
    super.didUpdateWidget(oldWidget);

    // Update language if it changed
    if (oldWidget.language != widget.language) {
      highlighter.setLanguage(widget.language);
    }

    // Update content if it changed from outside
    if (oldWidget.content != widget.content &&
        document.getContent() != widget.content) {
      document.setContent(widget.content);
      setState(() {});
    }
  }

  void _onDocumentChanged() {
    setState(() {});
    widget.onChanged(document.getContent());
  }

  void _onVisualUpdate() {
    setState(() {});
  }

  void _onSave(String content) {
    widget.onSave?.call(content);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(4),
      ),
      child: EditorInputHandler(
        document: document,
        onChanged: _onDocumentChanged,
        onVisualUpdate: _onVisualUpdate,
        onSave: _onSave,
        child: EditorView(document: document, highlighter: highlighter),
      ),
    );
  }
}
