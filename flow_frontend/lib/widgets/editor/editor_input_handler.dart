import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';
import 'editor_document.dart';
import 'editor_syntax_highlighter.dart';

class EditorKeyResult {
  final bool handled;
  final bool contentChanged;

  const EditorKeyResult(this.handled, this.contentChanged);

  static const EditorKeyResult ignored = EditorKeyResult(false, false);
  static const EditorKeyResult handledContentChanged = EditorKeyResult(
    true,
    true,
  );
  static const EditorKeyResult handledVisualOnly = EditorKeyResult(true, false);
}

Offset screenToCursor(RenderObject? obj, Offset pos) {
  List<RenderParagraph> pars = <RenderParagraph>[];
  findRenderParagraphs(obj, pars);

  RenderParagraph? targetPar;
  int line = -1;

  for (final par in pars) {
    Rect bounds = const Offset(0, 0) & par.size;
    Offset offsetForCaret = par.localToGlobal(
      par.getOffsetForCaret(const TextPosition(offset: 0), bounds),
    );
    Rect parBounds =
        offsetForCaret & Size(par.size.width * 10, par.size.height);
    if (parBounds.inflate(2).contains(pos)) {
      targetPar = par;
      break;
    }
  }

  if (targetPar == null) return const Offset(-1, -1);

  Rect bounds = const Offset(0, 0) & targetPar.size;
  List<InlineSpan> children =
      (targetPar.text as TextSpan).children ?? <InlineSpan>[];
  Size fontCharSize = const Size(0, 0);
  int textOffset = 0;
  bool found = false;

  for (var span in children) {
    if (found) break;
    if (span is! TextSpan) {
      continue;
    }

    if (fontCharSize.width == 0) {
      fontCharSize = getTextExtents(' ', span.style ?? const TextStyle());
    }

    String txt = (span).text ?? '';
    for (int i = 0; i < txt.length; i++) {
      Offset offsetForCaret = targetPar.localToGlobal(
        targetPar.getOffsetForCaret(TextPosition(offset: textOffset), bounds),
      );
      Rect charBounds = offsetForCaret & fontCharSize;
      if (charBounds.inflate(2).contains(Offset(pos.dx + 1, pos.dy + 1))) {
        found = true;
        break;
      }
      textOffset++;
    }
  }

  if (children.isNotEmpty && children.last is CustomWidgetSpan) {
    line = (children.last as CustomWidgetSpan).line;
  }

  return Offset(textOffset.toDouble(), line.toDouble());
}

void findRenderParagraphs(RenderObject? obj, List<RenderParagraph> res) {
  if (obj is RenderParagraph) {
    res.add(obj);
    return;
  }
  obj?.visitChildren((child) {
    findRenderParagraphs(child, res);
  });
}

class EditorInputHandler extends StatefulWidget {
  final Widget child;
  final EditorDocument document;
  final VoidCallback? onChanged;
  final VoidCallback?
  onVisualUpdate; // For cursor/selection changes without content changes
  final Function(String)? onSave;

  const EditorInputHandler({
    required this.child,
    required this.document,
    this.onChanged,
    this.onVisualUpdate,
    this.onSave,
    super.key,
  });

  @override
  State<EditorInputHandler> createState() => _EditorInputHandlerState();
}

class _EditorInputHandlerState extends State<EditorInputHandler> {
  late FocusNode focusNode;

  @override
  void initState() {
    super.initState();
    focusNode = FocusNode();
  }

  @override
  void dispose() {
    focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!focusNode.hasFocus) {
      focusNode.requestFocus();
    }

    EditorDocument d = widget.document;

    return GestureDetector(
      child: Focus(
        child: widget.child,
        focusNode: focusNode,
        autofocus: true,
        onKeyEvent: (FocusNode node, KeyEvent event) {
          if (event is KeyDownEvent) {
            var result = _handleKeyEvent(event, d);
            if (result.handled) {
              if (result.contentChanged) {
                widget.onChanged?.call();
              } else {
                widget.onVisualUpdate?.call();
              }
              return KeyEventResult.handled;
            }
          }
          return KeyEventResult.ignored;
        },
      ),
      onTapDown: (TapDownDetails details) {
        Offset o = screenToCursor(
          context.findRenderObject(),
          details.globalPosition,
        );
        d.moveCursor(o.dy.toInt(), o.dx.toInt());
        // Only trigger visual update for cursor movement, not content change
        widget.onVisualUpdate?.call();
      },
      onPanUpdate: (DragUpdateDetails details) {
        Offset o = screenToCursor(
          context.findRenderObject(),
          details.globalPosition,
        );
        if (o.dx == -1 || o.dy == -1) return;
        d.moveCursor(o.dy.toInt(), o.dx.toInt(), keepAnchor: true);
        // Only trigger visual update for selection, not content change
        widget.onVisualUpdate?.call();
      },
    );
  }

  EditorKeyResult _handleKeyEvent(KeyEvent event, EditorDocument d) {
    final isControlPressed = HardwareKeyboard.instance.isControlPressed;
    final isMetaPressed = HardwareKeyboard.instance.isMetaPressed;
    final isShiftPressed = HardwareKeyboard.instance.isShiftPressed;
    final isCmdOrCtrl = isControlPressed || isMetaPressed;

    switch (event.logicalKey) {
      case LogicalKeyboardKey.home:
        if (isCmdOrCtrl) {
          d.moveCursorToStartOfDocument(keepAnchor: isShiftPressed);
        } else {
          d.moveCursorToStartOfLine(keepAnchor: isShiftPressed);
        }
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.end:
        if (isCmdOrCtrl) {
          d.moveCursorToEndOfDocument(keepAnchor: isShiftPressed);
        } else {
          d.moveCursorToEndOfLine(keepAnchor: isShiftPressed);
        }
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.tab:
        d.insertText('    ');
        return EditorKeyResult.handledContentChanged;

      case LogicalKeyboardKey.enter:
        d.insertNewLine();
        return EditorKeyResult.handledContentChanged;

      case LogicalKeyboardKey.backspace:
        if (d.cursor.hasSelection()) {
          d.deleteSelectedText();
        } else {
          d.moveCursorLeft();
          d.deleteText();
        }
        return EditorKeyResult.handledContentChanged;

      case LogicalKeyboardKey.delete:
        if (d.cursor.hasSelection()) {
          d.deleteSelectedText();
        } else {
          d.deleteText();
        }
        return EditorKeyResult.handledContentChanged;

      case LogicalKeyboardKey.arrowLeft:
        d.moveCursorLeft(keepAnchor: isShiftPressed);
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.arrowRight:
        d.moveCursorRight(keepAnchor: isShiftPressed);
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.arrowUp:
        d.moveCursorUp(keepAnchor: isShiftPressed);
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.arrowDown:
        d.moveCursorDown(keepAnchor: isShiftPressed);
        return EditorKeyResult.handledVisualOnly;

      case LogicalKeyboardKey.keyS:
        if (isCmdOrCtrl) {
          widget.onSave?.call(d.getContent());
          return EditorKeyResult.handledVisualOnly;
        }
        break;

      case LogicalKeyboardKey.keyA:
        if (isCmdOrCtrl) {
          d.command(isMetaPressed ? 'cmd+a' : 'ctrl+a');
          return EditorKeyResult.handledVisualOnly;
        }
        break;

      case LogicalKeyboardKey.keyC:
        if (isCmdOrCtrl) {
          d.command(isMetaPressed ? 'cmd+c' : 'ctrl+c');
          return EditorKeyResult.handledVisualOnly;
        }
        break;

      case LogicalKeyboardKey.keyX:
        if (isCmdOrCtrl) {
          d.command(isMetaPressed ? 'cmd+x' : 'ctrl+x');
          return EditorKeyResult.handledContentChanged;
        }
        break;

      case LogicalKeyboardKey.keyV:
        if (isCmdOrCtrl) {
          d.command(isMetaPressed ? 'cmd+v' : 'ctrl+v');
          return EditorKeyResult.handledContentChanged;
        }
        break;
    }

    // Handle regular character input
    final character = event.character;
    if (character != null && character.length == 1 && !isCmdOrCtrl) {
      d.insertText(character);
      return EditorKeyResult.handledContentChanged;
    }

    return EditorKeyResult.ignored;
  }
}
