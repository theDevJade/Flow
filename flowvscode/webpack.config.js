const path = require('path');

module.exports = {
    entry: './src/extension.ts',
    target: 'node',
    mode: 'production',
    externals: {
        vscode: 'commonjs vscode'
    },
    resolve: {
        extensions: ['.ts', '.js']
    },
    module: {
        rules: [
            {
                test: /\.ts$/,
                exclude: /node_modules/,
                use: [
                    {
                        loader: 'ts-loader'
                    }
                ]
            }
        ]
    },
    output: {
        path: path.resolve(__dirname, 'out'),
        filename: 'extension.js',
        libraryTarget: 'commonjs2'
    },
    optimization: {
        minimize: false
    }
};
