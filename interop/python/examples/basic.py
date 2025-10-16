#!/usr/bin/env python3
"""
Flow Python Bindings - Basic Example

Demonstrates basic usage of the Flow Python bindings including:
- Runtime initialization
- Inline code compilation
- Calling Flow functions with different types
- Method syntax
"""

import sys
sys.path.insert(0, '..')

import flow

print("=" * 60)
print("Flow Python Bindings - Basic Example")
print("=" * 60)
print()

# Initialize runtime and compile inline Flow code
print("Initializing Flow runtime...")
runtime = flow.Runtime()
print("✓ Runtime initialized")
print()

print("Compiling inline Flow code...")
source = """
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(a: int, b: int) -> int {
    return a * b;
}

func subtract(a: int, b: int) -> int {
    return a - b;
}

func is_positive(n: int) -> bool {
    return n > 0;
}

func square(x: float) -> float {
    return x * x;
}
"""

mod = flow.Module.compile(runtime, source)
print("✓ Module compiled")
print()

# Test integer operations
print("Calling add(10, 20)...")
result = mod.call('add', 10, 20)
print(f"Result: {result}")
assert result == 30, f"Expected 30, got {result}"
print()

print("Calling multiply(6, 7)...")
result = mod.call('multiply', 6, 7)
print(f"Result: {result}")
assert result == 42, f"Expected 42, got {result}"
print()

print("Calling subtract(100, 58)...")
result = mod.call('subtract', 100, 58)
print(f"Result: {result}")
assert result == 42, f"Expected 42, got {result}"
print()

# Test boolean operations
print("Calling is_positive(42)...")
result = mod.call('is_positive', 42, 0)  # Need 2 args for FlowAPI
print(f"Result: {result}")
assert result == True, f"Expected True, got {result}"
print()

# Test float operations
print("Calling square(5.0)...")
result = mod.call('square', 5.0, 0.0)  # Need 2 args for FlowAPI
print(f"Result: {result}")
assert abs(result - 25.0) < 0.01, f"Expected 25.0, got {result}"
print()

# Test method syntax
print("Using method syntax: mod.add(15, 27)...")
result = mod.add(15, 27)
print(f"Result: {result}")
assert result == 42, f"Expected 42, got {result}"
print()

print("✓ All examples completed successfully!")

