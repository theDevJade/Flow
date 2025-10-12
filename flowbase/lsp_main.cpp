#include "include/LSP/LanguageServer.h"
#include <iostream>

int main(int argc, char **argv) {
    flow::lsp::LanguageServer server;

    std::cerr << "Flow Language Server started" << std::endl;

    try {
        server.run();
    } catch (const std::exception &e) {
        std::cerr << "Language Server error: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}