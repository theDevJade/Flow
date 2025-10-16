<?php
/**
 * Flow PHP Bindings - Error Handling Example
 * 
 * Demonstrates error handling in the Flow PHP bindings.
 */

// Load Flow class
if (file_exists(__DIR__ . '/../vendor/autoload.php')) {
    require_once __DIR__ . '/../vendor/autoload.php';
} else {
    require_once __DIR__ . '/../src/Flow.php';
}

use Flow\Flow;
use Flow\FlowCompileException;
use Flow\FlowRuntimeException;

echo str_repeat("=", 60) . "\n";
echo "Flow PHP Bindings - Error Handling Example\n";
echo str_repeat("=", 60) . "\n\n";

// Test 1: Loading non-existent file
echo "Test 1: Loading non-existent file\n";
try {
    $mod = Flow::loadModule('nonexistent.flow');
    echo "✗ Should have raised InvalidArgumentException\n";
} catch (\InvalidArgumentException $e) {
    echo "✓ Expected error: " . $e->getMessage() . "\n";
}
echo "\n";

// Test 2: Empty module
echo "Test 2: Empty module compilation\n";
try {
    // Note: Parser has error recovery, so syntax errors might not raise immediately
    $mod = Flow::compile('');
    // Try calling a non-existent function
    try {
        $result = $mod->call('test', 0, 0);
        echo "✗ Should have raised error\n";
    } catch (FlowRuntimeException $e) {
        echo "✓ Expected error (empty module): Function not found\n";
    }
} catch (FlowCompileException $e) {
    echo "✓ Expected compile error: " . $e->getMessage() . "\n";
}
echo "\n";

// Test 3: Calling non-existent function
echo "Test 3: Calling non-existent function\n";
try {
    $source = 'func test(x: int) -> int { return x; }';
    $mod = Flow::compile($source);
    $result = $mod->call('nonexistent', 1, 0);
    echo "✗ Should have raised FlowRuntimeException\n";
} catch (FlowRuntimeException $e) {
    echo "✓ Expected error: " . $e->getMessage() . "\n";
}
echo "\n";

// Test 4: Successful function call
echo "Test 4: Successful function call\n";
try {
    $source = 'func add(a: int, b: int) -> int { return a + b; }';
    $mod = Flow::compile($source);
    $result = $mod->call('add', 10, 20);
    echo "✓ Success: $result\n";
    assert($result === 30);
} catch (\Exception $e) {
    echo "✗ Unexpected error: " . $e->getMessage() . "\n";
}
echo "\n";

// Test 5: Type conversions
echo "Test 5: Type conversions\n";
try {
    $source = <<<'FLOW'
    func echo_int(x: int, y: int) -> int { return x; }
    func echo_float(x: float, y: float) -> float { return x; }
    func echo_bool(x: int, y: int) -> bool { return x > 0; }
    FLOW;
    $mod = Flow::compile($source);
    
    $result = $mod->call('echo_int', 42, 0);
    assert($result === 42, "Int conversion failed: $result");
    
    $result = $mod->call('echo_float', 3.14, 0.0);
    assert(abs($result - 3.14) < 0.01, "Float conversion failed: $result");
    
    $result = $mod->call('echo_bool', 1, 0);
    assert($result === true, "Bool conversion failed: $result");
    
    echo "✓ All type conversions successful\n";
} catch (\Exception $e) {
    echo "✗ Type conversion error: " . $e->getMessage() . "\n";
}
echo "\n";

// Test 6: Invalid type
echo "Test 6: Invalid type argument\n";
try {
    $source = 'func test(x: int) -> int { return x; }';
    $mod = Flow::compile($source);
    $result = $mod->call('test', []); // Pass array (invalid)
    echo "✗ Should have raised InvalidArgumentException\n";
} catch (\InvalidArgumentException $e) {
    echo "✓ Expected error: " . $e->getMessage() . "\n";
}
echo "\n";

echo "✓ Error handling tests completed!\n";

