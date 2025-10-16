

const flow = require('../index');

console.log('='.repeat(60));
console.log('Flow JavaScript Bindings - Basic Example');
console.log('='.repeat(60));
console.log();

console.log('Compiling inline Flow code...');
const source = `
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
`;

const mod = flow.compile(source);
console.log('✓ Module compiled');
console.log();

// Test integer operations
console.log('Calling add(10, 20)...');
let result = mod.call('add', 10, 20);
console.log(`Result: ${result}`);
console.assert(result === 30, `Expected 30, got ${result}`);
console.log();

console.log('Calling multiply(6, 7)...');
result = mod.call('multiply', 6, 7);
console.log(`Result: ${result}`);
console.assert(result === 42, `Expected 42, got ${result}`);
console.log();

console.log('Calling subtract(100, 58)...');
result = mod.call('subtract', 100, 58);
console.log(`Result: ${result}`);
console.assert(result === 42, `Expected 42, got ${result}`);
console.log();

// Test boolean operations
console.log('Calling is_positive(42)...');
result = mod.call('is_positive', 42, 0);  // Need 2 args for FlowAPI
console.log(`Result: ${result}`);
console.assert(result === true, `Expected true, got ${result}`);
console.log();

// Test float operations
console.log('Calling square(5.0)...');
result = mod.call('square', 5.0, 0.0);  // Need 2 args for FlowAPI
console.log(`Result: ${result}`);
console.assert(Math.abs(result - 25.0) < 0.01, `Expected 25.0, got ${result}`);
console.log();

// Test method syntax
console.log('Using method syntax: mod.add(15, 27)...');
result = mod.add(15, 27);
console.log(`Result: ${result}`);
console.assert(result === 42, `Expected 42, got ${result}`);
console.log();

console.log('✓ All examples completed successfully!');

