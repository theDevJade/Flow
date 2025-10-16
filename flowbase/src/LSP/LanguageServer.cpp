#include "../../include/LSP/LanguageServer.h"
#include "../../include/LSP/LSPErrorCollector.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Common/ErrorReporter.h"
#include "../../include/Runtime/ReflectionManager.h"
#include "../../include/Runtime/ForeignModuleLoader.h"
#include <iostream>
#include <sstream>
#include <algorithm>
#include <nlohmann/json.hpp>

namespace flow
{
    namespace lsp
    {
        static std::string extractIdentifierAtPosition(const std::string& text, Position pos);

        std::string escapeJSON(const std::string& str)
        {
            std::ostringstream oss;
            for (char c : str)
            {
                switch (c)
                {
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
                    if (c < 0x20)
                    {
                        char buf[8];
                        snprintf(buf, sizeof(buf), "\\u%04x", c);
                        oss << buf;
                    }
                    else
                    {
                        oss << c;
                    }
                }
            }
            return oss.str();
        }

        std::string extractJSONField(const std::string& json, const std::string& field)
        {
            std::string searchStr = "\"" + field + "\":";
            size_t pos = json.find(searchStr);
            if (pos == std::string::npos) return "";

            pos += searchStr.length();

            while (pos < json.length() && (json[pos] == ' ' || json[pos] == '\t')) pos++;

            if (pos >= json.length()) return "";


            if (json[pos] == '"')
            {
                pos++;
                size_t end = pos;
                std::string result;
                while (end < json.length() && json[end] != '"')
                {
                    if (json[end] == '\\' && end + 1 < json.length())
                    {
                        end++; // Skip the backslash
                        char escaped = json[end];
                        switch (escaped)
                        {
                        case 'n': result += '\n';
                            break;
                        case 't': result += '\t';
                            break;
                        case 'r': result += '\r';
                            break;
                        case '\\': result += '\\';
                            break;
                        case '"': result += '"';
                            break;
                        case '/': result += '/';
                            break;
                        default: result += escaped;
                            break;
                        }
                        end++;
                    }
                    else
                    {
                        result += json[end];
                        end++;
                    }
                }
                return result;
            }

            // Handle other values (objects, arrays, numbers, etc.)
            size_t end = pos;
            int braceDepth = 0;
            int bracketDepth = 0;
            while (end < json.length())
            {
                char c = json[end];
                if (c == '{') braceDepth++;
                else if (c == '}')
                {
                    if (braceDepth > 0) braceDepth--;
                    else break;
                }
                else if (c == '[') bracketDepth++;
                else if (c == ']')
                {
                    if (bracketDepth > 0) bracketDepth--;
                    else break;
                }
                else if ((c == ',' || c == '}' || c == ']') && braceDepth == 0 && bracketDepth == 0)
                {
                    break;
                }
                end++;
            }

            return json.substr(pos, end - pos);
        }

        int extractJSONInt(const std::string& json, const std::string& field)
        {
            std::string value = extractJSONField(json, field);
            if (value.empty()) return 0;
            return std::stoi(value);
        }


        LanguageServer::LanguageServer() : isInitialized(false), isShutdown(false)
        {
            // No more hardcoded modules! They'll be loaded dynamically when encountered in code.
        }

        std::string LanguageServer::readMessage()
        {
            std::string line;
            int contentLength = 0;

            // Read headers
            while (std::getline(std::cin, line))
            {
                if (line == "\r" || line.empty()) break;

                if (line.find("Content-Length: ") == 0)
                {
                    contentLength = std::stoi(line.substr(16));
                }
            }

            if (contentLength == 0) return "";

            // Read content
            std::string content(contentLength, '\0');
            std::cin.read(&content[0], contentLength);

            return content;
        }

        void LanguageServer::writeMessage(const std::string& message)
        {
            std::cout << "Content-Length: " << message.length() << "\r\n\r\n";
            std::cout << message << std::flush;
        }

        JSONRPCRequest LanguageServer::parseRequest(const std::string& message)
        {
            JSONRPCRequest req;
            try
            {
                auto json = nlohmann::json::parse(message);
                req.method = json["method"];
                req.id = json.value("id", -1);
                req.params = json["params"].dump();
            }
            catch (const std::exception& e)
            {
                std::cerr << "Error parsing JSON-RPC request: " << e.what() << std::endl;
                req.method = "";
                req.id = -1;
                req.params = "";
            }
            return req;
        }

        std::string LanguageServer::createResponse(int id, const std::string& result)
        {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"id\":" << id << ",\"result\":" << result << "}";
            return oss.str();
        }

        std::string LanguageServer::createError(int id, int code, const std::string& message)
        {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"id\":" << id << ",\"error\":{";
            oss << "\"code\":" << code << ",\"message\":\"" << escapeJSON(message) << "\"}}";
            return oss.str();
        }

        void LanguageServer::run()
        {
            while (!isShutdown)
            {
                std::string message = readMessage();
                if (message.empty()) continue;

                handleMessage(message);
            }
        }

        void LanguageServer::handleMessage(const std::string& message)
        {
            JSONRPCRequest req = parseRequest(message);
            std::string response;

            if (req.method == "initialize")
            {
                response = handleInitialize(req.params);
                writeMessage(createResponse(req.id, response));
            }
            else if (req.method == "initialized")
            {
                // Notification, no response needed
            }
            else if (req.method == "shutdown")
            {
                response = handleShutdown(req.params);
                writeMessage(createResponse(req.id, response));
            }
            else if (req.method == "exit")
            {
                handleExit(req.params);
            }
            else if (req.method == "textDocument/didOpen")
            {
                handleTextDocumentDidOpen(req.params);
            }
            else if (req.method == "textDocument/didChange")
            {
                handleTextDocumentDidChange(req.params);
            }
            else if (req.method == "textDocument/didClose")
            {
                handleTextDocumentDidClose(req.params);
            }
            else if (req.method == "textDocument/completion")
            {
                response = handleTextDocumentCompletion(req.params);
                writeMessage(createResponse(req.id, response));
            }
            else if (req.method == "textDocument/hover")
            {
                response = handleTextDocumentHover(req.params);
                writeMessage(createResponse(req.id, response));
            }
            else if (req.method == "textDocument/definition")
            {
                response = handleTextDocumentDefinition(req.params);
                writeMessage(createResponse(req.id, response));
            }
            else if (req.method == "textDocument/references")
            {
                response = handleTextDocumentReferences(req.params);
                writeMessage(createResponse(req.id, response));
            }
        }

        std::string LanguageServer::handleInitialize(const std::string& params)
        {
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

        std::string LanguageServer::handleInitialized(const std::string& params)
        {
            return "{}";
        }

        std::string LanguageServer::handleShutdown(const std::string& params)
        {
            isShutdown = true;
            return "null";
        }

        std::string LanguageServer::handleExit(const std::string& params)
        {
            exit(0);
            return "";
        }

        void LanguageServer::updateDocument(const std::string& uri, const std::string& text, int version)
        {
            DocumentState& doc = documents[uri];
            doc.uri = uri;
            doc.text = text;
            doc.version = version;

            analyzeDocument(doc);
        }

        void LanguageServer::analyzeDocument(DocumentState& doc)
        {
            doc.diagnostics.clear();

            try
            {
                // Create error collector for this analysis
                lsp::LSPErrorCollector errorCollector;

                // Debug: Log that we're starting analysis
                std::cerr << "Analyzing document: " << doc.uri << std::endl;

                // Lexical analysis
                Lexer lexer(doc.text, doc.uri);
                std::vector<Token> tokens = lexer.tokenize();

                if (tokens.empty() || tokens.back().type == TokenType::INVALID)
                {
                    Diagnostic diag;
                    diag.range.start = Position(0, 0);
                    diag.range.end = Position(0, 10);
                    diag.severity = DiagnosticSeverity::Error;
                    diag.message = "Lexical analysis failed";
                    diag.source = "Flow Lexer";
                    doc.diagnostics.push_back(diag);
                    publishDiagnostics(doc.uri, doc.diagnostics);
                    return;
                }

                // Parsing
                Parser parser(tokens);
                parser.setErrorCollector(&errorCollector);
                doc.ast = parser.parse();

                if (!doc.ast)
                {
                    Diagnostic diag;
                    diag.range.start = Position(0, 0);
                    diag.range.end = Position(0, 10);
                    diag.severity = DiagnosticSeverity::Error;
                    diag.message = "Parsing failed";
                    diag.source = "Flow Parser";
                    doc.diagnostics.push_back(diag);
                    publishDiagnostics(doc.uri, doc.diagnostics);
                    return;
                }

                // Register module with ReflectionManager for autocomplete/hover
                auto& reflectionMgr = flow::ReflectionManager::getInstance();
                reflectionMgr.registerFlowModuleFromAST(doc.uri, doc.ast);
                
                // Semantic analysis with error collector
                SemanticAnalyzer analyzer;
                analyzer.setLibraryPaths(libraryPaths);
                analyzer.setErrorCollector(&errorCollector);
                analyzer.setCurrentFile(doc.uri);
                analyzer.analyze(doc.ast);

                // Convert collected errors to diagnostics
                for (const auto& error : errorCollector.getErrors())
                {
                    Diagnostic diag;
                    diag.range.start = Position(error.location.line - 1, error.location.column - 1);
                    diag.range.end = Position(error.location.line - 1, error.location.column);
                    diag.severity = DiagnosticSeverity::Error;
                    diag.message = error.message;
                    diag.source = error.type;
                    doc.diagnostics.push_back(diag);
                }

                // Convert collected warnings to diagnostics
                for (const auto& warning : errorCollector.getWarnings())
                {
                    Diagnostic diag;
                    diag.range.start = Position(warning.location.line - 1, warning.location.column - 1);
                    diag.range.end = Position(warning.location.line - 1, warning.location.column);
                    diag.severity = DiagnosticSeverity::Warning;
                    diag.message = warning.message;
                    diag.source = warning.type;
                    doc.diagnostics.push_back(diag);
                }
            }
            catch (const std::exception& e)
            {
                Diagnostic diag;
                diag.range.start = Position(0, 0);
                diag.range.end = Position(0, 10);
                diag.severity = DiagnosticSeverity::Error;
                diag.message = std::string("Analysis error: ") + e.what();
                diag.source = "Flow LSP";
                doc.diagnostics.push_back(diag);
            }

            publishDiagnostics(doc.uri, doc.diagnostics);
        }

        void LanguageServer::publishDiagnostics(const std::string& uri, const std::vector<Diagnostic>& diagnostics)
        {
            std::ostringstream oss;
            oss << "{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/publishDiagnostics\",\"params\":{";
            oss << "\"uri\":\"" << escapeJSON(uri) << "\",\"diagnostics\":[";

            for (size_t i = 0; i < diagnostics.size(); i++)
            {
                if (i > 0) oss << ",";
                const auto& diag = diagnostics[i];
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

        std::string LanguageServer::handleTextDocumentDidOpen(const std::string& params)
        {
            try
            {
                auto json = nlohmann::json::parse(params);
                std::string uri = json["textDocument"]["uri"];
                std::string text = json["textDocument"]["text"];
                int version = json["textDocument"]["version"];

                updateDocument(uri, text, version);
                return "";
            }
            catch (const std::exception& e)
            {
                std::cerr << "Error parsing textDocument/didOpen: " << e.what() << std::endl;
                return "";
            }
        }

        std::string LanguageServer::handleTextDocumentDidChange(const std::string& params)
        {
            try
            {
                auto json = nlohmann::json::parse(params);
                std::string uri = json["textDocument"]["uri"];
                int version = json["textDocument"]["version"];

                auto docIt = documents.find(uri);
                if (docIt != documents.end())
                {
                    DocumentState& doc = docIt->second;

                    // Apply incremental changes
                    if (json.contains("contentChanges") && json["contentChanges"].is_array())
                    {
                        for (const auto& change : json["contentChanges"])
                        {
                            if (change.contains("text"))
                            {
                                doc.text = change["text"];
                                doc.version = version;
                            }
                        }
                    }


                    analyzeDocument(doc);
                }
                return "";
            }
            catch (const std::exception& e)
            {
                std::cerr << "Error parsing textDocument/didChange: " << e.what() << std::endl;
                return "";
            }
        }

        std::string LanguageServer::handleTextDocumentDidClose(const std::string& params)
        {
            try
            {
                auto json = nlohmann::json::parse(params);
                std::string uri = json["textDocument"]["uri"];

                documents.erase(uri);
                return "";
            }
            catch (const std::exception& e)
            {
                std::cerr << "Error parsing textDocument/didClose: " << e.what() << std::endl;
                return "";
            }
        }

        std::vector<CompletionItem> LanguageServer::getCompletions(const std::string& uri, Position pos)
        {
            std::vector<CompletionItem> items;

            // Add keywords
            std::vector<std::string> keywords = {
                "func", "let", "mut", "return", "struct", "type", "if", "else",
                "for", "in", "while", "link", "export", "async", "await",
                "import", "module", "from", "as", "inline", "true", "false", "null"
            };

            for (const auto& kw : keywords)
            {
                items.push_back(CompletionItem(kw, CompletionItemKind::Keyword));
            }

            // Add types
            std::vector<std::string> types = {"int", "float", "string", "bool", "void"};
            for (const auto& t : types)
            {
                CompletionItem item(t, CompletionItemKind::TypeParameter);
                item.detail = "Built-in type";
                items.push_back(item);
            }

            // Add stdlib functions
            std::vector<std::pair<std::string, std::string>> stdlibFuncs = {
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

            for (const auto& [name, sig] : stdlibFuncs)
            {
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
            
            // Add foreign functions from ReflectionManager
            auto& reflectionMgr = flow::ReflectionManager::getInstance();
            auto allForeignFuncs = reflectionMgr.getAllAvailableFunctions();
            for (const auto& sig : allForeignFuncs) {
                // Only add foreign functions (not Flow functions, as those come from AST)
                if (sig.sourceLanguage != "flow") {
                    CompletionItem item(sig.name, CompletionItemKind::Function);
                    item.detail = sig.toString();
                    item.documentation = sig.sourceLanguage + " function from module " + sig.sourceModule;
                    items.push_back(item);
                }
            }

            // Extract symbols from document AST
            auto docIt = documents.find(uri);
            if (docIt != documents.end() && docIt->second.ast)
            {
                auto& ast = docIt->second.ast;

                // Extract functions
                for (const auto& decl : ast->declarations)
                {
                    if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl))
                    {
                        CompletionItem item(funcDecl->name, CompletionItemKind::Function);

                        // Build signature
                        std::ostringstream sig;
                        sig << funcDecl->name << "(";
                        for (size_t i = 0; i < funcDecl->parameters.size(); i++)
                        {
                            if (i > 0) sig << ", ";
                            sig << funcDecl->parameters[i].name << ": ";
                            if (funcDecl->parameters[i].type)
                            {
                                sig << funcDecl->parameters[i].type->name;
                            }
                        }
                        sig << ") -> ";
                        if (funcDecl->returnType)
                        {
                            sig << funcDecl->returnType->name;
                        }
                        else
                        {
                            sig << "void";
                        }

                        item.detail = sig.str();
                        items.push_back(item);
                    }

                    // Extract structs
                    if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl))
                    {
                        CompletionItem item(structDecl->name, CompletionItemKind::Struct);
                        item.detail = "struct " + structDecl->name;
                        items.push_back(item);

                        // Add struct members as completions (for member access)
                        for (const auto& field : structDecl->fields)
                        {
                            CompletionItem fieldItem(field.name, CompletionItemKind::Field);
                            if (field.type)
                            {
                                fieldItem.detail = field.name + ": " + field.type->name;
                            }
                            items.push_back(fieldItem);
                        }
                    }

                    // Extract type aliases
                    if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl))
                    {
                        CompletionItem item(typedefDecl->name, CompletionItemKind::TypeParameter);
                        if (typedefDecl->aliasedType)
                        {
                            item.detail = "type " + typedefDecl->name + " = " + typedefDecl->aliasedType->name;
                        }
                        items.push_back(item);
                    }

                    // Extract foreign functions from link blocks
                    if (auto linkDecl = std::dynamic_pointer_cast<LinkDecl>(decl))
                    {
                        // DYNAMIC LOADING: Try to load the foreign module if not already loaded
                        auto& moduleLoader = flow::ForeignModuleLoader::getInstance();
                        moduleLoader.loadAndRegisterModule(linkDecl->adapter, linkDecl->module);
                        
                        for (const auto& func : linkDecl->functions)
                        {
                            if (func)
                            {
                                CompletionItem item(func->name, CompletionItemKind::Function);

                                std::ostringstream sig;
                                sig << func->name << "(";
                                for (size_t i = 0; i < func->parameters.size(); i++)
                                {
                                    if (i > 0) sig << ", ";
                                    sig << func->parameters[i].name << ": ";
                                    if (func->parameters[i].type)
                                    {
                                        sig << func->parameters[i].type->name;
                                    }
                                }
                                sig << ") -> ";
                                if (func->returnType)
                                {
                                    sig << func->returnType->name;
                                }
                                else
                                {
                                    sig << "void";
                                }

                                sig << " [foreign: " << linkDecl->adapter;
                                if (!linkDecl->module.empty())
                                {
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

        std::string LanguageServer::handleTextDocumentCompletion(const std::string& params)
        {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto items = getCompletions(uri, Position(line, character));

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < items.size(); i++)
            {
                if (i > 0) oss << ",";
                oss << "{\"label\":\"" << escapeJSON(items[i].label) << "\",";
                oss << "\"kind\":" << static_cast<int>(items[i].kind);
                if (!items[i].detail.empty())
                {
                    oss << ",\"detail\":\"" << escapeJSON(items[i].detail) << "\"";
                }
                oss << "}";
            }
            oss << "]";

            return oss.str();
        }

        Hover LanguageServer::getHover(const std::string& uri, Position pos)
        {
            Hover hover;
            hover.range = Range(pos, Position(pos.line, pos.character + 5));

            auto docIt = documents.find(uri);
            if (docIt == documents.end() || !docIt->second.ast)
            {
                hover.contents = "No document information available";
                return hover;
            }

            auto& doc = docIt->second;
            std::string identifier = extractIdentifierAtPosition(doc.text, pos);
            if (identifier.empty())
            {
                hover.contents = "Flow Language";
                return hover;
            }

            // Look for symbol information
            for (const auto& decl : doc.ast->declarations)
            {
                if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl))
                {
                    if (funcDecl->name == identifier)
                    {
                        std::ostringstream sig;
                        sig << "**" << funcDecl->name << "**\n\n";
                        sig << "```flow\n";
                        sig << "func " << funcDecl->name << "(";
                        for (size_t i = 0; i < funcDecl->parameters.size(); i++)
                        {
                            if (i > 0) sig << ", ";
                            sig << funcDecl->parameters[i].name << ": ";
                            if (funcDecl->parameters[i].type)
                            {
                                sig << funcDecl->parameters[i].type->name;
                            }
                        }
                        sig << ") -> ";
                        if (funcDecl->returnType)
                        {
                            sig << funcDecl->returnType->name;
                        }
                        else
                        {
                            sig << "void";
                        }
                        sig << "\n```\n\n";
                        sig << "Function defined in this file";
                        hover.contents = sig.str();
                        return hover;
                    }
                }

                if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl))
                {
                    if (structDecl->name == identifier)
                    {
                        std::ostringstream sig;
                        sig << "**" << structDecl->name << "**\n\n";
                        sig << "```flow\n";
                        sig << "struct " << structDecl->name << " {\n";
                        for (const auto& field : structDecl->fields)
                        {
                            sig << "    " << field.name << ": ";
                            if (field.type)
                            {
                                sig << field.type->name;
                            }
                            sig << "\n";
                        }
                        sig << "}\n```\n\n";
                        sig << "Struct defined in this file";
                        hover.contents = sig.str();
                        return hover;
                    }
                }

                if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl))
                {
                    if (typedefDecl->name == identifier)
                    {
                        std::ostringstream sig;
                        sig << "**" << typedefDecl->name << "**\n\n";
                        sig << "```flow\n";
                        sig << "type " << typedefDecl->name << " = ";
                        if (typedefDecl->aliasedType)
                        {
                            sig << typedefDecl->aliasedType->name;
                        }
                        sig << "\n```\n\n";
                        sig << "Type alias defined in this file";
                        hover.contents = sig.str();
                        return hover;
                    }
                }
            }

            // Check for built-in types and functions
            if (identifier == "int" || identifier == "float" || identifier == "string" || identifier == "bool" ||
                identifier == "void")
            {
                hover.contents = "**" + identifier + "**\n\nBuilt-in type in Flow language";
                return hover;
            }

            // Check for stdlib functions
            std::map<std::string, std::string> stdlibFuncs = {
                {"println", "println(message: string) -> void\n\nPrints a message to stdout"},
                {"print", "print(message: string) -> void\n\nPrints a message to stdout without newline"},
                {"readLine", "readLine() -> string\n\nReads a line from stdin"},
                {"readInt", "readInt() -> int\n\nReads an integer from stdin"},
                {"abs", "abs(n: int) -> int\n\nReturns absolute value"},
                {"sqrt", "sqrt(x: float) -> float\n\nReturns square root"},
                {"pow", "pow(base: float, exp: float) -> float\n\nReturns base raised to exp"},
                {"min", "min(a: int, b: int) -> int\n\nReturns minimum of two values"},
                {"max", "max(a: int, b: int) -> int\n\nReturns maximum of two values"},
                {"len", "len(array: T[]) -> int\n\nReturns length of array"},
                {"substr", "substr(s: string, start: int, len: int) -> string\n\nReturns substring"},
                {"concat", "concat(s1: string, s2: string) -> string\n\nConcatenates two strings"}
            };

            auto it = stdlibFuncs.find(identifier);
            if (it != stdlibFuncs.end())
            {
                hover.contents = "**" + identifier + "**\n\n```flow\n" + it->second +
                    "\n```\n\nStandard library function";
                return hover;
            }

            hover.contents = "**" + identifier + "**\n\nUnknown identifier";
            return hover;
        }

        std::string LanguageServer::handleTextDocumentHover(const std::string& params)
        {
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

        std::vector<Location> LanguageServer::getDefinition(const std::string& uri, Position pos)
        {
            std::vector<Location> locations;

            auto docIt = documents.find(uri);
            if (docIt == documents.end() || !docIt->second.ast)
            {
                return locations;
            }

            auto& doc = docIt->second;
            std::string identifier = extractIdentifierAtPosition(doc.text, pos);
            if (identifier.empty())
            {
                return locations;
            }

            for (auto& decl : doc.ast->declarations)
            {
                if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl))
                {
                    if (funcDecl->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                        loc.range.end = Position(funcDecl->location.line - 1,
                                                 funcDecl->location.column + identifier.length());
                        locations.push_back(loc);
                        return locations;
                    }
                }

                if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl))
                {
                    if (structDecl->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(structDecl->location.line - 1, structDecl->location.column);
                        loc.range.end = Position(structDecl->location.line - 1,
                                                 structDecl->location.column + identifier.length());
                        locations.push_back(loc);
                        return locations;
                    }

                    for (const auto& field : structDecl->fields)
                    {
                        if (field.name == identifier)
                        {
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

                if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl))
                {
                    if (typedefDecl->name == identifier)
                    {
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

        static std::string extractIdentifierAtPosition(const std::string& text, Position pos)
        {
            std::vector<std::string> lines;
            std::string line;
            std::istringstream stream(text);
            while (std::getline(stream, line))
            {
                lines.push_back(line);
            }

            if (pos.line >= static_cast<int>(lines.size()))
            {
                return "";
            }

            const std::string& currentLine = lines[pos.line];
            if (pos.character >= static_cast<int>(currentLine.length()))
            {
                return "";
            }

            int start = pos.character;
            int end = pos.character;

            while (start > 0 && (std::isalnum(currentLine[start - 1]) || currentLine[start - 1] == '_'))
            {
                start--;
            }

            while (end < static_cast<int>(currentLine.length()) &&
                (std::isalnum(currentLine[end]) || currentLine[end] == '_'))
            {
                end++;
            }

            if (start >= end)
            {
                return "";
            }

            return currentLine.substr(start, end - start);
        }

        std::string LanguageServer::handleTextDocumentDefinition(const std::string& params)
        {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto locations = getDefinition(uri, Position(line, character));

            if (locations.empty())
            {
                return "null";
            }

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < locations.size(); i++)
            {
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

        std::vector<Location> LanguageServer::getReferences(const std::string& uri, Position pos)
        {
            std::vector<Location> locations;

            auto docIt = documents.find(uri);
            if (docIt == documents.end() || !docIt->second.ast)
            {
                return locations;
            }

            auto& doc = docIt->second;
            std::string identifier = extractIdentifierAtPosition(doc.text, pos);
            if (identifier.empty())
            {
                return locations;
            }

            std::function < void(std::shared_ptr<Expr>) > searchExprForReferences;
            searchExprForReferences = [&](std::shared_ptr<Expr> expr)
            {
                if (!expr) return;

                if (auto idExpr = std::dynamic_pointer_cast<IdentifierExpr>(expr))
                {
                    if (idExpr->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(idExpr->location.line - 1, idExpr->location.column);
                        loc.range.end = Position(idExpr->location.line - 1,
                                                 idExpr->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                }
                else if (auto binExpr = std::dynamic_pointer_cast<BinaryExpr>(expr))
                {
                    searchExprForReferences(binExpr->left);
                    searchExprForReferences(binExpr->right);
                }
                else if (auto unaryExpr = std::dynamic_pointer_cast<UnaryExpr>(expr))
                {
                    searchExprForReferences(unaryExpr->operand);
                }
                else if (auto callExpr = std::dynamic_pointer_cast<CallExpr>(expr))
                {
                    searchExprForReferences(callExpr->callee);
                    for (auto& arg : callExpr->arguments)
                    {
                        searchExprForReferences(arg);
                    }
                }
                else if (auto memberExpr = std::dynamic_pointer_cast<MemberAccessExpr>(expr))
                {
                    searchExprForReferences(memberExpr->object);
                }
                else if (auto structExpr = std::dynamic_pointer_cast<StructInitExpr>(expr))
                {
                    for (auto& fieldVal : structExpr->fieldValues)
                    {
                        searchExprForReferences(fieldVal);
                    }
                }
                else if (auto arrayExpr = std::dynamic_pointer_cast<ArrayLiteralExpr>(expr))
                {
                    for (auto& elem : arrayExpr->elements)
                    {
                        searchExprForReferences(elem);
                    }
                }
                else if (auto indexExpr = std::dynamic_pointer_cast<IndexExpr>(expr))
                {
                    searchExprForReferences(indexExpr->array);
                    searchExprForReferences(indexExpr->index);
                }
            };

            std::function < void(std::shared_ptr<Stmt>) > searchStmtForReferences;
            searchStmtForReferences = [&](std::shared_ptr<Stmt> stmt)
            {
                if (!stmt) return;

                if (auto exprStmt = std::dynamic_pointer_cast<ExprStmt>(stmt))
                {
                    searchExprForReferences(exprStmt->expression);
                }
                else if (auto varDecl = std::dynamic_pointer_cast<VarDeclStmt>(stmt))
                {
                    if (varDecl->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(varDecl->location.line - 1, varDecl->location.column);
                        loc.range.end = Position(varDecl->location.line - 1,
                                                 varDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                    searchExprForReferences(varDecl->initializer);
                }
                else if (auto assignStmt = std::dynamic_pointer_cast<AssignmentStmt>(stmt))
                {
                    if (assignStmt->target == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(assignStmt->location.line - 1, assignStmt->location.column);
                        loc.range.end = Position(assignStmt->location.line - 1,
                                                 assignStmt->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                    searchExprForReferences(assignStmt->value);
                }
                else if (auto returnStmt = std::dynamic_pointer_cast<ReturnStmt>(stmt))
                {
                    searchExprForReferences(returnStmt->value);
                }
                else if (auto ifStmt = std::dynamic_pointer_cast<IfStmt>(stmt))
                {
                    searchExprForReferences(ifStmt->condition);
                    for (auto& s : ifStmt->thenBranch) searchStmtForReferences(s);
                    for (auto& s : ifStmt->elseBranch) searchStmtForReferences(s);
                }
                else if (auto whileStmt = std::dynamic_pointer_cast<WhileStmt>(stmt))
                {
                    searchExprForReferences(whileStmt->condition);
                    for (auto& s : whileStmt->body) searchStmtForReferences(s);
                }
                else if (auto forStmt = std::dynamic_pointer_cast<ForStmt>(stmt))
                {
                    if (forStmt->iteratorVar == identifier)
                    {
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
                    for (auto& s : forStmt->body) searchStmtForReferences(s);
                }
                else if (auto blockStmt = std::dynamic_pointer_cast<BlockStmt>(stmt))
                {
                    for (auto& s : blockStmt->statements) searchStmtForReferences(s);
                }
            };

            for (auto& decl : doc.ast->declarations)
            {
                if (auto funcDecl = std::dynamic_pointer_cast<FunctionDecl>(decl))
                {
                    if (funcDecl->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                        loc.range.end = Position(funcDecl->location.line - 1,
                                                 funcDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }

                    for (const auto& param : funcDecl->parameters)
                    {
                        if (param.name == identifier)
                        {
                            Location loc;
                            loc.uri = uri;
                            loc.range.start = Position(funcDecl->location.line - 1, funcDecl->location.column);
                            loc.range.end = Position(funcDecl->location.line - 1,
                                                     funcDecl->location.column + identifier.length());
                            locations.push_back(loc);
                        }
                    }

                    for (auto& stmt : funcDecl->body)
                    {
                        searchStmtForReferences(stmt);
                    }
                }
                else if (auto structDecl = std::dynamic_pointer_cast<StructDecl>(decl))
                {
                    if (structDecl->name == identifier)
                    {
                        Location loc;
                        loc.uri = uri;
                        loc.range.start = Position(structDecl->location.line - 1, structDecl->location.column);
                        loc.range.end = Position(structDecl->location.line - 1,
                                                 structDecl->location.column + identifier.length());
                        locations.push_back(loc);
                    }
                }
                else if (auto typedefDecl = std::dynamic_pointer_cast<TypeDefDecl>(decl))
                {
                    if (typedefDecl->name == identifier)
                    {
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

        std::string LanguageServer::handleTextDocumentReferences(const std::string& params)
        {
            std::string textDocument = extractJSONField(params, "textDocument");
            std::string uri = extractJSONField(textDocument, "uri");
            std::string position = extractJSONField(params, "position");
            int line = extractJSONInt(position, "line");
            int character = extractJSONInt(position, "character");

            auto locations = getReferences(uri, Position(line, character));

            std::ostringstream oss;
            oss << "[";
            for (size_t i = 0; i < locations.size(); i++)
            {
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