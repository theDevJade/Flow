

#ifndef FLOW_REFLECT_H
#define FLOW_REFLECT_H

#include "flow.h"

#ifdef __cplusplus
extern "C" {
#endif






typedef struct {
    const char* name;
    const char* type;
} flow_param_info_t;


typedef struct {
    const char* name;
    const char* return_type;
    int param_count;
    flow_param_info_t* params;
} flow_function_info_t;





/**
 * Get the number of functions in a module
 * @param module The module to inspect
 * @return Number of functions, or -1 on error
 */
int flow_reflect_function_count(flow_module_t* module);

/**
 * List all function names in a module
 * @param module The module to inspect
 * @param names_out Output array of function names (caller must free with flow_reflect_free_names)
 * @return Number of functions, or -1 on error
 */
int flow_reflect_list_functions(flow_module_t* module, char*** names_out);

/**
 * Free the array of function names returned by flow_reflect_list_functions
 * @param names The array to free
 * @param count Number of names in the array
 */
void flow_reflect_free_names(char** names, int count);





/**
 * Get detailed information about a function
 * @param module The module containing the function
 * @param function_name The name of the function
 * @return Function info (caller must free with flow_reflect_free_function_info), or NULL on error
 */
flow_function_info_t* flow_reflect_get_function_info(flow_module_t* module, const char* function_name);

/**
 * Free function info returned by flow_reflect_get_function_info
 * @param info The info to free
 */
void flow_reflect_free_function_info(flow_function_info_t* info);

/**
 * Get the name of a function by index
 * @param module The module
 * @param index The index (0-based)
 * @return Function name (owned by module, don't free), or NULL if out of bounds
 */
const char* flow_reflect_function_name_at(flow_module_t* module, int index);

// ============================================================
// BIDIRECTIONAL REFLECTION (Flow -> Other Languages)
// ============================================================

/**
 * Register a foreign language module for reflection
 * This allows Flow code to introspect and call functions from other languages.
 *
 * @param adapter The language adapter ("python", "go", "javascript", etc.)
 * @param module_name The module name
 * @param function_names Array of function names available in the module
 * @param function_count Number of functions
 * @return 0 on success, -1 on error
 */
int flow_reflect_register_foreign_module(
    const char* adapter,
    const char* module_name,
    const char** function_names,
    int function_count
);

/**
 * List available functions from a foreign module
 * @param adapter The language adapter
 * @param module_name The module name
 * @param names_out Output array of function names (caller must free)
 * @return Number of functions, or -1 if module not registered
 */
int flow_reflect_foreign_functions(
    const char* adapter,
    const char* module_name,
    char*** names_out
);

/**
 * Check if a foreign module is available
 * @param adapter The language adapter
 * @param module_name The module name
 * @return 1 if available, 0 if not, -1 on error
 */
int flow_reflect_has_foreign_module(
    const char* adapter,
    const char* module_name
);

#ifdef __cplusplus
}
#endif

#endif // FLOW_REFLECT_H

