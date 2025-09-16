import 'dart:convert';
import 'models.dart';

/// Handles serialization and deserialization of entire graphs
class GraphSerializer {
  /// Serialize a complete graph to JSON string
  static String serializeGraph({
    required List<GraphNode> nodes,
    required List<GraphConnection> connections,
    Map<String, dynamic>? metadata,
  }) {
    final graphData = {
      'version': '1.0',
      'metadata': metadata ?? {},
      'nodes': nodes.map((node) => node.toJson()).toList(),
      'connections': connections
          .map((connection) => connection.toJson())
          .toList(),
    };

    return jsonEncode(graphData);
  }

  /// Deserialize a complete graph from JSON string
  static GraphData deserializeGraph(String jsonString) {
    try {
      final Map<String, dynamic> graphData = jsonDecode(jsonString);

      // Parse nodes
      final List<GraphNode> nodes = [];
      if (graphData['nodes'] != null) {
        for (final nodeJson in graphData['nodes'] as List<dynamic>) {
          nodes.add(GraphNode.fromJson(nodeJson as Map<String, dynamic>));
        }
      }

      // Parse connections
      final List<GraphConnection> connections = [];
      if (graphData['connections'] != null) {
        for (final connectionJson
            in graphData['connections'] as List<dynamic>) {
          connections.add(
            GraphConnection.fromJson(connectionJson as Map<String, dynamic>),
          );
        }
      }

      return GraphData(
        nodes: nodes,
        connections: connections,
        version: graphData['version'] as String? ?? '1.0',
        metadata: graphData['metadata'] as Map<String, dynamic>? ?? {},
      );
    } catch (e) {
      throw GraphSerializationException('Failed to deserialize graph: $e');
    }
  }

  /// Save graph to a pretty-printed JSON string (for debugging)
  static String serializeGraphPretty({
    required List<GraphNode> nodes,
    required List<GraphConnection> connections,
    Map<String, dynamic>? metadata,
  }) {
    final graphData = {
      'version': '1.0',
      'metadata': metadata ?? {},
      'nodes': nodes.map((node) => node.toJson()).toList(),
      'connections': connections
          .map((connection) => connection.toJson())
          .toList(),
    };

    const encoder = JsonEncoder.withIndent('  ');
    return encoder.convert(graphData);
  }
}

/// Container for deserialized graph data
class GraphData {
  final List<GraphNode> nodes;
  final List<GraphConnection> connections;
  final String version;
  final Map<String, dynamic> metadata;

  GraphData({
    required this.nodes,
    required this.connections,
    required this.version,
    required this.metadata,
  });
}

/// Exception thrown when graph serialization fails
class GraphSerializationException implements Exception {
  final String message;

  GraphSerializationException(this.message);

  @override
  String toString() => 'GraphSerializationException: $message';
}
