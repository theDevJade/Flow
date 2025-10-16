package flow

import (
	"fmt"
	"strings"
	"testing"
)

func TestGetFunctionCount(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}

	code := `
func add(a: int, b: int) -> int {
	return a + b;
}

func subtract(x: int, y: int) -> int {
	return x - y;
}

func multiply(m: int, n: int) -> int {
	return m * n;
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	count := module.GetFunctionCount()
	if count != 3 {
		t.Errorf("Expected 3 functions, got %d", count)
	}
}

func TestListFunctions(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `
func greet(name: string) -> string {
	return "Hello, " + name;
}

func square(x: int) -> int {
	return x * x;
}

func is_positive(n: int) -> bool {
	return n > 0;
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	functions, err := module.ListFunctions()
	if err != nil {
		t.Fatalf("Failed to list functions: %v", err)
	}

	if len(functions) != 3 {
		t.Errorf("Expected 3 functions, got %d", len(functions))
	}

	expectedFuncs := []string{"greet", "square", "is_positive"}
	for _, expected := range expectedFuncs {
		found := false
		for _, actual := range functions {
			if actual == expected {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("Expected function '%s' not found in list: %v", expected, functions)
		}
	}
}

func TestGetFunctionInfo(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `
func add(a: int, b: int) -> int {
	return a + b;
}

func greet(name: string) -> string {
	return "Hello, " + name;
}

func get_pi() -> float {
	return 3.14159;
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	// Test function with parameters
	info, err := module.GetFunctionInfo("add")
	if err != nil {
		t.Fatalf("Failed to get function info: %v", err)
	}

	if info.Name != "add" {
		t.Errorf("Expected name 'add', got '%s'", info.Name)
	}
	if info.ReturnType != "int" {
		t.Errorf("Expected return type 'int', got '%s'", info.ReturnType)
	}
	if len(info.Parameters) != 2 {
		t.Errorf("Expected 2 parameters, got %d", len(info.Parameters))
	}
	if info.Parameters[0].Name != "a" || info.Parameters[0].Type != "int" {
		t.Errorf("Expected parameter 'a: int', got '%s: %s'", 
			info.Parameters[0].Name, info.Parameters[0].Type)
	}
	if info.Parameters[1].Name != "b" || info.Parameters[1].Type != "int" {
		t.Errorf("Expected parameter 'b: int', got '%s: %s'", 
			info.Parameters[1].Name, info.Parameters[1].Type)
	}

	// Test function with string parameter
	info, err = module.GetFunctionInfo("greet")
	if err != nil {
		t.Fatalf("Failed to get function info: %v", err)
	}
	if info.ReturnType != "string" {
		t.Errorf("Expected return type 'string', got '%s'", info.ReturnType)
	}
	if len(info.Parameters) != 1 || info.Parameters[0].Type != "string" {
		t.Errorf("Expected 1 string parameter, got %d parameters", len(info.Parameters))
	}

	// Test function with no parameters
	info, err = module.GetFunctionInfo("get_pi")
	if err != nil {
		t.Fatalf("Failed to get function info: %v", err)
	}
	if len(info.Parameters) != 0 {
		t.Errorf("Expected 0 parameters, got %d", len(info.Parameters))
	}
	if info.ReturnType != "float" {
		t.Errorf("Expected return type 'float', got '%s'", info.ReturnType)
	}
}

func TestGetFunctionInfoNonExistent(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `
func test() -> int {
	return 42;
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	_, err = module.GetFunctionInfo("nonexistent")
	if err == nil {
		t.Error("Expected error for non-existent function, got nil")
	}
}

func TestInspect(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `
func add(a: int, b: int) -> int {
	return a + b;
}

func greet(name: string) -> string {
	return "Hello";
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	inspection, err := module.Inspect()
	if err != nil {
		t.Fatalf("Failed to inspect module: %v", err)
	}

	if !strings.Contains(inspection, "add(a: int, b: int) -> int") {
		t.Errorf("Inspection should contain 'add' signature")
	}
	if !strings.Contains(inspection, "greet(name: string) -> string") {
		t.Errorf("Inspection should contain 'greet' signature")
	}
	if !strings.Contains(inspection, "2 function(s)") {
		t.Errorf("Inspection should mention 2 functions")
	}
}

func TestEmptyModule(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `// Empty module`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	count := module.GetFunctionCount()
	if count != 0 {
		t.Errorf("Expected 0 functions, got %d", count)
	}

	functions, err := module.ListFunctions()
	if err != nil {
		t.Fatalf("Failed to list functions: %v", err)
	}
	if len(functions) != 0 {
		t.Errorf("Expected empty function list, got %v", functions)
	}

	inspection, err := module.Inspect()
	if err != nil {
		t.Fatalf("Failed to inspect empty module: %v", err)
	}
	if !strings.Contains(strings.ToLower(inspection), "no functions") {
		t.Errorf("Inspection should mention no functions")
	}
}

func TestLargeModule(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	// Generate a large module with many functions
	var codeBuilder strings.Builder
	numFuncs := 20
	for i := 0; i < numFuncs; i++ {
		codeBuilder.WriteString(fmt.Sprintf(`
func func%d(x: int) -> int {
	return x * %d;
}
`, i, i))
	}

	module, err := Compile(runtime, codeBuilder.String())
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	count := module.GetFunctionCount()
	if count != numFuncs {
		t.Errorf("Expected %d functions, got %d", numFuncs, count)
	}

	functions, err := module.ListFunctions()
	if err != nil {
		t.Fatalf("Failed to list functions: %v", err)
	}
	if len(functions) != numFuncs {
		t.Errorf("Expected %d function names, got %d", numFuncs, len(functions))
	}

	// Verify we can get info for specific functions
	info, err := module.GetFunctionInfo("func0")
	if err != nil {
		t.Errorf("Failed to get info for func0: %v", err)
	}
	if info.Name != "func0" {
		t.Errorf("Expected 'func0', got '%s'", info.Name)
	}

	info, err = module.GetFunctionInfo("func19")
	if err != nil {
		t.Errorf("Failed to get info for func19: %v", err)
	}
	if info.Name != "func19" {
		t.Errorf("Expected 'func19', got '%s'", info.Name)
	}
}

func TestFunctionStillCallableAfterReflection(t *testing.T) {
	runtime, err := NewRuntime()
	if err != nil {
		t.Fatalf("Failed to initialize runtime: %v", err)
	}
	

	code := `
func add(a: int, b: int) -> int {
	return a + b;
}
`

	module, err := Compile(runtime, code)
	if err != nil {
		t.Fatalf("Failed to compile module: %v", err)
	}
	defer module.Close()

	// Do reflection
	count := module.GetFunctionCount()
	if count != 1 {
		t.Errorf("Expected 1 function, got %d", count)
	}

	info, err := module.GetFunctionInfo("add")
	if err != nil {
		t.Fatalf("Failed to get function info: %v", err)
	}
	if info.Name != "add" {
		t.Errorf("Expected name 'add', got '%s'", info.Name)
	}

	// Now call the function to ensure reflection didn't break it
	result, err := module.Call("add", NewInt(10), NewInt(20))
	if err != nil {
		t.Fatalf("Failed to call function after reflection: %v", err)
	}

	intVal := result.AsInt()
	if intVal != 30 {
		t.Errorf("Expected add(10, 20) = 30, got %d", intVal)
	}
}

