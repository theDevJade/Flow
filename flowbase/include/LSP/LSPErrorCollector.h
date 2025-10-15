#ifndef FLOW_LSP_ERROR_COLLECTOR_H
#define FLOW_LSP_ERROR_COLLECTOR_H

#include "../Lexer/Token.h"
#include <string>
#include <vector>

namespace flow {
    namespace lsp {
        struct LSPError {
            std::string type;
            std::string message;
            SourceLocation location;
            
            LSPError(const std::string &t, const std::string &msg, const SourceLocation &loc)
                : type(t), message(msg), location(loc) {}
        };
        
        class LSPErrorCollector {
        private:
            std::vector<LSPError> errors;
            std::vector<LSPError> warnings;
            
        public:
            void reportError(const std::string &type, const std::string &message, const SourceLocation &loc) {
                errors.emplace_back(type, message, loc);
            }
            
            void reportWarning(const std::string &message, const SourceLocation &loc) {
                warnings.emplace_back("Warning", message, loc);
            }
            
            const std::vector<LSPError>& getErrors() const { return errors; }
            const std::vector<LSPError>& getWarnings() const { return warnings; }
            
            bool hasErrors() const { return !errors.empty(); }
            bool hasWarnings() const { return !warnings.empty(); }
            
            void clear() {
                errors.clear();
                warnings.clear();
            }
        };
    }
}

#endif // FLOW_LSP_ERROR_COLLECTOR_H
