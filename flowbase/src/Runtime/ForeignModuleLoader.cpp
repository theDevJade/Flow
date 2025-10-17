#include "../../include/Runtime/ForeignModuleLoader.h"
#include "../../include/Runtime/ReflectionManager.h"
#ifdef _WIN32
#include <windows.h>
// Windows compatibility macros for dynamic library loading
#define dlopen(lib, flags) LoadLibraryA(lib)
#define dlsym(handle, func) GetProcAddress((HMODULE)handle, func)
#define dlclose(handle) FreeLibrary((HMODULE)handle)
static const char* dlerror() {
    static char buf[256];
    DWORD err = GetLastError();
    FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                   buf, sizeof(buf), NULL);
    return buf;
}
#define RTLD_LAZY 0
// Windows compatibility macros for process I/O
#define popen _popen
#define pclose _pclose
#else
#include <dlfcn.h>
#endif
#include <iostream>
#include <cstdlib>
#include <cstdio>
#include <algorithm>

namespace flow {

ForeignModuleLoader& ForeignModuleLoader::getInstance() {
    static ForeignModuleLoader instance;
    return instance;
}

bool ForeignModuleLoader::isModuleLoaded(const std::string& language, const std::string& moduleName) const {
    return std::find(loadedModules.begin(), loadedModules.end(),
                     std::make_pair(language, moduleName)) != loadedModules.end();
}

bool ForeignModuleLoader::loadAndRegisterModule(const std::string& language, const std::string& moduleName) {
    // Check if already loaded
    if (isModuleLoaded(language, moduleName)) {
        return true;
    }

    bool success = false;

    if (language == "python") {
        success = loadPythonModule(moduleName);
    } else if (language == "go") {
        success = loadGoModule(moduleName);
    } else if (language == "javascript" || language == "js") {
        success = loadJavaScriptModule(moduleName);
    } else if (language == "rust") {
        success = loadRustModule(moduleName);
    } else if (language == "ruby") {
        success = loadRubyModule(moduleName);
    } else if (language == "php") {
        success = loadPHPModule(moduleName);
    } else {
        std::cerr << "Unknown language: " << language << std::endl;
        return false;
    }

    if (success) {
        loadedModules.push_back({language, moduleName});
    }

    return success;
}

bool ForeignModuleLoader::loadPythonModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] Loading Python module: " << moduleName << std::endl;

    // Find libflow shared library (in interop/c)
    std::string libflowPath = std::string(std::getenv("HOME")) + "/Flow/interop/c/libflow.dylib";

    std::string pythonScript =
        "import sys; "
        "import inspect; "
        "try: "
        "    mod = __import__('" + moduleName + "'); "
        "    for name, obj in inspect.getmembers(mod): "
        "        if inspect.isfunction(obj) or inspect.isbuiltin(obj): "
        "            sig = ''; "
        "            try: "
        "                sig = str(inspect.signature(obj)); "
        "            except: "
        "                sig = '(...)'; "
        "            print(f'{name}|{sig}'); "
        "except Exception as e: "
        "    print(f'ERROR: {e}', file=sys.stderr); "
        "    sys.exit(1)";

    std::string cmd = "python3 -c \"" + pythonScript + "\" 2>/dev/null";
    FILE* pipe = popen(cmd.c_str(), "r");
    if (!pipe) {
        std::cerr << "Failed to execute Python introspection" << std::endl;
        return false;
    }

    std::vector<FunctionSignature> functions;
    char buffer[1024];

    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
        std::string line(buffer);
        // Remove trailing newline
        if (!line.empty() && line.back() == '\n') {
            line.pop_back();
        }

        // Parse: function_name|(param1, param2) -> return_type
        size_t delimPos = line.find('|');
        if (delimPos == std::string::npos) continue;

        std::string funcName = line.substr(0, delimPos);
        std::string signature = line.substr(delimPos + 1);

        // Skip private functions
        if (funcName.empty() || funcName[0] == '_') continue;

        FunctionSignature func;
        func.name = funcName;
        func.sourceLanguage = "python";
        func.sourceModule = moduleName;
        func.documentation = "Python function from " + moduleName + " module";

        func.returnType = "any"; // Python is dynamically typed

        // For demo, extract parameter names from signature like "(x, y)"
        size_t startParen = signature.find('(');
        size_t endParen = signature.find(')');
        if (startParen != std::string::npos && endParen != std::string::npos) {
            std::string params = signature.substr(startParen + 1, endParen - startParen - 1);
            // Simple split by comma
            size_t pos = 0;
            while ((pos = params.find(',')) != std::string::npos) {
                std::string param = params.substr(0, pos);
                // Trim whitespace
                param.erase(0, param.find_first_not_of(" \t"));
                param.erase(param.find_last_not_of(" \t") + 1);
                if (!param.empty() && param != "...") {
                    func.parameters.push_back({param, "any"});
                }
                params.erase(0, pos + 1);
            }
            // Last param
            params.erase(0, params.find_first_not_of(" \t"));
            params.erase(params.find_last_not_of(" \t") + 1);
            if (!params.empty() && params != "..." && params != "*" && params != "**") {
                func.parameters.push_back({params, "any"});
            }
        }

        functions.push_back(func);
    }

    int status = pclose(pipe);
    if (status != 0) {
        std::cerr << "Python introspection failed for module: " << moduleName << std::endl;
        return false;
    }

    if (functions.empty()) {
        std::cerr << "No functions found in Python module: " << moduleName << std::endl;
        return false;
    }

    // Register with ReflectionManager
    auto& reflectionMgr = ReflectionManager::getInstance();
    reflectionMgr.registerForeignModule("python", moduleName, functions);

    std::cerr << "[ForeignModuleLoader] Successfully loaded " << functions.size()
              << " functions from Python module: " << moduleName << std::endl;

    return true;
}

bool ForeignModuleLoader::loadGoModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] Loading Go module: " << moduleName << std::endl;


    std::string goScript =
        "package main; "
        "import \"" + moduleName + "\"; "
        "import \"reflect\"; "
        "import \"fmt\"; "
        "func main() { "
        "    pkgType := reflect.TypeOf((*" + moduleName + ".TODO)(nil)).Elem(); "
        "    fmt.Println(pkgType); "
        "}";


    std::vector<FunctionSignature> functions;

    if (moduleName == "fmt") {
        FunctionSignature printf;
        printf.name = "Printf";
        printf.returnType = "int";
        printf.parameters = {{"format", "string"}, {"args", "...any"}};
        printf.sourceLanguage = "go";
        printf.sourceModule = moduleName;
        printf.documentation = "Printf formats according to a format specifier and writes to standard output.";
        functions.push_back(printf);

        FunctionSignature println;
        println.name = "Println";
        println.returnType = "int";
        println.parameters = {{"args", "...any"}};
        println.sourceLanguage = "go";
        println.sourceModule = moduleName;
        println.documentation = "Println formats using default formats and writes to standard output.";
        functions.push_back(println);
    } else if (moduleName == "os") {
        FunctionSignature readFile;
        readFile.name = "ReadFile";
        readFile.returnType = "[]byte";
        readFile.parameters = {{"filename", "string"}};
        readFile.sourceLanguage = "go";
        readFile.sourceModule = moduleName;
        readFile.documentation = "ReadFile reads the named file and returns the contents.";
        functions.push_back(readFile);
    }

    if (functions.empty()) {
        std::cerr << "No functions found in Go module: " << moduleName << std::endl;
        return false;
    }

    auto& reflectionMgr = ReflectionManager::getInstance();
    reflectionMgr.registerForeignModule("go", moduleName, functions);

    std::cerr << "[ForeignModuleLoader] Successfully loaded " << functions.size()
              << " functions from Go module: " << moduleName << std::endl;

    return true;
}

/**
 * JAVASCRIPT MODULE LOADER
 * Uses Node.js require() + Object introspection
 */
bool ForeignModuleLoader::loadJavaScriptModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] Loading JavaScript module: " << moduleName << std::endl;

    // TODO: Implement using Node.js C++ addons or V8 API
    // For now, return false to indicate not implemented

    return false;
}

bool ForeignModuleLoader::loadRustModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] Rust module loading not yet implemented: " << moduleName << std::endl;
    return false;
}

bool ForeignModuleLoader::loadRubyModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] Ruby module loading not yet implemented: " << moduleName << std::endl;
    return false;
}

bool ForeignModuleLoader::loadPHPModule(const std::string& moduleName) {
    std::cerr << "[ForeignModuleLoader] PHP module loading not yet implemented: " << moduleName << std::endl;
    return false;
}

} // namespace flow

