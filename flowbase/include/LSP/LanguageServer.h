#ifndef FLOW_LANGUAGE_SERVER_H
#define FLOW_LANGUAGE_SERVER_H

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <functional>
#include "../Parser/Parser.h"
#include "../Sema/SemanticAnalyzer.h"

namespace flow {
namespace lsp {

// LSP Position (line, character)
struct Position {
    int line;
    int character;
    
    Position() : line(0), character(0) {}
    Position(int l, int c) : line(l), character(c) {}
};

// LSP Range (start, end)
struct Range {
    Position start;
    Position end;
    
    Range() {}
    Range(Position s, Position e) : start(s), end(e) {}
};

// LSP Location
struct Location {
    std::string uri;
    Range range;
    
    Location() {}
    Location(const std::string& u, Range r) : uri(u), range(r) {}
};

// LSP Diagnostic Severity
enum class DiagnosticSeverity {
    Error = 1,
    Warning = 2,
    Information = 3,
    Hint = 4
};

// LSP Diagnostic
struct Diagnostic {
    Range range;
    DiagnosticSeverity severity;
    std::string message;
    std::string source;
    
    Diagnostic() : severity(DiagnosticSeverity::Error), source("flow") {}
};

// LSP Completion Item Kind
enum class CompletionItemKind {
    Text = 1,
    Method = 2,
    Function = 3,
    Constructor = 4,
    Field = 5,
    Variable = 6,
    Class = 7,
    Interface = 8,
    Module = 9,
    Property = 10,
    Keyword = 14,
    Snippet = 15,
    Struct = 22,
    TypeParameter = 25
};

// LSP Completion Item
struct CompletionItem {
    std::string label;
    CompletionItemKind kind;
    std::string detail;
    std::string documentation;
    
    CompletionItem() : kind(CompletionItemKind::Text) {}
    CompletionItem(const std::string& l, CompletionItemKind k) : label(l), kind(k) {}
};

// LSP Hover
struct Hover {
    std::string contents;
    Range range;
    
    Hover() {}
    Hover(const std::string& c, Range r) : contents(c), range(r) {}
};

// Document state
struct DocumentState {
    std::string uri;
    std::string text;
    int version;
    std::shared_ptr<Program> ast;
    std::vector<Diagnostic> diagnostics;
    
    DocumentState() : version(0), ast(nullptr) {}
};

// JSON-RPC message types
struct JSONRPCRequest {
    std::string jsonrpc;
    int id;
    std::string method;
    std::string params; // JSON string
    
    JSONRPCRequest() : jsonrpc("2.0"), id(-1) {}
};

struct JSONRPCResponse {
    std::string jsonrpc;
    int id;
    std::string result; // JSON string
    std::string error;  // JSON string (if error)
    
    JSONRPCResponse() : jsonrpc("2.0"), id(-1) {}
};

class LanguageServer {
private:
    std::map<std::string, DocumentState> documents;
    bool isInitialized;
    bool isShutdown;
    
    // Helper methods
    std::string readMessage();
    void writeMessage(const std::string& message);
    JSONRPCRequest parseRequest(const std::string& message);
    std::string createResponse(int id, const std::string& result);
    std::string createError(int id, int code, const std::string& message);
    
    // Document management
    void updateDocument(const std::string& uri, const std::string& text, int version);
    void analyzeDocument(DocumentState& doc);
    
    // LSP method handlers
    std::string handleInitialize(const std::string& params);
    std::string handleInitialized(const std::string& params);
    std::string handleShutdown(const std::string& params);
    std::string handleExit(const std::string& params);
    std::string handleTextDocumentDidOpen(const std::string& params);
    std::string handleTextDocumentDidChange(const std::string& params);
    std::string handleTextDocumentDidClose(const std::string& params);
    std::string handleTextDocumentCompletion(const std::string& params);
    std::string handleTextDocumentHover(const std::string& params);
    std::string handleTextDocumentDefinition(const std::string& params);
    std::string handleTextDocumentReferences(const std::string& params);
    
    // Helper methods for LSP features
    std::vector<CompletionItem> getCompletions(const std::string& uri, Position pos);
    Hover getHover(const std::string& uri, Position pos);
    std::vector<Location> getDefinition(const std::string& uri, Position pos);
    std::vector<Location> getReferences(const std::string& uri, Position pos);
    
public:
    LanguageServer();
    
    // Main loop
    void run();
    void handleMessage(const std::string& message);
    
    // Diagnostics
    void publishDiagnostics(const std::string& uri, const std::vector<Diagnostic>& diagnostics);
};

// JSON helpers
std::string escapeJSON(const std::string& str);
std::string extractJSONField(const std::string& json, const std::string& field);
int extractJSONInt(const std::string& json, const std::string& field);

} // namespace lsp
} // namespace flow

#endif // FLOW_LANGUAGE_SERVER_H

