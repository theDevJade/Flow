package flow_test

import (
	"math"
	"os"
	"path/filepath"
	"testing"

	flow "github.com/theDevJade/flow/interop/go"
)


var testRuntime *flow.Runtime

func TestMain(m *testing.M) {

	var err error
	testRuntime, err = flow.NewRuntime()
	if err != nil {
		panic("Failed to initialize Flow runtime: " + err.Error())
	}


	code := m.Run()


	os.Exit(code)
}

func TestInlineCompilation(t *testing.T) {
	source := "func test() -> int { return 42; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}
	if mod.Path() != "<compiled>" {
		t.Errorf("Expected path '<compiled>', got '%s'", mod.Path())
	}
}

func TestIntegerFunctionAdd(t *testing.T) {
	source := "func add(a: int, b: int) -> int { return a + b; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("add", flow.NewInt(10), flow.NewInt(20))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsInt() != 30 {
		t.Errorf("Expected 30, got %d", result.AsInt())
	}
}

func TestIntegerFunctionMultiply(t *testing.T) {
	source := "func multiply(a: int, b: int) -> int { return a * b; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("multiply", flow.NewInt(6), flow.NewInt(7))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsInt() != 42 {
		t.Errorf("Expected 42, got %d", result.AsInt())
	}
}

func TestBooleanFunctionPositive(t *testing.T) {
	source := "func is_positive(n: int) -> bool { return n > 0; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("is_positive", flow.NewInt(5), flow.NewInt(0))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if !result.AsBool() {
		t.Errorf("Expected true, got %v", result.AsBool())
	}
}

func TestBooleanFunctionNegative(t *testing.T) {
	source := "func is_positive(n: int) -> bool { return n > 0; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("is_positive", flow.NewInt(-3), flow.NewInt(0))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsBool() {
		t.Errorf("Expected false, got %v", result.AsBool())
	}
}

func TestFloatFunction(t *testing.T) {
	source := "func square(x: float) -> float { return x * x; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("square", flow.NewFloat(5.0), flow.NewFloat(0.0))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if math.Abs(result.AsFloat()-25.0) > 0.01 {
		t.Errorf("Expected 25.0, got %f", result.AsFloat())
	}
}

func TestFileLoading(t *testing.T) {
	testFile := filepath.Join("..", "c", "test_module.flow")
	if _, err := os.Stat(testFile); os.IsNotExist(err) {
		t.Skip("test_module.flow not found")
	}

	mod, err := flow.LoadModule(testRuntime, testFile)
	if err != nil {
		t.Fatalf("Failed to load module: %v", err)
	}

	result, err := mod.Call("add", flow.NewInt(3), flow.NewInt(5))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsInt() != 8 {
		t.Errorf("Expected 8, got %d", result.AsInt())
	}
}

func TestMultipleCallsSameModule(t *testing.T) {
	source := "func add(a: int, b: int) -> int { return a + b; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	r1, err := mod.Call("add", flow.NewInt(1), flow.NewInt(2))
	if err != nil {
		t.Fatalf("First call failed: %v", err)
	}
	if r1.AsInt() != 3 {
		t.Errorf("First call: expected 3, got %d", r1.AsInt())
	}

	r2, err := mod.Call("add", flow.NewInt(10), flow.NewInt(20))
	if err != nil {
		t.Fatalf("Second call failed: %v", err)
	}
	if r2.AsInt() != 30 {
		t.Errorf("Second call: expected 30, got %d", r2.AsInt())
	}

	r3, err := mod.Call("add", flow.NewInt(100), flow.NewInt(200))
	if err != nil {
		t.Fatalf("Third call failed: %v", err)
	}
	if r3.AsInt() != 300 {
		t.Errorf("Third call: expected 300, got %d", r3.AsInt())
	}
}

func TestErrorHandlingEmptyModule(t *testing.T) {
	mod, err := flow.Compile(testRuntime, "")
	if err != nil {

		if _, ok := err.(*flow.FlowCompileError); ok {
			return
		}
		t.Fatalf("Unexpected error: %v", err)
	}

	// Try calling non-existent function
	_, err = mod.Call("nonexistent", flow.NewInt(0), flow.NewInt(0))
	if err == nil {
		t.Error("Expected error for non-existent function")
	}
	if _, ok := err.(*flow.FlowRuntimeError); !ok {
		t.Errorf("Expected FlowRuntimeError, got %T", err)
	}
}

func TestErrorHandlingFunctionNotFound(t *testing.T) {
	source := "func test(x: int) -> int { return x; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	_, err = mod.Call("nonexistent", flow.NewInt(1), flow.NewInt(0))
	if err == nil {
		t.Error("Expected error for non-existent function")
	}
	if _, ok := err.(*flow.FlowRuntimeError); !ok {
		t.Errorf("Expected FlowRuntimeError, got %T", err)
	}
}

func TestTypeConversionIntegers(t *testing.T) {
	source := "func echo(x: int, y: int) -> int { return x; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	testValues := []int64{0, 1, -1, 42, -42, 1000, -1000}
	for _, val := range testValues {
		result, err := mod.Call("echo", flow.NewInt(val), flow.NewInt(0))
		if err != nil {
			t.Errorf("Call failed for value %d: %v", val, err)
			continue
		}
		if result.AsInt() != val {
			t.Errorf("Expected %d, got %d", val, result.AsInt())
		}
	}
}

func TestTypeConversionFloats(t *testing.T) {
	source := "func echo(x: float, y: float) -> float { return x; }"
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	testValues := []float64{0.0, 1.5, -1.5, 3.14159, -3.14159}
	for _, val := range testValues {
		result, err := mod.Call("echo", flow.NewFloat(val), flow.NewFloat(0.0))
		if err != nil {
			t.Errorf("Call failed for value %f: %v", val, err)
			continue
		}
		if math.Abs(result.AsFloat()-val) > 0.0001 {
			t.Errorf("Expected %f, got %f", val, result.AsFloat())
		}
	}
}

func TestTypeConversionBooleans(t *testing.T) {
	source := `
func return_true(x: int, y: int) -> bool { return true; }
func return_false(x: int, y: int) -> bool { return false; }
`
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	resultTrue, err := mod.Call("return_true", flow.NewInt(0), flow.NewInt(0))
	if err != nil {
		t.Fatalf("return_true call failed: %v", err)
	}
	if !resultTrue.AsBool() {
		t.Errorf("Expected true, got %v", resultTrue.AsBool())
	}

	resultFalse, err := mod.Call("return_false", flow.NewInt(0), flow.NewInt(0))
	if err != nil {
		t.Fatalf("return_false call failed: %v", err)
	}
	if resultFalse.AsBool() {
		t.Errorf("Expected false, got %v", resultFalse.AsBool())
	}
}

func TestComplexCalculation(t *testing.T) {
	source := `
func calculate(a: int, b: int) -> int {
    return (a + b) * 2;
}
`
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("calculate", flow.NewInt(5), flow.NewInt(7))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsInt() != 24 {
		t.Errorf("Expected 24, got %d", result.AsInt())
	}
}

func TestMultipleOperations(t *testing.T) {
	source := `
func add(a: int, b: int) -> int { return a + b; }
func multiply(a: int, b: int) -> int { return a * b; }
func subtract(a: int, b: int) -> int { return a - b; }
`
	mod, err := flow.Compile(testRuntime, source)
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}

	result, err := mod.Call("add", flow.NewInt(10), flow.NewInt(20))
	if err != nil {
		t.Fatalf("add failed: %v", err)
	}
	if result.AsInt() != 30 {
		t.Errorf("add: expected 30, got %d", result.AsInt())
	}

	result, err = mod.Call("multiply", flow.NewInt(6), flow.NewInt(7))
	if err != nil {
		t.Fatalf("multiply failed: %v", err)
	}
	if result.AsInt() != 42 {
		t.Errorf("multiply: expected 42, got %d", result.AsInt())
	}

	result, err = mod.Call("subtract", flow.NewInt(100), flow.NewInt(58))
	if err != nil {
		t.Fatalf("subtract failed: %v", err)
	}
	if result.AsInt() != 42 {
		t.Errorf("subtract: expected 42, got %d", result.AsInt())
	}
}

func TestVersion(t *testing.T) {
	version := flow.Version()
	if version == "" {
		t.Error("Version should not be empty")
	}
}

func TestModulePath(t *testing.T) {
	mod, err := flow.Compile(testRuntime, "func test(x: int) -> int { return x; }")
	if err != nil {
		t.Fatalf("Compilation failed: %v", err)
	}
	if mod.Path() != "<compiled>" {
		t.Errorf("Expected '<compiled>', got '%s'", mod.Path())
	}
}

func TestSimpleCompile(t *testing.T) {
	mod, err := flow.CompileSimple("func test(x: int) -> int { return x; }")
	if err != nil {
		t.Fatalf("CompileSimple failed: %v", err)
	}
	result, err := mod.Call("test", flow.NewInt(42), flow.NewInt(0))
	if err != nil {
		t.Fatalf("Call failed: %v", err)
	}
	if result.AsInt() != 42 {
		t.Errorf("Expected 42, got %d", result.AsInt())
	}
}

