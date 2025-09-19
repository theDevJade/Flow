import 'package:flutter/material.dart';
import 'dart:collection';
import 'dart:ui' as ui;
import 'editor_document.dart';

double fontSize = 14;
double gutterFontSize = 13;

Size getTextExtents(String text, TextStyle style) {
  final TextPainter textPainter = TextPainter(
    text: TextSpan(text: text, style: style),
    maxLines: 1,
    textDirection: TextDirection.ltr,
  )..layout(minWidth: 0, maxWidth: double.infinity);
  return textPainter.size;
}

// Monokai-inspired color scheme
Color foreground = const Color(0xfff8f8f2);
Color background = const Color(0xff272822);
Color comment = const Color(0xff88846f);
Color selection = const Color(0xff44475a);
Color function = const Color(0xff50fa7b);
Color keyword = const Color(0xffff79c6);
Color string = Colors.yellow;

class LineDecoration {
  int start = 0;
  int end = 0;
  Color color = Colors.white;
  Color background = Colors.white;
  bool underline = false;
  bool italic = false;
}

class CustomWidgetSpan extends WidgetSpan {
  final int line;
  const CustomWidgetSpan({required Widget child, this.line = 0})
    : super(child: child);
}

class EditorSyntaxHighlighter {
  HashMap<String, Color> colorMap = HashMap<String, Color>();

  EditorSyntaxHighlighter() {
    _initializeColorMap();
  }

  void _initializeColorMap() {
    colorMap.clear();

    // Functions and classes
    colorMap['\\b(class|struct|function|def|func)\\b'] = function;

    // Strings
    colorMap[r'("|<){1}\b(.*)\b("|>){1}'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'"[^"]*"'] = string;

    // Comments
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    colorMap[r'#.*$'] = comment;

    // Keywords (common across many languages)
    colorMap[r'\b(if|else|elif|endif|while|for|do|switch|case|default|break|continue|return|try|catch|finally|throw|import|export|from|as|with|in|is|not|and|or|const|var|let|final|static|public|private|protected|abstract|override|virtual|async|await|yield|true|false|null|undefined|void|int|double|float|bool|string|char|long|short)\b'] =
        keyword;

    // Dart-specific
    colorMap[r'\b(class|extends|implements|mixin|with|abstract|factory|operator|typedef|enum|library|part|show|hide|deferred|covariant|late|required|external)\b'] =
        keyword;

    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  List<InlineSpan> run(String text, int line, EditorDocument document) {
    TextStyle defaultStyle = TextStyle(
      fontFamily: 'monospace',
      fontSize: fontSize,
      color: foreground,
      height: 1.4,
    );

    List<InlineSpan> res = <InlineSpan>[];
    List<LineDecoration> decors = <LineDecoration>[];

    // Apply syntax highlighting
    for (var exp in colorMap.keys) {
      RegExp regExp = RegExp(exp, caseSensitive: false, multiLine: false);
      var matches = regExp.allMatches(text);
      for (var m in matches) {
        if (m.start == m.end) continue;
        LineDecoration d = LineDecoration();
        d.start = m.start;
        d.end = m.end - 1;
        d.color = colorMap[exp] ?? foreground;
        decors.add(d);
      }
    }

    text += ' ';
    String prevText = '';

    for (int i = 0; i < text.length; i++) {
      String ch = text[i];
      TextStyle style = defaultStyle.copyWith();
      EditorCursor cur = document.cursor.normalized();

      // Apply decorations
      for (var d in decors) {
        if (i >= d.start && i <= d.end) {
          style = style.copyWith(color: d.color);
        }
      }

      // Apply selection highlighting
      if (cur.hasSelection()) {
        if (line < cur.line ||
            (line == cur.line && i < cur.column) ||
            line > cur.anchorLine ||
            (line == cur.anchorLine && i + 1 > cur.anchorColumn)) {
          // Outside selection
        } else {
          style = style.copyWith(backgroundColor: selection.withOpacity(0.75));
        }
      }

      // Show cursor
      if ((line == document.cursor.line && i == document.cursor.column)) {
        res.add(
          WidgetSpan(
            alignment: ui.PlaceholderAlignment.baseline,
            baseline: TextBaseline.alphabetic,
            child: Container(
              decoration: BoxDecoration(
                border: Border(
                  left: BorderSide(
                    width: 1.2,
                    color: style.color ?? Colors.yellow,
                  ),
                ),
              ),
              child: Text(ch, style: style.copyWith(letterSpacing: -1.5)),
            ),
          ),
        );
        continue;
      }

      // Combine adjacent spans with same style
      if (res.isNotEmpty && res[res.length - 1] is! WidgetSpan) {
        TextSpan prev = res[res.length - 1] as TextSpan;
        if (prev.style == style) {
          prevText += ch;
          res[res.length - 1] = TextSpan(
            text: prevText,
            style: style,
            mouseCursor: MaterialStateMouseCursor.textable,
          );
          continue;
        }
      }

      res.add(
        TextSpan(
          text: ch,
          style: style,
          mouseCursor: MaterialStateMouseCursor.textable,
        ),
      );
      prevText = ch;
    }

    res.add(
      CustomWidgetSpan(child: const SizedBox(height: 1, width: 8), line: line),
    );

    return res;
  }
}
