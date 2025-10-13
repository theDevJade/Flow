#include "../../include/Common/ErrorReporter.h"
#include <fstream>
#include <iostream>
#include <sstream>
#include <iomanip>

namespace flow {
    // ANSI color codes
    const std::string COLOR_RED = "\033[1;31m";
    const std::string COLOR_YELLOW = "\033[1;33m";
    const std::string COLOR_BLUE = "\033[1;34m";
    const std::string COLOR_RESET = "\033[0m";
    const std::string COLOR_BOLD = "\033[1m";
    void ErrorReporter::loadSourceFile(const std::string &filename) {
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

    void ErrorReporter::showContext(const SourceLocation &loc) {
        auto it = sourceLines.find(loc.filename);
        if (it == sourceLines.end()) {
            return; // Source not loaded
        }

        const auto &lines = it->second;
        if (loc.line < 1 || loc.line > lines.size()) {
            return;
        }

        std::cerr << "\n";

        // Show context line before (if exists)
        if (loc.line > 1) {
            std::cerr << COLOR_BLUE << std::setw(5) << (loc.line - 1) << " | " << COLOR_RESET 
                      << lines[loc.line - 2] << "\n";
        }

        // Show the error line
        std::cerr << COLOR_BLUE << std::setw(5) << loc.line << " | " << COLOR_RESET 
                  << lines[loc.line - 1] << "\n";

        // Show the error indicator
        std::cerr << COLOR_BLUE << "      | " << COLOR_RESET;
        for (int i = 1; i < loc.column; i++) {
            std::cerr << " ";
        }
        std::cerr << COLOR_RED << COLOR_BOLD << "^" << COLOR_RESET;
        
        // Add squiggly line for emphasis (like Rust)
        int endCol = loc.column + 3; // Show a few characters
        if (endCol > lines[loc.line - 1].length()) {
            endCol = lines[loc.line - 1].length();
        }
        for (int i = loc.column; i < endCol; i++) {
            std::cerr << COLOR_RED << COLOR_BOLD << "~" << COLOR_RESET;
        }
        std::cerr << "\n";

        // Show context line after (if exists)
        if (loc.line < lines.size()) {
            std::cerr << COLOR_BLUE << std::setw(5) << (loc.line + 1) << " | " << COLOR_RESET 
                      << lines[loc.line] << "\n";
        }

        std::cerr << "\n";
    }

    void ErrorReporter::reportError(const std::string &type, const std::string &message, const SourceLocation &loc) {
        std::cerr << "\n" << COLOR_RED << COLOR_BOLD << "error";
        if (!type.empty()) {
            std::cerr << "[" << type << "]";
        }
        std::cerr << ":" << COLOR_RESET << COLOR_BOLD << " " << message << COLOR_RESET << "\n";
        std::cerr << COLOR_BLUE << "  --> " << COLOR_RESET << loc.filename << ":" << loc.line << ":" << loc.column;

        showContext(loc);
    }

    void ErrorReporter::reportWarning(const std::string &message, const SourceLocation &loc) {
        std::cerr << "\n" << COLOR_YELLOW << COLOR_BOLD << "warning:" << COLOR_RESET 
                  << COLOR_BOLD << " " << message << COLOR_RESET << "\n";
        std::cerr << COLOR_BLUE << "  --> " << COLOR_RESET << loc.filename << ":" << loc.line << ":" << loc.column;

        showContext(loc);
    }
} // namespace flow