import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/persistence_service.dart';

class SettingsDialog extends StatefulWidget {
  final String? currentHost;
  final int? currentPort;
  final bool showIntroSplash;
  final Function(String host, int port, bool showIntro) onSettingsChanged;

  const SettingsDialog({
    super.key,
    this.currentHost,
    this.currentPort,
    this.showIntroSplash = true,
    required this.onSettingsChanged,
  });

  @override
  State<SettingsDialog> createState() => _SettingsDialogState();
}

class _SettingsDialogState extends State<SettingsDialog> {
  late TextEditingController _hostController;
  late TextEditingController _portController;
  late bool _showIntroSplash;

  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _hostController = TextEditingController(
      text: widget.currentHost ?? 'localhost',
    );
    _portController = TextEditingController(
      text: (widget.currentPort ?? 8080).toString(),
    );
    _showIntroSplash = widget.showIntroSplash;
  }

  @override
  void dispose() {
    _hostController.dispose();
    _portController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Row(
        children: [
          Icon(Icons.settings, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 12),
          const Text('Settings'),
        ],
      ),
      content: SizedBox(
        width: 400,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [

              Text(
                'WebSocket Configuration',
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),


              TextFormField(
                controller: _hostController,
                decoration: const InputDecoration(
                  labelText: 'Host',
                  hintText: 'localhost',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.computer),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter a host';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),


              TextFormField(
                controller: _portController,
                decoration: const InputDecoration(
                  labelText: 'Port',
                  hintText: '8080',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.settings_input_component),
                ),
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter a port';
                  }
                  final port = int.tryParse(value);
                  if (port == null || port < 1 || port > 65535) {
                    return 'Please enter a valid port (1-65535)';
                  }
                  return null;
                },
              ),

              const SizedBox(height: 24),

              // UI Preferences Section
              Text(
                'UI Preferences',
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),

              // Intro splash toggle
              SwitchListTile(
                title: const Text('Show intro splash screen'),
                subtitle: const Text('Display welcome screen on app startup'),
                value: _showIntroSplash,
                onChanged: (value) {
                  setState(() {
                    _showIntroSplash = value;
                  });
                },
                secondary: const Icon(Icons.play_circle_outline),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        ElevatedButton(onPressed: _saveSettings, child: const Text('Save')),
      ],
    );
  }

  void _saveSettings() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final host = _hostController.text.trim();
    final port = int.parse(_portController.text.trim());

    try {
      // Save settings to persistent storage
      await PersistenceService.instance.saveWebSocketConfig(host, port);

      await PersistenceService.instance.saveAppSettings({
        'showIntroSplash': _showIntroSplash,
      });

      // Call the callback
      widget.onSettingsChanged(host, port, _showIntroSplash);

      if (mounted) {
        Navigator.of(context).pop();

        // Show success message
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Settings saved successfully'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to save settings: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}
