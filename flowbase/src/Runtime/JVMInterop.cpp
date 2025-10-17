#include "../../include/Runtime/JVMInterop.h"
#include <iostream>
#include <cstring>

#ifndef _WIN32
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>
#endif

#ifdef HAS_JNI
#include <jni.h>
#endif

namespace flow
{
    JVMAdapter::JVMAdapter() : jvm(nullptr), env(nullptr)
    {
    }

    JVMAdapter::~JVMAdapter()
    {
        shutdown();
    }

    bool JVMAdapter::initialize(const std::string& module)
    {
        moduleName = module;

#ifdef HAS_JNI
        JavaVMInitArgs vm_args;
        JavaVMOption options[3];

        // Set classpath
        std::string classpath = "-Djava.class.path=.";
        if (!module.empty())
        {
            classpath += ":" + module;
        }
        options[0].optionString = const_cast<char*>(classpath.c_str());

        // Enable assertions
        options[1].optionString = const_cast<char*>("-ea");

        // Set memory
        options[2].optionString = const_cast<char*>("-Xmx512m");

        vm_args.version = JNI_VERSION_1_8;
        vm_args.nOptions = 3;
        vm_args.options = options;
        vm_args.ignoreUnrecognized = JNI_FALSE;

        // Create the JVM
        jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
        if (res != JNI_OK)
        {
            std::cerr << "Failed to create JVM (error " << res << ")" << std::endl;
            return false;
        }

        std::cout << "JVM initialized for " << getName() << std::endl;
        return true;
#else
        std::cerr << "JVM support not compiled (use -DHAS_JNI)" << std::endl;
        return false;
#endif
    }

#ifdef HAS_JNI
    jclass JVMAdapter::findClass(const std::string& className)
    {
        if (!env) return nullptr;

        // Convert dot notation to slash notation for JNI
        std::string jniClassName = className;
        for (char& c : jniClassName)
        {
            if (c == '.') c = '/';
        }

        jclass clazz = env->FindClass(jniClassName.c_str());
        if (!clazz)
        {
            std::cerr << "Class not found: " << className << std::endl;
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        return clazz;
    }

    jmethodID JVMAdapter::getMethod(jclass clazz, const std::string& methodName,
                                    const std::string& signature)
    {
        if (!env || !clazz) return nullptr;

        jmethodID method = env->GetStaticMethodID(clazz, methodName.c_str(), signature.c_str());
        if (!method)
        {
            // Try instance method
            method = env->GetMethodID(clazz, methodName.c_str(), signature.c_str());
        }

        if (!method)
        {
            std::cerr << "Method not found: " << methodName << std::endl;
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        return method;
    }

    jobject JVMAdapter::convertToJavaObject(const IPCValue& value)
    {
        if (!env) return nullptr;

        switch (value.type)
        {
        case IPCValue::Type::INT:
            {
                jclass intClass = env->FindClass("java/lang/Integer");
                jmethodID constructor = env->GetMethodID(intClass, "<init>", "(I)V");
                return env->NewObject(intClass, constructor, (jint)value.intValue);
            }
        case IPCValue::Type::FLOAT:
            {
                jclass doubleClass = env->FindClass("java/lang/Double");
                jmethodID constructor = env->GetMethodID(doubleClass, "<init>", "(D)V");
                return env->NewObject(doubleClass, constructor, (jdouble)value.floatValue);
            }
        case IPCValue::Type::STRING:
            return env->NewStringUTF(value.stringValue.c_str());
        case IPCValue::Type::BOOL:
            {
                jclass boolClass = env->FindClass("java/lang/Boolean");
                jmethodID constructor = env->GetMethodID(boolClass, "<init>", "(Z)V");
                return env->NewObject(boolClass, constructor, (jboolean)value.boolValue);
            }
        default:
            return nullptr;
        }
    }

    IPCValue JVMAdapter::convertFromJavaObject(jobject obj)
    {
        if (!env || !obj) return IPCValue();

        jclass objClass = env->GetObjectClass(obj);
        jclass intClass = env->FindClass("java/lang/Integer");
        jclass doubleClass = env->FindClass("java/lang/Double");
        jclass stringClass = env->FindClass("java/lang/String");
        jclass boolClass = env->FindClass("java/lang/Boolean");

        if (env->IsInstanceOf(obj, intClass))
        {
            jmethodID method = env->GetMethodID(intClass, "intValue", "()I");
            jint value = env->CallIntMethod(obj, method);
            return IPCValue::makeInt(value);
        }
        else if (env->IsInstanceOf(obj, doubleClass))
        {
            jmethodID method = env->GetMethodID(doubleClass, "doubleValue", "()D");
            jdouble value = env->CallDoubleMethod(obj, method);
            return IPCValue::makeFloat(value);
        }
        else if (env->IsInstanceOf(obj, stringClass))
        {
            const char* str = env->GetStringUTFChars((jstring)obj, nullptr);
            IPCValue result = IPCValue::makeString(str);
            env->ReleaseStringUTFChars((jstring)obj, str);
            return result;
        }
        else if (env->IsInstanceOf(obj, boolClass))
        {
            jmethodID method = env->GetMethodID(boolClass, "booleanValue", "()Z");
            jboolean value = env->CallBooleanMethod(obj, method);
            return IPCValue::makeBool(value);
        }

        return IPCValue();
    }
#endif

    IPCValue JVMAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
#ifdef HAS_JNI
        if (!env)
        {
            std::cerr << "JVM not initialized" << std::endl;
            return IPCValue();
        }

        // Function name format: "ClassName.methodName"
        size_t dotPos = function.find('.');
        if (dotPos == std::string::npos)
        {
            std::cerr << "Invalid function format. Use: ClassName.methodName" << std::endl;
            return IPCValue();
        }

        std::string className = function.substr(0, dotPos);
        std::string methodName = function.substr(dotPos + 1);

        return callStatic(className, methodName, args);
#else
        std::cerr << "JVM support not compiled" << std::endl;
        return IPCValue();
#endif
    }

    IPCValue JVMAdapter::callStatic(const std::string& className, const std::string& methodName,
                                    const std::vector<IPCValue>& args)
    {
#ifdef HAS_JNI
        jclass clazz = findClass(className);
        if (!clazz) return IPCValue();


        std::string signature = "(";
        for (const auto& arg : args)
        {
            switch (arg.type)
            {
            case IPCValue::Type::INT: signature += "I";
                break;
            case IPCValue::Type::FLOAT: signature += "D";
                break;
            case IPCValue::Type::STRING: signature += "Ljava/lang/String;";
                break;
            case IPCValue::Type::BOOL: signature += "Z";
                break;
            default: break;
            }
        }
        signature += ")Ljava/lang/Object;"; // Generic return type

        jmethodID method = env->GetStaticMethodID(clazz, methodName.c_str(), signature.c_str());
        if (!method) return IPCValue();

        // Convert arguments
        jvalue* jargs = new jvalue[args.size()];
        for (size_t i = 0; i < args.size(); i++)
        {
            switch (args[i].type)
            {
            case IPCValue::Type::INT:
                jargs[i].i = args[i].intValue;
                break;
            case IPCValue::Type::FLOAT:
                jargs[i].d = args[i].floatValue;
                break;
            case IPCValue::Type::STRING:
                jargs[i].l = env->NewStringUTF(args[i].stringValue.c_str());
                break;
            case IPCValue::Type::BOOL:
                jargs[i].z = args[i].boolValue;
                break;
            default:
                break;
            }
        }

        // Call method
        jobject result = env->CallStaticObjectMethodA(clazz, method, jargs);
        delete[] jargs;

        if (env->ExceptionCheck())
        {
            env->ExceptionDescribe();
            env->ExceptionClear();
            return IPCValue();
        }

        return convertFromJavaObject(result);
#else
        return IPCValue();
#endif
    }

    void JVMAdapter::shutdown()
    {
#ifdef HAS_JNI
        if (jvm)
        {
            jvm->DestroyJavaVM();
            jvm = nullptr;
            env = nullptr;
        }
#endif
    }


#ifndef _WIN32
    SubprocessAdapter::SubprocessAdapter(const std::string& lang, const std::string& exec)
        : languageName(lang), executable(exec), childPid(-1), running(false)
    {
        pipeIn[0] = pipeIn[1] = -1;
        pipeOut[0] = pipeOut[1] = -1;
    }

    SubprocessAdapter::~SubprocessAdapter()
    {
        shutdown();
    }

    bool SubprocessAdapter::initialize(const std::string& module)
    {
        // Create pipes for bidirectional communication
        if (pipe(pipeIn) == -1 || pipe(pipeOut) == -1)
        {
            std::cerr << "Failed to create pipes" << std::endl;
            return false;
        }

        childPid = fork();

        if (childPid == -1)
        {
            std::cerr << "Failed to fork process" << std::endl;
            return false;
        }

        if (childPid == 0)
        {
            // Child process
            close(pipeIn[1]); // Close write end of input
            close(pipeOut[0]); // Close read end of output

            dup2(pipeIn[0], STDIN_FILENO);
            dup2(pipeOut[1], STDOUT_FILENO);

            close(pipeIn[0]);
            close(pipeOut[1]);

            // Execute the language runtime
            if (!module.empty())
            {
                execlp(executable.c_str(), executable.c_str(), module.c_str(), NULL);
            }
            else
            {
                execlp(executable.c_str(), executable.c_str(), NULL);
            }

            exit(1);
        }

        // Parent process
        close(pipeIn[0]); // Close read end of input
        close(pipeOut[1]); // Close write end of output

        running = true;
        readerThread = std::thread(&SubprocessAdapter::messageLoop, this);

        std::cout << languageName << " subprocess adapter initialized (PID: " << childPid << ")" << std::endl;
        return true;
    }

    void SubprocessAdapter::messageLoop()
    {
        // Background thread to read responses
        while (running)
        {
            // In production: read and parse messages
            usleep(100000); // 100ms
        }
    }

    IPCValue SubprocessAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        if (childPid == -1)
        {
            std::cerr << languageName << " subprocess not running" << std::endl;
            return IPCValue();
        }

        IPCMessage msg;
        msg.type = IPCMessageType::CALL_FUNCTION;
        msg.function = function;
        msg.arguments = args;

        sendMessage(msg);

        // Wait for response
        IPCMessage response = receiveMessage();
        return response.returnValue;
    }

    void SubprocessAdapter::sendMessage(const IPCMessage& msg)
    {
        std::string serialized = msg.serialize() + "\n";
        write(pipeIn[1], serialized.c_str(), serialized.size());
    }

    IPCMessage SubprocessAdapter::receiveMessage()
    {
        // Simplified - in production, implement proper message reading
        IPCMessage msg;
        msg.returnValue = IPCValue::makeInt(0);
        return msg;
    }

    void SubprocessAdapter::shutdown()
    {
        running = false;

        if (readerThread.joinable())
        {
            readerThread.join();
        }

        if (childPid != -1)
        {
            kill(childPid, SIGTERM);
            waitpid(childPid, nullptr, 0);
            childPid = -1;
        }

        if (pipeIn[1] != -1) close(pipeIn[1]);
        if (pipeOut[0] != -1) close(pipeOut[0]);
    }
#else
    // Windows stubs for SubprocessAdapter
    SubprocessAdapter::SubprocessAdapter(const std::string& lang, const std::string& exec)
        : languageName(lang), executable(exec), childPid(-1), running(false)
    {
        std::cerr << "SubprocessAdapter not supported on Windows" << std::endl;
    }

    SubprocessAdapter::~SubprocessAdapter()
    {
    }

    bool SubprocessAdapter::initialize(const std::string& module)
    {
        std::cerr << languageName << " subprocess adapter not supported on Windows" << std::endl;
        return false;
    }

    void SubprocessAdapter::messageLoop()
    {
    }

    IPCValue SubprocessAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        std::cerr << languageName << " subprocess adapter not supported on Windows" << std::endl;
        return IPCValue();
    }

    void SubprocessAdapter::sendMessage(const IPCMessage& msg)
    {
    }

    IPCMessage SubprocessAdapter::receiveMessage()
    {
        return IPCMessage();
    }

    void SubprocessAdapter::shutdown()
    {
    }
#endif

    // ============================================================
    // GRPC ADAPTER - For any language supporting gRPC
    // ============================================================

    GRPCAdapter::GRPCAdapter() : channel(nullptr)
    {
    }

    GRPCAdapter::~GRPCAdapter()
    {
        shutdown();
    }

    bool GRPCAdapter::initialize(const std::string& module)
    {
        serverAddress = module;
        std::cout << "gRPC adapter initialized for: " << serverAddress << std::endl;
        // In production: create gRPC channel
        return true;
    }

    IPCValue GRPCAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        std::cout << "gRPC call to: " << function << std::endl;
        // In production: make gRPC call
        return IPCValue::makeInt(0);
    }

    void GRPCAdapter::shutdown()
    {
        if (channel)
        {
            // Cleanup gRPC channel
            channel = nullptr;
        }
    }

    void GRPCAdapter::setServerAddress(const std::string& address)
    {
        serverAddress = address;
    }

    void GRPCAdapter::setTimeout(int milliseconds)
    {
        (void)milliseconds; // Unused for now
    }

    // ============================================================
    // HTTP ADAPTER - For microservices
    // ============================================================

    HTTPAdapter::HTTPAdapter() : timeout(5000)
    {
    }

    HTTPAdapter::~HTTPAdapter()
    {
        shutdown();
    }

    bool HTTPAdapter::initialize(const std::string& module)
    {
        baseURL = module; // Module is the base URL
        std::cout << "HTTP adapter initialized for: " << baseURL << std::endl;
        return true;
    }

    void HTTPAdapter::addHeader(const std::string& key, const std::string& value)
    {
        headers[key] = value;
    }

    IPCValue HTTPAdapter::call(const std::string& function, const std::vector<IPCValue>& args)
    {
        std::string url = baseURL + "/" + function;

        // Build JSON body
        std::string body = "{\"args\":[";
        for (size_t i = 0; i < args.size(); i++)
        {
            if (i > 0) body += ",";
            switch (args[i].type)
            {
            case IPCValue::Type::INT:
                body += std::to_string(args[i].intValue);
                break;
            case IPCValue::Type::FLOAT:
                body += std::to_string(args[i].floatValue);
                break;
            case IPCValue::Type::STRING:
                body += "\"" + args[i].stringValue + "\"";
                break;
            case IPCValue::Type::BOOL:
                body += args[i].boolValue ? "true" : "false";
                break;
            default:
                body += "null";
            }
        }
        body += "]}";

        std::string response = httpPost(url, body);

        // Parse response (simplified)
        return IPCValue::makeString(response);
    }

    std::string HTTPAdapter::httpPost(const std::string& url, const std::string& body)
    {
        // In production: use libcurl or similar
        std::cout << "HTTP POST to: " << url << std::endl;
        std::cout << "Body: " << body << std::endl;
        return "{\"result\":\"ok\"}";
    }

    void HTTPAdapter::shutdown()
    {
        // Cleanup HTTP resources
    }

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    std::unique_ptr<EnhancedLanguageAdapter> createAdapterForLanguage(const std::string& language)
    {
        if (language == "java")
        {
            return std::make_unique<JavaAdapter>();
        }
        else if (language == "kotlin")
        {
            return std::make_unique<KotlinAdapter>();
        }
        else if (language == "scala")
        {
            return std::make_unique<ScalaAdapter>();
        }
        else if (language == "csharp" || language == "cs")
        {
            return std::make_unique<CSharpAdapter>();
        }
        else if (language == "ruby")
        {
            return std::make_unique<RubyAdapter>();
        }
        else if (language == "php")
        {
            return std::make_unique<PHPAdapter>();
        }
        else if (language == "swift")
        {
            return std::make_unique<SwiftAdapter>();
        }
        return nullptr;
    }

    bool canEmbedLanguage(const std::string& language)
    {
        return language == "python" || language == "javascript" || language == "js" ||
            language == "lua" || language == "tcl";
    }

    bool requiresSubprocess(const std::string& language)
    {
        return language == "ruby" || language == "php" || language == "swift" ||
            language == "csharp" || language == "cs";
    }

    bool requiresJVM(const std::string& language)
    {
        return language == "java" || language == "kotlin" || language == "scala" ||
            language == "clojure" || language == "groovy";
    }
} // namespace flow