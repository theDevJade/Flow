#ifndef FLOW_FFI_BRIDGE_H
#define FLOW_FFI_BRIDGE_H

#include <cstdint>


extern "C" {
    // Integer functions
    int64_t flow_ffi_call_int(const char* adapter, const char* module,
                               const char* function, int64_t* args, int arg_count);
    
    // Float functions
    double flow_ffi_call_float(const char* adapter, const char* module,
                                const char* function, double* args, int arg_count);
    
    // String functions (returns malloc'd string, caller must free)
    char* flow_ffi_call_string(const char* adapter, const char* module,
                                const char* function, const char** args, int arg_count);
    
    // Void functions
    void flow_ffi_call_void(const char* adapter, const char* module,
                            const char* function, void** args, int arg_count);
    
    // Generic call (mixed arg types)
    void* flow_ffi_call_generic(const char* adapter, const char* module,
                                 const char* function, void** args,
                                 const char* arg_types, const char* ret_type);
}

#endif // FLOW_FFI_BRIDGE_H

