/**
 * Flow JavaScript Bindings - Error Handling Example
 * 
 * Demonstrates error handling in the Flow JavaScript bindings.
 */

const flow = require('../index');

console.log('='.repeat(60));
console.log('Flow JavaScript Bindings - Error Handling Example');
console.log('='.repeat(60));
console.log();

// Test 1: Loading non-existent file
console.log('Test 1: Loading non-existent file');
try {
    const mod = flow.loadModule('nonexistent.flow');
    console.log('✗ Should have raised Error');
} catch (e) {
    console.log(`✓ Expected error: ${e.message}`);
}
console.log();

// Test 2: Empty module
console.log('Test 2: Empty module compilation');
try {
    const mod = flow.compile('');
    // Try calling non-existent function
    try {
        const result = mod.call('test', 0, 0);
        console.log('✗ Should have raised error');
    } catch (e) {
        console.log(`✓ Expected error (empty module): Function not found`);
    }
} catch (e) {
    if (e instanceof flow.FlowCompileError) {
        console.log(`✓ Expected compile error: ${e.message}`);
    } else {
        throw e;
    }
}
console.log();

// Test 3: Calling non-existent function
console.log('Test 3: Calling non-existent function');
try {
    const source = 'func test(x: int) -> int { return x; }';
    const mod = flow.compile(source);
    const result = mod.call('nonexistent', 1, 0);
    console.log('✗ Should have raised FlowRuntimeError');
} catch (e) {
    if (e instanceof flow.FlowRuntimeError) {
        console.log(`✓ Expected error: ${e.message}`);
    } else {
        throw e;
    }
}
console.log();

// Test 4: Successful function call
console.log('Test 4: Successful function call');
try {
    const source = 'func add(a: int, b: int) -> int { return a + b; }';
    const mod = flow.compile(source);
    const result = mod.call('add', 10, 20);
    console.log(`✓ Success: ${result}`);
    console.assert(result === 30);
} catch (e) {
    console.log(`✗ Unexpected error: ${e.message}`);
}
console.log();

// Test 5: Type conversions
console.log('Test 5: Type conversions');
try {
    const source = `
    func echo_int(x: int, y: int) -> int { return x; }
    func echo_float(x: float, y: float) -> float { return x; }
    func echo_bool(x: int, y: int) -> bool { return x > 0; }
    `;
    const mod = flow.compile(source);
    
    let result = mod.call('echo_int', 42, 0);
    console.assert(result === 42, `Int conversion failed: ${result}`);
    
    result = mod.call('echo_float', 3.14, 0.0);
    console.assert(Math.abs(result - 3.14) < 0.01, `Float conversion failed: ${result}`);
    
    result = mod.call('echo_bool', 1, 0);
    console.assert(result === true, `Bool conversion failed: ${result}`);
    
    console.log('✓ All type conversions successful');
} catch (e) {
    console.log(`✗ Type conversion error: ${e.message}`);
}
console.log();

// Test 6: Invalid type
console.log('Test 6: Invalid type argument');
try {
    const source = 'func test(x: int) -> int { return x; }';
    const mod = flow.compile(source);
    const result = mod.call('test', {});  // Pass object (invalid)
    console.log('✗ Should have raised TypeError');
} catch (e) {
    if (e instanceof TypeError) {
        console.log(`✓ Expected error: ${e.message}`);
    } else {
        throw e;
    }
}
console.log();

console.log('✓ Error handling tests completed!');

