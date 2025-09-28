import 'dart:convert';
import 'package:http/http.dart' as http;

class UpdateCheckService {
  static final UpdateCheckService _instance = UpdateCheckService._internal();
  factory UpdateCheckService() => _instance;
  UpdateCheckService._internal();

  static const String _currentVersion = '1.0.0';
  static const String _updateCheckUrl =
      'https://api.github.com/repos/thedevjade/flow/releases/latest';

  Future<UpdateInfo> checkForUpdates() async {
    try {

      final response = await http.get(
        Uri.parse(_updateCheckUrl),
        headers: {'Accept': 'application/vnd.github.v3+json'},
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final latestVersion =
            data['tag_name']?.toString().replaceFirst('v', '') ??
                _currentVersion;
        final releaseNotes = data['body']?.toString() ?? '';
        final publishedAt = data['published_at']?.toString() ?? '';
        final downloadUrl = data['html_url']?.toString() ?? '';

        final hasUpdate = _isNewerVersion(latestVersion, _currentVersion);

        return UpdateInfo(
          currentVersion: _currentVersion,
          latestVersion: latestVersion,
          hasUpdate: hasUpdate,
          releaseNotes: releaseNotes,
          publishedAt: publishedAt,
          downloadUrl: downloadUrl,
          error: null,
        );
      } else {
        return UpdateInfo.error(
          'Failed to check for updates: HTTP ${response.statusCode}',
        );
      }
    } catch (e) {

      return UpdateInfo.error('Unable to check for updates: ${e.toString()}');
    }
  }

  bool _isNewerVersion(String latest, String current) {
    final latestParts =
        latest.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    final currentParts =
        current.split('.').map((e) => int.tryParse(e) ?? 0).toList();


    while (latestParts.length < 3) latestParts.add(0);
    while (currentParts.length < 3) currentParts.add(0);

    for (int i = 0; i < 3; i++) {
      if (latestParts[i] > currentParts[i]) return true;
      if (latestParts[i] < currentParts[i]) return false;
    }

    return false;
  }

  Future<UpdateInfo> mockUpdateCheck() async {

    await Future.delayed(const Duration(milliseconds: 800));

    // @TODO mock response
    final scenarios = [
      UpdateInfo(
        currentVersion: _currentVersion,
        latestVersion: '1.0.1',
        hasUpdate: true,
        releaseNotes:
            '• Bug fixes and performance improvements\n• Enhanced graph editor\n• New WebSocket stability improvements',
        publishedAt:
            DateTime.now().subtract(const Duration(days: 2)).toIso8601String(),
        downloadUrl: 'https://github.com/thedevjade/flow/releases/latest',
        error: null,
      ),
      UpdateInfo(
        currentVersion: _currentVersion,
        latestVersion: _currentVersion,
        hasUpdate: false,
        releaseNotes: '',
        publishedAt: '',
        downloadUrl: '',
        error: null,
      ),
    ];


    return scenarios.first;
  }
}

class UpdateInfo {
  final String currentVersion;
  final String latestVersion;
  final bool hasUpdate;
  final String releaseNotes;
  final String publishedAt;
  final String downloadUrl;
  final String? error;

  UpdateInfo({
    required this.currentVersion,
    required this.latestVersion,
    required this.hasUpdate,
    required this.releaseNotes,
    required this.publishedAt,
    required this.downloadUrl,
    this.error,
  });

  factory UpdateInfo.error(String error) {
    return UpdateInfo(
      currentVersion: '1.0.0',
      latestVersion: '1.0.0',
      hasUpdate: false,
      releaseNotes: '',
      publishedAt: '',
      downloadUrl: '',
      error: error,
    );
  }

  String get updateStatusText {
    if (error != null) return 'Update check failed';
    if (hasUpdate) return 'Update available (v$latestVersion)';
    return 'Up to date (v$currentVersion)';
  }

  String get friendlyPublishedDate {
    if (publishedAt.isEmpty) return '';

    try {
      final published = DateTime.parse(publishedAt);
      final now = DateTime.now();
      final difference = now.difference(published);

      if (difference.inDays > 0) {
        return '${difference.inDays} days ago';
      } else if (difference.inHours > 0) {
        return '${difference.inHours} hours ago';
      } else {
        return 'Recently';
      }
    } catch (e) {
      return '';
    }
  }
}
