import 'dart:ui';

import 'package:flutter/material.dart';
import 'node_template.dart';

class GraphPort {
  final String id;
  final String name;
  final bool isInput;
  final Color color;

  GraphPort({
    required this.id,
    required this.name,
    required this.isInput,
    this.color = Colors.grey,
  });

  // JSON serialization
  Map<String, dynamic> toJson() {
    return {'id': id, 'name': name, 'isInput': isInput, 'color': color.value};
  }

  factory GraphPort.fromJson(Map<String, dynamic> json) {
    return GraphPort(
      id: json['id'] as String,
      name: json['name'] as String,
      isInput: json['isInput'] as bool,
      color: Color(json['color'] as int? ?? Colors.grey.value),
    );
  }
}

class GraphNode {
  final String id;
  final String name;
  final List<GraphPort> inputs;
  final List<GraphPort> outputs;
  final Color color;
  Offset position;
  Size? size;
  final String? templateId;
  final Map<String, dynamic> properties;

  GraphNode({
    required this.id,
    required this.name,
    required this.inputs,
    required this.outputs,
    this.color = Colors.blue,
    this.position = Offset.zero,
    this.size,
    this.templateId,
    this.properties = const {},
  });

  GraphNode copyWith({
    String? id,
    String? name,
    List<GraphPort>? inputs,
    List<GraphPort>? outputs,
    Color? color,
    Offset? position,
    Size? size,
    String? templateId,
    Map<String, dynamic>? properties,
  }) {
    return GraphNode(
      id: id ?? this.id,
      name: name ?? this.name,
      inputs: inputs ?? this.inputs,
      outputs: outputs ?? this.outputs,
      color: color ?? this.color,
      position: position ?? this.position,
      size: size ?? this.size,
      templateId: templateId ?? this.templateId,
      properties: properties ?? this.properties,
    );
  }

  // JSON serialization
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'inputs': inputs.map((port) => port.toJson()).toList(),
      'outputs': outputs.map((port) => port.toJson()).toList(),
      'color': color.value,
      'position': {'dx': position.dx, 'dy': position.dy},
      'size':
          size != null ? {'width': size!.width, 'height': size!.height} : null,
      'templateId': templateId,
      'properties': properties,
    };
  }

  factory GraphNode.fromJson(Map<String, dynamic> json) {
    return GraphNode(
      id: json['id'] as String,
      name: json['name'] as String,
      inputs: (json['inputs'] as List<dynamic>)
          .map(
            (portJson) => GraphPort.fromJson(portJson as Map<String, dynamic>),
          )
          .toList(),
      outputs: (json['outputs'] as List<dynamic>)
          .map(
            (portJson) => GraphPort.fromJson(portJson as Map<String, dynamic>),
          )
          .toList(),
      color: Color(json['color'] as int? ?? Colors.blue.value),
      position: Offset(
        (json['position']?['dx'] as double?) ??
            (json['position']?['x'] as double?) ??
            0.0,
        (json['position']?['dy'] as double?) ??
            (json['position']?['y'] as double?) ??
            0.0,
      ),
      size: json['size'] != null
          ? Size(
              (json['size']['width'] as double?) ?? 150.0,
              (json['size']['height'] as double?) ?? 80.0,
            )
          : null,
      templateId: json['templateId'] as String?,
      properties: Map<String, dynamic>.from(json['properties'] ?? {}),
    );
  }

  factory GraphNode.fromTemplate(
    NodeTemplate template, {
    required String nodeId,
    Offset? position,
  }) {
    // Create initial properties with default values
    final Map<String, dynamic> initialProperties = {};
    for (final property in template.properties) {
      initialProperties[property.name] = property.defaultValue;
    }

    return GraphNode(
      id: nodeId,
      name: template.name,
      inputs: template.inputs
          .map(
            (port) => GraphPort(
              id: port.id,
              name: port.name,
              isInput: true,
              color: port.color.toColor(),
            ),
          )
          .toList(),
      outputs: template.outputs
          .map(
            (port) => GraphPort(
              id: port.id,
              name: port.name,
              isInput: false,
              color: port.color.toColor(),
            ),
          )
          .toList(),
      color: template.color.toColor(),
      position: position ?? Offset.zero,
      size: Size(template.size.width, template.size.height),
      templateId: template.id,
      properties: initialProperties,
    );
  }

  Rect get rect {
    final nodeSize = size ?? const Size(150, 80);
    return Rect.fromLTWH(
      position.dx,
      position.dy,
      nodeSize.width,
      nodeSize.height,
    );
  }

  Offset getPortPosition(String portId) {
    final nodeSize = size ?? const Size(150, 80);
    final portHeight = 20.0;
    final headerHeight = 30.0;

    // Find input port position
    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i].id == portId) {
        return Offset(
          position.dx - 8, // Left side of node
          position.dy + headerHeight + (i * portHeight) + portHeight / 2,
        );
      }
    }

    // Find output port position
    for (int i = 0; i < outputs.length; i++) {
      if (outputs[i].id == portId) {
        return Offset(
          position.dx + nodeSize.width + 8, // Right side of node
          position.dy + headerHeight + (i * portHeight) + portHeight / 2,
        );
      }
    }

    return position;
  }

  String? getPortAt(Offset localPosition) {
    final nodeSize = size ?? const Size(150, 80);
    final portHeight = 20.0;
    final headerHeight = 30.0;
    const portRadius = 20.0; // Larger hit detection radius for easier clicking

    print('🎯 Port detection: localPos=$localPosition in node ${this.id}');

    // Check input ports (left side)
    for (int i = 0; i < inputs.length; i++) {
      final portCenter = Offset(
        -8, // Left side offset (matches painter: node.position.dx - 8)
        headerHeight + (i * portHeight) + portHeight / 2,
      );
      final distance = (localPosition - portCenter).distance;
      print(
        '  📍 Input ${inputs[i].id}: center=$portCenter, distance=${distance.toStringAsFixed(1)}',
      );
      if (distance <= portRadius) {
        print(
          '  ✅ HIT INPUT PORT: ${inputs[i].id} (distance: ${distance.toStringAsFixed(1)})',
        );
        return inputs[i].id;
      }
    }

    // Check output ports (right side)
    for (int i = 0; i < outputs.length; i++) {
      final portCenter = Offset(
        nodeSize.width +
            8, // Right side offset (matches painter: node.position.dx + nodeSize.width + 8)
        headerHeight + (i * portHeight) + portHeight / 2,
      );
      final distance = (localPosition - portCenter).distance;
      print(
        '  📍 Output ${outputs[i].id}: center=$portCenter, distance=${distance.toStringAsFixed(1)}',
      );
      if (distance <= portRadius) {
        print(
          '  ✅ HIT OUTPUT PORT: ${outputs[i].id} (distance: ${distance.toStringAsFixed(1)})',
        );
        return outputs[i].id;
      }
    }

    print('  ❌ No port hit within ${portRadius}px radius');
    return null;
  }
}

class GraphConnection {
  final String id;
  final String fromNodeId;
  final String fromPortId;
  final String toNodeId;
  final String toPortId;
  final Color color;

  GraphConnection({
    required this.id,
    required this.fromNodeId,
    required this.fromPortId,
    required this.toNodeId,
    required this.toPortId,
    this.color = Colors.white70,
  });

  GraphConnection copyWith({
    String? id,
    String? fromNodeId,
    String? fromPortId,
    String? toNodeId,
    String? toPortId,
    Color? color,
  }) {
    return GraphConnection(
      id: id ?? this.id,
      fromNodeId: fromNodeId ?? this.fromNodeId,
      fromPortId: fromPortId ?? this.fromPortId,
      toNodeId: toNodeId ?? this.toNodeId,
      toPortId: toPortId ?? this.toPortId,
      color: color ?? this.color,
    );
  }

  // JSON serialization
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fromNodeId': fromNodeId,
      'fromPortId': fromPortId,
      'toNodeId': toNodeId,
      'toPortId': toPortId,
      'color': color.value,
    };
  }

  factory GraphConnection.fromJson(Map<String, dynamic> json) {
    return GraphConnection(
      id: json['id'] as String,
      fromNodeId: json['fromNodeId'] as String,
      fromPortId: json['fromPortId'] as String,
      toNodeId: json['toNodeId'] as String,
      toPortId: json['toPortId'] as String,
      color: Color(json['color'] as int? ?? Colors.white70.value),
    );
  }
}

class PendingConnection {
  final String fromNodeId;
  final String fromPortId;
  final Offset currentPosition;

  PendingConnection({
    required this.fromNodeId,
    required this.fromPortId,
    required this.currentPosition,
  });

  PendingConnection copyWith({
    String? fromNodeId,
    String? fromPortId,
    Offset? currentPosition,
  }) {
    return PendingConnection(
      fromNodeId: fromNodeId ?? this.fromNodeId,
      fromPortId: fromPortId ?? this.fromPortId,
      currentPosition: currentPosition ?? this.currentPosition,
    );
  }
}
