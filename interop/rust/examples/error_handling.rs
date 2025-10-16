use flow::{FlowRuntime, FlowModule, FlowValue};

fn main() {
    println!("Flow Rust Bindings - Error Handling Example");
    println!("============================================\n");
    
    // Test 1: Runtime initialization
    println!("Test 1: Runtime initialization");
    match FlowRuntime::new() {
        Ok(_) => println!("✓ Runtime initialized successfully\n"),
        Err(e) => println!("✗ Failed to initialize runtime: {}\n", e),
    }
    
    let runtime = FlowRuntime::new().unwrap();
    
    // Test 2: Invalid file path
    println!("Test 2: Loading non-existent file");
    match FlowModule::load(&runtime, "nonexistent.flow") {
        Ok(_) => println!("✗ Should have failed\n"),
        Err(e) => println!("✓ Expected error: {}\n", e),
    }
    
    // Test 3: Invalid syntax
    println!("Test 3: Compiling invalid syntax");
    let bad_source = "func invalid syntax here";
    match FlowModule::compile(&runtime, bad_source) {
        Ok(_) => println!("✗ Should have failed\n"),
        Err(e) => println!("✓ Expected error: {}\n", e),
    }
    
    // Test 4: Calling non-existent function
    println!("Test 4: Calling non-existent function");
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}
"#;
    let module = FlowModule::compile(&runtime, source).unwrap();
    match module.call("nonexistent", &[FlowValue::Int(1)]) {
        Ok(_) => println!("✗ Should have failed\n"),
        Err(e) => println!("✓ Expected error: {}\n", e),
    }
    
    // Test 5: Successful call for comparison
    println!("Test 5: Successful function call");
    match module.call("add", &[FlowValue::Int(10), FlowValue::Int(20)]) {
        Ok(result) => println!("✓ Success: {:?}\n", result),
        Err(e) => println!("✗ Unexpected error: {}\n", e),
    }
    
    println!("✓ Error handling tests completed!");
}

