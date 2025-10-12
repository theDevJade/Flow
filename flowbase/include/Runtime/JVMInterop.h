#ifndef FLOW_JVM_INTEROP_H
#define FLOW_JVM_INTEROP_H

#include "Interop.h"
#include <map>
#include <string>

#ifdef HAS_JNI
#include <jni.h>
#endif

namespace flow {
    class JVMAdapter : public EnhancedLanguageAdapter {
    private:
#ifdef HAS_JNI
        JavaVM *jvm;
        JNIEnv *env;
        std::map<std::string, jobject> cachedObjects;
        std::map<std::string, jmethodID> cachedMethods;
#else
        void *jvm;
        void *env;
#endif
        std::string moduleName;

#ifdef HAS_JNI
        // JNI type conversion helpers
        jobject convertToJavaObject(const IPCValue &value);
        IPCValue convertFromJavaObject(jobject obj);
        jvalue *marshalArguments(const std::vector<IPCValue> &args);

        // Class and method lookup
        jclass findClass(const std::string &className);
        jmethodID getMethod(jclass clazz, const std::string &methodName, const std::string &signature);
#endif

    public:
        JVMAdapter();

        ~JVMAdapter() override;

        bool initialize(const std::string &module) override;

        IPCValue call(const std::string &function, const std::vector<IPCValue> &args) override;

        void shutdown() override;

        std::string getName() const override { return "jvm"; }

        // JVM-specific methods
        bool loadJar(const std::string &jarPath);

        IPCValue callStatic(const std::string &className, const std::string &methodName,
                            const std::vector<IPCValue> &args);

        IPCValue callInstance(const std::string &className, const std::string &methodName,
                              const std::vector<IPCValue> &args);

#ifdef HAS_JNI
        // Create Java objects from Flow
        jobject createObject(const std::string &className, const std::vector<IPCValue> &args);
#endif
    };


    class GRPCAdapter : public EnhancedLanguageAdapter {
    private:
        std::string serverAddress;
        void *channel; // gRPC channel
        std::string moduleName;

    public:
        GRPCAdapter();

        ~GRPCAdapter() override;

        bool initialize(const std::string &module) override;

        IPCValue call(const std::string &function, const std::vector<IPCValue> &args) override;

        void shutdown() override;

        std::string getName() const override { return "grpc"; }

        // gRPC-specific configuration
        void setServerAddress(const std::string &address);

        void setTimeout(int milliseconds);
    };


    class SubprocessAdapter : public EnhancedLanguageAdapter {
    private:
        std::string languageName;
        std::string executable; // e.g., "java", "kotlin", "dotnet", "ruby"
        pid_t childPid;
        int pipeIn[2]; // Flow -> Subprocess
        int pipeOut[2]; // Subprocess -> Flow
        std::thread readerThread;
        bool running;

        void startSubprocess(const std::string &command);

        void sendMessage(const IPCMessage &msg);

        IPCMessage receiveMessage();

        void messageLoop();

    public:
        SubprocessAdapter(const std::string &lang, const std::string &exec);

        ~SubprocessAdapter() override;

        bool initialize(const std::string &module) override;

        IPCValue call(const std::string &function, const std::vector<IPCValue> &args) override;

        void shutdown() override;

        std::string getName() const override { return languageName; }

        // Configuration
        void setExecutable(const std::string &exec) { executable = exec; }

        void setWorkingDirectory(const std::string &dir);

        void addEnvironmentVariable(const std::string &key, const std::string &value);
    };

    // ============================================================
    // HTTP/REST ADAPTER - For microservices
    // ============================================================

    class HTTPAdapter : public EnhancedLanguageAdapter {
    private:
        std::string baseURL;
        std::map<std::string, std::string> headers;
        int timeout;

        std::string httpPost(const std::string &url, const std::string &body);

    public:
        HTTPAdapter();

        ~HTTPAdapter() override;

        bool initialize(const std::string &module) override;

        IPCValue call(const std::string &function, const std::vector<IPCValue> &args) override;

        void shutdown() override;

        std::string getName() const override { return "http"; }

        // HTTP-specific configuration
        void setBaseURL(const std::string &url) { baseURL = url; }

        void addHeader(const std::string &key, const std::string &value);

        void setTimeout(int ms) { timeout = ms; }
    };

    // ============================================================
    // LANGUAGE-SPECIFIC CONVENIENCE ADAPTERS
    // ============================================================

    // Java adapter (uses JVM)
    class JavaAdapter : public JVMAdapter {
    public:
        std::string getName() const override { return "java"; }
    };

    // Kotlin adapter (uses JVM)
    class KotlinAdapter : public JVMAdapter {
    public:
        std::string getName() const override { return "kotlin"; }
    };

    // Scala adapter (uses JVM)
    class ScalaAdapter : public JVMAdapter {
    public:
        std::string getName() const override { return "scala"; }
    };

    // C# adapter (uses subprocess to dotnet)
    class CSharpAdapter : public SubprocessAdapter {
    public:
        CSharpAdapter() : SubprocessAdapter("csharp", "dotnet") {
        }
    };

    // Ruby adapter (subprocess)
    class RubyAdapter : public SubprocessAdapter {
    public:
        RubyAdapter() : SubprocessAdapter("ruby", "ruby") {
        }
    };

    // PHP adapter (subprocess)
    class PHPAdapter : public SubprocessAdapter {
    public:
        PHPAdapter() : SubprocessAdapter("php", "php") {
        }
    };

    // Swift adapter (subprocess)
    class SwiftAdapter : public SubprocessAdapter {
    public:
        SwiftAdapter() : SubprocessAdapter("swift", "swift") {
        }
    };

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    // Get appropriate adapter for a language
    std::unique_ptr<EnhancedLanguageAdapter> createAdapterForLanguage(const std::string &language);

    // Language detection
    bool canEmbedLanguage(const std::string &language);

    bool requiresSubprocess(const std::string &language);

    bool requiresJVM(const std::string &language);
} // namespace flow

#endif // FLOW_JVM_INTEROP_H