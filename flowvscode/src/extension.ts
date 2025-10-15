import * as path from 'path';
import * as fs from 'fs';
import {ExtensionContext, window, workspace, OutputChannel, commands, Terminal, DiagnosticSeverity, Uri} from 'vscode';
import {
    Executable,
    ExecutableOptions,
    LanguageClient,
    ServerOptions
} from 'vscode-languageclient/lib/node/main';
import { LanguageClientOptions } from 'vscode-languageclient';

let client: LanguageClient | undefined;
let flowOutputChannel: OutputChannel | undefined;

function log(message: string, level: 'info' | 'warn' | 'error' = 'info') {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] [${level.toUpperCase()}] ${message}`;

    if (flowOutputChannel) {
        flowOutputChannel.appendLine(logMessage);
    }

    // Also log to console for debugging
    console.log(logMessage);
}

export function activate(context: ExtensionContext) {
    log('Flow language extension activation started');


    flowOutputChannel = window.createOutputChannel('Flow Language Server');
    flowOutputChannel.show();
    log('Flow output channel created and shown');


    const showOutputCommand = commands.registerCommand('flow.showOutputChannel', () => {
        if (flowOutputChannel) {
            flowOutputChannel.show();
            log('Output channel shown via command');
        }
    });

    const restartServerCommand = commands.registerCommand('flow.restartLanguageServer', async () => {
        log('Restarting Flow language server via command');
        if (client) {
            try {
                await client.stop();
                log('Language server stopped for restart');
                await client.start();
                log('Language server restarted successfully');
                window.showInformationMessage('Flow Language Server restarted successfully!');
            } catch (error) {
                log(`Error restarting language server: ${error}`, 'error');
                window.showErrorMessage(`Failed to restart Flow language server: ${error}`);
            }
        } else {
            log('No client to restart', 'warn');
            window.showWarningMessage('No Flow language server to restart');
        }
    });

    const runFileCommand = commands.registerCommand('flow.runFile', async () => {
        const activeEditor = window.activeTextEditor;
        if (!activeEditor) {
            window.showWarningMessage('No active editor found');
            return;
        }

        const filePath = activeEditor.document.uri.fsPath;
        if (!filePath.endsWith('.flow')) {
            window.showWarningMessage('Current file is not a Flow file');
            return;
        }

        log(`Running Flow file: ${filePath}`);

        try {

            const riverTomlPath = findRiverToml(filePath);
            if (riverTomlPath) {
                await runWithRiver(filePath, riverTomlPath);
            } else {
                await runWithFlowbase(filePath);
            }
        } catch (error) {
            log(`Error running Flow file: ${error}`, 'error');
            window.showErrorMessage(`Failed to run Flow file: ${error}`);
        }
    });

    const checkSyntaxCommand = commands.registerCommand('flow.checkSyntax', async () => {
        window.showInformationMessage('✅ Flow syntax checking is now automatic! The LSP provides real-time diagnostics as you type.');
        log('User requested syntax check - informed that LSP provides real-time diagnostics');
    });

    context.subscriptions.push(showOutputCommand, restartServerCommand, runFileCommand, checkSyntaxCommand);
    log('Commands registered successfully');

    try {
        const config = workspace.getConfiguration('flow');
        log('Configuration loaded successfully');

        let serverPath = config.get<string>('languageServer.path');
        log(`Initial server path from config: ${serverPath || 'not set'}`);

        if (!serverPath) {
            log('No server path in config, attempting to find flow-lsp');

            const workspaceFolders = workspace.workspaceFolders;
            if (workspaceFolders && workspaceFolders.length > 0) {
                const workspaceRoot = workspaceFolders[0].uri.fsPath;
                const possiblePath = path.join(workspaceRoot, 'flowbase', 'build', 'flow-lsp');
                serverPath = possiblePath;
                log(`Using workspace-relative path: ${serverPath}`);
            } else {
                serverPath = 'flow-lsp';
                log('No workspace folders, using system PATH to find flow-lsp');
            }
        }

        log(`Final server path: ${serverPath}`);

        // Check if the server executable exists
        const fs = require('fs');
        try {
            if (fs.existsSync(serverPath)) {
                log(`Server executable found at: ${serverPath}`);
            } else {
                log(`WARNING: Server executable not found at: ${serverPath}`, 'warn');
            }
        } catch (error) {
            log(`Error checking server path: ${error}`, 'error');
        }

        const serverExecutable: Executable = {
            command: serverPath,
            args: [],
            options: {
                env: process.env
            } as ExecutableOptions
        };

        log('Server executable configuration created');

        const serverOptions: ServerOptions = {
            run: serverExecutable,
            debug: serverExecutable
        };

        log('Server options configured');

        const clientOptions: LanguageClientOptions = {
            documentSelector: [
                {
                    scheme: 'file',
                    language: 'flow'
                }
            ],
            synchronize: {
                configurationSection: 'flow',
                fileEvents: workspace.createFileSystemWatcher('**/*.flow')
            },
            outputChannelName: 'Flow Language Server',
            revealOutputChannelOn: 2, // RevealOutputChannelOn.Error
            // Add diagnostic logging
            diagnosticCollectionName: 'flow',
            middleware: {
                handleDiagnostics: (uri, diagnostics, next) => {
                    log(`LSP Diagnostics for ${uri}: ${diagnostics.length} diagnostics`);
                    diagnostics.forEach((diag, index) => {
                        log(`  Diagnostic ${index + 1}: ${diag.message} at line ${diag.range.start.line + 1}, col ${diag.range.start.character + 1}`);
                    });
                    return next(uri, diagnostics);
                }
            }
        };

        log('Client options configured');

        client = new LanguageClient(
            'flowLanguageServer',
            'Flow Language Server',
            serverOptions,
            clientOptions
        );

        log('Language client created');

        client.start().then(() => {
            log('Flow language server started successfully');
            window.showInformationMessage('Flow Language Server started successfully!');
        }).catch((error: any) => {
            log(`Failed to start Flow language server: ${error.message}`, 'error');
            log(`Error details: ${JSON.stringify(error)}`, 'error');
            window.showErrorMessage(`Failed to start Flow language server: ${error.message}`);
            console.error('Error starting Flow language server:', error);
        });

        context.subscriptions.push({
            dispose: () => {
                log('Extension disposing, stopping language server');
                if (client) {
                    return client.stop();
                }
            }
        });

        log('Flow language extension activation completed successfully');

    } catch (error) {
        log(`Error during extension activation: ${error}`, 'error');
        window.showErrorMessage(`Flow extension activation failed: ${error}`);
    }
}

export function deactivate(): Thenable<void> | undefined {
    log('Flow language extension deactivating');
    if (!client) {
        log('No client to stop');
        return undefined;
    }
    log('Stopping Flow language server');
    return client.stop();
}

// Helper functions
function findRiverToml(filePath: string): string | null {
    let currentDir = path.dirname(filePath);
    const rootDir = path.parse(filePath).root;

    while (currentDir !== rootDir) {
        const riverTomlPath = path.join(currentDir, 'River.toml');
        if (fs.existsSync(riverTomlPath)) {
            return riverTomlPath;
        }
        currentDir = path.dirname(currentDir);
    }

    return null;
}

async function runWithRiver(filePath: string, riverTomlPath: string): Promise<void> {
    const packageDir = path.dirname(riverTomlPath);
    const terminal = window.createTerminal('Flow Runner');

    terminal.show();
    terminal.sendText(`cd "${packageDir}"`);
    terminal.sendText('river run');

    log(`Running with River from: ${packageDir}`);
    window.showInformationMessage('Running Flow file with River package manager');
}

async function runWithFlowbase(filePath: string): Promise<void> {
    const flowbasePath = '/Users/alyx/Flow/flowbase/build/flowbase';

    if (!fs.existsSync(flowbasePath)) {
        window.showErrorMessage('Flow compiler not found. Please build flowbase first.');
        return;
    }

    const terminal = window.createTerminal('Flow Runner');
    terminal.show();
    terminal.sendText(`"${flowbasePath}" "${filePath}"`);

    log(`Running with Flow compiler: ${filePath}`);
    window.showInformationMessage('Running Flow file with Flow compiler');
}




