use flow::{FlowRuntime, FlowModule, FlowValue};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Flow Rust Bindings - File Loading Example");
    println!("==========================================\n");
    
    // Initialize runtime
    let runtime = FlowRuntime::new()?;
    
    // Load a Flow module from file
    let module_path = "../c/test_module.flow";
    
    println!("Loading module from: {}", module_path);
    let module = match FlowModule::load(&runtime, module_path) {
        Ok(m) => {
            println!("✓ Module loaded successfully\n");
            m
        },
        Err(e) => {
            eprintln!("Failed to load module: {}", e);
            eprintln!("\nNote: Make sure test_module.flow exists at: {}", module_path);
            return Err(e.into());
        }
    };
    
    // Test various functions
    println!("Testing integer operations:");
    
    let result = module.call("add", &[FlowValue::Int(5), FlowValue::Int(3)])?;
    println!("  add(5, 3) = {:?}", result);
    
    let result = module.call("multiply", &[FlowValue::Int(6), FlowValue::Int(7)])?;
    println!("  multiply(6, 7) = {:?}", result);
    
    let result = module.call("subtract", &[FlowValue::Int(10), FlowValue::Int(3)])?;
    println!("  subtract(10, 3) = {:?}", result);
    
    println!("\nTesting boolean operations:");
    let result = module.call("is_positive", &[FlowValue::Int(5), FlowValue::Int(0)])?;
    println!("  is_positive(5) = {:?}", result);
    
    let result = module.call("is_positive", &[FlowValue::Int(-3), FlowValue::Int(0)])?;
    println!("  is_positive(-3) = {:?}", result);
    
    println!("\nTesting float operations:");
    let result = module.call("square", &[FlowValue::Float(4.5), FlowValue::Float(0.0)])?;
    println!("  square(4.5) = {:?}", result);
    
    println!("\n✓ All tests passed!");
    
    Ok(())
}

