// Flow Go Bindings - File Loading Example
//
// Demonstrates loading and using Flow modules from files.
package main

import (
	"fmt"
	"math"
	"os"
	"path/filepath"
	
	flow "github.com/theDevJade/flow/interop/go"
)

func main() {
	fmt.Println("============================================================")
	fmt.Println("Flow Go Bindings - File Loading Example")
	fmt.Println("============================================================")
	fmt.Println()
	
	// Initialize runtime
	runtime, err := flow.NewRuntime()
	if err != nil {
		fmt.Printf("Failed to initialize runtime: %v\n", err)
		os.Exit(1)
	}
	
	// Load module from file
	modulePath := filepath.Join("..", "..", "c", "test_module.flow")
	if _, err := os.Stat(modulePath); os.IsNotExist(err) {
		fmt.Printf("✗ Test module not found: %s\n", modulePath)
		fmt.Println("  Please ensure the C library tests have been run.")
		os.Exit(1)
	}
	
	fmt.Printf("Loading module from: %s\n", modulePath)
	mod, err := flow.LoadModule(runtime, modulePath)
	if err != nil {
		fmt.Printf("Failed to load module: %v\n", err)
		os.Exit(1)
	}
	fmt.Println("✓ Module loaded successfully")
	fmt.Println()
	
	// Test integer operations
	fmt.Println("Testing integer operations:")
	result, err := mod.Call("add", flow.NewInt(5), flow.NewInt(3))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  add(5, 3) = %d\n", result.AsInt())
	if result.AsInt() != 8 {
		fmt.Printf("✗ Expected 8, got %d\n", result.AsInt())
		os.Exit(1)
	}
	
	result, err = mod.Call("multiply", flow.NewInt(6), flow.NewInt(7))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  multiply(6, 7) = %d\n", result.AsInt())
	if result.AsInt() != 42 {
		fmt.Printf("✗ Expected 42, got %d\n", result.AsInt())
		os.Exit(1)
	}
	
	result, err = mod.Call("subtract", flow.NewInt(10), flow.NewInt(3))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  subtract(10, 3) = %d\n", result.AsInt())
	if result.AsInt() != 7 {
		fmt.Printf("✗ Expected 7, got %d\n", result.AsInt())
		os.Exit(1)
	}
	fmt.Println()
	
	// Test boolean operations
	fmt.Println("Testing boolean operations:")
	result, err = mod.Call("is_positive", flow.NewInt(5), flow.NewInt(0))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  is_positive(5) = %v\n", result.AsBool())
	if !result.AsBool() {
		fmt.Printf("✗ Expected true, got %v\n", result.AsBool())
		os.Exit(1)
	}
	
	result, err = mod.Call("is_positive", flow.NewInt(-3), flow.NewInt(0))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  is_positive(-3) = %v\n", result.AsBool())
	if result.AsBool() {
		fmt.Printf("✗ Expected false, got %v\n", result.AsBool())
		os.Exit(1)
	}
	fmt.Println()
	
	// Test float operations
	fmt.Println("Testing float operations:")
	result, err = mod.Call("square", flow.NewFloat(4.5), flow.NewFloat(0.0))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("  square(4.5) = %f\n", result.AsFloat())
	if math.Abs(result.AsFloat()-20.25) > 0.01 {
		fmt.Printf("✗ Expected 20.25, got %f\n", result.AsFloat())
		os.Exit(1)
	}
	fmt.Println()
	
	fmt.Println("✓ All tests passed!")
}

