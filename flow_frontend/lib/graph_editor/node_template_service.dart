import 'dart:convert';
import 'dart:async';
import 'package:flutter/services.dart';
import 'node_template.dart';
import '../services/websocket_service.dart';

class NodeTemplateService {
  static NodeTemplateService? _instance;
  static NodeTemplateService get instance =>
      _instance ??= NodeTemplateService._();

  NodeTemplateService._();

  NodeTemplateLibrary? _library;
  bool _isLoaded = false;
  WebSocketService? _webSocketService;
  StreamSubscription<WebSocketMessage>? _messageSubscription;

  NodeTemplateLibrary? get library => _library;
  bool get isLoaded => _isLoaded;

  void initialize(WebSocketService webSocketService) {
    _webSocketService = webSocketService;
    _messageSubscription?.cancel();
    _messageSubscription = _webSocketService!.messages.listen(
      _handleWebSocketMessage,
    );
  }

  Future<void> loadTemplates() async {
    if (_isLoaded) {
      print('Node templates already loaded, skipping');
      return;
    }

    print('Loading node templates...');
    print('WebSocket service: $_webSocketService');
    print('WebSocket status: ${_webSocketService?.currentStatus}');

    try {
      if (_webSocketService != null &&
          _webSocketService!.currentStatus ==
              WebSocketConnectionStatus.connected) {
        print('WebSocket connected, requesting templates from server');
        // Request templates from WebSocket
        _webSocketService!.send(
          WebSocketMessage(
            type: 'node_templates',
            id: DateTime.now().millisecondsSinceEpoch.toString(),
            data: {},
          ),
        );

        print('Waiting for templates response...');
        // Wait for response (with timeout)
        await _waitForTemplates();
      } else {
        print('WebSocket not connected, loading from assets');
        // Fallback to loading from assets if WebSocket not available
        await _loadFromAssets();
      }
    } catch (e) {
      print('Error loading node templates: $e');
      print('Stack trace: ${StackTrace.current}');
      // Create fallback library with basic templates
      _createFallbackLibrary();
    }
  }

  Future<void> _waitForTemplates() async {
    final completer = Completer<void>();
    Timer? timeout;

    timeout = Timer(const Duration(seconds: 5), () {
      if (!completer.isCompleted) {
        completer.completeError('Timeout waiting for node templates');
      }
    });

    // Wait for templates to be loaded or timeout
    while (!_isLoaded && !completer.isCompleted) {
      await Future.delayed(const Duration(milliseconds: 100));
    }

    timeout.cancel();
    if (!completer.isCompleted) {
      completer.complete();
    }

    return completer.future;
  }

  void _handleWebSocketMessage(WebSocketMessage message) {
    if (message.type == 'node_templates_response') {
      try {
        print('Received node_templates_response: ${message.data}');
        
        if (message.data['success'] == true) {
          final templatesData = message.data['templates'];
          print('Templates data type: ${templatesData.runtimeType}');
          print('Templates data: $templatesData');
          
          // Handle both Map<String, dynamic> and raw JSON element
          Map<String, dynamic> templatesMap;
          if (templatesData is Map<String, dynamic>) {
            templatesMap = templatesData;
            print('Using templates data as Map directly');
          } else {
            // If it's a raw JSON element, convert it to Map
            print('Converting JSON element to Map');
            final jsonString = templatesData.toString();
            print('JSON string: $jsonString');
            final decodedJson = json.decode(jsonString);
            if (decodedJson is Map<String, dynamic>) {
              templatesMap = decodedJson;
              print('Successfully converted to Map');
            } else {
              throw Exception('Invalid templates data format: ${decodedJson.runtimeType}');
            }
          }
          
          print('Creating NodeTemplateLibrary from: $templatesMap');
          _library = NodeTemplateLibrary.fromJson(templatesMap);
          _isLoaded = true;
          print(
            'Successfully loaded ${_library?.nodeTemplates.length ?? 0} node templates from WebSocket',
          );
        } else {
          print('Node templates request failed: ${message.data['error']}');
          _createFallbackLibrary();
        }
      } catch (e) {
        print('Error parsing node templates from WebSocket: $e');
        print('Stack trace: ${StackTrace.current}');
        _createFallbackLibrary();
      }
    }
  }

  Future<void> _loadFromAssets() async {
    if (_isLoaded) return;

    try {
      final String jsonString = await rootBundle.loadString(
        'assets/node_templates.json',
      );
      final dynamic decodedJson = json.decode(jsonString);

      if (decodedJson is Map<String, dynamic>) {
        _library = NodeTemplateLibrary.fromJson(decodedJson);
        _isLoaded = true;
        print('Loaded ${_library?.nodeTemplates.length ?? 0} node templates');
      } else {
        throw Exception('Invalid JSON format: expected Map<String, dynamic>');
      }
    } catch (e) {
      print('Error loading node templates: $e');
      // Create a minimal fallback library with flowchart-specific templates
      _library = NodeTemplateLibrary(
        version: '1.0',
        nodeTemplates: [
          // TRIGGER BLOCKS
          NodeTemplate(
            id: 'trigger_start',
            name: 'Start',
            description: 'Flow start trigger',
            category: 'TRIGGER',
            color: const NodeColor(r: 0.2, g: 0.8, b: 0.2, a: 1.0),
            size: const NodeSize(width: 120, height: 60),
            inputs: const [],
            outputs: const [
              NodePort(
                id: 'output',
                name: 'Out',
                type: 'flow',
                color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
              ),
            ],
            properties: const [
              NodeProperty(
                name: 'triggerName',
                type: 'string',
                label: 'Trigger Name',
                defaultValue: 'StartTrigger',
                description: 'Name of the trigger',
              ),
            ],
          ),
          NodeTemplate(
            id: 'trigger_event',
            name: 'Event Trigger',
            description: 'Event-based trigger',
            category: 'TRIGGER',
            color: const NodeColor(r: 0.2, g: 0.8, b: 0.4, a: 1.0),
            size: const NodeSize(width: 140, height: 70),
            inputs: const [],
            outputs: const [
              NodePort(
                id: 'output',
                name: 'Out',
                type: 'flow',
                color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
              ),
            ],
            properties: const [
              NodeProperty(
                name: 'eventType',
                type: 'string',
                label: 'Event Type',
                defaultValue: 'onClick',
                description: 'Type of event to trigger on',
              ),
            ],
          ),

          // LOGIC BLOCKS
          NodeTemplate(
            id: 'logic_and',
            name: 'AND',
            description: 'Logical AND gate',
            category: 'LOGIC',
            color: const NodeColor(r: 0.2, g: 0.4, b: 0.8, a: 1.0),
            size: const NodeSize(width: 100, height: 80),
            inputs: const [
              NodePort(
                id: 'inputA',
                name: 'A',
                type: 'boolean',
                color: NodeColor(r: 0.8, g: 0.4, b: 0.4, a: 1.0),
              ),
              NodePort(
                id: 'inputB',
                name: 'B',
                type: 'boolean',
                color: NodeColor(r: 0.8, g: 0.4, b: 0.4, a: 1.0),
              ),
            ],
            outputs: const [
              NodePort(
                id: 'output',
                name: 'Out',
                type: 'boolean',
                color: NodeColor(r: 0.4, g: 0.8, b: 0.4, a: 1.0),
              ),
            ],
            properties: const [],
          ),
          NodeTemplate(
            id: 'logic_or',
            name: 'OR',
            description: 'Logical OR gate',
            category: 'LOGIC',
            color: const NodeColor(r: 0.3, g: 0.4, b: 0.8, a: 1.0),
            size: const NodeSize(width: 100, height: 80),
            inputs: const [
              NodePort(
                id: 'inputA',
                name: 'A',
                type: 'boolean',
                color: NodeColor(r: 0.8, g: 0.4, b: 0.4, a: 1.0),
              ),
              NodePort(
                id: 'inputB',
                name: 'B',
                type: 'boolean',
                color: NodeColor(r: 0.8, g: 0.4, b: 0.4, a: 1.0),
              ),
            ],
            outputs: const [
              NodePort(
                id: 'output',
                name: 'Out',
                type: 'boolean',
                color: NodeColor(r: 0.4, g: 0.8, b: 0.4, a: 1.0),
              ),
            ],
            properties: const [],
          ),

          // ACTION BLOCKS
          NodeTemplate(
            id: 'action_execute',
            name: 'Execute',
            description: 'Execute an action',
            category: 'ACTION',
            color: const NodeColor(r: 0.8, g: 0.4, b: 0.2, a: 1.0),
            size: const NodeSize(width: 140, height: 80),
            inputs: const [
              NodePort(
                id: 'trigger',
                name: 'Trigger',
                type: 'flow',
                color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
              ),
            ],
            outputs: const [
              NodePort(
                id: 'output',
                name: 'Done',
                type: 'flow',
                color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
              ),
            ],
            properties: const [
              NodeProperty(
                name: 'actionType',
                type: 'string',
                label: 'Action Type',
                defaultValue: 'print',
                description: 'Type of action to execute',
              ),
              NodeProperty(
                name: 'actionValue',
                type: 'string',
                label: 'Action Value',
                defaultValue: 'Hello World',
                description: 'Action parameter/value',
              ),
            ],
          ),

          // END BLOCKS
          NodeTemplate(
            id: 'end_complete',
            name: 'End',
            description: 'Flow completion',
            category: 'END',
            color: const NodeColor(r: 0.6, g: 0.2, b: 0.2, a: 1.0),
            size: const NodeSize(width: 100, height: 50),
            inputs: const [
              NodePort(
                id: 'input',
                name: 'In',
                type: 'flow',
                color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
              ),
            ],
            outputs: const [],
            properties: const [
              NodeProperty(
                name: 'exitCode',
                type: 'int',
                label: 'Exit Code',
                defaultValue: 0,
                description: 'Exit code',
              ),
            ],
          ),
        ],
      );
      _isLoaded = true;
    }
  }

  void _createFallbackLibrary() {
    print('Creating fallback node template library');
    _library = NodeTemplateLibrary(
      version: '1.0',
      nodeTemplates: [
        // TRIGGER BLOCKS
        NodeTemplate(
          id: 'trigger_start',
          name: 'Start',
          description: 'Flow start trigger',
          category: 'TRIGGER',
          color: const NodeColor(r: 0.2, g: 0.8, b: 0.2, a: 1.0),
          size: const NodeSize(width: 120, height: 60),
          inputs: const [],
          outputs: const [
            NodePort(
              id: 'output',
              name: 'Out',
              type: 'flow',
              color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
            ),
          ],
          properties: const [
            NodeProperty(
              name: 'triggerName',
              type: 'string',
              label: 'Trigger Name',
              defaultValue: 'StartTrigger',
              description: 'Name of the trigger',
            ),
          ],
        ),
        NodeTemplate(
          id: 'action_print',
          name: 'Print',
          description: 'Print a message',
          category: 'ACTION',
          color: const NodeColor(r: 0.8, g: 0.6, b: 0.2, a: 1.0),
          size: const NodeSize(width: 120, height: 80),
          inputs: const [
            NodePort(
              id: 'input',
              name: 'In',
              type: 'flow',
              color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
            ),
          ],
          outputs: const [
            NodePort(
              id: 'output',
              name: 'Out',
              type: 'flow',
              color: NodeColor(r: 0.8, g: 0.8, b: 0.8, a: 1.0),
            ),
          ],
          properties: const [
            NodeProperty(
              name: 'message',
              type: 'string',
              label: 'Message',
              defaultValue: 'Hello, World!',
              description: 'Message to print',
            ),
          ],
        ),
      ],
    );
    _isLoaded = true;
  }

  void dispose() {
    _messageSubscription?.cancel();
  }

  List<NodeTemplate> get allTemplates => _library?.nodeTemplates ?? [];

  List<String> get categories => _library?.categories ?? [];

  List<NodeTemplate> getTemplatesByCategory(String category) {
    return _library?.getTemplatesByCategory(category) ?? [];
  }

  NodeTemplate? getTemplateById(String id) {
    return _library?.getTemplateById(id);
  }

  Future<void> reloadTemplates() async {
    _isLoaded = false;
    _library = null;
    await loadTemplates();
  }
}
