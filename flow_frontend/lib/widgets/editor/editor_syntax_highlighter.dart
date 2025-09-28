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
  String currentLanguage = 'plaintext';

  EditorSyntaxHighlighter() {
    _initializeColorMap();
  }

  void setLanguage(String language) {
    currentLanguage = language.toLowerCase();
    _initializeColorMap();
  }

  void _initializeColorMap() {
    colorMap.clear();

    switch (currentLanguage) {
      case 'flowlang':
        _setupFlowLangHighlighting();
        break;
      case 'dart':
        _setupDartHighlighting();
        break;
      case 'kotlin':
        _setupKotlinHighlighting();
        break;
      case 'javascript':
      case 'js':
        _setupJavaScriptHighlighting();
        break;
      case 'python':
        _setupPythonHighlighting();
        break;
      case 'java':
        _setupJavaHighlighting();
        break;
      default:
        _setupGenericHighlighting();
    }
  }

  void _setupFlowLangHighlighting() {
    // FlowLang keywords - control flow
    colorMap[r'\b(if|else|while|for|do|break|continue|return)\b'] = keyword;
    
    // FlowLang keywords - declarations
    colorMap[r'\b(function|var|event|on|trigger|class|extends|new|this|super)\b'] = keyword;
    
    // FlowLang keywords - access modifiers
    colorMap[r'\b(public|private|protected)\b'] = keyword;
    
    // FlowLang keywords - logical operators
    colorMap[r'\b(and|or|not)\b'] = keyword;
    
    // FlowLang keywords - literals
    colorMap[r'\b(true|false|null)\b'] = keyword;
    
    // FlowLang types
    colorMap[r'\b(String|Number|Boolean|Object|List|Array)\b'] = function;
    
    // Comments (lines starting with #)
    colorMap[r'^#.*$'] = comment;
    colorMap[r'#.*$'] = comment; // Also match # comments anywhere on line
    
    // String literals - double quotes
    colorMap[r'"[^"\\]*(\\.[^"\\]*)*"'] = string;
    colorMap[r'"[^"]*"'] = string; // Fallback for simple strings
    
    // String literals - single quotes
    colorMap[r"'[^'\\]*(\\.[^'\\]*)*'"] = string;
    colorMap[r"'[^']*'"] = string; // Fallback for simple strings
    
    // Numbers - integers and decimals
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffbd93f9); // Purple for numbers
    
    // Operators - arithmetic
    colorMap[r'[+\-*/%]'] = const Color(0xff50fa7b); // Green for arithmetic operators
    
    // Operators - comparison
    colorMap[r'[=<>!]=?'] = const Color(0xff50fa7b); // Green for comparison operators
    
    // Operators - logical
    colorMap[r'[&|]'] = const Color(0xff50fa7b); // Green for logical operators
    
    // Assignment operator
    colorMap[r'='] = const Color(0xff50fa7b); // Green for assignment
    
    // Function calls - identifier followed by (
    colorMap[r'\b\w+(?=\()'] = function;
    
    // Built-in functions
    colorMap[r'\b(print|println|input|length|toString|parseInt|parseFloat|parseBoolean)\b'] = function;
    
    // Function definitions - function keyword followed by identifier
    colorMap[r'\bfunction\s+(\w+)'] = function;
    
    // Class definitions - class keyword followed by identifier
    colorMap[r'\bclass\s+(\w+)'] = function;
    
    // Event handlers - on keyword followed by identifier
    colorMap[r'\bon\s+(\w+)'] = function;
    
    // Brackets and parentheses
    colorMap[r'[{}()\[\]]'] = const Color(0xfff8f8f2); // Foreground color for brackets
    
    // Semicolons and commas
    colorMap[r'[;,.]'] = const Color(0xff88846f); // Comment color for punctuation
  }

  void _setupDartHighlighting() {
    // Dart keywords
    colorMap[r'\b(abstract|as|assert|async|await|break|case|catch|class|const|continue|covariant|default|deferred|do|dynamic|else|enum|export|extends|extension|external|factory|final|finally|for|Function|get|hide|if|implements|import|in|interface|is|late|library|mixin|new|on|operator|part|required|rethrow|return|set|show|static|super|switch|sync|this|throw|try|typedef|var|void|while|with|yield)\b'] = keyword;
    
    // Strings
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'`[^`]*`'] = string;
    
    // Comments
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    
    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  void _setupKotlinHighlighting() {
    // Kotlin keywords
    colorMap[r'\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|try|typealias|typeof|val|var|vararg|when|where|while)\b'] = keyword;
    
    // Strings
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'"""[\s\S]*?"""'] = string;
    
    // Comments
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    
    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  void _setupJavaScriptHighlighting() {
    // JavaScript keywords
    colorMap[r'\b(break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|true|false|null|undefined)\b'] = keyword;
    
    // Strings
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'`[^`]*`'] = string;
    
    // Comments
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    
    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  void _setupPythonHighlighting() {
    // Python keywords
    colorMap[r'\b(and|as|assert|break|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|not|or|pass|print|raise|return|try|while|with|yield|True|False|None)\b'] = keyword;
    
    // Strings
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'"""[^"]*"""'] = string;
    colorMap[r"'''[^']*'''"] = string;
    
    // Comments
    colorMap[r'#.*$'] = comment;
    
    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  void _setupJavaHighlighting() {
    // Java keywords
    colorMap[r'\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null)\b'] = keyword;
    
    // Strings
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    
    // Comments
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    
    // Numbers
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  void _setupGenericHighlighting() {
    // Generic highlighting for unknown languages
    colorMap[r'\b(class|struct|function|def|func)\b'] = function;
    colorMap[r'"[^"]*"'] = string;
    colorMap[r"'[^']*'"] = string;
    colorMap[r'//.*$'] = comment;
    colorMap[r'/\*.*?\*/'] = comment;
    colorMap[r'#.*$'] = comment;
    colorMap[r'\b(if|else|while|for|do|switch|case|default|break|continue|return|try|catch|finally|throw|import|export|from|as|with|in|is|not|and|or|const|var|let|final|static|public|private|protected|abstract|override|virtual|async|await|yield|true|false|null|undefined|void|int|double|float|bool|string|char|long|short)\b'] = keyword;
    colorMap[r'\b\d+\.?\d*\b'] = const Color(0xffae81ff);
  }

  List<InlineSpan> run(String text, int line, EditorDocument document) {
    try {
      TextStyle defaultStyle = TextStyle(
        fontFamily: 'monospace',
        fontSize: fontSize,
        color: foreground,
        height: 1.4,
      );

      List<InlineSpan> res = <InlineSpan>[];
      List<LineDecoration> decors = <LineDecoration>[];

      // Skip syntax highlighting for very long lines to prevent performance issues
      if (text.length > 1000) {
        res.add(TextSpan(text: text, style: defaultStyle));
        return res;
      }

      // Apply syntax highlighting with timeout protection
      for (var exp in colorMap.keys) {
        try {
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
        } catch (e) {
          // Skip problematic regex patterns
          continue;
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
    } catch (e) {
      // Fallback to simple text rendering if syntax highlighting fails
      return [
        TextSpan(
          text: text,
          style: TextStyle(
            fontFamily: 'monospace',
            fontSize: fontSize,
            color: foreground,
            height: 1.4,
          ),
        ),
      ];
    }
  }
}
