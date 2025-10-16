use flow::{FlowRuntime, FlowModule, FlowValue};

#[test]
fn test_function_count() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}

func subtract(x: int, y: int) -> int {
    return x - y;
}

func multiply(m: int, n: int) -> int {
    return m * n;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let count = module.function_count();
    
    assert_eq!(count, 3, "Should have 3 functions");
}

#[test]
fn test_list_functions() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func greet(name: string) -> string {
    return "Hello, " + name;
}

func square(x: int) -> int {
    return x * x;
}

func is_positive(n: int) -> bool {
    return n > 0;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let functions = module.list_functions().unwrap();
    
    assert_eq!(functions.len(), 3, "Should have 3 function names");
    assert!(functions.contains(&String::from("greet")), "Should include 'greet'");
    assert!(functions.contains(&String::from("square")), "Should include 'square'");
    assert!(functions.contains(&String::from("is_positive")), "Should include 'is_positive'");
}

#[test]
fn test_function_info_with_parameters() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let info = module.function_info("add").unwrap();
    
    assert_eq!(info.name, "add", "Function name should be 'add'");
    assert_eq!(info.return_type, "int", "Return type should be 'int'");
    assert_eq!(info.parameters.len(), 2, "Should have 2 parameters");
    
    assert_eq!(info.parameters[0].name, "a", "First param name");
    assert_eq!(info.parameters[0].type_, "int", "First param type");
    assert_eq!(info.parameters[1].name, "b", "Second param name");
    assert_eq!(info.parameters[1].type_, "int", "Second param type");
}

#[test]
fn test_function_info_string_parameter() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func greet(name: string) -> string {
    return "Hello, " + name;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let info = module.function_info("greet").unwrap();
    
    assert_eq!(info.name, "greet");
    assert_eq!(info.return_type, "string");
    assert_eq!(info.parameters.len(), 1);
    assert_eq!(info.parameters[0].name, "name");
    assert_eq!(info.parameters[0].type_, "string");
}

#[test]
fn test_function_info_no_parameters() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func get_pi() -> float {
    return 3.14159;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let info = module.function_info("get_pi").unwrap();
    
    assert_eq!(info.name, "get_pi");
    assert_eq!(info.return_type, "float");
    assert_eq!(info.parameters.len(), 0, "Should have no parameters");
}

#[test]
fn test_function_info_nonexistent() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func test() -> int {
    return 42;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let result = module.function_info("nonexistent");
    
    assert!(result.is_err(), "Should return error for non-existent function");
}

#[test]
fn test_inspect_functions() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}

func greet(name: string) -> string {
    return "Hello";
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let inspection = module.inspect_functions().unwrap();
    
    assert!(inspection.contains("add(a: int, b: int) -> int"), "Should contain 'add' signature");
    assert!(inspection.contains("greet(name: string) -> string"), "Should contain 'greet' signature");
    assert!(inspection.contains("2 function(s)"), "Should mention 2 functions");
}

#[test]
fn test_boolean_return_types() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func is_positive(x: int) -> bool {
    return x > 0;
}

func is_even(n: int) -> bool {
    return n % 2 == 0;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    let info = module.function_info("is_positive").unwrap();
    assert_eq!(info.return_type, "bool");
    
    let functions = module.list_functions().unwrap();
    assert_eq!(functions.len(), 2);
}

#[test]
fn test_large_module() {
    let runtime = FlowRuntime::new().unwrap();
    
    // Generate a module with many functions
    let mut source = String::new();
    for i in 0..20 {
        source.push_str(&format!(
            "func func{}(x: int) -> int {{\n  return x * {};\n}}\n\n",
            i, i
        ));
    }
    
    let module = FlowModule::compile(&runtime, &source).unwrap();
    
    let count = module.function_count();
    assert_eq!(count, 20, "Should have 20 functions");
    
    let functions = module.list_functions().unwrap();
    assert_eq!(functions.len(), 20, "Should list 20 functions");
    
    // Check specific functions
    let info0 = module.function_info("func0").unwrap();
    assert_eq!(info0.name, "func0");
    
    let info19 = module.function_info("func19").unwrap();
    assert_eq!(info19.name, "func19");
}

#[test]
fn test_functions_still_callable_after_reflection() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}

func multiply(x: int, y: int) -> int {
    return x * y;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    // Do reflection
    let count = module.function_count();
    assert_eq!(count, 2);
    
    let info = module.function_info("add").unwrap();
    assert_eq!(info.name, "add");
    
    // Now call the functions to ensure reflection didn't break them
    let result = module.call("add", &[FlowValue::Int(10), FlowValue::Int(20)]).unwrap();
    match result {
        FlowValue::Int(v) => assert_eq!(v, 30, "add(10, 20) should return 30"),
        _ => panic!("Expected Int value"),
    }
    
    let result = module.call("multiply", &[FlowValue::Int(7), FlowValue::Int(6)]).unwrap();
    match result {
        FlowValue::Int(v) => assert_eq!(v, 42, "multiply(7, 6) should return 42"),
        _ => panic!("Expected Int value"),
    }
}

#[test]
fn test_mixed_parameter_types() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func process(a: int, b: float, c: string, d: bool) -> string {
    return "processed";
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let info = module.function_info("process").unwrap();
    
    assert_eq!(info.parameters.len(), 4);
    assert_eq!(info.parameters[0].type_, "int");
    assert_eq!(info.parameters[1].type_, "float");
    assert_eq!(info.parameters[2].type_, "string");
    assert_eq!(info.parameters[3].type_, "bool");
}

#[test]
fn test_empty_module() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = "// Just a comment";
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    let count = module.function_count();
    assert_eq!(count, 0, "Empty module should have 0 functions");
    
    let functions = module.list_functions().unwrap();
    assert_eq!(functions.len(), 0, "Empty module should list 0 functions");
    
    let inspection = module.inspect_functions().unwrap();
    assert!(inspection.to_lowercase().contains("no functions"), "Should mention no functions");
}

#[test]
fn test_float_return_type() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func calculate_pi() -> float {
    return 3.14159;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    let info = module.function_info("calculate_pi").unwrap();
    
    assert_eq!(info.return_type, "float");
    assert_eq!(info.parameters.len(), 0);
}

#[test]
fn test_multiple_string_functions() {
    let runtime = FlowRuntime::new().unwrap();
    
    let source = r#"
func concat(a: string, b: string) -> string {
    return a + b;
}

func repeat(s: string, n: int) -> string {
    return s;
}
"#;
    
    let module = FlowModule::compile(&runtime, source).unwrap();
    
    let info1 = module.function_info("concat").unwrap();
    assert_eq!(info1.parameters.len(), 2);
    assert_eq!(info1.parameters[0].type_, "string");
    assert_eq!(info1.parameters[1].type_, "string");
    
    let info2 = module.function_info("repeat").unwrap();
    assert_eq!(info2.parameters.len(), 2);
    assert_eq!(info2.parameters[0].type_, "string");
    assert_eq!(info2.parameters[1].type_, "int");
}

