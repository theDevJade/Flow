use flow::{FlowRuntime, FlowModule, FlowValue};

#[test]
fn test_runtime_creation() {
    let runtime = FlowRuntime::new();
    assert!(runtime.is_ok(), "Failed to create runtime");
}

#[test]
fn test_inline_compilation() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func double(x: int) -> int {
    return x * 2;
}
"#;
    
    let module = FlowModule::compile(&runtime, source);
    assert!(module.is_ok(), "Failed to compile module");
}

#[test]
fn test_function_call_integer() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let result = module.call("add", &[
        FlowValue::Int(15),
        FlowValue::Int(27),
    ]);
    
    assert!(result.is_ok(), "Function call failed");
    match result.unwrap() {
        FlowValue::Int(val) => assert_eq!(val, 42),
        _ => panic!("Expected integer result"),
    }
}

#[test]
fn test_function_call_float() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func square(x: float) -> float {
    return x * x;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let result = module.call("square", &[
        FlowValue::Float(5.0),
        FlowValue::Float(0.0),
    ]);
    
    assert!(result.is_ok(), "Function call failed");
    match result.unwrap() {
        FlowValue::Float(val) => assert!((val - 25.0).abs() < 0.001),
        _ => panic!("Expected float result"),
    }
}

#[test]
fn test_function_call_bool() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func is_positive(n: int) -> bool {
    return n > 0;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    let result = module.call("is_positive", &[
        FlowValue::Int(10),
        FlowValue::Int(0),
    ]).unwrap();
    
    match result {
        FlowValue::Bool(val) => assert_eq!(val, true),
        _ => panic!("Expected boolean result"),
    }
}

#[test]
fn test_error_nonexistent_file() {
    let runtime = FlowRuntime::new().unwrap();
    let result = FlowModule::load(&runtime, "nonexistent_file.flow");
    assert!(result.is_err(), "Should fail to load non-existent file");
}

#[test]
fn test_error_invalid_syntax() {
    let runtime = FlowRuntime::new().unwrap();
    let result = FlowModule::compile(&runtime, "invalid syntax here");
    assert!(result.is_err(), "Should fail to compile invalid syntax");
}

#[test]
fn test_multiple_calls() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func multiply(a: int, b: int) -> int {
    return a * b;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    // Multiple calls should work
    for i in 1..=5 {
        let result = module.call("multiply", &[
            FlowValue::Int(i),
            FlowValue::Int(2),
        ]).unwrap();
        
        match result {
            FlowValue::Int(val) => assert_eq!(val, i * 2),
            _ => panic!("Expected integer result"),
        }
    }
}

