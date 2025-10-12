#!/bin/bash

# Flow VSCode Extension Setup Script

echo "Setting up Flow VSCode Extension..."

# Install dependencies
echo "Installing npm dependencies..."
npm install

if [ $? -ne 0 ]; then
    echo "Error: Failed to install dependencies"
    exit 1
fi

# Compile the extension
echo "Compiling TypeScript..."
npm run compile

if [ $? -ne 0 ]; then
    echo "Error: Failed to compile extension"
    exit 1
fi

echo ""
echo "✓ Extension setup complete!"
echo ""
echo "To use the extension:"
echo "1. Make sure the Flow language server is built:"
echo "   cd ../flowbase && ./build.sh"
echo ""
echo "2. To test the extension in development mode:"
echo "   - Open this folder in VS Code"
echo "   - Press F5 to launch Extension Development Host"
echo ""
echo "3. To package the extension for distribution:"
echo "   npm run package"
echo ""
echo "4. To install the packaged extension:"
echo "   code --install-extension flow-lang-0.1.0.vsix"
echo ""

