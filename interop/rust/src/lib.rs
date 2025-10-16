/*!
# Flow Language - Rust Bindings

Safe Rust bindings for calling Flow functions from Rust code.

## Usage

```rust
use flow::{FlowRuntime, FlowModule, FlowValue};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize runtime
    let runtime = FlowRuntime::new()?;

    // Load Flow module
    let module = FlowModule::load(&runtime, "mymodule.flow")?;

    // Call Flow function
    let result = module.call("add", &[
        FlowValue::Int(10),
        FlowValue::Int(20),
    ])?;

    println!("Result: {:?}", result);
    Ok(())
}
```
*/

use std::ffi::{CString, CStr, c_char, c_int};
use std::path::Path;
use std::ptr;
use std::fmt;





mod ffi {
    use super::*;

    #[repr(C)]
    #[derive(Debug, Copy, Clone)]
    pub enum FlowTypeT {
        Int = 0,
        Float = 1,
        String = 2,
        Bool = 3,
        Void = 4,
    }

    #[repr(C)]
    #[derive(Copy, Clone)]
    pub union FlowValueData {
        pub int_val: i64,
        pub float_val: f64,
        pub string_val: *const c_char,
        pub bool_val: bool,
    }

    #[repr(C)]
    #[derive(Copy, Clone)]
    pub struct FlowValueT {
        pub type_: FlowTypeT,
        pub data: FlowValueData,
    }

    #[repr(C)]
    pub struct FlowModuleT {
        _private: [u8; 0],
    }


    #[repr(C)]
    pub struct FlowParamInfo {
        pub name: *const c_char,
        pub type_: *const c_char,
    }

    #[repr(C)]
    pub struct FlowFunctionInfo {
        pub name: *const c_char,
        pub return_type: *const c_char,
        pub param_count: c_int,
        pub params: *const FlowParamInfo,
    }

    #[link(name = "flow")]
    extern "C" {

        pub fn flow_init() -> c_int;
        pub fn flow_cleanup();


        pub fn flow_load_module(path: *const c_char) -> *mut FlowModuleT;
        pub fn flow_compile_string(source: *const c_char) -> *mut FlowModuleT;
        pub fn flow_unload_module(module: *mut FlowModuleT);


        pub fn flowc_call_v(
            module: *mut FlowModuleT,
            function: *const c_char,
            argc: c_int,
            argv: *const FlowValueT
        ) -> FlowValueT;


        pub fn flow_int(value: i64) -> FlowValueT;
        pub fn flow_float(value: f64) -> FlowValueT;
        pub fn flow_string(value: *const c_char) -> FlowValueT;
        pub fn flow_bool(value: bool) -> FlowValueT;
        pub fn flow_void() -> FlowValueT;


        pub fn flow_as_int(value: FlowValueT) -> i64;
        pub fn flow_as_float(value: FlowValueT) -> f64;
        pub fn flow_as_string(value: FlowValueT) -> *const c_char;
        pub fn flow_as_bool(value: FlowValueT) -> bool;


        pub fn flow_get_error() -> *const c_char;
        pub fn flow_clear_error();





        // Module reflection
        pub fn flow_reflect_function_count(module: *mut FlowModuleT) -> c_int;
        pub fn flow_reflect_list_functions(module: *mut FlowModuleT, names_out: *mut *mut *const c_char) -> c_int;
        pub fn flow_reflect_free_names(names: *mut *const c_char, count: c_int);
        pub fn flow_reflect_function_name_at(module: *mut FlowModuleT, index: c_int) -> *const c_char;

        // Function reflection
        pub fn flow_reflect_get_function_info(module: *mut FlowModuleT, function_name: *const c_char) -> *const FlowFunctionInfo;
        pub fn flow_reflect_free_function_info(info: *const FlowFunctionInfo);

        // Bidirectional reflection
        pub fn flow_reflect_register_foreign_module(adapter: *const c_char, module_name: *const c_char, functions: *const *const c_char, count: c_int) -> c_int;
        pub fn flow_reflect_has_foreign_module(adapter: *const c_char, module_name: *const c_char) -> c_int;
        pub fn flow_reflect_foreign_function_count(adapter: *const c_char, module_name: *const c_char) -> c_int;
        pub fn flow_reflect_foreign_functions(adapter: *const c_char, module_name: *const c_char, names_out: *mut *mut *const c_char) -> c_int;
    }
}





/// Represents a Flow value
#[derive(Debug, Clone)]
pub enum FlowValue {
    Int(i64),
    Float(f64),
    String(String),
    Bool(bool),
    Void,
}

impl FlowValue {
    /// Convert Rust FlowValue to C FlowValueT
    fn to_c(&self) -> ffi::FlowValueT {
        unsafe {
            match self {
                FlowValue::Int(v) => ffi::flow_int(*v),
                FlowValue::Float(v) => ffi::flow_float(*v),
                FlowValue::String(s) => {
                    let c_str = CString::new(s.as_str()).unwrap();
                    ffi::flow_string(c_str.as_ptr())
                },
                FlowValue::Bool(v) => ffi::flow_bool(*v),
                FlowValue::Void => ffi::flow_void(),
            }
        }
    }

    /// Convert C FlowValueT to Rust FlowValue
    fn from_c(c_val: ffi::FlowValueT) -> Self {
        unsafe {
            match c_val.type_ {
                ffi::FlowTypeT::Int => {
                    FlowValue::Int(ffi::flow_as_int(c_val))
                },
                ffi::FlowTypeT::Float => {
                    FlowValue::Float(ffi::flow_as_float(c_val))
                },
                ffi::FlowTypeT::String => {
                    let c_str = ffi::flow_as_string(c_val);
                    if c_str.is_null() {
                        FlowValue::String(String::new())
                    } else {
                        let rust_str = CStr::from_ptr(c_str)
                            .to_string_lossy()
                            .into_owned();
                        FlowValue::String(rust_str)
                    }
                },
                ffi::FlowTypeT::Bool => {
                    FlowValue::Bool(ffi::flow_as_bool(c_val))
                },
                ffi::FlowTypeT::Void => FlowValue::Void,
            }
        }
    }
}

/// Flow runtime manager
pub struct FlowRuntime {
    initialized: bool,
}

impl FlowRuntime {
    /// Create and initialize a new Flow runtime
    pub fn new() -> Result<Self, FlowError> {
        unsafe {
            let ret = ffi::flow_init();
            if ret != 0 {
                let err = get_last_error();
                return Err(FlowError::InitializationError(err));
            }
        }

        Ok(FlowRuntime {
            initialized: true,
        })
    }
}

impl Drop for FlowRuntime {
    fn drop(&mut self) {
        if self.initialized {
            unsafe {
                ffi::flow_cleanup();
            }
        }
    }
}

/// Represents a compiled Flow module
pub struct FlowModule {
    handle: *mut ffi::FlowModuleT,
    path: String,
}

impl FlowModule {
    /// Load a Flow module from a file
    pub fn load<P: AsRef<Path>>(_runtime: &FlowRuntime, path: P) -> Result<Self, FlowError> {
        let path_str = path.as_ref().to_str()
            .ok_or_else(|| FlowError::InvalidPath)?;

        let c_path = CString::new(path_str)
            .map_err(|_| FlowError::InvalidPath)?;

        unsafe {
            let handle = ffi::flow_load_module(c_path.as_ptr());
            if handle.is_null() {
                let err = get_last_error();
                return Err(FlowError::CompileError(err));
            }

        Ok(FlowModule {
                handle,
            path: path_str.to_string(),
        })
        }
    }

    /// Compile Flow source code from a string
    pub fn compile(_runtime: &FlowRuntime, source: &str) -> Result<Self, FlowError> {
        let c_source = CString::new(source)
            .map_err(|_| FlowError::CompileError("Invalid source string".to_string()))?;

        unsafe {
            let handle = ffi::flow_compile_string(c_source.as_ptr());
            if handle.is_null() {
                let err = get_last_error();
                return Err(FlowError::CompileError(err));
            }

        Ok(FlowModule {
                handle,
            path: String::from("<compiled>"),
        })
        }
    }

    /// Call a Flow function
    pub fn call(&self, function: &str, args: &[FlowValue]) -> Result<FlowValue, FlowError> {
        let c_function = CString::new(function)
            .map_err(|_| FlowError::RuntimeError("Invalid function name".to_string()))?;

        // Convert Rust values to C values
        let c_args: Vec<ffi::FlowValueT> = args.iter()
            .map(|v| v.to_c())
            .collect();

        unsafe {
            let result = ffi::flowc_call_v(
                self.handle,
                c_function.as_ptr(),
                c_args.len() as c_int,
                c_args.as_ptr()
            );

            // Check for errors
            let err_ptr = ffi::flow_get_error();
            if !err_ptr.is_null() {
                let err_str = CStr::from_ptr(err_ptr).to_string_lossy().into_owned();
                if !err_str.is_empty() {
                    return Err(FlowError::RuntimeError(err_str));
                }
            }

            Ok(FlowValue::from_c(result))
        }
    }

    /// Get the path/name of this module
    pub fn path(&self) -> &str {
        &self.path
    }





    /// Get the number of functions in this module
    pub fn function_count(&self) -> usize {
        unsafe {
            ffi::flow_reflect_function_count(self.handle) as usize
        }
    }

    /// List all function names in this module
    pub fn list_functions(&self) -> Result<Vec<String>, FlowError> {
        unsafe {
            let count = ffi::flow_reflect_function_count(self.handle);
            if count == 0 {
                return Ok(Vec::new());
            }

            let mut names_ptr: *mut *const c_char = ptr::null_mut();
            let actual_count = ffi::flow_reflect_list_functions(self.handle, &mut names_ptr);

            if actual_count <= 0 || names_ptr.is_null() {
                return Ok(Vec::new());
            }

            let mut function_names = Vec::new();
            for i in 0..actual_count {
                let name_ptr = *names_ptr.offset(i as isize);
                if !name_ptr.is_null() {
                    let c_str = CStr::from_ptr(name_ptr);
                    function_names.push(c_str.to_string_lossy().into_owned());
                }
            }

            ffi::flow_reflect_free_names(names_ptr, actual_count);

            Ok(function_names)
        }
    }

    /// Get detailed information about a function
    pub fn function_info(&self, function_name: &str) -> Result<FunctionInfo, FlowError> {
        let c_func_name = CString::new(function_name)
            .map_err(|_| FlowError::RuntimeError("Invalid function name".to_string()))?;

        unsafe {
            let info_ptr = ffi::flow_reflect_get_function_info(self.handle, c_func_name.as_ptr());
            if info_ptr.is_null() {
                return Err(FlowError::RuntimeError(format!("Function '{}' not found in module", function_name)));
            }

            let info = &*info_ptr;
            let name = CStr::from_ptr(info.name).to_string_lossy().into_owned();
            let return_type = CStr::from_ptr(info.return_type).to_string_lossy().into_owned();

            let mut parameters = Vec::new();
            for i in 0..info.param_count {
                let param = &*info.params.offset(i as isize);
                parameters.push(ParameterInfo {
                    name: CStr::from_ptr(param.name).to_string_lossy().into_owned(),
                    type_: CStr::from_ptr(param.type_).to_string_lossy().into_owned(),
                });
            }

            let result = FunctionInfo {
                name,
                return_type,
                parameters,
            };

            ffi::flow_reflect_free_function_info(info_ptr);

            Ok(result)
        }
    }

    /// Get a human-readable string representation of all functions in the module
    pub fn inspect_functions(&self) -> Result<String, FlowError> {
        let functions = self.list_functions()?;

        if functions.is_empty() {
            return Ok(String::from("Module contains no functions"));
        }

        let mut lines = vec![format!("Module contains {} function(s):\n", functions.len())];

        for func_name in functions {
            match self.function_info(&func_name) {
                Ok(info) => {
                    let params_str: Vec<String> = info.parameters.iter()
                        .map(|p| format!("{}: {}", p.name, p.type_))
                        .collect();
                    lines.push(format!("  {}({}) -> {}",
                        info.name,
                        params_str.join(", "),
                        info.return_type
                    ));
                }
                Err(e) => {
                    lines.push(format!("  {} (error getting info: {})", func_name, e));
                }
            }
        }

        Ok(lines.join("\n"))
    }
}

impl Drop for FlowModule {
    fn drop(&mut self) {
        if !self.handle.is_null() {
            unsafe {
                ffi::flow_unload_module(self.handle);
            }
        }
    }
}





/// Information about a function parameter
#[derive(Debug, Clone)]
pub struct ParameterInfo {
    pub name: String,
    pub type_: String,
}

/// Information about a function
#[derive(Debug, Clone)]
pub struct FunctionInfo {
    pub name: String,
    pub return_type: String,
    pub parameters: Vec<ParameterInfo>,
}





/// Flow error type
#[derive(Debug)]
pub enum FlowError {
    InvalidPath,
    InitializationError(String),
    CompileError(String),
    RuntimeError(String),
}

impl fmt::Display for FlowError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            FlowError::InvalidPath => write!(f, "Invalid path"),
            FlowError::InitializationError(msg) => write!(f, "Initialization error: {}", msg),
            FlowError::CompileError(msg) => write!(f, "Compile error: {}", msg),
            FlowError::RuntimeError(msg) => write!(f, "Runtime error: {}", msg),
        }
    }
}

impl std::error::Error for FlowError {}

/// Get the last error from the C library
fn get_last_error() -> String {
    unsafe {
        let err_ptr = ffi::flow_get_error();
        if err_ptr.is_null() {
            return String::from("Unknown error");
        }

        CStr::from_ptr(err_ptr)
            .to_string_lossy()
            .into_owned()
    }
}





/// Example: Add two integers (callable from Flow)
#[no_mangle]
pub extern "C" fn rust_add(a: i64, b: i64) -> i64 {
    a + b
}

/// Example: Multiply two integers (callable from Flow)
#[no_mangle]
pub extern "C" fn rust_multiply(a: i64, b: i64) -> i64 {
    a * b
}

/// Example: Calculate fibonacci number (callable from Flow)
#[no_mangle]
pub extern "C" fn rust_fibonacci(n: i64) -> i64 {
    if n <= 1 {
        return n;
    }
    rust_fibonacci(n - 1) + rust_fibonacci(n - 2)
}

/// Example: String length (callable from Flow)
#[no_mangle]
pub extern "C" fn rust_string_length(s: *const c_char) -> i64 {
    if s.is_null() {
        return 0;
    }
    unsafe {
        CStr::from_ptr(s).to_bytes().len() as i64
    }
}





#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_rust_exports() {
        assert_eq!(rust_add(10, 20), 30);
        assert_eq!(rust_multiply(5, 6), 30);
        assert_eq!(rust_fibonacci(10), 55);
    }

    #[test]
    fn test_runtime_init() {
        let runtime = FlowRuntime::new();
        assert!(runtime.is_ok());
    }

    #[test]
    fn test_inline_compilation() {
        let runtime = FlowRuntime::new().unwrap();

        let source = r#"
func add(a: int, b: int) -> int {
    return a + b;
}
"#;

        let module = FlowModule::compile(&runtime, source);
        assert!(module.is_ok());

        let module = module.unwrap();
        let result = module.call("add", &[
            FlowValue::Int(15),
            FlowValue::Int(25),
        ]);

        assert!(result.is_ok());
        match result.unwrap() {
            FlowValue::Int(val) => assert_eq!(val, 40),
            _ => panic!("Expected integer result"),
        }
    }
}
