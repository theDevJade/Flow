import * as path from 'path';
import {ExtensionContext, window, workspace} from 'vscode';
import {
    Executable,
    ExecutableOptions,
    LanguageClient,
    LanguageClientOptions,
    ServerOptions
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export function activate(context: ExtensionContext) {
    console.log('Flow language extension is now active');


    const config = workspace.getConfiguration('flow');
    let serverPath = config.get<string>('languageServer.path');

    if (!serverPath) {

        const workspaceFolders = workspace.workspaceFolders;
        if (workspaceFolders && workspaceFolders.length > 0) {

            const workspaceRoot = workspaceFolders[0].uri.fsPath;
            const possiblePath = path.join(workspaceRoot, 'flowbase', 'build', 'flow-lsp');
            serverPath = possiblePath;
        } else {

            serverPath = 'flow-lsp';
        }
    }

    console.log(`Using Flow language server at: ${serverPath}`);


    const serverExecutable: Executable = {
        command: serverPath,
        args: [],
        options: {
            env: process.env
        } as ExecutableOptions
    };


    const serverOptions: ServerOptions = {
        run: serverExecutable,
        debug: serverExecutable
    };


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
        revealOutputChannelOn: 2 // RevealOutputChannelOn.Error
    };


    client = new LanguageClient(
        'flowLanguageServer',
        'Flow Language Server',
        serverOptions,
        clientOptions
    );


    client.start().then(() => {
        console.log('Flow language server started successfully');
    }).catch((error) => {
        window.showErrorMessage(`Failed to start Flow language server: ${error.message}`);
        console.error('Error starting Flow language server:', error);
    });


    context.subscriptions.push({
        dispose: () => {
            if (client) {
                return client.stop();
            }
        }
    });
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}

