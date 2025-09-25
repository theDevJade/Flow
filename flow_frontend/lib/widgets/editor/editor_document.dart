class EditorCursor {
  EditorCursor({
    this.line = 0,
    this.column = 0,
    this.anchorLine = 0,
    this.anchorColumn = 0,
  });

  int line = 0;
  int column = 0;
  int anchorLine = 0;
  int anchorColumn = 0;

  EditorCursor copy() {
    return EditorCursor(
      line: line,
      column: column,
      anchorLine: anchorLine,
      anchorColumn: anchorColumn,
    );
  }

  EditorCursor normalized() {
    EditorCursor res = copy();
    if (line > anchorLine || (line == anchorLine && column > anchorColumn)) {
      res.line = anchorLine;
      res.column = anchorColumn;
      res.anchorLine = line;
      res.anchorColumn = column;
      return res;
    }
    return res;
  }

  bool hasSelection() {
    return line != anchorLine || column != anchorColumn;
  }
}

/// A document model that handles text editing operations
class EditorDocument {
  String docPath = '';
  List<String> lines = <String>[''];
  EditorCursor cursor = EditorCursor();
  String clipboardText = '';

  /// Initialize document with content
  void setContent(String content) {
    lines = content.isEmpty ? [''] : content.split('\n');
    moveCursorToStartOfDocument();
  }

  /// Get the full document content as a string
  String getContent() {
    return lines.join('\n');
  }

  /// Save content to the provided callback
  void saveContent(Function(String) onSave) {
    onSave(getContent());
  }

  void _validateCursor(bool keepAnchor) {
    if (cursor.line >= lines.length) {
      cursor.line = lines.length - 1;
    }
    if (cursor.line < 0) cursor.line = 0;
    if (cursor.column > lines[cursor.line].length) {
      cursor.column = lines[cursor.line].length;
    }
    if (cursor.column == -1) cursor.column = lines[cursor.line].length;
    if (cursor.column < 0) cursor.column = 0;
    if (!keepAnchor) {
      cursor.anchorLine = cursor.line;
      cursor.anchorColumn = cursor.column;
    }
  }

  void moveCursor(int line, int column, {bool keepAnchor = false}) {
    cursor.line = line;
    cursor.column = column;
    _validateCursor(keepAnchor);
  }

  void moveCursorLeft({int count = 1, bool keepAnchor = false}) {
    cursor.column = cursor.column - count;
    if (cursor.column < 0) {
      moveCursorUp(keepAnchor: keepAnchor);
      moveCursorToEndOfLine(keepAnchor: keepAnchor);
    }
    _validateCursor(keepAnchor);
  }

  void moveCursorRight({int count = 1, bool keepAnchor = false}) {
    cursor.column = cursor.column + count;
    if (cursor.column > lines[cursor.line].length) {
      moveCursorDown(keepAnchor: keepAnchor);
      moveCursorToStartOfLine(keepAnchor: keepAnchor);
    }
    _validateCursor(keepAnchor);
  }

  void moveCursorUp({int count = 1, bool keepAnchor = false}) {
    cursor.line = cursor.line - count;
    _validateCursor(keepAnchor);
  }

  void moveCursorDown({int count = 1, bool keepAnchor = false}) {
    cursor.line = cursor.line + count;
    _validateCursor(keepAnchor);
  }

  void moveCursorToStartOfLine({bool keepAnchor = false}) {
    cursor.column = 0;
    _validateCursor(keepAnchor);
  }

  void moveCursorToEndOfLine({bool keepAnchor = false}) {
    cursor.column = lines[cursor.line].length;
    _validateCursor(keepAnchor);
  }

  void moveCursorToStartOfDocument({bool keepAnchor = false}) {
    cursor.line = 0;
    cursor.column = 0;
    _validateCursor(keepAnchor);
  }

  void moveCursorToEndOfDocument({bool keepAnchor = false}) {
    cursor.line = lines.length - 1;
    cursor.column = lines[cursor.line].length;
    _validateCursor(keepAnchor);
  }

  void insertNewLine() {
    deleteSelectedText();
    insertText('\n');
  }

  void insertText(String text) {
    deleteSelectedText();
    String l = lines[cursor.line];
    String left = l.substring(0, cursor.column);
    String right = l.substring(cursor.column);

    // Handle new line
    if (text == '\n') {
      lines[cursor.line] = left;
      lines.insert(cursor.line + 1, right);
      moveCursorDown();
      moveCursorToStartOfLine();
      return;
    }

    lines[cursor.line] = left + text + right;
    moveCursorRight(count: text.length);
  }

  void deleteText({int numberOfCharacters = 1}) {
    String l = lines[cursor.line];

    // Handle join lines
    if (cursor.column >= l.length) {
      EditorCursor cur = cursor.copy();
      lines[cursor.line] += lines[cursor.line + 1];
      moveCursorDown();
      deleteLine();
      cursor = cur;
      return;
    }

    EditorCursor cur = cursor.normalized();
    String left = l.substring(0, cur.column);
    String right = l.substring(cur.column + numberOfCharacters);
    cursor = cur;

    // Handle erase entire line
    if (lines.length > 1 && (left + right).isEmpty) {
      lines.removeAt(cur.line);
      moveCursorUp();
      moveCursorToStartOfLine();
      return;
    }

    lines[cursor.line] = left + right;
  }

  void deleteLine({int numberOfLines = 1}) {
    for (int i = 0; i < numberOfLines; i++) {
      moveCursorToStartOfLine();
      deleteText(numberOfCharacters: lines[cursor.line].length);
    }
    _validateCursor(false);
  }

  List<String> selectedLines() {
    List<String> res = <String>[];
    EditorCursor cur = cursor.normalized();
    if (cur.line == cur.anchorLine) {
      String sel = lines[cur.line].substring(cur.column, cur.anchorColumn);
      res.add(sel);
      return res;
    }

    res.add(lines[cur.line].substring(cur.column));
    for (int i = cur.line + 1; i < cur.anchorLine; i++) {
      res.add(lines[i]);
    }
    res.add(lines[cur.anchorLine].substring(0, cur.anchorColumn));
    return res;
  }

  String selectedText() {
    return selectedLines().join('\n');
  }

  void deleteSelectedText() {
    if (!cursor.hasSelection()) {
      return;
    }

    EditorCursor cur = cursor.normalized();
    List<String> res = selectedLines();
    if (res.length == 1) {
      deleteText(numberOfCharacters: cur.anchorColumn - cur.column);
      clearSelection();
      return;
    }

    String l = lines[cur.line];
    String left = l.substring(0, cur.column);
    l = lines[cur.anchorLine];
    String right = l.substring(cur.anchorColumn);

    cursor = cur;
    lines[cur.line] = left + right;
    lines[cur.anchorLine] = lines[cur.anchorLine].substring(cur.anchorColumn);
    for (int i = 0; i < res.length - 1; i++) {
      lines.removeAt(cur.line + 1);
    }
    _validateCursor(false);
  }

  void clearSelection() {
    cursor.anchorLine = cursor.line;
    cursor.anchorColumn = cursor.column;
  }

  void selectAll() {
    moveCursorToStartOfDocument();
    moveCursorToEndOfDocument(keepAnchor: true);
  }

  void command(String cmd) {
    switch (cmd) {
      case 'ctrl+c':
      case 'cmd+c':
        clipboardText = selectedText();
        break;
      case 'ctrl+x':
      case 'cmd+x':
        clipboardText = selectedText();
        deleteSelectedText();
        break;
      case 'ctrl+v':
      case 'cmd+v':
        insertText(clipboardText);
        break;
      case 'ctrl+a':
      case 'cmd+a':
        selectAll();
        break;
    }
  }
}
