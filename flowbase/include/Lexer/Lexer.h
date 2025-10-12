#ifndef FLOW_LEXER_H
#define FLOW_LEXER_H

#include "Token.h"
#include <string>
#include <vector>
#include <memory>

namespace flow {
    class Lexer {
    private:
        std::string source;
        std::string filename;
        size_t current;
        int line;
        int column;

        char peek() const;

        char peekNext() const;

        char advance();

        bool isAtEnd() const;

        bool match(char expected);

        void skipWhitespace();

        void skipLineComment();

        void skipBlockComment();

        Token makeToken(TokenType type, const std::string &lexeme);

        Token makeToken(TokenType type);

        Token errorToken(const std::string &message);

        Token scanNumber();

        Token scanString();

        Token scanIdentifier();

        TokenType identifierType(const std::string &text);

    public:
        Lexer(const std::string &src, const std::string &fname = "<input>");


        Token nextToken();

        std::vector<Token> tokenize();
    };
} // namespace flow

#endif // FLOW_LEXER_H