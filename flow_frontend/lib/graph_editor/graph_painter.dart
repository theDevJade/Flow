import 'package:flutter/material.dart';
import 'models.dart';

class GraphPainter extends CustomPainter {
  final List<GraphNode> nodes;
  final List<GraphConnection> connections;
  final PendingConnection? pendingConnection;
  final double scale;
  final Offset offset;

  GraphPainter({
    required this.nodes,
    required this.connections,
    this.pendingConnection,
    this.scale = 1.0,
    this.offset = Offset.zero,
  });

  @override
  void paint(Canvas canvas, Size size) {
    // Draw grid background
    _drawGrid(canvas, size);

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

    canvas.restore();
  }

  void _drawGrid(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.grey.withOpacity(0.2)
      ..strokeWidth = 1;

    const gridSize = 20.0;

    // Calculate grid offset based on transform
    final gridOffsetX = (offset.dx * scale) % gridSize;
    final gridOffsetY = (offset.dy * scale) % gridSize;

    // Draw vertical lines
    for (double x = gridOffsetX; x < size.width; x += gridSize) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }

    // Draw horizontal lines
    for (double y = gridOffsetY; y < size.height; y += gridSize) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  void _drawNode(Canvas canvas, GraphNode node) {
    final nodeSize = node.size ?? const Size(150, 80);
    final nodeRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(
        node.position.dx,
        node.position.dy,
        nodeSize.width,
        nodeSize.height,
      ),
      const Radius.circular(8),
    );

    // Draw node background
    final nodePaint = Paint()
      ..color = node.color
      ..style = PaintingStyle.fill;
    canvas.drawRRect(nodeRect, nodePaint);

    // Draw node border
    final borderPaint = Paint()
      ..color = Colors.white70
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawRRect(nodeRect, borderPaint);

    // Draw node title (centered)
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
    )..layout(maxWidth: nodeSize.width - 16);

    titlePainter.paint(
      canvas,
      Offset(
        node.position.dx +
            (nodeSize.width - titlePainter.width) / 2, // Center horizontally
        node.position.dy + 8,
      ),
    );

    // Draw input ports
    const portVisualRadius = 6.0; // Smaller visual size
    const portHeight = 20.0;
    const headerHeight = 30.0;

    for (int i = 0; i < node.inputs.length; i++) {
      final port = node.inputs[i];
      final portCenter = Offset(
        node.position.dx - 8, // Fixed to match models.dart hit detection
        node.position.dy + headerHeight + (i * portHeight) + portHeight / 2,
      );

      final portPaint = Paint()
        ..color = port.color
        ..style = PaintingStyle.fill;
      canvas.drawCircle(portCenter, portVisualRadius, portPaint);

      final portBorderPaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2;
      canvas.drawCircle(portCenter, portVisualRadius, portBorderPaint);

      // Draw port label (aligned better)
      final labelPainter = TextPainter(
        text: TextSpan(
          text: port.name,
          style: const TextStyle(color: Colors.white, fontSize: 12),
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
        node.position.dx +
            nodeSize.width +
            8, // Fixed to match models.dart hit detection
        node.position.dy + headerHeight + (i * portHeight) + portHeight / 2,
      );

      final portPaint = Paint()
        ..color = port.color
        ..style = PaintingStyle.fill;
      canvas.drawCircle(portCenter, portVisualRadius, portPaint);

      final portBorderPaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2;
      canvas.drawCircle(portCenter, portVisualRadius, portBorderPaint);

      // Draw port label (right-aligned for outputs)
      final labelPainter = TextPainter(
        text: TextSpan(
          text: port.name,
          style: const TextStyle(color: Colors.white, fontSize: 12),
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

    _drawConnectionLine(canvas, fromPos, toPos, connection.color);
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

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) {
    return true; // Always repaint for now
  }
}

extension OffsetExtensions on Offset {
  Offset get normalized {
    final length = distance;
    if (length == 0) return Offset.zero;
    return this / length;
  }
}
