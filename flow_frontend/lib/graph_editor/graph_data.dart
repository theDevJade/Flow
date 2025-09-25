import 'models.dart';


class GraphData {
  final List<GraphNode> nodes;
  final List<GraphConnection> connections;
  final Map<String, dynamic> metadata;
  final String version;

  GraphData({
    required this.nodes,
    required this.connections,
    this.metadata = const {},
    this.version = '1.0',
  });

  Map<String, dynamic> toJson() {
    return {
      'nodes': nodes.map((node) => node.toJson()).toList(),
      'connections': connections.map((conn) => conn.toJson()).toList(),
      'metadata': metadata,
      'version': version,
    };
  }
}
