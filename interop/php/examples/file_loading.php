<?php
/**
 * Flow PHP Bindings - File Loading Example
 * 
 * Demonstrates loading and using Flow modules from files.
 */

// Load Flow class
if (file_exists(__DIR__ . '/../vendor/autoload.php')) {
    require_once __DIR__ . '/../vendor/autoload.php';
} else {
    require_once __DIR__ . '/../src/Flow.php';
}

use Flow\Flow;

echo str_repeat("=", 60) . "\n";
echo "Flow PHP Bindings - File Loading Example\n";
echo str_repeat("=", 60) . "\n\n";

// Load module from file
$modulePath = __DIR__ . '/../../c/test_module.flow';
if (!file_exists($modulePath)) {
    echo "✗ Test module not found: $modulePath\n";
    echo "  Please ensure the C library tests have been run.\n";
    exit(1);
}

echo "Loading module from: $modulePath\n";
$mod = Flow::loadModule($modulePath);
echo "✓ Module loaded successfully\n\n";

// Test integer operations
echo "Testing integer operations:\n";
$result = $mod->call('add', 5, 3);
echo "  add(5, 3) = $result\n";
assert($result === 8, "Expected 8, got $result");

$result = $mod->call('multiply', 6, 7);
echo "  multiply(6, 7) = $result\n";
assert($result === 42, "Expected 42, got $result");

$result = $mod->call('subtract', 10, 3);
echo "  subtract(10, 3) = $result\n";
assert($result === 7, "Expected 7, got $result");
echo "\n";

// Test boolean operations
echo "Testing boolean operations:\n";
$result = $mod->call('is_positive', 5, 0);
echo "  is_positive(5) = " . ($result ? 'true' : 'false') . "\n";
assert($result === true, "Expected true, got " . var_export($result, true));

$result = $mod->call('is_positive', -3, 0);
echo "  is_positive(-3) = " . ($result ? 'true' : 'false') . "\n";
assert($result === false, "Expected false, got " . var_export($result, true));
echo "\n";

// Test float operations
echo "Testing float operations:\n";
$result = $mod->call('square', 4.5, 0.0);
echo "  square(4.5) = $result\n";
assert(abs($result - 20.25) < 0.01, "Expected 20.25, got $result");
echo "\n";

// Test method syntax
echo "Using method syntax:\n";
$result = $mod->add(100, 200);
echo "  \$mod->add(100, 200) = $result\n";
assert($result === 300, "Expected 300, got $result");

$result = $mod->multiply(12, 12);
echo "  \$mod->multiply(12, 12) = $result\n";
assert($result === 144, "Expected 144, got $result");
echo "\n";

echo "✓ All tests passed!\n";

