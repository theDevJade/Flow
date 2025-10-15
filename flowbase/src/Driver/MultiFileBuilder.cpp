#include "../../include/Driver/MultiFileBuilder.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Codegen/CodeGenerator.h"
#include <fstream>
#include <sstream>
#include <iostream>
#include <filesystem>
#include <sys/stat.h>
#include <cstdlib>
#include <iomanip>

namespace flow
{
    MultiFileBuilder::MultiFileBuilder(const std::string& mainFile, const std::string& outputFile, bool verbose)
        : mainFile(mainFile), outputFile(outputFile), buildDir(".flow_build"), verbose(verbose)
    {
        std::filesystem::create_directories(buildDir);
    }

    std::string MultiFileBuilder::resolveImportPath(const std::string& importPath, const std::string& currentDir)
    {
        namespace fs = std::filesystem;

        if (fs::path(importPath).is_absolute())
        {
            return importPath;
        }

        // First try: resolve relative to current directory
        fs::path fullPath = fs::path(currentDir) / importPath;

        try
        {
            return fs::canonical(fullPath).string();
        }
        catch (const fs::filesystem_error&)
        {
            // If that fails, try library paths from environment
            // Check FLOW_PATH or River packages directory
            if (const char* flowPath = std::getenv("FLOW_PATH"))
            {
                fs::path libFullPath = fs::path(flowPath) / importPath;
                try
                {
                    return fs::canonical(libFullPath).string();
                }
                catch (const fs::filesystem_error&)
                {
                    // Continue
                }
            }

            // Try ~/.river/packages/ as fallback
            if (const char* home = std::getenv("HOME"))
            {
                fs::path riverPath = fs::path(home) / ".river" / "packages";
                fs::path libFullPath = riverPath / importPath;
                try
                {
                    return fs::canonical(libFullPath).string();
                }
                catch (const fs::filesystem_error&)
                {
                    // Continue
                }
            }

            return fullPath.string();
        }
    }

    void MultiFileBuilder::discoverImports(const std::string& filePath)
    {
        if (processedModules.find(filePath) != processedModules.end())
        {
            return;
        }
        processedModules.insert(filePath);


        std::ifstream file(filePath);
        if (!file.is_open())
        {
            std::cerr << "Error: Cannot open file: " << filePath << std::endl;
            return;
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string source = buffer.str();
        file.close();

        // Get file size
        struct stat st;
        size_t fileSize = 0;
        if (stat(filePath.c_str(), &st) == 0)
        {
            fileSize = st.st_size;
        }


        ModuleInfo info;
        info.sourcePath = filePath;
        info.sourceSize = fileSize;
        info.compiled = false;


        std::filesystem::path srcPath(filePath);
        std::string objName = srcPath.stem().string() + ".o";
        info.objectPath = buildDir + "/" + objName;

        modules[filePath] = info;


        try
        {
            Lexer lexer(source, filePath);
            std::vector<Token> tokens = lexer.tokenize();

            Parser parser(tokens);
            auto program = parser.parse();

            if (program)
            {
                std::filesystem::path currentDir = std::filesystem::path(filePath).parent_path();


                for (auto& decl : program->declarations)
                {
                    if (auto* importDecl = dynamic_cast<ImportDecl*>(decl.get()))
                    {
                        std::string resolvedPath = resolveImportPath(importDecl->modulePath, currentDir.string());
                        discoverImports(resolvedPath); // Recursive
                    }
                }
            }
        }
        catch (const std::exception& e)
        {
            std::cerr << "Error parsing " << filePath << ": " << e.what() << std::endl;
        }
    }

    void MultiFileBuilder::printBuildHeader()
    {
        std::cout << "\n";
        std::cout << "================================================================\n";
        std::cout << "  Flow Multi-File Build System\n";
        std::cout << "================================================================\n";
        std::cout << "\n";
        std::cout << "  Main file: " << mainFile << "\n";
        std::cout << "  Output:    " << outputFile << "\n";
        std::cout << "  Modules:   " << modules.size() << "\n";
        std::cout << "\n";
    }

    void MultiFileBuilder::printModuleProgress(int current, int total, const std::string& moduleName)
    {
        int barWidth = 50;
        float progress = (float)current / total;
        int pos = barWidth * progress;

        std::cout << "  [";
        for (int i = 0; i < barWidth; ++i)
        {
            if (i < pos) std::cout << "=";
            else if (i == pos) std::cout << ">";
            else std::cout << " ";
        }
        std::cout << "] " << std::setw(3) << int(progress * 100.0) << "% ";
        std::cout << "(" << current << "/" << total << ") ";

        // Truncate module name if too long
        std::string displayName = moduleName;
        if (displayName.length() > 40)
        {
            displayName = "..." + displayName.substr(displayName.length() - 37);
        }

        std::cout << displayName << "       \r" << std::flush;
    }

    bool MultiFileBuilder::compileModule(const std::string& modulePath)
    {
        auto& info = modules[modulePath];

        // Read source
        std::ifstream file(modulePath);
        if (!file.is_open())
        {
            std::cerr << "\nError: Cannot open " << modulePath << std::endl;
            return false;
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string source = buffer.str();
        file.close();

        try
        {
            // Lex
            Lexer lexer(source, modulePath);
            std::vector<Token> tokens = lexer.tokenize();

            if (tokens.empty())
            {
                std::cerr << "\nError: Lexing failed for " << modulePath << std::endl;
                return false;
            }

            // Parse
            Parser parser(tokens);
            auto program = parser.parse();

            if (!program)
            {
                std::cerr << "\nError: Parsing failed for " << modulePath << std::endl;
                return false;
            }

            // Semantic analysis
            SemanticAnalyzer analyzer;
            analyzer.setCurrentFile(modulePath);
            analyzer.analyze(program);

            if (analyzer.hasErrors())
            {
                std::cerr << "\nError: Semantic analysis failed for " << modulePath << std::endl;
                for (const auto& err : analyzer.getErrors())
                {
                    std::cerr << "  " << err << std::endl;
                }
                return false;
            }

            // Code generation
            std::filesystem::path objPath(info.objectPath);
            std::string baseName = objPath.stem().string();

            CodeGenerator codegen(baseName);

            // For modules with imports, declare external functions from imported modules

            const auto& loadedModules = analyzer.getLoadedModules();
            for (const auto& [importPath, importedProgram] : loadedModules)
            {
                if (importedProgram)
                {
                    for (auto& decl : importedProgram->declarations)
                    {
                        if (auto* funcDecl = dynamic_cast<FunctionDecl*>(decl.get()))
                        {
                            codegen.declareExternalFunction(*funcDecl);
                        }
                    }
                }
            }

            codegen.generate(program);
            codegen.compileToObject(info.objectPath);

            // Get object file size
            struct stat st;
            if (stat(info.objectPath.c_str(), &st) == 0)
            {
                info.objectSize = st.st_size;
            }

            info.compiled = true;
            return true;
        }
        catch (const std::exception& e)
        {
            std::cerr << "\nError compiling " << modulePath << ": " << e.what() << std::endl;
            return false;
        }
    }

    void MultiFileBuilder::printLinkingInfo(const std::vector<std::string>& objectFiles)
    {
        std::cout << "\n----------------------------------------------------------------\n";
        std::cout << "  Linking Phase\n";
        std::cout << "----------------------------------------------------------------\n\n";

        size_t totalSize = 0;
        for (const auto& [path, info] : modules)
        {
            if (info.compiled)
            {
                totalSize += info.objectSize;
            }
        }

        std::cout << "  Object files: " << objectFiles.size() << "\n";
        std::cout << "  Total size:   " << totalSize << " bytes\n";
        std::cout << "  Output:       " << outputFile << "\n\n";
    }

    bool MultiFileBuilder::linkModules()
    {
        std::vector<std::string> objectFiles;
        for (const auto& [path, info] : modules)
        {
            if (info.compiled)
            {
                objectFiles.push_back(info.objectPath);
            }
        }

        if (objectFiles.empty())
        {
            std::cerr << "Error: No object files to link\n";
            return false;
        }

        printLinkingInfo(objectFiles);

        // Build link command
        std::string linkCmd;
#ifdef __APPLE__
        linkCmd = "clang++ -o " + outputFile;
#else
        linkCmd = "g++ -o " + outputFile;
#endif

        for (const auto& obj : objectFiles)
        {
            linkCmd += " " + obj;
        }

        if (verbose)
        {
            std::cout << "  Command: " << linkCmd << "\n\n";
        }

        int result = system(linkCmd.c_str());
        if (result != 0)
        {
            std::cerr << "Error: Linking failed\n";
            return false;
        }

        return true;
    }

    void MultiFileBuilder::printBuildSummary()
    {
        std::cout << "\n";
        std::cout << "================================================================\n";
        std::cout << "  Build Summary\n";
        std::cout << "================================================================\n\n";

        size_t totalSource = 0;
        size_t totalObject = 0;

        for (const auto& [path, info] : modules)
        {
            totalSource += info.sourceSize;
            if (info.compiled)
            {
                totalObject += info.objectSize;
            }
        }

        std::cout << "  Modules compiled:  " << modules.size() << "\n";
        std::cout << "  Source code size:  " << totalSource << " bytes\n";
        std::cout << "  Object code size:  " << totalObject << " bytes\n";

        // Get executable size
        struct stat st;
        if (stat(outputFile.c_str(), &st) == 0)
        {
            std::cout << "  Executable size:   " << st.st_size << " bytes\n";
        }

        std::cout << "\n";
        std::cout << "  BUILD SUCCESSFUL\n";
        std::cout << "  Output: " << outputFile << "\n";
        std::cout << "\n";
        std::cout << "================================================================\n\n";
    }

    bool MultiFileBuilder::build()
    {
        // Phase 1: Discover all modules
        if (verbose)
        {
            std::cout << "Discovering modules...\n";
        }

        discoverImports(mainFile);

        if (modules.empty())
        {
            std::cerr << "Error: No modules found\n";
            return false;
        }

        printBuildHeader();

        // Phase 2: Compile each module
        std::cout << "----------------------------------------------------------------\n";
        std::cout << "  Compilation Phase\n";
        std::cout << "----------------------------------------------------------------\n\n";

        int current = 0;
        int total = modules.size();

        for (auto& [path, info] : modules)
        {
            current++;

            std::filesystem::path p(path);
            std::string moduleName = p.filename().string();

            printModuleProgress(current, total, moduleName);

            if (!compileModule(path))
            {
                std::cout << "\n";
                return false;
            }
        }

        std::cout << "\n\n  All modules compiled successfully\n";

        // Phase 3: Link
        if (!linkModules())
        {
            return false;
        }

        // Phase 4: Summary
        printBuildSummary();

        return true;
    }
} // namespace flow