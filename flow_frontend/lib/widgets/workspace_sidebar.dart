import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/workspace_state.dart';
import '../state/workspace_manager.dart';
import '../state/page_manager.dart';

enum WorkspacePageType { graphEditor, codeEditor, terminal }

class WorkspacePage {
  final String id;
  final String name;
  final WorkspacePageType type;
  final IconData icon;
  final bool isActive;

  WorkspacePage({
    required this.id,
    required this.name,
    required this.type,
    required this.icon,
    this.isActive = false,
  });

  WorkspacePage copyWith({
    String? id,
    String? name,
    WorkspacePageType? type,
    IconData? icon,
    bool? isActive,
  }) {
    return WorkspacePage(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      icon: icon ?? this.icon,
      isActive: isActive ?? this.isActive,
    );
  }
}

class WorkspaceSidebar extends StatefulWidget {
  const WorkspaceSidebar({super.key});

  @override
  State<WorkspaceSidebar> createState() => _WorkspaceSidebarState();
}

class _WorkspaceSidebarState extends State<WorkspaceSidebar> {
  List<WorkspacePage> _pages = [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadWorkspacePages();
    });
  }

  void _loadWorkspacePages() {
    final workspaceManager = context.read<WorkspaceManager>();
    final currentWorkspace = workspaceManager.currentWorkspace;

    if (currentWorkspace != null) {

      final pagesData = currentWorkspace.data['pages'] as List<dynamic>?;

      if (pagesData != null && pagesData.isNotEmpty) {
        setState(() {
          _pages = pagesData
              .map(
                (pageData) => WorkspacePage(
                  id: pageData['id'] ?? '',
                  name: pageData['name'] ?? '',
                  type: _parsePageType(pageData['type'] ?? 'codeEditor'),
                  icon: _getIconForPageType(
                    _parsePageType(pageData['type'] ?? 'codeEditor'),
                  ),
                  isActive: pageData['isActive'] ?? false,
                ),
              )
              .toList();
        });
      } else {
        // Load default pages if no pages exist
        _loadDefaultPages();
      }
    } else {
      // No workspace selected, load defaults
      _loadDefaultPages();
    }
  }

  void _loadDefaultPages() {
    setState(() {
      _pages = [
        WorkspacePage(
          id: 'graph',
          name: 'Graph Editor',
          type: WorkspacePageType.graphEditor,
          icon: Icons.account_tree,
          isActive: true,
        ),
        WorkspacePage(
          id: 'code',
          name: 'Code Editor',
          type: WorkspacePageType.codeEditor,
          icon: Icons.code,
        ),
        WorkspacePage(
          id: 'terminal',
          name: 'Terminal',
          type: WorkspacePageType.terminal,
          icon: Icons.terminal,
        ),
      ];
    });
    _saveWorkspacePages();
  }

  WorkspacePageType _parsePageType(String typeString) {
    switch (typeString) {
      case 'graphEditor':
        return WorkspacePageType.graphEditor;
      case 'terminal':
        return WorkspacePageType.terminal;
      case 'codeEditor':
      default:
        return WorkspacePageType.codeEditor;
    }
  }

  void _saveWorkspacePages() {
    final workspaceManager = context.read<WorkspaceManager>();
    final currentWorkspace = workspaceManager.currentWorkspace;

    if (currentWorkspace != null) {
      final pagesData = _pages
          .map(
            (page) => {
              'id': page.id,
              'name': page.name,
              'type': page.type.name,
              'isActive': page.isActive,
            },
          )
          .toList();

      // Update workspace data with pages
      final updatedData = Map<String, dynamic>.from(currentWorkspace.data);
      updatedData['pages'] = pagesData;

      workspaceManager.updateWorkspace(currentWorkspace.id, data: updatedData);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<WorkspaceManager>(
      builder: (context, workspaceManager, child) {
        // Reload pages if workspace changed
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (workspaceManager.currentWorkspace != null) {
            _loadWorkspacePages();
          }
        });

        return Container(
          width: 200,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            border: Border(
              right: BorderSide(
                color: Theme.of(context).dividerColor,
                width: 0.5,
              ),
            ),
          ),
          child: Column(
            children: [
              // Header
              Container(
                height: 50,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surface,
                  border: Border(
                    bottom: BorderSide(
                      color: Theme.of(context).dividerColor,
                      width: 0.5,
                    ),
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.workspaces,
                      size: 18,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        workspaceManager.currentWorkspace?.name ?? 'Workspace',
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: Theme.of(context).colorScheme.onSurface,
                        ),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.add, size: 18),
                      onPressed: () => _showAddPageDialog(context),
                      tooltip: 'Add Page',
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(
                        minWidth: 28,
                        minHeight: 28,
                      ),
                    ),
                  ],
                ),
              ),
              // Pages list
              Expanded(
                child: ListView.builder(
                  padding: const EdgeInsets.all(8),
                  itemCount: _pages.length,
                  itemBuilder: (context, index) =>
                      _buildPageItem(_pages[index]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPageItem(WorkspacePage page) {
    return Container(
      margin: const EdgeInsets.only(bottom: 4),
      child: Material(
        color: page.isActive
            ? Theme.of(context).colorScheme.primaryContainer.withOpacity(0.5)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: () => _switchToPage(page),
          onSecondaryTapDown: (details) =>
              _showPageContextMenu(context, details.globalPosition, page),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              children: [
                Icon(
                  page.icon,
                  size: 16,
                  color: page.isActive
                      ? Theme.of(context).colorScheme.primary
                      : Theme.of(
                          context,
                        ).colorScheme.onSurface.withOpacity(0.7),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    page.name,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: page.isActive
                          ? Theme.of(context).colorScheme.onPrimaryContainer
                          : Theme.of(context).colorScheme.onSurface,
                      fontWeight: page.isActive
                          ? FontWeight.w500
                          : FontWeight.normal,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (!page.isActive)
                  Icon(
                    Icons.more_horiz,
                    size: 14,
                    color: Theme.of(
                      context,
                    ).colorScheme.onSurface.withOpacity(0.4),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _switchToPage(WorkspacePage page) {
    setState(() {
      for (int i = 0; i < _pages.length; i++) {
        _pages[i] = _pages[i].copyWith(isActive: _pages[i].id == page.id);
      }
    });

    // Update the PageManager with the active page
    final pageManager = PageManager.instance;
    pageManager.setActivePage(page.id, page.type.name);

    // Update workspace state for UI consistency (still needed for main shell)
    final workspaceState = context.read<WorkspaceState>();
    switch (page.type) {
      case WorkspacePageType.graphEditor:
        workspaceState.switchToWorkspace(WorkspaceType.graphEditor);
        break;
      case WorkspacePageType.codeEditor:
        workspaceState.switchToWorkspace(WorkspaceType.codeEditor);
        break;
      case WorkspacePageType.terminal:
        workspaceState.switchToWorkspace(WorkspaceType.terminal);
        break;
    }

    // Save the updated pages state
    _saveWorkspacePages();
  }

  void _showAddPageDialog(BuildContext context) {
    final nameController = TextEditingController();
    WorkspacePageType selectedType = WorkspacePageType.codeEditor;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Add New Page'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(
                  labelText: 'Page Name',
                  hintText: 'My Workflow',
                  border: OutlineInputBorder(),
                ),
                autofocus: true,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<WorkspacePageType>(
                value: selectedType,
                decoration: const InputDecoration(
                  labelText: 'Page Type',
                  border: OutlineInputBorder(),
                ),
                items: [
                  DropdownMenuItem(
                    value: WorkspacePageType.graphEditor,
                    child: Row(
                      children: [
                        const Icon(Icons.account_tree, size: 16),
                        const SizedBox(width: 8),
                        const Text('Graph Editor'),
                      ],
                    ),
                  ),
                  DropdownMenuItem(
                    value: WorkspacePageType.codeEditor,
                    child: Row(
                      children: [
                        const Icon(Icons.code, size: 16),
                        const SizedBox(width: 8),
                        const Text('Code Editor'),
                      ],
                    ),
                  ),
                  DropdownMenuItem(
                    value: WorkspacePageType.terminal,
                    child: Row(
                      children: [
                        const Icon(Icons.terminal, size: 16),
                        const SizedBox(width: 8),
                        const Text('Terminal'),
                      ],
                    ),
                  ),
                ],
                onChanged: (value) {
                  if (value != null) {
                    setDialogState(() {
                      selectedType = value;
                    });
                  }
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
              onPressed: () {
                if (nameController.text.trim().isNotEmpty) {
                  _addPage(nameController.text.trim(), selectedType);
                  Navigator.of(context).pop();
                }
              },
              child: const Text('Add'),
            ),
          ],
        ),
      ),
    );
  }

  void _addPage(String name, WorkspacePageType type) {
    final icon = _getIconForPageType(type);
    final newPage = WorkspacePage(
      id: 'page_${DateTime.now().millisecondsSinceEpoch}',
      name: name,
      type: type,
      icon: icon,
    );

    setState(() {
      _pages.add(newPage);
    });

    // Save pages to workspace
    _saveWorkspacePages();
  }

  IconData _getIconForPageType(WorkspacePageType type) {
    switch (type) {
      case WorkspacePageType.graphEditor:
        return Icons.account_tree;
      case WorkspacePageType.codeEditor:
        return Icons.code;
      case WorkspacePageType.terminal:
        return Icons.terminal;
    }
  }

  void _showPageContextMenu(
    BuildContext context,
    Offset position,
    WorkspacePage page,
  ) {
    showMenu(
      context: context,
      position: RelativeRect.fromLTRB(
        position.dx,
        position.dy,
        position.dx + 1,
        position.dy + 1,
      ),
      items: <PopupMenuEntry<String>>[
        PopupMenuItem<String>(
          value: 'rename',
          child: const Row(
            children: [
              Icon(Icons.edit, size: 16),
              SizedBox(width: 8),
              Text('Rename'),
            ],
          ),
        ),
        PopupMenuItem<String>(
          value: 'duplicate',
          child: const Row(
            children: [
              Icon(Icons.copy, size: 16),
              SizedBox(width: 8),
              Text('Duplicate'),
            ],
          ),
        ),
        const PopupMenuDivider(),
        PopupMenuItem<String>(
          value: 'delete',
          child: const Row(
            children: [
              Icon(Icons.delete, size: 16, color: Colors.red),
              SizedBox(width: 8),
              Text('Delete', style: TextStyle(color: Colors.red)),
            ],
          ),
        ),
      ],
    ).then((value) {
      if (value != null) {
        switch (value) {
          case 'rename':
            _showRenamePage(page);
            break;
          case 'duplicate':
            _duplicatePage(page);
            break;
          case 'delete':
            _deletePage(page);
            break;
        }
      }
    });
  }

  void _showRenamePage(WorkspacePage page) {
    final controller = TextEditingController(text: page.name);

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rename Page'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            labelText: 'Page Name',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) {
                _renamePage(page, controller.text.trim());
                Navigator.of(context).pop();
              }
            },
            child: const Text('Rename'),
          ),
        ],
      ),
    );
  }

  void _renamePage(WorkspacePage page, String newName) {
    setState(() {
      final index = _pages.indexWhere((p) => p.id == page.id);
      if (index >= 0) {
        _pages[index] = _pages[index].copyWith(name: newName);
      }
    });
    _saveWorkspacePages();
  }

  void _duplicatePage(WorkspacePage page) {
    final duplicatedPage = WorkspacePage(
      id: 'page_${DateTime.now().millisecondsSinceEpoch}',
      name: '${page.name} (Copy)',
      type: page.type,
      icon: page.icon,
    );

    setState(() {
      _pages.add(duplicatedPage);
    });
    _saveWorkspacePages();
  }

  void _deletePage(WorkspacePage page) {
    if (_pages.length <= 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cannot delete the last page')),
      );
      return;
    }

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Page'),
        content: Text('Are you sure you want to delete "${page.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              setState(() {
                _pages.removeWhere((p) => p.id == page.id);
                // If we deleted the active page, activate the first one
                if (page.isActive && _pages.isNotEmpty) {
                  _pages[0] = _pages[0].copyWith(isActive: true);
                  _switchToPage(_pages[0]);
                }
              });
              _saveWorkspacePages();
              Navigator.of(context).pop();
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Delete', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
