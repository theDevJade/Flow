# Lake - The Flow Toolchain Installer

Lake is the official installer for the Flow programming language, inspired by Rust's `rustup`.

## Quick Install

```bash
curl --proto '=https' --tlsv1.2 -sSf https://lake.flow-lang.org/init | sh
```

Or download and run manually:

```bash
curl -O https://lake.flow-lang.org/lake-init
chmod +x lake-init
./lake-init
```

## What is Lake?

Lake is a toolchain manager for Flow that:

- 📦 **Downloads prebuilt binaries** when available (fast!)
- 🔨 **Builds from source** as a fallback
- 🔄 **Manages multiple Flow versions**
- ⚡ **Updates automatically**
- 🎯 **Just works** - no configuration needed

## Usage

After installation, use `lake` to manage your Flow toolchain:

```bash
# Show help
lake help

# Show installed toolchains
lake show

# Update to latest
lake update

# Install specific version
lake toolchain install 0.2.0

# Switch default version
lake default 0.2.0

# Uninstall everything
lake uninstall

# Update lake itself
lake self update
```

## Commands

### Main Commands

- `lake install` - Install Flow toolchain (first time)
- `lake uninstall` - Remove all Flow installations
- `lake update` - Update to latest version
- `lake show` - Display installed toolchains
- `lake default <version>` - Set default toolchain

### Toolchain Management

- `lake toolchain install <version>` - Install a specific version
- `lake toolchain list` - List installed toolchains
- `lake toolchain uninstall <version>` - Remove a toolchain

### Utility Commands

- `lake self update` - Update lake itself
- `lake completions <shell>` - Generate shell completions
- `lake help` - Show detailed help

## Installation Process

Lake tries to install in this order:

1. **Download prebuilt binaries** from GitHub releases
    - Fastest option (~10 seconds)
    - Works for most common platforms

2. **Build from source** if prebuilts unavailable
    - Takes 2-5 minutes
    - Requires: LLVM, CMake, Rust, Git
    - Automatically checks dependencies

## Directory Structure

Lake installs everything to `~/.flow/`:

```
~/.flow/
├── bin/                  # Active binaries (symlinks)
│   ├── flow             # Flow compiler
│   ├── flow-lsp         # Language server
│   ├── river            # Package manager
│   └── lake             # Lake itself
├── toolchains/           # Installed versions
│   ├── 0.1.0/
│   └── 0.2.0/
├── downloads/            # Cached downloads
├── src/                  # Source builds
├── env                   # Environment setup
└── default               # Default version marker
```

## Environment Setup

Lake automatically:

- Creates `~/.flow/env` with environment variables
- Adds to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.)
- Sets up PATH to include `~/.flow/bin`

To manually activate:

```bash
source ~/.flow/env
```

## Platform Support

### Prebuilt Binaries Available

- **macOS**: x86_64 (Intel), aarch64 (Apple Silicon)
- **Linux**: x86_64 (GNU), aarch64
- **Windows**: Coming soon

### Source Build Supported

All platforms with:

- LLVM 12+
- CMake 3.15+
- C++17 compiler (GCC 7+, Clang 5+, MSVC 2019+)
- Rust 1.70+
- Git

## Examples

### First-Time Setup

```bash
# Install
curl -sSf https://lake.flow-lang.org/init | sh

# Activate (or restart terminal)
source ~/.flow/env

# Verify
flow --version
river --version
```

### Managing Versions

```bash
# Install multiple versions
lake toolchain install 0.1.0
lake toolchain install 0.2.0
lake toolchain install latest

# List installed
lake show

# Switch between versions
lake default 0.2.0

# Update to latest
lake update
```

### Development Setup

```bash
# Install lake
lake install

# Set up development version (from local source)
cd ~/flow-project
lake toolchain install --path .

# Use it
lake default dev
```

## Shell Completions

Generate completions for your shell:

```bash
# Bash
lake completions bash >> ~/.bashrc

# Zsh
lake completions zsh >> ~/.zshrc

# Fish
lake completions fish > ~/.config/fish/completions/lake.fish
```

## Updating

### Update Flow Toolchain

```bash
lake update
```

### Update Lake Itself

```bash
lake self update
```

## Uninstalling

```bash
# Remove everything
lake uninstall

# Manually remove (if needed)
rm -rf ~/.flow
# Remove the line from ~/.bashrc or ~/.zshrc that sources ~/.flow/env
```

## Troubleshooting

### "Command not found: lake"

```bash
# Make sure ~/.flow/bin is in PATH
echo 'export PATH="$HOME/.flow/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

### "Failed to download prebuilt binaries"

Lake will automatically build from source. Make sure you have:

```bash
# macOS
brew install llvm cmake rust

# Ubuntu/Debian
sudo apt install build-essential cmake llvm-dev libllvm-dev clang cargo

# Fedora
sudo dnf install llvm-devel cmake gcc-c++ cargo
```

### "Build failed"

Check LLVM version:

```bash
llvm-config --version  # Should be 12+
```

Make sure all dependencies are installed.

### Proxy Issues

Set these environment variables before running:

```bash
export http_proxy=http://proxy.example.com:8080
export https_proxy=http://proxy.example.com:8080
```

## Comparison with Other Tools

| Feature           | Lake | Rustup | NVM | pyenv |
|-------------------|------|--------|-----|-------|
| Prebuilt binaries | ✓    | ✓      | ✓   | ✗     |
| Source builds     | ✓    | ✓      | ✗   | ✓     |
| Multiple versions | ✓    | ✓      | ✓   | ✓     |
| Auto PATH setup   | ✓    | ✓      | ✓   | ✓     |
| Shell completions | ✓    | ✓      | ✓   | ✓     |
| Self-update       | ✓    | ✓      | ✗   | ✗     |

## Advanced Usage

### Custom Installation Directory

```bash
export FLOW_HOME=/opt/flow
curl -sSf https://lake.flow-lang.org/init | sh
```

### Offline Installation

```bash
# Download on internet-connected machine
curl -O https://lake.flow-lang.org/flow-latest-x86_64-apple-darwin.tar.gz

# Transfer to offline machine
scp flow-*.tar.gz offline-machine:~/

# Install manually
mkdir -p ~/.flow/bin
tar -xzf flow-*.tar.gz -C ~/.flow/bin
```

### CI/CD Integration

```bash
# .github/workflows/test.yml
- name: Install Flow
  run: |
    curl -sSf https://lake.flow-lang.org/init | sh -s -- -y
    source ~/.flow/env
    flow --version
```

## Contributing

Lake is part of the Flow language project. Contribute at:
https://github.com/flow-lang/flow

## License

MIT License - See the Flow repository for details.

---

**Get started:** https://flow-lang.org  
**Documentation:** https://docs.flow-lang.org  
**Community:** https://discord.gg/flow-lang
