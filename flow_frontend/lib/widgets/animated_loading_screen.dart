import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../services/project_stats_service.dart';
import '../services/update_check_service.dart';

class LoadingStep {
  final String message;
  final int durationMs;

  LoadingStep(this.message, this.durationMs);
}

class AnimatedLoadingScreen extends StatefulWidget {
  final VoidCallback onLoadingComplete;

  const AnimatedLoadingScreen({super.key, required this.onLoadingComplete});

  @override
  State<AnimatedLoadingScreen> createState() => _AnimatedLoadingScreenState();
}

class _AnimatedLoadingScreenState extends State<AnimatedLoadingScreen>
    with TickerProviderStateMixin {
  late AnimationController _logoController;

  double _progress = 0.0;
  String _currentStep = 'Initializing...';

  // Enhanced data
  ProjectStats? _projectStats;
  UpdateInfo? _updateInfo;
  String _greeting = '';
  bool _showStats = false;

  final List<LoadingStep> _steps = [
    LoadingStep('Initializing Flow...', 800),
    LoadingStep('Scanning project files...', 1200),
    LoadingStep('Calculating project statistics...', 1000),
    LoadingStep('Loading authentication system...', 800),
    LoadingStep('Checking for updates...', 1000),
    LoadingStep('Connecting to WebSocket...', 800),
    LoadingStep('Loading node templates...', 600),
    LoadingStep('Setting up workspace...', 600),
    LoadingStep('Preparing UI components...', 500),
    LoadingStep('Finalizing setup...', 400),
  ];

  @override
  void initState() {
    super.initState();

    _logoController = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    );

    _startLoadingSequence();
  }

  @override
  void dispose() {
    _logoController.dispose();
    super.dispose();
  }

  void _startLoadingSequence() {
    // Start logo animation immediately
    _logoController.forward();

    // Schedule progress updates
    _scheduleProgressUpdates();
  }

  void _scheduleProgressUpdates() {
    int currentIndex = 0;

    Future<void> updateStep() async {
      if (!mounted || currentIndex >= _steps.length) {
        if (mounted) {
          // Show greeting and final stats before completing
          setState(() {
            _currentStep = _generateGreeting();
            _showStats = true;
            _progress = 1.0;
          });

          // Show greeting for a moment
          await Future.delayed(const Duration(milliseconds: 1500));

          if (mounted) {
            widget.onLoadingComplete();
          }
        }
        return;
      }

      final step = _steps[currentIndex];
      setState(() {
        _currentStep = step.message;
        _progress = (currentIndex + 1) / _steps.length;
      });

      // Perform actual initialization tasks based on step
      await _performStepAction(currentIndex, step);

      currentIndex++;

      // Schedule next update with step-specific timing
      if (mounted && currentIndex < _steps.length) {
        Future.delayed(Duration(milliseconds: step.durationMs), updateStep);
      } else if (mounted) {
        // Final step
        Future.delayed(Duration(milliseconds: step.durationMs), updateStep);
      }
    }

    // Start the sequence after a small delay for logo animation
    Future.delayed(const Duration(milliseconds: 600), updateStep);
  }

  Future<void> _performStepAction(int stepIndex, LoadingStep step) async {
    try {
      switch (stepIndex) {
        case 0: // Initializing Flow
          final appState = Provider.of<AppState>(context, listen: false);
          await appState.initialize();
          break;

        case 1: // Scanning project files
          // Start project stats calculation
          break;

        case 2: // Calculating project statistics
          _projectStats = await ProjectStatsService().getProjectStats();
          break;

        case 3: // Loading authentication system
          // Authentication already handled in app state
          break;

        case 4: // Checking for updates
          _updateInfo = await UpdateCheckService().checkForUpdates();
          break;

        case 5: // Connecting to WebSocket
        case 6: // Loading node templates
        case 7: // Setting up workspace
        case 8: // Preparing UI components
        case 9: // Finalizing setup
          // These are simulated steps
          break;
      }
    } catch (e) {
      print('Error in loading step ${step.message}: $e');
      // Continue with loading even if a step fails
    }
  }

  String _generateGreeting() {
    final hour = DateTime.now().hour;
    final timeGreeting = hour < 12
        ? 'Good morning'
        : hour < 17
        ? 'Good afternoon'
        : 'Good evening';

    final projectName =
        _projectStats?.projectPath.split('/').last ?? 'Flow Project';

    return '$timeGreeting! Welcome to $projectName';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0A), // Deep dark background
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF0A0A0A),
              const Color(0xFF1A1A2E),
              const Color(0xFF0A0A0A),
            ],
          ),
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Logo and title
              _buildLogo(),

              const SizedBox(height: 80),

              // Progress section
              _buildProgressSection(),

              // Stats and update info (shown after loading completes)
              if (_showStats) ...[
                const SizedBox(height: 40),
                _buildStatsSection(),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLogo() {
    return AnimatedBuilder(
      animation: _logoController,
      builder: (context, child) {
        return Column(
          children: [
            // Animated logo
            Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      colors: [
                        Colors.blue.withOpacity(0.8),
                        Colors.purple.withOpacity(0.8),
                        Colors.pink.withOpacity(0.8),
                      ],
                      stops: [0.0, 0.5, 1.0],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.blue.withOpacity(0.3),
                        blurRadius: 20,
                        spreadRadius: 5,
                      ),
                    ],
                  ),
                  child: Icon(
                    Icons.dynamic_form,
                    size: 60,
                    color: Colors.white,
                  ),
                )
                .animate(controller: _logoController)
                .scale(
                  begin: const Offset(0.0, 0.0),
                  end: const Offset(1.0, 1.0),
                  curve: Curves.elasticOut,
                )
                .fade(begin: 0.0, end: 1.0, curve: Curves.easeOut),

            const SizedBox(height: 24),

            // App title
            Text(
                  'Flow',
                  style: TextStyle(
                    fontSize: 48,
                    fontWeight: FontWeight.bold,
                    foreground: Paint()
                      ..shader =
                          LinearGradient(
                            colors: [Colors.blue, Colors.purple, Colors.pink],
                          ).createShader(
                            const Rect.fromLTWH(0.0, 0.0, 200.0, 70.0),
                          ),
                  ),
                )
                .animate(controller: _logoController)
                .slideY(
                  begin: 50,
                  end: 0,
                  delay: const Duration(milliseconds: 400),
                  curve: Curves.easeOut,
                )
                .fade(
                  begin: 0.0,
                  end: 1.0,
                  delay: const Duration(milliseconds: 400),
                  curve: Curves.easeOut,
                ),

            const SizedBox(height: 8),

            // Subtitle
            Text(
                  'Visual Workflow Editor',
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.white.withOpacity(0.7),
                    letterSpacing: 2.0,
                  ),
                )
                .animate(controller: _logoController)
                .slideY(
                  begin: 30,
                  end: 0,
                  delay: const Duration(milliseconds: 800),
                  curve: Curves.easeOut,
                )
                .fade(
                  begin: 0.0,
                  end: 1.0,
                  delay: const Duration(milliseconds: 800),
                  curve: Curves.easeOut,
                ),
          ],
        );
      },
    );
  }

  Widget _buildProgressSection() {
    return Column(
      children: [
        // Current step text
        AnimatedSwitcher(
          duration: Duration(milliseconds: 300),
          child: Container(
            key: ValueKey(_currentStep),
            width: 300,
            alignment: Alignment.center,
            child: Text(
              _currentStep,
              style: TextStyle(
                fontSize: 14,
                color: Colors.white.withOpacity(0.8),
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ),

        const SizedBox(height: 24),

        // Progress bar
        Container(
          width: 280,
          height: 6,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(3),
            color: Colors.white.withOpacity(0.1),
          ),
          child: Stack(
            children: [
              // Background
              Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(3),
                  color: Colors.white.withOpacity(0.05),
                ),
              ),
              // Progress
              AnimatedContainer(
                duration: Duration(milliseconds: 500),
                curve: Curves.easeInOut,
                width: 280 * _progress,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(3),
                  gradient: LinearGradient(
                    colors: [Colors.blue, Colors.purple, Colors.pink],
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.blue.withOpacity(0.3),
                      blurRadius: 8,
                      spreadRadius: 1,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: 16),

        // Progress percentage
        AnimatedDefaultTextStyle(
          duration: Duration(milliseconds: 300),
          style: TextStyle(
            fontSize: 12,
            color: Colors.white.withOpacity(0.6),
            fontWeight: FontWeight.w400,
          ),
          child: Text('${(_progress * 100).round()}%'),
        ),
      ],
    );
  }

  Widget _buildStatsSection() {
    return Column(
      children: [
        // Project stats
        if (_projectStats != null) ...[
          Container(
            padding: const EdgeInsets.all(16),
            margin: const EdgeInsets.symmetric(horizontal: 40),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              color: Colors.white.withOpacity(0.05),
              border: Border.all(color: Colors.white.withOpacity(0.1)),
            ),
            child: Column(
              children: [
                Text(
                  'Project Statistics',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.white.withOpacity(0.9),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    _buildStatItem(
                      'Lines of Code',
                      _projectStats!.formattedLinesOfCode,
                      Icons.code,
                    ),
                    _buildStatItem(
                      'Files',
                      _projectStats!.totalFiles.toString(),
                      Icons.description,
                    ),
                    _buildStatItem(
                      'Primary Language',
                      _projectStats!.topLanguage,
                      Icons.language,
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
        ],

        // Update info
        if (_updateInfo != null) ...[
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            margin: const EdgeInsets.symmetric(horizontal: 40),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              color: _updateInfo!.hasUpdate
                  ? Colors.blue.withOpacity(0.1)
                  : Colors.green.withOpacity(0.1),
              border: Border.all(
                color: _updateInfo!.hasUpdate
                    ? Colors.blue.withOpacity(0.3)
                    : Colors.green.withOpacity(0.3),
              ),
            ),
            child: Row(
              children: [
                Icon(
                  _updateInfo!.hasUpdate
                      ? Icons.system_update
                      : Icons.check_circle,
                  color: _updateInfo!.hasUpdate ? Colors.blue : Colors.green,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _updateInfo!.updateStatusText,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.white.withOpacity(0.8),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.white.withOpacity(0.7), size: 20),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: Colors.white.withOpacity(0.9),
          ),
        ),
        Text(
          label,
          style: TextStyle(fontSize: 10, color: Colors.white.withOpacity(0.6)),
        ),
      ],
    );
  }
}
