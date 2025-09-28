import 'package:flutter/material.dart';
import 'models.dart';

class GraphPainter extends CustomPainter {
  final List<GraphNode> nodes;
  final List<GraphConnection> connections;
  final PendingConnection? pendingConnection;
  final double scale;
  final Offset offset;
  final String? hoveredPortNodeId;
  final String? hoveredPortId;
  final String? hoveredNodeId;
  final String? selectedNodeId;
  final double connectionAnimation;
  final String? animatingConnectionId;
  final bool showDebugHitboxes;

  GraphPainter({
    required this.nodes,
    required this.connections,
    this.pendingConnection,
    this.scale = 1.0,
    this.offset = Offset.zero,
    this.hoveredPortNodeId,
    this.hoveredPortId,
    this.hoveredNodeId,
    this.selectedNodeId,
    this.connectionAnimation = 1.0,
    this.animatingConnectionId,
    this.showDebugHitboxes = false,
  });

  @override
  void paint(Canvas canvas, Size size) {
    // Apply transform
    canvas.save();
    canvas.translate(offset.dx, offset.dy);
    canvas.scale(scale);

    // Draw connections
    for (final connection in connections) {
      _drawConnection(canvas, connection);
    }

    // Draw pending connection
    if (pendingConnection != null) {
      _drawPendingConnection(canvas, pendingConnection!);
    }


    // Draw nodes
    for (final node in nodes) {
      _drawNode(canvas, node);
    }

    // Draw debug hitboxes
    if (showDebugHitboxes) {
      _drawDebugHitboxes(canvas);
    }

    canvas.restore();
  }


  void _drawNode(Canvas canvas, GraphNode node) {
    final nodeSize = node.size ?? const Size(150, 100);
    final nodeRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(
        node.position.dx,
        node.position.dy,
        nodeSize.width,
        nodeSize.height,
      ),
      const Radius.circular(8),
    );

    // Check if node is hovered or selected
    final isHovered = hoveredNodeId == node.id;
    final isSelected = selectedNodeId == node.id;

    // Draw node background with hover/selection effects
    final nodePaint = Paint()
      ..color = isHovered 
          ? node.color.withOpacity(0.9)
          : isSelected
              ? node.color.withOpacity(0.95)
              : node.color
      ..style = PaintingStyle.fill;
    canvas.drawRRect(nodeRect, nodePaint);

    // Draw hover glow effect
    if (isHovered) {
      final glowPaint = Paint()
        ..color = node.color.withOpacity(0.3)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4;
      canvas.drawRRect(nodeRect, glowPaint);
    }

    // Draw node border with different styles for hover/selection
    final borderPaint = Paint()
      ..color = isSelected 
          ? Colors.cyan
          : isHovered 
              ? Colors.white
              : Colors.white70
      ..style = PaintingStyle.stroke
      ..strokeWidth = isSelected ? 3 : isHovered ? 2.5 : 2;
    canvas.drawRRect(nodeRect, borderPaint);

    // Draw node title (centered with proper text overflow handling)
    final titlePainter = TextPainter(
      text: TextSpan(
        text: node.name,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 14,
          fontWeight: FontWeight.bold,
        ),
      ),
      textDirection: TextDirection.ltr,
      maxLines: 2, // Allow up to 2 lines for long titles
      ellipsis: '...', // Add ellipsis for overflow
    )..layout(maxWidth: nodeSize.width - 16);

    // Calculate title position with proper centering
    final titleX = node.position.dx + (nodeSize.width - titlePainter.width) / 2;
    final titleY = node.position.dy + 8;
    
    // Ensure title doesn't go outside node bounds
    final clampedTitleX = titleX.clamp(
      node.position.dx + 8, // Left margin
      node.position.dx + nodeSize.width - titlePainter.width - 8, // Right margin
    );

    titlePainter.paint(
      canvas,
      Offset(clampedTitleX, titleY),
    );

    // Draw input ports
    const portVisualRadius = 6.0;
    const portHeight = 20.0;
    const headerHeight = 30.0;

    for (int i = 0; i < node.inputs.length; i++) {
      final port = node.inputs[i];
      final portCenter = Offset(
        node.position.dx - 8, // Left side of node
        node.position.dy + headerHeight + (i * portHeight) + portHeight / 2,
      );

      // Check if this port is being hovered
      final isHovered = hoveredPortNodeId == node.id && hoveredPortId == port.id;
      final isPendingTarget = pendingConnection != null && 
          node.canAcceptConnection(port.id) &&
          _isValidConnectionTarget(node.id, port.id);

      // Draw port background (larger when hovered)
      final portRadius = isHovered ? portVisualRadius + 2 : portVisualRadius;
      final portPaint = Paint()
        ..color = isPendingTarget 
            ? Colors.green.withOpacity(0.8)
            : isHovered 
                ? port.color.withOpacity(0.9)
                : port.color
        ..style = PaintingStyle.fill;
      canvas.drawCircle(portCenter, portRadius, portPaint);

      // Draw port border
      final portBorderPaint = Paint()
        ..color = isPendingTarget 
            ? Colors.green
            : isHovered 
                ? Colors.white
                : Colors.white70
        ..style = PaintingStyle.stroke
        ..strokeWidth = isHovered ? 3 : 2;
      canvas.drawCircle(portCenter, portRadius, portBorderPaint);

      // Draw port label (aligned better)
      final labelPainter = TextPainter(
        text: TextSpan(
          text: port.name,
          style: TextStyle(
            color: isHovered ? Colors.white : Colors.white70, 
            fontSize: 12,
            fontWeight: isHovered ? FontWeight.bold : FontWeight.normal,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();

      labelPainter.paint(
        canvas,
        Offset(
          node.position.dx + 16, // Better spacing from port
          node.position.dy +
              headerHeight +
              (i * portHeight) +
              portHeight / 2 -
              labelPainter.height / 2, // Center vertically
        ),
      );
    }

    // Draw output ports
    for (int i = 0; i < node.outputs.length; i++) {
      final port = node.outputs[i];
      final portCenter = Offset(
        node.position.dx + nodeSize.width + 8, // Right side of node
        node.position.dy + headerHeight + (i * portHeight) + portHeight / 2,
      );

      // Check if this port is being hovered
      final isHovered = hoveredPortNodeId == node.id && hoveredPortId == port.id;

      // Draw port background (larger when hovered)
      final portRadius = isHovered ? portVisualRadius + 2 : portVisualRadius;
      final portPaint = Paint()
        ..color = isHovered 
            ? port.color.withOpacity(0.9)
            : port.color
        ..style = PaintingStyle.fill;
      canvas.drawCircle(portCenter, portRadius, portPaint);

      // Draw port border
      final portBorderPaint = Paint()
        ..color = isHovered 
            ? Colors.white
            : Colors.white70
        ..style = PaintingStyle.stroke
        ..strokeWidth = isHovered ? 3 : 2;
      canvas.drawCircle(portCenter, portRadius, portBorderPaint);

      // Draw port label (right-aligned for outputs)
      final labelPainter = TextPainter(
        text: TextSpan(
          text: port.name,
          style: TextStyle(
            color: isHovered ? Colors.white : Colors.white70, 
            fontSize: 12,
            fontWeight: isHovered ? FontWeight.bold : FontWeight.normal,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();

      labelPainter.paint(
        canvas,
        Offset(
          node.position.dx +
              nodeSize.width -
              labelPainter.width -
              16, // Better spacing from port
          node.position.dy +
              headerHeight +
              (i * portHeight) +
              portHeight / 2 -
              labelPainter.height / 2, // Center vertically
        ),
      );
    }
  }

  void _drawConnection(Canvas canvas, GraphConnection connection) {
    final fromNode = nodes.firstWhere((n) => n.id == connection.fromNodeId);
    final toNode = nodes.firstWhere((n) => n.id == connection.toNodeId);

    final fromPos = fromNode.getPortPosition(connection.fromPortId);
    final toPos = toNode.getPortPosition(connection.toPortId);

    // Apply animation if this connection is being animated
    if (connectionAnimation < 1.0 && animatingConnectionId == connection.id) {
      _drawAnimatedConnectionLine(canvas, fromPos, toPos, connection.color);
    } else {
      _drawConnectionLine(canvas, fromPos, toPos, connection.color);
    }
  }

  void _drawPendingConnection(Canvas canvas, PendingConnection pending) {
    final fromNode = nodes.firstWhere((n) => n.id == pending.fromNodeId);
    final fromPos = fromNode.getPortPosition(pending.fromPortId);

    _drawConnectionLine(
      canvas,
      fromPos,
      pending.currentPosition,
      Colors.yellow.withOpacity(0.7),
    );
  }


  void _drawDashedConnectionLine(Canvas canvas, Offset from, Offset to, Color color) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;

    final path = Path();
    path.moveTo(from.dx, from.dy);

    // Create a smooth curve
    final controlPoint1 = Offset(from.dx + (to.dx - from.dx) * 0.5, from.dy);
    final controlPoint2 = Offset(from.dx + (to.dx - from.dx) * 0.5, to.dy);

    path.cubicTo(
      controlPoint1.dx,
      controlPoint1.dy,
      controlPoint2.dx,
      controlPoint2.dy,
      to.dx,
      to.dy,
    );

    // Draw dashed line
    final dashLength = 8.0;
    final dashSpace = 4.0;
    final pathMetrics = path.computeMetrics();
    
    for (final pathMetric in pathMetrics) {
      double distance = 0.0;
      while (distance < pathMetric.length) {
        final extractPath = pathMetric.extractPath(
          distance,
          distance + dashLength,
        );
        canvas.drawPath(extractPath, paint);
        distance += dashLength + dashSpace;
      }
    }
  }

  void _drawAnimatedConnectionLine(Canvas canvas, Offset from, Offset to, Color color) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.0;

    final path = Path();
    path.moveTo(from.dx, from.dy);

    // Calculate animated end point
    final animatedTo = Offset.lerp(from, to, connectionAnimation)!;

    // Create a smooth curve with better control points
    final distance = (animatedTo - from).distance;
    final controlOffset = distance * 0.3;
    
    final controlPoint1 = Offset(from.dx + controlOffset, from.dy);
    final controlPoint2 = Offset(animatedTo.dx - controlOffset, animatedTo.dy);

    path.cubicTo(
      controlPoint1.dx,
      controlPoint1.dy,
      controlPoint2.dx,
      controlPoint2.dy,
      animatedTo.dx,
      animatedTo.dy,
    );

    canvas.drawPath(path, paint);

    // Draw animated arrow
    if (connectionAnimation > 0.1) { // Only show arrow when connection is partially drawn
      _drawArrow(canvas, animatedTo, controlPoint2, color);
    }
  }

  void _drawConnectionLine(Canvas canvas, Offset from, Offset to, Color color) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;

    final path = Path();
    path.moveTo(from.dx, from.dy);

    // Create a smooth curve
    final controlPoint1 = Offset(from.dx + (to.dx - from.dx) * 0.5, from.dy);
    final controlPoint2 = Offset(from.dx + (to.dx - from.dx) * 0.5, to.dy);

    path.cubicTo(
      controlPoint1.dx,
      controlPoint1.dy,
      controlPoint2.dx,
      controlPoint2.dy,
      to.dx,
      to.dy,
    );

    canvas.drawPath(path, paint);

    // Draw arrow at the end - use the curve's tangent direction at the endpoint
    _drawArrow(canvas, to, controlPoint2, color);
  }

  void _drawArrow(Canvas canvas, Offset to, Offset controlPoint, Color color) {
    // Calculate the tangent direction at the endpoint of the curve
    // For a cubic Bezier curve, the tangent at the end is the direction from the last control point to the end point
    Offset direction = (to - controlPoint).normalized;

    // If the direction is zero (control point equals endpoint), fall back to horizontal direction
    if (direction.distance == 0) {
      direction = const Offset(1, 0);
    }

    const arrowSize = 10.0; // Slightly smaller for better proportions
    const arrowWidth = 6.0; // Width of the arrowhead

    // Position the arrow tip slightly back from the connection endpoint
    final arrowTip = to - direction * 3;

    // Calculate perpendicular direction for arrow wings
    final perpendicular = Offset(-direction.dy, direction.dx);

    // Calculate arrow wing points
    final arrowBase = arrowTip - direction * arrowSize;
    final arrowWing1 = arrowBase + perpendicular * arrowWidth * 0.5;
    final arrowWing2 = arrowBase - perpendicular * arrowWidth * 0.5;

    final arrowPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    // Draw filled triangle arrow
    final arrowPath = Path()
      ..moveTo(arrowTip.dx, arrowTip.dy)
      ..lineTo(arrowWing1.dx, arrowWing1.dy)
      ..lineTo(arrowWing2.dx, arrowWing2.dy)
      ..close();

    canvas.drawPath(arrowPath, arrowPaint);
  }

  /// Check if a connection target is valid
  bool _isValidConnectionTarget(String targetNodeId, String targetPortId) {
    if (pendingConnection == null) return false;
    
    // Can't connect to the same node
    if (pendingConnection!.fromNodeId == targetNodeId) return false;
    
    // Find the source node and port
    final sourceNode = nodes.firstWhere((n) => n.id == pendingConnection!.fromNodeId);
    final sourcePortType = sourceNode.getPortType(pendingConnection!.fromPortId);
    
    // Source must be an output port
    if (sourcePortType != false) return false;
    
    // Target must be an input port
    final targetNode = nodes.firstWhere((n) => n.id == targetNodeId);
    final targetPortType = targetNode.getPortType(targetPortId);
    if (targetPortType != true) return false;
    
    return true;
  }

  void _drawDebugHitboxes(Canvas canvas) {
    // Draw node hitboxes
    for (final node in nodes) {
      final nodeSize = node.size ?? const Size(150, 100);
      final nodeRect = Rect.fromLTWH(
        node.position.dx,
        node.position.dy,
        nodeSize.width,
        nodeSize.height,
      );

      // Draw node hitbox
      final nodeHitboxPaint = Paint()
        ..color = Colors.red.withOpacity(0.3)
        ..style = PaintingStyle.fill;
      canvas.drawRect(nodeRect, nodeHitboxPaint);

      // Draw node hitbox border
      final nodeBorderPaint = Paint()
        ..color = Colors.red
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2;
      canvas.drawRect(nodeRect, nodeBorderPaint);

      // Draw port hitboxes
      const portHeight = 20.0;
      const headerHeight = 30.0;
      const portHitboxWidth = 60.0; // Much wider rectangular hitbox extending further out
      const portHitboxHeight = 20.0; // Height of the rectangular hitbox

      // Input ports - use exact same logic as getPortAt method
      for (int i = 0; i < node.inputs.length; i++) {
        // Calculate relative position (exact same as getPortAt method)
        final relativePortCenter = Offset(
          -30, // Move center further left so hitbox extends more to the left
          headerHeight + (i * portHeight) + portHeight / 2,
        );
        
        // Convert to absolute position for drawing
        final absolutePortCenter = Offset(
          node.position.dx + relativePortCenter.dx,
          node.position.dy + relativePortCenter.dy,
        );

        // Create rectangular hitbox (exact same as getPortAt method)
        final portHitboxRect = Rect.fromCenter(
          center: absolutePortCenter,
          width: portHitboxWidth,
          height: portHitboxHeight,
        );

        final portHitboxPaint = Paint()
          ..color = Colors.blue.withOpacity(0.3)
          ..style = PaintingStyle.fill;
        canvas.drawRect(portHitboxRect, portHitboxPaint);

        final portBorderPaint = Paint()
          ..color = Colors.blue
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1;
        canvas.drawRect(portHitboxRect, portBorderPaint);
      }

      // Output ports - use exact same logic as getPortAt method
      for (int i = 0; i < node.outputs.length; i++) {
        // Calculate relative position (exact same as getPortAt method)
        final relativePortCenter = Offset(
          nodeSize.width + 30, // Move center further right so hitbox extends more to the right
          headerHeight + (i * portHeight) + portHeight / 2,
        );
        
        // Convert to absolute position for drawing
        final absolutePortCenter = Offset(
          node.position.dx + relativePortCenter.dx,
          node.position.dy + relativePortCenter.dy,
        );

        // Create rectangular hitbox (exact same as getPortAt method)
        final portHitboxRect = Rect.fromCenter(
          center: absolutePortCenter,
          width: portHitboxWidth,
          height: portHitboxHeight,
        );

        final portHitboxPaint = Paint()
          ..color = Colors.green.withOpacity(0.3)
          ..style = PaintingStyle.fill;
        canvas.drawRect(portHitboxRect, portHitboxPaint);

        final portBorderPaint = Paint()
          ..color = Colors.green
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1;
        canvas.drawRect(portHitboxRect, portBorderPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) {
    if (oldDelegate is! GraphPainter) return true;
    
    return oldDelegate.nodes != nodes ||
        oldDelegate.connections != connections ||
        oldDelegate.pendingConnection != pendingConnection ||
        oldDelegate.hoveredPortNodeId != hoveredPortNodeId ||
        oldDelegate.hoveredPortId != hoveredPortId ||
        oldDelegate.hoveredNodeId != hoveredNodeId ||
        oldDelegate.selectedNodeId != selectedNodeId ||
        oldDelegate.connectionAnimation != connectionAnimation ||
        oldDelegate.animatingConnectionId != animatingConnectionId ||
        oldDelegate.showDebugHitboxes != showDebugHitboxes ||
        oldDelegate.scale != scale ||
        oldDelegate.offset != offset;
  }
}

extension OffsetExtensions on Offset {
  Offset get normalized {
    final length = distance;
    if (length == 0) return Offset.zero;
    return this / length;
  }
}
