import 'package:flutter/material.dart';
import '../widgets/file_tree_view.dart';
import '../widgets/tabbed_code_editor.dart';

class CodeEditorScreen extends StatelessWidget {
  const CodeEditorScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.background,
      child: Row(
        children: [
          // File explorer sidebar
          Container(
            width: 250,
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              border: Border(
                right: BorderSide(
                  color: Theme.of(context).dividerColor,
                  width: 0.5,
                ),
              ),
            ),
            child: const FileTreeView(),
          ),
          // Main editor area
          const Expanded(child: TabbedCodeEditor()),
        ],
      ),
    );
  }
}
