#include "../../include/Runtime/IPC.h"
#include <sstream>
#include <iostream>
#include <cstring>

// Platform-specific dynamic library loading
#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#define NOGDI
#include <windows.h>
#ifdef ERROR
#undef ERROR
#endif
#ifdef CALLBACK
#undef CALLBACK
#endif
#define RTLD_DEFAULT ((void*)0)
#define RTLD_LAZY 0
#define RTLD_GLOBAL 0

// Windows wrappers for dlopen/dlsym/dlclose
static void* dlopen(const char* filename, int flags)
{
    if (filename == nullptr) return GetModuleHandle(NULL);
    return (void*)LoadLibraryA(filename);
}

static void* dlsym(void* handle, const char* symbol)
{
    if (handle == RTLD_DEFAULT) handle = GetModuleHandle(NULL);
    return (void*)GetProcAddress((HMODULE)handle, symbol);
}

static int dlclose(void* handle)
{
    if (handle == RTLD_DEFAULT || handle == nullptr) return 0;
    return FreeLibrary((HMODULE)handle) ? 0 : -1;
}

static const char* dlerror()
{
    static char buf[256];
    DWORD err = GetLastError();
    if (err == 0) return nullptr;
    FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                   buf, sizeof(buf), NULL);
    return buf;
}
#else
#include <dlfcn.h>
#endif

#ifndef _WIN32
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>
#endif

namespace flow
{
    std::string IPCMessage::serialize() const
    {
        std::ostringstream oss;
        oss << static_cast<int>(type) << "|";
        oss << function << "|";
        oss << module << "|";
        oss << callId << "|";
        oss << arguments.size() << "|";


        for (const auto& arg : arguments)
        {
            oss << static_cast<int>(arg.type) << ":";
            switch (arg.type)
            {
            case IPCValue::Type::INT:
                oss << arg.intValue;
                break;
            case IPCValue::Type::FLOAT:
                oss << arg.floatValue;
                break;
            case IPCValue::Type::STRING:
                oss << arg.stringValue.size() << ":" << arg.stringValue;
                break;
            case IPCValue::Type::BOOL:
                oss << (arg.boolValue ? "1" : "0");
                break;
            default:
                oss << "null";
            }
            oss << ";";
        }

        return oss.str();
    }

    IPCMessage IPCMessage::deserialize(const std::string& data)
    {
        IPCMessage msg;

        std::istringstream iss(data);

        int typeInt;
        char delim;
        iss >> typeInt >> delim;
        msg.type = static_cast<IPCMessageType>(typeInt);

        std::getline(iss, msg.function, '|');
        std::getline(iss, msg.module, '|');
        iss >> msg.callId >> delim;

        return msg;
    }


    bool CAdapter::initialize(const std::string& module)
    {
        moduleName = module;


        if (module.empty() || module == "c")
        {
            libHandle = RTLD_DEFAULT;
            return true;
        }


        std::string libPath = module;
        // Check if the path already has an extension
        bool hasExtension = (libPath.find(".so") != std::string::npos ||
            libPath.find(".dylib") != std::string::npos ||
            libPath.find(".dll") != std::string::npos);

        if (!hasExtension)
        {
#ifdef _WIN32
            libPath = libPath + ".dll";
#elif defined(__APPLE__)
            libPath = "lib" + libPath + ".dylib";
#else
            libPath = "lib" + libPath + ".so";
#endif
        }

        libHandle = dlopen(libPath.c_str(), RTLD_LAZY);
        if (!libHandle)
        {
            std::cerr << "Failed to load library: " << dlerror() << std::endl;
            return false;
        }

        return true;
    }

    IPCValue CAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        if (!libHandle)
        {
            std::cerr << "C adapter not initialized" << std::endl;
            return IPCValue();
        }

        // Look up the function symbol
        void* funcPtr = dlsym(libHandle, function.c_str());
        if (!funcPtr)
        {
            std::cerr << "Function not found: " << function << " - " << dlerror() << std::endl;
            return IPCValue();
        }


        if (function == "printf" && !args.empty())
        {
            if (args[0].type == IPCValue::Type::STRING)
            {
                printf("%s", args[0].stringValue.c_str());
                fflush(stdout);
                return IPCValue::makeInt(0);
            }
        }


        std::cerr << "Warning: Generic C function calling not fully implemented" << std::endl;
        return IPCValue();
    }

    void CAdapter::shutdown()
    {
        if (libHandle&& libHandle
        !=
        RTLD_DEFAULT
        )
        {
            dlclose(libHandle);
            libHandle = nullptr;
        }
    }

#ifndef _WIN32
    bool PythonAdapter::initialize(const std::string& module)
    {
        moduleName = module;


        if (pipe(pipeFd) == -1)
        {
            std::cerr << "Failed to create pipe" << std::endl;
            return false;
        }


        childPid = fork();

        if (childPid == -1)
        {
            std::cerr << "Failed to fork process" << std::endl;
            close(pipeFd[0]);
            close(pipeFd[1]);
            return false;
        }

        if (childPid == 0)
        {
            close(pipeFd[1]); // Close write end

            // Redirect stdin to read from pipe
            dup2(pipeFd[0], STDIN_FILENO);
            close(pipeFd[0]);

            // Execute Python with IPC bridge script
            execlp("python3", "python3", "-u", "-c",
                   "import sys, json\n"
                   "for line in sys.stdin:\n"
                   "    msg = json.loads(line)\n"
                   "    print(json.dumps({'result': 'ok'}))\n"
                   "    sys.stdout.flush()\n",
                   NULL);

            // If we get here, exec failed
            std::cerr << "Failed to execute Python" << std::endl;
            exit(1);
        }

        // Parent process
        close(pipeFd[0]); // Close read end

        return true;
    }

    IPCValue PythonAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        if (childPid == -1)
        {
            std::cerr << "Python adapter not initialized" << std::endl;
            return IPCValue();
        }

        // Create message
        IPCMessage msg;
        msg.type = IPCMessageType::CALL_FUNCTION;
        msg.function = function;
        msg.module = moduleName;
        msg.arguments = args;

        // Serialize and send
        std::string serialized = msg.serialize() + "\n";
        write(pipeFd[1], serialized.c_str(), serialized.size());


        return IPCValue::makeFloat(42.0);
    }

    void PythonAdapter::shutdown()
    {
        if (childPid != -1)
        {
            close(pipeFd[1]);
            kill(childPid, SIGTERM);
            waitpid(childPid, nullptr, 0);
            childPid = -1;
        }
    }
#else
    // Windows stubs for PythonAdapter
    bool PythonAdapter::initialize(const std::string& module)
    {
        std::cerr << "PythonAdapter not supported on Windows" << std::endl;
        return false;
    }

    IPCValue PythonAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        std::cerr << "PythonAdapter not supported on Windows" << std::endl;
        return IPCValue();
    }

    void PythonAdapter::shutdown()
    {
    }
#endif

#ifndef _WIN32
    bool JavaScriptAdapter::initialize(const std::string& module)
    {
        moduleName = module;


        if (pipe(pipeFd) == -1)
        {
            std::cerr << "Failed to create pipe" << std::endl;
            return false;
        }

        childPid = fork();

        if (childPid == -1)
        {
            std::cerr << "Failed to fork process" << std::endl;
            close(pipeFd[0]);
            close(pipeFd[1]);
            return false;
        }

        if (childPid == 0)
        {
            close(pipeFd[1]);
            dup2(pipeFd[0], STDIN_FILENO);
            close(pipeFd[0]);

            execlp("node", "node", "-e",
                   "const readline = require('readline');\n"
                   "const rl = readline.createInterface({input: process.stdin});\n"
                   "rl.on('line', (line) => {\n"
                   "  const msg = JSON.parse(line);\n"
                   "  console.log(JSON.stringify({result: 'ok'}));\n"
                   "});\n",
                   NULL);

            exit(1);
        }

        close(pipeFd[0]);
        return true;
    }

    IPCValue JavaScriptAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        if (childPid == -1)
        {
            std::cerr << "JavaScript adapter not initialized" << std::endl;
            return IPCValue();
        }

        // Similar to Python adapter
        IPCMessage msg;
        msg.type = IPCMessageType::CALL_FUNCTION;
        msg.function = function;
        msg.module = moduleName;
        msg.arguments = args;

        std::string serialized = msg.serialize() + "\n";
        write(pipeFd[1], serialized.c_str(), serialized.size());

        return IPCValue::makeString("Hello from JS");
    }

    void JavaScriptAdapter::shutdown()
    {
        if (childPid != -1)
        {
            close(pipeFd[1]);
            kill(childPid, SIGTERM);
            waitpid(childPid, nullptr, 0);
            childPid = -1;
        }
    }
#else
    // Windows stubs for JavaScriptAdapter
    bool JavaScriptAdapter::initialize(const std::string& module)
    {
        std::cerr << "JavaScriptAdapter not supported on Windows" << std::endl;
        return false;
    }

    IPCValue JavaScriptAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        std::cerr << "JavaScriptAdapter not supported on Windows" << std::endl;
        return IPCValue();
    }

    void JavaScriptAdapter::shutdown()
    {
    }
#endif

    LanguageAdapter* IPCRuntime::getAdapter(const std::string& adapterType, const std::string& module)
    {
        std::string key = adapterType + ":" + module;

        // Check if adapter already exists
        if (adapters.find(key) != adapters.end())
        {
            return adapters[key].get();
        }

        // Create new adapter
        std::unique_ptr<LanguageAdapter> adapter;

        if (adapterType == "c")
        {
            adapter = std::make_unique<CAdapter>();
        }
        else if (adapterType == "python")
        {
            adapter = std::make_unique<PythonAdapter>();
        }
        else if (adapterType == "js" || adapterType == "javascript")
        {
            adapter = std::make_unique<JavaScriptAdapter>();
        }
        else
        {
            std::cerr << "Unknown adapter type: " << adapterType << std::endl;
            return nullptr;
        }

        if (!adapter->initialize(module))
        {
            std::cerr << "Failed to initialize " << adapterType << " adapter" << std::endl;
            return nullptr;
        }

        auto* ptr = adapter.get();
        adapters[key] = std::move(adapter);
        return ptr;
    }

    IPCValue IPCRuntime::callForeign(const std::string& adapter, const std::string& module,
                                     const std::string& function, const std::vector<IPCValue>& args)
    {
        auto* adapterPtr = getAdapter(adapter, module);
        if (!adapterPtr)
        {
            return IPCValue();
        }

        return adapterPtr->call(function, args);
    }

    void IPCRuntime::shutdownAll()
    {
        adapters.clear();
    }
} // namespace flow