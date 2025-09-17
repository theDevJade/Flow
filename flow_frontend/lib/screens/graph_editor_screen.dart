import 'package:flutter/material.dart';
import '../graph_editor/graph_editor.dart';
import '../graph_editor/models.dart';
import '../services/graph_project_service.dart';
import '../widgets/graph_project_manager.dart';

class GraphEditorScreen extends StatefulWidget {
  const GraphEditorScreen({super.key});

  @override
  State<GraphEditorScreen> createState() => _GraphEditorScreenState();
}

class _GraphEditorScreenState extends State<GraphEditorScreen> {
  GraphProject? currentProject;
  final GraphProjectService _projectService = GraphProjectService.instance;
  List<GraphNode> sampleNodes = [
    GraphNode(
      id: 'input_node',
      name: 'Input Node',
      inputs: [],
      outputs: [
        GraphPort(
          id: 'output1',
          name: 'Value',
          isInput: false,
          color: Colors.green,
        ),
        GraphPort(
          id: 'output2',
          name: 'Signal',
          isInput: false,
          color: Colors.blue,
        ),
      ],
      color: Colors.deepPurple,
      position: const Offset(50, 100),
      size: const Size(150, 80),
    ),
    GraphNode(
      id: 'process_node',
      name: 'Process Node',
      inputs: [
        GraphPort(
          id: 'input1',
          name: 'Data',
          isInput: true,
          color: Colors.green,
        ),
        GraphPort(
          id: 'input2',
          name: 'Control',
          isInput: true,
          color: Colors.blue,
        ),
      ],
      outputs: [
        GraphPort(
          id: 'output1',
          name: 'Result',
          isInput: false,
          color: Colors.red,
        ),
      ],
      color: Colors.indigo,
      position: const Offset(300, 80),
      size: const Size(150, 100),
    ),
    GraphNode(
      id: 'output_node',
      name: 'Output Node',
      inputs: [
        GraphPort(
          id: 'input1',
          name: 'Final',
          isInput: true,
          color: Colors.red,
        ),
      ],
      outputs: [],
      color: Colors.teal,
      position: const Offset(550, 100),
      size: const Size(150, 60),
    ),
  ];

  List<GraphConnection> sampleConnections = [];

  @override
  void initState() {
    super.initState();

    sampleConnections = [
      GraphConnection(
        id: 'conn1',
        fromNodeId: 'input_node',
        fromPortId: 'output1',
        toNodeId: 'process_node',
        toPortId: 'input1',
        color: Colors.green,
      ),
      GraphConnection(
        id: 'conn2',
        fromNodeId: 'input_node',
        fromPortId: 'output2',
        toNodeId: 'process_node',
        toPortId: 'input2',
        color: Colors.blue,
      ),
      GraphConnection(
        id: 'conn3',
        fromNodeId: 'process_node',
        fromPortId: 'output1',
        toNodeId: 'output_node',
        toPortId: 'input1',
        color: Colors.red,
      ),
    ];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          currentProject != null ? currentProject!.name : 'Graph Editor',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
        ),
        actions: [
          if (currentProject != null) ...[
            IconButton(
              icon: const Icon(Icons.save),
              onPressed: _saveCurrentProject,
              tooltip: 'Save Project',
            ),
          ],
        ],
      ),
      body: GraphEditor(
        initialNodes: sampleNodes,
        initialConnections: sampleConnections,
        onNodeAdded: (node) {
          print('Node added: ${node.name}');
        },
        onNodeDeleted: (nodeId) {
          print('Node deleted: $nodeId');
        },
        onConnectionAdded: (connection) {
          print('Connection added: ${connection.id}');
        },
        onConnectionDeleted: (connectionId) {
          print('Connection deleted: $connectionId');
        },
        onGraphSaved: (jsonString) {
          _onGraphSaved(jsonString);
        },
        onGraphLoaded: (graphData) {
          print(
            'Graph loaded: ${graphData.nodes.length} nodes, ${graphData.connections.length} connections',
          );
        },
      ),
    );
  }

  void _showProjectManager() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => GraphProjectManager(
          onProjectSelected: _loadProject,
          onNewProject: () {
            setState(() {
              currentProject = null;

              sampleNodes = _getDefaultNodes();
              sampleConnections = _getDefaultConnections();
            });
          },
        ),
      ),
    );
  }

  void _loadProject(GraphProject project) {
    setState(() {
      currentProject = project;
      // @TODO: Load nodes and connections from project.graphData
    });
    Navigator.of(context).pop();
  }

  void _onGraphSaved(String jsonString) {
    print('Graph saved to JSON (${jsonString.length} characters)');
    if (currentProject != null) {
      _saveCurrentProject();
    }
  }

  Future<void> _saveCurrentProject() async {
    if (currentProject == null) return;

    try {
      // TODO: Get actual graph data from GraphEditor

      final graphData = {
        'nodes': sampleNodes
            .map(
              (n) => {
                'id': n.id,
                'name': n.name,
                'position': {'x': n.position.dx, 'y': n.position.dy},
              },
            )
            .toList(),
        'connections': sampleConnections
            .map((c) => {'id': c.id, 'from': c.fromNodeId, 'to': c.toNodeId})
            .toList(),
      };

      await _projectService.updateProject(
        currentProject!.id,
        graphData: graphData,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Project saved successfully'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving project: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  List<GraphNode> _getDefaultNodes() {
    return [
      GraphNode(
        id: 'input_node',
        name: 'Input Node',
        inputs: [],
        outputs: [
          GraphPort(
            id: 'output1',
            name: 'Value',
            isInput: false,
            color: Colors.green,
          ),
          GraphPort(
            id: 'output2',
            name: 'Signal',
            isInput: false,
            color: Colors.blue,
          ),
        ],
        color: Colors.deepPurple,
        position: const Offset(50, 100),
        size: const Size(150, 80),
      ),
      GraphNode(
        id: 'process_node',
        name: 'Process Node',
        inputs: [
          GraphPort(
            id: 'input1',
            name: 'Data',
            isInput: true,
            color: Colors.green,
          ),
          GraphPort(
            id: 'input2',
            name: 'Control',
            isInput: true,
            color: Colors.blue,
          ),
        ],
        outputs: [
          GraphPort(
            id: 'output1',
            name: 'Result',
            isInput: false,
            color: Colors.red,
          ),
        ],
        color: Colors.indigo,
        position: const Offset(300, 80),
        size: const Size(150, 100),
      ),
      GraphNode(
        id: 'output_node',
        name: 'Output Node',
        inputs: [
          GraphPort(
            id: 'input1',
            name: 'Final',
            isInput: true,
            color: Colors.red,
          ),
        ],
        outputs: [],
        color: Colors.teal,
        position: const Offset(550, 100),
        size: const Size(150, 60),
      ),
    ];
  }

  List<GraphConnection> _getDefaultConnections() {
    return [
      GraphConnection(
        id: 'conn1',
        fromNodeId: 'input_node',
        fromPortId: 'output1',
        toNodeId: 'process_node',
        toPortId: 'input1',
        color: Colors.green,
      ),
      GraphConnection(
        id: 'conn2',
        fromNodeId: 'input_node',
        fromPortId: 'output2',
        toNodeId: 'process_node',
        toPortId: 'input2',
        color: Colors.blue,
      ),
      GraphConnection(
        id: 'conn3',
        fromNodeId: 'process_node',
        fromPortId: 'output1',
        toNodeId: 'output_node',
        toPortId: 'input1',
        color: Colors.red,
      ),
    ];
  }
}
