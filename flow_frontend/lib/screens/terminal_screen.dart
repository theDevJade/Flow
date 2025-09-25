import 'package:flutter/material.dart';
import '../widgets/custom_terminal.dart';

class TerminalScreen extends StatelessWidget {
  final String pageId;

  const TerminalScreen({super.key, required this.pageId});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.background,
      child: CustomTerminal(pageId: pageId),
    );
  }
}
