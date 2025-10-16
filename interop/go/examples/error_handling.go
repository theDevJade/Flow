// Flow Go Bindings - Error Handling Example
//
// Demonstrates error handling in the Flow Go bindings.
package main

import (
	"fmt"
	"os"
	
	flow "github.com/theDevJade/flow/interop/go"
)

func main() {
	fmt.Println("============================================================")
	fmt.Println("Flow Go Bindings - Error Handling Example")
	fmt.Println("============================================================")
	fmt.Println()
	
	// Initialize runtime
	runtime, err := flow.NewRuntime()
	if err != nil {
		fmt.Printf("Failed to initialize runtime: %v\n", err)
		os.Exit(1)
	}
	
	// Test 1: Loading non-existent file
	fmt.Println("Test 1: Loading non-existent file")
	_, err = flow.LoadModule(runtime, "nonexistent.flow")
	if err != nil {
		fmt.Printf("✓ Expected error: %v\n", err)
	} else {
		fmt.Println("✗ Should have raised error")
	}
	fmt.Println()
	
	// Test 2: Empty module
	fmt.Println("Test 2: Empty module compilation")
	mod, err := flow.Compile(runtime, "")
	if err != nil {
		if _, ok := err.(*flow.FlowCompileError); ok {
			fmt.Printf("✓ Expected compile error: %v\n", err)
		}
	} else {
		// Try calling non-existent function
		_, err = mod.Call("test", flow.NewInt(0), flow.NewInt(0))
		if err != nil {
			fmt.Println("✓ Expected error (empty module): Function not found")
		} else {
			fmt.Println("✗ Should have raised error")
		}
	}
	fmt.Println()
	
	// Test 3: Calling non-existent function
	fmt.Println("Test 3: Calling non-existent function")
	source := "func test(x: int) -> int { return x; }"
	mod, err = flow.Compile(runtime, source)
	if err != nil {
		fmt.Printf("✗ Unexpected compile error: %v\n", err)
		os.Exit(1)
	}
	
	_, err = mod.Call("nonexistent", flow.NewInt(1), flow.NewInt(0))
	if err != nil {
		if _, ok := err.(*flow.FlowRuntimeError); ok {
			fmt.Printf("✓ Expected error: %v\n", err)
		} else {
			fmt.Printf("✗ Wrong error type: %v\n", err)
		}
	} else {
		fmt.Println("✗ Should have raised FlowRuntimeError")
	}
	fmt.Println()
	
	// Test 4: Successful function call
	fmt.Println("Test 4: Successful function call")
	source = "func add(a: int, b: int) -> int { return a + b; }"
	mod, err = flow.Compile(runtime, source)
	if err != nil {
		fmt.Printf("✗ Unexpected error: %v\n", err)
		os.Exit(1)
	}
	
	result, err := mod.Call("add", flow.NewInt(10), flow.NewInt(20))
	if err != nil {
		fmt.Printf("✗ Unexpected error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("✓ Success: %d\n", result.AsInt())
	if result.AsInt() != 30 {
		fmt.Printf("✗ Expected 30, got %d\n", result.AsInt())
		os.Exit(1)
	}
	fmt.Println()
	
	// Test 5: Type conversions
	fmt.Println("Test 5: Type conversions")
	source = `
func echo_int(x: int, y: int) -> int { return x; }
func echo_float(x: float, y: float) -> float { return x; }
func echo_bool(x: int, y: int) -> bool { return x > 0; }
`
	mod, err = flow.Compile(runtime, source)
	if err != nil {
		fmt.Printf("✗ Unexpected error: %v\n", err)
		os.Exit(1)
	}
	
	result, err = mod.Call("echo_int", flow.NewInt(42), flow.NewInt(0))
	if err != nil || result.AsInt() != 42 {
		fmt.Printf("✗ Int conversion failed: %v\n", err)
		os.Exit(1)
	}
	
	result, err = mod.Call("echo_float", flow.NewFloat(3.14), flow.NewFloat(0.0))
	if err != nil {
		fmt.Printf("✗ Float conversion failed: %v\n", err)
		os.Exit(1)
	}
	
	result, err = mod.Call("echo_bool", flow.NewInt(1), flow.NewInt(0))
	if err != nil || !result.AsBool() {
		fmt.Printf("✗ Bool conversion failed: %v\n", err)
		os.Exit(1)
	}
	
	fmt.Println("✓ All type conversions successful")
	fmt.Println()
	
	fmt.Println("✓ Error handling tests completed!")
}

