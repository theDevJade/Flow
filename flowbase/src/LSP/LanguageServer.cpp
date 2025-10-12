#include "../../include/LSP/LanguageServer.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Common/ErrorReporter.h"
#include <iostream>
#include <sstream>
#include <algorithm>

namespace flow {
    namespace lsp {
        static std::string extractIdentifierAtPosition(const std::string &text, Position pos);

        std::string escapeJSON(const std::string &str) {
            std::ostringstream oss;
            for (char c: str) {
                switch (c) {
                    case '"': oss << "\\\"";
                        break;
                    case '\\': oss << "\\\\";
                        break;
                    case '\b': oss << "\\b";
                        break;
                    case '\f': oss << "\\f";
                        break;
                    case '\n': oss << "\\n";
                        break;
                    case '\r': oss << "\\r";
                        break;
                    case '\t': oss << "\\t";
                        break;
                    default:
                        if (c < 0x20) {
                            char buf[8];
                            snprintf(buf, sizeof(buf), "\\u%04x", c);
                            oss << buf;
                        } else {
                            oss << c;
                        }
                }
            }
            return oss.str();
        }

        std::string extractJSONField(const std::string &json, const std::string &field) {
            std::string searchStr = "\"" + field + "\":";
            size_t pos = json.find(searchStr);
            if (pos == std::string::npos) return "";

            pos += searchStr.length();

            while (pos < json.length() && (json[pos] == ' ' || json[pos] == '\t')) pos++;

            if (pos >= json.length()) return "";


            if (json[pos] == '"') {
                pos++;
                size_t end = pos;
                while (end < json.length() && json[end] != '"') {
                    if (json[end] == '\\') end++; // Skip escaped chars
                    end++;
                }
                return json.substr(pos, end - pos);
            }

            // Handle other values (objects, arrays, numbers, etc.)
            size_t end = pos;
            int braceDepth = 0;
            int bracketDepth = 0;
            while (end < json.length()) {
                char c = json[end];
                if (c == '{') braceDepth++;
                else if (c == '}') {
                    if (braceDepth > 0) braceDepth--;
                    else break;
                } else if (c == '[') bracketDepth++;
                else if (c == ']') {
                    if (bracketDepth > 0) bracketDepth--;
                    else break;
                } else if ((c == ',' || c == '}' || c == ']') && braceDepth == 0 && bracketDepth == 0) {
                    break;
                }
                end++;
            }

            return json.substr(pos, end - pos);
        }

        int extractJSONInt(const std::string &json, const std::string &field) {
            std::string value = extractJSONField(json, field);
            if (value.empty()) return 0;
            return std::stoi(value);
        }


        LanguageServer::LanguageServer() : isInitialized(false), isShutdown(false) {
        }

        std::string LanguageServer::readMessage() {
            std::string line;
            int contentLength = 0;

            // Read headers
            while (std::getline(std::cin, line)) {
                if (line == "\r" || line.empty()) break;

                if (line.find("Content-Length: ") == 0) {
                    contentLength = std::stoi(line.substr(16));
                }
            }

            if (contentLength == 0) return "";

            // Read content
            std::string content(contentLength, '\0');
            std::cin.read(&content[0], contentLength);

            return content;
        }

        void LanguageServer::writeMessage(const std::string &message) {
            std::cout << "Content-Length: " << message.length() << "\r\n\r\n";
            std::cout << message << std::flush;
        }

        JSONRPCRequest LanguageServer::parseRequest(const std::string &message) {
            JSONRPCRequest req;
            req.method = extractJSONField(message, "method");
            req.id = extractJSONInt(message, "id");
            req.params = extractJSONField(message, "params");
            return req;
        }

        std::string LanguageServer::createResponse(int id, const std::string &result) {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"id\":" << id << ",\"result\":" << result << "}";
            return oss.str();
        }

        std::string LanguageServer::createError(int id, int code, const std::string &message) {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"id\":" << id << ",\"error\":{";
            oss << "\"code\":" << code << ",\"message\":\"" << escapeJSON(message) << "\"}}";
            return oss.str();
        }

        void LanguageServer::run() {
            while (!isShutdown) {
                std::string message = readMessage();
                if (message.empty()) continue;

                handleMessage(message);
            }
        }

        void LanguageServer::handleMessage(const std::string &message) {
            JSONRPCRequest req = parseRequest(message);
            std::string response;

            if (req.method == "initialize") {
                response = handleInitialize(req.params);
                writeMessage(createResponse(req.id, response));
            } else if (req.method == "initialized") {
                // Notification, no response needed
            } else if (req.method == "shutdown") {
                response = handleShutdown(req.params);
                writeMessage(createResponse(req.id, response));
            } else if (req.method == "exit") {
                handleExit(req.params);
            } else if (req.method == "textDocument/didOpen") {
                handleTextDocumentDidOpen(req.params);
            } else if (req.method == "textDocument/didChange") {
                handleTextDocumentDidChange(req.params);
            } else if (req.method == "textDocument/didClose") {
                handleTextDocumentDidClose(req.params);
            } else if (req.method == "textDocument/completion") {
                response = handleTextDocumentCompletion(req.params);
                writeMessage(createResponse(req.id, response));
            } else if (req.method == "textDocument/hover") {
                response = handleTextDocumentHover(req.params);
                writeMessage(createResponse(req.id, response));
            } else if (req.method == "textDocument/definition") {
                response = handleTextDocumentDefinition(req.params);
                writeMessage(createResponse(req.id, response));
            } else if (req.method == "textDocument/references") {
                response = handleTextDocumentReferences(req.params);
                writeMessage(createResponse(req.id, response));
            }
        }

        std::string LanguageServer::handleInitialize(const std::string &params) {
            isInitialized = true;

            // Return server capabilities
            return R"({
        "capabilities": {
            "textDocumentSync": 1,
            "completionProvider": {
                "resolveProvider": false,
                "triggerCharacters": ["."]
            },
            "hoverProvider": true,
            "definitionProvider": true,
            "referencesProvider": true
        }
    })";
        }

        std::string LanguageServer::handleInitialized(const std::string &params) {
            return "{}";
        }

        std::string LanguageServer::handleShutdown(const std::string &params) {
            isShutdown = true;
            return "null";
        }

        std::string LanguageServer::handleExit(const std::string &params) {
            exit(0);
            return "";
        }

        void LanguageServer::updateDocument(const std::string &uri, const std::string &text, int version) {
            DocumentState &doc = documents[uri];
            doc.uri = uri;
            doc.text = text;
            doc.version = version;

            analyzeDocument(doc);
        }

        void LanguageServer::analyzeDocument(DocumentState &doc) {
            doc.diagnostics.clear();

            try {
                // Lexical analysis
                Lexer lexer(doc.text, doc.uri);
                std::vector<Token> tokens = lexer.tokenize();

                if (tokens.empty() || tokens.back().type == TokenType::INVALID) {
                    Diagnostic diag;
                    diag.range.start = Position(0, 0);
                    diag.range.end = Position(0, 10);
                    diag.severity = DiagnosticSeverity::Error;
                    diag.message = "Lexical analysis failed";
                    doc.diagnostics.push_back(diag);
                    publishDiagnostics(doc.uri, doc.diagnostics);
                    return;
                }

                // Parsing
                Parser parser(tokens);
                doc.ast = parser.parse();

                if (!doc.ast) {
                    Diagnostic diag;
                    diag.range.start = Position(0, 0);
                    diag.range.end = Position(0, 10);
                    diag.severity = DiagnosticSeverity::Error;
                    diag.message = "Parsing failed";
                    doc.diagnostics.push_back(diag);
                    publishDiagnostics(doc.uri, doc.diagnostics);
                    return;
                }

                // Semantic analysis
                SemanticAnalyzer analyzer;
                analyzer.analyze(doc.ast);

                if (analyzer.hasErrors()) {
                    for (const auto &error: analyzer.getErrors()) {
                        Diagnostic diag;
                        diag.range.start = Position(0, 0);
                        diag.range.end = Position(0, 10);
                        diag.severity = DiagnosticSeverity::Error;
                        diag.message = error;
                        doc.diagnostics.push_back(diag);
                    }
                }
            } catch (const std::exception &e) {
                Diagnostic diag;
                diag.range.start = Position(0, 0);
                diag.range.end = Position(0, 10);
                diag.severity = DiagnosticSeverity::Error;
                diag.message = std::string("Analysis error: ") + e.what();
                doc.diagnostics.push_back(diag);
            }

            publishDiagnostics(doc.uri, doc.diagnostics);
        }

        void LanguageServer::publishDiagnostics(const std::string &uri, const std::vector<Diagnostic> &diagnostics) {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/publishDiagnostics\",\"params\":{";
            oss << "\"uri\":\"" << escapeJSON(uri) << "\",\"diagnostics\":[";

            for (size_t i = 0; i < diagnostics.size(); i++) {
                if (i > 0) oss << ",";
                const auto &diag = diagnostics[i];
                oss << "{\"range\":{\"start\":{\"line\":" << diag.range.start.line
                        << ",\"character\":" << diag.range.start.character << "},";
                oss << "\"end\":{\"line\":" << diag.range.end.line
                        << ",\"character\":" << diag.range.end.character << "}},";
                oss << "\"severity\":" << static_cast<int>(diag.severity) << ",";
                oss << "\"message\":\"" << escapeJSON(diag.message) << "\",";
                oss << "\"source\":\"" << escapeJSON(diag.source) << "\"}";
            }

            oss << "]}}";
            writeMessage(oss.str());
        }

        std::string LanguageServer::handleTextDocumentDidOpen(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string text = extractJSONField(textDocument, "text");
            int version = extractJSONInt(textDocument, "version");

            updateDocument(uri, text, version);
            return "";
        }

        std::string LanguageServer::handleTextDocumentDidChange(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            int version = extractJSONInt(textDocument, "version");

            std::string contentChanges = extractJSONField(params, "contentChanges");
            std::string text = extractJSONField(contentChanges, "text");

            updateDocument(uri, text, version);
            return "";
        }

        std::string LanguageServer::handleTextDocumentDidClose(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");

            documents.erase(uri);
            return "";
        }

        std::vector<CompletionItem> LanguageServer::getCompletions(const std::string &uri, Position pos) {
            std::vector<CompletionItem> items;

            // Add keywords
            std::vector<std::string> keywords = {
                "func", "let", "mut", "return", "struct", "type", "if", "else",
                "for", "in", "while", "link", "export", "async", "await",
                "import", "module", "from", "as", "inline", "true", "false", "null"
            };

            for (const auto &kw: keywords) {
                items.push_back(CompletionItem(kw, CompletionItemKind::Keyword));
            }

            // Add types
            std::vector<std::string> types = {"int", "float", "string", "bool", "void"};
            for (const auto &t: types) {
                CompletionItem item(t, CompletionItemKind::TypeParameter);
                item.detail = "Built-in type";
                items.push_back(item);
            }

            // Add stdlib functions
            std::vector<std::pair<std::string, std::string> > stdlibFuncs = {
                {"println", "println(message: string) -> void"},
                {"print", "print(message: string) -> void"},
                {"readLine", "readLine() -> string"},
                {"readInt", "readInt() -> int"},
                {"abs", "abs(n: int) -> int"},
                {"sqrt", "sqrt(x: float) -> float"},
                {"pow", "pow(base: float, exp: float) -> float"},
                {"min", "min(a: int, b: int) -> int"},
                {"max", "max(a: int, b: int) -> int"},
                {"len", "len(array: T[]) -> int"},
                {"substr", "substr(s: string, start: int, len: int) -> string"},
                {"concat", "concat(s1: string, s2: string) -> string"}
            };

            for (const auto &[name, sig]: stdlibFuncs) {
                CompletionItem item(name, CompletionItemKind::Function);
                item.detail = sig;
                items.push_back(item);
            }

            // Add code snippets
            CompletionItem funcSnippet("func", CompletionItemKind::Snippet);
            funcSnippet.label = "func (function declaration)";
            funcSnippet.detail = "func name(params) -> type { body }";
            items.push_back(funcSnippet);

            CompletionItem structSnippet("struct", CompletionItemKind::Snippet);
            structSnippet.label = "struct (struct declaration)";
            structSnippet.detail = "struct Name { fields }";
            items.push_back(structSnippet);

            CompletionItem forSnippet("for", CompletionItemKind::Snippet);
            forSnippet.label = "for (for loop)";
            forSnippet.detail = "for (i in range) { body }";
            items.push_back(forSnippet);

            CompletionItem ifSnippet("if", CompletionItemKind::Snippet);
            ifSnippet.label = "if (if statement)";
            ifSnippet.detail = "if (condition) { body }";
            items.push_back(ifSnippet);

            // Extract symbols from document AST
            auto docIt = documents.find(uri);
            if (docIt != documents.end() && docIt->second.ast) {
                auto &ast = docIt->second.ast;

                // Extract functions
                for (const auto &decl: ast->declarations) {
                    if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl)) {
                        CompletionItem item(funcDecl->name, CompletionItemKind::Function);

                        // Build signature
                        std::ostringstream sig;
                        sig << funcDecl->name << "(";
                        for (size_t i = 0; i < funcDecl->parameters.size(); i++) {
                            if (i > 0) sig << ", ";
                            sig << funcDecl->parameters[i].name << ": ";
                            if (funcDecl->parameters[i].type) {
                                sig << funcDecl->parameters[i].type->name;
                            }
                        }
                        sig << ") -> ";
                        if (funcDecl->returnType) {
                            sig << funcDecl->returnType->name;
                        } else {
                            sig << "void";
                        }

                        item.detail = sig.str();
                        items.push_back(item);
                    }

                    // Extract structs
                    if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl)) {
                        CompletionItem item(structDecl->name, CompletionItemKind::Struct);
                        item.detail = "struct " + structDecl->name;
                        items.push_back(item);

                        // Add struct members as completions (for member access)
                        for (const auto &field: structDecl->fields) {
                            CompletionItem fieldItem(field.name, CompletionItemKind::Field);
                            if (field.type) {
                                fieldItem.detail = field.name + ": " + field.type->name;
                            }
                            items.push_back(fieldItem);
                        }
                    }

                    // Extract type aliases
                    if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl)) {
                        CompletionItem item(typedefDecl->name, CompletionItemKind::TypeParameter);
                        if (typedefDecl->aliasedType) {
                            item.detail = "type " + typedefDecl->name + " = " + typedefDecl->aliasedType->name;
                        }
                        items.push_back(item);
                    }

                    // Extract foreign functions from link blocks
                    if (auto linkDecl = std::dynamic_pointer_cast<LinkDecl>(decl)) {
                        for (const auto &func: linkDecl->functions) {
                            if (func) {
                                CompletionItem item(func->name, CompletionItemKind::Function);

                                std::ostringstream sig;
                                sig << func->name << "(";
                                for (size_t i = 0; i < func->parameters.size(); i++) {
                                    if (i > 0) sig << ", ";
                                    sig << func->parameters[i].name << ": ";
                                    if (func->parameters[i].type) {
                                        sig << func->parameters[i].type->name;
                                    }
                                }
                                sig << ") -> ";
                                if (func->returnType) {
                                    sig << func->returnType->name;
                                } else {
                                    sig << "void";
                                }

                                sig << " [foreign: " << linkDecl->adapter;
                                if (!linkDecl->module.empty()) {
                                    sig << ":" << linkDecl->module;
                                }
                                sig << "]";

                                item.detail = sig.str();
                                items.push_back(item);
                            }
                        }
                    }
                }
            }

            return items;
        }

        std::string LanguageServer::handleTextDocumentCompletion(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto items = getCompletions(uri, Position(line, character));

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < items.size(); i++) {
                if (i > 0) oss << ",";
                oss << "{\"label\":\"" << escapeJSON(items[i].label) << "\",";
                oss << "\"kind\":" << static_cast<int>(items[i].kind);
                if (!items[i].detail.empty()) {
                    oss << ",\"detail\":\"" << escapeJSON(items[i].detail) << "\"";
                }
                oss << "}";
            }
            oss << "]";

            return oss.str();
        }

        Hover LanguageServer::getHover(const std::string &uri, Position pos) {
            Hover hover;
            hover.range = Range(pos, Position(pos.line, pos.character + 5));
            hover.contents = "Flow Language";
            return hover;
        }

        std::string LanguageServer::handleTextDocumentHover(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto hover = getHover(uri, Position(line, character));

            std::ostringstream oss;
            oss << "{\"contents\":\"" << escapeJSON(hover.contents) << "\"}";
            return oss.str();
        }

        std::vector<Location> LanguageServer::getDefinition(const std::string &uri, Position pos) {
            std::vector<Location> locations;

            auto docIt = documents.find(uri);
            if (docIt == documents.end() || !docIt->second.ast) {
                return locations;
            }

            auto &doc = docIt->second;
            std::string identifier = extractIdentifierAtPosition(doc.text, pos);
            if (identifier.empty()) {
                return locations;
            }

            for (auto &decl: doc.ast->declarations) {
                if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl)) {
                    if (funcDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                        loc.range.end = Position(funcDecl->location.line - 1,
                                                 funcDecl->location.column + identifier.length());
                        locations.push_back(loc);
                        return locations;
                    }
                }

                if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl)) {
                    if (structDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(structDecl->location.line - 1, structDecl->location.column);
                        loc.range.end = Position(structDecl->location.line - 1,
                                                 structDecl->location.column + identifier.length());
                        locations.push_back(loc);
                        return locations;
                    }

                    for (const auto &field: structDecl->fields) {
                        if (field.name == identifier) {
                            Location loc;
                            loc.uri = uri;
                            loc.range.start = Position(structDecl->location.line - 1, structDecl->location.column);
                            loc.range.end = Position(structDecl->location.line - 1,
                                                     structDecl->location.column + identifier.length());
                            locations.push_back(loc);
                            return locations;
                        }
                    }
                }

                if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl)) {
                    if (typedefDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(typedefDecl->location.line - 1, typedefDecl->location.column);
                        loc.range.end = Position(typedefDecl->location.line - 1,
                                                 typedefDecl->location.column + identifier.length());
                        locations.push_back(loc);
                        return locations;
                    }
                }
            }

            return locations;
        }

        static std::string extractIdentifierAtPosition(const std::string &text, Position pos) {
            std::vector<std::string> lines;
            std::string line;
            std::istringstream stream(text);
            while (std::getline(stream, line)) {
                lines.push_back(line);
            }

            if (pos.line >= static_cast<int>(lines.size())) {
                return "";
            }

            const std::string &currentLine = lines[pos.line];
            if (pos.character >= static_cast<int>(currentLine.length())) {
                return "";
            }

            int start = pos.character;
            int end = pos.character;

            while (start > 0 && (std::isalnum(currentLine[start - 1]) || currentLine[start - 1] == '_')) {
                start--;
            }

            while (end < static_cast<int>(currentLine.length()) &&
                   (std::isalnum(currentLine[end]) || currentLine[end] == '_')) {
                end++;
            }

            if (start >= end) {
                return "";
            }

            return currentLine.substr(start, end - start);
        }

        std::string LanguageServer::handleTextDocumentDefinition(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto locations = getDefinition(uri, Position(line, character));

            if (locations.empty()) {
                return "null";
            }

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < locations.size(); i++) {
                if (i > 0) oss << ",";
                oss << "{\"uri\":\"" << escapeJSON(locations[i].uri) << "\",";
                oss << "\"range\":{\"start\":{\"line\":" << locations[i].range.start.line
                        << ",\"character\":" << locations[i].range.start.character << "},";
                oss << "\"end\":{\"line\":" << locations[i].range.end.line
                        << ",\"character\":" << locations[i].range.end.character << "}}}";
            }
            oss << "]";

            return oss.str();
        }

        std::vector<Location> LanguageServer::getReferences(const std::string &uri, Position pos) {
            std::vector<Location> locations;

            auto docIt = documents.find(uri);
            if (docIt == documents.end() || !docIt->second.ast) {
                return locations;
            }

            auto &doc = docIt->second;
            std::string identifier = extractIdentifierAtPosition(doc.text, pos);
            if (identifier.empty()) {
                return locations;
            }

            std::function < void(std::shared_ptr<Expr>) > searchExprForReferences;
            searchExprForReferences = [&](std::shared_ptr<Expr> expr) {
                if (!expr) return;

                if (auto idExpr = std::dynamic_pointer_cast<IdentifierExpr>(expr)) {
                    if (idExpr->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(idExpr->location.line - 1, idExpr->location.column);
                        loc.range.end = Position(idExpr->location.line - 1,
                                                 idExpr->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                } else if (auto binExpr = std::dynamic_pointer_cast<BinaryExpr>(expr)) {
                    searchExprForReferences(binExpr->left);
                    searchExprForReferences(binExpr->right);
                } else if (auto unaryExpr = std::dynamic_pointer_cast<UnaryExpr>(expr)) {
                    searchExprForReferences(unaryExpr->operand);
                } else if (auto callExpr = std::dynamic_pointer_cast<CallExpr>(expr)) {
                    searchExprForReferences(callExpr->callee);
                    for (auto &arg: callExpr->arguments) {
                        searchExprForReferences(arg);
                    }
                } else if (auto memberExpr = std::dynamic_pointer_cast<MemberAccessExpr>(expr)) {
                    searchExprForReferences(memberExpr->object);
                } else if (auto structExpr = std::dynamic_pointer_cast<StructInitExpr>(expr)) {
                    for (auto &fieldVal: structExpr->fieldValues) {
                        searchExprForReferences(fieldVal);
                    }
                } else if (auto arrayExpr = std::dynamic_pointer_cast<ArrayLiteralExpr>(expr)) {
                    for (auto &elem: arrayExpr->elements) {
                        searchExprForReferences(elem);
                    }
                } else if (auto indexExpr = std::dynamic_pointer_cast<IndexExpr>(expr)) {
                    searchExprForReferences(indexExpr->array);
                    searchExprForReferences(indexExpr->index);
                }
            };

            std::function < void(std::shared_ptr<Stmt>) > searchStmtForReferences;
            searchStmtForReferences = [&](std::shared_ptr<Stmt> stmt) {
                if (!stmt) return;

                if (auto exprStmt = std::dynamic_pointer_cast<ExprStmt>(stmt)) {
                    searchExprForReferences(exprStmt->expression);
                } else if (auto varDecl = std::dynamic_pointer_cast<VarDeclStmt>(stmt)) {
                    if (varDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(varDecl->location.line - 1, varDecl->location.column);
                        loc.range.end = Position(varDecl->location.line - 1,
                                                 varDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                    searchExprForReferences(varDecl->initializer);
                } else if (auto assignStmt = std::dynamic_pointer_cast<AssignmentStmt>(stmt)) {
                    if (assignStmt->target == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(assignStmt->location.line - 1, assignStmt->location.column);
                        loc.range.end = Position(assignStmt->location.line - 1,
                                                 assignStmt->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                    searchExprForReferences(assignStmt->value);
                } else if (auto returnStmt = std::dynamic_pointer_cast<ReturnStmt>(stmt)) {
                    searchExprForReferences(returnStmt->value);
                } else if (auto ifStmt = std::dynamic_pointer_cast<IfStmt>(stmt)) {
                    searchExprForReferences(ifStmt->condition);
                    for (auto &s: ifStmt->thenBranch) searchStmtForReferences(s);
                    for (auto &s: ifStmt->elseBranch) searchStmtForReferences(s);
                } else if (auto whileStmt = std::dynamic_pointer_cast<WhileStmt>(stmt)) {
                    searchExprForReferences(whileStmt->condition);
                    for (auto &s: whileStmt->body) searchStmtForReferences(s);
                } else if (auto forStmt = std::dynamic_pointer_cast<ForStmt>(stmt)) {
                    if (forStmt->iteratorVar == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(forStmt->location.line - 1, forStmt->location.column);
                        loc.range.end = Position(forStmt->location.line - 1,
                                                 forStmt->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                    searchExprForReferences(forStmt->rangeStart);
                    searchExprForReferences(forStmt->rangeEnd);
                    searchExprForReferences(forStmt->iterable);
                    for (auto &s: forStmt->body) searchStmtForReferences(s);
                } else if (auto blockStmt = std::dynamic_pointer_cast<BlockStmt>(stmt)) {
                    for (auto &s: blockStmt->statements) searchStmtForReferences(s);
                }
            };

            for (auto &decl: doc.ast->declarations) {
                if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl)) {
                    if (funcDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                        loc.range.end = Position(funcDecl->location.line - 1,
                                                 funcDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }

                    for (const auto &param: funcDecl->parameters) {
                        if (param.name == identifier) {
                            Location loc;
                            loc.uri = uri;
                            loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                            loc.range.end = Position(funcDecl->location.line - 1,
                                                     funcDecl->location.column + identifier.length());
                            locations.push_back(loc);
                        }
                    }

                    for (auto &stmt: funcDecl->body) {
                        searchStmtForReferences(stmt);
                    }
                } else if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl)) {
                    if (structDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(structDecl->location.line - 1, structDecl->location.column);
                        loc.range.end = Position(structDecl->location.line - 1,
                                                 structDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                } else if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl)) {
                    if (typedefDecl->name == identifier) {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(typedefDecl->location.line - 1, typedefDecl->location.column);
                        loc.range.end = Position(typedefDecl->location.line - 1,
                                                 typedefDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                }
            }

            return locations;
        }

        std::string LanguageServer::handleTextDocumentReferences(const std::string &params) {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto locations = getReferences(uri, Position(line, character));

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < locations.size(); i++) {
                if (i > 0) oss << ",";
                oss << "{\"uri\":\"" << escapeJSON(locations[i].uri) << "\",";
                oss << "\"range\":{\"start\":{\"line\":" << locations[i].range.start.line
                        << ",\"character\":" << locations[i].range.start.character << "},";
                oss << "\"end\":{\"line\":" << locations[i].range.end.line
                        << ",\"character\":" << locations[i].range.end.character << "}}}";
            }
            oss << "]";

            return oss.str();
        }
    } // namespace lsp
} // namespace flow