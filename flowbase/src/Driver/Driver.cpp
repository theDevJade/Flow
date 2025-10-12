#include "../../include/Driver/Driver.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Codegen/CodeGenerator.h"
#include "../../include/Common/ErrorReporter.h"
#include <fstream>
#include <sstream>
#include <iostream>
#include <cstdlib>

namespace flow {

void Driver::reportError(const std::string& message) {
    std::cerr << "Error: " << message << std::endl;
    errors.push_back(message);
}

std::string Driver::readFile(const std::string& filename) {
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
    analyzer.analyze(program);
    
    if (analyzer.hasErrors()) {
        reportError("Semantic analysis failed");
        for (const auto& err : analyzer.getErrors()) {
            std::cerr << "  " << err << std::endl;
        }
        return 1;
    }
    
    // Code generation
    if (options.verbose) {
        std::cout << "Phase 4: Code Generation" << std::endl;
    }
    
    CodeGenerator codegen(options.outputFile);
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
    
    // Link to create executable
    if (options.verbose) {
        std::cout << "Phase 6: Linking" << std::endl;
    }
    
    // Use clang/gcc to link the object file
    std::string linkCmd;
    #ifdef __APPLE__
        linkCmd = "clang++ -o " + options.outputFile + " " + objectFile;
    #else
        linkCmd = "g++ -o " + options.outputFile + " " + objectFile;
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
    for (const auto& error : errors) {
        std::cerr << error << std::endl;
    }
}

} // namespace flow

