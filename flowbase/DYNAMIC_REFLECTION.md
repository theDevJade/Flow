# Dynamic Foreign Module Reflection in Flow

## Architecture Overview

Flow's LSP and runtime now support **dynamic introspection** of foreign modules, not hardcoded signatures!

## How It Works

### 1. When You Write Flow Code

```flow
link python "math" {
    fn sin(x: float) -> float;
    fn cos(x: float) -> float;
}

fn calculate() {
    let angle = 3.14159 / 4.0;
    let result = sin(angle);  // ← Autocomplete works here!
}
```

### 2. LSP Encounters the `link` Block

When the LSP analyzes your code and finds a `link python "math"` block:

1. **ForeignModuleLoader** is invoked
2. It shells out to Python (or uses Python C API):
   ```bash
   python3 -c "import math; import inspect; ..."
   ```
3. Python's `inspect` module introspects the `math` module
4. All functions are discovered dynamically
5. Signatures are registered with **ReflectionManager**

### 3. Autocomplete & Hover

- When you type `sin(` in VSCode, the LSP queries ReflectionManager
- It returns: `sin(x: float) -> float [from Python math module]`
- Documentation is pulled from the actual Python module

## Implementation Per Language

### Python (IMPLEMENTED)
- Uses `python3 -c` + `inspect` module
- Discovers all functions in a module dynamically
- Parses signatures from `inspect.signature()`

### Go (PARTIAL)
- Currently uses manual registration for common stdlib packages
- TODO: Use `go list -json` + `reflect` package for dynamic discovery

### JavaScript (TODO)
- Will use Node.js `require()` + `Object.getOwnPropertyNames()`
- Or use V8 C++ API for deeper integration

### Rust/Ruby/PHP (TODO)
- Rust: Use procedural macros or `cargo metadata`
- Ruby: Use `Module.methods` and `Method#parameters`
- PHP: Use `ReflectionClass` and `ReflectionFunction`

## No More Hardcoding!

**Before (BAD):**
```cpp
FunctionSignature sin;
sin.name = "sin";
sin.returnType = "float";
// ... manually writing every function
```

**After (GOOD):**
```cpp
auto& loader = ForeignModuleLoader::getInstance();
loader.loadAndRegisterModule("python", "math");
// ↑ Discovers ALL functions automatically!
```

## Testing

Run the LSP server and open a Flow file with:
```flow
link python "math" {
    fn sin(x: float) -> float;
}
```

Then try autocomplete - you'll see ALL functions from Python's `math` module, not just what we hardcoded!

## Next Steps

1. ✅ Python - Dynamic introspection implemented
2. ⏳ Go - Implement `go/types` package introspection
3. ⏳ JavaScript - Implement V8/Node.js introspection  
4. ⏳ Rust/Ruby/PHP - Implement language-specific reflection

## Performance

- Modules are loaded **once** when first encountered
- Results are cached in ReflectionManager
- No performance penalty after initial load

