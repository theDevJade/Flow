#include "include/LSP/LanguageServer.h"
#include <iostream>
#include <cstdlib>

int main(int argc, char **argv) {
    flow::lsp::LanguageServer server;
    
    // Check for library paths from environment or default to River packages
    std::vector<std::string> libraryPaths;
    if (const char* flowPath = std::getenv("FLOW_PATH")) {
        libraryPaths.push_back(flowPath);
    }
    if (const char* home = std::getenv("HOME")) {
        libraryPaths.push_back(std::string(home) + "/.river/packages");
    }
    
    server.setLibraryPaths(libraryPaths);

    std::cerr << "Flow Language Server started" << std::endl;

    try {
        server.run();
    } catch (const std::exception &e) {
        std::cerr << "Language Server error: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}