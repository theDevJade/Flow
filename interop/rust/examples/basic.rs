use flow::{FlowRuntime, FlowModule, FlowValue};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Flow Rust Bindings - Basic Example");
    println!("===================================\n");
    
    // Initialize runtime
    println!("Initializing Flow runtime...");
    let runtime = FlowRuntime::new()?;
    println!("✓ Runtime initialized\n");
    
    // Compile inline Flow code
    println!("Compiling inline Flow code...");
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(x: int, y: int) -> int {
    return x * y;
}

func subtract(a: int, b: int) -> int {
    return a - b;
}

func is_positive(n: int) -> bool {
    return n > 0;
}
"#;
    
    let module = FlowModule::compile(&runtime, source)?;
    println!("✓ Module compiled\n");
    
    // Call integer function
    println!("Calling add(10, 20)...");
    let result = module.call("add", &[
        FlowValue::Int(10),
        FlowValue::Int(20),
    ])?;
    println!("Result: {:?}\n", result);
    
    // Call multiplication
    println!("Calling multiply(6, 7)...");
    let result = module.call("multiply", &[
        FlowValue::Int(6),
        FlowValue::Int(7),
    ])?;
    println!("Result: {:?}\n", result);
    
    // Call subtraction
    println!("Calling subtract(100, 58)...");
    let result = module.call("subtract", &[
        FlowValue::Int(100),
        FlowValue::Int(58),
    ])?;
    println!("Result: {:?}\n", result);
    
    // Call boolean function
    println!("Calling is_positive(42)...");
    let result = module.call("is_positive", &[
        FlowValue::Int(42),
        FlowValue::Int(0),  // FlowAPI requires 2 args for bool functions
    ])?;
    println!("Result: {:?}\n", result);
    
    println!("✓ All examples completed successfully!");
    
    Ok(())
}

