#include "../../include/Embedding/FlowAPI.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Codegen/CodeGenerator.h"
#include <llvm/ExecutionEngine/ExecutionEngine.h>
#include <llvm/ExecutionEngine/GenericValue.h>
#include <llvm/ExecutionEngine/MCJIT.h>
#include <llvm/Support/TargetSelect.h>
#include <llvm/Transforms/Utils/Cloning.h>
#include <llvm-c/ExecutionEngine.h>
#include <string>
#include <map>
#include <vector>
#include <memory>
#include <fstream>
#include <sstream>

using namespace flow;


struct FlowRuntime
{
    std::string lastError;
    bool initialized;

    FlowRuntime() : initialized(false)
    {
        llvm::InitializeNativeTarget();
        llvm::InitializeNativeTargetAsmPrinter();
        llvm::InitializeNativeTargetAsmParser();
        LLVMLinkInMCJIT();
        initialized = true;
    }
};

struct FlowModule
{
    FlowRuntime* runtime;
    std::string name;
    std::shared_ptr<Program> ast;
    std::unique_ptr<CodeGenerator> codegen;
    std::unique_ptr<llvm::ExecutionEngine> engine;
    std::map<std::string, FlowFunction*> functions;

    ~FlowModule()
    {
        for (auto& pair : functions)
        {
            delete pair.second;
        }
    }
};

struct FlowFunction
{
    FlowModule* module;
    std::string name;
    FunctionDecl* decl;
    void* nativePtr;
    
    // Cached reflection data
    mutable std::vector<std::string> cachedParamTypes;
    mutable std::string cachedReturnType;

    FlowFunction(FlowModule* m, const std::string& n, FunctionDecl* d)
        : module(m), name(n), decl(d), nativePtr(nullptr)
    {
        // Cache parameter types
        if (decl) {
            for (const auto& param : decl->parameters) {
                cachedParamTypes.push_back(param.type ? param.type->toString() : "unknown");
            }
            cachedReturnType = (decl->returnType ? decl->returnType->toString() : "void");
        }
    }

    ~FlowFunction()
    {
    }
};

struct FlowValue
{
    FlowValueType type;

    union
    {
        int64_t int_value;
        double float_value;
        int bool_value;
    };

    std::string string_value;

    FlowValue(FlowValueType t) : type(t), int_value(0)
    {
    }
};


FlowRuntime* flow_runtime_new()
{
    try
    {
        return new FlowRuntime();
    }
    catch (...)
    {
        return nullptr;
    }
}

void flow_runtime_free(FlowRuntime* runtime)
{
    delete runtime;
}

const char* flow_runtime_get_error(FlowRuntime* runtime)
{
    if (!runtime) return "Invalid runtime";
    return runtime->lastError.c_str();
}


FlowModule* flow_module_compile(FlowRuntime* runtime, const char* source, const char* module_name)
{
    if (!runtime || !source || !module_name)
    {
        if (runtime) runtime->lastError = "Invalid parameters";
        return nullptr;
    }

    try
    {
        // Lexical analysis
        Lexer lexer(source, module_name);
        auto tokens = lexer.tokenize();

        // Parsing
        Parser parser(tokens);
        auto program = parser.parse();

        // Semantic analysis
        SemanticAnalyzer analyzer;
        analyzer.analyze(program);
        if (analyzer.hasErrors())
        {
            runtime->lastError = "Semantic analysis failed";
            return nullptr;
        }

        // Code generation
        auto codegen = std::make_unique<CodeGenerator>(module_name);
        codegen->generate(program);

        // Create module
        FlowModule* module = new FlowModule();
        module->runtime = runtime;
        module->name = module_name;
        module->ast = program;
        module->codegen = std::move(codegen);

        // Index functions
        for (auto& decl : program->declarations)
        {
            if (FunctionDecl* funcDecl = dynamic_cast<FunctionDecl*>(decl.get()))
            {
                FlowFunction* func = new FlowFunction(module, funcDecl->name, funcDecl);
                module->functions[funcDecl->name] = func;
            }
        }

        return module;
    }
    catch (const std::exception& e)
    {
        runtime->lastError = std::string("Compilation error: ") + e.what();
        return nullptr;
    }
}

FlowModule* flow_module_load_file(FlowRuntime* runtime, const char* file_path)
{
    if (!runtime || !file_path)
    {
        if (runtime) runtime->lastError = "Invalid parameters";
        return nullptr;
    }

    try
    {
        std::ifstream file(file_path);
        if (!file.is_open())
        {
            runtime->lastError = std::string("Failed to open file: ") + file_path;
            return nullptr;
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string source = buffer.str();

        return flow_module_compile(runtime, source.c_str(), file_path);
    }
    catch (const std::exception& e)
    {
        runtime->lastError = std::string("File load error: ") + e.what();
        return nullptr;
    }
}

void flow_module_free(FlowModule* module)
{
    delete module;
}


FlowFunction* flow_module_get_function(FlowModule* module, const char* function_name)
{
    if (!module || !function_name) return nullptr;

    auto it = module->functions.find(function_name);
    if (it != module->functions.end())
    {
        return it->second;
    }

    module->runtime->lastError = std::string("Function not found: ") + function_name;
    return nullptr;
}

int flow_function_get_param_count(FlowFunction* function)
{
    if (!function || !function->decl) return -1;
    return function->decl->parameters.size();
}

// ============================================================
// REFLECTION API IMPLEMENTATIONS
// ============================================================

int flow_module_get_function_count(FlowModule* module)
{
    if (!module) return 0;
    return module->functions.size();
}

const char* flow_module_get_function_name(FlowModule* module, int index)
{
    if (!module || index < 0 || index >= (int)module->functions.size())
        return nullptr;
    
    auto it = module->functions.begin();
    std::advance(it, index);
    return it->first.c_str();
}

const char* flow_function_get_name(FlowFunction* function)
{
    if (!function) return nullptr;
    return function->name.c_str();
}

const char* flow_function_get_param_name(FlowFunction* function, int param_index)
{
    if (!function || !function->decl) return nullptr;
    if (param_index < 0 || param_index >= (int)function->decl->parameters.size())
        return nullptr;
    
    return function->decl->parameters[param_index].name.c_str();
}

const char* flow_function_get_param_type(FlowFunction* function, int param_index)
{
    if (!function) return nullptr;
    if (param_index < 0 || param_index >= (int)function->cachedParamTypes.size())
        return nullptr;
    
    return function->cachedParamTypes[param_index].c_str();
}

const char* flow_function_get_return_type(FlowFunction* function)
{
    if (!function) return nullptr;
    return function->cachedReturnType.c_str();
}


FlowValue* flow_value_new_int(FlowRuntime* runtime, int64_t value)
{
    if (!runtime) return nullptr;
    FlowValue* v = new FlowValue(FLOW_TYPE_INT);
    v->int_value = value;
    return v;
}

FlowValue* flow_value_new_float(FlowRuntime* runtime, double value)
{
    if (!runtime) return nullptr;
    FlowValue* v = new FlowValue(FLOW_TYPE_FLOAT);
    v->float_value = value;
    return v;
}

FlowValue* flow_value_new_string(FlowRuntime* runtime, const char* value)
{
    if (!runtime || !value) return nullptr;
    FlowValue* v = new FlowValue(FLOW_TYPE_STRING);
    v->string_value = value;
    return v;
}

FlowValue* flow_value_new_bool(FlowRuntime* runtime, int value)
{
    if (!runtime) return nullptr;
    FlowValue* v = new FlowValue(FLOW_TYPE_BOOL);
    v->bool_value = value ? 1 : 0;
    return v;
}

FlowValue* flow_value_new_null(FlowRuntime* runtime)
{
    if (!runtime) return nullptr;
    return new FlowValue(FLOW_TYPE_NULL);
}

void flow_value_free(FlowValue* value)
{
    delete value;
}

FlowValueType flow_value_get_type(FlowValue* value)
{
    if (!value) return FLOW_TYPE_NULL;
    return value->type;
}

FlowResult flow_value_get_int(FlowValue* value, int64_t* out)
{
    if (!value || !out) return FLOW_ERROR_INVALID_ARGS;
    if (value->type != FLOW_TYPE_INT) return FLOW_ERROR_TYPE_MISMATCH;
    *out = value->int_value;
    return FLOW_OK;
}

FlowResult flow_value_get_float(FlowValue* value, double* out)
{
    if (!value || !out) return FLOW_ERROR_INVALID_ARGS;
    if (value->type != FLOW_TYPE_FLOAT) return FLOW_ERROR_TYPE_MISMATCH;
    *out = value->float_value;
    return FLOW_OK;
}

const char* flow_value_get_string(FlowValue* value)
{
    if (!value || value->type != FLOW_TYPE_STRING) return nullptr;
    return value->string_value.c_str();
}

FlowResult flow_value_get_bool(FlowValue* value, int* out)
{
    if (!value || !out) return FLOW_ERROR_INVALID_ARGS;
    if (value->type != FLOW_TYPE_BOOL) return FLOW_ERROR_TYPE_MISMATCH;
    *out = value->bool_value;
    return FLOW_OK;
}


FlowResult flow_function_call(FlowRuntime* runtime, FlowFunction* function,
                              FlowValue** args, int arg_count, FlowValue** result)
{
    if (!runtime || !function || !result) return FLOW_ERROR_INVALID_ARGS;

    FlowModule* module = function->module;
    if (!module || !module->codegen)
    {
        runtime->lastError = "Invalid module or code generator";
        return FLOW_ERROR_INVALID_ARGS;
    }

    try
    {
        llvm::Module* llvmModule = module->codegen->getModule();
        if (!llvmModule)
        {
            runtime->lastError = "LLVM module not available";
            return FLOW_ERROR_INVALID_ARGS;
        }


        if (!module->engine)
        {
            std::string errorStr;


            std::unique_ptr<llvm::Module> moduleClone = llvm::CloneModule(*llvmModule);

            llvm::EngineBuilder builder(std::move(moduleClone));
            builder.setErrorStr(&errorStr);
            builder.setEngineKind(llvm::EngineKind::JIT);

            llvm::ExecutionEngine* engine = builder.create();
            if (!engine)
            {
                runtime->lastError = "Failed to create execution engine: " + errorStr;
                return FLOW_ERROR_INVALID_ARGS;
            }

            module->engine.reset(engine);
            module->engine->finalizeObject();
        }


        llvm::Function* llvmFunc = module->engine->FindFunctionNamed(function->name.c_str());
        if (!llvmFunc)
        {
            runtime->lastError = "Function not found in LLVM module: " + function->name;
            return FLOW_ERROR_NOT_FOUND;
        }


        uint64_t funcAddr = module->engine->getFunctionAddress(function->name);
        if (!funcAddr)
        {
            runtime->lastError = "Failed to get function address";
            return FLOW_ERROR_NOT_FOUND;
        }


        llvm::Type* returnType = llvmFunc->getReturnType();


        if (returnType->isIntegerTy())
        {
            if (returnType->getIntegerBitWidth() == 1)
            {
                if (arg_count == 2)
                {
                    typedef bool (*FuncType)(int64_t, int64_t);
                    FuncType func = reinterpret_cast<FuncType>(funcAddr);
                    bool result_value = func(args[0]->int_value, args[1]->int_value);
                    *result = flow_value_new_bool(runtime, result_value ? 1 : 0);
                }
                else
                {
                    runtime->lastError = "Unsupported number of arguments for bool return type";
                    return FLOW_ERROR_TYPE_MISMATCH;
                }
            }
            else
            {
                if (arg_count == 2)
                {
                    typedef int64_t (*FuncType)(int64_t, int64_t);
                    FuncType func = reinterpret_cast<FuncType>(funcAddr);
                    int64_t result_value = func(args[0]->int_value, args[1]->int_value);
                    *result = flow_value_new_int(runtime, result_value);
                }
                else if (arg_count == 0)
                {
                    typedef int64_t (*FuncType)();
                    FuncType func = reinterpret_cast<FuncType>(funcAddr);
                    int64_t result_value = func();
                    *result = flow_value_new_int(runtime, result_value);
                }
                else
                {
                    runtime->lastError = "Unsupported number of arguments for int return type";
                    return FLOW_ERROR_TYPE_MISMATCH;
                }
            }
        }
        else if (returnType->isDoubleTy())
        {
            if (arg_count == 2)
            {
                typedef double (*FuncType)(double, double);
                FuncType func = reinterpret_cast<FuncType>(funcAddr);
                double result_value = func(args[0]->float_value, args[1]->float_value);
                *result = flow_value_new_float(runtime, result_value);
            }
            else
            {
                runtime->lastError = "Unsupported number of arguments for float return type";
                return FLOW_ERROR_TYPE_MISMATCH;
            }
        }
        else if (returnType->isPointerTy())
        {
            if (arg_count == 2)
            {
                typedef const char* (*FuncType)(const char*, const char*);
                FuncType func = reinterpret_cast<FuncType>(funcAddr);
                const char* result_value = func(args[0]->string_value.c_str(), args[1]->string_value.c_str());
                *result = flow_value_new_string(runtime, result_value ? result_value : "");
            }
            else
            {
                runtime->lastError = "Unsupported number of arguments for string return type";
                return FLOW_ERROR_TYPE_MISMATCH;
            }
        }
        else if (returnType->isVoidTy())
        {
            *result = flow_value_new_null(runtime);
        }
        else
        {
            runtime->lastError = "Unsupported return type";
            return FLOW_ERROR_TYPE_MISMATCH;
        }

        return FLOW_OK;
    }
    catch (const std::exception& e)
    {
        runtime->lastError = std::string("Execution error: ") + e.what();
        return FLOW_ERROR_INVALID_ARGS;
    }
    catch (...)
    {
        runtime->lastError = "Unknown execution error";
        return FLOW_ERROR_INVALID_ARGS;
    }
}

FlowResult flow_call(FlowRuntime* runtime, FlowModule* module, const char* function_name,
                     FlowValue** args, int arg_count, FlowValue** result)
{
    if (!runtime || !module || !function_name) return FLOW_ERROR_INVALID_ARGS;

    FlowFunction* func = flow_module_get_function(module, function_name);
    if (!func) return FLOW_ERROR_NOT_FOUND;

    return flow_function_call(runtime, func, args, arg_count, result);
}