#ifndef FLOW_SEMANTIC_ANALYZER_H
#define FLOW_SEMANTIC_ANALYZER_H

#include "../AST/AST.h"
#include <map>
#include <string>
#include <vector>
#include <memory>
#include <stdexcept>

namespace flow {
    class SemanticError : public std::runtime_error {
    public:
        SourceLocation location;

        SemanticError(const std::string &msg, const SourceLocation &loc)
            : std::runtime_error(msg), location(loc) {
        }
    };

    // Symbol table for tracking variables, functions, and types
    class SymbolTable {
    public:
        struct Symbol {
            std::string name;
            std::shared_ptr<Type> type;
            bool isMutable;
            bool isFunction;

            Symbol() : name(""), type(nullptr), isMutable(false), isFunction(false) {
            }

            Symbol(const std::string &n, std::shared_ptr<Type> t, bool mut = false, bool func = false)
                : name(n), type(t), isMutable(mut), isFunction(func) {
            }
        };

    private:
        std::vector<std::map<std::string, Symbol> > scopes;

    public:
        SymbolTable() { enterScope(); }

        void enterScope();

        void exitScope();

        void define(const std::string &name, std::shared_ptr<Type> type, bool isMutable = false,
                    bool isFunction = false);

        Symbol *lookup(const std::string &name);

        bool isDefined(const std::string &name);

        bool isMutable(const std::string &name);
    };

    class SemanticAnalyzer : public ASTVisitor {
    private:
        SymbolTable symbolTable;
        std::shared_ptr<Type> currentFunctionReturnType;
        std::vector<std::string> errors;

        // Struct field tracking: structName -> (fieldName -> fieldType)
        std::map<std::string, std::map<std::string, std::shared_ptr<Type> > > structFields;

        // Type aliases: aliasName -> actualType
        std::map<std::string, std::shared_ptr<Type> > typeAliases;

        // Module tracking: modulePath -> parsed Program
        std::map<std::string, std::shared_ptr<Program> > loadedModules;

        // Current file directory for resolving relative imports
        std::string currentDirectory;
        
        // Library paths for import resolution
        std::vector<std::string> libraryPaths;

        void reportError(const std::string &message, const SourceLocation &loc);

        bool typesMatch(std::shared_ptr<Type> t1, std::shared_ptr<Type> t2);

        std::shared_ptr<Type> resolveTypeAlias(std::shared_ptr<Type> type);

        // Module loading helpers
        std::shared_ptr<Program> loadModule(const std::string &modulePath);

        std::string resolveModulePath(const std::string &importPath);

        void importSymbolsFrom(std::shared_ptr<Program> module, const std::vector<std::string> &symbols,
                               const std::string &alias);

    public:
        SemanticAnalyzer() : currentFunctionReturnType(nullptr), currentDirectory(".") {
        }

        void analyze(std::shared_ptr<Program> program);

        void setCurrentFile(const std::string &filePath);
        
        void setLibraryPaths(const std::vector<std::string> &paths) {
            libraryPaths = paths;
        }

        const std::vector<std::string> &getErrors() const { return errors; }
        bool hasErrors() const { return !errors.empty(); }

        // Get loaded modules for multi-file compilation
        const std::map<std::string, std::shared_ptr<Program> > &getLoadedModules() const { return loadedModules; }

        // Expression visitors
        void visit(IntLiteralExpr &node) override;

        void visit(FloatLiteralExpr &node) override;

        void visit(StringLiteralExpr &node) override;

        void visit(BoolLiteralExpr &node) override;

        void visit(IdentifierExpr &node) override;

        void visit(BinaryExpr &node) override;

        void visit(UnaryExpr &node) override;

        void visit(CallExpr &node) override;

        void visit(MemberAccessExpr &node) override;

        void visit(StructInitExpr &node) override;

        void visit(ArrayLiteralExpr &node) override;

        void visit(IndexExpr &node) override;

        // Statement visitors
        void visit(ExprStmt &node) override;

        void visit(VarDeclStmt &node) override;

        void visit(AssignmentStmt &node) override;

        void visit(ReturnStmt &node) override;

        void visit(IfStmt &node) override;

        void visit(ForStmt &node) override;

        void visit(WhileStmt &node) override;

        void visit(BlockStmt &node) override;

        // Declaration visitors
        void visit(FunctionDecl &node) override;

        void visit(StructDecl &node) override;

        void visit(TypeDefDecl &node) override;

        void visit(LinkDecl &node) override;

        void visit(ImportDecl &node) override;

        void visit(ModuleDecl &node) override;

        void visit(Program &node) override;
    };
} // namespace flow

#endif // FLOW_SEMANTIC_ANALYZER_H