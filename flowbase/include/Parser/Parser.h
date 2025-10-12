#ifndef FLOW_PARSER_H
#define FLOW_PARSER_H

#include "../Lexer/Lexer.h"
#include "../Lexer/Token.h"
#include "../AST/AST.h"
#include <memory>
#include <vector>
#include <stdexcept>

namespace flow {

class ParseError : public std::runtime_error {
public:
    SourceLocation location;
    
    ParseError(const std::string& msg, const SourceLocation& loc)
        : std::runtime_error(msg), location(loc) {}
};

class Parser {
private:
    std::vector<Token> tokens;
    size_t current;
    
    Token peek() const;
    Token previous() const;
    bool isAtEnd() const;
    Token advance();
    bool check(TokenType type) const;
    bool match(TokenType type);
    Token consume(TokenType type, const std::string& message);
    
    ParseError error(const Token& token, const std::string& message);
    void synchronize();
    
    // TODO: Implement parsing methods
    std::shared_ptr<Program> parseProgram();
    std::shared_ptr<Decl> parseDeclaration();
    std::shared_ptr<FunctionDecl> parseFunctionDecl();
    std::shared_ptr<StructDecl> parseStructDecl();
    std::shared_ptr<TypeDefDecl> parseTypeDefDecl();
    std::shared_ptr<LinkDecl> parseLinkDecl();
    std::shared_ptr<ImportDecl> parseImportDecl();
    std::shared_ptr<ModuleDecl> parseModuleDecl();
    
    std::shared_ptr<Stmt> parseStatement();
    std::shared_ptr<VarDeclStmt> parseVarDecl();
    std::shared_ptr<ReturnStmt> parseReturnStmt();
    std::shared_ptr<IfStmt> parseIfStmt();
    std::shared_ptr<ForStmt> parseForStmt();
    std::shared_ptr<WhileStmt> parseWhileStmt();
    std::shared_ptr<BlockStmt> parseBlockStmt();
    std::shared_ptr<Stmt> parseExprStmt();
    
    std::shared_ptr<Expr> parseExpression();
    std::shared_ptr<Expr> parseAssignment();
    std::shared_ptr<Expr> parseLogicalOr();
    std::shared_ptr<Expr> parseLogicalAnd();
    std::shared_ptr<Expr> parseEquality();
    std::shared_ptr<Expr> parseComparison();
    std::shared_ptr<Expr> parseTerm();
    std::shared_ptr<Expr> parseFactor();
    std::shared_ptr<Expr> parseUnary();
    std::shared_ptr<Expr> parseCall();
    std::shared_ptr<Expr> parsePrimary();
    
    std::shared_ptr<Type> parseType();
    Parameter parseParameter();
    
public:
    Parser(const std::vector<Token>& toks) : tokens(toks), current(0) {}
    
    std::shared_ptr<Program> parse();
};

} // namespace flow

#endif // FLOW_PARSER_H

