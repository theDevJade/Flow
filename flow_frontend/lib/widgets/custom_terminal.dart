import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/websocket_service.dart';
import '../state/page_state_manager.dart';
import '../state/app_state.dart';

class CustomTerminal extends StatefulWidget {
  final String pageId;

  const CustomTerminal({super.key, required this.pageId});

  @override
  State<CustomTerminal> createState() => _CustomTerminalState();
}

class _CustomTerminalState extends State<CustomTerminal> {
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _focusNode = FocusNode();

  late TerminalPageState _terminalState;

  List<String> _currentSuggestions = [];
  int _selectedSuggestionIndex = -1;
  Timer? _autocompleteTimer;

  @override
  void initState() {
    super.initState();
    _terminalState =
        PageStateManager.instance.getOrCreatePageState(
              widget.pageId,
              'terminal',
            )
            as TerminalPageState;

    _initializeTerminal();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (_webSocketListener == null && mounted) {
      _setupWebSocketListener();
    }
  }

  StreamSubscription<WebSocketMessage>? _webSocketListener;

  void _setupWebSocketListener() {
    if (!mounted) return;

    final appState = context.read<AppState>();
    _webSocketListener = appState.webSocketService.messages.listen((message) {
      if (!mounted) {
        _webSocketListener?.cancel();
        return;
      }

      if (message.type == 'terminal_response' &&
          message.data['pageId'] == widget.pageId) {
        _handleTerminalResponse(message);
      } else if (message.type == 'terminal_autocomplete_response' &&
          message.data['pageId'] == widget.pageId) {
        _handleAutocompleteResponse(message);
      }
    });
  }

  void _handleTerminalResponse(WebSocketMessage message) {
    if (!mounted) return;

    final responseType = message.data['type']?.toString() ?? '';

    switch (responseType) {
      case 'start':

        setState(() {
          _terminalState.currentDirectory =
              message.data['cwd']?.toString() ??
              _terminalState.currentDirectory;
        });
        break;

      case 'stream':

        final stream = message.data['stream']?.toString() ?? 'stdout';
        final data = message.data['data']?.toString() ?? '';

        if (data.isNotEmpty) {
          setState(() {

            final streamPrefix = stream == 'stderr' ? '⚠️ ' : '';
            _terminalState.history.add('$streamPrefix$data');
          });
          _scrollToBottom();
          _saveTerminalState();
        }
        break;

      case 'end':

        final exitCode = message.data['exitCode'] as int? ?? 0;


        if (message.data['action']?.toString() == 'clear') {
          setState(() {
            _terminalState.history.clear();
            _terminalState.currentDirectory =
                message.data['cwd']?.toString() ??
                _terminalState.currentDirectory;
          });
        } else {
          setState(() {
            _terminalState.currentDirectory =
                message.data['cwd']?.toString() ??
                _terminalState.currentDirectory;
            if (exitCode != 0) {
              _terminalState.history.add('Process exited with code $exitCode');
            }
          });
        }

        _scrollToBottom();
        _saveTerminalState();
        break;

      case 'error':

        final error = message.data['error']?.toString() ?? 'Unknown error';
        setState(() {
          _terminalState.history.add('Error: $error');
        });
        _scrollToBottom();
        _saveTerminalState();
        break;

      default:
        // Legacy format support (fallback)
        if (message.data['success'] == true) {
          final output = message.data['output'] as List<dynamic>? ?? [];
          final outputLines = output.map((e) => e.toString()).toList();

          setState(() {
            _terminalState.history.addAll(outputLines);
            _terminalState.currentDirectory =
                message.data['cwd']?.toString() ??
                _terminalState.currentDirectory;
          });

          _scrollToBottom();
          _saveTerminalState();
        }
        break;
    }
  }

  void _handleAutocompleteResponse(WebSocketMessage message) {
    if (!mounted) return;

    if (message.data['success'] == true) {
      final suggestionsData =
          message.data['suggestions'] as List<dynamic>? ?? [];
      final suggestions = suggestionsData
          .map((s) => s as Map<String, dynamic>)
          .toList();

      setState(() {
        _currentSuggestions = suggestions
            .map((s) => s['text']?.toString() ?? '')
            .toList();
        _selectedSuggestionIndex = _currentSuggestions.isNotEmpty ? 0 : -1;
      });

    }
  }

  @override
  void dispose() {
    _webSocketListener?.cancel();
    _autocompleteTimer?.cancel();
    _inputController.dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _initializeTerminal() {
    if (_terminalState.history.isEmpty) {
      final welcomeMessages = [
        'Flow Custom Terminal v1.0.0',
        'Type "help" to see available Flow commands.',
        'This is a custom command interpreter for Flow development.',
        '',
      ];
      _terminalState.history = welcomeMessages;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          Container(
            height: 40,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: const Color(0xFF2D2D30),
              border: Border(
                bottom: BorderSide(
                  color: Theme.of(context).dividerColor.withOpacity(0.3),
                  width: 0.5,
                ),
              ),
            ),
            child: Row(
              children: [
                const Icon(Icons.terminal, size: 16, color: Colors.green),
                const SizedBox(width: 8),
                Text(
                  'Terminal',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Colors.white70,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const Spacer(),
                Text(
                  _terminalState.currentDirectory,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Colors.green,
                    fontFamily: 'monospace',
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Stack(
              children: [
                ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(8),
                  itemCount: _terminalState.history.length + 1,
                  itemBuilder: (context, index) {
                    if (index < _terminalState.history.length) {
                      return _buildHistoryLine(_terminalState.history[index]);
                    } else {
                      return _buildInputLine();
                    }
                  },
                ),

              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryLine(String line) {
    final isCommand = line.contains('\$ ');
    final isError = line.startsWith('Error:') || line.startsWith('bash:');

    if (isCommand) {
      return _buildCommandLine(line);
    } else if (isError) {
      return _buildErrorLine(line);
    } else {
      return _buildOutputLine(line);
    }
  }

  Widget _buildCommandLine(String line) {
    // Parse command line format: "/path/to/dir$ command args"
    final parts = line.split('\$ ');
    if (parts.length != 2) {
      return _buildSimpleLine(line, Colors.green);
    }

    final pathPart = parts[0];
    final commandPart = parts[1];

    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: RichText(
        text: TextSpan(
          style: const TextStyle(
            fontFamily: 'monospace',
            fontSize: 13,
            height: 1.2,
          ),
          children: [

            TextSpan(
              text: pathPart,
              style: const TextStyle(color: Colors.cyan),
            ),

            const TextSpan(
              text: '\$ ',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),

            ..._parseCommand(commandPart),
          ],
        ),
      ),
    );
  }

  List<TextSpan> _parseCommand(String command) {
    final List<TextSpan> spans = [];
    final words = command.split(' ');

    for (int i = 0; i < words.length; i++) {
      final word = words[i];
      Color color;
      FontWeight? fontWeight;

      if (i == 0) {

        color = _isBuiltinCommand(word) ? Colors.blue : Colors.lightBlue;
        fontWeight = FontWeight.w500;
      } else if (word.startsWith('-')) {
        // Flags/options
        color = Colors.orange;
      } else if (word.contains('/') || word.contains('.')) {
        // Paths or files
        color = Colors.lightGreen;
      } else if (_isKeyword(word)) {
        // Keywords
        color = Colors.purple;
      } else {
        // Regular arguments
        color = Colors.white70;
      }

      spans.add(
        TextSpan(
          text: word,
          style: TextStyle(color: color, fontWeight: fontWeight),
        ),
      );


      if (i < words.length - 1) {
        spans.add(
          const TextSpan(
            text: ' ',
            style: TextStyle(color: Colors.white70),
          ),
        );
      }
    }

    return spans;
  }

  Widget _buildErrorLine(String line) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: RichText(
        text: TextSpan(
          style: const TextStyle(
            fontFamily: 'monospace',
            fontSize: 13,
            height: 1.2,
          ),
          children: [
            const TextSpan(
              text: '✗ ',
              style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold),
            ),
            TextSpan(
              text: line,
              style: const TextStyle(color: Colors.red),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOutputLine(String line) {

    if (line.trim().isEmpty) {
      return const SizedBox(height: 14);
    }


    if (line.trim().startsWith('{') || line.trim().startsWith('[')) {
      return _buildJsonLine(line);
    }


    if (_looksLikeFileList(line)) {
      return _buildFileListLine(line);
    }


    return _buildSimpleLine(line, Colors.white70);
  }

  Widget _buildJsonLine(String line) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: Text(
        line,
        style: const TextStyle(
          fontFamily: 'monospace',
          fontSize: 13,
          color: Colors.lightBlueAccent,
          height: 1.2,
        ),
      ),
    );
  }

  Widget _buildFileListLine(String line) {
    final words = line.split(RegExp(r'\s+'));
    final List<TextSpan> spans = [];

    for (int i = 0; i < words.length; i++) {
      final word = words[i];
      Color color = Colors.white70;

      if (word.contains('.')) {

        if (word.endsWith('.dart')) {
          color = Colors.blue;
        } else if (word.endsWith('.json') || word.endsWith('.yaml')) {
          color = Colors.orange;
        } else if (word.endsWith('.md')) {
          color = Colors.cyan;
        }
      } else if (!word.contains('.') && word.isNotEmpty) {

        color = Colors.green;
      }

      spans.add(
        TextSpan(
          text: word,
          style: TextStyle(color: color),
        ),
      );

      if (i < words.length - 1) {
        spans.add(
          const TextSpan(
            text: ' ',
            style: TextStyle(color: Colors.white70),
          ),
        );
      }
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: RichText(
        text: TextSpan(
          style: const TextStyle(
            fontFamily: 'monospace',
            fontSize: 13,
            height: 1.2,
          ),
          children: spans,
        ),
      ),
    );
  }

  Widget _buildSimpleLine(String line, Color color) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: SelectableText(
        line,
        style: TextStyle(
          fontFamily: 'monospace',
          fontSize: 13,
          color: color,
          height: 1.2,
        ),
      ),
    );
  }

  bool _isBuiltinCommand(String command) {
    const builtins = {
      'ls',
      'cd',
      'pwd',
      'echo',
      'cat',
      'grep',
      'find',
      'mkdir',
      'rmdir',
      'rm',
      'cp',
      'mv',
      'chmod',
      'chown',
      'ps',
      'kill',
      'top',
      'df',
      'du',
      'git',
      'npm',
      'yarn',
      'flutter',
      'dart',
      'python',
      'node',
      'pip',
    };
    return builtins.contains(command.toLowerCase());
  }

  bool _isKeyword(String word) {
    const keywords = {
      'true',
      'false',
      'null',
      'undefined',
      'yes',
      'no',
      'on',
      'off',
    };
    return keywords.contains(word.toLowerCase());
  }

  bool _looksLikeFileList(String line) {
    // Simple heuristic: if line contains multiple words and some look like files
    final words = line.split(RegExp(r'\s+'));
    return words.length > 1 &&
        words.any(
          (word) =>
              word.contains('.') ||
              RegExp(r'^[a-zA-Z_][a-zA-Z0-9_]*$').hasMatch(word),
        );
  }

  Widget _buildInputLine() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '\$ ',
            style: TextStyle(
              fontFamily: 'monospace',
              fontSize: 13,
              color: Colors.green,
              height: 1.2,
            ),
          ),
          Expanded(
            child: KeyboardListener(
              focusNode: _focusNode,
              onKeyEvent: _handleKeyEvent,
              child: Stack(
                alignment: Alignment.centerLeft,
                children: [
                  // Suggestion text (gray background)
                  if (_currentSuggestions.isNotEmpty &&
                      _selectedSuggestionIndex >= 0)
                    _buildSuggestionText(),
                  // Actual input field
                  TextField(
                    controller: _inputController,
                    style: TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 13,
                      color: Colors.white,
                      height: 1.2,
                      backgroundColor: Colors.transparent,
                    ),
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      isDense: true,
                      contentPadding: EdgeInsets.zero,
                    ),
                    onChanged: _onInputChanged,
                    onSubmitted: _executeCommand,
                    autofocus: true,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSuggestionText() {
    if (_currentSuggestions.isEmpty || _selectedSuggestionIndex < 0) {
      return const SizedBox.shrink();
    }

    final suggestion = _currentSuggestions[_selectedSuggestionIndex];
    final currentText = _inputController.text;

    // Find the completion part that should be shown in gray
    String completionText = '';
    if (suggestion.startsWith(currentText) &&
        suggestion.length > currentText.length) {
      completionText = suggestion.substring(currentText.length);
    } else {
      // For more complex completions, show the full suggestion after current text
      final words = currentText.split(' ');
      if (words.isNotEmpty) {
        // Replace the last word with the suggestion
        words[words.length - 1] = suggestion;
        final fullText = words.join(' ');
        if (fullText.length > currentText.length) {
          completionText = fullText.substring(currentText.length);
        }
      }
    }

    if (completionText.isEmpty) return const SizedBox.shrink();

    return Positioned(
      left: _getTextWidth(currentText),
      child: Text(
        completionText,
        style: TextStyle(
          fontFamily: 'monospace',
          fontSize: 13,
          color: Colors.grey.shade600,
          height: 1.2,
        ),
      ),
    );
  }

  double _getTextWidth(String text) {
    final TextPainter textPainter = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(fontFamily: 'monospace', fontSize: 13, height: 1.2),
      ),
      maxLines: 1,
      textDirection: TextDirection.ltr,
    )..layout();
    return textPainter.size.width;
  }

  void _handleKeyEvent(KeyEvent event) {
    if (event is KeyDownEvent) {
      switch (event.logicalKey) {
        case LogicalKeyboardKey.tab:
          _handleTabCompletion();
          break;
        case LogicalKeyboardKey.arrowUp:
          if (_currentSuggestions.isNotEmpty) {
            _navigateSuggestions(-1);
          } else {
            _navigateHistory(-1);
          }
          break;
        case LogicalKeyboardKey.arrowDown:
          if (_currentSuggestions.isNotEmpty) {
            _navigateSuggestions(1);
          } else {
            _navigateHistory(1);
          }
          break;
        case LogicalKeyboardKey.escape:
          _hideSuggestions();
          break;
        case LogicalKeyboardKey.enter:
          if (_currentSuggestions.isNotEmpty && _selectedSuggestionIndex >= 0) {
            _applySuggestion(_currentSuggestions[_selectedSuggestionIndex]);
          }
          break;
      }
    }
  }

  void _onInputChanged(String text) {
    // Cancel any existing autocomplete timer
    _autocompleteTimer?.cancel();

    // Debounce autocomplete requests
    _autocompleteTimer = Timer(const Duration(milliseconds: 300), () {
      _requestBackendAutocomplete(text);
    });
  }

  void _requestBackendAutocomplete(String input) {
    if (input.isEmpty || !mounted) {
      _hideSuggestions();
      return;
    }

    // Get cursor position
    final cursorPosition = _inputController.selection.baseOffset;

    final appState = context.read<AppState>();
    appState.webSocketService.send(
      WebSocketMessage(
        type: 'terminal_autocomplete',
        data: {
          'input': input,
          'cursorPosition': cursorPosition,
          'cwd': _terminalState.currentDirectory,
          'pageId': widget.pageId,
        },
      ),
    );
  }

  void _handleTabCompletion() {
    if (_currentSuggestions.isNotEmpty) {
      _applySuggestion(
        _currentSuggestions[_selectedSuggestionIndex >= 0
            ? _selectedSuggestionIndex
            : 0],
      );
    }
  }

  void _applySuggestion(String suggestion) {
    final currentText = _inputController.text;

    // Simple replacement logic for the current word
    final words = currentText.split(' ');
    if (words.isNotEmpty) {
      words[words.length - 1] = suggestion;
      final newText = words.join(' ');
      _inputController.value = TextEditingValue(
        text: newText,
        selection: TextSelection.collapsed(offset: newText.length),
      );
    }

    _hideSuggestions();
  }

  void _navigateSuggestions(int direction) {
    if (_currentSuggestions.isEmpty) return;

    setState(() {
      _selectedSuggestionIndex = (_selectedSuggestionIndex + direction).clamp(
        0,
        _currentSuggestions.length - 1,
      );
    });

    // No overlay needed - suggestions are shown inline automatically via setState
  }

  void _navigateHistory(int direction) {
    final commandHistory = _terminalState.commandHistory;
    if (commandHistory.isEmpty) return;

    final newIndex = (_terminalState.historyIndex + direction).clamp(
      -1,
      commandHistory.length - 1,
    );

    setState(() {
      _terminalState.historyIndex = newIndex;
      if (newIndex >= 0) {
        _inputController.text = commandHistory[newIndex];
        _inputController.selection = TextSelection.collapsed(
          offset: _inputController.text.length,
        );
      } else {
        _inputController.clear();
      }
    });
  }

  void _hideSuggestions() {
    setState(() {
      _currentSuggestions.clear();
      _selectedSuggestionIndex = -1;
    });
  }

  void _executeCommand(String command) {
    if (command.trim().isEmpty) return;

    // Add command to command history for navigation (not display history)
    final updatedCommandHistory = List<String>.from(
      _terminalState.commandHistory,
    );
    updatedCommandHistory.add(command);
    if (updatedCommandHistory.length > 100) {
      updatedCommandHistory.removeAt(0); // Keep only last 100 commands
    }

    // Reset history index
    _terminalState.historyIndex = -1;
    _terminalState.commandHistory = updatedCommandHistory;

    // Add command line to display history - but don't duplicate, backend will send the prompt
    final commandLine = '${_terminalState.currentDirectory}\$ $command';
    setState(() {
      _terminalState.history.add(commandLine);
      _inputController.clear();
      _hideSuggestions();
    });

    // Send command to WebSocket - response will come via stream
    _sendCommandToWebSocket(command);

    _scrollToBottom();
    _saveTerminalState();
  }

  void _scrollToBottom() {
    // Scroll to bottom
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _sendCommandToWebSocket(String command) {
    final appState = context.read<AppState>();
    appState.webSocketService.send(
      WebSocketMessage(
        type: 'terminal_command',
        data: {
          'command': command,
          'cwd': _terminalState.currentDirectory,
          'pageId': widget.pageId,
        },
      ),
    );
  }

  void _saveTerminalState() {
    // Save terminal state to persistence
    PageStateManager.instance.saveTerminalState(widget.pageId);
  }
}
