#ifndef FLOW_TOKEN_H
#define FLOW_TOKEN_H

#include <string>
#include <ostream>

namespace flow {
    enum class TokenType {
        // Keywords
        KW_LET,
        KW_MUT,
        KW_FUNC,
        KW_RETURN,
        KW_STRUCT,
        KW_TYPE,
        KW_IF,
        KW_ELSE,
        KW_FOR,
        KW_IN,
        KW_WHILE,
        KW_LINK,
        KW_EXPORT,
        KW_ASYNC,
        KW_AWAIT,
        KW_SOME,
        KW_NONE,
        KW_HAS,
        KW_VALUE,
        KW_INLINE,
        KW_IMPORT,
        KW_MODULE,
        KW_FROM,
        KW_AS,

        // Types
        TYPE_INT,
        TYPE_FLOAT,
        TYPE_STRING,
        TYPE_BOOL,
        TYPE_VOID,

        // Identifiers and Literals
        IDENTIFIER,
        INT_LITERAL,
        FLOAT_LITERAL,
        STRING_LITERAL,
        BOOL_LITERAL,

        // Operators
        PLUS, // +
        MINUS, // -
        STAR, // *
        SLASH, // /
        PERCENT, // %
        ASSIGN, // =
        EQ, // ==
        NE, // !=
        LT, // <
        LE, // <=
        GT, // >
        GE, // >=
        AND, // &&
        OR, // ||
        NOT, // !
        AMPERSAND, // &
        PIPE, // |

        // Delimiters
        LPAREN, // (
        RPAREN, // )
        LBRACE, // {
        RBRACE, // }
        LBRACKET, // [
        RBRACKET, // ]
        SEMICOLON, // ;
        COLON, // :
        COMMA, // ,
        QUESTION, // ?
        DOT, // .
        ARROW, // ->
        DOUBLE_DOT, // ..
        TRIPLE_DOT, // ...
        HASH, // #

        // Special
        END_OF_FILE,
        INVALID
    };

    struct SourceLocation {
        std::string filename;
        int line;
        int column;

        SourceLocation() : filename(""), line(0), column(0) {
        }

        SourceLocation(const std::string &file, int l, int c)
            : filename(file), line(l), column(c) {
        }
    };

    class Token {
    public:
        TokenType type;
        std::string lexeme;
        SourceLocation location;

        Token() : type(TokenType::INVALID), lexeme(""), location() {
        }

        Token(TokenType t, const std::string &lex, const SourceLocation &loc)
            : type(t), lexeme(lex), location(loc) {
        }

        bool is(TokenType t) const { return type == t; }
        bool isNot(TokenType t) const { return type != t; }

        std::string toString() const;

        static std::string tokenTypeToString(TokenType type);
    };

    std::ostream &operator<<(std::ostream &os, const Token &token);
} // namespace flow

#endif // FLOW_TOKEN_H