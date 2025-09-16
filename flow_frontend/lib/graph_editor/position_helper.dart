import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class PositionHelper {
  /// Get the exact screen position and size of a widget using its GlobalKey
  static Rect? getWidgetBounds(GlobalKey key) {
    final RenderObject? renderObject = key.currentContext?.findRenderObject();
    if (renderObject is RenderBox) {
      final Offset position = renderObject.localToGlobal(Offset.zero);
      final Size size = renderObject.size;
      return Rect.fromLTWH(position.dx, position.dy, size.width, size.height);
    }
    return null;
  }

  /// Get the center point of a widget
  static Offset? getWidgetCenter(GlobalKey key) {
    final rect = getWidgetBounds(key);
    return rect?.center;
  }

  /// Check if a point is within a widget's bounds
  static bool isPointInWidget(GlobalKey key, Offset point) {
    final rect = getWidgetBounds(key);
    return rect?.contains(point) ?? false;
  }

  /// Get the local position within a widget from a global position
  static Offset? globalToLocal(GlobalKey key, Offset globalPosition) {
    final RenderObject? renderObject = key.currentContext?.findRenderObject();
    if (renderObject is RenderBox) {
      return renderObject.globalToLocal(globalPosition);
    }
    return null;
  }

  /// Get the global position from a local position within a widget
  static Offset? localToGlobal(GlobalKey key, Offset localPosition) {
    final RenderObject? renderObject = key.currentContext?.findRenderObject();
    if (renderObject is RenderBox) {
      return renderObject.localToGlobal(localPosition);
    }
    return null;
  }
}
