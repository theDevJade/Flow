/**
 * Flow Language - Node.js Bindings
 * 
 * Allows calling Flow functions from JavaScript/Node.js using FFI
 */

const koffi = require('koffi');
const fs = require('fs');
const path = require('path');

// ============================================================================
// Find and load the Flow C library
// ============================================================================

function findLibrary() {
    const libName = process.platform === 'darwin' ? 'libflow.dylib' : 'libflow.so';
    
    // Calculate path relative to this file
    const interopDir = path.resolve(__dirname, '..');
    const cLibPath = path.join(interopDir, 'c', libName);
    
    const searchPaths = [
        cLibPath,                           // Development: interop/c/
        `/usr/local/lib/${libName}`,        // System install
        `/usr/lib/${libName}`,              // System install
    ];
    
    for (const libPath of searchPaths) {
        if (fs.existsSync(libPath)) {
            return libPath;
        }
    }
    
    throw new Error(
        `Could not find ${libName}. Tried:\n` +
        searchPaths.map(p => `  - ${p}`).join('\n') +
        '\n\nPlease build the C library first (see interop/c/README.md)'
    );
}

// ============================================================================
// C Type Definitions
// ============================================================================

// flow_value_t structure
const FlowValue = koffi.struct('flow_value_t', {
    type: 'int',
    data: koffi.union({
        int_val: 'int64',
        float_val: 'double',
        string_val: 'char *',
        bool_val: 'bool',
    }),
});

// Load the library
const libPath = findLibrary();
const lib = koffi.load(libPath);

// Define functions
const flow_init = lib.func('int flow_init()');
const flow_cleanup = lib.func('void flow_cleanup()');
const flow_load_module = lib.func('void *flow_load_module(const char *path)');
const flow_compile_string = lib.func('void *flow_compile_string(const char *source)');
const flow_unload_module = lib.func('void flow_unload_module(void *module)');
const flowc_call_v = lib.func('flow_value_t flowc_call_v(void *module, const char *function, int argc, flow_value_t *argv)');

const flow_int = lib.func('flow_value_t flow_int(int64 value)');
const flow_float = lib.func('flow_value_t flow_float(double value)');
const flow_string = lib.func('flow_value_t flow_string(const char *value)');
const flow_bool = lib.func('flow_value_t flow_bool(bool value)');
const flow_void = lib.func('flow_value_t flow_void()');

const flow_as_int = lib.func('int64 flow_as_int(flow_value_t value)');
const flow_as_float = lib.func('double flow_as_float(flow_value_t value)');
const flow_as_string = lib.func('const char *flow_as_string(flow_value_t value)');
const flow_as_bool = lib.func('bool flow_as_bool(flow_value_t value)');

const flow_get_error = lib.func('const char *flow_get_error()');
const flow_clear_error = lib.func('void flow_clear_error()');

// ============================================================================
// Reflection API
// ============================================================================

const flow_param_info_t = koffi.struct('flow_param_info_t', {
    name: 'const char *',
    type: 'const char *',
});

const flow_function_info_t = koffi.struct('flow_function_info_t', {
    name: 'const char *',
    return_type: 'const char *',
    param_count: 'int',
    params: koffi.pointer(flow_param_info_t),
});

const flow_reflect_function_count = lib.func('int flow_reflect_function_count(void *module)');
const flow_reflect_list_functions = lib.func('int flow_reflect_list_functions(void *module, char ***names_out)');
const flow_reflect_free_names = lib.func('void flow_reflect_free_names(char **names, int count)');
const flow_reflect_function_name_at = lib.func('const char *flow_reflect_function_name_at(void *module, int index)');
const flow_reflect_get_function_info = lib.func('flow_function_info_t *flow_reflect_get_function_info(void *module, const char *function_name)');
const flow_reflect_free_function_info = lib.func('void flow_reflect_free_function_info(flow_function_info_t *info)');

// Bidirectional reflection
const flow_reflect_register_foreign_module = lib.func('int flow_reflect_register_foreign_module(const char *adapter, const char *module_name, char **functions, int count)');
const flow_reflect_has_foreign_module = lib.func('int flow_reflect_has_foreign_module(const char *adapter, const char *module_name)');
const flow_reflect_foreign_function_count = lib.func('int flow_reflect_foreign_function_count(const char *adapter, const char *module_name)');
const flow_reflect_foreign_functions = lib.func('int flow_reflect_foreign_functions(const char *adapter, const char *module_name, char ***names_out)');

// Initialize runtime
const initResult = flow_init();
if (initResult !== 0) {
    const error = flow_get_error();
    const errorMsg = error ? error : 'Unknown error';
    flow_clear_error();
    throw new Error(`Failed to initialize Flow runtime: ${errorMsg}`);
}

// Cleanup on exit
process.on('exit', () => {
    flow_cleanup();
});

// ============================================================================
// Exception Classes
// ============================================================================

class FlowError extends Error {
    constructor(message) {
        super(message);
        this.name = 'FlowError';
    }
}

class FlowCompileError extends FlowError {
    constructor(message) {
        super(message);
        this.name = 'FlowCompileError';
    }
}

class FlowRuntimeError extends FlowError {
    constructor(message) {
        super(message);
        this.name = 'FlowRuntimeError';
    }
}

// ============================================================================
// Type Conversion Helpers
// ============================================================================

function jsToFlow(value) {
    if (typeof value === 'boolean') {
        return flow_bool(value);
    } else if (typeof value === 'number') {
        if (Number.isInteger(value)) {
            return flow_int(value);
        } else {
            return flow_float(value);
        }
    } else if (typeof value === 'string') {
        return flow_string(value);
    } else if (value === null || value === undefined) {
        return flow_void();
    } else {
        throw new TypeError(`Unsupported type for Flow: ${typeof value}`);
    }
}

function flowToJs(value) {
    const type = value.type;
    
    const FLOW_VAL_INT = 0;
    const FLOW_VAL_FLOAT = 1;
    const FLOW_VAL_STRING = 2;
    const FLOW_VAL_BOOL = 3;
    const FLOW_VAL_VOID = 4;
    
    if (type === FLOW_VAL_INT) {
        return Number(flow_as_int(value));
    } else if (type === FLOW_VAL_FLOAT) {
        return flow_as_float(value);
    } else if (type === FLOW_VAL_STRING) {
        const str = flow_as_string(value);
        return str ? str : null;
    } else if (type === FLOW_VAL_BOOL) {
        return flow_as_bool(value);
    } else if (type === FLOW_VAL_VOID) {
        return null;
    } else {
        throw new FlowRuntimeError(`Unknown Flow type: ${type}`);
    }
}

// ============================================================================
// FlowModule Class
// ============================================================================

class FlowModule {
    constructor(handle, modulePath) {
        this._handle = handle;
        this._path = modulePath;
        
        // Return a proxy to allow calling functions as methods
        return new Proxy(this, {
            get(target, prop) {
                if (prop in target || prop === 'then' || prop === 'constructor') {
                    return target[prop];
                }
                
                // Return a function that calls the Flow function
                return (...args) => target.call(prop, ...args);
            }
        });
    }
    
    /**
     * Call a Flow function
     */
    call(functionName, ...args) {
        // Convert arguments
        const flowArgs = args.map(jsToFlow);
        
        // Call the function
        const result = flowc_call_v(this._handle, functionName, flowArgs.length, flowArgs);
        
        // Check for errors
        const error = flow_get_error();
        if (error && error.length > 0) {
            flow_clear_error();
            throw new FlowRuntimeError(error);
        }
        
        // Convert result
        return flowToJs(result);
    }
    
    getPath() {
        return this._path;
    }
    
    // ========================================================================
    // REFLECTION API
    // ========================================================================
    
    /**
     * Get the number of functions in this module
     * @returns {number} Number of functions
     */
    functionCount() {
        if (!this._handle || this._handle === koffi.nullptr) {
            throw new FlowError('Module is not loaded');
        }
        return flow_reflect_function_count(this._handle);
    }
    
    /**
     * List all function names in this module
     * @returns {string[]} Array of function names
     */
    listFunctions() {
        if (!this._handle || this._handle === koffi.nullptr) {
            throw new FlowError('Module is not loaded');
        }
        
        const count = flow_reflect_function_count(this._handle);
        if (count === 0) {
            return [];
        }
        
        // Use a simpler approach: get function names one at a time using function_name_at
        const functionNames = [];
        for (let i = 0; i < count; i++) {
            const namePtr = flow_reflect_function_name_at(this._handle, i);
            if (namePtr && namePtr !== koffi.nullptr) {
                functionNames.push(namePtr);
            }
        }
        
        return functionNames;
    }
    
    /**
     * Get detailed information about a function
     * @param {string} functionName - Name of the function
     * @returns {object} Function info with name, return_type, and parameters
     */
    functionInfo(functionName) {
        if (!this._handle || this._handle === koffi.nullptr) {
            throw new FlowError('Module is not loaded');
        }
        
        const infoPtr = flow_reflect_get_function_info(this._handle, functionName);
        if (!infoPtr || infoPtr === koffi.nullptr) {
            throw new FlowError(`Function '${functionName}' not found in module`);
        }
        
        try {
            // Read the function info struct
            const info = koffi.decode(infoPtr, flow_function_info_t);
            
            const result = {
                name: info.name,
                return_type: info.return_type,
                parameters: []
            };
            
            // Extract parameters
            if (info.param_count > 0 && info.params && info.params !== koffi.nullptr) {
                const paramsArray = koffi.decode(info.params, koffi.array(flow_param_info_t, info.param_count));
                for (let i = 0; i < info.param_count; i++) {
                    result.parameters.push({
                        name: paramsArray[i].name,
                        type: paramsArray[i].type
                    });
                }
            }
            
            return result;
        } finally {
            // Free the function info
            flow_reflect_free_function_info(infoPtr);
        }
    }
    
    /**
     * Get a human-readable string representation of all functions in the module
     * @returns {string} Formatted string with function signatures
     */
    inspect() {
        const functions = this.listFunctions();
        
        if (functions.length === 0) {
            return 'Module contains no functions';
        }
        
        let lines = [`Module contains ${functions.length} function(s):\n`];
        
        for (const funcName of functions) {
            try {
                const info = this.functionInfo(funcName);
                const params = info.parameters
                    .map(p => `${p.name}: ${p.type}`)
                    .join(', ');
                lines.push(`  ${info.name}(${params}) -> ${info.return_type}`);
            } catch (e) {
                lines.push(`  ${funcName} (error getting info: ${e.message})`);
            }
        }
        
        return lines.join('\n');
    }
    
    /**
     * Cleanup when module is garbage collected
     */
    [Symbol.for('nodejs.util.inspect.custom')]() {
        return `FlowModule { path: '${this._path}' }`;
    }
}

// ============================================================================
// Public API
// ============================================================================

/**
 * Load a Flow module from a file
 * 
 * @param {string} modulePath - Path to the .flow file
 * @returns {FlowModule} Module with callable functions
 */
function loadModule(modulePath) {
    if (!fs.existsSync(modulePath)) {
        throw new Error(`File not found: ${modulePath}`);
    }
    
    const handle = flow_load_module(modulePath);
    if (!handle || handle === koffi.nullptr) {
        const error = flow_get_error();
        const errorMsg = error ? error : 'Unknown error';
        flow_clear_error();
        throw new FlowCompileError(`Failed to load module: ${errorMsg}`);
    }
    
    return new FlowModule(handle, modulePath);
}

/**
 * Compile Flow source code from a string
 * 
 * @param {string} source - Flow source code
 * @returns {FlowModule} Module with callable functions
 */
function compile(source) {
    const handle = flow_compile_string(source);
    if (!handle || handle === koffi.nullptr) {
        const error = flow_get_error();
        const errorMsg = error ? error : 'Unknown error';
        flow_clear_error();
        throw new FlowCompileError(`Failed to compile: ${errorMsg}`);
    }
    
    return new FlowModule(handle, '<compiled>');
}

/**
 * Get Flow language version
 * 
 * @returns {string} Version string
 */
function version() {
    return '0.1.0';
}

// ============================================================================
// Exports
// ============================================================================

module.exports = {
    loadModule,
    compile,
    version,
    FlowModule,
    FlowError,
    FlowCompileError,
    FlowRuntimeError,
};
