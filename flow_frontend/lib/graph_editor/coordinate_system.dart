import 'package:flutter/material.dart';
import 'package:vector_math/vector_math_64.dart' as vm;

class CoordinateSystem {
  final double scale;
  final Offset panOffset;

  const CoordinateSystem({required this.scale, required this.panOffset});

  /// Convert screen coordinates to graph coordinates
  Offset screenToGraph(Offset screenPosition) {
    // Create transformation matrix that matches canvas operations:
    // canvas.translate(panOffset.dx, panOffset.dy);
    // canvas.scale(scale);

    final matrix = vm.Matrix4.identity();
    matrix.translate(panOffset.dx, panOffset.dy);
    matrix.scale(scale);

    // Get the inverse transformation
    final inverseMatrix = vm.Matrix4.copy(matrix);
    inverseMatrix.invert();

    // Apply inverse transformation to screen position
    final vector = vm.Vector4(screenPosition.dx, screenPosition.dy, 0, 1);
    final transformed = inverseMatrix * vector;

    // Validate the transformed values before creating Offset
    final x = transformed.x.isFinite ? transformed.x : 0.0;
    final y = transformed.y.isFinite ? transformed.y : 0.0;

    return Offset(x, y);
  }

  /// Convert graph coordinates to screen coordinates
  Offset graphToScreen(Offset graphPosition) {
    final matrix = vm.Matrix4.identity();
    matrix.translate(panOffset.dx, panOffset.dy);
    matrix.scale(scale);

    final vector = vm.Vector4(graphPosition.dx, graphPosition.dy, 0, 1);
    final transformed = matrix * vector;

    // Validate the transformed values before creating Offset
    final x = transformed.x.isFinite ? transformed.x : 0.0;
    final y = transformed.y.isFinite ? transformed.y : 0.0;

    return Offset(x, y);
  }

  /// Get the visible graph area given a screen size
  Rect getVisibleGraphArea(Size screenSize) {
    final topLeft = screenToGraph(Offset.zero);
    final bottomRight = screenToGraph(
      Offset(screenSize.width, screenSize.height),
    );

    return Rect.fromPoints(topLeft, bottomRight);
  }

  /// Create a new coordinate system with updated values
  CoordinateSystem copyWith({double? scale, Offset? panOffset}) {
    return CoordinateSystem(
      scale: scale ?? this.scale,
      panOffset: panOffset ?? this.panOffset,
    );
  }
}
