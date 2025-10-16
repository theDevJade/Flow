<?php
/**
 * Flow PHP Bindings - Basic Example
 * 
 * Demonstrates basic usage of the Flow PHP bindings including:
 * - Runtime initialization
 * - Inline code compilation
 * - Calling Flow functions with different types
 * - Method syntax
 */

// Load Flow class
if (file_exists(__DIR__ . '/../vendor/autoload.php')) {
    require_once __DIR__ . '/../vendor/autoload.php';
} else {
    require_once __DIR__ . '/../src/Flow.php';
}

use Flow\Flow;

echo str_repeat("=", 60) . "\n";
echo "Flow PHP Bindings - Basic Example\n";
echo str_repeat("=", 60) . "\n\n";

echo "Compiling inline Flow code...\n";
$source = <<<'FLOW'
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
FLOW;

$mod = Flow::compile($source);
echo "✓ Module compiled\n\n";

// Test integer operations
echo "Calling add(10, 20)...\n";
$result = $mod->call('add', 10, 20);
echo "Result: $result\n";
assert($result === 30, "Expected 30, got $result");
echo "\n";

echo "Calling multiply(6, 7)...\n";
$result = $mod->call('multiply', 6, 7);
echo "Result: $result\n";
assert($result === 42, "Expected 42, got $result");
echo "\n";

echo "Calling subtract(100, 58)...\n";
$result = $mod->call('subtract', 100, 58);
echo "Result: $result\n";
assert($result === 42, "Expected 42, got $result");
echo "\n";

// Test boolean operations
echo "Calling is_positive(42)...\n";
$result = $mod->call('is_positive', 42, 0);  // Need 2 args for FlowAPI
echo "Result: " . ($result ? 'true' : 'false') . "\n";
assert($result === true, "Expected true, got " . var_export($result, true));
echo "\n";

// Test float operations
echo "Calling square(5.0)...\n";
$result = $mod->call('square', 5.0, 0.0);  // Need 2 args for FlowAPI
echo "Result: $result\n";
assert(abs($result - 25.0) < 0.01, "Expected 25.0, got $result");
echo "\n";

// Test method syntax
echo "Using method syntax: \$mod->add(15, 27)...\n";
$result = $mod->add(15, 27);
echo "Result: $result\n";
assert($result === 42, "Expected 42, got $result");
echo "\n";

echo "✓ All examples completed successfully!\n";

