#ifndef FLOW_MULTIFILE_BUILDER_H
#define FLOW_MULTIFILE_BUILDER_H

#include <string>
#include <vector>
#include <set>
#include <map>

namespace flow {

struct ModuleInfo {
    std::string sourcePath;
    std::string objectPath;
    size_t sourceSize;
    size_t objectSize;
    bool compiled;
};

class MultiFileBuilder {
private:
    std::string mainFile;
    std::string outputFile;
    std::string buildDir;
    bool verbose;
    
    std::map<std::string, ModuleInfo> modules;
    std::set<std::string> processedModules;
    
    void discoverImports(const std::string& filePath);
    std::string resolveImportPath(const std::string& importPath, const std::string& currentDir);
    bool compileModule(const std::string& modulePath);
    bool linkModules();
    
    void printBuildHeader();
    void printModuleProgress(int current, int total, const std::string& moduleName);
    void printLinkingInfo(const std::vector<std::string>& objectFiles);
    void printBuildSummary();
    
public:
    MultiFileBuilder(const std::string& mainFile, const std::string& outputFile, bool verbose = false);
    
    bool build();
    const std::map<std::string, ModuleInfo>& getModules() const { return modules; }
};

} // namespace flow

#endif // FLOW_MULTIFILE_BUILDER_H

