import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

class ConnectionConfigScreen extends StatefulWidget {
  final Function(String host, int port) onConfigureConnection;
  final VoidCallback? onSkip;

  const ConnectionConfigScreen({
    super.key,
    required this.onConfigureConnection,
    this.onSkip,
  });

  @override
  State<ConnectionConfigScreen> createState() => _ConnectionConfigScreenState();
}

class _ConnectionConfigScreenState extends State<ConnectionConfigScreen>
    with TickerProviderStateMixin {
  late AnimationController _animationController;
  late TextEditingController _hostController;
  late TextEditingController _portController;

  bool _isValidating = false;
  String? _errorMessage;

  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );

    // Default values
    _hostController = TextEditingController(text: 'localhost');
    _portController = TextEditingController(text: '9090');

    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    _hostController.dispose();
    _portController.dispose();
    super.dispose();
  }

  String? _validateHost(String? value) {
    if (value == null || value.isEmpty) {
      return 'Please enter a host';
    }

    // Basic validation for hostname/IP
    final hostRegex = RegExp(r'^[a-zA-Z0-9.-]+$');
    if (!hostRegex.hasMatch(value)) {
      return 'Invalid host format';
    }

    return null;
  }

  String? _validatePort(String? value) {
    if (value == null || value.isEmpty) {
      return 'Please enter a port';
    }

    final port = int.tryParse(value);
    if (port == null || port < 1 || port > 65535) {
      return 'Port must be between 1-65535';
    }

    return null;
  }

  void _handleConnect() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isValidating = true;
      _errorMessage = null;
    });

    try {
      final host = _hostController.text.trim();
      final port = int.parse(_portController.text.trim());

      // Call the callback with the configuration
      widget.onConfigureConnection(host, port);
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to configure connection: $e';
        _isValidating = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0A),
      body: SafeArea(
        child: Center(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 480),
            padding: const EdgeInsets.all(32.0),
            child:
                Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Logo and title
                        _buildHeader(),

                        const SizedBox(height: 48),

                        // Connection form
                        _buildConnectionForm(),

                        const SizedBox(height: 32),

                        // Action buttons
                        _buildActionButtons(),

                        const SizedBox(height: 24),

                        // Skip option
                        _buildSkipOption(),
                      ],
                    )
                    .animate(controller: _animationController)
                    .fadeIn(duration: const Duration(milliseconds: 600))
                    .slideY(
                      begin: 0.1,
                      end: 0,
                      duration: const Duration(milliseconds: 800),
                      curve: Curves.easeOutCubic,
                    ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        // Flow logo
        Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF6C63FF), Color(0xFF9C27B0)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF6C63FF).withOpacity(0.3),
                    blurRadius: 20,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: const Icon(
                Icons.hub_outlined,
                color: Colors.white,
                size: 40,
              ),
            )
            .animate(delay: const Duration(milliseconds: 200))
            .scale(duration: const Duration(milliseconds: 600))
            .then()
            .shimmer(duration: const Duration(milliseconds: 1000)),

        const SizedBox(height: 24),

        Text(
              'Flow',
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            )
            .animate(delay: const Duration(milliseconds: 400))
            .fadeIn()
            .slideY(begin: 0.3, end: 0),

        const SizedBox(height: 8),

        Text(
              'Configure WebSocket Connection',
              style: Theme.of(
                context,
              ).textTheme.bodyLarge?.copyWith(color: Colors.grey[400]),
              textAlign: TextAlign.center,
            )
            .animate(delay: const Duration(milliseconds: 600))
            .fadeIn()
            .slideY(begin: 0.3, end: 0),
      ],
    );
  }

  Widget _buildConnectionForm() {
    return Card(
          color: const Color(0xFF1A1A1A),
          elevation: 8,
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'WebSocket Server Configuration',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Host input
                  TextFormField(
                    controller: _hostController,
                    enabled: !_isValidating,
                    decoration: InputDecoration(
                      labelText: 'Host',
                      hintText: 'localhost or IP address',
                      prefixIcon: const Icon(Icons.dns_outlined),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      filled: true,
                      fillColor: const Color(0xFF2A2A2A),
                    ),
                    validator: _validateHost,
                  ),

                  const SizedBox(height: 16),

                  // Port input
                  TextFormField(
                    controller: _portController,
                    enabled: !_isValidating,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      labelText: 'Port',
                      hintText: '9090',
                      prefixIcon: const Icon(Icons.settings_ethernet_outlined),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      filled: true,
                      fillColor: const Color(0xFF2A2A2A),
                    ),
                    validator: _validatePort,
                  ),

                  if (_errorMessage != null) ...[
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.red.withOpacity(0.1),
                        border: Border.all(color: Colors.red.withOpacity(0.3)),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.error_outline,
                            color: Colors.red[400],
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: TextStyle(color: Colors.red[400]),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        )
        .animate(delay: const Duration(milliseconds: 800))
        .fadeIn()
        .slideY(begin: 0.2, end: 0);
  }

  Widget _buildActionButtons() {
    return SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _isValidating ? null : _handleConnect,
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF6C63FF),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              elevation: 8,
            ),
            child: _isValidating
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                : const Text(
                    'Connect',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
          ),
        )
        .animate(delay: const Duration(milliseconds: 1000))
        .fadeIn()
        .slideY(begin: 0.2, end: 0);
  }

  Widget _buildSkipOption() {
    // Skip option removed - WebSocket connection is now required
    return const SizedBox.shrink();
  }
}
