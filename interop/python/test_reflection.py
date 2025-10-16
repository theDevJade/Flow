#!/usr/bin/env python3
"""
Comprehensive test suite for Flow Python bindings reflection API
Tests the ability to introspect Flow modules from Python
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

import flow

# Test counters
tests_passed = 0
tests_failed = 0

def assert_equal(actual, expected, test_name):
    global tests_passed, tests_failed
    if actual == expected:
        print(f"✓ {test_name}")
        tests_passed += 1
    else:
        print(f"✗ {test_name}")
        print(f"  Expected: {expected}")
        print(f"  Got: {actual}")
        tests_failed += 1

def assert_true(condition, test_name):
    assert_equal(condition, True, test_name)

def assert_in(item, container, test_name):
    global tests_passed, tests_failed
    if item in container:
        print(f"✓ {test_name}")
        tests_passed += 1
    else:
        print(f"✗ {test_name}")
        print(f"  Expected '{item}' to be in {container}")
        tests_failed += 1

def assert_not_none(value, test_name):
    global tests_passed, tests_failed
    if value is not None:
        print(f"✓ {test_name}")
        tests_passed += 1
    else:
        print(f"✗ {test_name}")
        print(f"  Expected value to not be None")
        tests_failed += 1

print("=" * 60)
print("Flow Python Reflection API Tests")
print("=" * 60)

# Test 1: Initialize runtime
print("\n[Test 1] Initialize Runtime")
try:
    runtime = flow.Runtime()
    print("✓ Runtime initialized")
    tests_passed += 1
except Exception as e:
    print(f"✗ Runtime initialization failed: {e}")
    tests_failed += 1
    sys.exit(1)

# Test 2: Compile module with functions
print("\n[Test 2] Compile Module with Multiple Functions")
code = """
func add(a: int, b: int) -> int {
    return a + b;
}

func subtract(x: int, y: int) -> int {
    return x - y;
}

func multiply(a: int, b: int) -> int {
    return a * b;
}

func greet(name: string) -> string {
    return "Hello, " + name;
}

func get_pi() -> float {
    return 3.14159;
}
"""

try:
    module = flow.Module.compile(runtime, code)
    print("✓ Module compiled successfully")
    tests_passed += 1
except Exception as e:
    print(f"✗ Module compilation failed: {e}")
    tests_failed += 1
    sys.exit(1)

# Test 3: Get function count
print("\n[Test 3] Get Function Count")
try:
    count = module.get_function_count()
    assert_equal(count, 5, "Function count should be 5")
except Exception as e:
    print(f"✗ get_function_count failed: {e}")
    tests_failed += 1

# Test 4: List all functions
print("\n[Test 4] List All Functions")
try:
    functions = module.list_functions()
    assert_equal(len(functions), 5, "Should have 5 function names")
    assert_in("add", functions, "'add' should be in function list")
    assert_in("subtract", functions, "'subtract' should be in function list")
    assert_in("multiply", functions, "'multiply' should be in function list")
    assert_in("greet", functions, "'greet' should be in function list")
    assert_in("get_pi", functions, "'get_pi' should be in function list")
except Exception as e:
    print(f"✗ list_functions failed: {e}")
    tests_failed += 1

# Test 5: Get function info for 'add'
print("\n[Test 5] Get Function Info - 'add'")
try:
    info = module.get_function_info("add")
    assert_not_none(info, "Function info should not be None")
    assert_equal(info['name'], "add", "Function name should be 'add'")
    assert_equal(info['return_type'], "int", "Return type should be 'int'")
    assert_equal(len(info['parameters']), 2, "Should have 2 parameters")
    assert_equal(info['parameters'][0]['name'], "a", "First param name should be 'a'")
    assert_equal(info['parameters'][0]['type'], "int", "First param type should be 'int'")
    assert_equal(info['parameters'][1]['name'], "b", "Second param name should be 'b'")
    assert_equal(info['parameters'][1]['type'], "int", "Second param type should be 'int'")
except Exception as e:
    print(f"✗ get_function_info('add') failed: {e}")
    tests_failed += 1

# Test 6: Get function info for 'greet' (string parameter)
print("\n[Test 6] Get Function Info - 'greet' (string parameter)")
try:
    info = module.get_function_info("greet")
    assert_equal(info['name'], "greet", "Function name should be 'greet'")
    assert_equal(info['return_type'], "string", "Return type should be 'string'")
    assert_equal(len(info['parameters']), 1, "Should have 1 parameter")
    assert_equal(info['parameters'][0]['name'], "name", "Param name should be 'name'")
    assert_equal(info['parameters'][0]['type'], "string", "Param type should be 'string'")
except Exception as e:
    print(f"✗ get_function_info('greet') failed: {e}")
    tests_failed += 1

# Test 7: Get function info for 'get_pi' (no parameters)
print("\n[Test 7] Get Function Info - 'get_pi' (no parameters)")
try:
    info = module.get_function_info("get_pi")
    assert_equal(info['name'], "get_pi", "Function name should be 'get_pi'")
    assert_equal(info['return_type'], "float", "Return type should be 'float'")
    assert_equal(len(info['parameters']), 0, "Should have 0 parameters")
except Exception as e:
    print(f"✗ get_function_info('get_pi') failed: {e}")
    tests_failed += 1

# Test 8: Error handling - non-existent function
print("\n[Test 8] Error Handling - Non-existent Function")
try:
    info = module.get_function_info("nonexistent")
    print(f"✗ Should have raised FlowError for non-existent function")
    tests_failed += 1
except flow.FlowError as e:
    print(f"✓ Correctly raised FlowError: {e}")
    tests_passed += 1
except Exception as e:
    print(f"✗ Wrong exception type: {e}")
    tests_failed += 1

# Test 9: Module inspect() method
print("\n[Test 9] Module inspect() Method")
try:
    inspection = module.inspect()
    assert_true("add(a: int, b: int) -> int" in inspection, "Should show 'add' signature")
    assert_true("greet(name: string) -> string" in inspection, "Should show 'greet' signature")
    assert_true("get_pi() -> float" in inspection, "Should show 'get_pi' signature")
    print(f"\nInspection output:\n{inspection}")
except Exception as e:
    print(f"✗ inspect() failed: {e}")
    tests_failed += 1

# Test 10: Module with boolean functions
print("\n[Test 10] Boolean Return Types")
code_bool = """
func is_positive(x: int) -> bool {
    return x > 0;
}

func is_even(n: int) -> bool {
    return n % 2 == 0;
}
"""
try:
    module_bool = flow.Module.compile(runtime, code_bool)
    info = module_bool.get_function_info("is_positive")
    assert_equal(info['return_type'], "bool", "Return type should be 'bool'")
    
    functions = module_bool.list_functions()
    assert_equal(len(functions), 2, "Should have 2 functions")
except Exception as e:
    print(f"✗ Boolean functions test failed: {e}")
    tests_failed += 1

# Test 11: Large module with many functions
print("\n[Test 11] Large Module with Many Functions")
code_large = "\n".join([
    f"func func{i}(x: int) -> int {{ return x * {i}; }}"
    for i in range(20)
])
try:
    module_large = flow.Module.compile(runtime, code_large)
    count = module_large.get_function_count()
    assert_equal(count, 20, "Should have 20 functions")
    
    functions = module_large.list_functions()
    assert_equal(len(functions), 20, "Should list 20 functions")
    
    # Check a few function infos
    info0 = module_large.get_function_info("func0")
    assert_equal(info0['name'], "func0", "Function 0 name")
    
    info19 = module_large.get_function_info("func19")
    assert_equal(info19['name'], "func19", "Function 19 name")
except Exception as e:
    print(f"✗ Large module test failed: {e}")
    tests_failed += 1

# Test 12: Verify functions still callable after reflection
print("\n[Test 12] Functions Still Callable After Reflection")
try:
    # Use the original module
    result = module.call("add", 10, 20)
    assert_equal(result, 30, "add(10, 20) should return 30")
    
    result = module.call("multiply", 7, 6)
    assert_equal(result, 42, "multiply(7, 6) should return 42")
except Exception as e:
    print(f"✗ Calling functions after reflection failed: {e}")
    tests_failed += 1

# Test 13: Multiple parameter types
print("\n[Test 13] Mixed Parameter Types")
code_mixed = """
func process(a: int, b: float, c: string, d: bool) -> string {
    return "processed";
}
"""
try:
    module_mixed = flow.Module.compile(runtime, code_mixed)
    info = module_mixed.get_function_info("process")
    
    assert_equal(len(info['parameters']), 4, "Should have 4 parameters")
    assert_equal(info['parameters'][0]['type'], "int", "Param 0 type")
    assert_equal(info['parameters'][1]['type'], "float", "Param 1 type")
    assert_equal(info['parameters'][2]['type'], "string", "Param 2 type")
    assert_equal(info['parameters'][3]['type'], "bool", "Param 3 type")
except Exception as e:
    print(f"✗ Mixed parameter types test failed: {e}")
    tests_failed += 1

# Test 14: Empty module
print("\n[Test 14] Empty Module")
try:
    module_empty = flow.Module.compile(runtime, "// Just a comment")
    count = module_empty.get_function_count()
    assert_equal(count, 0, "Empty module should have 0 functions")
    
    functions = module_empty.list_functions()
    assert_equal(len(functions), 0, "Empty module should list 0 functions")
    
    inspection = module_empty.inspect()
    assert_true("no functions" in inspection.lower(), "Inspection should mention no functions")
except Exception as e:
    print(f"✗ Empty module test failed: {e}")
    tests_failed += 1

# Test 15: Reflection on loaded file module
print("\n[Test 15] Reflection on File-Loaded Module")
test_file = "/tmp/test_reflect.flow"
with open(test_file, "w") as f:
    f.write("""
func square(x: int) -> int {
    return x * x;
}

func cube(x: int) -> int {
    return x * x * x;
}
""")

try:
    module_file = flow.Module.load(runtime, test_file)
    functions = module_file.list_functions()
    assert_in("square", functions, "'square' should be in loaded module")
    assert_in("cube", functions, "'cube' should be in loaded module")
    
    info = module_file.get_function_info("square")
    assert_equal(info['name'], "square", "Loaded function name")
except Exception as e:
    print(f"✗ File-loaded module reflection failed: {e}")
    tests_failed += 1
finally:
    if os.path.exists(test_file):
        os.remove(test_file)

# Summary
print("\n" + "=" * 60)
print(f"Tests Passed: {tests_passed}")
print(f"Tests Failed: {tests_failed}")
print(f"Total Tests: {tests_passed + tests_failed}")
print("=" * 60)

if tests_failed == 0:
    print("✓ All tests passed!")
    sys.exit(0)
else:
    print(f"✗ {tests_failed} test(s) failed")
    sys.exit(1)

