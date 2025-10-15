#include "../../include/Lexer/Lexer.h"
#include <cctype>
#include <map>
#include <iostream>

namespace flow {
    Lexer::Lexer(const std::string &src, const std::string &fname)
        : source(src), filename(fname), current(0), line(1), column(1) {
    }

    char Lexer::peek() const {
        if (isAtEnd()) return '\0';
        return source[current];
    }

    char Lexer::peekNext() const {
        if (current + 1 >= source.length()) return '\0';
        return source[current + 1];
    }

    char Lexer::advance() {
        char c = source[current++];
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    bool Lexer::isAtEnd() const {
        return current >= source.length();
    }

    bool Lexer::match(char expected) {
        if (isAtEnd()) return false;
        if (source[current] != expected) return false;
        advance();
        return true;
    }

    void Lexer::skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                skipLineComment();
            } else if (c == '/' && peekNext() == '*') {
                skipBlockComment();
            } else {
                break;
            }
        }
    }

    void Lexer::skipLineComment() {
        while (peek() != '\n' && !isAtEnd()) {
            advance();
        }
    }

    void Lexer::skipBlockComment() {
        advance(); // /
        advance(); // *

        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance(); // *
                advance(); // /
                break;
            }
            advance();
        }
    }

    Token Lexer::makeToken(TokenType type, const std::string &lexeme) {
        return Token(type, lexeme, SourceLocation(filename, line, column - lexeme.length()));
    }

    Token Lexer::makeToken(TokenType type) {
        return Token(type, "", SourceLocation(filename, line, column));
    }

    Token Lexer::errorToken(const std::string &message) {
        return Token(TokenType::INVALID, message, SourceLocation(filename, line, column));
    }

    TokenType Lexer::identifierType(const std::string &text) {
        static std::map<std::string, TokenType> keywords = {
            {"let", TokenType::KW_LET},
            {"mut", TokenType::KW_MUT},
            {"func", TokenType::KW_FUNC},
            {"return", TokenType::KW_RETURN},
            {"struct", TokenType::KW_STRUCT},
            {"type", TokenType::KW_TYPE},
            {"if", TokenType::KW_IF},
            {"else", TokenType::KW_ELSE},
            {"for", TokenType::KW_FOR},
            {"in", TokenType::KW_IN},
            {"while", TokenType::KW_WHILE},
            {"link", TokenType::KW_LINK},
            {"export", TokenType::KW_EXPORT},
            {"async", TokenType::KW_ASYNC},
            {"await", TokenType::KW_AWAIT},
            {"some", TokenType::KW_SOME},
            {"none", TokenType::KW_NONE},
            {"has", TokenType::KW_HAS},
            {"value", TokenType::KW_VALUE},
            {"inline", TokenType::KW_INLINE},
            {"import", TokenType::KW_IMPORT},
            {"module", TokenType::KW_MODULE},
            {"from", TokenType::KW_FROM},
            {"as", TokenType::KW_AS},
            {"int", TokenType::TYPE_INT},
            {"float", TokenType::TYPE_FLOAT},
            {"string", TokenType::TYPE_STRING},
            {"bool", TokenType::TYPE_BOOL},
            {"void", TokenType::TYPE_VOID},
            {"true", TokenType::BOOL_LITERAL},
            {"false", TokenType::BOOL_LITERAL},
        };

        auto it = keywords.find(text);
        if (it != keywords.end()) {
            return it->second;
        }
        return TokenType::IDENTIFIER;
    }

    Token Lexer::scanNumber() {
        size_t start = current - 1;

        while (std::isdigit(peek())) {
            advance();
        }

        // Check for decimal point
        if (peek() == '.' && std::isdigit(peekNext())) {
            advance(); // consume '.'
            while (std::isdigit(peek())) {
                advance();
            }
            std::string lexeme = source.substr(start, current - start);
            return makeToken(TokenType::FLOAT_LITERAL, lexeme);
        }

        std::string lexeme = source.substr(start, current - start);
        return makeToken(TokenType::INT_LITERAL, lexeme);
    }

    Token Lexer::scanString() {
        size_t start = current;
        std::string value;

        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\\') {
                advance();
                if (!isAtEnd()) {
                    char escaped = advance();
                    switch (escaped) {
                        case 'n': value += '\n'; break;
                        case 't': value += '\t'; break;
                        case 'r': value += '\r'; break;
                        case '\\': value += '\\'; break;
                        case '"': value += '"'; break;
                        case '0': value += '\0'; break;
                        default: 
                            value += '\\';
                            value += escaped;
                            break;
                    }
                }
            } else {
                if (peek() == '\n') line++;
                value += advance();
            }
        }

        if (isAtEnd()) {
            return errorToken("Unterminated string");
        }

        advance();

        return makeToken(TokenType::STRING_LITERAL, value);
    }

    Token Lexer::scanIdentifier() {
        size_t start = current - 1;

        while (std::isalnum(peek()) || peek() == '_') {
            advance();
        }

        std::string text = source.substr(start, current - start);
        TokenType type = identifierType(text);

        return makeToken(type, text);
    }

    Token Lexer::nextToken() {
        skipWhitespace();

        if (isAtEnd()) {
            return makeToken(TokenType::END_OF_FILE);
        }

        char c = advance();


        if (std::isdigit(c)) {
            return scanNumber();
        }


        if (std::isalpha(c) || c == '_') {
            return scanIdentifier();
        }


        if (c == '"') {
            return scanString();
        }


        switch (c) {
            case '(': return makeToken(TokenType::LPAREN, "(");
            case ')': return makeToken(TokenType::RPAREN, ")");
            case '{': return makeToken(TokenType::LBRACE, "{");
            case '}': return makeToken(TokenType::RBRACE, "}");
            case '[': return makeToken(TokenType::LBRACKET, "[");
            case ']': return makeToken(TokenType::RBRACKET, "]");
            case ';': return makeToken(TokenType::SEMICOLON, ";");
            case ':': return makeToken(TokenType::COLON, ":");
            case ',': return makeToken(TokenType::COMMA, ",");
            case '?': return makeToken(TokenType::QUESTION, "?");
            case '%': return makeToken(TokenType::PERCENT, "%");
            case '#': return makeToken(TokenType::HASH, "#");
            case '&':
                if (match('&')) return makeToken(TokenType::AND, "&&");
                return makeToken(TokenType::AMPERSAND, "&");
            case '|':
                if (match('|')) return makeToken(TokenType::OR, "||");
                return makeToken(TokenType::PIPE, "|");
            case '+': return makeToken(TokenType::PLUS, "+");
            case '*': return makeToken(TokenType::STAR, "*");
            case '/': return makeToken(TokenType::SLASH, "/");
            case '!':
                if (match('=')) return makeToken(TokenType::NE, "!=");
                return makeToken(TokenType::NOT, "!");
            case '=':
                if (match('=')) return makeToken(TokenType::EQ, "==");
                return makeToken(TokenType::ASSIGN, "=");
            case '<':
                if (match('=')) return makeToken(TokenType::LE, "<=");
                return makeToken(TokenType::LT, "<");
            case '>':
                if (match('=')) return makeToken(TokenType::GE, ">=");
                return makeToken(TokenType::GT, ">");
            case '.':
                if (match('.')) {
                    if (match('.')) return makeToken(TokenType::TRIPLE_DOT, "...");
                    return makeToken(TokenType::DOUBLE_DOT, "..");
                }
                return makeToken(TokenType::DOT, ".");
            case '-':
                if (match('>')) return makeToken(TokenType::ARROW, "->");
                return makeToken(TokenType::MINUS, "-");
        }

        std::cerr << "Unexpected character: '" << c << "' (ASCII " << static_cast<int>(c) << ") at line " << line << ", column " << column << std::endl;
        return errorToken("Unexpected character");
    }

    std::vector<Token> Lexer::tokenize() {
        std::vector<Token> tokens;

        while (true) {
            Token token = nextToken();
            tokens.push_back(token);

            if (token.type == TokenType::END_OF_FILE || token.type == TokenType::INVALID) {
                break;
            }
        }

        return tokens;
    }
} // namespace flow