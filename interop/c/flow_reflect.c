

#include "flow_reflect.h"
#include "../../flowbase/include/Embedding/FlowAPI.h"
#include <stdlib.h>
#include <string.h>


extern FlowRuntime* g_runtime;





int flow_reflect_function_count(flow_module_t* module)
{
    FlowModule* handle = (FlowModule*)flow_module_get_handle(module);
    if (!handle) {
        return -1;
    }

    return flow_module_get_function_count(handle);
}

int flow_reflect_list_functions(flow_module_t* module, char*** names_out)
{
    FlowModule* handle = (FlowModule*)flow_module_get_handle(module);
    if (!handle || !names_out) {
        return -1;
    }

    int count = flow_module_get_function_count(handle);
    if (count <= 0) {
        return count;
    }


    char** names = (char**)malloc(sizeof(char*) * count);
    if (!names) {
        return -1;
    }


    for (int i = 0; i < count; i++) {
        const char* name = flow_module_get_function_name(handle, i);
        if (name) {
            names[i] = strdup(name);
        } else {
            names[i] = NULL;
        }
    }

    *names_out = names;
    return count;
}

void flow_reflect_free_names(char** names, int count)
{
    if (!names) return;

    for (int i = 0; i < count; i++) {
        free(names[i]);
    }
    free(names);
}





flow_function_info_t* flow_reflect_get_function_info(flow_module_t* module, const char* function_name)
{
    FlowModule* handle = (FlowModule*)flow_module_get_handle(module);
    if (!handle || !function_name) {
        return NULL;
    }


    FlowFunction* func = flow_module_get_function(handle, function_name);
    if (!func) {
        return NULL;
    }

    // Allocate info structure
    flow_function_info_t* info = (flow_function_info_t*)malloc(sizeof(flow_function_info_t));
    if (!info) {
        return NULL;
    }

    // Get function name
    const char* func_name = flow_function_get_name(func);
    info->name = func_name ? strdup(func_name) : NULL;

    // Get return type
    const char* ret_type = flow_function_get_return_type(func);
    info->return_type = ret_type ? strdup(ret_type) : strdup("void");

    // Get parameter count
    int param_count = flow_function_get_param_count(func);
    info->param_count = param_count;

    // Allocate parameter array
    if (param_count > 0) {
        info->params = (flow_param_info_t*)malloc(sizeof(flow_param_info_t) * param_count);
        if (!info->params) {
            free((void*)info->name);
            free((void*)info->return_type);
            free(info);
            return NULL;
        }

        // Get each parameter
        for (int i = 0; i < param_count; i++) {
            const char* param_name = flow_function_get_param_name(func, i);
            const char* param_type = flow_function_get_param_type(func, i);

            info->params[i].name = param_name ? strdup(param_name) : strdup("unknown");
            info->params[i].type = param_type ? strdup(param_type) : strdup("unknown");
        }
    } else {
        info->params = NULL;
    }

    return info;
}

void flow_reflect_free_function_info(flow_function_info_t* info)
{
    if (!info) return;

    free((void*)info->name);
    free((void*)info->return_type);

    if (info->params) {
        for (int i = 0; i < info->param_count; i++) {
            free((void*)info->params[i].name);
            free((void*)info->params[i].type);
        }
        free(info->params);
    }

    free(info);
}

const char* flow_reflect_function_name_at(flow_module_t* module, int index)
{
    FlowModule* handle = (FlowModule*)flow_module_get_handle(module);
    if (!handle || index < 0) {
        return NULL;
    }

    return flow_module_get_function_name(handle, index);
}

// ============================================================
// BIDIRECTIONAL REFLECTION (Flow -> Other Languages)
// ============================================================

// Simple registry for foreign modules
typedef struct {
    char* adapter;
    char* module_name;
    char** function_names;
    int function_count;
} foreign_module_t;

static foreign_module_t* g_foreign_modules = NULL;
static int g_foreign_module_count = 0;
static int g_foreign_module_capacity = 0;

int flow_reflect_register_foreign_module(
    const char* adapter,
    const char* module_name,
    const char** function_names,
    int function_count)
{
    if (!adapter || !module_name || !function_names || function_count <= 0) {
        return -1;
    }

    // Check capacity
    if (g_foreign_module_count >= g_foreign_module_capacity) {
        int new_capacity = g_foreign_module_capacity == 0 ? 10 : g_foreign_module_capacity * 2;
        foreign_module_t* new_modules = (foreign_module_t*)realloc(
            g_foreign_modules,
            sizeof(foreign_module_t) * new_capacity
        );
        if (!new_modules) {
            return -1;
        }
        g_foreign_modules = new_modules;
        g_foreign_module_capacity = new_capacity;
    }

    // Add module
    foreign_module_t* mod = &g_foreign_modules[g_foreign_module_count];
    mod->adapter = strdup(adapter);
    mod->module_name = strdup(module_name);
    mod->function_count = function_count;

    // Copy function names
    mod->function_names = (char**)malloc(sizeof(char*) * function_count);
    for (int i = 0; i < function_count; i++) {
        mod->function_names[i] = strdup(function_names[i]);
    }

    g_foreign_module_count++;
    return 0;
}

int flow_reflect_foreign_function_count(const char* adapter, const char* module_name)
{
    if (!adapter || !module_name) {
        return 0;
    }

    // Find the module
    for (int i = 0; i < g_foreign_module_count; i++) {
        if (strcmp(g_foreign_modules[i].adapter, adapter) == 0 &&
            strcmp(g_foreign_modules[i].module_name, module_name) == 0) {
            return g_foreign_modules[i].function_count;
        }
    }

    return 0;  // Not found
}

int flow_reflect_foreign_functions(
    const char* adapter,
    const char* module_name,
    char*** names_out)
{
    if (!adapter || !module_name || !names_out) {
        return -1;
    }

    // Find module
    for (int i = 0; i < g_foreign_module_count; i++) {
        foreign_module_t* mod = &g_foreign_modules[i];
        if (strcmp(mod->adapter, adapter) == 0 && strcmp(mod->module_name, module_name) == 0) {
            // Allocate and copy names
            char** names = (char**)malloc(sizeof(char*) * mod->function_count);
            for (int j = 0; j < mod->function_count; j++) {
                names[j] = strdup(mod->function_names[j]);
            }
            *names_out = names;
            return mod->function_count;
        }
    }

    return -1;  // Not found
}

int flow_reflect_has_foreign_module(
    const char* adapter,
    const char* module_name)
{
    if (!adapter || !module_name) {
        return -1;
    }

    for (int i = 0; i < g_foreign_module_count; i++) {
        foreign_module_t* mod = &g_foreign_modules[i];
        if (strcmp(mod->adapter, adapter) == 0 && strcmp(mod->module_name, module_name) == 0) {
            return 1;
        }
    }

    return 0;
}

