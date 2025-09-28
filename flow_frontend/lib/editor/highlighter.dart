import 'package:flutter/material.dart';
import 'dart:collection';
import 'dart:ui' as ui;

import 'document.dart';

double fontSize = 18;
double gutterFontSize = 16;

Size getTextExtents(String text, TextStyle style) {
  final TextPainter textPainter = TextPainter(
      text: TextSpan(text: text, style: style),
      maxLines: 1,
      textDirection: TextDirection.ltr)
    ..layout(minWidth: 0, maxWidth: double.infinity);
  return textPainter.size;
}

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

class Highlighter {
  HashMap<String, Color> colorMap = HashMap<String, Color>();
  String currentLanguage = 'cpp'; // Default language

  Highlighter() {
    _setupCppHighlighting();
  }

  void setLanguage(String language) {
    currentLanguage = language;
    colorMap.clear();

    switch (language.toLowerCase()) {
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
      default:
        _setupCppHighlighting();
    }
  }

  void _setupFlowLangHighlighting() {

    colorMap['\\b(if|else|while|for|do|break|continue|return)\\b'] = keyword;


    colorMap['\\b(function|var|event|on|trigger|class|extends|new|this|super)\\b'] = keyword;


    colorMap['\\b(public|private|protected)\\b'] = keyword;


    colorMap['\\b(and|or|not)\\b'] = keyword;


    colorMap['\\b(true|false|null)\\b'] = keyword;


    colorMap['\\b(String|Number|Boolean|Object|List|Array)\\b'] = function;


    colorMap['^#.*'] = comment;
    colorMap['#.*'] = comment;


    colorMap['"[^"\\\\]*(\\\\.[^"\\\\]*)*"'] = string;
    colorMap['"[^"]*"'] = string;


    colorMap["'[^'\\\\]*(\\\\.[^'\\\\]*)*'"] = string;
    colorMap["'[^']*'"] = string;


    colorMap['\\b\\d+\\.?\\d*\\b'] = const Color(0xffbd93f9);


    colorMap['[+\\-*/%]'] = const Color(0xff50fa7b);


    colorMap['[=<>!]=?'] = const Color(0xff50fa7b);


    colorMap['[&|]'] = const Color(0xff50fa7b);


    colorMap['='] = const Color(0xff50fa7b);


    colorMap['\\b\\w+(?=\\()'] = function;


    colorMap['\\b(print|println|input|length|toString|parseInt|parseFloat|parseBoolean)\\b'] = function;


    colorMap['\\bfunction\\s+(\\w+)'] = function;


    colorMap['\\bclass\\s+(\\w+)'] = function;


    colorMap['\\bon\\s+(\\w+)'] = function;

    // Brackets and parentheses
    colorMap['[{}()\\[\\]]'] = const Color(0xfff8f8f2); // Foreground color for brackets

    // Semicolons and commas
    colorMap['[;,.]'] = const Color(0xff88846f); // Comment color for punctuation
  }

  void _setupCppHighlighting() {
    colorMap['\\b(class|struct)\\b'] = function;
    colorMap['("|<){1}\\b(.*)\\b("|>){1}'] = string;

    colorMap[
            '\\b(if|else|elif|endif|define|undef|warning|error|line|pragma|_Pragma|ifdef|ifndef|include)\\b'] =
        function;
    colorMap[
            '\\b(keyword|int|float|while|private|char|char8_t|char16_t|char32_t|catch|import|module|export|virtual|operator|sizeof|dynamic_cast|10|typedef|const_cast|10|const|for|static_cast|10|union|namespace|unsigned|long|volatile|static|protected|bool|template|mutable|if|public|friend|do|goto|auto|void|enum|else|break|extern|using|asm|case|typeid|wchar_tshort|reinterpret_cast|10|default|double|register|explicit|signed|typename|try|this|switch|continue|inline|delete|alignas|alignof|constexpr|consteval|constinit|decltype|concept|co_await|co_return|co_yield|requires|noexcept|static_assert|thread_local|restrict|final|override|atomic_bool|atomic_char|atomic_schar|atomic_uchar|atomic_short|atomic_ushort|atomic_int|atomic_uint|atomic_long|atomic_ulong|atomic_llong|atomic_ullong|new|throw|return|and|and_eq|bitand|bitor|compl|not|not_eq|or|or_eq|xor|xor_eq)\\b'] =
        keyword;
  }

  void _setupDartHighlighting() {
    colorMap['\\b(abstract|as|assert|async|await|break|case|catch|class|const|continue|covariant|default|deferred|do|dynamic|else|enum|export|extends|extension|external|factory|final|finally|for|Function|get|hide|if|implements|import|in|interface|is|late|library|mixin|new|on|operator|part|required|rethrow|return|set|show|static|super|switch|sync|this|throw|try|typedef|var|void|while|with|yield)\\b'] = keyword;
    colorMap['"[^"]*"'] = string;
    colorMap["'[^']*'"] = string;
    colorMap['//.*'] = comment;
    colorMap['/\\*[\\s\\S]*?\\*/'] = comment;
  }

  void _setupKotlinHighlighting() {
    colorMap['\\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|try|typealias|typeof|val|var|vararg|when|where|while)\\b'] = keyword;
    colorMap['"[^"]*"'] = string;
    colorMap["'[^']*'"] = string;
    colorMap['//.*'] = comment;
    colorMap['/\\*[\\s\\S]*?\\*/'] = comment;
  }

  void _setupJavaScriptHighlighting() {
    colorMap['\\b(break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|true|false|null|undefined)\\b'] = keyword;
    colorMap['"[^"]*"'] = string;
    colorMap["'[^']*'"] = string;
    colorMap['`[^`]*`'] = string;
    colorMap['//.*'] = comment;
    colorMap['/\\*[\\s\\S]*?\\*/'] = comment;
  }

  void _setupPythonHighlighting() {
    colorMap['\\b(and|as|assert|break|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|not|or|pass|print|raise|return|try|while|with|yield|True|False|None)\\b'] = keyword;
    colorMap['"[^"]*"'] = string;
    colorMap["'[^']*'"] = string;
    colorMap['"""[^"]*"""'] = string;
    colorMap["'''[^']*'''"] = string;
    colorMap['#.*'] = comment;
  }

  List<InlineSpan> run(String text, int line, Document document) {
    TextStyle defaultStyle = TextStyle(
        fontFamily: 'FiraCode', fontSize: fontSize, color: foreground);
    List<InlineSpan> res = <InlineSpan>[];
    List<LineDecoration> decors = <LineDecoration>[];

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
      Cursor cur = document.cursor.normalized();


      for (var d in decors) {
        if (i >= d.start && i <= d.end) {
          style = style.copyWith(color: d.color);
        }
      }


      if (cur.hasSelection()) {
        if (line < cur.line ||
            (line == cur.line && i < cur.column) ||
            line > cur.anchorLine ||
            (line == cur.anchorLine && i + 1 > cur.anchorColumn)) {
        } else {
          style = style.copyWith(backgroundColor: selection.withOpacity(0.75));
        }
      }


      if ((line == document.cursor.line && i == document.cursor.column)) {
        res.add(WidgetSpan(
            alignment: ui.PlaceholderAlignment.baseline,
            baseline: TextBaseline.alphabetic,
            child: Container(
                decoration: BoxDecoration(
                    border: Border(
                        left: BorderSide(
                            width: 1.2, color: style.color ?? Colors.yellow))),
                child: Text(ch, style: style.copyWith(letterSpacing: -1.5)))));
        continue;
      }

      if (res.isNotEmpty && res[res.length - 1] is! WidgetSpan) {
        TextSpan prev = res[res.length - 1] as TextSpan;
        if (prev.style == style) {
          prevText += ch;
          res[res.length - 1] = TextSpan(
              text: prevText,
              style: style,
              mouseCursor: MaterialStateMouseCursor.textable);
          continue;
        }
      }

      res.add(TextSpan(
          text: ch,
          style: style,
          mouseCursor: MaterialStateMouseCursor.textable));
      prevText = ch;
    }

    res.add(CustomWidgetSpan(
        child: const SizedBox(height: 1, width: 8), line: line));
    return res;
  }
}
