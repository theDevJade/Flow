#ifndef FLOW_C_API_H
#define FLOW_C_API_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif





typedef enum {
    FLOW_VAL_INT,
    FLOW_VAL_FLOAT,
    FLOW_VAL_STRING,
    FLOW_VAL_BOOL,
    FLOW_VAL_VOID
} flow_type_t;


typedef struct {
    flow_type_t type;
    union {
        int64_t int_val;
        double float_val;
        const char* string_val;
        bool bool_val;
    } data;
} flow_value_t;


typedef struct flow_module flow_module_t;


int flow_init(void);
void flow_cleanup(void);


flow_module_t* flow_load_module(const char* path);
flow_module_t* flow_compile_string(const char* source);
void flow_unload_module(flow_module_t* module);


flow_value_t flowc_call(flow_module_t* module, const char* function, int argc, ...);
flow_value_t flowc_call_v(flow_module_t* module, const char* function, int argc, flow_value_t* argv);


flow_value_t flow_int(int64_t value);
flow_value_t flow_float(double value);
flow_value_t flow_string(const char* value);
flow_value_t flow_bool(bool value);
flow_value_t flow_void(void);


int64_t flow_as_int(flow_value_t value);
double flow_as_float(flow_value_t value);
const char* flow_as_string(flow_value_t value);
bool flow_as_bool(flow_value_t value);


const char* flow_get_error(void);
void flow_clear_error(void);


void* flow_module_get_handle(flow_module_t* module);

#ifdef __cplusplus
}
#endif

#endif // FLOW_C_API_H

