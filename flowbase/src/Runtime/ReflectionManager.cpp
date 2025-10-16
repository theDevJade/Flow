/**
 * Flow Reflection Manager Implementation
 */

#include "../../include/Runtime/ReflectionManager.h"
#include <algorithm>

namespace flow {

// Static instance
ReflectionManager* ReflectionManager::instance = nullptr;

// ============================================================================
// FLOW MODULE REFLECTION
// ============================================================================

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
    if (!ast) return;
    
    ModuleInfo info;
    info.name = name;
    info.language = "flow";
    info.isLoaded = true;
    
    // Extract function signatures from AST
    for (const auto& decl : ast->declarations) {
        if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl)) {
            FunctionSignature sig;
            sig.name = funcDecl->name;
            sig.sourceLanguage = "flow";
            sig.sourceModule = name;
            
            // Return type
            sig.returnType = funcDecl->returnType ? funcDecl->returnType->toString() : "void";
            
            // Parameters
            for (const auto& param : funcDecl->parameters) {
                sig.parameters.push_back({
                    param.name,
                    param.type ? param.type->toString() : "unknown"
                });
            }
            
            info.functions[funcDecl->name] = sig;
        }
    }
    
    modules[name] = info;
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

