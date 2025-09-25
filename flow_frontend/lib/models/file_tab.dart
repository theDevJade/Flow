class FileTab {
  String path;
  String content;
  bool isModified;

  FileTab({required this.path, this.content = '', this.isModified = false});
}
