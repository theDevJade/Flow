#!/usr/bin/env python3
"""
Flow Python Bindings - Error Handling Example

Demonstrates error handling in the Flow Python bindings.
"""

import sys
sys.path.insert(0, '..')

import flow

print("=" * 60)
print("Flow Python Bindings - Error Handling Example")
print("=" * 60)
print()

# Test 1: Runtime initialization
print("Test 1: Runtime initialization")
try:
    runtime = flow.Runtime()
    print("✓ Runtime initialized successfully")
except flow.FlowInitializationError as e:
    print(f"✗ Initialization failed: {e}")
print()

# Test 2: Loading non-existent file
print("Test 2: Loading non-existent file")
try:
    mod = flow.Module.load(runtime, 'nonexistent.flow')
    print("✗ Should have raised FileNotFoundError")
except FileNotFoundError as e:
    print(f"✓ Expected error: {e}")
print()

# Test 3: Empty module
print("Test 3: Empty module compilation")
try:
    # Note: Parser has error recovery, so syntax errors might not raise immediately
    mod = flow.Module.compile(runtime, '')
    # Try calling a non-existent function
    try:
        result = mod.call('test', 0, 0)
        print("✗ Should have raised error")
    except flow.FlowRuntimeError as e:
        print(f"✓ Expected error (empty module): Function not found")
except flow.FlowCompileError as e:
    print(f"✓ Expected compile error: {e}")
print()

# Test 4: Calling non-existent function
print("Test 4: Calling non-existent function")
try:
    source = 'func test(x: int) -> int { return x; }'
    mod = flow.Module.compile(runtime, source)
    result = mod.call('nonexistent', 1, 0)
    print("✗ Should have raised FlowRuntimeError")
except flow.FlowRuntimeError as e:
    print(f"✓ Expected error: {e}")
print()

# Test 5: Successful function call
print("Test 5: Successful function call")
try:
    source = 'func add(a: int, b: int) -> int { return a + b; }'
    mod = flow.Module.compile(runtime, source)
    result = mod.call('add', 10, 20)
    print(f"✓ Success: {result}")
    assert result == 30
except Exception as e:
    print(f"✗ Unexpected error: {e}")
print()

# Test 6: AttributeError for Python-only methods
print("Test 6: AttributeError for private attributes")
try:
    source = 'func test(x: int) -> int { return x; }'
    mod = flow.Module.compile(runtime, source)
    _ = mod._private_method
    print("✗ Should have raised AttributeError")
except AttributeError:
    print("✓ AttributeError raised as expected")
print()

# Test 7: Type conversions
print("Test 7: Type conversions")
try:
    source = '''
    func echo_int(x: int, y: int) -> int { return x; }
    func echo_float(x: float, y: float) -> float { return x; }
    func echo_bool(x: int, y: int) -> bool { return x > 0; }
    '''
    mod = flow.Module.compile(runtime, source)
    
    result = mod.call('echo_int', 42, 0)
    assert result == 42, f"Int conversion failed: {result}"
    
    result = mod.call('echo_float', 3.14, 0.0)
    assert abs(result - 3.14) < 0.01, f"Float conversion failed: {result}"
    
    result = mod.call('echo_bool', 1, 0)
    assert result == True, f"Bool conversion failed: {result}"
    
    print("✓ All type conversions successful")
except Exception as e:
    print(f"✗ Type conversion error: {e}")
print()

print("✓ Error handling tests completed!")

