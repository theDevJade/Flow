#!/usr/bin/env php
<?php


require_once __DIR__ . '/src/Flow.php';

use Flow\Flow;
use Flow\FlowException;

$testsPassed = 0;
$testsFailed = 0;

function test(string $name, callable $fn): void
{
    global $testsPassed, $testsFailed;

    try {
        $fn();
        echo "✓ $name\n";
        $testsPassed++;
    } catch (Exception $e) {
        echo "✗ $name\n";
        echo "  {$e->getMessage()}\n";
        $testsFailed++;
    }
}

function assertEqual($actual, $expected, string $message): void
{
    if ($actual !== $expected) {
        throw new Exception("$message: expected " . var_export($expected, true) . ", got " . var_export($actual, true));
    }
}

function assertArrayEqual(array $actual, array $expected, string $message): void
{
    sort($actual);
    sort($expected);
    if ($actual !== $expected) {
        throw new Exception("$message: expected " . json_encode($expected) . ", got " . json_encode($actual));
    }
}

function assertTrue(bool $condition, string $message): void
{
    if (!$condition) {
        throw new Exception($message);
    }
}

function assertContains(string $haystack, string $needle, string $message): void
{
    if (strpos($haystack, $needle) === false) {
        throw new Exception("$message: '$haystack' does not contain '$needle'");
    }
}

echo "============================================================\n";
echo "Flow PHP Reflection API Tests\n";
echo "============================================================\n\n";


test('test_function_count', function() {
    $code = <<<'FLOW'
func add(a: int, b: int) -> int {
    return a + b;
}

func subtract(x: int, y: int) -> int {
    return x - y;
}

func multiply(m: int, n: int) -> int {
    return m * n;
}
FLOW;

    $module = Flow::compile($code);
    $count = $module->functionCount();

    assertEqual($count, 3, 'Should have 3 functions');
});


test('test_list_functions', function() {
    $code = <<<'FLOW'
func greet(name: string) -> string {
    return "Hello, " + name;
}

func square(x: int) -> int {
    return x * x;
}

func is_positive(n: int) -> bool {
    return n > 0;
}
FLOW;

    $module = Flow::compile($code);
    $functions = $module->listFunctions();

    assertEqual(count($functions), 3, 'Should have 3 function names');
    assertArrayEqual($functions, ['greet', 'square', 'is_positive'], 'Function names should match');
});


test('test_function_info_with_parameters', function() {
    $code = <<<'FLOW'
func add(a: int, b: int) -> int {
    return a + b;
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('add');

    assertEqual($info['name'], 'add', 'Function name should be add');
    assertEqual($info['return_type'], 'int', 'Return type should be int');
    assertEqual(count($info['parameters']), 2, 'Should have 2 parameters');
    assertEqual($info['parameters'][0]['name'], 'a', 'First param name');
    assertEqual($info['parameters'][0]['type'], 'int', 'First param type');
    assertEqual($info['parameters'][1]['name'], 'b', 'Second param name');
    assertEqual($info['parameters'][1]['type'], 'int', 'Second param type');
});


test('test_function_info_string_parameter', function() {
    $code = <<<'FLOW'
func greet(name: string) -> string {
    return "Hello, " + name;
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('greet');

    assertEqual($info['name'], 'greet', 'Function name');
    assertEqual($info['return_type'], 'string', 'Return type');
    assertEqual(count($info['parameters']), 1, 'Parameter count');
    assertEqual($info['parameters'][0]['name'], 'name', 'Param name');
    assertEqual($info['parameters'][0]['type'], 'string', 'Param type');
});


test('test_function_info_no_parameters', function() {
    $code = <<<'FLOW'
func get_pi() -> float {
    return 3.14159;
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('get_pi');

    assertEqual($info['name'], 'get_pi', 'Function name');
    assertEqual($info['return_type'], 'float', 'Return type');
    assertEqual(count($info['parameters']), 0, 'Should have no parameters');
});


test('test_function_info_nonexistent', function() {
    $code = <<<'FLOW'
func test() -> int {
    return 42;
}
FLOW;

    $module = Flow::compile($code);

    $threw = false;
    try {
        $module->functionInfo('nonexistent');
    } catch (FlowException $e) {
        $threw = true;
        assertContains($e->getMessage(), 'not found', 'Error message should mention not found');
    }

    assertTrue($threw, 'Should throw error for non-existent function');
});


test('test_inspect_functions', function() {
    $code = <<<'FLOW'
func add(a: int, b: int) -> int {
    return a + b;
}

func greet(name: string) -> string {
    return "Hello";
}
FLOW;

    $module = Flow::compile($code);
    $inspection = $module->inspect();

    assertContains($inspection, 'add(a: int, b: int) -> int', 'Should contain add signature');
    assertContains($inspection, 'greet(name: string) -> string', 'Should contain greet signature');
    assertContains($inspection, '2 function(s)', 'Should mention 2 functions');
});


test('test_boolean_return_types', function() {
    $code = <<<'FLOW'
func is_positive(x: int) -> bool {
    return x > 0;
}

func is_even(n: int) -> bool {
    return n % 2 == 0;
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('is_positive');

    assertEqual($info['return_type'], 'bool', 'Return type should be bool');
    assertEqual($module->functionCount(), 2, 'Should have 2 functions');
});


test('test_large_module', function() {
    $code = '';
    for ($i = 0; $i < 20; $i++) {
        $code .= "func func$i(x: int) -> int { return x * $i; }\n";
    }

    $module = Flow::compile($code);
    $count = $module->functionCount();

    assertEqual($count, 20, 'Should have 20 functions');

    $functions = $module->listFunctions();
    assertEqual(count($functions), 20, 'Should list 20 functions');

    $info0 = $module->functionInfo('func0');
    assertEqual($info0['name'], 'func0', 'First function name');

    $info19 = $module->functionInfo('func19');
    assertEqual($info19['name'], 'func19', 'Last function name');
});


test('test_functions_still_callable_after_reflection', function() {
    $code = <<<'FLOW'
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(x: int, y: int) -> int {
    return x * y;
}
FLOW;

    $module = Flow::compile($code);

    // Do reflection
    $count = $module->functionCount();
    assertEqual($count, 2, 'Should have 2 functions');

    $info = $module->functionInfo('add');
    assertEqual($info['name'], 'add', 'Function name should be add');

    // Now call the functions to ensure reflection didn't break them
    $result1 = $module->call('add', 10, 20);
    assertEqual($result1, 30, 'add(10, 20) should return 30');

    $result2 = $module->call('multiply', 7, 6);
    assertEqual($result2, 42, 'multiply(7, 6) should return 42');
});

// Test 11: Mixed parameter types
test('test_mixed_parameter_types', function() {
    $code = <<<'FLOW'
func process(a: int, b: float, c: string, d: bool) -> string {
    return "processed";
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('process');

    assertEqual(count($info['parameters']), 4, 'Should have 4 parameters');
    assertEqual($info['parameters'][0]['type'], 'int', 'First param type');
    assertEqual($info['parameters'][1]['type'], 'float', 'Second param type');
    assertEqual($info['parameters'][2]['type'], 'string', 'Third param type');
    assertEqual($info['parameters'][3]['type'], 'bool', 'Fourth param type');
});

// Test 12: Empty module
test('test_empty_module', function() {
    $code = '// Just a comment';

    $module = Flow::compile($code);
    $count = $module->functionCount();

    assertEqual($count, 0, 'Empty module should have 0 functions');

    $functions = $module->listFunctions();
    assertEqual(count($functions), 0, 'Empty module should list 0 functions');

    $inspection = $module->inspect();
    assertContains(strtolower($inspection), 'no functions', 'Should mention no functions');
});

// Test 13: Float return type
test('test_float_return_type', function() {
    $code = <<<'FLOW'
func calculate_pi() -> float {
    return 3.14159;
}
FLOW;

    $module = Flow::compile($code);
    $info = $module->functionInfo('calculate_pi');

    assertEqual($info['return_type'], 'float', 'Return type should be float');
    assertEqual(count($info['parameters']), 0, 'Should have no parameters');
});

// Test 14: Multiple string functions
test('test_multiple_string_functions', function() {
    $code = <<<'FLOW'
func concat(a: string, b: string) -> string {
    return a + b;
}

func repeat(s: string, n: int) -> string {
    return s;
}
FLOW;

    $module = Flow::compile($code);

    $info1 = $module->functionInfo('concat');
    assertEqual(count($info1['parameters']), 2, 'concat should have 2 parameters');
    assertEqual($info1['parameters'][0]['type'], 'string', 'First param type');
    assertEqual($info1['parameters'][1]['type'], 'string', 'Second param type');

    $info2 = $module->functionInfo('repeat');
    assertEqual(count($info2['parameters']), 2, 'repeat should have 2 parameters');
    assertEqual($info2['parameters'][0]['type'], 'string', 'First param type');
    assertEqual($info2['parameters'][1]['type'], 'int', 'Second param type');
});

// Print summary
echo "\n============================================================\n";
echo "Tests passed: $testsPassed\n";
echo "Tests failed: $testsFailed\n";
echo "============================================================\n";

exit($testsFailed > 0 ? 1 : 0);

