#include "flow.h"
#include "../../flowbase/include/Embedding/FlowAPI.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>



// Global runtime instance
static FlowRuntime* g_runtime = NULL;
static char error_buffer[1024] = {0};

// Internal structures that wrap FlowAPI types
struct flow_module {
    FlowModule* handle;
};

int flow_init(void) {
    if (g_runtime) {
        return 0; // Already initialized
    }

    g_runtime = flow_runtime_new();
    if (!g_runtime) {
        snprintf(error_buffer, sizeof(error_buffer), "Failed to create Flow runtime");
        return -1;
    }

    return 0;
}

void flow_cleanup(void) {
    if (g_runtime) {
        flow_runtime_free(g_runtime);
        g_runtime = NULL;
    }
}

flow_module_t* flow_load_module(const char* path) {
    if (!g_runtime) {
        snprintf(error_buffer, sizeof(error_buffer), "Runtime not initialized. Call flow_init() first.");
        return NULL;
    }

    if (!path) {
        snprintf(error_buffer, sizeof(error_buffer), "Invalid path parameter");
        return NULL;
    }

    FlowModule* module_handle = flow_module_load_file(g_runtime, path);
    if (!module_handle) {
        const char* err = flow_runtime_get_error(g_runtime);
        snprintf(error_buffer, sizeof(error_buffer), "%s", err ? err : "Unknown error");
        return NULL;
    }

    flow_module_t* module = (flow_module_t*)malloc(sizeof(flow_module_t));
    if (!module) {
        flow_module_free(module_handle);
        snprintf(error_buffer, sizeof(error_buffer), "Memory allocation failed");
        return NULL;
    }

    module->handle = module_handle;
    return module;
}

flow_module_t* flow_compile_string(const char* source) {
    if (!g_runtime) {
        snprintf(error_buffer, sizeof(error_buffer), "Runtime not initialized. Call flow_init() first.");
        return NULL;
    }

    if (!source) {
        snprintf(error_buffer, sizeof(error_buffer), "Invalid source parameter");
        return NULL;
    }

    FlowModule* module_handle = flow_module_compile(g_runtime, source, "inline_module");
    if (!module_handle) {
        const char* err = flow_runtime_get_error(g_runtime);
        snprintf(error_buffer, sizeof(error_buffer), "%s", err ? err : "Unknown error");
        return NULL;
    }

    flow_module_t* module = (flow_module_t*)malloc(sizeof(flow_module_t));
    if (!module) {
        flow_module_free(module_handle);
        snprintf(error_buffer, sizeof(error_buffer), "Memory allocation failed");
        return NULL;
    }

    module->handle = module_handle;
    return module;
}

void flow_unload_module(flow_module_t* module) {
    if (module) {
        if (module->handle) {
            flow_module_free(module->handle);
        }
        free(module);
    }
}

flow_value_t flowc_call(flow_module_t* module, const char* function, int argc, ...) {
    va_list args;
    va_start(args, argc);

    flow_value_t* argv = (flow_value_t*)malloc(sizeof(flow_value_t) * argc);
    for (int i = 0; i < argc; i++) {
        argv[i] = va_arg(args, flow_value_t);
    }

    va_end(args);

    flow_value_t result = flowc_call_v(module, function, argc, argv);
    free(argv);

    return result;
}

flow_value_t flowc_call_v(flow_module_t* module, const char* function, int argc, flow_value_t* argv) {
    if (!g_runtime || !module || !module->handle || !function) {
        snprintf(error_buffer, sizeof(error_buffer), "Invalid parameters to flow_call_v");
        return flow_void();
    }

    // Convert flow_value_t array to FlowValue* array
    FlowValue** flow_args = (FlowValue**)malloc(sizeof(FlowValue*) * argc);
    if (!flow_args && argc > 0) {
        snprintf(error_buffer, sizeof(error_buffer), "Memory allocation failed");
        return flow_void();
    }

    for (int i = 0; i < argc; i++) {
        switch (argv[i].type) {
            case FLOW_VAL_INT:
                flow_args[i] = flow_value_new_int(g_runtime, argv[i].data.int_val);
                break;
            case FLOW_VAL_FLOAT:
                flow_args[i] = flow_value_new_float(g_runtime, argv[i].data.float_val);
                break;
            case FLOW_VAL_STRING:
                flow_args[i] = flow_value_new_string(g_runtime, argv[i].data.string_val);
                break;
            case FLOW_VAL_BOOL:
                flow_args[i] = flow_value_new_bool(g_runtime, argv[i].data.bool_val);
                break;
            case FLOW_VAL_VOID:
                flow_args[i] = flow_value_new_null(g_runtime);
                break;
            default:
                flow_args[i] = flow_value_new_null(g_runtime);
                break;
        }
    }

    // Call the function using FlowAPI
    FlowValue* result_handle = NULL;

    FlowFunction* func = flow_module_get_function(module->handle, function);
    if (!func) {
        const char* err = flow_runtime_get_error(g_runtime);
        snprintf(error_buffer, sizeof(error_buffer), "%s", err ? err : "Function not found");
        for (int i = 0; i < argc; i++) {
            if (flow_args[i]) flow_value_free(flow_args[i]);
        }
        free(flow_args);
        return flow_void();
    }

    FlowResult res = flow_function_call(g_runtime, func, flow_args, argc, &result_handle);

    // Free argument wrappers
    for (int i = 0; i < argc; i++) {
        if (flow_args[i]) {
            flow_value_free(flow_args[i]);
        }
    }
    free(flow_args);

    // Convert result back
    flow_value_t result;

    if (res != FLOW_OK || !result_handle) {
        const char* err = flow_runtime_get_error(g_runtime);
        snprintf(error_buffer, sizeof(error_buffer), "%s", err ? err : "Unknown error");
        result = flow_void();
    } else {
        FlowValueType type = flow_value_get_type(result_handle);

        switch (type) {
            case FLOW_TYPE_INT: {
                int64_t val;
                if (flow_value_get_int(result_handle, &val) == FLOW_OK) {
                    result = flow_int(val);
                } else {
                    result = flow_void();
                }
                break;
            }
            case FLOW_TYPE_FLOAT: {
                double val;
                if (flow_value_get_float(result_handle, &val) == FLOW_OK) {
                    result = flow_float(val);
                } else {
                    result = flow_void();
                }
                break;
            }
            case FLOW_TYPE_STRING: {
                const char* val = flow_value_get_string(result_handle);
                if (val) {
                    // Note: We need to copy the string as the original may be freed
                    char* str_copy = strdup(val);
                    result = flow_string(str_copy);
                } else {
                    result = flow_void();
                }
                break;
            }
            case FLOW_TYPE_BOOL: {
                int val;
                if (flow_value_get_bool(result_handle, &val) == FLOW_OK) {
                    result = flow_bool(val != 0);
                } else {
                    result = flow_void();
                }
                break;
            }
            default:
                result = flow_void();
                break;
        }

        flow_value_free(result_handle);
    }

    return result;
}

flow_value_t flow_int(int64_t value) {
    flow_value_t v;
    v.type = FLOW_VAL_INT;
    v.data.int_val = value;
    return v;
}

flow_value_t flow_float(double value) {
    flow_value_t v;
    v.type = FLOW_VAL_FLOAT;
    v.data.float_val = value;
    return v;
}

flow_value_t flow_string(const char* value) {
    flow_value_t v;
    v.type = FLOW_VAL_STRING;
    v.data.string_val = value;
    return v;
}

flow_value_t flow_bool(bool value) {
    flow_value_t v;
    v.type = FLOW_VAL_BOOL;
    v.data.bool_val = value;
    return v;
}

flow_value_t flow_void(void) {
    flow_value_t v;
    v.type = FLOW_VAL_VOID;
    return v;
}

int64_t flow_as_int(flow_value_t value) {
    return value.data.int_val;
}

double flow_as_float(flow_value_t value) {
    return value.data.float_val;
}

const char* flow_as_string(flow_value_t value) {
    return value.data.string_val;
}

bool flow_as_bool(flow_value_t value) {
    return value.data.bool_val;
}

const char* flow_get_error(void) {
    return error_buffer;
}

void flow_clear_error(void) {
    error_buffer[0] = '\0';
}

void* flow_module_get_handle(flow_module_t* module) {
    return module ? module->handle : NULL;
}

