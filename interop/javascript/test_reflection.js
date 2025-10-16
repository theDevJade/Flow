#!/usr/bin/env node
/**
 * Comprehensive test suite for Flow JavaScript bindings reflection API
 */

const flow = require('./index.js');
const assert = require('assert');

let testsPassed = 0;
let testsFailed = 0;

function test(name, fn) {
    try {
        fn();
        console.log(`✓ ${name}`);
        testsPassed++;
    } catch (error) {
        console.log(`✗ ${name}`);
        console.log(`  ${error.message}`);
        testsFailed++;
    }
}

function assertEqual(actual, expected, message) {
    if (actual !== expected) {
        throw new Error(`${message}: expected ${expected}, got ${actual}`);
    }
}

function assertArrayEqual(actual, expected, message) {
    if (JSON.stringify(actual.sort()) !== JSON.stringify(expected.sort())) {
        throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
    }
}

function assertTrue(condition, message) {
    if (!condition) {
        throw new Error(message);
    }
}

function assertContains(haystack, needle, message) {
    if (!haystack.includes(needle)) {
        throw new Error(`${message}: '${haystack}' does not contain '${needle}'`);
    }
}

console.log('============================================================');
console.log('Flow JavaScript Reflection API Tests');
console.log('============================================================\n');

// Test 1: Function count
test('test_function_count', () => {
    const code = `
func add(a: int, b: int) -> int {
    return a + b;
}

func subtract(x: int, y: int) -> int {
    return x - y;
}

func multiply(m: int, n: int) -> int {
    return m * n;
}
`;
    
    const module = flow.compile(code);
    const count = module.functionCount();
    
    assertEqual(count, 3, 'Should have 3 functions');
});

// Test 2: List functions
test('test_list_functions', () => {
    const code = `
func greet(name: string) -> string {
    return "Hello, " + name;
}

func square(x: int) -> int {
    return x * x;
}

func is_positive(n: int) -> bool {
    return n > 0;
}
`;
    
    const module = flow.compile(code);
    const functions = module.listFunctions();
    
    assertEqual(functions.length, 3, 'Should have 3 function names');
    assertArrayEqual(functions, ['greet', 'square', 'is_positive'], 'Function names should match');
});

// Test 3: Function info with parameters
test('test_function_info_with_parameters', () => {
    const code = `
func add(a: int, b: int) -> int {
    return a + b;
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('add');
    
    assertEqual(info.name, 'add', 'Function name should be add');
    assertEqual(info.return_type, 'int', 'Return type should be int');
    assertEqual(info.parameters.length, 2, 'Should have 2 parameters');
    assertEqual(info.parameters[0].name, 'a', 'First param name');
    assertEqual(info.parameters[0].type, 'int', 'First param type');
    assertEqual(info.parameters[1].name, 'b', 'Second param name');
    assertEqual(info.parameters[1].type, 'int', 'Second param type');
});

// Test 4: Function info with string parameter
test('test_function_info_string_parameter', () => {
    const code = `
func greet(name: string) -> string {
    return "Hello, " + name;
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('greet');
    
    assertEqual(info.name, 'greet', 'Function name');
    assertEqual(info.return_type, 'string', 'Return type');
    assertEqual(info.parameters.length, 1, 'Parameter count');
    assertEqual(info.parameters[0].name, 'name', 'Param name');
    assertEqual(info.parameters[0].type, 'string', 'Param type');
});

// Test 5: Function info with no parameters
test('test_function_info_no_parameters', () => {
    const code = `
func get_pi() -> float {
    return 3.14159;
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('get_pi');
    
    assertEqual(info.name, 'get_pi', 'Function name');
    assertEqual(info.return_type, 'float', 'Return type');
    assertEqual(info.parameters.length, 0, 'Should have no parameters');
});

// Test 6: Function info nonexistent
test('test_function_info_nonexistent', () => {
    const code = `
func test() -> int {
    return 42;
}
`;
    
    const module = flow.compile(code);
    
    let threw = false;
    try {
        module.functionInfo('nonexistent');
    } catch (e) {
        threw = true;
        assertContains(e.message, 'not found', 'Error message should mention not found');
    }
    
    assertTrue(threw, 'Should throw error for non-existent function');
});

// Test 7: Inspect functions
test('test_inspect_functions', () => {
    const code = `
func add(a: int, b: int) -> int {
    return a + b;
}

func greet(name: string) -> string {
    return "Hello";
}
`;
    
    const module = flow.compile(code);
    const inspection = module.inspect();
    
    assertContains(inspection, 'add(a: int, b: int) -> int', 'Should contain add signature');
    assertContains(inspection, 'greet(name: string) -> string', 'Should contain greet signature');
    assertContains(inspection, '2 function(s)', 'Should mention 2 functions');
});

// Test 8: Boolean return types
test('test_boolean_return_types', () => {
    const code = `
func is_positive(x: int) -> bool {
    return x > 0;
}

func is_even(n: int) -> bool {
    return n % 2 == 0;
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('is_positive');
    
    assertEqual(info.return_type, 'bool', 'Return type should be bool');
    assertEqual(module.functionCount(), 2, 'Should have 2 functions');
});

// Test 9: Large module
test('test_large_module', () => {
    let code = '';
    for (let i = 0; i < 20; i++) {
        code += `func func${i}(x: int) -> int { return x * ${i}; }\n`;
    }
    
    const module = flow.compile(code);
    const count = module.functionCount();
    
    assertEqual(count, 20, 'Should have 20 functions');
    
    const functions = module.listFunctions();
    assertEqual(functions.length, 20, 'Should list 20 functions');
    
    const info0 = module.functionInfo('func0');
    assertEqual(info0.name, 'func0', 'First function name');
    
    const info19 = module.functionInfo('func19');
    assertEqual(info19.name, 'func19', 'Last function name');
});

// Test 10: Functions still callable after reflection
test('test_functions_still_callable_after_reflection', () => {
    const code = `
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(x: int, y: int) -> int {
    return x * y;
}
`;
    
    const module = flow.compile(code);
    
    // Do reflection
    const count = module.functionCount();
    assertEqual(count, 2, 'Should have 2 functions');
    
    const info = module.functionInfo('add');
    assertEqual(info.name, 'add', 'Function name should be add');
    
    // Now call the functions to ensure reflection didn't break them
    const result1 = module.call('add', 10, 20);
    assertEqual(result1, 30, 'add(10, 20) should return 30');
    
    const result2 = module.call('multiply', 7, 6);
    assertEqual(result2, 42, 'multiply(7, 6) should return 42');
});

// Test 11: Mixed parameter types
test('test_mixed_parameter_types', () => {
    const code = `
func process(a: int, b: float, c: string, d: bool) -> string {
    return "processed";
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('process');
    
    assertEqual(info.parameters.length, 4, 'Should have 4 parameters');
    assertEqual(info.parameters[0].type, 'int', 'First param type');
    assertEqual(info.parameters[1].type, 'float', 'Second param type');
    assertEqual(info.parameters[2].type, 'string', 'Third param type');
    assertEqual(info.parameters[3].type, 'bool', 'Fourth param type');
});

// Test 12: Empty module
test('test_empty_module', () => {
    const code = '// Just a comment';
    
    const module = flow.compile(code);
    const count = module.functionCount();
    
    assertEqual(count, 0, 'Empty module should have 0 functions');
    
    const functions = module.listFunctions();
    assertEqual(functions.length, 0, 'Empty module should list 0 functions');
    
    const inspection = module.inspect();
    assertContains(inspection.toLowerCase(), 'no functions', 'Should mention no functions');
});

// Test 13: Float return type
test('test_float_return_type', () => {
    const code = `
func calculate_pi() -> float {
    return 3.14159;
}
`;
    
    const module = flow.compile(code);
    const info = module.functionInfo('calculate_pi');
    
    assertEqual(info.return_type, 'float', 'Return type should be float');
    assertEqual(info.parameters.length, 0, 'Should have no parameters');
});

// Test 14: Multiple string functions
test('test_multiple_string_functions', () => {
    const code = `
func concat(a: string, b: string) -> string {
    return a + b;
}

func repeat(s: string, n: int) -> string {
    return s;
}
`;
    
    const module = flow.compile(code);
    
    const info1 = module.functionInfo('concat');
    assertEqual(info1.parameters.length, 2, 'concat should have 2 parameters');
    assertEqual(info1.parameters[0].type, 'string', 'First param type');
    assertEqual(info1.parameters[1].type, 'string', 'Second param type');
    
    const info2 = module.functionInfo('repeat');
    assertEqual(info2.parameters.length, 2, 'repeat should have 2 parameters');
    assertEqual(info2.parameters[0].type, 'string', 'First param type');
    assertEqual(info2.parameters[1].type, 'int', 'Second param type');
});

// Print summary
console.log('\n============================================================');
console.log(`Tests passed: ${testsPassed}`);
console.log(`Tests failed: ${testsFailed}`);
console.log('============================================================');

process.exit(testsFailed > 0 ? 1 : 0);

