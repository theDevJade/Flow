#include "../../include/Lexer/Token.h"
#include <sstream>

namespace flow {
    std::string Token::toString() const {
        std::stringstream ss;
        ss << tokenTypeToString(type) << " '" << lexeme << "' at "
                << location.filename << ":" << location.line << ":" << location.column;
        return ss.str();
    }

    std::string Token::tokenTypeToString(TokenType type) {
        switch (type) {
            case TokenType::KW_LET: return "KW_LET";
            case TokenType::KW_MUT: return "KW_MUT";
            case TokenType::KW_FUNC: return "KW_FUNC";
            case TokenType::KW_RETURN: return "KW_RETURN";
            case TokenType::KW_STRUCT: return "KW_STRUCT";
            case TokenType::KW_IF: return "KW_IF";
            case TokenType::KW_ELSE: return "KW_ELSE";
            case TokenType::KW_FOR: return "KW_FOR";
            case TokenType::KW_IN: return "KW_IN";
            case TokenType::KW_WHILE: return "KW_WHILE";
            case TokenType::KW_LINK: return "KW_LINK";
            case TokenType::KW_EXPORT: return "KW_EXPORT";
            case TokenType::KW_ASYNC: return "KW_ASYNC";
            case TokenType::KW_AWAIT: return "KW_AWAIT";
            case TokenType::KW_SOME: return "KW_SOME";
            case TokenType::KW_NONE: return "KW_NONE";
            case TokenType::KW_HAS: return "KW_HAS";
            case TokenType::KW_VALUE: return "KW_VALUE";
            case TokenType::KW_INLINE: return "KW_INLINE";
            case TokenType::KW_IMPORT: return "KW_IMPORT";
            case TokenType::KW_MODULE: return "KW_MODULE";
            case TokenType::KW_FROM: return "KW_FROM";
            case TokenType::KW_AS: return "KW_AS";
            case TokenType::TYPE_INT: return "TYPE_INT";
            case TokenType::TYPE_FLOAT: return "TYPE_FLOAT";
            case TokenType::TYPE_STRING: return "TYPE_STRING";
            case TokenType::TYPE_BOOL: return "TYPE_BOOL";
            case TokenType::TYPE_VOID: return "TYPE_VOID";
            case TokenType::IDENTIFIER: return "IDENTIFIER";
            case TokenType::INT_LITERAL: return "INT_LITERAL";
            case TokenType::FLOAT_LITERAL: return "FLOAT_LITERAL";
            case TokenType::STRING_LITERAL: return "STRING_LITERAL";
            case TokenType::BOOL_LITERAL: return "BOOL_LITERAL";
            case TokenType::PLUS: return "PLUS";
            case TokenType::MINUS: return "MINUS";
            case TokenType::STAR: return "STAR";
            case TokenType::SLASH: return "SLASH";
            case TokenType::PERCENT: return "PERCENT";
            case TokenType::ASSIGN: return "ASSIGN";
            case TokenType::EQ: return "EQ";
            case TokenType::NE: return "NE";
            case TokenType::LT: return "LT";
            case TokenType::LE: return "LE";
            case TokenType::GT: return "GT";
            case TokenType::GE: return "GE";
            case TokenType::AND: return "AND";
            case TokenType::OR: return "OR";
            case TokenType::NOT: return "NOT";
            case TokenType::AMPERSAND: return "AMPERSAND";
            case TokenType::PIPE: return "PIPE";
            case TokenType::LPAREN: return "LPAREN";
            case TokenType::RPAREN: return "RPAREN";
            case TokenType::LBRACE: return "LBRACE";
            case TokenType::RBRACE: return "RBRACE";
            case TokenType::LBRACKET: return "LBRACKET";
            case TokenType::RBRACKET: return "RBRACKET";
            case TokenType::SEMICOLON: return "SEMICOLON";
            case TokenType::COLON: return "COLON";
            case TokenType::COMMA: return "COMMA";
            case TokenType::DOT: return "DOT";
            case TokenType::ARROW: return "ARROW";
            case TokenType::DOUBLE_DOT: return "DOUBLE_DOT";
            case TokenType::TRIPLE_DOT: return "TRIPLE_DOT";
            case TokenType::HASH: return "HASH";
            case TokenType::END_OF_FILE: return "END_OF_FILE";
            case TokenType::INVALID: return "INVALID";
            default: return "UNKNOWN";
        }
    }

    std::ostream &operator<<(std::ostream &os, const Token &token) {
        os << token.toString();
        return os;
    }
} // namespace flow