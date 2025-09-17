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
  List<String> _availableCommands = [];

  List<String> _currentSuggestions = [];
  bool _showSuggestions = false;
  int _selectedSuggestionIndex = -1;
  OverlayEntry? _suggestionOverlay;

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
    _loadAvailableCommands();

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
      }
    });
  }

  void _handleTerminalResponse(WebSocketMessage message) {
    if (!mounted) return;

    if (message.data['success'] == true) {
      final output = message.data['output'] as List<dynamic>;
      final outputLines = output.map((e) => e.toString()).toList();

      setState(() {
        _terminalState.history.addAll(outputLines);
        _terminalState.currentDirectory =
            message.data['cwd'] ?? _terminalState.currentDirectory;
      });

      _scrollToBottom();
      _saveTerminalState();
    }
  }

  @override
  void dispose() {
    _hideSuggestionOverlay();
    _webSocketListener?.cancel();
    _inputController.dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _initializeTerminal() {

    if (_terminalState.history.isEmpty) {
      final welcomeMessages = [
        'Flow Terminal v1.0.0',
        'Type "help" for available commands.',
        'Use Tab for autocompletion.',
        '',
      ];
      _terminalState.history = welcomeMessages;
    }
  }

  void _loadAvailableCommands() {
    // Mock available commands @TODO
    _availableCommands = [
      'help',
      'ls',
      'cd',
      'pwd',
      'mkdir',
      'rmdir',
      'rm',
      'cp',
      'mv',
      'cat',
      'echo',
      'grep',
      'find',
      'ps',
      'top',
      'kill',
      'clear',
      'npm',
      'node',
      'flutter',
      'dart',
      'git',
      'python',
      'pip',
      'docker',
      'kubectl',
      'ssh',
      'scp',
      'curl',
      'wget',
      'vim',
      'nano',
    ];
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
                  itemCount:
                      _terminalState.history.length +
                      1,
                  itemBuilder: (context, index) {
                    if (index < _terminalState.history.length) {
                      return _buildHistoryLine(_terminalState.history[index]);
                    } else {
                      return _buildInputLine();
                    }
                  },
                ),
                // Inline autocomplete suggestions will be shown as overlay
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
            // Path part
            TextSpan(
              text: pathPart,
              style: const TextStyle(color: Colors.cyan),
            ),
            // Prompt symbol
            const TextSpan(
              text: '\$ ',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
            // Command with syntax highlighting
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
        // First word is the command
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

      // Add space between words (except for the last word)
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
    // Check for different types of output
    if (line.trim().isEmpty) {
      return const SizedBox(height: 14); // Empty line spacing
    }

    // JSON-like output
    if (line.trim().startsWith('{') || line.trim().startsWith('[')) {
      return _buildJsonLine(line);
    }

    // File listings (ls output)
    if (_looksLikeFileList(line)) {
      return _buildFileListLine(line);
    }

    // Default output
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
        // Files with extensions
        if (word.endsWith('.dart')) {
          color = Colors.blue;
        } else if (word.endsWith('.json') || word.endsWith('.yaml')) {
          color = Colors.orange;
        } else if (word.endsWith('.md')) {
          color = Colors.cyan;
        }
      } else if (!word.contains('.') && word.isNotEmpty) {
        // Directories (no extension)
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
              child: TextField(
                controller: _inputController,
                style: TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 13,
                  color: Colors.white,
                  height: 1.2,
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
            ),
          ),
        ],
      ),
    );
  }

  void _hideSuggestionOverlay() {
    _suggestionOverlay?.remove();
    _suggestionOverlay = null;
    if (mounted) {
      setState(() {
        _showSuggestions = false;
      });
    }
  }

  void _showSuggestionOverlay() {
    if (_currentSuggestions.isEmpty) return;

    _hideSuggestionOverlay();

    final overlay = Overlay.of(context);
    final renderBox = context.findRenderObject() as RenderBox?;
    if (renderBox == null) return;

    final position = renderBox.localToGlobal(Offset.zero);

    _suggestionOverlay = OverlayEntry(
      builder: (context) => Positioned(
        left: position.dx + 20, // Position near cursor
        top: position.dy + renderBox.size.height - 150, // Above input
        child: Material(
          elevation: 4,
          borderRadius: BorderRadius.circular(8),
          child: Container(
            constraints: const BoxConstraints(maxWidth: 300, maxHeight: 200),
            decoration: BoxDecoration(
              color: const Color(0xFF1E1E1E),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey.withOpacity(0.3)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: const BoxDecoration(
                    color: Color(0xFF2D2D30),
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(8),
                      topRight: Radius.circular(8),
                    ),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.lightbulb_outline,
                        size: 14,
                        color: Colors.amber,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        'Suggestions',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white70,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                Flexible(
                  child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: _currentSuggestions.length,
                    itemBuilder: (context, index) {
                      final suggestion = _currentSuggestions[index];
                      final isSelected = index == _selectedSuggestionIndex;

                      return InkWell(
                        onTap: () => _applySuggestion(suggestion),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 8,
                          ),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? Colors.blue.withOpacity(0.2)
                                : null,
                          ),
                          child: Row(
                            children: [
                              Icon(
                                Icons.terminal,
                                size: 14,
                                color: isSelected
                                    ? Colors.blue
                                    : Colors.white38,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  suggestion,
                                  style: TextStyle(
                                    fontFamily: 'monospace',
                                    fontSize: 13,
                                    color: isSelected
                                        ? Colors.white
                                        : Colors.white70,
                                  ),
                                ),
                              ),
                              if (isSelected)
                                const Icon(
                                  Icons.keyboard_tab,
                                  size: 14,
                                  color: Colors.blue,
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );

    overlay.insert(_suggestionOverlay!);
    setState(() {
      _showSuggestions = true;
    });
  }

  void _handleKeyEvent(KeyEvent event) {
    if (event is KeyDownEvent) {
      switch (event.logicalKey) {
        case LogicalKeyboardKey.tab:
          _handleTabCompletion();
          break;
        case LogicalKeyboardKey.arrowUp:
          if (_showSuggestions) {
            _navigateSuggestions(-1);
          } else {
            _navigateHistory(-1);
          }
          break;
        case LogicalKeyboardKey.arrowDown:
          if (_showSuggestions) {
            _navigateSuggestions(1);
          } else {
            _navigateHistory(1);
          }
          break;
        case LogicalKeyboardKey.escape:
          _hideSuggestions();
          break;
        case LogicalKeyboardKey.enter:
          if (_showSuggestions && _selectedSuggestionIndex >= 0) {
            _applySuggestion(_currentSuggestions[_selectedSuggestionIndex]);
          }
          break;
      }
    }
  }

  void _onInputChanged(String text) {
    _updateSuggestions(text);
  }

  void _updateSuggestions(String input) {
    if (input.isEmpty) {
      _hideSuggestions();
      return;
    }

    final words = input.split(' ');
    final currentWord = words.isNotEmpty ? words.last : '';

    if (currentWord.isEmpty) {
      _hideSuggestions();
      return;
    }

    final suggestions = _availableCommands
        .where((cmd) => cmd.toLowerCase().startsWith(currentWord.toLowerCase()))
        .toList();

    setState(() {
      _currentSuggestions = suggestions;
      _selectedSuggestionIndex = suggestions.isNotEmpty ? 0 : -1;
    });

    if (suggestions.isNotEmpty) {
      // Delay showing overlay to ensure widget is built
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _showSuggestionOverlay();
      });
    } else {
      _hideSuggestions();
    }
  }

  void _handleTabCompletion() {
    if (_showSuggestions && _currentSuggestions.isNotEmpty) {
      _applySuggestion(
        _currentSuggestions[_selectedSuggestionIndex >= 0
            ? _selectedSuggestionIndex
            : 0],
      );
    }
  }

  void _applySuggestion(String suggestion) {
    final currentText = _inputController.text;
    final words = currentText.split(' ');

    if (words.isNotEmpty) {
      words[words.length - 1] = suggestion;
      final newText = '${words.join(' ')} ';
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

    // Update the overlay to reflect the new selection
    _showSuggestionOverlay();
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
    _hideSuggestionOverlay();
    setState(() {
      _currentSuggestions.clear();
      _selectedSuggestionIndex = -1;
    });
  }

  void _executeCommand(String command) {
    if (command.trim().isEmpty) return;

    // Add command to input history for display
    final commandLine = '${_terminalState.currentDirectory}\$ $command';
    final updatedHistory = List<String>.from(_terminalState.history);
    updatedHistory.add(commandLine);

    // Add to command history for navigation
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

    // Update state immediately with command input
    _terminalState.history = updatedHistory;

    setState(() {
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
