#ifndef FLOW_EMBEDDING_API_H
#define FLOW_EMBEDDING_API_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif







typedef struct FlowRuntime FlowRuntime;
typedef struct FlowModule FlowModule;
typedef struct FlowFunction FlowFunction;
typedef struct FlowValue FlowValue;

// Value types
typedef enum {
    FLOW_TYPE_INT,
    FLOW_TYPE_FLOAT,
    FLOW_TYPE_STRING,
    FLOW_TYPE_BOOL,
    FLOW_TYPE_ARRAY,
    FLOW_TYPE_STRUCT,
    FLOW_TYPE_NULL
} FlowValueType;

// Result codes
typedef enum {
    FLOW_OK = 0,
    FLOW_ERROR_RUNTIME = -1,
    FLOW_ERROR_COMPILE = -2,
    FLOW_ERROR_NOT_FOUND = -3,
    FLOW_ERROR_TYPE_MISMATCH = -4,
    FLOW_ERROR_INVALID_ARGS = -5
} FlowResult;





/**
 * Create a new Flow runtime instance
 * @return Pointer to runtime or NULL on error
 */
FlowRuntime *flow_runtime_new();

/**
 * Destroy a Flow runtime instance
 * @param runtime The runtime to destroy
 */
void flow_runtime_free(FlowRuntime *runtime);

/**
 * Get the last error message from the runtime
 * @param runtime The runtime
 * @return Error message string (owned by runtime, don't free)
 */
const char *flow_runtime_get_error(FlowRuntime *runtime);

// ============================================================
// MODULE MANAGEMENT
// ============================================================

/**
 * Compile and load a Flow module from source code
 * @param runtime The runtime
 * @param source Flow source code
 * @param module_name Name for the module
 * @return Module handle or NULL on error
 */
FlowModule *flow_module_compile(FlowRuntime *runtime, const char *source, const char *module_name);

/**
 * Load a Flow module from a file
 * @param runtime The runtime
 * @param file_path Path to .flow file
 * @return Module handle or NULL on error
 */
FlowModule *flow_module_load_file(FlowRuntime *runtime, const char *file_path);

/**
 * Free a module
 * @param module The module to free
 */
void flow_module_free(FlowModule *module);

// ============================================================
// FUNCTION MANAGEMENT
// ============================================================

/**
 * Get a function from a module by name
 * @param module The module
 * @param function_name Name of the function
 * @return Function handle or NULL if not found
 */
FlowFunction *flow_module_get_function(FlowModule *module, const char *function_name);

/**
 * Get the number of parameters a function takes
 * @param function The function
 * @return Parameter count or -1 on error
 */
int flow_function_get_param_count(FlowFunction *function);

// ============================================================
// VALUE MANAGEMENT
// ============================================================

/**
 * Create a new integer value
 * @param runtime The runtime
 * @param value The integer value
 * @return Value handle
 */
FlowValue *flow_value_new_int(FlowRuntime *runtime, int64_t value);

/**
 * Create a new float value
 * @param runtime The runtime
 * @param value The float value
 * @return Value handle
 */
FlowValue *flow_value_new_float(FlowRuntime *runtime, double value);

/**
 * Create a new string value
 * @param runtime The runtime
 * @param value The string (will be copied)
 * @return Value handle
 */
FlowValue *flow_value_new_string(FlowRuntime *runtime, const char *value);

/**
 * Create a new boolean value
 * @param runtime The runtime
 * @param value The boolean value
 * @return Value handle
 */
FlowValue *flow_value_new_bool(FlowRuntime *runtime, int value);

/**
 * Create a null value
 * @param runtime The runtime
 * @return Value handle
 */
FlowValue *flow_value_new_null(FlowRuntime *runtime);

/**
 * Free a value
 * @param value The value to free
 */
void flow_value_free(FlowValue *value);

/**
 * Get the type of a value
 * @param value The value
 * @return The value type
 */
FlowValueType flow_value_get_type(FlowValue *value);

/**
 * Get an integer from a value
 * @param value The value
 * @param out Output pointer for the integer
 * @return FLOW_OK or error code
 */
FlowResult flow_value_get_int(FlowValue * value, int64_t * out);

/**
 * Get a float from a value
 * @param value The value
 * @param out Output pointer for the float
 * @return FLOW_OK or error code
 */
FlowResult flow_value_get_float(FlowValue *value, double *out);

/**
 * Get a string from a value
 * @param value The value
 * @return String pointer (owned by value, don't free) or NULL on error
 */
const char *flow_value_get_string(FlowValue *value);

/**
 * Get a boolean from a value
 * @param value The value
 * @param out Output pointer for the boolean
 * @return FLOW_OK or error code
 */
FlowResult flow_value_get_bool(FlowValue *value, int *out);

// ============================================================
// FUNCTION EXECUTION
// ============================================================

/**
 * Call a Flow function
 * @param runtime The runtime
 * @param function The function to call
 * @param args Array of argument values
 * @param arg_count Number of arguments
 * @param result Output pointer for return value (caller must free)
 * @return FLOW_OK or error code
 */
FlowResult flow_function_call(FlowRuntime *runtime, FlowFunction *function,
                              FlowValue **args, int arg_count, FlowValue **result);

/**
 * Call a function by name (convenience function)
 * @param runtime The runtime
 * @param module The module
 * @param function_name Name of the function
 * @param args Array of argument values
 * @param arg_count Number of arguments
 * @param result Output pointer for return value (caller must free)
 * @return FLOW_OK or error code
 */
FlowResult flow_call(FlowRuntime *runtime, FlowModule *module, const char *function_name,
                     FlowValue **args, int arg_count, FlowValue **result);

// ============================================================
// CONVENIENCE MACROS
// ============================================================

#define FLOW_CALL_INT(runtime, module, func, result, ...) \
    do { \
        FlowValue* _args[] = {__VA_ARGS__}; \
        flow_call(runtime, module, func, _args, sizeof(_args)/sizeof(FlowValue*), result); \
    } while(0)

#ifdef __cplusplus
}
#endif

#endif // FLOW_EMBEDDING_API_H