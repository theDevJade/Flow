#ifndef FLOW_ERROR_REPORTER_H
#define FLOW_ERROR_REPORTER_H

#include "../Lexer/Token.h"
#include <string>
#include <vector>
#include <map>

namespace flow {

class ErrorReporter {
private:
    std::map<std::string, std::vector<std::string>> sourceLines; // filename -> lines
    
public:
    void loadSourceFile(const std::string& filename);
    void reportError(const std::string& type, const std::string& message, const SourceLocation& loc);
    void reportWarning(const std::string& message, const SourceLocation& loc);
    
    static ErrorReporter& instance() {
        static ErrorReporter reporter;
        return reporter;
    }
    
private:
    void showContext(const SourceLocation& loc);
};

} // namespace flow

#endif // FLOW_ERROR_REPORTER_H

