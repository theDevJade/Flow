# River - Flow Package Manager

**River** is the official package manager for the Flow programming language. It handles package creation, dependency management, building, and distribution.

---

## Features

- 📦 **Package Management** - Create, build, and publish Flow packages
- 🔗 **Dependency Resolution** - Automatic dependency resolution and installation
- 🏗️ **Build System** - Integrated build system for executables and libraries
- 📚 **Package Types** - Support for both binary (executable) and library packages
- 🌊 **Registry Integration** - (Coming soon) Central package registry
- 🔍 **Package Search** - Find and install packages from the registry

---

## Installation

### Build from Source

```bash
cd river
cargo build --release
```

The binary will be at `target/release/river`.

### Add to PATH

```bash
export PATH="/path/to/Flow/river/target/release:$PATH"
```

---

## Quick Start

### Create a New Package

```bash
# Create a new executable package
river init my-app --kind bin

# Create a new library package
river init my-lib --kind lib
```

### Project Structure

River creates the following structure:

```
my-app/
├── River.toml          # Package manifest
├── .gitignore          # Git ignore file
├── README.md           # Project documentation
└── src/
    └── main.flow       # Entry point (for bin)
    └── lib.flow        # Entry point (for lib)
```

### Package Manifest (River.toml)

```toml
[package]
name = "my-app"
version = "0.1.0"
authors = ["Your Name <you@example.com>"]
license = "MIT"
type = "bin"
entry = "src/main.flow"

[dependencies]
# Add dependencies here
# http = "1.0.0"
# json = { version = "2.0", optional = true }
# local-lib = { path = "../local-lib" }
```

---

## Commands

### `river init`

Initialize a new Flow package.

```bash
river init <name> [--kind <bin|lib>]
```

**Options:**
- `--kind`, `-k`: Package type - `bin` for executable, `lib` for library (default: `bin`)

**Examples:**
```bash
river init my-app                    # Create executable in ./my-app
river init my-lib --kind lib         # Create library
river init .                         # Initialize in current directory
```

### `river build`

Build the current package.

```bash
river build
```

Compiles the package and places the output in `target/`.

**Output:**
- **Executable:** `target/<package-name>`
- **Library:** `target/lib<package-name>`

### `river add`

Add a dependency to the current package.

```bash
river add <package> [--version <version>]
```

**Examples:**
```bash
river add http                      # Add latest version
river add json --version "2.0.0"    # Add specific version
```

This updates `River.toml` and adds the dependency.

### `river remove`

Remove a dependency from the current package.

```bash
river remove <package>
```

### `river install`

Install a package from the registry.

```bash
river install <package>
```

Downloads and installs the package to `~/.river/packages/`.

### `river publish`

Publish the current package to the registry.

```bash
river publish
```

**Requirements:**
- Valid `River.toml`
- Authentication token (run `river login` first)
- Package must build successfully

### `river search`

Search for packages in the registry.

```bash
river search <query>
```

### `river list`

List all installed packages.

```bash
river list
```

### `river update`

Update all dependencies to their latest versions.

```bash
river update
```

### `river clean`

Remove build artifacts.

```bash
river clean
```

---

## Package Types

### Binary (Executable)

A binary package produces an executable that can be run directly.

**Manifest:**
```toml
[package]
type = "bin"
entry = "src/main.flow"
```

**Entry Point (`src/main.flow`):**
```flow
func main() {
    print("Hello, World!");
}
```

**Build Output:** `target/my-app` (executable)

### Library

A library package can be imported by other packages.

**Manifest:**
```toml
[package]
type = "lib"
entry = "src/lib.flow"
```

**Entry Point (`src/lib.flow`):**
```flow
/// Public API function
func greet(name: string) -> string {
    return "Hello, " + name + "!";
}
```

**Usage in Another Package:**
```flow
import my-lib;

func main() {
    let msg: string = my-lib::greet("Flow");
    print(msg);
}
```

---

## Dependency Specification

### Version

```toml
[dependencies]
http = "1.0.0"
```

### Version Range

```toml
[dependencies]
json = "*"              # Latest version
parser = "^2.0"         # Compatible with 2.x
utils = ">=1.0, <2.0"   # Range
```

### Detailed Specification

```toml
[dependencies]
advanced = { version = "1.0", optional = true, features = ["async"] }
```

### Path Dependency

```toml
[dependencies]
local-lib = { path = "../local-lib" }
```

### Git Dependency

```toml
[dependencies]
cutting-edge = { git = "https://github.com/user/repo" }
specific-branch = { git = "https://github.com/user/repo", branch = "dev" }
specific-tag = { git = "https://github.com/user/repo", tag = "v1.0.0" }
```

---

## Configuration

River stores its configuration in `~/.river/config.toml`.

```toml
[registry]
url = "https://packages.flow-lang.org"
token = "your-auth-token"  # Optional

[paths]
packages = "/Users/you/.river/packages"
cache = "/Users/you/.river/cache"
```

---

## Directory Structure

```
~/.river/
├── config.toml              # River configuration
├── packages/                # Installed packages
│   ├── http-1.0.0/
│   ├── json-2.0.0/
│   └── ...
└── cache/                   # Cached downloads
    └── ...
```

---

## Building Packages

River uses the Flow compiler (`flowbase`) to build packages.

**Compiler Discovery:**
1. `../flowbase/build/flowbase` (relative to package)
2. `flowbase/build/flowbase` (in current directory)
3. `flowbase` in PATH
4. `$FLOW_HOME/bin/flowbase`

**Set Flow Home:**
```bash
export FLOW_HOME=/path/to/Flow/flowbase
```

---

## Examples

### Creating and Building an Executable

```bash
# Create package
river init hello --kind bin
cd hello

# Edit src/main.flow
cat > src/main.flow << 'EOF'
func main() {
    print("Hello from River!");
}
EOF

# Build
river build

# Run
./target/hello
```

### Creating a Library

```bash
# Create library
river init mathlib --kind lib
cd mathlib

# Edit src/lib.flow
cat > src/lib.flow << 'EOF'
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(a: int, b: int) -> int {
    return a * b;
}
EOF

# Build
river build
```

### Using the Library

```bash
# Create app that uses the library
cd ..
river init calculator --kind bin
cd calculator

# Add dependency
river add mathlib --version "0.1.0"

# Or edit River.toml manually:
# [dependencies]
# mathlib = { path = "../mathlib" }

# Edit src/main.flow
cat > src/main.flow << 'EOF'
import mathlib;

func main() {
    let sum: int = mathlib::add(10, 20);
    print("10 + 20 = " + sum);
    
    let product: int = mathlib::multiply(5, 6);
    print("5 * 6 = " + product);
}
EOF

# Build
river build

# Run
./target/calculator
```

---

## Comparison with Other Package Managers

| Feature | River | Cargo (Rust) | npm (Node.js) | pip (Python) |
|---------|-------|--------------|---------------|--------------|
| Manifest | River.toml | Cargo.toml | package.json | setup.py |
| Lock file | River.lock | Cargo.lock | package-lock.json | Pipfile.lock |
| Build system | ✓ | ✓ | ✗ | ✗ |
| Registry | Coming soon | crates.io | npmjs.com | pypi.org |
| Binary/Lib types | ✓ | ✓ | Partial | ✗ |

---

## Development Status

| Feature | Status |
|---------|--------|
| Package initialization | ✅ Complete |
| Build system | ✅ Complete |
| Dependency management | ✅ Complete |
| Local dependencies | ✅ Complete |
| Version resolution | ⚠️ Basic |
| Registry integration | 🚧 In Progress |
| Package publishing | 🚧 Planned |
| Git dependencies | 🚧 Planned |
| Workspace support | 🚧 Planned |

---

## Contributing

River is part of the Flow project and welcomes contributions!

---

## License

MIT

---

**River - Flow with packages** this was really cringe

