class OpenFile {
  final String path;
  final String content;
  final bool isModified;

  const OpenFile({
    required this.path,
    required this.content,
    this.isModified = false,
  });

  String get fileName => path.split('/').last;

  String get fileExtension {
    final parts = fileName.split('.');
    return parts.length > 1 ? parts.last : '';
  }
}
