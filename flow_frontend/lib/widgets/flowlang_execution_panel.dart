import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/flowlang_execution_service.dart';

class FlowLangExecutionPanel extends StatefulWidget {
  final String code;
  final String? fileName;
  final Future<String> Function()? getCurrentCode; // Function to get current code from Monaco editor

  const FlowLangExecutionPanel({
    super.key,
    required this.code,
    this.fileName,
    this.getCurrentCode,
  });

  @override
  State<FlowLangExecutionPanel> createState() => _FlowLangExecutionPanelState();
}

class _FlowLangExecutionPanelState extends State<FlowLangExecutionPanel> {
  final ScrollController _scrollController = ScrollController();
  late FlowLangExecutionService _executionService;

  @override
  void initState() {
    super.initState();
    _executionService = FlowLangExecutionService.instance;
    
    // Auto-scroll to newest execution when new results are added
    _executionService.executionStream.listen((_) {
      if (mounted && _scrollController.hasClients) {
        _scrollController.animateTo(
          0.0, // Scroll to top since we're showing newest first
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _executeCode() async {
    try {
      // Get current code from Monaco editor if available, otherwise use widget.code
      String codeToExecute;
      if (widget.getCurrentCode != null) {
        codeToExecute = await widget.getCurrentCode!();
        debugPrint('🚀 FlowLang Execution: Got current code from Monaco editor, length: ${codeToExecute.length}');
      } else {
        codeToExecute = widget.code;
        debugPrint('🚀 FlowLang Execution: Using widget.code, length: ${widget.code.length}');
      }

      if (codeToExecute.trim().isEmpty) {
        _showSnackBar('No code to execute', isError: true);
        return;
      }

      await _executionService.executeFlowLang(
        codeToExecute,
        fileName: widget.fileName,
      );
    } catch (e) {
      _showSnackBar('Execution failed: $e', isError: true);
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: isError ? Colors.red : Colors.green,
          duration: const Duration(seconds: 3),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Column(
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceVariant,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.play_arrow,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  'FlowLang Execution',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                Consumer<FlowLangExecutionService>(
                  builder: (context, service, child) {
                    return Row(
                      children: [
                        if (service.isExecuting) ...[
                          const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          ),
                          const SizedBox(width: 8),
                        ],
                        ElevatedButton.icon(
                          onPressed: service.isExecuting ? null : _executeCode,
                          icon: const Icon(Icons.play_arrow, size: 16),
                          label: const Text('Run'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Theme.of(context).colorScheme.primary,
                            foregroundColor: Theme.of(context).colorScheme.onPrimary,
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          ),
                        ),
                        const SizedBox(width: 8),
                        IconButton(
                          onPressed: service.clearHistory,
                          icon: const Icon(Icons.clear_all),
                          tooltip: 'Clear History',
                        ),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
          // Output area
          Expanded(
            child: Consumer<FlowLangExecutionService>(
              builder: (context, service, child) {
                if (service.executionHistory.isEmpty) {
                  return Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.code_off,
                          size: 24,
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.3),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'No executions yet',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Click "Run" to execute your FlowLang code',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  );
                }

                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(16),
                  itemCount: service.executionHistory.length,
                  itemBuilder: (context, index) {
                    // Reverse the index to show newest first
                    final reversedIndex = service.executionHistory.length - 1 - index;
                    final result = service.executionHistory[reversedIndex];
                    return _buildExecutionResult(result, reversedIndex);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExecutionResult(FlowLangExecutionResult result, int index) {
    final isSuccess = result.success;
    final timestamp = result.timestamp;
    final timeStr = '${timestamp.hour.toString().padLeft(2, '0')}:'
        '${timestamp.minute.toString().padLeft(2, '0')}:'
        '${timestamp.second.toString().padLeft(2, '0')}';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: isSuccess 
                  ? Colors.green.withOpacity(0.1)
                  : Colors.red.withOpacity(0.1),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
            ),
            child: Row(
              children: [
                Icon(
                  isSuccess ? Icons.check_circle : Icons.error,
                  color: isSuccess ? Colors.green : Colors.red,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Text(
                  'Execution #${index + 1}',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: isSuccess ? Colors.green : Colors.red,
                  ),
                ),
                const Spacer(),
                Text(
                  timeStr,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  '${result.executionTime.inMilliseconds}ms',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                  ),
                ),
              ],
            ),
          ),
          // Output content
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (result.output.isNotEmpty) ...[
                  Text(
                    'Output:',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(
                        color: Theme.of(context).colorScheme.outline.withOpacity(0.3),
                      ),
                    ),
                    child: SelectableText(
                      result.output,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        fontFamily: 'monospace',
                      ),
                    ),
                  ),
                ],
                if (result.errors.isNotEmpty) ...[
                  if (result.output.isNotEmpty) const SizedBox(height: 12),
                  Text(
                    'Errors:',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: Colors.red,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.red.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(
                        color: Colors.red.withOpacity(0.3),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: result.errors.map((error) => Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: SelectableText(
                          '• $error',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontFamily: 'monospace',
                            color: Colors.red[700],
                          ),
                        ),
                      )).toList(),
                    ),
                  ),
                ],
                if (result.logs.isNotEmpty) ...[
                  if (result.output.isNotEmpty || result.errors.isNotEmpty) const SizedBox(height: 12),
                  Text(
                    'Execution Logs:',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.blue.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(
                        color: Colors.blue.withOpacity(0.3),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: result.logs.map((log) => Padding(
                        padding: const EdgeInsets.only(bottom: 2),
                        child: SelectableText(
                          log,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontFamily: 'monospace',
                            color: Colors.blue[700],
                            fontSize: 11,
                          ),
                        ),
                      )).toList(),
                    ),
                  ),
                ],
                if (result.output.isEmpty && result.errors.isEmpty && result.logs.isEmpty) ...[
                  Text(
                    'No output',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
