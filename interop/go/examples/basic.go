// Flow Go Bindings - Basic Example
//
// Demonstrates basic usage of the Flow Go bindings including:
// - Runtime initialization
// - Inline code compilation
// - Calling Flow functions with different types
package main

import (
	"fmt"
	"math"
	"os"
	
	flow "github.com/theDevJade/flow/interop/go"
)

func main() {
	fmt.Println("============================================================")
	fmt.Println("Flow Go Bindings - Basic Example")
	fmt.Println("============================================================")
	fmt.Println()
	
	// Initialize runtime
	runtime, err := flow.NewRuntime()
	if err != nil {
		fmt.Printf("Failed to initialize runtime: %v\n", err)
		os.Exit(1)
	}
	fmt.Println("✓ Runtime initialized")
	fmt.Println()
	
	// Compile inline Flow code
	fmt.Println("Compiling inline Flow code...")
	source := `
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
`
	
	mod, err := flow.Compile(runtime, source)
	if err != nil {
		fmt.Printf("Compilation failed: %v\n", err)
		os.Exit(1)
	}
	fmt.Println("✓ Module compiled")
	fmt.Println()
	
	// Test integer operations
	fmt.Println("Calling add(10, 20)...")
	result, err := mod.Call("add", flow.NewInt(10), flow.NewInt(20))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("Result: %d\n", result.AsInt())
	if result.AsInt() != 30 {
		fmt.Printf("✗ Expected 30, got %d\n", result.AsInt())
		os.Exit(1)
	}
	fmt.Println()
	
	fmt.Println("Calling multiply(6, 7)...")
	result, err = mod.Call("multiply", flow.NewInt(6), flow.NewInt(7))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("Result: %d\n", result.AsInt())
	if result.AsInt() != 42 {
		fmt.Printf("✗ Expected 42, got %d\n", result.AsInt())
		os.Exit(1)
	}
	fmt.Println()
	
	fmt.Println("Calling subtract(100, 58)...")
	result, err = mod.Call("subtract", flow.NewInt(100), flow.NewInt(58))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("Result: %d\n", result.AsInt())
	if result.AsInt() != 42 {
		fmt.Printf("✗ Expected 42, got %d\n", result.AsInt())
		os.Exit(1)
	}
	fmt.Println()
	
	// Test boolean operations
	fmt.Println("Calling is_positive(42)...")
	result, err = mod.Call("is_positive", flow.NewInt(42), flow.NewInt(0))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("Result: %v\n", result.AsBool())
	if !result.AsBool() {
		fmt.Printf("✗ Expected true, got %v\n", result.AsBool())
		os.Exit(1)
	}
	fmt.Println()
	
	// Test float operations
	fmt.Println("Calling square(5.0)...")
	result, err = mod.Call("square", flow.NewFloat(5.0), flow.NewFloat(0.0))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("Result: %f\n", result.AsFloat())
	if math.Abs(result.AsFloat()-25.0) > 0.01 {
		fmt.Printf("✗ Expected 25.0, got %f\n", result.AsFloat())
		os.Exit(1)
	}
	fmt.Println()
	
	fmt.Println("✓ All examples completed successfully!")
}

