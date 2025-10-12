#include "include/Driver/Driver.h"
#include <iostream>
#include <cstring>

void printUsage(const char* programName) {
    std::cout << "Flow Compiler v0.1.0\n"
              << "Usage: " << programName << " [options] <input-file>\n"
              << "\nOptions:\n"
              << "  -o <file>        Write output to <file>\n"
              << "  --emit-llvm      Emit LLVM IR (.ll file)\n"
              << "  --emit-ast       Print AST\n"
              << "  -O<level>        Optimization level (0-3)\n"
              << "  -v, --verbose    Verbose output\n"
              << "  -h, --help       Display this help message\n"
              << std::endl;
}

int main(int argc, char** argv) {
    flow::CompilerOptions options;
    
    // Parse command-line arguments
    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];
        
        if (arg == "-h" || arg == "--help") {
            printUsage(argv[0]);
            return 0;
        } else if (arg == "-v" || arg == "--verbose") {
            options.verbose = true;
        } else if (arg == "--emit-llvm") {
            options.emitLLVM = true;
        } else if (arg == "--emit-ast") {
            options.emitAST = true;
        } else if (arg == "-o") {
            if (i + 1 < argc) {
                options.outputFile = argv[++i];
            } else {
                std::cerr << "Error: -o requires an argument" << std::endl;
                return 1;
            }
        } else if (arg.substr(0, 2) == "-O") {
            options.optimize = true;
            if (arg.length() > 2) {
                options.optimizationLevel = arg[2] - '0';
            }
        } else if (arg[0] == '-') {
            std::cerr << "Error: Unknown option: " << arg << std::endl;
            printUsage(argv[0]);
            return 1;
        } else {
            // Input file
            options.inputFile = arg;
        }
    }
    
    // Check if input file was provided
    if (options.inputFile.empty()) {
        std::cerr << "Error: No input file specified" << std::endl;
        printUsage(argv[0]);
        return 1;
    }
    
    // Create driver and compile
    flow::Driver driver(options);
    return driver.compile();
}
