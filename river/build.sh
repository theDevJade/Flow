#!/bin/bash

echo "╔══════════════════════════════════════╗"
echo "║  Building River Package Manager      ║"
echo "╚══════════════════════════════════════╝"
echo ""

# Check if cargo is available
if ! command -v cargo &> /dev/null; then
    echo "✗ Error: Cargo not found"
    echo "  Please install Rust: https://rustup.rs/"
    exit 1
fi

echo "→ Building River..."
cargo build --release

if [ $? -eq 0 ]; then
    echo ""
    echo "╔══════════════════════════════════════╗"
    echo "║  ✓ Build successful!                 ║"
    echo "╚══════════════════════════════════════╝"
    echo ""
    echo "Binary location: target/release/river"
    echo ""
    echo "Add to PATH:"
    echo "  export PATH=\"$(pwd)/target/release:\$PATH\""
    echo ""
    echo "Or install globally:"
    echo "  cargo install --path ."
else
    echo ""
    echo "✗ Build failed"
    exit 1
fi

