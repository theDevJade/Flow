import 'dart:ui' show Color;

class NodeColor {
  final double r;
  final double g;
  final double b;
  final double a;

  const NodeColor({
    required this.r,
    required this.g,
    required this.b,
    required this.a,
  });

  factory NodeColor.fromJson(Map<String, dynamic> json) {
    return NodeColor(
      r: json['r']?.toDouble() ?? 0.0,
      g: json['g']?.toDouble() ?? 0.0,
      b: json['b']?.toDouble() ?? 0.0,
      a: json['a']?.toDouble() ?? 1.0,
    );
  }

  Color toColor() {
    return Color.fromRGBO(
      (r * 255).round().clamp(0, 255),
      (g * 255).round().clamp(0, 255),
      (b * 255).round().clamp(0, 255),
      a,
    );
  }

  Map<String, dynamic> toJson() {
    return {'r': r, 'g': g, 'b': b, 'a': a};
  }
}

class NodeSize {
  final double width;
  final double height;

  const NodeSize({required this.width, required this.height});

  factory NodeSize.fromJson(Map<String, dynamic> json) {
    return NodeSize(
      width: json['width']?.toDouble() ?? 150.0,
      height: json['height']?.toDouble() ?? 80.0,
    );
  }

  Map<String, dynamic> toJson() {
    return {'width': width, 'height': height};
  }
}

class NodePort {
  final String id;
  final String name;
  final String type;
  final NodeColor color;

  const NodePort({
    required this.id,
    required this.name,
    required this.type,
    required this.color,
  });

  factory NodePort.fromJson(Map<String, dynamic> json) {
    return NodePort(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      type: json['type'] ?? 'data',
      color: NodeColor.fromJson(json['color'] ?? {}),
    );
  }

  Map<String, dynamic> toJson() {
    return {'id': id, 'name': name, 'type': type, 'color': color.toJson()};
  }
}

class NodeProperty {
  final String name;
  final String type;
  final String label;
  final dynamic defaultValue;
  final String description;
  final List<String>? options;
  final double? min;
  final double? max;

  const NodeProperty({
    required this.name,
    required this.type,
    required this.label,
    required this.defaultValue,
    required this.description,
    this.options,
    this.min,
    this.max,
  });

  factory NodeProperty.fromJson(Map<String, dynamic> json) {
    return NodeProperty(
      name: json['name'] ?? '',
      type: json['type'] ?? 'string',
      label: json['label'] ?? '',
      defaultValue: json['default'],
      description: json['description'] ?? '',
      options: json['options'] != null
          ? List<String>.from(json['options'])
          : null,
      min: json['min']?.toDouble(),
      max: json['max']?.toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{
      'name': name,
      'type': type,
      'label': label,
      'default': defaultValue,
      'description': description,
    };

    if (options != null) {
      json['options'] = options;
    }
    if (min != null) {
      json['min'] = min;
    }
    if (max != null) {
      json['max'] = max;
    }

    return json;
  }
}

class NodeTemplate {
  final String id;
  final String name;
  final String description;
  final String category;
  final NodeColor color;
  final NodeSize size;
  final List<NodePort> inputs;
  final List<NodePort> outputs;
  final List<NodeProperty> properties;

  const NodeTemplate({
    required this.id,
    required this.name,
    required this.description,
    required this.category,
    required this.color,
    required this.size,
    required this.inputs,
    required this.outputs,
    required this.properties,
  });

  factory NodeTemplate.fromJson(Map<String, dynamic> json) {
    return NodeTemplate(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      category: json['category'] ?? 'General',
      color: NodeColor.fromJson(json['color'] ?? {}),
      size: NodeSize.fromJson(json['size'] ?? {}),
      inputs:
          (json['inputs'] as List<dynamic>?)
              ?.map((e) => NodePort.fromJson(e))
              .toList() ??
          [],
      outputs:
          (json['outputs'] as List<dynamic>?)
              ?.map((e) => NodePort.fromJson(e))
              .toList() ??
          [],
      properties:
          (json['properties'] as List<dynamic>?)
              ?.map((e) => NodeProperty.fromJson(e))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'category': category,
      'color': color.toJson(),
      'size': size.toJson(),
      'inputs': inputs.map((e) => e.toJson()).toList(),
      'outputs': outputs.map((e) => e.toJson()).toList(),
      'properties': properties.map((e) => e.toJson()).toList(),
    };
  }
}

class NodeTemplateLibrary {
  final String version;
  final List<NodeTemplate> nodeTemplates;

  const NodeTemplateLibrary({
    required this.version,
    required this.nodeTemplates,
  });

  factory NodeTemplateLibrary.fromJson(Map<String, dynamic> json) {
    return NodeTemplateLibrary(
      version: json['version'] ?? '1.0',
      nodeTemplates:
          (json['node_templates'] as List<dynamic>?)
              ?.map((e) => NodeTemplate.fromJson(e))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'version': version,
      'node_templates': nodeTemplates.map((e) => e.toJson()).toList(),
    };
  }

  List<String> get categories {
    return nodeTemplates.map((template) => template.category).toSet().toList()
      ..sort();
  }

  List<NodeTemplate> getTemplatesByCategory(String category) {
    return nodeTemplates
        .where((template) => template.category == category)
        .toList();
  }

  NodeTemplate? getTemplateById(String id) {
    try {
      return nodeTemplates.firstWhere((template) => template.id == id);
    } catch (e) {
      return null;
    }
  }
}
