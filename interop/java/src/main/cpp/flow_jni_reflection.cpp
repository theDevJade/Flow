/**
 * JNI bridge for Flow module reflection
 * Connects Java reflection methods to the C reflection API
 */

#include <jni.h>
#include <string>
#include <vector>
#include "flow.h"
#include "flow_reflect.h"

// Helper to convert Java string to C string
const char* jstringToC(JNIEnv* env, jstring jstr) {
    if (!jstr) return nullptr;
    return env->GetStringUTFChars(jstr, nullptr);
}

// Helper to release C string
void releaseJString(JNIEnv* env, jstring jstr, const char* cstr) {
    if (jstr && cstr) {
        env->ReleaseStringUTFChars(jstr, cstr);
    }
}

extern "C" {

/*
 * Class:     com_flowlang_bindings_FlowModule
 * Method:    getFunctionCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_flowlang_bindings_FlowModule_getFunctionCount
  (JNIEnv* env, jobject obj) {
    
    // Get the native handle field
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "nativeHandle", "J");
    jlong handle = env->GetLongField(obj, fid);
    
    if (handle == 0) {
        return 0;
    }
    
    flow_module_t* module = reinterpret_cast<flow_module_t*>(handle);
    return flow_reflect_function_count(module);
}

/*
 * Class:     com_flowlang_bindings_FlowModule
 * Method:    listFunctions
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_flowlang_bindings_FlowModule_listFunctions
  (JNIEnv* env, jobject obj) {
    
    // Get the native handle
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "nativeHandle", "J");
    jlong handle = env->GetLongField(obj, fid);
    
    if (handle == 0) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
    
    flow_module_t* module = reinterpret_cast<flow_module_t*>(handle);
    int count = flow_reflect_function_count(module);
    
    if (count == 0) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
    
    // Create Java string array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, stringClass, nullptr);
    
    // Get function names one at a time
    for (int i = 0; i < count; i++) {
        const char* name = flow_reflect_function_name_at(module, i);
        if (name) {
            jstring jname = env->NewStringUTF(name);
            env->SetObjectArrayElement(result, i, jname);
            env->DeleteLocalRef(jname);
        }
    }
    
    return result;
}

/*
 * Class:     com_flowlang_bindings_FlowModule
 * Method:    getFunctionInfo
 * Signature: (Ljava/lang/String;)Lcom/flowlang/bindings/FlowModule$FunctionInfo;
 */
JNIEXPORT jobject JNICALL Java_com_flowlang_bindings_FlowModule_getFunctionInfo
  (JNIEnv* env, jobject obj, jstring jfuncName) {
    
    // Get the native handle
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "nativeHandle", "J");
    jlong handle = env->GetLongField(obj, fid);
    
    if (handle == 0) {
        jclass exceptionClass = env->FindClass("com/flowlang/bindings/FlowException");
        env->ThrowNew(exceptionClass, "Module is not loaded");
        return nullptr;
    }
    
    const char* funcName = jstringToC(env, jfuncName);
    if (!funcName) {
        jclass exceptionClass = env->FindClass("com/flowlang/bindings/FlowException");
        env->ThrowNew(exceptionClass, "Invalid function name");
        return nullptr;
    }
    
    flow_module_t* module = reinterpret_cast<flow_module_t*>(handle);
    flow_function_info_t* info = flow_reflect_get_function_info(module, funcName);
    
    releaseJString(env, jfuncName, funcName);
    
    if (!info) {
        jclass exceptionClass = env->FindClass("com/flowlang/bindings/FlowException");
        std::string msg = "Function '";
        msg += funcName;
        msg += "' not found in module";
        env->ThrowNew(exceptionClass, msg.c_str());
        return nullptr;
    }
    
    // Create ParameterInfo array
    jclass paramInfoClass = env->FindClass("com/flowlang/bindings/FlowModule$ParameterInfo");
    jmethodID paramConstructor = env->GetMethodID(paramInfoClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;)V");
    
    jobjectArray paramsArray = env->NewObjectArray(info->param_count, paramInfoClass, nullptr);
    
    for (int i = 0; i < info->param_count; i++) {
        jstring paramName = env->NewStringUTF(info->params[i].name);
        jstring paramType = env->NewStringUTF(info->params[i].type);
        
        jobject paramInfo = env->NewObject(paramInfoClass, paramConstructor, paramName, paramType);
        env->SetObjectArrayElement(paramsArray, i, paramInfo);
        
        env->DeleteLocalRef(paramName);
        env->DeleteLocalRef(paramType);
        env->DeleteLocalRef(paramInfo);
    }
    
    // Create FunctionInfo
    jclass funcInfoClass = env->FindClass("com/flowlang/bindings/FlowModule$FunctionInfo");
    jmethodID funcConstructor = env->GetMethodID(funcInfoClass, "<init>",
        "(Ljava/lang/String;Ljava/lang/String;[Lcom/flowlang/bindings/FlowModule$ParameterInfo;)V");
    
    jstring jname = env->NewStringUTF(info->name);
    jstring jreturnType = env->NewStringUTF(info->return_type);
    
    jobject functionInfo = env->NewObject(funcInfoClass, funcConstructor, 
        jname, jreturnType, paramsArray);
    
    env->DeleteLocalRef(jname);
    env->DeleteLocalRef(jreturnType);
    env->DeleteLocalRef(paramsArray);
    
    // Free the C structure
    flow_reflect_free_function_info(info);
    
    return functionInfo;
}

} // extern "C"

