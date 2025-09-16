import 'dart:convert';
import 'package:flutter/material.dart';
import 'graph_editor/graph_editor.dart';
import 'services/websocket_service.dart';

class WebSocketTest extends StatefulWidget {
  const WebSocketTest({super.key});

  @override
  State<WebSocketTest> createState() => _WebSocketTestState();
}

class _WebSocketTestState extends State<WebSocketTest> {
  String _lastJsonExport = '';
  final WebSocketService _webSocketService = WebSocketService.instance;
  String _connectionStatus = 'Disconnected';
  List<String> _messages = [];

  @override
  void initState() {
    super.initState();
    _initializeWebSocket();
  }

  void _initializeWebSocket() {
    // Set to production mode with local server
    // _webSocketService.setDefaultProductionServer();

    // Listen to connection status
    _webSocketService.status.listen((status) {
      setState(() {
        _connectionStatus = status.toString().split('.').last;
      });
    });

    // Listen to messages
    _webSocketService.messages.listen((message) {
      setState(() {
        _messages.insert(0, '${message.type}: ${jsonEncode(message.data)}');
        if (_messages.length > 10) {
          _messages = _messages.take(10).toList();
        }
      });
    });

    // Connect to server
    _connectToServer();
  }

  void _connectToServer() async {
    try {
      await _webSocketService.connect(null);
    } catch (e) {
      setState(() {
        _messages.insert(0, 'Connection error: $e');
      });
    }
  }

  void _exportToWebSocket() {
    final executableFlowchart = GraphEditor.getExecutableFlowchart(context);
    if (executableFlowchart != null) {
      final jsonString = jsonEncode(executableFlowchart);

      setState(() {
        _lastJsonExport = jsonString;
      });

      // Send to real WebSocket server
      _webSocketService.send(
        WebSocketMessage(
          type: 'graph_save',
          data: {'graphId': 'test_graph', 'graphData': executableFlowchart},
        ),
      );

      print('WebSocket JSON Export:');
      print(jsonString);
    }
  }

  void _toggleConnection() {
    if (_webSocketService.currentStatus ==
        WebSocketConnectionStatus.connected) {
      _webSocketService.disconnect();
    } else {
      _connectToServer();
    }
  }

  void _toggleMode() {
    // if (_webSocketService.isInMockMode) {
    //   _webSocketService.setDefaultProductionServer();
    //   setState(() {
    //     _messages.insert(0, 'Switched to Production mode');
    //   });
    // } else {
    //   _webSocketService.enableMockMode();
    //   setState(() {
    //     _messages.insert(0, 'Switched to Mock mode');
    //   });
    // }
    _connectToServer();
  }

  @override
  void dispose() {
    _webSocketService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('WebSocket Test - $_connectionStatus'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            onPressed: _toggleMode,
            icon: Icon(Icons.cloud),
            tooltip: 'Switch to Mock',
          ),
          IconButton(
            onPressed: _toggleConnection,
            icon: Icon(
              _webSocketService.currentStatus ==
                      WebSocketConnectionStatus.connected
                  ? Icons.stop
                  : Icons.play_arrow,
            ),
            tooltip:
                _webSocketService.currentStatus ==
                    WebSocketConnectionStatus.connected
                ? 'Disconnect'
                : 'Connect',
          ),
          IconButton(
            onPressed: _exportToWebSocket,
            icon: const Icon(Icons.send),
            tooltip: 'Export to WebSocket',
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(flex: 2, child: const GraphEditor()),

          // Connection status and recent messages
          Container(
            height: 100,
            color: const Color(0xFF2E2E2E),
            padding: const EdgeInsets.all(8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Status: $_connectionStatus | Mode: Production',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Recent Messages:',
                  style: TextStyle(color: Colors.white70, fontSize: 12),
                ),
                Expanded(
                  child: ListView.builder(
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      return Text(
                        _messages[index],
                        style: const TextStyle(
                          color: Color(0xFF66BB6A),
                          fontSize: 10,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      );
                    },
                  ),
                ),
              ],
            ),
          ),

          if (_lastJsonExport.isNotEmpty)
            Expanded(
              flex: 1,
              child: Container(
                color: const Color(0xFF1E1E1E),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Latest WebSocket Export:',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Expanded(
                      child: SingleChildScrollView(
                        child: Text(
                          _lastJsonExport,
                          style: const TextStyle(
                            color: Color(0xFF66BB6A),
                            fontFamily: 'monospace',
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}
