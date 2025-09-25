import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

class KeyboardShortcuts {
  // Use correct modifier key based on platform
  static LogicalKeyboardKey get primaryModifier {
    switch (defaultTargetPlatform) {
      case TargetPlatform.macOS:
        return LogicalKeyboardKey.meta; // Cmd key on macOS
      case TargetPlatform.windows:
      case TargetPlatform.linux:
      case TargetPlatform.fuchsia:
      default:
        return LogicalKeyboardKey.control; // Ctrl key on other platforms
    }
  }

  // Platform-specific modifier display with icons
  static String get modifierIcon {
    switch (defaultTargetPlatform) {
      case TargetPlatform.macOS:
        return '⌘'; // Mac Cmd symbol
      case TargetPlatform.windows:
        return 'Ctrl'; // Windows
      case TargetPlatform.linux:
        return 'Ctrl'; // Linux
      default:
        return kIsWeb ? 'Ctrl' : 'Ctrl'; // Web and others
    }
  }

  static String get shiftIcon => '⇧';
  static String get altIcon {
    switch (defaultTargetPlatform) {
      case TargetPlatform.macOS:
        return '⌥'; // Mac Option symbol
      default:
        return 'Alt';
    }
  }

  static Map<String, List<LogicalKeyboardKey>> get shortcuts => {
        'delete': [LogicalKeyboardKey.delete, LogicalKeyboardKey.keyX],
        'deselect': [LogicalKeyboardKey.escape],
        'selectAll': [primaryModifier, LogicalKeyboardKey.keyA],
        'copy': [primaryModifier, LogicalKeyboardKey.keyC],
        'paste': [primaryModifier, LogicalKeyboardKey.keyV],
        'undo': [primaryModifier, LogicalKeyboardKey.keyZ],
        'redo': [primaryModifier, LogicalKeyboardKey.keyY],
        'save': [primaryModifier, LogicalKeyboardKey.keyS],
        'open': [primaryModifier, LogicalKeyboardKey.keyO],
        'duplicate': [LogicalKeyboardKey.shift, LogicalKeyboardKey.keyD],
        'addNode': [LogicalKeyboardKey.shift, LogicalKeyboardKey.keyA],
        'zoomIn': [LogicalKeyboardKey.equal], // Plus key
        'zoomOut': [LogicalKeyboardKey.minus],
        'resetZoom': [LogicalKeyboardKey.keyR],
        'home': [LogicalKeyboardKey.keyH],
        'commandPalette': [primaryModifier, LogicalKeyboardKey.space],
      };

  static bool isPressed(String shortcut, Set<LogicalKeyboardKey> pressed) {
    final keys = shortcuts[shortcut];
    if (keys == null || keys.isEmpty) return false;

    // All keys in the shortcut must be pressed
    return keys.every((key) => pressed.contains(key));
  }

  static String getShortcutDisplay(String shortcut) {
    final keys = shortcuts[shortcut];
    if (keys == null || keys.isEmpty) return '';

    return keys.map((key) {
      if (key == LogicalKeyboardKey.control) return 'Ctrl';
      if (key == LogicalKeyboardKey.shift) return shiftIcon;
      if (key == LogicalKeyboardKey.alt) return altIcon;
      if (key == LogicalKeyboardKey.meta) return '⌘';
      if (key == LogicalKeyboardKey.escape) return 'Esc';
      if (key == LogicalKeyboardKey.space) return 'Space';
      if (key == LogicalKeyboardKey.delete) return 'Del';
      if (key == LogicalKeyboardKey.equal) return '+';
      if (key == LogicalKeyboardKey.minus) return '-';
      if (key == primaryModifier) {
        return modifierIcon;
      }
      return key.keyLabel.toUpperCase();
    }).join(' + ');
  }

  // Get a formatted tooltip with all main shortcuts
  static String getShortcutTooltip() {
    final shortcuts = [
      '${getShortcutDisplay('addNode')}: Add Node',
      '${getShortcutDisplay('delete')}: Delete',
      '${getShortcutDisplay('copy')}: Copy',
      '${getShortcutDisplay('paste')}: Paste',
      '${getShortcutDisplay('duplicate')}: Duplicate',
      '${getShortcutDisplay('save')}: Save',
      '${getShortcutDisplay('selectAll')}: Select All',
      '${getShortcutDisplay('deselect')}: Deselect',
      '${getShortcutDisplay('resetZoom')}: Reset View',
      '${getShortcutDisplay('home')}: Frame All',
      '${getShortcutDisplay('zoomIn')}/${getShortcutDisplay('zoomOut')}: Zoom',
      'Mouse Wheel: Zoom',
    ];

    return shortcuts.join('\n');
  }
}
