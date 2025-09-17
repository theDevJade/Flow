import 'package:flutter/foundation.dart';
import '../services/file_system_service.dart';
import '../models/open_file.dart';

class FileSystemState with ChangeNotifier {
  final FileSystemService _fileSystemService = FileSystemService.instance;

  FileNode? _fileTree;
  final List<OpenFile> _openFiles = [];
  OpenFile? _activeFile;
  bool _isLoading = false;

  FileNode? get fileTree => _fileTree;
  List<OpenFile> get openFiles => _openFiles;
  OpenFile? get activeFile => _activeFile;
  bool get isLoading => _isLoading;

  FileSystemState() {
    _fileSystemService.fileTreeStream.listen((rootNode) {
      _fileTree = rootNode;
      notifyListeners();
    });
  }

  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void setFileTree(Map<String, dynamic> treeData) {
    _fileTree = FileNode.fromJson(treeData);
    notifyListeners();
  }

  void openFile(String path, String content) {
    debugPrint(
      '📂 FileSystemState: Opening file: $path (content length: ${content.length})',
    );

    final existingFileIndex = _openFiles.indexWhere(
      (file) => file.path == path,
    );
    if (existingFileIndex == -1) {
      _openFiles.add(OpenFile(path: path, content: content));
      debugPrint(
        '📂 FileSystemState: Added new file to openFiles, now have ${_openFiles.length} files',
      );
    } else {
      // Update content of existing file
      final existingFile = _openFiles[existingFileIndex];
      _openFiles[existingFileIndex] = OpenFile(
        path: existingFile.path,
        content: content,
        isModified: existingFile.isModified,
      );
      debugPrint(
        '📂 FileSystemState: Updated content of existing file (was ${existingFile.content.length}, now ${content.length} chars)',
      );
    }

    _activeFile = _openFiles.firstWhere((file) => file.path == path);
    debugPrint('📂 FileSystemState: Set active file: ${_activeFile?.path}');

    notifyListeners();
    debugPrint('📂 FileSystemState: notifyListeners() called');
  }

  void closeFile(String path) {
    final fileIndex = _openFiles.indexWhere((file) => file.path == path);
    if (fileIndex != -1) {
      _openFiles.removeAt(fileIndex);
      if (_activeFile?.path == path) {
        _activeFile = _openFiles.isNotEmpty ? _openFiles.last : null;
      }
      notifyListeners();
    }
  }

  void switchToFile(String path) {
    final file = _openFiles.firstWhere((file) => file.path == path);
    _activeFile = file;
    notifyListeners();
  }

  void updateFileContent(String path, String content) {
    final index = _openFiles.indexWhere((file) => file.path == path);
    if (index != -1) {
      final oldFile = _openFiles[index];
      _openFiles[index] = OpenFile(
        path: oldFile.path,
        content: content,
        isModified: true,
      );
      notifyListeners();
    }
  }

  void markFileSaved(String path) {
    final index = _openFiles.indexWhere((file) => file.path == path);
    if (index != -1) {
      final oldFile = _openFiles[index];
      _openFiles[index] = OpenFile(
        path: oldFile.path,
        content: oldFile.content,
        isModified: false,
      );
      notifyListeners();
    }
  }
}
