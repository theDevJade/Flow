/**
 * Flow Reflection Manager
 * 
 * Unified reflection interface for LSP, Parser, and runtime to discover
 * and introspect Flow modules and foreign language functions.
 * 
 * This enables:
 * - LSP autocomplete for foreign (Go/Python/etc) functions
 * - Parser validation of foreign function calls
 * - Runtime discovery of available functions
 * - Bidirectional reflection between Flow and other languages
 */

#ifndef FLOW_REFLECTION_MANAGER_H
#define FLOW_REFLECTION_MANAGER_H

#include <string>
#include <vector>
#include <map>
#include <memory>
#include "../AST/AST.h"
#include "../Embedding/FlowAPI.h"

namespace flow {

// ============================================================================
// REFLECTION DATA STRUCTURES
// ============================================================================

/**
 * Function signature information
 */
struct FunctionSignature {
    std::string name;
    std::string returnType;
    std::vector<std::pair<std::string, std::string>> parameters; // (name, type)
    std::string documentation;
    std::string sourceLanguage; // "flow", "go", "python", etc.
    std::string sourceModule;
    
    FunctionSignature() : sourceLanguage("flow") {}
    
    /**
     * Format signature as string: "func_name(param1: type1, param2: type2) -> return_type"
     */
    std::string toString() const {
        std::string result = name + "(";
        for (size_t i = 0; i < parameters.size(); i++) {
            if (i > 0) result += ", ";
            result += parameters[i].first + ": " + parameters[i].second;
        }
        result += ") -> " + returnType;
        return result;
    }
    
    /**
     * Format as LSP hover markdown
     */
    std::string toMarkdown() const {
        std::string result = "**" + name + "**\n\n";
        result += "```" + sourceLanguage + "\n";
        result += toString();
        result += "\n```\n\n";
        if (!documentation.empty()) {
            result += documentation + "\n\n";
        }
        if (sourceLanguage != "flow") {
            result += "Foreign function from **" + sourceLanguage + "**";
            if (!sourceModule.empty()) {
                result += " module `" + sourceModule + "`";
            }
        }
        return result;
    }
};

/**
 * Module information
 */
struct ModuleInfo {
    std::string name;
    std::string language; // "flow", "go", "python", etc.
    std::string path;
    std::map<std::string, FunctionSignature> functions;
    bool isLoaded;
    
    ModuleInfo() : language("flow"), isLoaded(false) {}
};

// ============================================================================
// REFLECTION MANAGER
// ============================================================================

/**
 * Central reflection manager for Flow
 * Singleton pattern - accessible from LSP, Parser, and Runtime
 */
class ReflectionManager {
private:
    // Registered modules (both Flow and foreign)
    std::map<std::string, ModuleInfo> modules;
    
    // Foreign module registry: adapter -> module -> functions
    std::map<std::string, std::map<std::string, std::vector<FunctionSignature>>> foreignModules;
    
    static ReflectionManager* instance;
    
    ReflectionManager() {}

public:
    /**
     * Get singleton instance
     */
    static ReflectionManager& getInstance() {
        if (!instance) {
            instance = new ReflectionManager();
        }
        return *instance;
    }
    
    // ========================================================================
    // FLOW MODULE REFLECTION
    // ========================================================================
    
    /**
     * Register a Flow module for reflection
     */
    void registerFlowModule(const std::string& name, FlowModule* module);
    
    /**
     * Register a Flow module from AST (for LSP use)
     */
    void registerFlowModuleFromAST(const std::string& name, std::shared_ptr<Program> ast);
    
    /**
     * Get signatures for all functions in a Flow module
     */
    std::vector<FunctionSignature> getFlowModuleFunctions(const std::string& name);
    
    /**
     * Get signature for a specific Flow function
     */
    FunctionSignature getFlowFunctionSignature(const std::string& moduleName, const std::string& functionName);
    
    // ========================================================================
    // FOREIGN MODULE REFLECTION
    // ========================================================================
    
    /**
     * Register a foreign module (Go, Python, JavaScript, etc.)
     * 
     * @param adapter Language adapter ("go", "python", "javascript", etc.)
     * @param moduleName Module name ("os", "math", etc.)
     * @param functions List of function signatures available in the module
     */
    void registerForeignModule(
        const std::string& adapter,
        const std::string& moduleName,
        const std::vector<FunctionSignature>& functions
    );
    
    /**
     * Get all functions from a foreign module
     */
    std::vector<FunctionSignature> getForeignModuleFunctions(
        const std::string& adapter,
        const std::string& moduleName
    );
    
    /**
     * Get signature for a specific foreign function
     */
    FunctionSignature getForeignFunctionSignature(
        const std::string& adapter,
        const std::string& moduleName,
        const std::string& functionName
    );
    
    /**
     * Check if a foreign module is available
     */
    bool hasForeignModule(const std::string& adapter, const std::string& moduleName);
    
    /**
     * Get all registered foreign modules for an adapter
     */
    std::vector<std::string> getForeignModules(const std::string& adapter);
    
    // ========================================================================
    // UNIFIED QUERIES
    // ========================================================================
    
    /**
     * Search for a function across all registered modules
     * Returns all matching functions (could be multiple with same name)
     */
    std::vector<FunctionSignature> searchFunction(const std::string& functionName);
    
    /**
     * Get all available functions for LSP autocomplete
     */
    std::vector<FunctionSignature> getAllAvailableFunctions();
    
    /**
     * Validate a function call against registered signatures
     * Returns true if signature matches, false otherwise
     */
    bool validateFunctionCall(
        const std::string& functionName,
        const std::vector<std::string>& argTypes,
        std::string& errorMessage
    );
    
    // ========================================================================
    // DISCOVERY HELPERS
    // ========================================================================
    
    /**
     * Auto-discover Go packages at compile time
     * This would scan Go import paths and register available functions
     */
    bool discoverGoPackage(const std::string& packagePath);
    
    /**
     * Auto-discover Python modules
     * This would use Python introspection to register available functions
     */
    bool discoverPythonModule(const std::string& modulePath);
    
    /**
     * Clear all registered modules (for testing)
     */
    void clear() {
        modules.clear();
        foreignModules.clear();
    }
};

} // namespace flow

#endif // FLOW_REFLECTION_MANAGER_H

