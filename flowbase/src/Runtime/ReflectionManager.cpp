/**
 * Flow Reflection Manager Implementation
 */

#include "../../include/Runtime/ReflectionManager.h"
#include <algorithm>
#include <iostream>

namespace flow {

// Static instance
ReflectionManager* ReflectionManager::instance = nullptr;


void ReflectionManager::registerFlowModule(const std::string& name, FlowModule* module)
{
    if (!module) return;

    ModuleInfo info;
    info.name = name;
    info.language = "flow";
    info.isLoaded = true;

    // Get function count
    int funcCount = flow_module_get_function_count(module);

    // Extract each function's signature
    for (int i = 0; i < funcCount; i++) {
        const char* funcName = flow_module_get_function_name(module, i);
        if (!funcName) continue;

        FlowFunction* func = flow_module_get_function(module, funcName);
        if (!func) continue;

        FunctionSignature sig;
        sig.name = funcName;
        sig.sourceLanguage = "flow";
        sig.sourceModule = name;

        // Get return type
        const char* retType = flow_function_get_return_type(func);
        sig.returnType = retType ? retType : "void";

        // Get parameters
        int paramCount = flow_function_get_param_count(func);
        for (int j = 0; j < paramCount; j++) {
            const char* paramName = flow_function_get_param_name(func, j);
            const char* paramType = flow_function_get_param_type(func, j);
            sig.parameters.push_back({
                paramName ? paramName : "arg" + std::to_string(j),
                paramType ? paramType : "unknown"
            });
        }

        info.functions[funcName] = sig;
    }

    modules[name] = info;
}

void ReflectionManager::registerFlowModuleFromAST(const std::string& name, std::shared_ptr<Program> ast)
{
    std::cerr << "ReflectionManager::registerFlowModuleFromAST called for: " << name << std::endl;
    if (!ast) {
        std::cerr << "  AST is null, returning" << std::endl;
        return;
    }

    // Make a local copy of the name to avoid any reference issues
    std::string moduleName = name;

    std::cerr << "  Creating ModuleInfo on HEAP..." << std::endl;
    auto info = std::make_unique<ModuleInfo>();
    std::cerr << "  ModuleInfo created" << std::endl;
    std::cerr << "  Setting ModuleInfo fields..." << std::endl;
    info->name = moduleName;
    std::cerr << "    name set" << std::endl;
    info->language = "flow";
    std::cerr << "    language set" << std::endl;
    info->isLoaded = true;
    std::cerr << "    isLoaded set" << std::endl;
    std::cerr << "  Checking functions map..." << std::endl;
    std::cerr << "  functions.size() = " << info->functions.size() << std::endl;

    std::cerr << "  Processing " << ast->declarations.size() << " declarations..." << std::endl;
    // Extract function signatures from AST
    int declIdx = 0;
    for (const auto& decl : ast->declarations) {
        std::cerr << "    Declaration " << declIdx << ": ";

        if (!decl) {
            std::cerr << "NULL (skipped)" << std::endl;
            declIdx++;
            continue;
        }

        if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl)) {
            std::cerr << "FunctionDecl";

            // Check if the function declaration has a valid name
            if (funcDecl->name.empty()) {
                std::cerr << " (EMPTY NAME - skipped)" << std::endl;
                declIdx++;
                continue;
            }

            std::cerr << " '" << funcDecl->name << "'" << std::endl;

            std::cerr << "      Creating FunctionSignature on HEAP..." << std::endl;
            auto sig = std::make_unique<FunctionSignature>();
            std::cerr << "      FunctionSignature created, setting fields..." << std::endl;

            sig->name = funcDecl->name;
            std::cerr << "      Set name" << std::endl;
            sig->sourceLanguage = "flow";
            std::cerr << "      Set sourceLanguage" << std::endl;
            sig->sourceModule = moduleName;  // Use local copy instead of reference
            std::cerr << "      Set sourceModule" << std::endl;

            std::cerr << "      Getting return type..." << std::endl;
            // Return type
            std::string retType = "void";
            if (funcDecl->returnType) {
                std::cerr << "      returnType exists, calling toString()..." << std::endl;
                try {
                    retType = funcDecl->returnType->toString();
                    std::cerr << "      toString() returned: '" << retType << "'" << std::endl;
                } catch (...) {
                    std::cerr << "      EXCEPTION in toString()!" << std::endl;
                    retType = "error";
                }
            }
            sig->returnType = retType;
            std::cerr << "      Assigned return type: " << sig->returnType << std::endl;

            std::cerr << "      Processing " << funcDecl->parameters.size() << " parameters..." << std::endl;
            // Parameters
            for (const auto& param : funcDecl->parameters) {
                std::cerr << "        Param: " << param.name << std::endl;
                sig->parameters.push_back({
                    param.name,
                    param.type ? param.type->toString() : "unknown"
                });
            }

            std::cerr << "      Adding to module with key: 'testMath'" << std::endl;
            std::cerr << "      Checking sig object integrity..." << std::endl;
            std::cerr << "      sig->name = '" << sig->name << "'" << std::endl;
            std::cerr << "      sig->returnType = '" << sig->returnType << "'" << std::endl;
            std::cerr << "      sig->parameters.size() = " << sig->parameters.size() << std::endl;
            std::cerr << "      About to insert into map..." << std::endl;
            std::cerr << "      Current map size: " << info->functions.size() << std::endl;

            std::cerr << "      TEST: Inserting with simple key 'test'..." << std::endl;
            try {
                std::cerr << "      TEST: Creating minimal FunctionSignature..." << std::endl;
                FunctionSignature testSig;                std::cerr << "      TEST: Setting name..." << std::endl;
                testSig.name = std::string("test");
                std::cerr << "      TEST: Name set to: '" << testSig.name << "'" << std::endl;
                std::cerr << "      TEST: Setting returnType..." << std::endl;
                testSig.returnType = std::string("void");
                std::cerr << "      TEST: returnType set to: '" << testSig.returnType << "'" << std::endl;
                std::cerr << "      TEST: About to emplace into map..." << std::endl;
                info->functions.emplace(std::string("test"), std::move(testSig));
                std::cerr << "      TEST: Simple insertion worked!" << std::endl;
            } catch (const std::exception& e) {
                std::cerr << "      TEST: Simple insertion FAILED with exception: " << e.what() << std::endl;
            } catch (...) {
                std::cerr << "      TEST: Simple insertion FAILED with unknown exception!" << std::endl;
            }

            std::cerr << "      TEST: Exiting early to avoid further crashes" << std::endl;
            return;  // TEMPORARY: Exit early for debugging

            std::cerr << "      Now trying with actual function name..." << std::endl;

            try {
                // Try using emplace, dereferencing the unique_ptr
                std::cerr << "      Using emplace with key: '" << funcDecl->name << "'" << std::endl;
                auto result = info->functions.emplace(funcDecl->name, *sig);
                std::cerr << "      Emplace returned" << std::endl;
                std::cerr << "      Successfully emplaced" << std::endl;
            } catch (const std::exception& e) {
                std::cerr << "      EXCEPTION during insert: " << e.what() << std::endl;
            } catch (...) {
                std::cerr << "      UNKNOWN EXCEPTION during insert" << std::endl;
            }
            std::cerr << "      Done with FunctionDecl" << std::endl;
        } else {
            std::cerr << "Other type (skipped)" << std::endl;
        }
        declIdx++;
    }

    std::cerr << "  Registering module in map..." << std::endl;
    modules[moduleName] = *info;  // Dereference unique_ptr
    std::cerr << "  registerFlowModuleFromAST complete!" << std::endl;
}

std::vector<FunctionSignature> ReflectionManager::getFlowModuleFunctions(const std::string& name)
{
    std::vector<FunctionSignature> result;

    auto it = modules.find(name);
    if (it != modules.end()) {
        for (const auto& pair : it->second.functions) {
            result.push_back(pair.second);
        }
    }

    return result;
}

FunctionSignature ReflectionManager::getFlowFunctionSignature(
    const std::string& moduleName,
    const std::string& functionName)
{
    auto modIt = modules.find(moduleName);
    if (modIt != modules.end()) {
        auto funcIt = modIt->second.functions.find(functionName);
        if (funcIt != modIt->second.functions.end()) {
            return funcIt->second;
        }
    }

    return FunctionSignature(); // Empty signature
}

// ============================================================================
// FOREIGN MODULE REFLECTION
// ============================================================================

void ReflectionManager::registerForeignModule(
    const std::string& adapter,
    const std::string& moduleName,
    const std::vector<FunctionSignature>& functions)
{
    foreignModules[adapter][moduleName] = functions;

    // Also register in modules map for unified queries
    ModuleInfo info;
    info.name = adapter + ":" + moduleName;
    info.language = adapter;
    info.isLoaded = true;

    for (const auto& sig : functions) {
        info.functions[sig.name] = sig;
    }

    modules[info.name] = info;
}

std::vector<FunctionSignature> ReflectionManager::getForeignModuleFunctions(
    const std::string& adapter,
    const std::string& moduleName)
{
    auto adapterIt = foreignModules.find(adapter);
    if (adapterIt != foreignModules.end()) {
        auto moduleIt = adapterIt->second.find(moduleName);
        if (moduleIt != adapterIt->second.end()) {
            return moduleIt->second;
        }
    }

    return {};
}

FunctionSignature ReflectionManager::getForeignFunctionSignature(
    const std::string& adapter,
    const std::string& moduleName,
    const std::string& functionName)
{
    auto funcs = getForeignModuleFunctions(adapter, moduleName);
    for (const auto& sig : funcs) {
        if (sig.name == functionName) {
            return sig;
        }
    }

    return FunctionSignature(); // Empty
}

bool ReflectionManager::hasForeignModule(const std::string& adapter, const std::string& moduleName)
{
    auto adapterIt = foreignModules.find(adapter);
    if (adapterIt != foreignModules.end()) {
        return adapterIt->second.find(moduleName) != adapterIt->second.end();
    }
    return false;
}

std::vector<std::string> ReflectionManager::getForeignModules(const std::string& adapter)
{
    std::vector<std::string> result;

    auto adapterIt = foreignModules.find(adapter);
    if (adapterIt != foreignModules.end()) {
        for (const auto& pair : adapterIt->second) {
            result.push_back(pair.first);
        }
    }

    return result;
}

// ============================================================================
// UNIFIED QUERIES
// ============================================================================

std::vector<FunctionSignature> ReflectionManager::searchFunction(const std::string& functionName)
{
    std::vector<FunctionSignature> results;

    // Search in all modules
    for (const auto& modPair : modules) {
        auto funcIt = modPair.second.functions.find(functionName);
        if (funcIt != modPair.second.functions.end()) {
            results.push_back(funcIt->second);
        }
    }

    return results;
}

std::vector<FunctionSignature> ReflectionManager::getAllAvailableFunctions()
{
    std::vector<FunctionSignature> results;

    for (const auto& modPair : modules) {
        for (const auto& funcPair : modPair.second.functions) {
            results.push_back(funcPair.second);
        }
    }

    return results;
}

bool ReflectionManager::validateFunctionCall(
    const std::string& functionName,
    const std::vector<std::string>& argTypes,
    std::string& errorMessage)
{
    auto signatures = searchFunction(functionName);

    if (signatures.empty()) {
        errorMessage = "Function '" + functionName + "' not found";
        return false;
    }

    // Try to find a matching signature
    for (const auto& sig : signatures) {
        if (sig.parameters.size() != argTypes.size()) {
            continue; // Wrong argument count
        }

        bool matches = true;
        for (size_t i = 0; i < argTypes.size(); i++) {
            // TODO: Implement proper type checking/coercion
            // For now, just check if types match exactly
            if (sig.parameters[i].second != argTypes[i]) {
                matches = false;
                break;
            }
        }

        if (matches) {
            return true; // Found a matching signature
        }
    }

    // No matching signature found
    errorMessage = "No matching signature for '" + functionName + "' with argument types (";
    for (size_t i = 0; i < argTypes.size(); i++) {
        if (i > 0) errorMessage += ", ";
        errorMessage += argTypes[i];
    }
    errorMessage += ")";

    return false;
}

// ============================================================================
// DISCOVERY HELPERS
// ============================================================================

bool ReflectionManager::discoverGoPackage(const std::string& packagePath)
{
    // TODO: Implement Go package discovery
    // This would execute `go doc` or parse Go source files
    // to discover available functions
    (void)packagePath;
    return false;
}

bool ReflectionManager::discoverPythonModule(const std::string& modulePath)
{
    // TODO: Implement Python module discovery
    // This would use Python's inspect module or parse Python source
    // to discover available functions
    (void)modulePath;
    return false;
}

} // namespace flow

