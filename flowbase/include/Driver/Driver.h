#ifndef FLOW_DRIVER_H
#define FLOW_DRIVER_H

#include <string>
#include <vector>

namespace flow {

struct CompilerOptions {
    std::string inputFile;
    std::string outputFile;
    bool emitLLVM;
    bool emitAST;
    bool optimize;
    int optimizationLevel;
    bool verbose;
    bool objectOnly;      // Compile to .o only, don't link
    bool multiFile;       // Enable multi-file compilation mode
    
    CompilerOptions()
        : inputFile(""),
          outputFile("out"),
          emitLLVM(false),
          emitAST(false),
          optimize(false),
          optimizationLevel(0),
          verbose(false),
          objectOnly(false),
          multiFile(true) {}  // Enable by default
};

class Driver {
private:
    CompilerOptions options;
    std::vector<std::string> errors;
    
    void reportError(const std::string& message);
    std::string readFile(const std::string& filename);
    
public:
    Driver(const CompilerOptions& opts) : options(opts) {}
    

    int compile();
    
    void printErrors();
    bool hasErrors() const { return !errors.empty(); }
};

} // namespace flow

#endif // FLOW_DRIVER_H

