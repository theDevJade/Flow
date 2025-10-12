#include "../../include/Common/ErrorReporter.h"
#include <fstream>
#include <iostream>
#include <sstream>

namespace flow {

void ErrorReporter::loadSourceFile(const std::string& filename) {
    if (sourceLines.find(filename) != sourceLines.end()) {
        return; // Already loaded
    }
    
    std::ifstream file(filename);
    if (!file.is_open()) {
        std::cerr << "Warning: Could not open source file for error reporting: " << filename << std::endl;
        return;
    }
    
    std::vector<std::string> lines;
    std::string line;
    while (std::getline(file, line)) {
        lines.push_back(line);
    }
    
    sourceLines[filename] = lines;
}

void ErrorReporter::showContext(const SourceLocation& loc) {
    auto it = sourceLines.find(loc.filename);
    if (it == sourceLines.end()) {
        return; // Source not loaded
    }
    
    const auto& lines = it->second;
    if (loc.line < 1 || loc.line > lines.size()) {
        return;
    }
    

    std::cerr << "\n";
    

    if (loc.line > 1) {
        std::cerr << " " << (loc.line - 1) << " | " << lines[loc.line - 2] << "\n";
    }
    

    std::cerr << " " << loc.line << " | " << lines[loc.line - 1] << "\n";
    

    std::cerr << "     | ";
    for (int i = 1; i < loc.column; i++) {
        std::cerr << " ";
    }
    std::cerr << "^~~~\n";
    

    if (loc.line < lines.size()) {
        std::cerr << " " << (loc.line + 1) << " | " << lines[loc.line] << "\n";
    }
    
    std::cerr << "\n";
}

void ErrorReporter::reportError(const std::string& type, const std::string& message, const SourceLocation& loc) {
    std::cerr << "\n\033[1;31m" << type << " error\033[0m at "
              << loc.filename << ":" << loc.line << ":" << loc.column
              << ": " << message;
    
    showContext(loc);
}

void ErrorReporter::reportWarning(const std::string& message, const SourceLocation& loc) {
    std::cerr << "\n\033[1;33mWarning\033[0m at "
              << loc.filename << ":" << loc.line << ":" << loc.column
              << ": " << message;
    
    showContext(loc);
}

} // namespace flow

