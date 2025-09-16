import 'dart:convert';
import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';

class GraphProject {
  final String id;
  final String name;
  final String description;
  final String category;
  final DateTime createdAt;
  final DateTime modifiedAt;
  final Map<String, dynamic> graphData;

  GraphProject({
    required this.id,
    required this.name,
    required this.description,
    required this.category,
    required this.createdAt,
    required this.modifiedAt,
    required this.graphData,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'category': category,
      'createdAt': createdAt.toIso8601String(),
      'modifiedAt': modifiedAt.toIso8601String(),
      'graphData': graphData,
    };
  }

  factory GraphProject.fromJson(Map<String, dynamic> json) {
    return GraphProject(
      id: json['id'],
      name: json['name'],
      description: json['description'],
      category: json['category'],
      createdAt: DateTime.parse(json['createdAt']),
      modifiedAt: DateTime.parse(json['modifiedAt']),
      graphData: json['graphData'] ?? {},
    );
  }

  GraphProject copyWith({
    String? name,
    String? description,
    String? category,
    DateTime? modifiedAt,
    Map<String, dynamic>? graphData,
  }) {
    return GraphProject(
      id: id,
      name: name ?? this.name,
      description: description ?? this.description,
      category: category ?? this.category,
      createdAt: createdAt,
      modifiedAt: modifiedAt ?? DateTime.now(),
      graphData: graphData ?? this.graphData,
    );
  }
}

class GraphProjectService {
  static GraphProjectService? _instance;
  static GraphProjectService get instance =>
      _instance ??= GraphProjectService._();

  GraphProjectService._();

  final StreamController<List<GraphProject>> _projectsController =
      StreamController<List<GraphProject>>.broadcast();

  List<GraphProject> _projects = [];
  bool _isInitialized = false;

  Stream<List<GraphProject>> get projectsStream => _projectsController.stream;
  List<GraphProject> get projects => List.unmodifiable(_projects);
  bool get isInitialized => _isInitialized;

  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      await _loadProjectsFromStorage();
      _isInitialized = true;
    } catch (e) {
      print('Error initializing GraphProjectService: $e');
      _projects = [];
      _isInitialized = true;
    }

    _projectsController.add(_projects);
  }

  Future<void> _loadProjectsFromStorage() async {
    final prefs = await SharedPreferences.getInstance();
    final projectsJson = prefs.getString('graph_projects') ?? '[]';

    try {
      final List<dynamic> projectsList = json.decode(projectsJson);
      _projects = projectsList
          .map((json) => GraphProject.fromJson(json))
          .toList();

      // Sort by modified date (newest first)
      _projects.sort((a, b) => b.modifiedAt.compareTo(a.modifiedAt));

      print('Loaded ${_projects.length} graph projects');
    } catch (e) {
      print('Error loading projects from storage: $e');
      _projects = [];
    }
  }

  Future<void> _saveProjectsToStorage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final projectsJson = json.encode(
        _projects.map((p) => p.toJson()).toList(),
      );
      await prefs.setString('graph_projects', projectsJson);
    } catch (e) {
      print('Error saving projects to storage: $e');
    }
  }

  Future<GraphProject> createProject({
    required String name,
    required String description,
    required String category,
    Map<String, dynamic>? initialGraphData,
  }) async {
    final now = DateTime.now();
    final project = GraphProject(
      id: 'project_${now.millisecondsSinceEpoch}',
      name: name,
      description: description,
      category: category,
      createdAt: now,
      modifiedAt: now,
      graphData: initialGraphData ?? {},
    );

    _projects.insert(0, project); // Add to beginning (newest first)
    await _saveProjectsToStorage();
    _projectsController.add(_projects);

    return project;
  }

  Future<void> updateProject(
    String projectId, {
    String? name,
    String? description,
    String? category,
    Map<String, dynamic>? graphData,
  }) async {
    final index = _projects.indexWhere((p) => p.id == projectId);
    if (index == -1) return;

    _projects[index] = _projects[index].copyWith(
      name: name,
      description: description,
      category: category,
      graphData: graphData,
      modifiedAt: DateTime.now(),
    );

    // Resort by modified date
    _projects.sort((a, b) => b.modifiedAt.compareTo(a.modifiedAt));

    await _saveProjectsToStorage();
    _projectsController.add(_projects);
  }

  Future<void> deleteProject(String projectId) async {
    _projects.removeWhere((p) => p.id == projectId);
    await _saveProjectsToStorage();
    _projectsController.add(_projects);
  }

  GraphProject? getProject(String projectId) {
    try {
      return _projects.firstWhere((p) => p.id == projectId);
    } catch (e) {
      return null;
    }
  }

  List<GraphProject> getProjectsByCategory(String category) {
    return _projects.where((p) => p.category == category).toList();
  }

  List<String> getCategories() {
    final categories = _projects.map((p) => p.category).toSet().toList();
    categories.sort();
    return categories;
  }

  Future<GraphProject> duplicateProject(
    String projectId, {
    String? newName,
  }) async {
    final original = getProject(projectId);
    if (original == null) throw Exception('Project not found');

    final now = DateTime.now();
    final duplicate = GraphProject(
      id: 'project_${now.millisecondsSinceEpoch}',
      name: newName ?? '${original.name} (Copy)',
      description: original.description,
      category: original.category,
      createdAt: now,
      modifiedAt: now,
      graphData: Map<String, dynamic>.from(original.graphData),
    );

    _projects.insert(0, duplicate);
    await _saveProjectsToStorage();
    _projectsController.add(_projects);

    return duplicate;
  }

  void dispose() {
    _projectsController.close();
  }
}
