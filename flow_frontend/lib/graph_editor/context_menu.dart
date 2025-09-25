import 'package:flutter/material.dart';

enum ContextMenuType { canvas, node, connection }

class ContextMenuItem {
  final String label;
  final IconData? icon;
  final String? shortcut;
  final VoidCallback? onTap;
  final List<ContextMenuItem>? submenu;
  final bool enabled;
  final bool isDivider;
  final Color? color;

  const ContextMenuItem({
    required this.label,
    this.icon,
    this.shortcut,
    this.onTap,
    this.submenu,
    this.enabled = true,
    this.isDivider = false,
    this.color,
  });

  const ContextMenuItem.divider()
      : label = '',
        icon = null,
        shortcut = null,
        onTap = null,
        submenu = null,
        enabled = true,
        isDivider = true,
        color = null;
}

class ContextMenu extends StatefulWidget {
  final List<ContextMenuItem> items;
  final Offset position;
  final VoidCallback onDismiss;

  const ContextMenu({
    super.key,
    required this.items,
    required this.position,
    required this.onDismiss,
  });

  @override
  State<ContextMenu> createState() => _ContextMenuState();
}

class _ContextMenuState extends State<ContextMenu>
    with TickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 150),
      vsync: this,
    );

    _scaleAnimation = Tween<double>(begin: 0.95, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: widget.position.dx,
      top: widget.position.dy,
      child: AnimatedBuilder(
        animation: _animationController,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            alignment: Alignment.topLeft,
            child: Opacity(
              opacity: _fadeAnimation.value,
              child: Material(
                color: Colors.transparent,
                child: Container(
                  constraints: const BoxConstraints(
                    minWidth: 200,
                    maxWidth: 300,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFF2B2B2B),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: const Color(0xFF404040),
                      width: 1,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.3),
                        blurRadius: 10,
                        spreadRadius: 2,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: widget.items
                          .map((item) => _buildMenuItem(item))
                          .toList(),
                    ),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildMenuItem(ContextMenuItem item) {
    if (item.isDivider) {
      return Container(
        height: 1,
        margin: const EdgeInsets.symmetric(vertical: 4),
        color: const Color(0xFF404040),
      );
    }

    return InkWell(
      onTap: item.enabled
          ? () {
              widget.onDismiss();
              item.onTap?.call();
            }
          : null,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            if (item.icon != null) ...[
              Icon(
                item.icon,
                size: 16,
                color: item.enabled
                    ? (item.color ?? Colors.white70)
                    : Colors.grey[600],
              ),
              const SizedBox(width: 8),
            ],
            Expanded(
              child: Text(
                item.label,
                style: TextStyle(
                  color: item.enabled
                      ? (item.color ?? Colors.white70)
                      : Colors.grey[600],
                  fontSize: 13,
                ),
              ),
            ),
            if (item.shortcut != null) ...[
              const SizedBox(width: 8),
              Text(
                item.shortcut!,
                style: TextStyle(color: Colors.grey[500], fontSize: 11),
              ),
            ],
            if (item.submenu != null) ...[
              const SizedBox(width: 4),
              Icon(
                Icons.chevron_right,
                size: 16,
                color: item.enabled ? Colors.white70 : Colors.grey[600],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
