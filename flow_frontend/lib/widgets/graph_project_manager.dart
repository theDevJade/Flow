import 'package:flutter/material.dart';
import '../services/graph_project_service.dart';

class GraphProjectManager extends StatefulWidget {
  final Function(GraphProject)? onProjectSelected;
  final Function()? onNewProject;

  const GraphProjectManager({
    super.key,
    this.onProjectSelected,
    this.onNewProject,
  });

  @override
  State<GraphProjectManager> createState() => _GraphProjectManagerState();
}

class _GraphProjectManagerState extends State<GraphProjectManager> {
  final GraphProjectService _projectService = GraphProjectService.instance;
  List<GraphProject> _projects = [];
  String _selectedCategory = 'All';
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _initializeService();
    _searchController.addListener(() {
      setState(() {
        _searchQuery = _searchController.text.toLowerCase();
      });
    });
  }

  Future<void> _initializeService() async {
    await _projectService.initialize();
    _projectService.projectsStream.listen((projects) {
      if (mounted) {
        setState(() {
          _projects = projects;
        });
      }
    });
    setState(() {
      _projects = _projectService.projects;
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<GraphProject> get _filteredProjects {
    var filtered = _projects;

    if (_selectedCategory != 'All') {
      filtered = filtered
          .where((p) => p.category == _selectedCategory)
          .toList();
    }

    if (_searchQuery.isNotEmpty) {
      filtered = filtered
          .where(
            (p) =>
                p.name.toLowerCase().contains(_searchQuery) ||
                p.description.toLowerCase().contains(_searchQuery),
          )
          .toList();
    }

    return filtered;
  }

  @override
  Widget build(BuildContext context) {
    final categories = ['All', ..._projectService.getCategories()];
    final filteredProjects = _filteredProjects;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Graph Projects'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _showCreateProjectDialog,
            tooltip: 'New Project',
          ),
        ],
      ),
      body: Column(
        children: [
          // Search and filter bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      hintText: 'Search projects...',
                      prefixIcon: Icon(Icons.search),
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                DropdownButton<String>(
                  value: _selectedCategory,
                  onChanged: (value) {
                    setState(() {
                      _selectedCategory = value ?? 'All';
                    });
                  },
                  items: categories.map((category) {
                    return DropdownMenuItem(
                      value: category,
                      child: Text(category),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          // Projects list
          Expanded(
            child: filteredProjects.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    itemCount: filteredProjects.length,
                    itemBuilder: (context, index) {
                      final project = filteredProjects[index];
                      return _buildProjectTile(project);
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.folder_open,
            size: 64,
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
          ),
          const SizedBox(height: 16),
          Text(
            _searchQuery.isNotEmpty || _selectedCategory != 'All'
                ? 'No projects found'
                : 'No projects yet',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _searchQuery.isNotEmpty || _selectedCategory != 'All'
                ? 'Try adjusting your search or filter'
                : 'Create your first graph project',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
            ),
          ),
          if (_searchQuery.isEmpty && _selectedCategory == 'All') ...[
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _showCreateProjectDialog,
              icon: const Icon(Icons.add),
              label: const Text('Create Project'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildProjectTile(GraphProject project) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: _getCategoryColor(project.category),
          child: Text(
            project.name.isNotEmpty ? project.name[0].toUpperCase() : 'P',
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        title: Text(
          project.name,
          style: const TextStyle(fontWeight: FontWeight.w500),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(project.description),
            const SizedBox(height: 4),
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: _getCategoryColor(project.category).withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    project.category,
                    style: TextStyle(
                      fontSize: 12,
                      color: _getCategoryColor(project.category),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
                const Spacer(),
                Text(
                  'Modified ${_formatDate(project.modifiedAt)}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(
                      context,
                    ).colorScheme.onSurface.withOpacity(0.6),
                  ),
                ),
              ],
            ),
          ],
        ),
        onTap: () {
          widget.onProjectSelected?.call(project);
        },
        trailing: PopupMenuButton<String>(
          onSelected: (action) => _handleProjectAction(action, project),
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'edit',
              child: ListTile(
                leading: Icon(Icons.edit),
                title: Text('Edit'),
                dense: true,
              ),
            ),
            const PopupMenuItem(
              value: 'duplicate',
              child: ListTile(
                leading: Icon(Icons.copy),
                title: Text('Duplicate'),
                dense: true,
              ),
            ),
            const PopupMenuDivider(),
            const PopupMenuItem(
              value: 'delete',
              child: ListTile(
                leading: Icon(Icons.delete, color: Colors.red),
                title: Text('Delete', style: TextStyle(color: Colors.red)),
                dense: true,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _getCategoryColor(String category) {
    final colors = [
      Colors.blue,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.red,
      Colors.teal,
      Colors.indigo,
      Colors.amber,
    ];
    return colors[category.hashCode % colors.length];
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final diff = now.difference(date);

    if (diff.inDays == 0) {
      if (diff.inHours == 0) {
        return '${diff.inMinutes}m ago';
      }
      return '${diff.inHours}h ago';
    } else if (diff.inDays < 7) {
      return '${diff.inDays}d ago';
    } else {
      return '${date.day}/${date.month}/${date.year}';
    }
  }

  void _handleProjectAction(String action, GraphProject project) {
    switch (action) {
      case 'edit':
        _showEditProjectDialog(project);
        break;
      case 'duplicate':
        _duplicateProject(project);
        break;
      case 'delete':
        _showDeleteConfirmation(project);
        break;
    }
  }

  void _showCreateProjectDialog() {
    _showProjectDialog();
  }

  void _showEditProjectDialog(GraphProject project) {
    _showProjectDialog(project: project);
  }

  void _showProjectDialog({GraphProject? project}) {
    final isEditing = project != null;
    final nameController = TextEditingController(text: project?.name ?? '');
    final descriptionController = TextEditingController(
      text: project?.description ?? '',
    );
    String selectedCategory = project?.category ?? 'Workflow';

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(isEditing ? 'Edit Project' : 'Create New Project'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(
                  labelText: 'Project Name',
                  border: OutlineInputBorder(),
                ),
                autofocus: true,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: descriptionController,
                decoration: const InputDecoration(
                  labelText: 'Description',
                  border: OutlineInputBorder(),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: selectedCategory,
                decoration: const InputDecoration(
                  labelText: 'Category',
                  border: OutlineInputBorder(),
                ),
                items:
                    [
                      'Workflow',
                      'Algorithm',
                      'Data Processing',
                      'Automation',
                      'Machine Learning',
                      'System Design',
                      'Game Logic',
                      'Other',
                    ].map((category) {
                      return DropdownMenuItem(
                        value: category,
                        child: Text(category),
                      );
                    }).toList(),
                onChanged: (value) {
                  setDialogState(() {
                    selectedCategory = value!;
                  });
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () => _saveProject(
                context,
                nameController.text.trim(),
                descriptionController.text.trim(),
                selectedCategory,
                project,
              ),
              child: Text(isEditing ? 'Update' : 'Create'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _saveProject(
    BuildContext context,
    String name,
    String description,
    String category,
    GraphProject? existingProject,
  ) async {
    if (name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Project name is required')));
      return;
    }

    try {
      if (existingProject != null) {
        await _projectService.updateProject(
          existingProject.id,
          name: name,
          description: description,
          category: category,
        );
      } else {
        await _projectService.createProject(
          name: name,
          description: description,
          category: category,
        );
      }

      if (context.mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              existingProject != null
                  ? 'Project updated successfully'
                  : 'Project created successfully',
            ),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error saving project: $e')));
      }
    }
  }

  Future<void> _duplicateProject(GraphProject project) async {
    try {
      await _projectService.duplicateProject(project.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Project "${project.name}" duplicated')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error duplicating project: $e')),
        );
      }
    }
  }

  void _showDeleteConfirmation(GraphProject project) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Project'),
        content: Text(
          'Are you sure you want to delete "${project.name}"? This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(context).pop();
              await _projectService.deleteProject(project.id);
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Project "${project.name}" deleted')),
                );
              }
            },
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}
