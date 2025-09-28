import 'package:flutter/material.dart';
import 'models.dart';
import 'node_template.dart';
import 'node_template_service.dart';

class PropertyInspector extends StatefulWidget {
  final GraphNode? selectedNode;
  final Function(GraphNode)? onNodeUpdated;
  final VoidCallback? onPanelClosed;
  final Function(bool)? onEditingChanged;

  const PropertyInspector({
    super.key,
    required this.selectedNode,
    this.onNodeUpdated,
    this.onPanelClosed,
    this.onEditingChanged,
  });

  @override
  State<PropertyInspector> createState() => _PropertyInspectorState();
}

class _PropertyInspectorState extends State<PropertyInspector>
    with SingleTickerProviderStateMixin {
  final Map<String, TextEditingController> _textControllers = {};
  NodeTemplate? _nodeTemplate;
  AnimationController? _animationController;
  Animation<double>? _slideAnimation;

  @override
  void initState() {
    super.initState();
    _initializeAnimation();
  }

  void _initializeAnimation() {
    _animationController?.dispose();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _slideAnimation = Tween<double>(begin: 300.0, end: 0.0).animate(
      CurvedAnimation(
        parent: _animationController!,
        curve: Curves.easeOutCubic,
      ),
    );

    _loadTemplate();
    _initializeControllers();

    if (widget.selectedNode != null) {
      _animationController?.forward();
    }
  }

  @override
  void didUpdateWidget(PropertyInspector oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.selectedNode != oldWidget.selectedNode) {
      _loadTemplate();
      _initializeControllers();

      if (widget.selectedNode != null && oldWidget.selectedNode == null) {
        _animationController?.forward();
      } else if (widget.selectedNode == null &&
          oldWidget.selectedNode != null) {
        _animationController?.reverse();
        // Call the panel closed callback when selection is cleared
        widget.onPanelClosed?.call();
      }
    }
  }

  @override
  void dispose() {
    _animationController?.dispose();
    _disposeControllers();
    super.dispose();
  }

  void _loadTemplate() {
    if (widget.selectedNode?.templateId != null) {
      _nodeTemplate = NodeTemplateService.instance.getTemplateById(
        widget.selectedNode!.templateId!,
      );
    } else {
      _nodeTemplate = null;
    }
  }

  void _initializeControllers() {
    if (_nodeTemplate == null || widget.selectedNode == null) return;

    for (final property in _nodeTemplate!.properties) {
      if (property.type == 'string' ||
          property.type == 'number' ||
          property.type == 'float' ||
          property.type == 'int') {
        final currentValue =
            widget.selectedNode!.properties[property.name] ??
            property.defaultValue;
        
        // Only create controller if it doesn't exist, or update text if value changed
        if (!_textControllers.containsKey(property.name)) {
          _textControllers[property.name] = TextEditingController(
            text: currentValue.toString(),
          );
        } else {
          // Update text only if it's different to avoid cursor jumping
          final controller = _textControllers[property.name]!;
          if (controller.text != currentValue.toString()) {
            final selection = controller.selection;
            controller.text = currentValue.toString();
            // Restore cursor position if it was at the end
            if (selection.baseOffset == selection.extentOffset && 
                selection.baseOffset == controller.text.length) {
              controller.selection = TextSelection.collapsed(offset: controller.text.length);
            }
          }
        }
      }
    }
  }

  void _disposeControllers() {
    for (final controller in _textControllers.values) {
      controller.dispose();
    }
    _textControllers.clear();
  }

  void _updateNodeProperty(String propertyName, dynamic value) {
    if (widget.selectedNode == null) return;

    final updatedProperties = Map<String, dynamic>.from(
      widget.selectedNode!.properties,
    );
    updatedProperties[propertyName] = value;

    final updatedNode = widget.selectedNode!.copyWith(
      properties: updatedProperties,
    );
    widget.onNodeUpdated?.call(updatedNode);
  }

  Widget _buildPropertyWidget(NodeProperty property) {
    final currentValue =
        widget.selectedNode?.properties[property.name] ?? property.defaultValue;

    switch (property.type) {
      case 'string':
        return _buildStringProperty(property, currentValue);
      case 'number':
        return _buildNumberProperty(property, currentValue);
      case 'float':
      case 'double':
        return _buildFloatProperty(property, currentValue);
      case 'int':
      case 'integer':
        return _buildIntProperty(property, currentValue);
      case 'bool':
      case 'boolean':
        return _buildBoolProperty(property, currentValue);
      case 'enum':
        return _buildEnumProperty(property, currentValue);
      default:
        return _buildUnsupportedProperty(property);
    }
  }

  Widget _buildStringProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        TextFormField(
          key: ValueKey('${widget.selectedNode?.id}_${property.name}'),
          controller: _textControllers[property.name],
          decoration: InputDecoration(
            hintText: property.defaultValue?.toString(),
            hintStyle: const TextStyle(color: Color(0xFF666666)),
            border: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF404040)),
            ),
            focusedBorder: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF66BB6A)),
            ),
            fillColor: const Color(0xFF1E1E1E),
            filled: true,
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
          ),
          style: const TextStyle(color: Colors.white, fontSize: 12),
          onChanged: (value) {
            _updateNodeProperty(property.name, value);
          },
          onTap: () {
            // Notify that editing has started
            widget.onEditingChanged?.call(true);
            // Ensure cursor is at the end when tapping
            final controller = _textControllers[property.name];
            if (controller != null) {
              controller.selection = TextSelection.collapsed(offset: controller.text.length);
            }
          },
          onTapOutside: (event) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onFieldSubmitted: (value) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onEditingComplete: () {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildNumberProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        TextFormField(
          key: ValueKey('${widget.selectedNode?.id}_${property.name}'),
          controller: _textControllers[property.name],
          decoration: InputDecoration(
            hintText: property.defaultValue?.toString(),
            hintStyle: const TextStyle(color: Color(0xFF666666)),
            border: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF404040)),
            ),
            focusedBorder: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF66BB6A)),
            ),
            fillColor: const Color(0xFF1E1E1E),
            filled: true,
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
            suffixText: property.min != null && property.max != null
                ? '[${property.min}-${property.max}]'
                : null,
            suffixStyle: const TextStyle(
              color: Color(0xFF666666),
              fontSize: 10,
            ),
          ),
          style: const TextStyle(color: Colors.white, fontSize: 12),
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          onTap: () {
            // Notify that editing has started
            widget.onEditingChanged?.call(true);
            // Ensure cursor is at the end when tapping
            final controller = _textControllers[property.name];
            if (controller != null) {
              controller.selection = TextSelection.collapsed(offset: controller.text.length);
            }
          },
          onTapOutside: (event) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onFieldSubmitted: (value) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onEditingComplete: () {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onChanged: (value) {
            // Try to parse as int first, then as double
            final intValue = int.tryParse(value);
            if (intValue != null) {
              // Apply min/max constraints for integers
              num clampedValue = intValue;
              if (property.min != null) {
                clampedValue = clampedValue.clamp(
                  property.min!.toInt(),
                  intValue >= 0 ? double.maxFinite.toInt() : 0,
                );
              }
              if (property.max != null) {
                clampedValue = clampedValue.clamp(
                  intValue < 0 ? double.negativeInfinity.toInt() : 0,
                  property.max!.toInt(),
                );
              }
              _updateNodeProperty(property.name, clampedValue);
            } else {
              final doubleValue = double.tryParse(value);
              if (doubleValue != null) {
                // Apply min/max constraints for doubles
                double clampedValue = doubleValue;
                if (property.min != null) {
                  clampedValue = clampedValue.clamp(
                    property.min!,
                    double.infinity,
                  );
                }
                if (property.max != null) {
                  clampedValue = clampedValue.clamp(
                    double.negativeInfinity,
                    property.max!,
                  );
                }
                _updateNodeProperty(property.name, clampedValue);
              }
            }
          },
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildFloatProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        TextFormField(
          key: ValueKey('${widget.selectedNode?.id}_${property.name}'),
          controller: _textControllers[property.name],
          decoration: InputDecoration(
            hintText: property.defaultValue?.toString(),
            hintStyle: const TextStyle(color: Color(0xFF666666)),
            border: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF404040)),
            ),
            focusedBorder: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF66BB6A)),
            ),
            fillColor: const Color(0xFF1E1E1E),
            filled: true,
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
            suffixText: property.min != null && property.max != null
                ? '[${property.min}-${property.max}]'
                : null,
            suffixStyle: const TextStyle(
              color: Color(0xFF666666),
              fontSize: 10,
            ),
          ),
          style: const TextStyle(color: Colors.white, fontSize: 12),
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          onTap: () {
            // Notify that editing has started
            widget.onEditingChanged?.call(true);
            // Ensure cursor is at the end when tapping
            final controller = _textControllers[property.name];
            if (controller != null) {
              controller.selection = TextSelection.collapsed(offset: controller.text.length);
            }
          },
          onTapOutside: (event) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onFieldSubmitted: (value) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onEditingComplete: () {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onChanged: (value) {
            final doubleValue = double.tryParse(value);
            if (doubleValue != null) {
              double clampedValue = doubleValue;
              if (property.min != null) {
                clampedValue = clampedValue.clamp(
                  property.min!,
                  double.infinity,
                );
              }
              if (property.max != null) {
                clampedValue = clampedValue.clamp(
                  double.negativeInfinity,
                  property.max!,
                );
              }
              _updateNodeProperty(property.name, clampedValue);
            }
          },
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildIntProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        TextFormField(
          key: ValueKey('${widget.selectedNode?.id}_${property.name}'),
          controller: _textControllers[property.name],
          decoration: InputDecoration(
            hintText: property.defaultValue?.toString(),
            hintStyle: const TextStyle(color: Color(0xFF666666)),
            border: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF404040)),
            ),
            focusedBorder: const OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF66BB6A)),
            ),
            fillColor: const Color(0xFF1E1E1E),
            filled: true,
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
          ),
          style: const TextStyle(color: Colors.white, fontSize: 12),
          keyboardType: TextInputType.number,
          onTap: () {
            // Notify that editing has started
            widget.onEditingChanged?.call(true);
            // Ensure cursor is at the end when tapping
            final controller = _textControllers[property.name];
            if (controller != null) {
              controller.selection = TextSelection.collapsed(offset: controller.text.length);
            }
          },
          onTapOutside: (event) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onFieldSubmitted: (value) {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onEditingComplete: () {
            // Notify that editing has stopped
            widget.onEditingChanged?.call(false);
          },
          onChanged: (value) {
            final intValue = int.tryParse(value);
            if (intValue != null) {
              _updateNodeProperty(property.name, intValue);
            }
          },
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildBoolProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                property.label,
                style: const TextStyle(
                  fontWeight: FontWeight.w500,
                  color: Colors.white,
                  fontSize: 13,
                ),
              ),
            ),
            Switch.adaptive(
              value:
                  currentValue as bool? ??
                  property.defaultValue as bool? ??
                  false,
              onChanged: (value) {
                _updateNodeProperty(property.name, value);
              },
              activeColor: const Color(0xFF66BB6A),
              inactiveThumbColor: const Color(0xFF666666),
              inactiveTrackColor: const Color(0xFF404040),
            ),
          ],
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildEnumProperty(NodeProperty property, dynamic currentValue) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        DropdownButtonFormField<String>(
          value: currentValue as String? ?? property.defaultValue as String?,
          decoration: const InputDecoration(
            border: OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF404040)),
            ),
            focusedBorder: OutlineInputBorder(
              borderSide: BorderSide(color: Color(0xFF66BB6A)),
            ),
            fillColor: Color(0xFF1E1E1E),
            filled: true,
            isDense: true,
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          style: const TextStyle(color: Colors.white, fontSize: 12),
          dropdownColor: const Color(0xFF2A2A2A),
          items: property.options?.map((option) {
            return DropdownMenuItem<String>(
              value: option,
              child: Text(option, style: const TextStyle(color: Colors.white)),
            );
          }).toList(),
          onChanged: (value) {
            if (value != null) {
              _updateNodeProperty(property.name, value);
            }
          },
        ),
        if (property.description.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              property.description,
              style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
            ),
          ),
      ],
    );
  }

  Widget _buildUnsupportedProperty(NodeProperty property) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          property.label,
          style: const TextStyle(
            fontWeight: FontWeight.w500,
            color: Colors.white,
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xFF404040)),
            borderRadius: BorderRadius.circular(4),
            color: const Color(0xFF1E1E1E),
          ),
          child: Text(
            'Unsupported property type: ${property.type}',
            style: const TextStyle(
              color: Color(0xFF888888),
              fontStyle: FontStyle.italic,
              fontSize: 11,
            ),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_slideAnimation == null) {
      return _buildContent(context);
    }

    return AnimatedBuilder(
      animation: _slideAnimation!,
      builder: (context, child) {
        return Transform.translate(
          offset: Offset(_slideAnimation!.value, 0),
          child: _buildContent(context),
        );
      },
    );
  }

  Widget _buildContent(BuildContext context) {
    if (widget.selectedNode == null) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: const BoxDecoration(
          color: Color(0xFF2A2A2A),
          border: Border(left: BorderSide(color: Color(0xFF404040))),
        ),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.info_outline,
                size: 48,
                color: Color(0xFF666666),
              ),
              const SizedBox(height: 16),
              const Text(
                'No Selection',
                style: TextStyle(
                  color: Color(0xFFAAAAAA),
                  fontWeight: FontWeight.w500,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Select a node to edit its properties',
                style: TextStyle(color: Color(0xFF888888), fontSize: 12),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: Color(0xFF2A2A2A),
        border: Border(left: BorderSide(color: Color(0xFF404040))),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: const BoxDecoration(
              color: Color(0xFF333333),
              border: Border(bottom: BorderSide(color: Color(0xFF404040))),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.settings, size: 20, color: Color(0xFF66BB6A)),
                    SizedBox(width: 8),
                    Text(
                      'Properties',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: widget.selectedNode!.color.withAlpha(50),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    widget.selectedNode!.name,
                    style: TextStyle(
                      color: widget.selectedNode!.color,
                      fontWeight: FontWeight.w500,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
          ),

          // Properties list
          Expanded(
            child: _nodeTemplate == null || _nodeTemplate!.properties.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.settings_outlined,
                            size: 48,
                            color: Color(0xFF666666),
                          ),
                          const SizedBox(height: 16),
                          const Text(
                            'No Properties',
                            style: TextStyle(
                              color: Color(0xFFAAAAAA),
                              fontSize: 14,
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'This node has no editable properties',
                            style: TextStyle(
                              color: Color(0xFF888888),
                              fontSize: 12,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: _nodeTemplate!.properties.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 16),
                    itemBuilder: (context, index) {
                      final property = _nodeTemplate!.properties[index];
                      return _buildPropertyWidget(property);
                    },
                  ),
          ),

          // Node info
          Container(
            padding: const EdgeInsets.all(16),
            decoration: const BoxDecoration(
              color: Color(0xFF1E1E1E),
              border: Border(top: BorderSide(color: Color(0xFF404040))),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Node Info',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: Color(0xFFAAAAAA),
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 8),
                _buildInfoRow('ID', widget.selectedNode!.id),
                const SizedBox(height: 4),
                if (widget.selectedNode!.templateId != null)
                  _buildInfoRow('Template', widget.selectedNode!.templateId!),
                const SizedBox(height: 4),
                _buildInfoRow(
                  'Inputs',
                  '${widget.selectedNode!.inputs.length}',
                ),
                const SizedBox(height: 4),
                _buildInfoRow(
                  'Outputs',
                  '${widget.selectedNode!.outputs.length}',
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      children: [
        Expanded(
          flex: 2,
          child: Text(
            '$label:',
            style: const TextStyle(color: Color(0xFF888888), fontSize: 11),
          ),
        ),
        Expanded(
          flex: 3,
          child: Text(
            value,
            style: const TextStyle(
              fontFamily: 'monospace',
              fontSize: 10,
              color: Color(0xFFAAAAAA),
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}
