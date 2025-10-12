#ifndef FLOW_INTEROP_H
#define FLOW_INTEROP_H

#include "IPC.h"
#include <functional>
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>

namespace flow {

// Callback function type - Flow functions that can be called from foreign code
using FlowCallback = std::function<IPCValue(const std::vector<IPCValue>&)>;

// Enhanced adapter with full bidirectional support
class EnhancedLanguageAdapter : public LanguageAdapter {
protected:
    std::map<std::string, FlowCallback> registeredCallbacks;
    std::mutex callbackMutex;
    
public:
    virtual ~EnhancedLanguageAdapter() = default;
    
    // Register a Flow function that can be called from foreign code
    virtual void registerCallback(const std::string& name, FlowCallback callback);
    
    // Get registered callback
    virtual FlowCallback getCallback(const std::string& name);
    
    // Check if callback exists
    virtual bool hasCallback(const std::string& name);
};


struct FunctionSignature {
    std::vector<IPCValue::Type> argTypes;
    IPCValue::Type returnType;
    
    FunctionSignature() : returnType(IPCValue::Type::INT) {}
    FunctionSignature(const std::vector<IPCValue::Type>& args, IPCValue::Type ret)
        : argTypes(args), returnType(ret) {}
};

class EnhancedCAdapter : public EnhancedLanguageAdapter {
private:
    void* libHandle;
    std::string moduleName;
    std::map<std::string, void*> functionPointers;
    std::map<std::string, FunctionSignature> functionSignatures;
    
    // Type marshalling helpers
    void* marshalArguments(const std::vector<IPCValue>& args, std::vector<void*>& marshalled);
    IPCValue unmarshalReturn(void* result, IPCValue::Type expectedType);
    
    // Type inference from function name patterns
    IPCValue::Type inferReturnType(const std::string& function, const std::vector<IPCValue>& args);
    
public:
    EnhancedCAdapter() : libHandle(nullptr) {}
    ~EnhancedCAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "c"; }
    
    // Enhanced features
    void exportFunction(const std::string& name, void* funcPtr);
    void* getFunctionPointer(const std::string& name);
    
    // Register function signature for accurate type handling
    void registerFunctionSignature(const std::string& name, 
                                   const std::vector<IPCValue::Type>& argTypes,
                                   IPCValue::Type returnType);
};


class EnhancedPythonAdapter : public EnhancedLanguageAdapter {
private:
    void* pythonState;  // Python interpreter state
    std::string moduleName;
    std::map<std::string, void*> pythonFunctions;
    bool isEmbedded;
    
    // IPC for subprocess mode
    int pipeFd[2];
    pid_t childPid;
    std::thread messageThread;
    std::queue<IPCMessage> messageQueue;
    std::mutex queueMutex;
    std::condition_variable queueCV;
    
    void initializeEmbedded();
    void initializeSubprocess();
    void messageLoop();
    IPCValue callEmbedded(const std::string& function, const std::vector<IPCValue>& args);
    IPCValue callSubprocess(const std::string& function, const std::vector<IPCValue>& args);
    
public:
    EnhancedPythonAdapter(bool embed = true);
    ~EnhancedPythonAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "python"; }
    
    // Execute inline Python code
    IPCValue executeCode(const std::string& code);
    
    // Export Flow functions to Python
    void exportToPython(const std::string& name, FlowCallback callback);
};


class EnhancedJavaScriptAdapter : public EnhancedLanguageAdapter {
private:
    void* isolate;  // V8 isolate (if using V8)
    void* context;  // V8 context
    std::string moduleName;
    bool useV8;  // If false, use Node.js subprocess
    
    // IPC for subprocess mode
    int pipeFd[2];
    pid_t childPid;
    std::thread messageThread;
    std::queue<IPCMessage> messageQueue;
    std::mutex queueMutex;
    std::condition_variable queueCV;
    
    void initializeV8();
    void initializeNodeJS();
    void messageLoop();
    IPCValue callV8(const std::string& function, const std::vector<IPCValue>& args);
    IPCValue callNodeJS(const std::string& function, const std::vector<IPCValue>& args);
    
public:
    EnhancedJavaScriptAdapter(bool useV8Engine = false);
    ~EnhancedJavaScriptAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "javascript"; }
    
    // Execute inline JavaScript code
    IPCValue executeCode(const std::string& code);
    
    // Export Flow functions to JavaScript
    void exportToJavaScript(const std::string& name, FlowCallback callback);
};

// Rust Adapter (bonus!)
class RustAdapter : public EnhancedLanguageAdapter {
private:
    void* libHandle;
    std::string moduleName;
    std::map<std::string, void*> functionPointers;
    
public:
    RustAdapter() : libHandle(nullptr) {}
    ~RustAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "rust"; }
};

// Go Adapter (bonus!)
class GoAdapter : public EnhancedLanguageAdapter {
private:
    void* libHandle;
    std::string moduleName;
    
public:
    GoAdapter() : libHandle(nullptr) {}
    ~GoAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "go"; }
};

// Enhanced IPC Runtime with full interop
class EnhancedIPCRuntime {
private:
    std::map<std::string, std::unique_ptr<EnhancedLanguageAdapter>> adapters;
    std::map<std::string, FlowCallback> exportedFunctions;
    std::mutex adapterMutex;
    int nextCallId;
    
public:
    EnhancedIPCRuntime() : nextCallId(0) {}
    ~EnhancedIPCRuntime() { shutdownAll(); }
    
    // Get or create adapter
    EnhancedLanguageAdapter* getAdapter(const std::string& adapterType, const std::string& module);
    
    // Call foreign function
    IPCValue callForeign(const std::string& adapter, const std::string& module,
                         const std::string& function, const std::vector<IPCValue>& args);
    
    // Export Flow function to all adapters
    void exportFunction(const std::string& name, FlowCallback callback);
    
    // Get exported function
    FlowCallback getExportedFunction(const std::string& name);
    
    // Execute inline code in any language
    IPCValue executeInlineCode(const std::string& adapter, const std::string& code);
    
    void shutdownAll();
    
    static EnhancedIPCRuntime& instance() {
        static EnhancedIPCRuntime runtime;
        return runtime;
    }
};

// Helper functions for creating IPCValues from C++ types
template<typename T>
IPCValue toIPCValue(const T& value);

template<>
inline IPCValue toIPCValue<int>(const int& value) {
    return IPCValue::makeInt(value);
}

template<>
inline IPCValue toIPCValue<double>(const double& value) {
    return IPCValue::makeFloat(value);
}

template<>
inline IPCValue toIPCValue<std::string>(const std::string& value) {
    return IPCValue::makeString(value);
}

template<>
inline IPCValue toIPCValue<bool>(const bool& value) {
    return IPCValue::makeBool(value);
}

// Helper functions for extracting values from IPCValue
template<typename T>
T fromIPCValue(const IPCValue& value);

template<>
inline int fromIPCValue<int>(const IPCValue& value) {
    return static_cast<int>(value.intValue);
}

template<>
inline double fromIPCValue<double>(const IPCValue& value) {
    return value.floatValue;
}

template<>
inline std::string fromIPCValue<std::string>(const IPCValue& value) {
    return value.stringValue;
}

template<>
inline bool fromIPCValue<bool>(const IPCValue& value) {
    return value.boolValue;
}

} // namespace flow

#endif // FLOW_INTEROP_H

