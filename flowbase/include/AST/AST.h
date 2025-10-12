#ifndef FLOW_AST_H
#define FLOW_AST_H

#include <string>
#include <vector>
#include <memory>
#include "../Lexer/Token.h"

namespace flow {
    // Forward declarations
    class ASTVisitor;


    // ============================================================

    class ASTNode {
    public:
        SourceLocation location;

        ASTNode(const SourceLocation &loc) : location(loc) {
        }

        virtual ~ASTNode() = default;

        virtual void accept(ASTVisitor &visitor) = 0;
    };

    // ============================================================
    // TYPE SYSTEM
    // ============================================================

    enum class TypeKind {
        INT,
        FLOAT,
        STRING,
        BOOL,
        VOID,
        STRUCT,
        FUNCTION,
        ARRAY,
        UNKNOWN
    };

    class Type {
    public:
        TypeKind kind;
        std::string name;
        std::vector<std::shared_ptr<Type> > typeParams; // For Option<T>, etc.

        Type(TypeKind k, const std::string &n = "") : kind(k), name(n) {
        }

        bool isNumeric() const { return kind == TypeKind::INT || kind == TypeKind::FLOAT; }
        bool isVoid() const { return kind == TypeKind::VOID; }

        std::string toString() const;
    };

    // ============================================================
    // EXPRESSIONS
    // ============================================================

    class Expr : public ASTNode {
    public:
        std::shared_ptr<Type> type; // Type inference result

        Expr(const SourceLocation &loc) : ASTNode(loc), type(nullptr) {
        }
    };

    class IntLiteralExpr : public Expr {
    public:
        int value;

        IntLiteralExpr(int val, const SourceLocation &loc) : Expr(loc), value(val) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class FloatLiteralExpr : public Expr {
    public:
        double value;

        FloatLiteralExpr(double val, const SourceLocation &loc) : Expr(loc), value(val) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class StringLiteralExpr : public Expr {
    public:
        std::string value;

        StringLiteralExpr(const std::string &val, const SourceLocation &loc) : Expr(loc), value(val) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class BoolLiteralExpr : public Expr {
    public:
        bool value;

        BoolLiteralExpr(bool val, const SourceLocation &loc) : Expr(loc), value(val) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class IdentifierExpr : public Expr {
    public:
        std::string name;

        IdentifierExpr(const std::string &n, const SourceLocation &loc) : Expr(loc), name(n) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class BinaryExpr : public Expr {
    public:
        std::shared_ptr<Expr> left;
        TokenType op;
        std::shared_ptr<Expr> right;

        BinaryExpr(std::shared_ptr<Expr> l, TokenType o, std::shared_ptr<Expr> r, const SourceLocation &loc)
            : Expr(loc), left(l), op(o), right(r) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class UnaryExpr : public Expr {
    public:
        TokenType op;
        std::shared_ptr<Expr> operand;

        UnaryExpr(TokenType o, std::shared_ptr<Expr> operand, const SourceLocation &loc)
            : Expr(loc), op(o), operand(operand) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class CallExpr : public Expr {
    public:
        std::shared_ptr<Expr> callee;
        std::vector<std::shared_ptr<Expr> > arguments;

        CallExpr(std::shared_ptr<Expr> c, std::vector<std::shared_ptr<Expr> > args, const SourceLocation &loc)
            : Expr(loc), callee(c), arguments(args) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class MemberAccessExpr : public Expr {
    public:
        std::shared_ptr<Expr> object;
        std::string member;

        MemberAccessExpr(std::shared_ptr<Expr> obj, const std::string &mem, const SourceLocation &loc)
            : Expr(loc), object(obj), member(mem) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class StructInitExpr : public Expr {
    public:
        std::string structName;
        std::vector<std::shared_ptr<Expr> > fieldValues;

        StructInitExpr(const std::string &name, std::vector<std::shared_ptr<Expr> > fields, const SourceLocation &loc)
            : Expr(loc), structName(name), fieldValues(fields) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class ArrayLiteralExpr : public Expr {
    public:
        std::vector<std::shared_ptr<Expr> > elements;

        ArrayLiteralExpr(std::vector<std::shared_ptr<Expr> > elems, const SourceLocation &loc)
            : Expr(loc), elements(elems) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class IndexExpr : public Expr {
    public:
        std::shared_ptr<Expr> array;
        std::shared_ptr<Expr> index;

        IndexExpr(std::shared_ptr<Expr> arr, std::shared_ptr<Expr> idx, const SourceLocation &loc)
            : Expr(loc), array(arr), index(idx) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // ============================================================
    // STATEMENTS
    // ============================================================

    class Stmt : public ASTNode {
    public:
        Stmt(const SourceLocation &loc) : ASTNode(loc) {
        }
    };

    class ExprStmt : public Stmt {
    public:
        std::shared_ptr<Expr> expression;

        ExprStmt(std::shared_ptr<Expr> expr, const SourceLocation &loc)
            : Stmt(loc), expression(expr) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class VarDeclStmt : public Stmt {
    public:
        std::string name;
        bool isMutable;
        std::shared_ptr<Type> declaredType;
        std::shared_ptr<Expr> initializer;

        VarDeclStmt(const std::string &n, bool mut, std::shared_ptr<Type> t,
                    std::shared_ptr<Expr> init, const SourceLocation &loc)
            : Stmt(loc), name(n), isMutable(mut), declaredType(t), initializer(init) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class AssignmentStmt : public Stmt {
    public:
        std::string target;
        std::shared_ptr<Expr> value;

        AssignmentStmt(const std::string &t, std::shared_ptr<Expr> v, const SourceLocation &loc)
            : Stmt(loc), target(t), value(v) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class ReturnStmt : public Stmt {
    public:
        std::shared_ptr<Expr> value;

        ReturnStmt(std::shared_ptr<Expr> val, const SourceLocation &loc)
            : Stmt(loc), value(val) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class IfStmt : public Stmt {
    public:
        std::shared_ptr<Expr> condition;
        std::vector<std::shared_ptr<Stmt> > thenBranch;
        std::vector<std::shared_ptr<Stmt> > elseBranch;

        IfStmt(std::shared_ptr<Expr> cond, std::vector<std::shared_ptr<Stmt> > thenB,
               std::vector<std::shared_ptr<Stmt> > elseB, const SourceLocation &loc)
            : Stmt(loc), condition(cond), thenBranch(thenB), elseBranch(elseB) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class ForStmt : public Stmt {
    public:
        std::string iteratorVar;
        std::shared_ptr<Expr> rangeStart;
        std::shared_ptr<Expr> rangeEnd;
        std::shared_ptr<Expr> iterable; // For array iteration
        std::vector<std::shared_ptr<Stmt> > body;

        ForStmt(const std::string &var, const SourceLocation &loc)
            : Stmt(loc), iteratorVar(var), rangeStart(nullptr), rangeEnd(nullptr), iterable(nullptr) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class WhileStmt : public Stmt {
    public:
        std::shared_ptr<Expr> condition;
        std::vector<std::shared_ptr<Stmt> > body;

        WhileStmt(std::shared_ptr<Expr> cond, std::vector<std::shared_ptr<Stmt> > b, const SourceLocation &loc)
            : Stmt(loc), condition(cond), body(b) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class BlockStmt : public Stmt {
    public:
        std::vector<std::shared_ptr<Stmt> > statements;

        BlockStmt(std::vector<std::shared_ptr<Stmt> > stmts, const SourceLocation &loc)
            : Stmt(loc), statements(stmts) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // ============================================================
    // DECLARATIONS
    // ============================================================

    class Decl : public ASTNode {
    public:
        std::string name;

        Decl(const std::string &n, const SourceLocation &loc) : ASTNode(loc), name(n) {
        }
    };

    class Parameter {
    public:
        std::string name;
        std::shared_ptr<Type> type;

        Parameter(const std::string &n, std::shared_ptr<Type> t) : name(n), type(t) {
        }
    };

    class FunctionDecl : public Decl {
    public:
        std::vector<Parameter> parameters;
        std::shared_ptr<Type> returnType;
        std::vector<std::shared_ptr<Stmt> > body;
        bool isAsync;
        bool isExported;
        std::string abi; // For exported functions

        FunctionDecl(const std::string &n, const SourceLocation &loc)
            : Decl(n, loc), isAsync(false), isExported(false), abi("") {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class StructField {
    public:
        std::shared_ptr<Type> type;
        std::string name;

        StructField(std::shared_ptr<Type> t, const std::string &n) : type(t), name(n) {
        }
    };

    class StructDecl : public Decl {
    public:
        std::vector<StructField> fields;

        StructDecl(const std::string &n, std::vector<StructField> f, const SourceLocation &loc)
            : Decl(n, loc), fields(f) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    class LinkDecl : public Decl {
    public:
        std::string adapter; // "c", "python", "js"
        std::string module; // Optional module name
        std::string inlineCode; // For inline blocks
        std::vector<std::shared_ptr<FunctionDecl> > functions;

        LinkDecl(const std::string &adp, const std::string &mod, const SourceLocation &loc)
            : Decl("", loc), adapter(adp), module(mod), inlineCode("") {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // Type definition declaration
    class TypeDefDecl : public Decl {
    public:
        std::shared_ptr<Type> aliasedType;

        TypeDefDecl(const std::string &name, std::shared_ptr<Type> aliasedType, const SourceLocation &loc)
            : Decl(name, loc), aliasedType(aliasedType) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // Import declaration - for multi-file support
    class ImportDecl : public Decl {
    public:
        std::string modulePath; // Path to the file being imported
        std::vector<std::string> imports; // Specific symbols to import (empty means import all)
        std::string alias; // Optional alias for the module

        ImportDecl(const std::string &path, const SourceLocation &loc)
            : Decl("", loc), modulePath(path), alias("") {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // Module declaration - defines the current module name
    class ModuleDecl : public Decl {
    public:
        ModuleDecl(const std::string &name, const SourceLocation &loc)
            : Decl(name, loc) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // ============================================================
    // PROGRAM ROOT
    // ============================================================

    class Program : public ASTNode {
    public:
        std::vector<std::shared_ptr<Decl> > declarations;

        Program(const SourceLocation &loc) : ASTNode(loc) {
        }

        void accept(ASTVisitor &visitor) override;
    };

    // ============================================================
    // VISITOR PATTERN
    // ============================================================

    class ASTVisitor {
    public:
        virtual ~ASTVisitor() = default;

        // Expressions
        virtual void visit(IntLiteralExpr &node) = 0;

        virtual void visit(FloatLiteralExpr &node) = 0;

        virtual void visit(StringLiteralExpr &node) = 0;

        virtual void visit(BoolLiteralExpr &node) = 0;

        virtual void visit(IdentifierExpr &node) = 0;

        virtual void visit(BinaryExpr &node) = 0;

        virtual void visit(UnaryExpr &node) = 0;

        virtual void visit(CallExpr &node) = 0;

        virtual void visit(MemberAccessExpr &node) = 0;

        virtual void visit(StructInitExpr &node) = 0;

        virtual void visit(ArrayLiteralExpr &node) = 0;

        virtual void visit(IndexExpr &node) = 0;

        // Statements
        virtual void visit(ExprStmt &node) = 0;

        virtual void visit(VarDeclStmt &node) = 0;

        virtual void visit(AssignmentStmt &node) = 0;

        virtual void visit(ReturnStmt &node) = 0;

        virtual void visit(IfStmt &node) = 0;

        virtual void visit(ForStmt &node) = 0;

        virtual void visit(WhileStmt &node) = 0;

        virtual void visit(BlockStmt &node) = 0;

        // Declarations
        virtual void visit(FunctionDecl &node) = 0;

        virtual void visit(StructDecl &node) = 0;

        virtual void visit(TypeDefDecl &node) = 0;

        virtual void visit(LinkDecl &node) = 0;

        virtual void visit(ImportDecl &node) = 0;

        virtual void visit(ModuleDecl &node) = 0;

        virtual void visit(Program &node) = 0;
    };
} // namespace flow

#endif // FLOW_AST_H