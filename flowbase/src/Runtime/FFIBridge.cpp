#include "../../include/Runtime/FFIBridge.h"
#include "../../include/Runtime/Interop.h"
#include <iostream>
#include <vector>
#include <cstring>

using namespace flow;

extern "C" {
int64_t flow_ffi_call_int(const char* adapter, const char* module,
                          const char* function, int64_t* args, int arg_count)
{
    try
    {
        std::vector<IPCValue> ipcArgs;
        for (int i = 0; i < arg_count; i++)
        {
            ipcArgs.push_back(IPCValue::makeInt(args[i]));
        }

        IPCValue result = EnhancedIPCRuntime::instance().callForeign(
            adapter, module, function, ipcArgs);

        if (result.type == IPCValue::Type::INT)
        {
            return result.intValue;
        }
        else if (result.type == IPCValue::Type::FLOAT)
        {
            return static_cast<int64_t>(result.floatValue);
        }

        std::cerr << "FFI call returned non-integer type" << std::endl;
        return 0;
    }
    catch (const std::exception& e)
    {
        std::cerr << "FFI call error: " << e.what() << std::endl;
        return 0;
    }
}

double flow_ffi_call_float(const char* adapter, const char* module,
                           const char* function, double* args, int arg_count)
{
    try
    {
        std::vector<IPCValue> ipcArgs;
        for (int i = 0; i < arg_count; i++)
        {
            ipcArgs.push_back(IPCValue::makeFloat(args[i]));
        }

        IPCValue result = EnhancedIPCRuntime::instance().callForeign(
            adapter, module, function, ipcArgs);

        if (result.type == IPCValue::Type::FLOAT)
        {
            return result.floatValue;
        }
        else if (result.type == IPCValue::Type::INT)
        {
            return static_cast<double>(result.intValue);
        }

        std::cerr << "FFI call returned non-float type" << std::endl;
        return 0.0;
    }
    catch (const std::exception& e)
    {
        std::cerr << "FFI call error: " << e.what() << std::endl;
        return 0.0;
    }
}

char* flow_ffi_call_string(const char* adapter, const char* module,
                           const char* function, const char** args, int arg_count)
{
    try
    {
        std::vector<IPCValue> ipcArgs;
        for (int i = 0; i < arg_count; i++)
        {
            ipcArgs.push_back(IPCValue::makeString(args[i]));
        }

        IPCValue result = EnhancedIPCRuntime::instance().callForeign(
            adapter, module, function, ipcArgs);

        if (result.type == IPCValue::Type::STRING)
        {
            return strdup(result.stringValue.c_str());
        }

        std::cerr << "FFI call returned non-string type" << std::endl;
        return strdup("");
    }
    catch (const std::exception& e)
    {
        std::cerr << "FFI call error: " << e.what() << std::endl;
        return strdup("");
    }
}

void flow_ffi_call_void(const char* adapter, const char* module,
                        const char* function, void** args, int arg_count)
{
    try
    {
        std::vector<IPCValue> ipcArgs;
        // Assume void* args are actually pointers to primitive types for now

        (void)args; // Unused for now
        (void)arg_count;

        EnhancedIPCRuntime::instance().callForeign(adapter, module, function, ipcArgs);
    }
    catch (const std::exception& e)
    {
        std::cerr << "FFI call error: " << e.what() << std::endl;
    }
}

void* flow_ffi_call_generic(const char* adapter, const char* module,
                            const char* function, void** args,
                            const char* arg_types, const char* ret_type)
{
    try
    {
        std::vector<IPCValue> ipcArgs;

        // Parse arg_types string (e.g., "iif" = int, int, float)
        int arg_idx = 0;
        for (const char* p = arg_types; *p != '\0'; p++, arg_idx++)
        {
            switch (*p)
            {
            case 'i': // int
                ipcArgs.push_back(IPCValue::makeInt(*static_cast<int64_t*>(args[arg_idx])));
                break;
            case 'f': // float
                ipcArgs.push_back(IPCValue::makeFloat(*static_cast<double*>(args[arg_idx])));
                break;
            case 's': // string
                ipcArgs.push_back(IPCValue::makeString(static_cast<const char*>(args[arg_idx])));
                break;
            default:
                std::cerr << "Unknown arg type: " << *p << std::endl;
                break;
            }
        }

        IPCValue result = EnhancedIPCRuntime::instance().callForeign(
            adapter, module, function, ipcArgs);

        // Convert result based on ret_type
        switch (ret_type[0])
        {
        case 'i':
            {
                int64_t* ret = new int64_t(result.intValue);
                return ret;
            }
        case 'f':
            {
                double* ret = new double(result.floatValue);
                return ret;
            }
        case 's':
            {
                return strdup(result.stringValue.c_str());
            }
        case 'v': // void
            return nullptr;
        default:
            std::cerr << "Unknown return type: " << ret_type << std::endl;
            return nullptr;
        }
    }
    catch (const std::exception& e)
    {
        std::cerr << "FFI call error: " << e.what() << std::endl;
        return nullptr;
    }
}
} // extern "C"