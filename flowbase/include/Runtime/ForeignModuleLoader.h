#ifndef FLOW_FOREIGN_MODULE_LOADER_H
#define FLOW_FOREIGN_MODULE_LOADER_H

#include <string>
#include <vector>
#include "ReflectionManager.h"

namespace flow {

/**
 * ForeignModuleLoader - Dynamically loads foreign modules and introspects them
 * using each language's native reflection capabilities.
 * 
 * This is the real implementation - not hardcoded functions!
 */
class ForeignModuleLoader {
public:
    static ForeignModuleLoader& getInstance();
    
    /**
     * Dynamically load a foreign module and register all its functions
     * Returns true if successful, false otherwise
     */
    bool loadAndRegisterModule(const std::string& language, const std::string& moduleName);
    
    /**
     * Check if a module is already loaded
     */
    bool isModuleLoaded(const std::string& language, const std::string& moduleName) const;
    
private:
    ForeignModuleLoader() = default;
    
    // Language-specific loaders that use actual introspection
    bool loadPythonModule(const std::string& moduleName);
    bool loadGoModule(const std::string& moduleName);
    bool loadJavaScriptModule(const std::string& moduleName);
    bool loadRustModule(const std::string& moduleName);
    bool loadRubyModule(const std::string& moduleName);
    bool loadPHPModule(const std::string& moduleName);
    
    // Track loaded modules
    std::vector<std::pair<std::string, std::string>> loadedModules; // (language, module)
};

} // namespace flow

#endif // FLOW_FOREIGN_MODULE_LOADER_H

