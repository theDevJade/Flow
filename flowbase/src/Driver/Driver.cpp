#include "../../include/Driver/Driver.h"
#include "../../include/Driver/MultiFileBuilder.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Codegen/CodeGenerator.h"
#include "../../include/Common/ErrorReporter.h"
#include <fstream>
#include <sstream>
#include <iostream>
#include <cstdlib>
#include <filesystem>

namespace flow {
    void Driver::reportError(const std::string &message) {
        std::cerr << "Error: " << message << std::endl;
        errors.push_back(message);
    }

    std::string Driver::readFile(const std::string &filename) {
        std::ifstream file(filename);
        if (!file.is_open()) {
            reportError("Could not open file: " + filename);
            return "";
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        return buffer.str();
    }

    int Driver::compile() {
        // Check if multi-file compilation is needed
        if (options.multiFile && !options.objectOnly) {
            // Check if the input file has any imports
            std::ifstream file(options.inputFile);
            if (file.is_open()) {
                std::string line;
                bool hasImports = false;
                while (std::getline(file, line)) {
                    if (line.find("import") != std::string::npos && line.find("\"") != std::string::npos) {
                        hasImports = true;
                        break;
                    }
                }
                file.close();

                if (hasImports) {
                    // Use multi-file builder
                    MultiFileBuilder builder(options.inputFile, options.outputFile, options.verbose);
                    return builder.build() ? 0 : 1;
                }
            }
        }

        if (options.verbose) {
            std::cout << "Flow Compiler v0.1.0" << std::endl;
            std::cout << "Compiling: " << options.inputFile << std::endl;
        }


        std::string source = readFile(options.inputFile);
        if (source.empty() && hasErrors()) {
            printErrors();
            return 1;
        }


        ErrorReporter::instance().loadSourceFile(options.inputFile);

        // Lexical analysis
        if (options.verbose) {
            std::cout << "Phase 1: Lexical Analysis" << std::endl;
        }

        Lexer lexer(source, options.inputFile);
        std::vector<Token> tokens = lexer.tokenize();

        if (tokens.empty() || tokens.back().type == TokenType::INVALID) {
            reportError("Lexical analysis failed");
            printErrors();
            return 1;
        }

        if (options.verbose) {
            std::cout << "  Tokens generated: " << tokens.size() << std::endl;
        }

        // Parsing
        if (options.verbose) {
            std::cout << "Phase 2: Parsing" << std::endl;
        }

        Parser parser(tokens);
        auto program = parser.parse();

        if (!program) {
            reportError("Parsing failed");
            printErrors();
            return 1;
        }

        if (options.emitAST) {
            std::cout << "AST dump not yet implemented" << std::endl;
        }

        // Semantic analysis
        if (options.verbose) {
            std::cout << "Phase 3: Semantic Analysis" << std::endl;
        }

        SemanticAnalyzer analyzer;
        analyzer.setCurrentFile(options.inputFile);
        analyzer.setLibraryPaths(options.libraryPaths);
        analyzer.analyze(program);

        if (analyzer.hasErrors()) {
            reportError("Semantic analysis failed");
            for (const auto &err: analyzer.getErrors()) {
                std::cerr << "  " << err << std::endl;
            }
            return 1;
        }

        // Code generation
        if (options.verbose) {
            std::cout << "Phase 4: Code Generation" << std::endl;
        }

        CodeGenerator codegen(options.outputFile);
        codegen.setLibraryPaths(options.libraryPaths);
        codegen.generate(program);

        if (options.emitLLVM) {
            std::string llvmFile = options.outputFile + ".ll";
            codegen.writeIRToFile(llvmFile);
            if (options.verbose) {
                std::cout << "  LLVM IR written to: " << llvmFile << std::endl;
            }
        }

        // Generate object file
        if (options.verbose) {
            std::cout << "Phase 5: Object File Generation" << std::endl;
        }

        std::string objectFile = options.outputFile + ".o";
        codegen.compileToObject(objectFile);

        if (options.verbose) {
            std::cout << "  Object file written to: " << objectFile << std::endl;
        }

        // Skip linking if object-only mode
        if (options.objectOnly) {
            if (options.verbose) {
                std::cout << "  Object file kept: " << objectFile << std::endl;
                std::cout << "Compilation successful (object-only mode)" << std::endl;
            }
            return 0;
        }

        // Link to create executable
        if (options.verbose) {
            std::cout << "Phase 6: Linking" << std::endl;
        }

        // Get list of libraries to link
        auto linkedLibs = codegen.getLinkedLibraries();
        std::string libFlags = "";
        for (const auto &lib: linkedLibs) {
            // Strip "lib" prefix if present (since -l adds it automatically)
            std::string libName = lib;
            if (libName.substr(0, 3) == "lib") {
                libName = libName.substr(3);
            }
            libFlags += " -L. -L/tmp/ffi_test -L/usr/local/lib -l" + libName;
        }
        
        // Add library paths from options
        for (const auto &libPath: options.libraryPaths) {
            libFlags += " -L" + libPath;
        }
        
        // Add object files from options
        std::string objFiles = "";
        for (const auto &obj: options.objectFiles) {
            objFiles += " " + obj;
        }

        // Use clang/gcc to link the object file
        std::string linkCmd;
#ifdef __APPLE__
        linkCmd = "clang++ -o " + options.outputFile + " " + objectFile + objFiles + libFlags;
#else
        linkCmd = "g++ -o " + options.outputFile + " " + objectFile + objFiles + libFlags;
#endif

        int linkResult = system(linkCmd.c_str());
        if (linkResult != 0) {
            reportError("Linking failed");
            return 1;
        }

        // Clean up object file
        remove(objectFile.c_str());

        if (options.verbose) {
            std::cout << "  Executable written to: " << options.outputFile << std::endl;
            std::cout << "Compilation successful" << std::endl;
        }

        return 0;
    }

    void Driver::printErrors() {
        for (const auto &error: errors) {
            std::cerr << error << std::endl;
        }
    }
} // namespace flow