import 'dart:ui';
import 'dart:math' as math;

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


  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'inputs': inputs.map((port) => port.toJson()).toList(),
      'outputs': outputs.map((port) => port.toJson()).toList(),
      'color': color.value,
      'position': {'x': position.dx, 'y': position.dy},
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
        (json['position']?['x'] as double?) ??
            (json['position']?['dx'] as double?) ??
            0.0,
        (json['position']?['y'] as double?) ??
            (json['position']?['dy'] as double?) ??
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
      size: null,
      templateId: template.id,
      properties: initialProperties,
    );
  }

  Rect get rect {
    final nodeSize = size ?? const Size(150, 100);
    return Rect.fromLTWH(
      position.dx,
      position.dy,
      nodeSize.width,
      nodeSize.height,
    );
  }

  Offset getPortPosition(String portId) {
    final nodeSize = size ?? const Size(150, 100);
    final portHeight = 20.0;
    final headerHeight = 30.0;


    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i].id == portId) {
        return Offset(
          position.dx - 8,
          position.dy + headerHeight + (i * portHeight) + portHeight / 2,
        );
      }
    }


    for (int i = 0; i < outputs.length; i++) {
      if (outputs[i].id == portId) {
        return Offset(
          position.dx + nodeSize.width + 8,
          position.dy + headerHeight + (i * portHeight) + portHeight / 2,
        );
      }
    }

    return position;
  }

  String? getPortAt(Offset localPosition) {
    final nodeSize = size ?? const Size(150, 100);
    final portHeight = 20.0;
    final headerHeight = 30.0;
    const portHitboxWidth = 60.0;
    const portHitboxHeight = 20.0;


    for (int i = 0; i < inputs.length; i++) {
      final portCenter = Offset(
        -30,
        headerHeight + (i * portHeight) + portHeight / 2,
      );


      final portHitboxRect = Rect.fromCenter(
        center: portCenter,
        width: portHitboxWidth,
        height: portHitboxHeight,
      );

      if (portHitboxRect.contains(localPosition)) {
        return inputs[i].id;
      }
    }


    for (int i = 0; i < outputs.length; i++) {
      final portCenter = Offset(
        nodeSize.width + 30,
        headerHeight + (i * portHeight) + portHeight / 2,
      );


      final portHitboxRect = Rect.fromCenter(
        center: portCenter,
        width: portHitboxWidth,
        height: portHitboxHeight,
      );

      if (portHitboxRect.contains(localPosition)) {
        return outputs[i].id;
      }
    }

    return null;
  }


  bool containsWithPorts(Offset localPosition) {
    final nodeSize = size ?? const Size(150, 100);
    const portHitboxWidth = 60.0;
    const portHitboxHeight = 20.0;
    final portHeight = 20.0;
    final headerHeight = 30.0;


    final nodeRect = Rect.fromLTWH(0, 0, nodeSize.width, nodeSize.height);
    if (nodeRect.contains(localPosition)) {
      return true;
    }


    for (int i = 0; i < inputs.length; i++) {
      final portCenter = Offset(
        -30,
        headerHeight + (i * portHeight) + portHeight / 2,
      );

      final portHitboxRect = Rect.fromCenter(
        center: portCenter,
        width: portHitboxWidth,
        height: portHitboxHeight,
      );

      if (portHitboxRect.contains(localPosition)) {
        return true;
      }
    }


    for (int i = 0; i < outputs.length; i++) {
      final portCenter = Offset(
        nodeSize.width + 30,
        headerHeight + (i * portHeight) + portHeight / 2,
      );

      final portHitboxRect = Rect.fromCenter(
        center: portCenter,
        width: portHitboxWidth,
        height: portHitboxHeight,
      );

      if (portHitboxRect.contains(localPosition)) {
        return true;
      }
    }

    return false;
  }


  bool? getPortType(String portId) {

    for (final port in inputs) {
      if (port.id == portId) return true;
    }

    for (final port in outputs) {
      if (port.id == portId) return false;
    }
    return null;
  }


  bool canAcceptConnection(String portId) {
    final portType = getPortType(portId);
    return portType == true;
  }

  /// Auto-scales the node to be rectangular with consistent height but flexible width
  void autoScaleHeight() {
    const portHeight = 20.0;
    const headerHeight = 30.0;
    const standardHeight = 100.0; // Fixed height for all nodes
    const minWidth = 120.0;
    const maxWidth = 300.0;
    const padding = 16.0;

    // Calculate required height based on ports (but use standard height)
    final maxPorts = math.max(inputs.length, outputs.length);
    final calculatedHeight = headerHeight + (maxPorts * portHeight) + padding;
    final newHeight = math.max(calculatedHeight, standardHeight);

    // Calculate width based on content
    double calculatedWidth = minWidth;

    // Consider node name length
    final nameLength = name.length;
    if (nameLength > 10) {
      calculatedWidth = math.min(minWidth + (nameLength - 10) * 8, maxWidth);
    }

    // Consider number of ports (more ports = wider node)
    final totalPorts = inputs.length + outputs.length;
    if (totalPorts > 2) {
      calculatedWidth = math.min(calculatedWidth + (totalPorts - 2) * 15, maxWidth);
    }

    // Ensure minimum width for port labels
    final maxPortNameLength = math.max(
      inputs.map((p) => p.name.length).fold(0, math.max),
      outputs.map((p) => p.name.length).fold(0, math.max),
    );
    if (maxPortNameLength > 5) {
      calculatedWidth = math.min(calculatedWidth + (maxPortNameLength - 5) * 6, maxWidth);
    }

    // Update size if it changed
    final currentSize = size ?? const Size(150, 80);
    print('🔧 Auto-scaling node "$name": inputs=${inputs.length}, outputs=${outputs.length}, calculatedWidth=$calculatedWidth, calculatedHeight=$calculatedHeight, newHeight=$newHeight, currentSize=${currentSize.width}x${currentSize.height}');

    if ((currentSize.height - newHeight).abs() > 1.0 || (currentSize.width - calculatedWidth).abs() > 1.0) {
      size = Size(calculatedWidth, newHeight);
      print('✅ Node "$name" resized to: ${size!.width}x${size!.height}');
    } else {
      print('⏭️ Node "$name" size unchanged: ${currentSize.width}x${currentSize.height}');
    }
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
