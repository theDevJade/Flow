# Flow Programming Language

A modern, function-first programming language with seamless foreign function interoperability, compiled using LLVM.

## Features

- **Simple Syntax**: C++-inspired syntax that's easy to learn
- **Type Safety**: Strong static typing with type inference
- **Immutability by Default**: `let` for immutable, `mut` for mutable variables
- **Foreign Function Interface**: Call functions from C, Python, JavaScript, and more
- **Struct Support**: Define custom data structures
- **Optional Types**: Built-in `Option<T>` for null safety
- **Async/Await**: First-class support for asynchronous operations
- **LLVM Backend**: Efficient compilation to native code

## Project Structure

```
flowbase/
├── include/
│   ├── AST/
│   │   └── AST.h              # Abstract Syntax Tree definitions
│   ├── Lexer/
│   │   ├── Token.h            # Token definitions
│   │   └── Lexer.h            # Lexical analyzer
│   ├── Parser/
│   │   └── Parser.h           # Parser interface
│   ├── Sema/
│   │   └── SemanticAnalyzer.h # Semantic analysis
│   ├── Codegen/
│   │   └── CodeGenerator.h    # LLVM IR generation
│   └── Driver/
│       └── Driver.h           # Compiler driver
├── src/
│   ├── AST/
│   │   └── AST.cpp
│   ├── Lexer/
│   │   ├── Token.cpp
│   │   └── Lexer.cpp
│   ├── Parser/
│   │   └── Parser.cpp
│   ├── Sema/
│   │   └── SemanticAnalyzer.cpp
│   ├── Codegen/
│   │   └── CodeGenerator.cpp
│   └── Driver/
│       └── Driver.cpp
├── examples/
│   ├── hello.flow
│   ├── variables.flow
│   ├── functions.flow
│   ├── structs.flow
│   ├── foreign.flow
│   ├── control_flow.flow
│   └── optionals.flow
├── CMakeLists.txt
└── main.cpp
```

## Building

### Prerequisites

- CMake 3.20 or higher
- C++17 compatible compiler (GCC, Clang, or MSVC)
- LLVM 10+ development libraries

### Install LLVM on macOS

```bash
brew install llvm
export LLVM_DIR=/usr/local/opt/llvm/lib/cmake/llvm
```

### Build the Compiler

```bash
mkdir build
cd build
cmake ..
make
```

## Usage

```bash
# Compile a Flow program
./flowbase examples/hello.flow

# Emit LLVM IR
./flowbase --emit-llvm examples/hello.flow -o hello

# Verbose output
./flowbase -v examples/functions.flow

# Optimization
./flowbase -O2 examples/hello.flow -o hello
```

## Language Quick Reference

### Variables

```flow
let x: int = 10;           // immutable
let mut y: float = 3.14;       // mutable
```

### Functions

```flow
func add(a: int, b: int) -> int {
    return a + b;
}

func say_hello(name: string) {
    print("Hello " + name);
}
```

### Structs

```flow
struct Person {
    string name;
    int age;
}

let p: Person = { "Bob", 25 };
```

### Foreign Functions

```flow
link "c" {
    func printf(fmt: string, ...) -> int;
}

link "python:math" {
    func sqrt(x: float) -> float;
}

// Export Flow function
#[abi="c"]
export func multiply(a: int, b: int) -> int {
    return a * b;
}
```

### Control Flow

```flow
if (score > 90) {
    print("Great!");
} else if (score > 50) {
    print("Okay");
} else {
    print("Try again");
}

for (i in 0..5) {
    print(i);
}
```

### Optional Types

```flow
func find_user(id: int) -> Option<Person> {
    if (id == 1) return some { "Admin", 999 };
    return none;
}

let result = find_user(2);
if (result has value) {
    print(result.name);
}
```

## Implementation Status

### ✅ Completed

- [x] Project structure and build system
- [x] Token definitions (all keywords, operators, delimiters)
- [x] Complete lexer implementation
- [x] AST node definitions for all language features
- [x] Skeleton frameworks for all compiler phases

### 🚧 In Progress (TODOs)

The following components have skeleton implementations with TODOs marked for full implementation:

1. **Documentation**
   - TODO: Write language specification
   - TODO: Create tutorial
   - TODO: Add API documentation
   - TODO: Write contributor guide

## Development Roadmap

### Phase 1: Core Language (Done)
- Complete parser implementation
- Finish basic code generation
- Support primitive types and basic operations

### Phase 2: Advanced Features (Done)
- Implement struct support fully
- Add arrays and collections
- Complete control flow constructs

### Phase 3: Foreign Function Interface (Done)
- C ABI support
- Python bindings
- JavaScript bindings

### Phase 4: Async/Await (TO-DO)
- Design async runtime
- Implement coroutines
- Add promise-based FFI

### Phase 5: Optimization & Tooling (Semi-done)
- LLVM optimization passes AHHHHH
- Debugger support DONE-ISH
- Package manager DONE
- Language server protocol (LSP) DONE

## Contributing

Please.

## License

MIT License

## References

- [LLVM Documentation](https://llvm.org/docs/)
- [Crafting Interpreters](https://craftinginterpreters.com/)
- [Modern Compiler Implementation](https://www.cs.princeton.edu/~appel/modern/)

