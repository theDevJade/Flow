import 'dart:io';

class ProjectStatsService {
  static final ProjectStatsService _instance = ProjectStatsService._internal();
  factory ProjectStatsService() => _instance;
  ProjectStatsService._internal();

  Future<ProjectStats> getProjectStats({String? projectPath}) async {

    projectPath ??= _getProjectRoot();

    int totalFiles = 0;
    int linesOfCode = 0;
    Map<String, int> languageBreakdown = {};
    List<String> recentFiles = [];

    try {
      final directory = Directory(projectPath);
      if (!await directory.exists()) {
        print('Project directory does not exist: $projectPath');
        return ProjectStats(
          totalFiles: 0,
          linesOfCode: 0,
          languageBreakdown: {},
          recentFiles: [],
          projectPath: projectPath,
        );
      }


      final files = await _getAllFiles(directory);
      totalFiles = files.length;

      print('Found $totalFiles files to analyze in $projectPath');


      for (final file in files) {
        final extension = _getFileExtension(file.path);
        final language = _getLanguageFromExtension(extension);

        if (language != null) {
          try {
            final lines = await file.readAsLines();
            final codeLines = lines
                .where(
                  (line) =>
                      line.trim().isNotEmpty &&
                      !_isComment(line.trim(), language),
                )
                .length;

            linesOfCode += codeLines;
            languageBreakdown[language] =
                (languageBreakdown[language] ?? 0) + codeLines;
          } catch (e) {

            continue;
          }
        }
      }


      if (files.isNotEmpty) {
        files.sort(
          (a, b) => b.lastModifiedSync().compareTo(a.lastModifiedSync()),
        );
        recentFiles = files.take(5).map((f) => f.path.split('/').last).toList();
      }

      print('Project stats: $totalFiles files, $linesOfCode lines of code');
    } catch (e) {
      print('Error calculating project stats: $e');
    }

    return ProjectStats(
      totalFiles: totalFiles,
      linesOfCode: linesOfCode,
      languageBreakdown: languageBreakdown,
      recentFiles: recentFiles,
      projectPath: projectPath,
    );
  }

  String _getProjectRoot() {

    String currentPath = Directory.current.path;


    final flowIndicators = [
      'settings.gradle.kts',
      'flow_frontend/',
      'webserver/',
      'flow/',
      'common/',
    ];

    Directory dir = Directory(currentPath);
    while (dir.path != dir.parent.path) {
      bool foundFlowIndicator = false;
      for (final indicator in flowIndicators) {
        final testPath = '${dir.path}/$indicator';
        if (Directory(testPath).existsSync() || File(testPath).existsSync()) {
          foundFlowIndicator = true;
          break;
        }
      }

      if (foundFlowIndicator) {

        final keyDirs = ['flow_frontend', 'webserver', 'flow', 'common'];
        int foundDirs = 0;
        for (final keyDir in keyDirs) {
          if (Directory('${dir.path}/$keyDir').existsSync()) {
            foundDirs++;
          }
        }

        if (foundDirs >= 2) {
          print('Found Flow project root at: ${dir.path}');
          return dir.path;
        }
      }

      dir = dir.parent;
    }


    dir = Directory(currentPath);
    final flutterIndicators = ['pubspec.yaml', 'android/', 'ios/', 'lib/'];

    while (dir.path != dir.parent.path) {
      bool foundIndicator = false;
      for (final indicator in flutterIndicators) {
        final testPath = '${dir.path}/$indicator';
        if (Directory(testPath).existsSync() || File(testPath).existsSync()) {
          foundIndicator = true;
          break;
        }
      }

      if (foundIndicator) {
        print('Found Flutter project root at: ${dir.path}');
        return dir.path;
      }

      dir = dir.parent;
    }


    print('Using fallback project root: $currentPath');
    return currentPath;
  }

  Future<List<File>> _getAllFiles(Directory directory) async {
    final List<File> files = [];

    try {
      await for (final entity in directory.list(recursive: true)) {
        if (entity is File && !_shouldIgnoreFile(entity.path)) {
          files.add(entity);
        }
      }
    } catch (e) {

    }

    return files;
  }

  bool _shouldIgnoreFile(String path) {
    final ignoredPatterns = [
      '.git/',
      'node_modules/',
      '.dart_tool/',
      'build/',
      '.gradle/',
      '.idea/',
      '.vscode/',
      'android/.gradle/',
      'ios/Pods/',
      'macos/Pods/',
      'windows/flutter/',
      'linux/flutter/',
      '.cache/',
      'tmp/',
      '.tmp/',

      'webserver/build/',
      'flow/build/',
      'common/build/',
      'plugin/build/',
      'app/build/',
      'webserver/data/',
      'flow/data/',
      'gradle/wrapper/',
      'flow_frontend/build/',
      '.kotlin/',
      'reports/',
      'test-results/',
      'distributions/',
      'libs/',
      'scripts/',
      'scriptsShadow/',
    ];

    final ignoredExtensions = [
      '.jar',
      '.class',
      '.dex',
      '.apk',
      '.ipa',
      '.exe',
      '.dll',
      '.so',
      '.png',
      '.jpg',
      '.jpeg',
      '.gif',
      '.bmp',
      '.ico',
      '.svg',
      '.mp4',
      '.avi',
      '.mov',
      '.mkv',
      '.mp3',
      '.wav',
      '.ogg',
      '.zip',
      '.tar',
      '.gz',
      '.7z',
      '.rar',
      '.log',
      '.tmp',
    ];


    for (final pattern in ignoredPatterns) {
      if (path.contains(pattern)) return true;
    }


    final extension = _getFileExtension(path).toLowerCase();
    if (ignoredExtensions.contains(extension)) return true;

    return false;
  }

  String _getFileExtension(String path) {
    final lastDot = path.lastIndexOf('.');
    return lastDot != -1 ? path.substring(lastDot) : '';
  }

  String? _getLanguageFromExtension(String extension) {
    final languageMap = {
      '.dart': 'Dart',
      '.kt': 'Kotlin',
      '.kts': 'Kotlin',
      '.java': 'Java',
      '.js': 'JavaScript',
      '.ts': 'TypeScript',
      '.py': 'Python',
      '.cpp': 'C++',
      '.c': 'C',
      '.cs': 'C#',
      '.go': 'Go',
      '.rs': 'Rust',
      '.php': 'PHP',
      '.rb': 'Ruby',
      '.swift': 'Swift',
      '.html': 'HTML',
      '.css': 'CSS',
      '.scss': 'SCSS',
      '.less': 'LESS',
      '.json': 'JSON',
      '.yaml': 'YAML',
      '.yml': 'YAML',
      '.xml': 'XML',
      '.md': 'Markdown',
      '.sh': 'Shell',
      '.bash': 'Shell',
      '.zsh': 'Shell',
      '.sql': 'SQL',
      '.gradle': 'Gradle',
      '.cmake': 'CMake',
    };

    return languageMap[extension.toLowerCase()];
  }

  bool _isComment(String line, String language) {
    final commentPrefixes = {
      'Dart': ['//', '/*', '*', '*/'],
      'Kotlin': ['//', '/*', '*', '*/'],
      'Java': ['//', '/*', '*', '*/'],
      'JavaScript': ['//', '/*', '*', '*/'],
      'TypeScript': ['//', '/*', '*', '*/'],
      'C++': ['//', '/*', '*', '*/'],
      'C': ['//', '/*', '*', '*/'],
      'C#': ['//', '/*', '*', '*/'],
      'Go': ['//', '/*', '*', '*/'],
      'Rust': ['//', '/*', '*', '*/'],
      'PHP': ['//', '/*', '*', '*/', '#'],
      'Python': ['#', '"""', "'''"],
      'Ruby': ['#'],
      'Shell': ['#'],
      'HTML': ['<!--', '-->'],
      'CSS': ['/*', '*', '*/'],
      'SCSS': ['//', '/*', '*', '*/'],
      'LESS': ['//', '/*', '*', '*/'],
      'SQL': ['--', '/*', '*', '*/'],
    };

    final prefixes = commentPrefixes[language] ?? [];
    return prefixes.any((prefix) => line.startsWith(prefix));
  }
}

class ProjectStats {
  final int totalFiles;
  final int linesOfCode;
  final Map<String, int> languageBreakdown;
  final List<String> recentFiles;
  final String projectPath;

  ProjectStats({
    required this.totalFiles,
    required this.linesOfCode,
    required this.languageBreakdown,
    required this.recentFiles,
    required this.projectPath,
  });

  String get topLanguage {
    if (languageBreakdown.isEmpty) return 'Unknown';

    final sortedLanguages = languageBreakdown.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));

    return sortedLanguages.first.key;
  }

  String get formattedLinesOfCode {
    if (linesOfCode < 1000) return linesOfCode.toString();
    if (linesOfCode < 1000000)
      return '${(linesOfCode / 1000).toStringAsFixed(1)}k';
    return '${(linesOfCode / 1000000).toStringAsFixed(1)}M';
  }
}
