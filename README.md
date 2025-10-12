# Flow Programming Language

**A modern systems programming language with seamless interoperability**

Flow is a statically-typed, compiled programming language that combines the performance of systems languages with the
ease of modern high-level languages. Flow compiles to native code via LLVM and features seamless FFI with C and Java,
plus a powerful package manager called River.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🚀 Features

- **Native Performance** - Compiles to optimized machine code via LLVM
- **C Interoperability** - Call C functions directly with `link` blocks
- **Java Integration** - Full JNI bindings for embedding Flow in Java
- **Package Manager** - River package manager for dependency management
- **Modern Syntax** - Clean, readable syntax with type inference
- **Great Tooling** - VSCode extension with Language Server Protocol (LSP) support
- **Memory Safe** - Immutability by default with explicit mutability
- **Structs & Types** - Rich type system with optional types

---

## 📦 Project Structure

This repository contains the complete Flow ecosystem:

### [`flowbase/`](flowbase/) - Core Compiler & Runtime

The main Flow compiler, LLVM code generator, and runtime system.

- **[README](flowbase/README.md)** - Detailed compiler documentation
- Built with C++17 and LLVM
- Includes lexer, parser, semantic analyzer, and code generator
- LSP server for editor integration

### [`river/`](river/) - Package Manager

River is Flow's official package manager for creating, building, and managing Flow packages.

- **[README](river/README.md)** - Complete River documentation
- Built with Rust
- Commands: `init`, `build`, `add`, `remove`, `publish`, and more
- Supports both binary (executable) and library packages

### [`javabindings/`](javabindings/) - Java Integration

JNI bindings for embedding Flow in Java applications.

- **[README](javabindings/README.md)** - Java API documentation
- Built with Gradle
- Type-safe Java API for compiling and executing Flow code
- Automatic resource management

### [`flowvscode/`](flowvscode/) - VSCode Extension

Official VSCode extension with syntax highlighting and LSP integration.

- Syntax highlighting via TextMate grammar
- Code completion and diagnostics
- Go to definition and find references
- Built with TypeScript

### [`test-packages/`](test-packages/) - Example Packages

Sample Flow projects demonstrating River package manager usage:

- `hello-world/` - Simple hello world executable
- `mathlib/` - Library package example
- `calculator/` - App using library dependencies
- `c-math/` - C interop example

---

## ⚡ Quick Start

### Hello World

```flow
func main() {
    print("Hello, Flow!");
}
```

### Variables and Types

```flow
func main() {
    let x: int = 10;           // Immutable
    let mut y: int = 20;       // Mutable
    
    y = y + x;
    print("Result: " + y);     // 30
}
```

### Functions

```flow
func add(a: int, b: int) -> int {
    return a + b;
}

func main() {
    let result: int = add(5, 3);
    print("5 + 3 = " + result);
}
```

### C Interoperability

```flow
link "c" {
    func sqrt(x: float) -> float;
    func pow(x: float, y: float) -> float;
}

func main() {
    let root: float = sqrt(16.0);
    let power: float = pow(2.0, 10.0);
    
    print("sqrt(16) = " + root);      // 4.0
    print("pow(2, 10) = " + power);   // 1024.0
}
```

### Structs

```flow
struct Point {
    float x;
    float y;
}

func main() {
    let p: Point = { x: 10.0, y: 20.0 };
    print("Point: (" + p.x + ", " + p.y + ")");
}
```

---

## 🛠️ Installation

### Quick Install with Lake (Recommended)

**Lake** is our rustup-style toolchain installer:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://install.flowc.dev/init | sh
```

Or locally:

```bash
cd lake && ./lake-init
```

Lake automatically downloads prebuilt binaries or builds from source.

### Manual Installation

#### Prerequisites

- **C++ compiler** (Clang 10+ or GCC 9+)
- **LLVM 18+** with development libraries
- **CMake 3.20+**
- **Rust** (for River package manager)
- *Optional:* JDK 11+ (for Java bindings)
- *Optional:* Node.js 14+ (for VSCode extension)

### macOS

```bash
# Install dependencies
brew install llvm cmake rust

# Set LLVM path
export LLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm
```

### Linux (Ubuntu/Debian)

```bash
# Install dependencies
sudo apt-get install build-essential cmake llvm-18-dev clang-18
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

---

## 🔨 Building

### 1. Build the Flow Compiler

```bash
cd flowbase
./build.sh
```

This creates:

- `build/flowbase` - Flow compiler executable
- `build/flow-lsp` - Language Server Protocol server

### 2. Build River Package Manager

```bash
cd river
cargo build --release
# or
./build.sh
```

The binary will be at `target/release/river`.

### 3. Install VSCode Extension (Optional)

```bash
cd flowvscode
npm install
npm run compile
code --install-extension flow-lang-0.1.0.vsix
```

### 4. Build Java Bindings (Optional)

```bash
cd javabindings
./gradlew build
```

---

## 📚 Documentation

### Core Documentation

- **[Flow Compiler README](flowbase/README.md)** - Compiler architecture and usage
- **[Language Examples](flowbase/examples/)** - Sample Flow programs
- **Getting Started Guide** - Tutorial for beginners
- **Language Reference** - Complete language specification

### Package Manager

- **[River README](river/README.md)** - Complete package manager guide
- **[River Commands](river/README.md#commands)** - All CLI commands
- **[Package Types](river/README.md#package-types)** - Binary vs library packages

### Java Integration

- **[Java Bindings README](javabindings/README.md)** - Java API documentation
- **[API Reference](javabindings/README.md#api-reference)** - Complete Java API


---

## 🎯 Using River Package Manager

### Create a New Package

```bash
# Create an executable
river init my-app --kind bin

# Create a library
river init my-lib --kind lib
```

### Manage Dependencies

```bash
# Add a dependency
river add http --version "2.0.0"

# Build the package
river build

# Remove a dependency
river remove http

# Clean build artifacts
river clean
```

### Package Types

- **Binary (`bin`)** - Produces an executable with a `main()` function
- **Library (`lib`)** - Reusable code that other packages can import

See the **[River README](river/README.md)** for complete documentation.

---

## ☕ Java Integration Example

```java
import com.flowlang.bindings.*;

public class Example {
    public static void main(String[] args) {
        try (FlowRuntime runtime = new FlowRuntime()) {
            // Compile Flow code
            String code = 
                "func multiply(a: int, b: int) -> int {\n" +
                "    return a * b;\n" +
                "}";
            
            FlowModule module = runtime.compile(code, "math");
            
            // Call Flow function
            FlowValue result = module.call(
                runtime, 
                "multiply",
                runtime.createInt(6),
                runtime.createInt(7)
            );
            
            System.out.println("Result: " + result.asInt());  // 42
        } catch (FlowException e) {
            e.printStackTrace();
        }
    }
}
```

See the **[Java Bindings README](javabindings/README.md)** for more examples.

---

## 🎨 VSCode Features

- ✅ Syntax highlighting
- ✅ Code completion (via LSP)
- ✅ Go to definition
- ✅ Find references
- ✅ Hover information
- ✅ Error diagnostics
- ✅ Bracket matching
- ✅ Comment toggling

---

## 📖 Language Features

### Type System

```flow
// Primitive types
let i: int = 42;
let f: float = 3.14;
let s: string = "hello";
let b: bool = true;

// Optional types
let maybe: int? = 10;
let nothing: int? = null;

// Type inference
let auto = 42;  // Inferred as int
```

### Control Flow

```flow
// If expressions
if (x > 0) {
    print("positive");
} else {
    print("non-positive");
}

// For loops
for (i in 0..10) {
    print(i);
}

// While loops
while (x < 100) {
    x = x * 2;
}
```

### Exported Functions

```flow
// Export for C FFI
export func add(a: int, b: int) -> int {
    return a + b;
}
```

---

## 📊 Project Status

| Component             | Status     | Notes                             |
|-----------------------|------------|-----------------------------------|
| Compiler              | ✅ Complete | 10/10 examples working            |
| LLVM Codegen          | ✅ Complete | Full type support                 |
| JIT Engine            | ✅ Working  | Native execution                  |
| Language Server       | ✅ Complete | All LSP features                  |
| VSCode Extension      | ✅ Complete | Full integration                  |
| Java Bindings         | ✅ Complete | 5/5 tests passing                 |
| River Package Manager | ✅ Complete | 10 commands implemented           |
| Documentation         | ✅ Basic    | Need to finish full documentation |

---

## 🚦 Running Examples

```bash
# Navigate to flowbase
cd flowbase

# Run examples
./build/flowbase examples/hello.flow
./build/flowbase examples/variables.flow
./build/flowbase examples/functions.flow
./build/flowbase examples/structs.flow
./build/flowbase examples/c_interop_test.flow
./build/flowbase examples/for_loops.flow
./build/flowbase examples/mutability.flow
./build/flowbase examples/optional_types.flow
./build/flowbase examples/simple_math.flow
./build/flowbase examples/variadic_test.flow
```

Or run the test suite:

```bash
./TEST_ALL.sh
```

---

## 🤝 Contributing

Contributions are welcome! Here are some areas where you can help:

- **Language Features** - Generics, closures, async/await
- **Standard Library** - Built-in functions and modules
- **Optimizations** - LLVM optimization passes
- **Error Messages** - Better diagnostics and suggestions
- **Documentation** - Tutorials, guides, and examples
- **Tooling** - Editor plugins, debugger support
- **Package Ecosystem** - Create and publish Flow packages

### Getting Started

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./TEST_ALL.sh`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Flow is built with and inspired by:

- **[LLVM](https://llvm.org/)** - Compiler infrastructure and optimization framework
- **[Rust](https://www.rust-lang.org/)** - River package manager implementation
- **[VSCode](https://code.visualstudio.com/)** - Editor integration
- **[JNI](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/)** - Java Native Interface

---

## 🔗 Links

- **Repository**: https://github.com/theDevJade/Flow
- **Issues**: https://github.com/theDevJade/Flow/issues
- **Discussions**: https://github.com/theDevJade/Flow/discussions

---

## 📞 Contact

For questions, suggestions, or contributions, please open an issue or start a discussion on GitHub.

---

**Flow - Write once, run anywhere, interoperate with everything, well everything in the future ofc.** 🚀
