import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'node_template.dart';
import 'node_template_service.dart';

class CommandPalette extends StatefulWidget {
  final Function(NodeTemplate, Offset) onNodeSelected;
  final VoidCallback onDismiss;
  final Offset spawnPosition;

  const CommandPalette({
    super.key,
    required this.onNodeSelected,
    required this.onDismiss,
    required this.spawnPosition,
  });

  @override
  State<CommandPalette> createState() => _CommandPaletteState();
}

class _CommandPaletteState extends State<CommandPalette>
    with TickerProviderStateMixin {
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocus = FocusNode();
  List<NodeTemplate> filteredTemplates = [];
  int selectedIndex = 0;
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 200),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );
    _slideAnimation =
        Tween<Offset>(begin: const Offset(0, -0.1), end: Offset.zero).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    _loadTemplatesAndFilter();
    _animationController.forward();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _searchFocus.requestFocus();
    });
  }

  void _loadTemplatesAndFilter([String query = '']) async {
    // Ensure templates are loaded
    await NodeTemplateService.instance.loadTemplates();
    _filterTemplates(query);
  }

  void _filterTemplates(String query) {
    final allTemplates = NodeTemplateService.instance.allTemplates;

    setState(() {
      if (query.isEmpty) {
        filteredTemplates = List.from(allTemplates);
      } else {
        filteredTemplates = allTemplates.where((template) {
          final searchText = query.toLowerCase();
          return template.name.toLowerCase().contains(searchText) ||
              template.category.toLowerCase().contains(searchText) ||
              template.description.toLowerCase().contains(searchText);
        }).toList();
      }
      selectedIndex = 0;
    });
  }

  @override
  void dispose() {
    _animationController.dispose();
    _searchController.dispose();
    _searchFocus.dispose();
    super.dispose();
  }

  void _selectTemplate(NodeTemplate template) {
    widget.onNodeSelected(template, widget.spawnPosition);
  }

  void _handleKeyPress(KeyEvent event) {
    if (event is KeyDownEvent) {
      if (event.logicalKey == LogicalKeyboardKey.escape) {
        widget.onDismiss();
      } else if (event.logicalKey == LogicalKeyboardKey.arrowUp) {
        setState(() {
          selectedIndex = (selectedIndex - 1).clamp(
            0,
            filteredTemplates.length - 1,
          );
        });
      } else if (event.logicalKey == LogicalKeyboardKey.arrowDown) {
        setState(() {
          selectedIndex = (selectedIndex + 1).clamp(
            0,
            filteredTemplates.length - 1,
          );
        });
      } else if (event.logicalKey == LogicalKeyboardKey.enter) {
        if (filteredTemplates.isNotEmpty &&
            selectedIndex < filteredTemplates.length) {
          _selectTemplate(filteredTemplates[selectedIndex]);
        }
      }
    }
  }

  Color _getCategoryColor(String category) {
    switch (category.toLowerCase()) {
      case 'input':
        return Colors.green.shade700;
      case 'math':
        return Colors.orange.shade700;
      case 'processing':
        return Colors.blue.shade700;
      case 'output':
        return Colors.red.shade700;
      default:
        return Colors.grey.shade700;
    }
  }

  IconData _getCategoryIcon(String category) {
    switch (category.toLowerCase()) {
      case 'input':
        return Icons.input;
      case 'math':
        return Icons.functions;
      case 'processing':
        return Icons.settings;
      case 'output':
        return Icons.output;
      default:
        return Icons.extension;
    }
  }

  @override
  Widget build(BuildContext context) {
    // Group templates by category
    final categories = <String>[];
    final templatesByCategory = <String, List<NodeTemplate>>{};

    for (final template in filteredTemplates) {
      if (!categories.contains(template.category)) {
        categories.add(template.category);
        templatesByCategory[template.category] = [];
      }
      templatesByCategory[template.category]!.add(template);
    }

    return KeyboardListener(
      focusNode: FocusNode(),
      onKeyEvent: _handleKeyPress,
      autofocus: true,
      child: Material(
        color: Colors.transparent,
        child: GestureDetector(
          onTap: widget.onDismiss,
          child: Container(
            color: Colors.black.withAlpha(77),
            child: Center(
              child: FadeTransition(
                opacity: _fadeAnimation,
                child: SlideTransition(
                  position: _slideAnimation,
                  child: GestureDetector(
                    onTap: () {}, // Prevent dismissal when clicking on palette
                    child: Container(
                      width: 500,
                      height: 600,
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.surface,
                        borderRadius: BorderRadius.circular(12),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.3),
                            blurRadius: 20,
                            offset: const Offset(0, 8),
                          ),
                        ],
                      ),
                      child: Column(
                        children: [
                          // Header with search
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: Theme.of(
                                context,
                              ).colorScheme.primaryContainer.withAlpha(77),
                              borderRadius: const BorderRadius.only(
                                topLeft: Radius.circular(12),
                                topRight: Radius.circular(12),
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Add Node',
                                  style: Theme.of(context)
                                      .textTheme
                                      .titleLarge
                                      ?.copyWith(fontWeight: FontWeight.bold),
                                ),
                                const SizedBox(height: 8),
                                TextField(
                                  controller: _searchController,
                                  focusNode: _searchFocus,
                                  decoration: InputDecoration(
                                    hintText: 'Search nodes...',
                                    prefixIcon: const Icon(Icons.search),
                                    border: OutlineInputBorder(
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    filled: true,
                                    fillColor: Theme.of(
                                      context,
                                    ).colorScheme.surface,
                                  ),
                                  onChanged: (query) {
                                    _filterTemplates(query);
                                  },
                                ),
                              ],
                            ),
                          ),

                          // Results list
                          Expanded(
                            child: filteredTemplates.isEmpty
                                ? Center(
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        Icon(
                                          Icons.search_off,
                                          size: 64,
                                          color: Colors.grey.shade400,
                                        ),
                                        const SizedBox(height: 16),
                                        Text(
                                          'No nodes found',
                                          style: Theme.of(context)
                                              .textTheme
                                              .titleMedium
                                              ?.copyWith(
                                                color: Colors.grey.shade600,
                                              ),
                                        ),
                                      ],
                                    ),
                                  )
                                : ListView.builder(
                                    padding: const EdgeInsets.all(8),
                                    itemCount: categories.length,
                                    itemBuilder: (context, categoryIndex) {
                                      final category =
                                          categories[categoryIndex];
                                      final categoryTemplates =
                                          templatesByCategory[category]!;

                                      return Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          // Category header
                                          Padding(
                                            padding: const EdgeInsets.fromLTRB(
                                              12,
                                              16,
                                              12,
                                              8,
                                            ),
                                            child: Row(
                                              children: [
                                                Icon(
                                                  _getCategoryIcon(category),
                                                  size: 16,
                                                  color: _getCategoryColor(
                                                    category,
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                Text(
                                                  category,
                                                  style: Theme.of(context)
                                                      .textTheme
                                                      .titleSmall
                                                      ?.copyWith(
                                                        color:
                                                            _getCategoryColor(
                                                          category,
                                                        ),
                                                        fontWeight:
                                                            FontWeight.bold,
                                                        letterSpacing: 0.5,
                                                      ),
                                                ),
                                              ],
                                            ),
                                          ),

                                          // Category templates
                                          ...categoryTemplates
                                              .asMap()
                                              .entries
                                              .map((
                                            entry,
                                          ) {
                                            final template = entry.value;
                                            final globalIndex =
                                                filteredTemplates.indexOf(
                                              template,
                                            );
                                            final isSelected =
                                                globalIndex == selectedIndex;

                                            return Container(
                                              margin:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 4,
                                                vertical: 1,
                                              ),
                                              decoration: BoxDecoration(
                                                color: isSelected
                                                    ? Theme.of(context)
                                                        .colorScheme
                                                        .primary
                                                        .withAlpha(26)
                                                    : Colors.transparent,
                                                borderRadius:
                                                    BorderRadius.circular(6),
                                                border: isSelected
                                                    ? Border.all(
                                                        color: Theme.of(context)
                                                            .colorScheme
                                                            .primary
                                                            .withAlpha(77),
                                                        width: 1,
                                                      )
                                                    : null,
                                              ),
                                              child: ListTile(
                                                dense: true,
                                                leading: Container(
                                                  width: 32,
                                                  height: 32,
                                                  decoration: BoxDecoration(
                                                    color: template.color
                                                        .toColor()
                                                        .withAlpha(51),
                                                    borderRadius:
                                                        BorderRadius.circular(
                                                      6,
                                                    ),
                                                  ),
                                                  child: Icon(
                                                    _getCategoryIcon(
                                                      template.category,
                                                    ),
                                                    size: 16,
                                                    color: template.color
                                                        .toColor(),
                                                  ),
                                                ),
                                                title: Text(
                                                  template.name,
                                                  style: Theme.of(context)
                                                      .textTheme
                                                      .bodyMedium
                                                      ?.copyWith(
                                                        fontWeight:
                                                            FontWeight.w500,
                                                      ),
                                                ),
                                                subtitle: Text(
                                                  template.description,
                                                  style: Theme.of(context)
                                                      .textTheme
                                                      .bodySmall
                                                      ?.copyWith(
                                                        color: Colors
                                                            .grey.shade600,
                                                      ),
                                                ),
                                                trailing: Row(
                                                  mainAxisSize:
                                                      MainAxisSize.min,
                                                  children: [
                                                    if (template
                                                        .inputs.isNotEmpty)
                                                      Row(
                                                        mainAxisSize:
                                                            MainAxisSize.min,
                                                        children: [
                                                          Icon(
                                                            Icons.input,
                                                            size: 12,
                                                            color: Colors
                                                                .grey.shade500,
                                                          ),
                                                          Text(
                                                            '${template.inputs.length}',
                                                            style: TextStyle(
                                                              fontSize: 10,
                                                              color: Colors.grey
                                                                  .shade500,
                                                            ),
                                                          ),
                                                        ],
                                                      ),
                                                    if (template.inputs
                                                            .isNotEmpty &&
                                                        template
                                                            .outputs.isNotEmpty)
                                                      const SizedBox(width: 8),
                                                    if (template
                                                        .outputs.isNotEmpty)
                                                      Row(
                                                        mainAxisSize:
                                                            MainAxisSize.min,
                                                        children: [
                                                          Icon(
                                                            Icons.output,
                                                            size: 12,
                                                            color: Colors
                                                                .grey.shade500,
                                                          ),
                                                          Text(
                                                            '${template.outputs.length}',
                                                            style: TextStyle(
                                                              fontSize: 10,
                                                              color: Colors.grey
                                                                  .shade500,
                                                            ),
                                                          ),
                                                        ],
                                                      ),
                                                  ],
                                                ),
                                                onTap: () =>
                                                    _selectTemplate(template),
                                              ),
                                            );
                                          }),
                                        ],
                                      );
                                    },
                                  ),
                          ),

                          // Footer with instructions
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Theme.of(context)
                                  .colorScheme
                                  .surfaceContainerHighest
                                  .withAlpha(77),
                              borderRadius: const BorderRadius.only(
                                bottomLeft: Radius.circular(12),
                                bottomRight: Radius.circular(12),
                              ),
                            ),
                            child: Row(
                              children: [
                                Icon(
                                  Icons.keyboard_arrow_up,
                                  size: 16,
                                  color: Colors.grey.shade600,
                                ),
                                Icon(
                                  Icons.keyboard_arrow_down,
                                  size: 16,
                                  color: Colors.grey.shade600,
                                ),
                                Text(
                                  ' Navigate',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey.shade600,
                                  ),
                                ),
                                const SizedBox(width: 16),
                                Icon(
                                  Icons.keyboard_return,
                                  size: 16,
                                  color: Colors.grey.shade600,
                                ),
                                Text(
                                  ' Select',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey.shade600,
                                  ),
                                ),
                                const SizedBox(width: 16),
                                Text(
                                  'Esc',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey.shade600,
                                  ),
                                ),
                                Text(
                                  ' Cancel',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey.shade600,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
