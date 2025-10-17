#include "../../include/Parser/Parser.h"
#include "../../include/Common/ErrorReporter.h"
#include "../../include/LSP/LSPErrorCollector.h"
#include <iostream>

namespace flow
{
    Token Parser::peek() const
    {
        return tokens[current];
    }

    Token Parser::previous() const
    {
        return tokens[current - 1];
    }

    bool Parser::isAtEnd() const
    {
        return peek().type == TokenType::END_OF_FILE;
    }

    Token Parser::advance()
    {
        if (!isAtEnd()) current++;
        return previous();
    }

    bool Parser::check(TokenType type) const
    {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    bool Parser::match(TokenType type)
    {
        if (check(type))
        {
            advance();
            return true;
        }
        return false;
    }

    Token Parser::consume(TokenType type, const std::string& message)
    {
        if (check(type)) return advance();
        if (errorCollector)
        {
            // When using error collector, don't throw - just report and return a dummy token
            errorCollector->reportError("Parse", message, peek().location);
            return Token(TokenType::INVALID, "", peek().location);
        }
        else
        {
            throw error(peek(), message);
        }
    }

    ParseError Parser::error(const Token& token, const std::string& message)
    {
        if (errorCollector)
        {
            errorCollector->reportError("Parse", message, token.location);
            // Don't throw exception when using error collector
            return ParseError(message, token.location);
        }
        else
        {
            ErrorReporter::instance().reportError("Parse", message, token.location);
            return ParseError(message, token.location);
        }
    }

    void Parser::synchronize()
    {
        advance();

        while (!isAtEnd())
        {
            if (previous().type == TokenType::SEMICOLON) return;

            switch (peek().type)
            {
            case TokenType::KW_FUNC:
            case TokenType::KW_STRUCT:
            case TokenType::KW_LET:
            case TokenType::KW_MUT:
            case TokenType::KW_RETURN:
            case TokenType::KW_IF:
            case TokenType::KW_FOR:
            case TokenType::KW_WHILE:
                return;
            default:
                break;
            }

            advance();
        }
    }


    std::shared_ptr<Program> Parser::parse()
    {
        auto program = std::make_shared<Program>(SourceLocation("", 0, 0));


        try
        {
            while (!isAtEnd())
            {
                auto decl = parseDeclaration();
                if (decl)
                {
                    program->declarations.push_back(decl);
                }
            }
        }
        catch (const ParseError& e)
        {
            std::cerr << "Parsing failed: " << e.what() << std::endl;
            return nullptr;
        }

        return program;
    }

    std::shared_ptr<Decl> Parser::parseDeclaration()
    {
        try
        {
            if (match(TokenType::KW_EXPORT))
            {
            }

            if (match(TokenType::KW_IMPORT))
            {
                return parseImportDecl();
            }
            if (match(TokenType::KW_MODULE))
            {
                return parseModuleDecl();
            }
            if (match(TokenType::KW_FUNC))
            {
                return parseFunctionDecl();
            }
            if (match(TokenType::KW_STRUCT))
            {
                return parseStructDecl();
            }
            if (match(TokenType::KW_IMPL))
            {
                return parseImplDecl();
            }
            if (match(TokenType::KW_TYPE))
            {
                return parseTypeDefDecl();
            }
            if (match(TokenType::KW_LINK))
            {
                return parseLinkDecl();
            }


            auto stmt = parseStatement();
            if (stmt)
            {
            }
        }
        catch (const ParseError& e)
        {
            synchronize();
            return nullptr;
        }

        return nullptr;
    }

    std::shared_ptr<FunctionDecl> Parser::parseFunctionDecl()
    {
        Token name = consume(TokenType::IDENTIFIER, "Expected function name");
        auto func = std::make_shared<FunctionDecl>(name.lexeme, name.location);

        // Parse parameters
        consume(TokenType::LPAREN, "Expected '(' after function name");

        if (!check(TokenType::RPAREN))
        {
            do
            {
                func->parameters.push_back(parseParameter());
            }
            while (match(TokenType::COMMA));
        }

        consume(TokenType::RPAREN, "Expected ')' after parameters");

        // Parse return type (optional for void functions)
        if (match(TokenType::ARROW))
        {
            func->returnType = parseType();
        }
        else
        {
            func->returnType = std::make_shared<Type>(TypeKind::VOID, "void");
        }

        // Parse function body
        consume(TokenType::LBRACE, "Expected '{' before function body");

        while (!check(TokenType::RBRACE) && !isAtEnd())
        {
            func->body.push_back(parseStatement());
        }

        consume(TokenType::RBRACE, "Expected '}' after function body");

        return func;
    }

    std::shared_ptr<StructDecl> Parser::parseStructDecl()
    {
        Token name = consume(TokenType::IDENTIFIER, "Expected struct name");

        consume(TokenType::LBRACE, "Expected '{' after struct name");

        std::vector<StructField> fields;

        // Parse struct fields
        while (!check(TokenType::RBRACE) && !isAtEnd())
        {
            // Parse type
            std::shared_ptr<Type> fieldType = parseType();

            // Parse field name
            Token fieldName = consume(TokenType::IDENTIFIER, "Expected field name");

            // Expect semicolon after field
            consume(TokenType::SEMICOLON, "Expected ';' after struct field");

            fields.push_back(StructField(fieldType, fieldName.lexeme));
        }

        consume(TokenType::RBRACE, "Expected '}' after struct fields");

        return std::make_shared<StructDecl>(name.lexeme, fields, name.location);
    }

    std::shared_ptr<ImplDecl> Parser::parseImplDecl()
    {
        // impl StructName::methodName(params) -> returnType { body }
        Token structName = consume(TokenType::IDENTIFIER, "Expected struct name after 'impl'");
        consume(TokenType::DOUBLE_COLON, "Expected '::' after struct name");
        Token methodName = consume(TokenType::IDENTIFIER, "Expected method name after '::'");

        auto implDecl = std::make_shared<ImplDecl>(structName.lexeme, methodName.lexeme, structName.location);

        // Parse parameters
        consume(TokenType::LPAREN, "Expected '(' after method name");
        if (!check(TokenType::RPAREN)) {
            do {
                implDecl->parameters.push_back(parseParameter());
            } while (match(TokenType::COMMA));
        }
        consume(TokenType::RPAREN, "Expected ')' after parameters");

        // Parse return type (optional)
        if (match(TokenType::ARROW)) {
            implDecl->returnType = parseType();
        } else {
            implDecl->returnType = std::make_shared<Type>(TypeKind::VOID, "void");
        }

        // Parse body
        consume(TokenType::LBRACE, "Expected '{' before method body");
        while (!check(TokenType::RBRACE) && !isAtEnd()) {
            implDecl->body.push_back(parseStatement());
        }
        consume(TokenType::RBRACE, "Expected '}' after method body");

        return implDecl;
    }

    std::shared_ptr<TypeDefDecl> Parser::parseTypeDefDecl()
    {
        // type UserId = int;
        Token name = consume(TokenType::IDENTIFIER, "Expected type alias name");
        consume(TokenType::ASSIGN, "Expected '=' after type name");
        auto aliasedType = parseType();
        consume(TokenType::SEMICOLON, "Expected ';' after type definition");

        return std::make_shared<TypeDefDecl>(name.lexeme, aliasedType, name.location);
    }

    std::shared_ptr<LinkDecl> Parser::parseLinkDecl()
    {
        Token linkToken = previous(); // 'link' keyword

        // Parse the adapter string: "c", "python:math", "js:dom"
        Token adapterToken = consume(TokenType::STRING_LITERAL, "Expected adapter string after 'link'");
        std::string adapterString = adapterToken.lexeme;

        // Remove quotes from adapter string
        if (adapterString.size() >= 2 && adapterString.front() == '"' && adapterString.back() == '"')
        {
            adapterString = adapterString.substr(1, adapterString.size() - 2);
        }

        // Parse adapter and optional module: "python:math" -> adapter="python", module="math"
        std::string adapter, module;
        size_t colonPos = adapterString.find(':');
        if (colonPos != std::string::npos)
        {
            adapter = adapterString.substr(0, colonPos);
            module = adapterString.substr(colonPos + 1);
        }
        else
        {
            adapter = adapterString;
            module = "";
        }

        auto linkDecl = std::make_shared<LinkDecl>(adapter, module, linkToken.location);

        // Parse the function declarations inside the link block
        consume(TokenType::LBRACE, "Expected '{' after link adapter");

        while (!check(TokenType::RBRACE) && !isAtEnd())
        {
            // Check for inline code: inline """...""";
            if (match(TokenType::KW_INLINE))
            {
                Token codeToken = consume(TokenType::STRING_LITERAL, "Expected inline code string");
                linkDecl->inlineCode = codeToken.lexeme;
                consume(TokenType::SEMICOLON, "Expected ';' after inline code");
                continue;
            }

            // Parse function declaration
            if (match(TokenType::KW_FUNC))
            {
                Token funcName = consume(TokenType::IDENTIFIER, "Expected function name");

                consume(TokenType::LPAREN, "Expected '(' after function name");
                std::vector<Parameter> params;

                if (!check(TokenType::RPAREN))
                {
                    do
                    {
                        // Handle variadic parameters: ...
                        if (match(TokenType::TRIPLE_DOT))
                        {
                            // Variadic parameter
                            params.push_back(Parameter{
                                "__varargs", std::make_shared<Type>(TypeKind::UNKNOWN, "varargs")
                            });
                            break;
                        }
                        else
                        {
                            params.push_back(parseParameter());
                        }
                    }
                    while (match(TokenType::COMMA));
                }

                consume(TokenType::RPAREN, "Expected ')' after parameters");

                // Parse return type
                std::shared_ptr<Type> returnType = std::make_shared<Type>(TypeKind::VOID, "void");
                if (match(TokenType::ARROW))
                {
                    returnType = parseType();
                }

                consume(TokenType::SEMICOLON, "Expected ';' after foreign function declaration");

                // Create function declaration (mark as foreign)
                auto funcDecl = std::make_shared<FunctionDecl>(funcName.lexeme, funcName.location);
                funcDecl->parameters = params;
                funcDecl->returnType = returnType;
                // body is empty for foreign functions
                linkDecl->functions.push_back(funcDecl);
            }
            else
            {
                throw error(peek(), "Expected 'func' or 'inline' in link block");
            }
        }

        consume(TokenType::RBRACE, "Expected '}' after link block");

        return linkDecl;
    }

    std::shared_ptr<ImportDecl> Parser::parseImportDecl()
    {
        // Support multiple import syntaxes:
        // import "path/to/module.flow";
        // import "path/to/module.flow" as alias;
        // import { func1, func2 } from "path/to/module.flow";

        Token startToken = previous();

        // Check for selective imports: import { ... } from "..."
        if (check(TokenType::LBRACE))
        {
            consume(TokenType::LBRACE, "Expected '{'");

            std::vector<std::string> imports;
            do
            {
                Token id = consume(TokenType::IDENTIFIER, "Expected identifier");
                imports.push_back(id.lexeme);
            }
            while (match(TokenType::COMMA));

            consume(TokenType::RBRACE, "Expected '}' after import list");
            consume(TokenType::KW_FROM, "Expected 'from' after import list");

            Token pathToken = consume(TokenType::STRING_LITERAL, "Expected module path string");
            consume(TokenType::SEMICOLON, "Expected ';' after import");

            auto importDecl = std::make_shared<ImportDecl>(pathToken.lexeme, startToken.location);
            importDecl->imports = imports;
            return importDecl;
        }

        // Simple import: import "path";
        Token pathToken = consume(TokenType::STRING_LITERAL, "Expected module path string");
        auto importDecl = std::make_shared<ImportDecl>(pathToken.lexeme, pathToken.location);

        // Check for alias: import "path" as alias;
        if (match(TokenType::KW_AS))
        {
            Token aliasToken = consume(TokenType::IDENTIFIER, "Expected alias identifier");
            importDecl->alias = aliasToken.lexeme;
        }

        consume(TokenType::SEMICOLON, "Expected ';' after import");
        return importDecl;
    }

    std::shared_ptr<ModuleDecl> Parser::parseModuleDecl()
    {
        // module name;
        Token nameToken = consume(TokenType::IDENTIFIER, "Expected module name");
        consume(TokenType::SEMICOLON, "Expected ';' after module declaration");

        return std::make_shared<ModuleDecl>(nameToken.lexeme, nameToken.location);
    }

    std::shared_ptr<Stmt> Parser::parseStatement()
    {
        if (match(TokenType::KW_RETURN))
        {
            return parseReturnStmt();
        }

        if (match(TokenType::KW_LET))
        {
            return parseVarDecl();
        }

        if (match(TokenType::KW_IF))
        {
            return parseIfStmt();
        }

        if (match(TokenType::KW_FOR))
        {
            return parseForStmt();
        }

        if (match(TokenType::KW_WHILE))
        {
            return parseWhileStmt();
        }

        if (check(TokenType::LBRACE))
        {
            return parseBlockStmt();
        }

        return parseExprStmt();
    }

    std::shared_ptr<VarDeclStmt> Parser::parseVarDecl()
    {
        // Check if 'mut' follows 'let'
        bool isMutable = match(TokenType::KW_MUT);

        Token name = consume(TokenType::IDENTIFIER, "Expected variable name");

        // Type annotation is optional for type inference
        std::shared_ptr<Type> type = nullptr;
        if (match(TokenType::COLON))
        {
            type = parseType();
        }

        std::shared_ptr<Expr> initializer = nullptr;
        if (match(TokenType::ASSIGN))
        {
            initializer = parseExpression();
        }

        // If no type annotation and no initializer, error
        if (!type && !initializer)
        {
            throw error(name, "Variable must have either a type annotation or an initializer for type inference");
        }

        consume(TokenType::SEMICOLON, "Expected ';' after variable declaration");
        return std::make_shared<VarDeclStmt>(name.lexeme, isMutable, type, initializer, name.location);
    }

    std::shared_ptr<ReturnStmt> Parser::parseReturnStmt()
    {
        Token keyword = previous(); // 'return' keyword

        std::shared_ptr<Expr> value = nullptr;
        if (!check(TokenType::SEMICOLON))
        {
            value = parseExpression();
        }

        consume(TokenType::SEMICOLON, "Expected ';' after return value");
        return std::make_shared<ReturnStmt>(value, keyword.location);
    }

    std::shared_ptr<IfStmt> Parser::parseIfStmt()
    {
        Token keyword = previous();

        consume(TokenType::LPAREN, "Expected '(' after 'if'");
        auto condition = parseExpression();
        consume(TokenType::RPAREN, "Expected ')' after condition");

        // Parse then branch
        std::vector<std::shared_ptr<Stmt>> thenBranch;
        if (check(TokenType::LBRACE))
        {
            auto block = parseBlockStmt();
            thenBranch = dynamic_cast<BlockStmt*>(block.get())->statements;
        }
        else
        {
            thenBranch.push_back(parseStatement());
        }

        // Parse else branch
        std::vector<std::shared_ptr<Stmt>> elseBranch;
        if (match(TokenType::KW_ELSE))
        {
            if (check(TokenType::LBRACE))
            {
                auto block = parseBlockStmt();
                elseBranch = dynamic_cast<BlockStmt*>(block.get())->statements;
            }
            else
            {
                elseBranch.push_back(parseStatement());
            }
        }

        return std::make_shared<IfStmt>(condition, thenBranch, elseBranch, keyword.location);
    }

    std::shared_ptr<ForStmt> Parser::parseForStmt()
    {
        Token keyword = previous();

        consume(TokenType::LPAREN, "Expected '(' after 'for'");
        Token iterVar = consume(TokenType::IDENTIFIER, "Expected iterator variable");
        consume(TokenType::KW_IN, "Expected 'in' after iterator variable");

        auto forStmt = std::make_shared<ForStmt>(iterVar.lexeme, keyword.location);

        // Parse range or iterable
        auto start = parseExpression();

        if (match(TokenType::DOUBLE_DOT))
        {
            // Range-based loop: for (i in 0..10)
            forStmt->rangeStart = start;
            forStmt->rangeEnd = parseExpression();
        }
        else
        {
            // Iterator-based loop: for (item in array)
            forStmt->iterable = start;
        }

        consume(TokenType::RPAREN, "Expected ')' after for clause");

        // Parse body
        if (check(TokenType::LBRACE))
        {
            auto block = parseBlockStmt();
            forStmt->body = dynamic_cast<BlockStmt*>(block.get())->statements;
        }
        else
        {
            forStmt->body.push_back(parseStatement());
        }

        return forStmt;
    }

    std::shared_ptr<WhileStmt> Parser::parseWhileStmt()
    {
        Token keyword = previous(); // 'while' keyword

        consume(TokenType::LPAREN, "Expected '(' after 'while'");
        auto condition = parseExpression();
        consume(TokenType::RPAREN, "Expected ')' after condition");

        // Parse body
        std::vector<std::shared_ptr<Stmt>> body;
        if (check(TokenType::LBRACE))
        {
            auto block = parseBlockStmt();
            body = dynamic_cast<BlockStmt*>(block.get())->statements;
        }
        else
        {
            body.push_back(parseStatement());
        }

        return std::make_shared<WhileStmt>(condition, body, keyword.location);
    }

    std::shared_ptr<BlockStmt> Parser::parseBlockStmt()
    {
        Token lbrace = consume(TokenType::LBRACE, "Expected '{'");
        std::vector<std::shared_ptr<Stmt>> statements;

        while (!check(TokenType::RBRACE) && !isAtEnd())
        {
            statements.push_back(parseStatement());
        }

        consume(TokenType::RBRACE, "Expected '}'");
        return std::make_shared<BlockStmt>(statements, lbrace.location);
    }

    std::shared_ptr<Stmt> Parser::parseExprStmt()
    {
        // Check if this is an assignment statement
        if (check(TokenType::IDENTIFIER))
        {
            Token id = peek();
            int savedPos = current;
            advance(); // consume identifier

            if (check(TokenType::ASSIGN))
            {
                // This is an assignment statement
                advance(); // consume '='
                auto value = parseExpression();
                consume(TokenType::SEMICOLON, "Expected ';' after assignment");
                return std::make_shared<AssignmentStmt>(id.lexeme, value, id.location);
            }

            // Not an assignment, backtrack
            current = savedPos;
        }

        // Regular expression statement
        auto expr = parseExpression();
        consume(TokenType::SEMICOLON, "Expected ';' after expression");
        return std::make_shared<ExprStmt>(expr, expr->location);
    }

    std::shared_ptr<Expr> Parser::parseExpression()
    {
        return parseAssignment();
    }

    std::shared_ptr<Expr> Parser::parseAssignment()
    {
        auto expr = parseLogicalOr();

        if (match(TokenType::ASSIGN))
        {
            Token equals = previous();
            auto value = parseAssignment();

            if (auto* idExpr = dynamic_cast<IdentifierExpr*>(expr.get()))
            {
                // Assignment to variable - we'll handle this as a statement later
                // For now, return a binary expression
                return std::make_shared<BinaryExpr>(expr, equals.type, value, equals.location);
            }

            throw error(equals, "Invalid assignment target");
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseLogicalOr()
    {
        auto expr = parseLogicalAnd();

        while (match(TokenType::OR))
        {
            Token op = previous();
            auto right = parseLogicalAnd();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseLogicalAnd()
    {
        auto expr = parseBitwiseOr();

        while (match(TokenType::AND))
        {
            Token op = previous();
            auto right = parseBitwiseOr();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseBitwiseOr()
    {
        auto expr = parseBitwiseXor();

        while (match(TokenType::PIPE))
        {
            Token op = previous();
            auto right = parseBitwiseXor();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseBitwiseXor()
    {
        auto expr = parseBitwiseAnd();

        while (match(TokenType::CARET))
        {
            Token op = previous();
            auto right = parseBitwiseAnd();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseBitwiseAnd()
    {
        auto expr = parseEquality();

        while (match(TokenType::AMPERSAND))
        {
            Token op = previous();
            auto right = parseEquality();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseEquality()
    {
        auto expr = parseComparison();

        while (match(TokenType::EQ) || match(TokenType::NE))
        {
            Token op = previous();
            auto right = parseComparison();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseComparison()
    {
        auto expr = parseBitwiseShift();

        while (match(TokenType::LT) || match(TokenType::LE) ||
            match(TokenType::GT) || match(TokenType::GE))
        {
            Token op = previous();
            auto right = parseBitwiseShift();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseBitwiseShift()
    {
        auto expr = parseTerm();

        while (match(TokenType::LEFT_SHIFT) || match(TokenType::RIGHT_SHIFT))
        {
            Token op = previous();
            auto right = parseTerm();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseTerm()
    {
        auto expr = parseFactor();

        while (match(TokenType::PLUS) || match(TokenType::MINUS))
        {
            Token op = previous();
            auto right = parseFactor();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseFactor()
    {
        auto expr = parseUnary();

        while (match(TokenType::STAR) || match(TokenType::SLASH) || match(TokenType::PERCENT))
        {
            Token op = previous();
            auto right = parseUnary();
            expr = std::make_shared<BinaryExpr>(expr, op.type, right, op.location);
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parseUnary()
    {
        if (match(TokenType::NOT) || match(TokenType::MINUS) || match(TokenType::TILDE))
        {
            Token op = previous();
            auto right = parseUnary();
            return std::make_shared<UnaryExpr>(op.type, right, op.location);
        }

        return parseCall();
    }

    std::shared_ptr<Expr> Parser::parseCall()
    {
        auto expr = parsePrimary();

        while (true)
        {
            if (match(TokenType::LPAREN))
            {
                // Function call
                std::vector<std::shared_ptr<Expr>> arguments;

                if (!check(TokenType::RPAREN))
                {
                    do
                    {
                        arguments.push_back(parseExpression());
                    }
                    while (match(TokenType::COMMA));
                }

                consume(TokenType::RPAREN, "Expected ')' after arguments");
                expr = std::make_shared<CallExpr>(expr, arguments, expr->location);
            }
            else if (match(TokenType::DOT))
            {
                // Member access
                Token member = consume(TokenType::IDENTIFIER, "Expected property name after '.'");
                expr = std::make_shared<MemberAccessExpr>(expr, member.lexeme, expr->location);
            }
            else if (match(TokenType::LBRACKET))
            {
                // Array indexing
                auto index = parseExpression();
                consume(TokenType::RBRACKET, "Expected ']' after array index");
                expr = std::make_shared<IndexExpr>(expr, index, expr->location);
            }
            else
            {
                break;
            }
        }

        return expr;
    }

    std::shared_ptr<Expr> Parser::parsePrimary()
    {
        Token token = peek();

        // This expression
        if (token.type == TokenType::KW_THIS)
        {
            advance();
            return std::make_shared<ThisExpr>(token.location);
        }

        // Integer literals
        if (token.type == TokenType::INT_LITERAL)
        {
            advance();
            int value = std::stoi(token.lexeme);
            return std::make_shared<IntLiteralExpr>(value, token.location);
        }

        // Float literals
        if (token.type == TokenType::FLOAT_LITERAL)
        {
            advance();
            double value = std::stod(token.lexeme);
            return std::make_shared<FloatLiteralExpr>(value, token.location);
        }

        // String literals
        if (token.type == TokenType::STRING_LITERAL)
        {
            advance();
            return std::make_shared<StringLiteralExpr>(token.lexeme, token.location);
        }

        // Boolean literals
        if (token.type == TokenType::BOOL_LITERAL)
        {
            advance();
            bool value = (token.lexeme == "true");
            return std::make_shared<BoolLiteralExpr>(value, token.location);
        }

        // Lambda expressions with optional return type, or identifiers
        // Syntax: lambda[params] { body } or returnType lambda[params] { body }
        if (token.type == TokenType::KW_LAMBDA ||
            (token.type >= TokenType::TYPE_INT && token.type <= TokenType::TYPE_VOID) ||
            token.type == TokenType::IDENTIFIER)
        {
            // Check if this might be a lambda with return type
            std::shared_ptr<Type> returnType = nullptr;
            SourceLocation lambdaLoc = token.location;
            
            // Check if we have "type lambda" pattern
            if (token.type >= TokenType::TYPE_INT && token.type <= TokenType::TYPE_VOID)
            {
                // Consume the type token and create the return type
                advance(); // consume the type token (int, float, etc.)
                if (token.type == TokenType::TYPE_INT)
                {
                    returnType = std::make_shared<Type>(TypeKind::INT, "int");
                }
                else if (token.type == TokenType::TYPE_FLOAT)
                {
                    returnType = std::make_shared<Type>(TypeKind::FLOAT, "float");
                }
                else if (token.type == TokenType::TYPE_STRING)
                {
                    returnType = std::make_shared<Type>(TypeKind::STRING, "string");
                }
                else if (token.type == TokenType::TYPE_BOOL)
                {
                    returnType = std::make_shared<Type>(TypeKind::BOOL, "bool");
                }
                else if (token.type == TokenType::TYPE_VOID)
                {
                    returnType = std::make_shared<Type>(TypeKind::VOID, "void");
                }
                
                if (check(TokenType::KW_LAMBDA))
                {
                    // Yes, it's a typed lambda!
                    advance(); // consume 'lambda'
                    lambdaLoc = previous().location;
                }
                else
                {
                    // Not a lambda expression, this is an error
                    throw error(token, "Expected expression");
                }
            }
            else if (token.type == TokenType::IDENTIFIER)
            {
                // Could be custom type + lambda, or just identifier
                int savedPos = current;
                Token idToken = token;
                advance();
                
                if (check(TokenType::KW_LAMBDA))
                {
                    // It's a custom typed lambda: MyType lambda[...]
                    advance(); // consume 'lambda'
                    lambdaLoc = previous().location;
                    returnType = std::make_shared<Type>(TypeKind::STRUCT, idToken.lexeme);
                }
                else
                {
                    // Just an identifier, backtrack
                    current = savedPos;
                    advance();
                    return std::make_shared<IdentifierExpr>(token.lexeme, token.location);
                }
            }
            else if (token.type == TokenType::KW_LAMBDA)
            {
                advance(); // consume 'lambda'
                lambdaLoc = token.location;
                returnType = std::make_shared<Type>(TypeKind::VOID, "void");
            }

            auto lambda = std::make_shared<LambdaExpr>(lambdaLoc);
            lambda->returnType = returnType ? returnType : std::make_shared<Type>(TypeKind::VOID, "void");

            // Parse parameters in brackets: lambda[param1: type1, param2: type2]
            consume(TokenType::LBRACKET, "Expected '[' after 'lambda'");

            if (!check(TokenType::RBRACKET))
            {
                do
                {
                    lambda->parameters.push_back(parseParameter());
                }
                while (match(TokenType::COMMA));
            }

            consume(TokenType::RBRACKET, "Expected ']' after lambda parameters");

            // Parse body
            consume(TokenType::LBRACE, "Expected '{' before lambda body");

            while (!check(TokenType::RBRACE) && !isAtEnd())
            {
                lambda->body.push_back(parseStatement());
            }

            consume(TokenType::RBRACE, "Expected '}' after lambda body");

            return lambda;
        }

        // Parenthesized expressions
        if (token.type == TokenType::LPAREN)
        {
            advance();
            auto expr = parseExpression();
            consume(TokenType::RPAREN, "Expected ')' after expression");
            return expr;
        }

        // Array literals
        if (token.type == TokenType::LBRACKET)
        {
            advance();
            std::vector<std::shared_ptr<Expr>> elements;

            if (!check(TokenType::RBRACKET))
            {
                do
                {
                    elements.push_back(parseExpression());
                }
                while (match(TokenType::COMMA));
            }

            consume(TokenType::RBRACKET, "Expected ']' after array elements");
            return std::make_shared<ArrayLiteralExpr>(elements, token.location);
        }

        // Struct initialization
        if (token.type == TokenType::LBRACE)
        {
            advance();
            std::vector<std::shared_ptr<Expr>> fields;

            if (!check(TokenType::RBRACE))
            {
                do
                {
                    fields.push_back(parseExpression());
                }
                while (match(TokenType::COMMA));
            }

            consume(TokenType::RBRACE, "Expected '}' after struct fields");
            return std::make_shared<StructInitExpr>("", fields, token.location);
        }

        throw error(token, "Expected expression");
    }

    std::shared_ptr<Type> Parser::parseType()
    {
        Token token = advance();
        std::shared_ptr<Type> baseType;

        // Check for lambda/function types: "return_type lambda[param_types]"
        // We need to check if this is a type followed by 'lambda' keyword
        if ((token.type >= TokenType::TYPE_INT && token.type <= TokenType::TYPE_VOID) ||
            token.type == TokenType::IDENTIFIER)
        {
            // Save current position in case this isn't a lambda type
            int savedPos = current;
            
            // Parse the potential return type
            std::shared_ptr<Type> returnType;
            if (token.type == TokenType::TYPE_INT)
            {
                returnType = std::make_shared<Type>(TypeKind::INT, "int");
            }
            else if (token.type == TokenType::TYPE_FLOAT)
            {
                returnType = std::make_shared<Type>(TypeKind::FLOAT, "float");
            }
            else if (token.type == TokenType::TYPE_STRING)
            {
                returnType = std::make_shared<Type>(TypeKind::STRING, "string");
            }
            else if (token.type == TokenType::TYPE_BOOL)
            {
                returnType = std::make_shared<Type>(TypeKind::BOOL, "bool");
            }
            else if (token.type == TokenType::TYPE_VOID)
            {
                returnType = std::make_shared<Type>(TypeKind::VOID, "void");
            }
            else if (token.type == TokenType::IDENTIFIER)
            {
                returnType = std::make_shared<Type>(TypeKind::STRUCT, token.lexeme);
            }
            
            // Check if next token is 'lambda'
            if (check(TokenType::KW_LAMBDA))
            {
                advance(); // consume 'lambda'
                
                // This is a function type!
                auto funcType = std::make_shared<Type>(TypeKind::FUNCTION, "lambda");
                
                // Store return type in typeParams[0]
                funcType->typeParams.push_back(returnType);
                
                // Parse parameter types: lambda[type1, type2, ...]
                consume(TokenType::LBRACKET, "Expected '[' after 'lambda' in function type");
                
                if (!check(TokenType::RBRACKET))
                {
                    do
                    {
                        auto paramType = parseType();
                        funcType->typeParams.push_back(paramType);
                    }
                    while (match(TokenType::COMMA));
                }
                
                consume(TokenType::RBRACKET, "Expected ']' after lambda parameter types");
                
                return funcType;
            }
            else
            {
                // Not a lambda type, use the return type as base type
                baseType = returnType;
            }
        }
        else
        {
            throw error(token, "Expected type name");
        }

        // Check for array type: type[]
        if (match(TokenType::LBRACKET))
        {
            consume(TokenType::RBRACKET, "Expected ']' after '['");
            auto arrayType = std::make_shared<Type>(TypeKind::ARRAY, "array");
            arrayType->typeParams.push_back(baseType);
            return arrayType;
        }

        // Check for optional type: type? -> desugar to Option<type>
        if (match(TokenType::QUESTION))
        {
            auto optionalType = std::make_shared<Type>(TypeKind::STRUCT, "Option");
            optionalType->typeParams.push_back(baseType);
            return optionalType;
        }

        return baseType;
    }

    Parameter Parser::parseParameter()
    {
        Token name = consume(TokenType::IDENTIFIER, "Expected parameter name");
        consume(TokenType::COLON, "Expected ':' after parameter name");
        auto type = parseType();

        return Parameter(name.lexeme, type);
    }
} // namespace flow