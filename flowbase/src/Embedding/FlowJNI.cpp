#include "../../include/Embedding/FlowAPI.h"
#include <jni.h>
#include <string>
#include <cstring>
#include <iostream>


static std::string jstring_to_string(JNIEnv *env, jstring jstr) {
    if (!jstr) return "";
    const char *str = env->GetStringUTFChars(jstr, nullptr);
    std::string result(str);
    env->ReleaseStringUTFChars(jstr, str);
    return result;
}


static void throw_flow_exception(JNIEnv *env, const char *message) {
    jclass exClass = env->FindClass("com/flowlang/bindings/FlowException");
    if (exClass) {
        env->ThrowNew(exClass, message);
    }
}


extern "C" {
JNIEXPORT jlong

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createNative(JNIEnv *env, jobject obj) {
    FlowRuntime *runtime = flow_runtime_new();
    return reinterpret_cast<jlong>(runtime);
}

JNIEXPORT void JNICALL

Java_com_flowlang_bindings_FlowRuntime_freeNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle != 0) {
        FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(handle);
        flow_runtime_free(runtime);
    }
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_compile(JNIEnv *env, jobject obj, jstring jsource, jstring jmoduleName) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    if (!runtime) {
        throw_flow_exception(env, "Invalid runtime handle");
        return nullptr;
    }

    std::string source = jstring_to_string(env, jsource);
    std::string moduleName = jstring_to_string(env, jmoduleName);

    FlowModule *module = flow_module_compile(runtime, source.c_str(), moduleName.c_str());
    if (!module) {
        const char *error = flow_runtime_get_error(runtime);
        throw_flow_exception(env, error ? error : "Compilation failed");
        return nullptr;
    }


    jclass moduleClass = env->FindClass("com/flowlang/bindings/FlowModule");
    jmethodID constructor = env->GetMethodID(moduleClass, "<init>", "(JLjava/lang/String;)V");
    return env->NewObject(moduleClass, constructor, reinterpret_cast<jlong>(module), jmoduleName);
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_loadFileNative(JNIEnv *env, jobject obj, jstring jfilePath) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    if (!runtime) {
        throw_flow_exception(env, "Invalid runtime handle");
        return nullptr;
    }

    std::string filePath = jstring_to_string(env, jfilePath);

    FlowModule *module = flow_module_load_file(runtime, filePath.c_str());
    if (!module) {
        const char *error = flow_runtime_get_error(runtime);
        throw_flow_exception(env, error ? error : "Failed to load file");
        return nullptr;
    }


    size_t lastSlash = filePath.find_last_of("/\\");
    size_t lastDot = filePath.find_last_of('.');
    std::string moduleName = filePath.substr(lastSlash + 1, lastDot - lastSlash - 1);

    jclass moduleClass = env->FindClass("com/flowlang/bindings/FlowModule");
    jmethodID constructor = env->GetMethodID(moduleClass, "<init>", "(JLjava/lang/String;)V");
    return env->NewObject(moduleClass, constructor, reinterpret_cast<jlong>(module),
                          env->NewStringUTF(moduleName.c_str()));
}

JNIEXPORT jstring

JNICALL
Java_com_flowlang_bindings_FlowRuntime_getLastError(JNIEnv *env, jobject obj) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    if (!runtime) return nullptr;

    const char *error = flow_runtime_get_error(runtime);
    return error ? env->NewStringUTF(error) : nullptr;
}



JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createInt(JNIEnv *env, jobject obj, jlong value) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    FlowValue *val = flow_value_new_int(runtime, value);

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(val));
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createFloat(JNIEnv *env, jobject obj, jdouble value) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    FlowValue *val = flow_value_new_float(runtime, value);

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(val));
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createString(JNIEnv *env, jobject obj, jstring jvalue) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    std::string str = jstring_to_string(env, jvalue);
    FlowValue *val = flow_value_new_string(runtime, str.c_str());

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(val));
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createBool(JNIEnv *env, jobject obj, jboolean value) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    FlowValue *val = flow_value_new_bool(runtime, value ? 1 : 0);

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(val));
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowRuntime_createNull(JNIEnv *env, jobject obj) {
    jlong runtimeHandle = env->GetLongField(obj,
                                            env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);
    FlowValue *val = flow_value_new_null(runtime);

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(val));
}





JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowValue_getType(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowValue *value = reinterpret_cast<FlowValue *>(handle);
    if (!value) return nullptr;

    FlowValueType type = flow_value_get_type(value);

    jclass typeClass = env->FindClass("com/flowlang/bindings/FlowValueType");
    jmethodID fromInt = env->GetStaticMethodID(typeClass, "fromInt",
                                               "(I)Lcom/flowlang/bindings/FlowValueType;");
    return env->CallStaticObjectMethod(typeClass, fromInt, static_cast<jint>(type));
}

JNIEXPORT jlong

JNICALL
Java_com_flowlang_bindings_FlowValue_asInt(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowValue *value = reinterpret_cast<FlowValue *>(handle);
    if (!value) {
        throw_flow_exception(env, "Invalid value handle");
        return 0;
    }

    int64_t result;
    if (flow_value_get_int(value, &result) != FLOW_OK) {
        throw_flow_exception(env, "Value is not an integer");
        return 0;
    }

    return static_cast<jlong>(result);
}

JNIEXPORT jdouble

JNICALL
Java_com_flowlang_bindings_FlowValue_asFloat(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowValue *value = reinterpret_cast<FlowValue *>(handle);
    if (!value) {
        throw_flow_exception(env, "Invalid value handle");
        return 0.0;
    }

    double result;
    if (flow_value_get_float(value, &result) != FLOW_OK) {
        throw_flow_exception(env, "Value is not a float");
        return 0.0;
    }

    return result;
}

JNIEXPORT jstring

JNICALL
Java_com_flowlang_bindings_FlowValue_asString(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowValue *value = reinterpret_cast<FlowValue *>(handle);
    if (!value) {
        throw_flow_exception(env, "Invalid value handle");
        return nullptr;
    }

    const char *str = flow_value_get_string(value);
    if (!str) {
        throw_flow_exception(env, "Value is not a string");
        return nullptr;
    }

    return env->NewStringUTF(str);
}

JNIEXPORT jboolean

JNICALL
Java_com_flowlang_bindings_FlowValue_asBool(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowValue *value = reinterpret_cast<FlowValue *>(handle);
    if (!value) {
        throw_flow_exception(env, "Invalid value handle");
        return false;
    }

    int result;
    if (flow_value_get_bool(value, &result) != FLOW_OK) {
        throw_flow_exception(env, "Value is not a boolean");
        return false;
    }

    return result != 0;
}

JNIEXPORT void JNICALL

Java_com_flowlang_bindings_FlowValue_freeNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle != 0) {
        FlowValue *value = reinterpret_cast<FlowValue *>(handle);
        flow_value_free(value);
    }
}





JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowModule_getFunction(JNIEnv *env, jobject obj, jstring jfunctionName) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowModule *module = reinterpret_cast<FlowModule *>(handle);
    if (!module) return nullptr;

    std::string functionName = jstring_to_string(env, jfunctionName);
    FlowFunction *function = flow_module_get_function(module, functionName.c_str());

    if (!function) return nullptr;

    jclass funcClass = env->FindClass("com/flowlang/bindings/FlowFunction");
    jmethodID constructor = env->GetMethodID(funcClass, "<init>", "(JLjava/lang/String;)V");
    return env->NewObject(funcClass, constructor, reinterpret_cast<jlong>(function), jfunctionName);
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowModule_call(JNIEnv *env, jobject obj, jobject jruntime,
                                           jstring jfunctionName, jobjectArray jargs) {
    jlong moduleHandle = env->GetLongField(obj,
                                           env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));
    jlong runtimeHandle = env->GetLongField(jruntime,
                                            env->GetFieldID(env->GetObjectClass(jruntime), "nativeHandle", "J"));

    FlowModule *module = reinterpret_cast<FlowModule *>(moduleHandle);
    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);

    if (!module || !runtime) {
        throw_flow_exception(env, "Invalid module or runtime handle");
        return nullptr;
    }

    std::string functionName = jstring_to_string(env, jfunctionName);


    int argCount = jargs ? env->GetArrayLength(jargs) : 0;
    FlowValue **args = new FlowValue *[argCount];

    for (int i = 0; i < argCount; i++) {
        jobject jarg = env->GetObjectArrayElement(jargs, i);
        jlong argHandle = env->GetLongField(jarg,
                                            env->GetFieldID(env->GetObjectClass(jarg), "nativeHandle", "J"));
        args[i] = reinterpret_cast<FlowValue *>(argHandle);
    }

    FlowValue *result = nullptr;
    FlowResult res = flow_call(runtime, module, functionName.c_str(), args, argCount, &result);

    delete[] args;

    if (res != FLOW_OK) {
        const char *error = flow_runtime_get_error(runtime);
        throw_flow_exception(env, error ? error : "Function call failed");
        return nullptr;
    }

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(result));
}

JNIEXPORT void JNICALL

Java_com_flowlang_bindings_FlowModule_freeNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle != 0) {
        FlowModule *module = reinterpret_cast<FlowModule *>(handle);
        flow_module_free(module);
    }
}





JNIEXPORT jint

JNICALL
Java_com_flowlang_bindings_FlowFunction_getParameterCount(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj,
                                     env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));

    FlowFunction *function = reinterpret_cast<FlowFunction *>(handle);
    if (!function) return -1;

    return flow_function_get_param_count(function);
}

JNIEXPORT jobject

JNICALL
Java_com_flowlang_bindings_FlowFunction_call(JNIEnv *env, jobject obj, jobject jruntime, jobjectArray jargs) {
    jlong functionHandle = env->GetLongField(obj,
                                             env->GetFieldID(env->GetObjectClass(obj), "nativeHandle", "J"));
    jlong runtimeHandle = env->GetLongField(jruntime,
                                            env->GetFieldID(env->GetObjectClass(jruntime), "nativeHandle", "J"));

    FlowFunction *function = reinterpret_cast<FlowFunction *>(functionHandle);
    FlowRuntime *runtime = reinterpret_cast<FlowRuntime *>(runtimeHandle);

    if (!function || !runtime) {
        throw_flow_exception(env, "Invalid function or runtime handle");
        return nullptr;
    }

    int argCount = jargs ? env->GetArrayLength(jargs) : 0;
    FlowValue **args = new FlowValue *[argCount];

    for (int i = 0; i < argCount; i++) {
        jobject jarg = env->GetObjectArrayElement(jargs, i);
        jlong argHandle = env->GetLongField(jarg,
                                            env->GetFieldID(env->GetObjectClass(jarg), "nativeHandle", "J"));
        args[i] = reinterpret_cast<FlowValue *>(argHandle);
    }

    FlowValue *result = nullptr;
    FlowResult res = flow_function_call(runtime, function, args, argCount, &result);

    delete[] args;

    if (res != FLOW_OK) {
        const char *error = flow_runtime_get_error(runtime);
        throw_flow_exception(env, error ? error : "Function call failed");
        return nullptr;
    }

    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(result));
}
} // extern "C"