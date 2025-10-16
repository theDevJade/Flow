// Complete JNI bridge for Flow Java bindings
// This file implements all native methods for FlowRuntime, FlowModule, and FlowValue

#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include "../../flowbase/include/Embedding/FlowAPI.h"

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

// Throw a FlowException to Java
void throwFlowException(JNIEnv* env, const char* message) {
    jclass exClass = env->FindClass("com/flowlang/bindings/FlowException");
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
    }
}

// Convert C string to Java string (handles NULL)
jstring cStringToJString(JNIEnv* env, const char* str) {
    if (str == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(str);
}

// Get FlowRuntime* from Java object
FlowRuntime* getFlowRuntime(JNIEnv* env, jobject runtime_obj) {
    jclass runtimeClass = env->GetObjectClass(runtime_obj);
    jmethodID getNativeHandle = env->GetMethodID(runtimeClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(runtime_obj, getNativeHandle);
    return reinterpret_cast<FlowRuntime*>(handle);
}

// ============================================================================
// FLOWRUNTIME JNI METHODS
// ============================================================================

extern "C" {

// Create Flow runtime
JNIEXPORT jlong JNICALL Java_com_flowlang_bindings_FlowRuntime_createNative
  (JNIEnv* env, jobject) {
    FlowRuntime* runtime = flow_runtime_new();
    return reinterpret_cast<jlong>(runtime);
}

// Free Flow runtime
JNIEXPORT void JNICALL Java_com_flowlang_bindings_FlowRuntime_freeNative
  (JNIEnv* env, jobject, jlong handle) {
    if (handle != 0) {
        FlowRuntime* runtime = reinterpret_cast<FlowRuntime*>(handle);
        flow_runtime_free(runtime);
    }
}

// Compile source code
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_compile
  (JNIEnv* env, jobject runtime_obj, jstring source, jstring module_name) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    if (!runtime) {
        throwFlowException(env, "Invalid runtime handle");
        return nullptr;
    }
    
    const char* sourceStr = env->GetStringUTFChars(source, nullptr);
    const char* moduleNameStr = env->GetStringUTFChars(module_name, nullptr);
    
    FlowModule* module = flow_module_compile(runtime, sourceStr, moduleNameStr);
    
    env->ReleaseStringUTFChars(source, sourceStr);
    env->ReleaseStringUTFChars(module_name, moduleNameStr);
    
    if (!module) {
        const char* error = flow_runtime_get_error(runtime);
        throwFlowException(env, error ? error : "Compilation failed");
        return nullptr;
    }
    
    // Create Java FlowModule object
    jclass moduleClass = env->FindClass("com/flowlang/bindings/FlowModule");
    jmethodID constructor = env->GetMethodID(moduleClass, "<init>", "(JLjava/lang/String;)V");
    
    jobject moduleObj = env->NewObject(moduleClass, constructor, 
                                       reinterpret_cast<jlong>(module),
                                       module_name);
    
    return moduleObj;
}

// Load module from file
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_loadFileNative
  (JNIEnv* env, jobject runtime_obj, jstring file_path) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    if (!runtime) {
        throwFlowException(env, "Invalid runtime handle");
        return nullptr;
    }
    
    const char* pathStr = env->GetStringUTFChars(file_path, nullptr);
    
    FlowModule* module = flow_module_load_file(runtime, pathStr);
    
    env->ReleaseStringUTFChars(file_path, pathStr);
    
    if (!module) {
        const char* error = flow_runtime_get_error(runtime);
        throwFlowException(env, error ? error : "Failed to load module");
        return nullptr;
    }
    
    // Extract module name from path (simple version - just use the path)
    jstring moduleName = file_path;
    
    // Create Java FlowModule object
    jclass moduleClass = env->FindClass("com/flowlang/bindings/FlowModule");
    jmethodID constructor = env->GetMethodID(moduleClass, "<init>", "(JLjava/lang/String;)V");
    
    jobject moduleObj = env->NewObject(moduleClass, constructor, 
                                       reinterpret_cast<jlong>(module),
                                       moduleName);
    
    return moduleObj;
}

// Get last error
JNIEXPORT jstring JNICALL Java_com_flowlang_bindings_FlowRuntime_getLastError
  (JNIEnv* env, jobject runtime_obj) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    if (!runtime) {
        return nullptr;
    }
    
    const char* error = flow_runtime_get_error(runtime);
    return cStringToJString(env, error);
}

// Create integer value
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_createInt
  (JNIEnv* env, jobject runtime_obj, jlong value) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    FlowValue* flowVal = flow_value_new_int(runtime, value);
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(flowVal));
}

// Create float value
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_createFloat
  (JNIEnv* env, jobject runtime_obj, jdouble value) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    FlowValue* flowVal = flow_value_new_float(runtime, value);
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(flowVal));
}

// Create string value
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_createString
  (JNIEnv* env, jobject runtime_obj, jstring value) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    const char* strVal = env->GetStringUTFChars(value, nullptr);
    FlowValue* flowVal = flow_value_new_string(runtime, strVal);
    env->ReleaseStringUTFChars(value, strVal);
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(flowVal));
}

// Create boolean value
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_createBool
  (JNIEnv* env, jobject runtime_obj, jboolean value) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    FlowValue* flowVal = flow_value_new_bool(runtime, value);
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(flowVal));
}

// Create null value
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowRuntime_createNull
  (JNIEnv* env, jobject runtime_obj) {
    
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    FlowValue* flowVal = flow_value_new_null(runtime);
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID constructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    
    return env->NewObject(valueClass, constructor, reinterpret_cast<jlong>(flowVal));
}

// ============================================================================
// FLOWMODULE JNI METHODS
// ============================================================================

// Get function by name
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowModule_getFunction
  (JNIEnv* env, jobject module_obj, jstring function_name) {
    
    jclass moduleClass = env->GetObjectClass(module_obj);
    jmethodID getNativeHandle = env->GetMethodID(moduleClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(module_obj, getNativeHandle);
    
    FlowModule* module = reinterpret_cast<FlowModule*>(handle);
    if (!module) {
        return nullptr;
    }
    
    const char* funcName = env->GetStringUTFChars(function_name, nullptr);
    FlowFunction* function = flow_module_get_function(module, funcName);
    env->ReleaseStringUTFChars(function_name, funcName);
    
    if (!function) {
        return nullptr;
    }
    
    // Create Java FlowFunction object
    jclass functionClass = env->FindClass("com/flowlang/bindings/FlowFunction");
    jmethodID constructor = env->GetMethodID(functionClass, "<init>", "(JLjava/lang/String;)V");
    
    return env->NewObject(functionClass, constructor, 
                         reinterpret_cast<jlong>(function),
                         function_name);
}

// Call function
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowModule_call
  (JNIEnv* env, jobject module_obj, jobject runtime_obj, jstring function_name, jobjectArray args) {
    
    // Get module handle
    jclass moduleClass = env->GetObjectClass(module_obj);
    jmethodID getModuleHandle = env->GetMethodID(moduleClass, "getNativeHandle", "()J");
    jlong moduleHandle = env->CallLongMethod(module_obj, getModuleHandle);
    FlowModule* module = reinterpret_cast<FlowModule*>(moduleHandle);
    
    if (!module) {
        throwFlowException(env, "Invalid module handle");
        return nullptr;
    }
    
    // Get runtime
    FlowRuntime* runtime = getFlowRuntime(env, runtime_obj);
    if (!runtime) {
        throwFlowException(env, "Invalid runtime handle");
        return nullptr;
    }
    
    // Get function
    const char* funcName = env->GetStringUTFChars(function_name, nullptr);
    FlowFunction* function = flow_module_get_function(module, funcName);
    env->ReleaseStringUTFChars(function_name, funcName);
    
    if (!function) {
        throwFlowException(env, "Function not found");
        return nullptr;
    }
    
    // Convert Java args to FlowValue array
    jsize argc = args ? env->GetArrayLength(args) : 0;
    std::vector<FlowValue*> flowArgs;
    
    jclass valueClass = env->FindClass("com/flowlang/bindings/FlowValue");
    jmethodID getValueHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    
    for (jsize i = 0; i < argc; i++) {
        jobject argObj = env->GetObjectArrayElement(args, i);
        jlong argHandle = env->CallLongMethod(argObj, getValueHandle);
        flowArgs.push_back(reinterpret_cast<FlowValue*>(argHandle));
    }
    
    // Call function
    FlowValue* result = nullptr;
    FlowResult flowResult = flow_function_call(runtime, function, flowArgs.data(), argc, &result);
    
    if (flowResult != FLOW_OK || !result) {
        const char* error = flow_runtime_get_error(runtime);
        throwFlowException(env, error ? error : "Function call failed");
        return nullptr;
    }
    
    // Create Java FlowValue for result
    jmethodID valueConstructor = env->GetMethodID(valueClass, "<init>", "(J)V");
    return env->NewObject(valueClass, valueConstructor, reinterpret_cast<jlong>(result));
}

// Free module
JNIEXPORT void JNICALL Java_com_flowlang_bindings_FlowModule_freeNative
  (JNIEnv* env, jobject, jlong handle) {
    if (handle != 0) {
        FlowModule* module = reinterpret_cast<FlowModule*>(handle);
        flow_module_free(module);
    }
}

// ============================================================================
// FLOWVALUE JNI METHODS
// ============================================================================

// Get value type
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowValue_getType
  (JNIEnv* env, jobject value_obj) {
    
    jclass valueClass = env->GetObjectClass(value_obj);
    jmethodID getNativeHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(value_obj, getNativeHandle);
    
    FlowValue* value = reinterpret_cast<FlowValue*>(handle);
    if (!value) {
        return nullptr;
    }
    
    FlowValueType type = flow_value_get_type(value);
    
    // Map to Java enum
    jclass enumClass = env->FindClass("com/flowlang/bindings/FlowValueType");
    const char* enumName = nullptr;
    
    switch (type) {
        case FLOW_TYPE_INT: enumName = "INT"; break;
        case FLOW_TYPE_FLOAT: enumName = "FLOAT"; break;
        case FLOW_TYPE_STRING: enumName = "STRING"; break;
        case FLOW_TYPE_BOOL: enumName = "BOOL"; break;
        case FLOW_TYPE_NULL: enumName = "NULL"; break;
        default: enumName = "NULL"; break;
    }
    
    jfieldID field = env->GetStaticFieldID(enumClass, enumName, "Lcom/flowlang/bindings/FlowValueType;");
    return env->GetStaticObjectField(enumClass, field);
}

// Extract int
JNIEXPORT jlong JNICALL Java_com_flowlang_bindings_FlowValue_asInt
  (JNIEnv* env, jobject value_obj) {
    
    jclass valueClass = env->GetObjectClass(value_obj);
    jmethodID getNativeHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(value_obj, getNativeHandle);
    
    FlowValue* value = reinterpret_cast<FlowValue*>(handle);
    if (!value) {
        throwFlowException(env, "Invalid value handle");
        return 0;
    }
    
    if (flow_value_get_type(value) != FLOW_TYPE_INT) {
        throwFlowException(env, "Value is not an integer");
        return 0;
    }
    
    int64_t result = 0;
    flow_value_get_int(value, &result);
    return result;
}

// Extract float
JNIEXPORT jdouble JNICALL Java_com_flowlang_bindings_FlowValue_asFloat
  (JNIEnv* env, jobject value_obj) {
    
    jclass valueClass = env->GetObjectClass(value_obj);
    jmethodID getNativeHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(value_obj, getNativeHandle);
    
    FlowValue* value = reinterpret_cast<FlowValue*>(handle);
    if (!value) {
        throwFlowException(env, "Invalid value handle");
        return 0.0;
    }
    
    if (flow_value_get_type(value) != FLOW_TYPE_FLOAT) {
        throwFlowException(env, "Value is not a float");
        return 0.0;
    }
    
    double result = 0.0;
    flow_value_get_float(value, &result);
    return result;
}

// Extract string
JNIEXPORT jstring JNICALL Java_com_flowlang_bindings_FlowValue_asString
  (JNIEnv* env, jobject value_obj) {
    
    jclass valueClass = env->GetObjectClass(value_obj);
    jmethodID getNativeHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(value_obj, getNativeHandle);
    
    FlowValue* value = reinterpret_cast<FlowValue*>(handle);
    if (!value) {
        throwFlowException(env, "Invalid value handle");
        return nullptr;
    }
    
    if (flow_value_get_type(value) != FLOW_TYPE_STRING) {
        throwFlowException(env, "Value is not a string");
        return nullptr;
    }
    
    return cStringToJString(env, flow_value_get_string(value));
}

// Extract bool
JNIEXPORT jboolean JNICALL Java_com_flowlang_bindings_FlowValue_asBool
  (JNIEnv* env, jobject value_obj) {
    
    jclass valueClass = env->GetObjectClass(value_obj);
    jmethodID getNativeHandle = env->GetMethodID(valueClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(value_obj, getNativeHandle);
    
    FlowValue* value = reinterpret_cast<FlowValue*>(handle);
    if (!value) {
        throwFlowException(env, "Invalid value handle");
        return false;
    }
    
    if (flow_value_get_type(value) != FLOW_TYPE_BOOL) {
        throwFlowException(env, "Value is not a boolean");
        return false;
    }
    
    int result = 0;
    flow_value_get_bool(value, &result);
    return result != 0;
}

// Free value
JNIEXPORT void JNICALL Java_com_flowlang_bindings_FlowValue_freeNative
  (JNIEnv* env, jobject, jlong handle) {
    if (handle != 0) {
        FlowValue* value = reinterpret_cast<FlowValue*>(handle);
        flow_value_free(value);
    }
}

// ============================================================================
// FLOWMODULE REFLECTION JNI METHODS
// ============================================================================

// Get function count (Reflection API)
JNIEXPORT jint JNICALL Java_com_flowlang_bindings_FlowModule_getFunctionCount
  (JNIEnv* env, jobject module_obj) {
    
    jclass moduleClass = env->GetObjectClass(module_obj);
    jmethodID getNativeHandle = env->GetMethodID(moduleClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(module_obj, getNativeHandle);
    
    if (handle == 0) {
        return 0;
    }
    
    FlowModule* module = reinterpret_cast<FlowModule*>(handle);
    return flow_module_get_function_count(module);
}

// List all functions (Reflection API)
JNIEXPORT jobjectArray JNICALL Java_com_flowlang_bindings_FlowModule_listFunctions
  (JNIEnv* env, jobject module_obj) {
    
    jclass moduleClass = env->GetObjectClass(module_obj);
    jmethodID getNativeHandle = env->GetMethodID(moduleClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(module_obj, getNativeHandle);
    
    if (handle == 0) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
    
    FlowModule* module = reinterpret_cast<FlowModule*>(handle);
    int count = flow_module_get_function_count(module);
    
    if (count == 0) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
    
    // Create Java string array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, stringClass, nullptr);
    
    // Get function names one at a time
    for (int i = 0; i < count; i++) {
        const char* name = flow_module_get_function_name(module, i);
        if (name) {
            jstring jname = env->NewStringUTF(name);
            env->SetObjectArrayElement(result, i, jname);
            env->DeleteLocalRef(jname);
        }
    }
    
    return result;
}

// Get function info (Reflection API)
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowModule_getFunctionInfo
  (JNIEnv* env, jobject module_obj, jstring jfuncName) {
    
    jclass moduleClass = env->GetObjectClass(module_obj);
    jmethodID getNativeHandle = env->GetMethodID(moduleClass, "getNativeHandle", "()J");
    jlong handle = env->CallLongMethod(module_obj, getNativeHandle);
    
    if (handle == 0) {
        throwFlowException(env, "Module is not loaded");
        return nullptr;
    }
    
    FlowModule* module = reinterpret_cast<FlowModule*>(handle);
    const char* funcName = env->GetStringUTFChars(jfuncName, nullptr);
    
    if (!funcName) {
        throwFlowException(env, "Invalid function name");
        return nullptr;
    }
    
    // Get the function
    FlowFunction* function = flow_module_get_function(module, funcName);
    
    if (!function) {
        std::string msg = "Function '";
        msg += funcName;
        msg += "' not found in module";
        env->ReleaseStringUTFChars(jfuncName, funcName);
        throwFlowException(env, msg.c_str());
        return nullptr;
    }
    
    // Get function details using reflection API
    const char* funcNameFromReflect = flow_function_get_name(function);
    const char* returnType = flow_function_get_return_type(function);
    int paramCount = flow_function_get_param_count(function);
    
    // Create ParameterInfo array
    jclass paramInfoClass = env->FindClass("com/flowlang/bindings/FlowModule$ParameterInfo");
    jmethodID paramConstructor = env->GetMethodID(paramInfoClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;)V");
    
    jobjectArray paramsArray = env->NewObjectArray(paramCount, paramInfoClass, nullptr);
    
    for (int i = 0; i < paramCount; i++) {
        const char* paramName = flow_function_get_param_name(function, i);
        const char* paramType = flow_function_get_param_type(function, i);
        
        jstring jparamName = env->NewStringUTF(paramName ? paramName : "");
        jstring jparamType = env->NewStringUTF(paramType ? paramType : "");
        
        jobject paramInfo = env->NewObject(paramInfoClass, paramConstructor, jparamName, jparamType);
        env->SetObjectArrayElement(paramsArray, i, paramInfo);
        
        env->DeleteLocalRef(jparamName);
        env->DeleteLocalRef(jparamType);
        env->DeleteLocalRef(paramInfo);
    }
    
    // Create FunctionInfo
    jclass funcInfoClass = env->FindClass("com/flowlang/bindings/FlowModule$FunctionInfo");
    jmethodID funcConstructor = env->GetMethodID(funcInfoClass, "<init>",
        "(Ljava/lang/String;Ljava/lang/String;[Lcom/flowlang/bindings/FlowModule$ParameterInfo;)V");
    
    jstring jname = env->NewStringUTF(funcNameFromReflect ? funcNameFromReflect : funcName);
    jstring jreturnType = env->NewStringUTF(returnType ? returnType : "void");
    
    jobject functionInfo = env->NewObject(funcInfoClass, funcConstructor, 
        jname, jreturnType, paramsArray);
    
    env->DeleteLocalRef(jname);
    env->DeleteLocalRef(jreturnType);
    env->DeleteLocalRef(paramsArray);
    
    env->ReleaseStringUTFChars(jfuncName, funcName);
    
    return functionInfo;
}

} // extern "C"

