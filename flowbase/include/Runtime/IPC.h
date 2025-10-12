#ifndef FLOW_IPC_H
#define FLOW_IPC_H

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <functional>

namespace flow {

// Message types for IPC
enum class IPCMessageType {
    CALL_FUNCTION,      // Call a foreign function
    RETURN_VALUE,       // Return value from foreign function
    CALLBACK,           // Callback from foreign code to Flow
    ERROR,              // Error occurred
    INIT,               // Initialize adapter
    SHUTDOWN            // Shutdown adapter
};

// Value type for cross-language data
struct IPCValue {
    enum class Type {
        INT, FLOAT, STRING, BOOL, ARRAY, MAP, STRUCT, NULL_VALUE
    } type;
    
    union {
        int64_t intValue;
        double floatValue;
        bool boolValue;
    };
    std::string stringValue;
    std::vector<IPCValue> arrayValue;
    std::map<std::string, IPCValue> mapValue;
    
    IPCValue() : type(Type::NULL_VALUE), intValue(0) {}
    
    static IPCValue makeInt(int64_t v) {
        IPCValue val;
        val.type = Type::INT;
        val.intValue = v;
        return val;
    }
    
    static IPCValue makeFloat(double v) {
        IPCValue val;
        val.type = Type::FLOAT;
        val.floatValue = v;
        return val;
    }
    
    static IPCValue makeString(const std::string& v) {
        IPCValue val;
        val.type = Type::STRING;
        val.stringValue = v;
        return val;
    }
    
    static IPCValue makeBool(bool v) {
        IPCValue val;
        val.type = Type::BOOL;
        val.boolValue = v;
        return val;
    }
};

// IPC Message structure
struct IPCMessage {
    IPCMessageType type;
    std::string function;
    std::string module;
    std::vector<IPCValue> arguments;
    IPCValue returnValue;
    std::string error;
    int callId;  // For matching async calls
    
    std::string serialize() const;
    static IPCMessage deserialize(const std::string& data);
};

// Language adapter interface
class LanguageAdapter {
public:
    virtual ~LanguageAdapter() = default;
    
    virtual bool initialize(const std::string& module) = 0;
    virtual IPCValue call(const std::string& function, const std::vector<IPCValue>& args) = 0;
    virtual void shutdown() = 0;
    
    virtual std::string getName() const = 0;
};

// C Language Adapter
class CAdapter : public LanguageAdapter {
private:
    void* libHandle;
    std::string moduleName;
    
public:
    CAdapter() : libHandle(nullptr) {}
    ~CAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "c"; }
};

// Python Language Adapter (IPC-based)
class PythonAdapter : public LanguageAdapter {
private:
    int pipeFd[2];  // Pipe for IPC
    pid_t childPid;
    std::string moduleName;
    
public:
    PythonAdapter() : childPid(-1) { pipeFd[0] = pipeFd[1] = -1; }
    ~PythonAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "python"; }
};

// JavaScript/Node.js Language Adapter (IPC-based)
class JavaScriptAdapter : public LanguageAdapter {
private:
    int pipeFd[2];
    pid_t childPid;
    std::string moduleName;
    
public:
    JavaScriptAdapter() : childPid(-1) { pipeFd[0] = pipeFd[1] = -1; }
    ~JavaScriptAdapter() override { shutdown(); }
    
    bool initialize(const std::string& module) override;
    IPCValue call(const std::string& function, const std::vector<IPCValue>& args) override;
    void shutdown() override;
    std::string getName() const override { return "js"; }
};

// IPC Runtime - manages all adapters
class IPCRuntime {
private:
    std::map<std::string, std::unique_ptr<LanguageAdapter>> adapters;
    int nextCallId;
    
public:
    IPCRuntime() : nextCallId(0) {}
    ~IPCRuntime() { shutdownAll(); }
    
    LanguageAdapter* getAdapter(const std::string& adapterType, const std::string& module);
    IPCValue callForeign(const std::string& adapter, const std::string& module,
                         const std::string& function, const std::vector<IPCValue>& args);
    void shutdownAll();
    
    static IPCRuntime& instance() {
        static IPCRuntime runtime;
        return runtime;
    }
};

} // namespace flow

#endif // FLOW_IPC_H

