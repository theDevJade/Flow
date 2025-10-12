import * as assert from 'assert';
import * as vscode from 'vscode';

suite('Flow Extension Test Suite', () => {
    vscode.window.showInformationMessage('Start all tests.');

    test('Extension should be present', () => {
        assert.ok(vscode.extensions.getExtension('flow-lang.flow-lang'));
    });

    test('Extension should activate', async () => {
        const ext = vscode.extensions.getExtension('flow-lang.flow-lang');
        assert.ok(ext);
        await ext!.activate();
        assert.strictEqual(ext!.isActive, true);
    });

    test('Flow language should be registered', () => {
        const languages = vscode.languages.getLanguages();
        return languages.then((langs) => {
            assert.ok(langs.includes('flow'), 'Flow language not found in registered languages');
        });
    });

    test('Should recognize .flow files', async () => {
        // Create a simple flow document
        const doc = await vscode.workspace.openTextDocument({
            language: 'flow',
            content: 'fn main() {\n  println("Hello, World!");\n}'
        });

        assert.strictEqual(doc.languageId, 'flow');
    });

    test('Extension configuration should be available', () => {
        const config = vscode.workspace.getConfiguration('flow');
        assert.ok(config !== undefined);

        // Check if our configuration options exist
        const lspPath = config.get('languageServer.path');
        assert.ok(lspPath !== undefined, 'LSP path configuration missing');

        const traceServer = config.get('trace.server');
        assert.ok(traceServer !== undefined, 'Trace server configuration missing');
    });
});


