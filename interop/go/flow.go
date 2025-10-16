


package flow


import "C"
import (
	"fmt"
	goruntime "runtime"
	"strings"
	"unsafe"
)






type FlowError struct {
	Message string
}

func (e *FlowError) Error() string {
	return e.Message
}

// FlowCompileError is returned when Flow code compilation fails
type FlowCompileError struct {
	FlowError
}

// FlowRuntimeError is returned when Flow function execution fails
type FlowRuntimeError struct {
	FlowError
}



// ValueType represents the type of a Flow value
type ValueType int

const (
	TypeInt    ValueType = 0
	TypeFloat  ValueType = 1
	TypeString ValueType = 2
	TypeBool   ValueType = 3
	TypeVoid   ValueType = 4
)

// Value represents a Flow value
type Value struct {
	cValue C.flow_value_t
}

// NewInt creates a Flow integer value
func NewInt(value int64) Value {
	return Value{cValue: C.flow_int(C.int64_t(value))}
}

// NewFloat creates a Flow float value
func NewFloat(value float64) Value {
	return Value{cValue: C.flow_float(C.double(value))}
}

// NewString creates a Flow string value
func NewString(value string) Value {
	cstr := C.CString(value)
	defer C.free(unsafe.Pointer(cstr))
	return Value{cValue: C.flow_string(cstr)}
}

// NewBool creates a Flow boolean value
func NewBool(value bool) Value {
	return Value{cValue: C.flow_bool(C.bool(value))}
}

// NewVoid creates a Flow void value
func NewVoid() Value {
	return Value{cValue: C.flow_void()}
}

// AsInt returns the value as an int64
func (v Value) AsInt() int64 {
	return int64(C.flow_as_int(v.cValue))
}

// AsFloat returns the value as a float64
func (v Value) AsFloat() float64 {
	return float64(C.flow_as_float(v.cValue))
}

// AsString returns the value as a string
func (v Value) AsString() string {
	cstr := C.flow_as_string(v.cValue)
	if cstr == nil {
		return ""
	}
	return C.GoString(cstr)
}

// AsBool returns the value as a bool
func (v Value) AsBool() bool {
	return bool(C.flow_as_bool(v.cValue))
}

// Type returns the type of the value
func (v Value) Type() ValueType {
	return ValueType(v.cValue._type)
}

// ============================================================================
// Runtime
// ============================================================================

var runtimeInitialized bool

// Runtime manages the Flow runtime
type Runtime struct {
	initialized bool
}

// NewRuntime creates a new Flow runtime
func NewRuntime() (*Runtime, error) {
	if !runtimeInitialized {
		ret := C.flow_init()
		if ret != 0 {
			cerr := C.flow_get_error()
			var errMsg string
			if cerr != nil {
				errMsg = C.GoString(cerr)
				C.flow_clear_error()
			} else {
				errMsg = "Unknown error"
			}
			return nil, &FlowError{Message: fmt.Sprintf("Failed to initialize Flow runtime: %s", errMsg)}
		}
		runtimeInitialized = true
	}

	r := &Runtime{initialized: true}

	// Register finalizer for cleanup
	goruntime.SetFinalizer(r, func(r *Runtime) {
		if runtimeInitialized {
			C.flow_cleanup()
			runtimeInitialized = false
		}
	})

	return r, nil
}

// IsInitialized returns whether the runtime is initialized
func (r *Runtime) IsInitialized() bool {
	return r.initialized
}

// ============================================================================
// Module
// ============================================================================

// Module represents a compiled Flow module
type Module struct {
	handle  *C.flow_module_t
	path    string
	runtime *Runtime
}

// Close unloads the module
func (m *Module) Close() error {
	if m.handle != nil {
		C.flow_unload_module(m.handle)
		m.handle = nil
	}
	return nil
}

// LoadModule loads a Flow module from a file
func LoadModule(runtime *Runtime, path string) (*Module, error) {
	cpath := C.CString(path)
	defer C.free(unsafe.Pointer(cpath))

	handle := C.flow_load_module(cpath)
	if handle == nil {
		cerr := C.flow_get_error()
		var errMsg string
		if cerr != nil {
			errMsg = C.GoString(cerr)
			C.flow_clear_error()
		} else {
			errMsg = "Unknown error"
		}
		return nil, &FlowCompileError{FlowError{Message: fmt.Sprintf("Failed to load module: %s", errMsg)}}
	}

	m := &Module{
		handle:  handle,
		path:    path,
		runtime: runtime,
	}

	goruntime.SetFinalizer(m, func(m *Module) {
		if m.handle != nil {
			C.flow_unload_module(m.handle)
		}
	})

	return m, nil
}

// Compile compiles Flow source code from a string
func Compile(runtime *Runtime, source string) (*Module, error) {
	csource := C.CString(source)
	defer C.free(unsafe.Pointer(csource))

	handle := C.flow_compile_string(csource)
	if handle == nil {
		cerr := C.flow_get_error()
		var errMsg string
		if cerr != nil {
			errMsg = C.GoString(cerr)
			C.flow_clear_error()
		} else {
			errMsg = "Unknown error"
		}
		return nil, &FlowCompileError{FlowError{Message: fmt.Sprintf("Failed to compile: %s", errMsg)}}
	}

	m := &Module{
		handle:  handle,
		path:    "<compiled>",
		runtime: runtime,
	}

	goruntime.SetFinalizer(m, func(m *Module) {
		if m.handle != nil {
			C.flow_unload_module(m.handle)
		}
	})

	return m, nil
}

// Call calls a Flow function
func (m *Module) Call(function string, args ...Value) (Value, error) {
	cfunc := C.CString(function)
	defer C.free(unsafe.Pointer(cfunc))

	// Convert arguments
	var cargs *C.flow_value_t
	if len(args) > 0 {
		// Allocate C array
		cargs = (*C.flow_value_t)(C.malloc(C.size_t(len(args)) * C.size_t(unsafe.Sizeof(C.flow_value_t{}))))
		defer C.free(unsafe.Pointer(cargs))

		// Copy values
		slice := (*[1 << 30]C.flow_value_t)(unsafe.Pointer(cargs))[:len(args):len(args)]
		for i, arg := range args {
			slice[i] = arg.cValue
		}
	}

	// Call the function
	result := C.flowc_call_v(m.handle, cfunc, C.int(len(args)), cargs)

	// Check for errors
	cerr := C.flow_get_error()
	if cerr != nil {
		errMsg := C.GoString(cerr)
		if errMsg != "" {
			C.flow_clear_error()
			return Value{}, &FlowRuntimeError{FlowError{Message: errMsg}}
		}
	}

	return Value{cValue: result}, nil
}

// Path returns the module's path
func (m *Module) Path() string {
	return m.path
}

// ============================================================
// REFLECTION API
// ============================================================

// FunctionInfo contains information about a Flow function
type FunctionInfo struct {
	Name       string
	ReturnType string
	Parameters []ParameterInfo
}

// ParameterInfo contains information about a function parameter
type ParameterInfo struct {
	Name string
	Type string
}

// GetFunctionCount returns the number of functions in this module
func (m *Module) GetFunctionCount() int {
	if m.handle == nil {
		return 0
	}
	return int(C.flow_reflect_function_count(m.handle))
}

// ListFunctions returns a list of all function names in this module
func (m *Module) ListFunctions() ([]string, error) {
	if m.handle == nil {
		return nil, fmt.Errorf("module is not loaded")
	}

	count := C.flow_reflect_function_count(m.handle)
	if count == 0 {
		return []string{}, nil
	}

	// Allocate array for function names
	var namesPtr **C.char
	actualCount := C.flow_reflect_list_functions(m.handle, &namesPtr)
	if actualCount <= 0 {
		return []string{}, nil
	}

	// Convert C array to Go slice
	names := make([]string, actualCount)
	namesPtrSlice := unsafe.Slice(namesPtr, actualCount)
	for i := 0; i < int(actualCount); i++ {
		names[i] = C.GoString(namesPtrSlice[i])
	}

	// Free the C array
	C.flow_reflect_free_names(namesPtr, actualCount)

	return names, nil
}

// GetFunctionInfo returns detailed information about a function
func (m *Module) GetFunctionInfo(functionName string) (*FunctionInfo, error) {
	if m.handle == nil {
		return nil, fmt.Errorf("module is not loaded")
	}

	cFuncName := C.CString(functionName)
	defer C.free(unsafe.Pointer(cFuncName))

	infoPtr := C.flow_reflect_get_function_info(m.handle, cFuncName)
	if infoPtr == nil {
		return nil, fmt.Errorf("function '%s' not found in module", functionName)
	}
	defer C.flow_reflect_free_function_info(infoPtr)

	info := &FunctionInfo{
		Name:       C.GoString(infoPtr.name),
		ReturnType: C.GoString(infoPtr.return_type),
		Parameters: make([]ParameterInfo, int(infoPtr.param_count)),
	}

	// Extract parameters
	if infoPtr.param_count > 0 {
		paramsSlice := unsafe.Slice(infoPtr.params, infoPtr.param_count)
		for i := 0; i < int(infoPtr.param_count); i++ {
			info.Parameters[i] = ParameterInfo{
				Name: C.GoString(paramsSlice[i].name),
				Type: C.GoString(paramsSlice[i]._type),
			}
		}
	}

	return info, nil
}

// Inspect returns a human-readable string representation of all functions in the module
func (m *Module) Inspect() (string, error) {
	functions, err := m.ListFunctions()
	if err != nil {
		return "", err
	}

	if len(functions) == 0 {
		return "Module contains no functions", nil
	}

	var builder strings.Builder
	fmt.Fprintf(&builder, "Module contains %d function(s):\n\n", len(functions))

	for _, funcName := range functions {
		info, err := m.GetFunctionInfo(funcName)
		if err != nil {
			fmt.Fprintf(&builder, "  %s (error getting info: %v)\n", funcName, err)
			continue
		}

		// Build parameter string
		var params []string
		for _, p := range info.Parameters {
			params = append(params, fmt.Sprintf("%s: %s", p.Name, p.Type))
		}

		fmt.Fprintf(&builder, "  %s(%s) -> %s\n",
			info.Name,
			strings.Join(params, ", "),
			info.ReturnType)
	}

	return builder.String(), nil
}

// ============================================================================
// Convenience Functions
// ============================================================================

// LoadModuleSimple loads a Flow module without explicit runtime management
func LoadModuleSimple(path string) (*Module, error) {
	runtime, err := NewRuntime()
	if err != nil {
		return nil, err
	}
	return LoadModule(runtime, path)
}

// CompileSimple compiles Flow source without explicit runtime management
func CompileSimple(source string) (*Module, error) {
	runtime, err := NewRuntime()
	if err != nil {
		return nil, err
	}
	return Compile(runtime, source)
}

// Version returns the Go bindings version
func Version() string {
	return "0.1.0"
}
