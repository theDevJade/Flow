import 'dart:async';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as path;
import 'websocket_service.dart';

class FileNode {
  final String name;
  final String fullPath;
  final bool isDirectory;
  final List<FileNode> children;
  final DateTime lastModified;
  final int size;

  FileNode({
    required this.name,
    required this.fullPath,
    required this.isDirectory,
    this.children = const [],
    required this.lastModified,
    this.size = 0,
  });

  factory FileNode.fromJson(Map<String, dynamic> json) {
    var childrenList = json['children'] as List? ?? [];
    List<FileNode> children = childrenList
        .map((i) => FileNode.fromJson(i))
        .toList();

    // Handle lastModified - it might be null, empty string, or a valid date
    DateTime lastModified;
    final lastModifiedValue = json['lastModified'];
    if (lastModifiedValue == null || lastModifiedValue.toString().isEmpty) {
      lastModified = DateTime.now();
    } else {
      try {
        lastModified = DateTime.parse(lastModifiedValue.toString());
      } catch (e) {
        lastModified = DateTime.now();
      }
    }

    return FileNode(
      name: json['name'] ?? '',
      fullPath: json['fullPath'] ?? '',
      isDirectory: json['isDirectory'] ?? false,
      children: children,
      lastModified: lastModified,
      size: json['size'] ?? 0,
    );
  }
}

class FileSystemService {
  static FileSystemService? _instance;
  static FileSystemService get instance => _instance ??= FileSystemService._();

  final WebSocketService _webSocketService = WebSocketService.instance;

  FileSystemService._() {
    _webSocketService.messages.listen((message) {
      if (message.type == 'file_tree') {
        if (message.data['success'] == true) {
          // The file tree data is nested in the message.data field
          final fileTreeData = Map<String, dynamic>.from(message.data);
          final rootNode = FileNode.fromJson(fileTreeData);
          _fileTreeController.add(rootNode);
        } else {
          debugPrint(
            'FileSystemService: Failed to get file tree: ${message.data['error']}',
          );
        }
      } else if (message.type == 'file_content') {
        final requestId = message.data['requestId'] as String?;
        if (requestId != null && _fileReadCompleters.containsKey(requestId)) {
          final content = message.data['content'] as String?;
          _fileReadCompleters[requestId]!.complete(content);
          _fileReadCompleters.remove(requestId);
        }
      }
    });
  }

  final StreamController<FileNode> _fileTreeController =
      StreamController<FileNode>.broadcast();
  final StreamController<List<String>> _openFilesController =
      StreamController<List<String>>.broadcast();
  final Map<String, Completer<String>> _fileReadCompleters = {};

  Stream<FileNode> get fileTreeStream => _fileTreeController.stream;
  Stream<List<String>> get openFilesStream => _openFilesController.stream;

  List<String> _openFiles = [];
  String? _activeFile;

  String get rootPath => ''; // This will be provided by the backend
  List<String> get openFiles => List.unmodifiable(_openFiles);
  String? get activeFile => _activeFile;

  Future<void> initialize() {
    // FileSystemService is ready to handle messages
    // File tree will be requested by AppState after WebSocket connection
    debugPrint(
      'FileSystemService: Initialized and ready to handle file operations',
    );
    return Future.value();
  }

  /// Request file tree from server
  void requestFileTree() {
    _webSocketService.sendMessage('get_file_tree', {});
  }

  Future<String?> readFile(String filePath) {
    final completer = Completer<String>();
    final requestId = 'read_${DateTime.now().millisecondsSinceEpoch}';
    _fileReadCompleters[requestId] = completer;
    _webSocketService.sendMessage('read_file', {
      'path': filePath,
      'requestId': requestId,
    });
    return completer.future;
  }

  Future<bool> writeFile(String filePath, String content) async {
    _webSocketService.sendMessage('write_file', {
      'path': filePath,
      'content': content,
    });
    return true;
  }

  Future<bool> createFile(String dirPath, String fileName) async {
    _webSocketService.sendMessage('create_file', {
      'dirPath': dirPath,
      'fileName': fileName,
    });
    return true;
  }

  Future<bool> createDirectory(String parentPath, String dirName) async {
    _webSocketService.sendMessage('create_directory', {
      'parentPath': parentPath,
      'dirName': dirName,
    });
    return true;
  }

  Future<bool> deleteFile(String filePath) async {
    _webSocketService.sendMessage('delete_file', {'path': filePath});
    _closeFile(filePath);
    return true;
  }

  Future<bool> deleteDirectory(String dirPath) async {
    _webSocketService.sendMessage('delete_directory', {'path': dirPath});
    _openFiles.removeWhere((file) => file.startsWith(dirPath));
    _openFilesController.add(_openFiles);
    return true;
  }

  void openFile(String filePath) {
    if (!_openFiles.contains(filePath)) {
      _openFiles.add(filePath);
      _openFilesController.add(_openFiles);
    }
    _activeFile = filePath;
  }

  void _closeFile(String filePath) {
    _openFiles.remove(filePath);
    if (_activeFile == filePath) {
      _activeFile = _openFiles.isNotEmpty ? _openFiles.last : null;
    }
    _openFilesController.add(_openFiles);
  }

  void closeFile(String filePath) {
    _closeFile(filePath);
  }

  void setActiveFile(String filePath) {
    if (_openFiles.contains(filePath)) {
      _activeFile = filePath;
    }
  }

  String getFileExtension(String filePath) {
    return path.extension(filePath).toLowerCase();
  }

  IconData getFileIcon(String filePath) {
    final extension = getFileExtension(filePath);
    switch (extension) {
      case '.dart':
        return Icons.code;
      case '.json':
        return Icons.data_object;
      case '.yaml':
      case '.yml':
        return Icons.settings;
      case '.md':
        return Icons.article;
      case '.txt':
        return Icons.description;
      case '.html':
        return Icons.web;
      case '.css':
        return Icons.style;
      case '.js':
        return Icons.javascript;
      default:
        return Icons.insert_drive_file;
    }
  }

  void dispose() {
    _fileTreeController.close();
    _openFilesController.close();
  }
}
