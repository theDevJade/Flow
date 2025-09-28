import 'package:flow_frontend/services/file_system_service.dart';
import 'package:flow_frontend/services/websocket_service.dart';
import 'package:flow_frontend/services/flutter_log_service.dart';
import 'package:flow_frontend/services/flowlang_execution_service.dart';
import 'package:flow_frontend/state/file_system_state.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'state/app_state.dart';
import 'state/workspace_state.dart';
import 'state/workspace_manager.dart';
import 'services/persistence_service.dart';
import 'widgets/main_app_shell.dart';
import 'screens/login_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await PersistenceService.instance.initialize();
  await FileSystemService.instance.initialize();

  runApp(const FlowApp());
}

class FlowApp extends StatelessWidget {
  const FlowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<PersistenceService>(
          create: (_) => PersistenceService.instance,
        ),
        Provider<FileSystemService>(create: (_) => FileSystemService.instance),
        ChangeNotifierProvider<WebSocketService>(
          create: (_) => WebSocketService.instance,
        ),
        ChangeNotifierProvider<FlutterLogService>(
          create: (_) => FlutterLogService.instance,
        ),
        ChangeNotifierProvider<FlowLangExecutionService>(
          create: (_) => FlowLangExecutionService.instance,
        ),
        ChangeNotifierProvider(create: (_) => AppState()),
        ChangeNotifierProxyProvider<AppState, WorkspaceState>(
          create: (_) => WorkspaceState(),
          update: (_, appState, workspaceState) =>
              workspaceState ?? WorkspaceState(),
        ),
        ChangeNotifierProxyProvider<AppState, WorkspaceManager>(
          create: (context) => context.read<AppState>().workspaceManager,
          update: (_, appState, workspaceManager) => appState.workspaceManager,
        ),
        ChangeNotifierProxyProvider<AppState, FileSystemState>(
          create: (context) => context.read<AppState>().fileSystemState,
          update: (_, appState, fileSystemState) => appState.fileSystemState,
        ),
      ],
      child: MaterialApp(
        title: 'Flow - Graph & Code Editor',
        debugShowCheckedModeBanner: false,
        theme: _buildDarkTheme(),
        home: const AuthenticationWrapper(),
        routes: {
          '/login': (context) => const LoginScreen(),
          '/home': (context) => const MainAppShell(),
        },
      ),
    );
  }

  ThemeData _buildDarkTheme() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF6C63FF),
        brightness: Brightness.dark,
      ).copyWith(
        surface: const Color(0xFF1A1A1A),
        background: const Color(0xFF0F0F0F),
        surfaceVariant: const Color(0xFF2A2A2A),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Color(0xFF1A1A1A),
        elevation: 0,
      ),
      cardTheme: const CardThemeData(color: Color(0xFF1A1A1A), elevation: 2),
      dividerColor: const Color(0xFF333333),
      textTheme: const TextTheme().apply(
        bodyColor: Colors.white,
        displayColor: Colors.white,
      ),
      fontFamily: 'SF Pro Text',
    );
  }
}

class AuthenticationWrapper extends StatelessWidget {
  const AuthenticationWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, child) {
        if (appState.authService.isAuthenticated) {
          return const MainAppShell();
        } else {
          return const LoginScreen();
        }
      },
    );
  }
}
