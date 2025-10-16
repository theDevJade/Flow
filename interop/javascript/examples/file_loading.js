

const flow = require('../index');
const path = require('path');
const fs = require('fs');

console.log('='.repeat(60));
console.log('Flow JavaScript Bindings - File Loading Example');
console.log('='.repeat(60));
console.log();

// Load module from file
const modulePath = path.resolve(__dirname, '../../c/test_module.flow');
if (!fs.existsSync(modulePath)) {
    console.error(`✗ Test module not found: ${modulePath}`);
    console.error('  Please ensure the C library tests have been run.');
    process.exit(1);
}

console.log(`Loading module from: ${modulePath}`);
const mod = flow.loadModule(modulePath);
console.log('✓ Module loaded successfully');
console.log();

// Test integer operations
console.log('Testing integer operations:');
let result = mod.call('add', 5, 3);
console.log(`  add(5, 3) = ${result}`);
console.assert(result === 8, `Expected 8, got ${result}`);

result = mod.call('multiply', 6, 7);
console.log(`  multiply(6, 7) = ${result}`);
console.assert(result === 42, `Expected 42, got ${result}`);

result = mod.call('subtract', 10, 3);
console.log(`  subtract(10, 3) = ${result}`);
console.assert(result === 7, `Expected 7, got ${result}`);
console.log();

// Test boolean operations
console.log('Testing boolean operations:');
result = mod.call('is_positive', 5, 0);
console.log(`  is_positive(5) = ${result}`);
console.assert(result === true, `Expected true, got ${result}`);

result = mod.call('is_positive', -3, 0);
console.log(`  is_positive(-3) = ${result}`);
console.assert(result === false, `Expected false, got ${result}`);
console.log();

// Test float operations
console.log('Testing float operations:');
result = mod.call('square', 4.5, 0.0);
console.log(`  square(4.5) = ${result}`);
console.assert(Math.abs(result - 20.25) < 0.01, `Expected 20.25, got ${result}`);
console.log();

// Test method syntax
console.log('Using method syntax:');
result = mod.add(100, 200);
console.log(`  mod.add(100, 200) = ${result}`);
console.assert(result === 300, `Expected 300, got ${result}`);

result = mod.multiply(12, 12);
console.log(`  mod.multiply(12, 12) = ${result}`);
console.assert(result === 144, `Expected 144, got ${result}`);
console.log();

console.log('✓ All tests passed!');

