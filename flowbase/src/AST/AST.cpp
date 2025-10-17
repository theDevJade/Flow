#include "../../include/AST/AST.h"
#include <sstream>

namespace flow
{
    std::string Type::toString() const
    {
        switch (kind)
        {
        case TypeKind::INT: return "int";
        case TypeKind::FLOAT: return "float";
        case TypeKind::STRING: return "string";
        case TypeKind::BOOL: return "bool";
        case TypeKind::VOID: return "void";
        case TypeKind::STRUCT:
            if (!typeParams.empty())
            {
                std::string result = name + "<";
                for (size_t i = 0; i < typeParams.size(); i++)
                {
                    if (i > 0) result += ", ";
                    // Add null check to prevent crash
                    if (typeParams[i]) {
                        result += typeParams[i]->toString();
                    } else {
                        result += "null";
                    }
                }
                result += ">";
                return result;
            }
            return name;
        case TypeKind::FUNCTION: return "function";
        case TypeKind::ARRAY:
            if (!typeParams.empty() && typeParams[0])
            {
                return typeParams[0]->toString() + "[]";
            }
            return "array";
        case TypeKind::UNKNOWN: return "unknown";
        default: return "?";
        }
    }


    void IntLiteralExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void FloatLiteralExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void StringLiteralExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void BoolLiteralExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void IdentifierExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ThisExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void BinaryExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void UnaryExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void CallExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void MemberAccessExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void StructInitExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ArrayLiteralExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void IndexExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void LambdaExpr::accept(ASTVisitor& visitor) { visitor.visit(*this); }

    void ExprStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void VarDeclStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void AssignmentStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ReturnStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void IfStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ForStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void WhileStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void BlockStmt::accept(ASTVisitor& visitor) { visitor.visit(*this); }

    void FunctionDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void StructDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ImplDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void TypeDefDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void LinkDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ImportDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void ModuleDecl::accept(ASTVisitor& visitor) { visitor.visit(*this); }
    void Program::accept(ASTVisitor& visitor) { visitor.visit(*this); }
} // namespace flow