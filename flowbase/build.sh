#!/bin/bash

# Flow Compiler Build Script

set -e  # Exit on error

echo "========================================"
echo "Flow Compiler Build Script"
echo "========================================"
echo ""

# Determine LLVM directory first (before checking for llvm-config)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - try Homebrew location first
    if [ -d "/opt/homebrew/opt/llvm" ]; then
        export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
        export LLVM_DIR="/opt/homebrew/opt/llvm/lib/cmake/llvm"
    elif [ -d "/usr/local/opt/llvm" ]; then
        export PATH="/usr/local/opt/llvm/bin:$PATH"
        export LLVM_DIR="/usr/local/opt/llvm/lib/cmake/llvm"
    elif [ -d "/opt/homebrew/Cellar/llvm" ]; then
        LLVM_PATH=$(find /opt/homebrew/Cellar/llvm -type d -name "bin" | head -1)
        if [ -n "$LLVM_PATH" ]; then
            export PATH="$LLVM_PATH:$PATH"
            export LLVM_DIR=$(dirname "$LLVM_PATH")/lib/cmake/llvm
        fi
    fi
fi

# Check if LLVM is installed
if ! command -v llvm-config &> /dev/null; then
    echo "Error: LLVM not found!"
    echo "Please install LLVM:"
    echo "  macOS: brew install llvm"
    echo "  Ubuntu: sudo apt-get install llvm-dev"
    echo "  Fedora: sudo dnf install llvm-devel"
    exit 1
fi

LLVM_VERSION=$(llvm-config --version)
echo "Found LLVM version: $LLVM_VERSION"
echo "LLVM_DIR: $LLVM_DIR"
echo ""

# Create build directory
if [ -d "build" ]; then
    echo "Cleaning old build directory..."
    rm -rf build
fi

mkdir -p build
cd build

# Run CMake
echo "Running CMake..."
cmake .. \
    -DCMAKE_BUILD_TYPE=Debug \
    -DCMAKE_EXPORT_COMPILE_COMMANDS=ON

echo ""
echo "Building Flow compiler..."
make -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

echo ""
echo "========================================"
echo "Build complete!"
echo "========================================"
echo ""
echo "Executable: $(pwd)/flowbase"
echo ""
echo "Try running:"
echo "  ./flowbase --help"
echo "  ./flowbase ../examples/hello.flow -v --emit-llvm"
echo ""

