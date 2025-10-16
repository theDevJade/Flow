#!/usr/bin/env python3
"""
Flow Python Bindings - File Loading Example

Demonstrates loading and using Flow modules from files.
"""

import sys
import os
sys.path.insert(0, '..')

import flow

print("=" * 60)
print("Flow Python Bindings - File Loading Example")
print("=" * 60)
print()

# Initialize runtime
runtime = flow.Runtime()

# Load module from file
module_path = '../../c/test_module.flow'
if not os.path.exists(module_path):
    print(f"✗ Test module not found: {module_path}")
    print("  Please ensure the C library tests have been run.")
    sys.exit(1)

print(f"Loading module from: {module_path}")
mod = flow.Module.load(runtime, module_path)
print("✓ Module loaded successfully")
print()

# Test integer operations
print("Testing integer operations:")
result = mod.call('add', 5, 3)
print(f"  add(5, 3) = {result}")
assert result == 8, f"Expected 8, got {result}"

result = mod.call('multiply', 6, 7)
print(f"  multiply(6, 7) = {result}")
assert result == 42, f"Expected 42, got {result}"

result = mod.call('subtract', 10, 3)
print(f"  subtract(10, 3) = {result}")
assert result == 7, f"Expected 7, got {result}"
print()

# Test boolean operations
print("Testing boolean operations:")
result = mod.call('is_positive', 5, 0)
print(f"  is_positive(5) = {result}")
assert result == True, f"Expected True, got {result}"

result = mod.call('is_positive', -3, 0)
print(f"  is_positive(-3) = {result}")
assert result == False, f"Expected False, got {result}"
print()

# Test float operations
print("Testing float operations:")
result = mod.call('square', 4.5, 0.0)
print(f"  square(4.5) = {result}")
assert abs(result - 20.25) < 0.01, f"Expected 20.25, got {result}"
print()

# Test method syntax
print("Using method syntax:")
result = mod.add(100, 200)
print(f"  mod.add(100, 200) = {result}")
assert result == 300, f"Expected 300, got {result}"

result = mod.multiply(12, 12)
print(f"  mod.multiply(12, 12) = {result}")
assert result == 144, f"Expected 144, got {result}"
print()

print("✓ All tests passed!")

