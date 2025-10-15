#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Common/ErrorReporter.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include "../../include/LSP/LSPErrorCollector.h"
#include <algorithm>
#include <iostream>
#include <fstream>
#include <sstream>
#include <filesystem>

namespace flow
{
    void SymbolTable::enterScope()
    {
        scopes.emplace_back();
    }

    void SymbolTable::exitScope()
    {
        if (!scopes.empty())
        {
            scopes.pop_back();
        }
    }

    void SymbolTable::define(const std::string& name, std::shared_ptr<Type> type, bool isMutable, bool isFunction)
    {
        if (scopes.empty()) return;
        scopes.back()[name] = Symbol(name, type, isMutable, isFunction);
    }

    SymbolTable::Symbol* SymbolTable::lookup(const std::string& name)
    {
        // Search from innermost to outermost scope
        for (auto it = scopes.rbegin(); it != scopes.rend(); ++it)
        {
            auto found = it->find(name);
            if (found != it->end())
            {
                return &found->second;
            }
        }
        return nullptr;
    }

    bool SymbolTable::isDefined(const std::string& name)
    {
        return lookup(name) != nullptr;
    }

    bool SymbolTable::isMutable(const std::string& name)
    {
        Symbol* symbol = lookup(name);
        return symbol ? symbol->isMutable : false;
    }


    void SemanticAnalyzer::reportError(const std::string& message, const SourceLocation& loc)
    {
        if (errorCollector)
        {
            errorCollector->reportError("Semantic", message, loc);
        }
        else
        {
            ErrorReporter::instance().reportError("Semantic", message, loc);
        }
        errors.push_back(message);
    }

    void SemanticAnalyzer::setErrorCollector(lsp::LSPErrorCollector* collector)
    {
        errorCollector = collector;
    }

    bool SemanticAnalyzer::typesMatch(std::shared_ptr<Type> t1, std::shared_ptr<Type> t2)
    {
        if (!t1 || !t2) return false;

        t1 = resolveTypeAlias(t1);
        t2 = resolveTypeAlias(t2);

        if (t1->kind == t2->kind && t1->name == t2->name)
        {
            if (!t1->typeParams.empty() || !t2->typeParams.empty())
            {
                if (t1->typeParams.size() != t2->typeParams.size())
                {
                    return false;
                }
                for (size_t i = 0; i < t1->typeParams.size(); i++)
                {
                    if (!typesMatch(t1->typeParams[i], t2->typeParams[i]))
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        // Type coercion: int <-> float
        if ((t1->kind == TypeKind::INT && t2->kind == TypeKind::FLOAT) ||
            (t1->kind == TypeKind::FLOAT && t2->kind == TypeKind::INT))
        {
            return true;
        }

        // Coercion to string
        if (t2->kind == TypeKind::STRING && (t1->kind == TypeKind::INT ||
            t1->kind == TypeKind::FLOAT ||
            t1->kind == TypeKind::BOOL))
        {
            return true;
        }

        // bool to numeric
        if (t1->kind == TypeKind::BOOL && (t2->kind == TypeKind::INT || t2->kind == TypeKind::FLOAT))
        {
            return true;
        }

        return false;
    }

    void SemanticAnalyzer::setCurrentFile(const std::string& filePath)
    {
        namespace fs = std::filesystem;
        currentDirectory = fs::path(filePath).parent_path().string();
        if (currentDirectory.empty())
        {
            currentDirectory = ".";
        }
    }

    void SemanticAnalyzer::analyze(std::shared_ptr<Program> program)
    {
        // Register built-in functions
        auto voidType = std::make_shared<Type>(TypeKind::VOID, "void");
        auto intType = std::make_shared<Type>(TypeKind::INT, "int");
        auto floatType = std::make_shared<Type>(TypeKind::FLOAT, "float");
        auto stringType = std::make_shared<Type>(TypeKind::STRING, "string");
        auto boolType = std::make_shared<Type>(TypeKind::BOOL, "bool");


        auto optionType = std::make_shared<Type>(TypeKind::STRUCT, "Option");
        symbolTable.define("Option", optionType, false);


        symbolTable.define("print", voidType, false, true);


        symbolTable.define("println", voidType, false, true);

        symbolTable.define("len", intType, false, true);

        // String functions
        symbolTable.define("strlen", intType, false, true);
        symbolTable.define("substr", stringType, false, true);
        symbolTable.define("concat", stringType, false, true);

        // Math functions
        symbolTable.define("abs", intType, false, true);
        symbolTable.define("sqrt", floatType, false, true);
        symbolTable.define("pow", floatType, false, true);
        symbolTable.define("min", intType, false, true);
        symbolTable.define("max", intType, false, true);

        // I/O functions
        symbolTable.define("readLine", stringType, false, true);
        symbolTable.define("readInt", intType, false, true);
        symbolTable.define("writeFile", boolType, false, true);
        symbolTable.define("readFile", stringType, false, true);

        if (program)
        {
            program->accept(*this);
        }
    }


    void SemanticAnalyzer::visit(IntLiteralExpr& node)
    {
        node.type = std::make_shared<Type>(TypeKind::INT, "int");
    }

    void SemanticAnalyzer::visit(FloatLiteralExpr& node)
    {
        node.type = std::make_shared<Type>(TypeKind::FLOAT, "float");
    }

    void SemanticAnalyzer::visit(StringLiteralExpr& node)
    {
        node.type = std::make_shared<Type>(TypeKind::STRING, "string");
    }

    void SemanticAnalyzer::visit(BoolLiteralExpr& node)
    {
        node.type = std::make_shared<Type>(TypeKind::BOOL, "bool");
    }

    void SemanticAnalyzer::visit(IdentifierExpr& node)
    {
        auto* symbol = symbolTable.lookup(node.name);
        if (symbol)
        {
            node.type = symbol->type;
        }
        else
        {
            reportError("Undefined identifier: " + node.name, node.location);
            node.type = std::make_shared<Type>(TypeKind::UNKNOWN);
        }
    }

    void SemanticAnalyzer::visit(BinaryExpr& node)
    {
        // Type check operands
        if (node.left) node.left->accept(*this);
        if (node.right) node.right->accept(*this);

        // Infer result type
        if (node.left && node.left->type)
        {
            // For arithmetic and comparison operators, result follows left operand type

            switch (node.op)
            {
            case TokenType::LT:
            case TokenType::LE:
            case TokenType::GT:
            case TokenType::GE:
            case TokenType::EQ:
            case TokenType::NE:
            case TokenType::AND:
            case TokenType::OR:
                // Comparison and logical operators return bool
                node.type = std::make_shared<Type>(TypeKind::BOOL, "bool");
                break;
            default:
                // Arithmetic operators inherit type from left operand
                node.type = node.left->type;
                break;
            }
        }
    }

    void SemanticAnalyzer::visit(UnaryExpr& node)
    {
        // Type check operand
        if (node.operand)
        {
            node.operand->accept(*this);
            // Result type is same as operand for -, !
            if (node.operand->type)
            {
                node.type = node.operand->type;
            }
        }
    }

    void SemanticAnalyzer::visit(CallExpr& node)
    {
        // Type check function call
        if (node.callee) node.callee->accept(*this);
        for (auto& arg : node.arguments)
        {
            if (arg) arg->accept(*this);
        }

        // Infer return type from function
        if (auto* idExpr = dynamic_cast<IdentifierExpr*>(node.callee.get()))
        {
            auto* symbol = symbolTable.lookup(idExpr->name);
            if (symbol && symbol->isFunction)
            {
                node.type = symbol->type;
            }
        }
    }

    void SemanticAnalyzer::visit(MemberAccessExpr& node)
    {
        // Type check the object
        if (node.object)
        {
            node.object->accept(*this);

            // Check that it's a struct
            if (node.object->type && node.object->type->kind == TypeKind::STRUCT)
            {
                // Look up the field type from struct definition
                auto structIt = structFields.find(node.object->type->name);
                if (structIt != structFields.end())
                {
                    auto fieldIt = structIt->second.find(node.member);
                    if (fieldIt != structIt->second.end())
                    {
                        node.type = fieldIt->second;
                    }
                    else
                    {
                        reportError("Unknown field '" + node.member + "' in struct '" + node.object->type->name + "'",
                                    node.location);
                        node.type = std::make_shared<Type>(TypeKind::UNKNOWN, "unknown");
                    }
                }
                else
                {
                    reportError("Unknown struct type: " + node.object->type->name, node.location);
                    node.type = std::make_shared<Type>(TypeKind::UNKNOWN, "unknown");
                }
            }
            else if (node.object->type)
            {
                reportError("Member access on non-struct type", node.location);
            }
        }
    }

    void SemanticAnalyzer::visit(StructInitExpr& node)
    {
        for (auto& field : node.fieldValues)
        {
            if (field) field->accept(*this);
        }

        auto structIt = structFields.find(node.structName);
        if (structIt == structFields.end())
        {
            reportError("Unknown struct type: " + node.structName, node.location);
            node.type = std::make_shared<Type>(TypeKind::UNKNOWN, "unknown");
            return;
        }

        const auto& expectedFields = structIt->second;
        if (node.fieldValues.size() != expectedFields.size())
        {
            reportError("Struct '" + node.structName + "' expects " +
                        std::to_string(expectedFields.size()) + " fields, but got " +
                        std::to_string(node.fieldValues.size()), node.location);
            node.type = std::make_shared<Type>(TypeKind::STRUCT, node.structName);
            return;
        }

        size_t i = 0;
        for (const auto& [fieldName, fieldType] : expectedFields)
        {
            if (i >= node.fieldValues.size()) break;

            auto& fieldValue = node.fieldValues[i];
            if (fieldValue && fieldValue->type)
            {
                if (!typesMatch(fieldValue->type, fieldType))
                {
                    reportError("Field '" + fieldName + "' of struct '" + node.structName +
                                "' expects type '" + fieldType->name + "' but got '" +
                                fieldValue->type->name + "'", node.location);
                }
            }
            i++;
        }

        node.type = std::make_shared<Type>(TypeKind::STRUCT, node.structName);
    }

    void SemanticAnalyzer::visit(ArrayLiteralExpr& node)
    {
        // Type check all elements
        std::shared_ptr<Type> elementType = nullptr;

        for (auto& elem : node.elements)
        {
            if (elem)
            {
                elem->accept(*this);

                // Infer array element type from first element
                if (!elementType && elem->type)
                {
                    elementType = elem->type;
                }

                // Check that all elements have the same type
                if (elementType && elem->type && !typesMatch(elem->type, elementType))
                {
                    reportError("Array elements must all have the same type", node.location);
                }
            }
        }

        // Set the array type
        if (elementType)
        {
            auto arrayType = std::make_shared<Type>(TypeKind::ARRAY, "array");
            arrayType->typeParams.push_back(elementType);
            node.type = arrayType;
        }
        else
        {
            node.type = std::make_shared<Type>(TypeKind::ARRAY, "array");
        }
    }

    void SemanticAnalyzer::visit(IndexExpr& node)
    {
        // Type check the array expression
        if (node.array)
        {
            node.array->accept(*this);

            // Check that it's actually an array
            if (node.array->type && node.array->type->kind != TypeKind::ARRAY)
            {
                reportError("Cannot index non-array type", node.location);
            }

            // Set the result type to the element type
            if (node.array->type && !node.array->type->typeParams.empty())
            {
                node.type = node.array->type->typeParams[0];
            }
        }

        // Type check the index expression
        if (node.index)
        {
            node.index->accept(*this);

            // Check that index is an integer
            if (node.index->type && node.index->type->kind != TypeKind::INT)
            {
                reportError("Array index must be an integer", node.location);
            }
        }
    }

    void SemanticAnalyzer::visit(ExprStmt& node)
    {
        if (node.expression)
        {
            node.expression->accept(*this);
        }
    }

    void SemanticAnalyzer::visit(VarDeclStmt& node)
    {
        // Type check initializer
        if (node.initializer)
        {
            node.initializer->accept(*this);
        }

        // Type inference: if no explicit type, infer from initializer
        std::shared_ptr<Type> varType = node.declaredType;
        if (!varType && node.initializer && node.initializer->type)
        {
            varType = node.initializer->type;
        }

        // Check for redefinition
        if (symbolTable.isDefined(node.name))
        {
            reportError("Redefinition of variable: " + node.name, node.location);
        }
        else
        {
            if (varType)
            {
                symbolTable.define(node.name, varType, node.isMutable);
            }
            else
            {
                reportError("Cannot infer type for variable: " + node.name, node.location);
            }
        }
    }

    void SemanticAnalyzer::visit(AssignmentStmt& node)
    {
        // Check that the variable exists and is mutable
        if (!symbolTable.isDefined(node.target))
        {
            reportError("Assignment to undefined variable: " + node.target, node.location);
            return;
        }

        if (!symbolTable.isMutable(node.target))
        {
            reportError("Cannot assign to immutable variable: " + node.target, node.location);
            return;
        }

        // Type check the value
        if (node.value)
        {
            node.value->accept(*this);
        }
    }

    void SemanticAnalyzer::visit(ReturnStmt& node)
    {
        if (node.value)
        {
            node.value->accept(*this);
        }

        if (!currentFunctionReturnType)
        {
            reportError("Return statement outside of function", node.location);
            return;
        }

        if (currentFunctionReturnType->isVoid())
        {
            if (node.value)
            {
                reportError("Void function should not return a value", node.location);
            }
        }
        else
        {
            if (!node.value)
            {
                reportError("Non-void function must return a value", node.location);
            }
            else if (node.value->type)
            {
                if (!typesMatch(node.value->type, currentFunctionReturnType))
                {
                    reportError("Return type '" + node.value->type->name +
                                "' does not match function return type '" +
                                currentFunctionReturnType->name + "'", node.location);
                }
            }
        }
    }

    void SemanticAnalyzer::visit(IfStmt& node)
    {
        if (node.condition)
        {
            node.condition->accept(*this);

            if (node.condition->type)
            {
                auto condType = resolveTypeAlias(node.condition->type);
                if (condType->kind != TypeKind::BOOL &&
                    condType->kind != TypeKind::INT &&
                    condType->kind != TypeKind::FLOAT)
                {
                    reportError("If condition must be a boolean expression, got '" +
                                condType->name + "'", node.location);
                }
            }
        }

        symbolTable.enterScope();
        for (auto& stmt : node.thenBranch)
        {
            if (stmt) stmt->accept(*this);
        }
        symbolTable.exitScope();

        if (!node.elseBranch.empty())
        {
            symbolTable.enterScope();
            for (auto& stmt : node.elseBranch)
            {
                if (stmt) stmt->accept(*this);
            }
            symbolTable.exitScope();
        }
    }

    void SemanticAnalyzer::visit(ForStmt& node)
    {
        // Check range expressions or iterable
        if (node.rangeStart)
        {
            node.rangeStart->accept(*this);
        }
        if (node.rangeEnd)
        {
            node.rangeEnd->accept(*this);
        }
        if (node.iterable)
        {
            node.iterable->accept(*this);
        }

        // Enter new scope for loop body
        symbolTable.enterScope();

        // Add iterator variable to scope (defaults to int for ranges)
        auto iterType = std::make_shared<Type>(TypeKind::INT, "int");
        symbolTable.define(node.iteratorVar, iterType, false); // false = immutable

        // Check body statements
        for (auto& stmt : node.body)
        {
            if (stmt) stmt->accept(*this);
        }

        symbolTable.exitScope();
    }

    void SemanticAnalyzer::visit(WhileStmt& node)
    {
        if (node.condition)
        {
            node.condition->accept(*this);

            if (node.condition->type)
            {
                auto condType = resolveTypeAlias(node.condition->type);
                if (condType->kind != TypeKind::BOOL &&
                    condType->kind != TypeKind::INT &&
                    condType->kind != TypeKind::FLOAT)
                {
                    reportError("While condition must be a boolean expression, got '" +
                                condType->name + "'", node.location);
                }
            }
        }

        // Check body statements
        symbolTable.enterScope();
        for (auto& stmt : node.body)
        {
            if (stmt) stmt->accept(*this);
        }
        symbolTable.exitScope();
    }

    void SemanticAnalyzer::visit(BlockStmt& node)
    {
        symbolTable.enterScope();
        for (auto& stmt : node.statements)
        {
            if (stmt) stmt->accept(*this);
        }
        symbolTable.exitScope();
    }

    void SemanticAnalyzer::visit(FunctionDecl& node)
    {
        symbolTable.define(node.name, node.returnType, false, true);

        symbolTable.enterScope();
        currentFunctionReturnType = node.returnType;

        // Add parameters to scope
        for (const auto& param : node.parameters)
        {
            symbolTable.define(param.name, param.type, false);
        }

        // Check body
        for (auto& stmt : node.body)
        {
            if (stmt) stmt->accept(*this);
        }

        currentFunctionReturnType = nullptr;
        symbolTable.exitScope();
    }

    void SemanticAnalyzer::visit(StructDecl& node)
    {
        // Register struct type
        auto structType = std::make_shared<Type>(TypeKind::STRUCT, node.name);
        symbolTable.define(node.name, structType, false);

        // Register field types for member access
        std::map<std::string, std::shared_ptr<Type>> fields;
        for (const auto& field : node.fields)
        {
            fields[field.name] = field.type;
        }
        structFields[node.name] = fields;
    }

    void SemanticAnalyzer::visit(TypeDefDecl& node)
    {
        // Register type alias
        typeAliases[node.name] = node.aliasedType;
    }

    std::shared_ptr<Type> SemanticAnalyzer::resolveTypeAlias(std::shared_ptr<Type> type)
    {
        if (!type) return type;

        // Check if this type name is an alias
        auto it = typeAliases.find(type->name);
        if (it != typeAliases.end())
        {
            // Recursively resolve in case of chained aliases
            return resolveTypeAlias(it->second);
        }

        return type;
    }

    void SemanticAnalyzer::visit(LinkDecl& node)
    {
        std::vector<std::string> validAdapters = {"c", "python", "js", "jvm", "inline"};
        bool isValidAdapter = false;
        for (const auto& adapter : validAdapters)
        {
            if (node.adapter == adapter)
            {
                isValidAdapter = true;
                break;
            }
        }

        if (!isValidAdapter)
        {
            reportError("Unknown FFI adapter '" + node.adapter + "'. Valid adapters are: c, python, js, jvm, inline",
                        node.location);
        }

        if (node.adapter != "inline" && node.adapter != "c" && node.module.empty())
        {
            reportError("Link declaration with adapter '" + node.adapter + "' must specify a module",
                        node.location);
        }

        if (node.adapter == "inline" && node.inlineCode.empty())
        {
            reportError("Inline link declaration must provide code block", node.location);
        }

        for (auto& func : node.functions)
        {
            if (!func) continue;

            for (const auto& param : func->parameters)
            {
                // Skip type checking for variadic parameters
                if (param.name == "__varargs")
                {
                    continue;
                }

                if (param.type)
                {
                    auto paramType = resolveTypeAlias(param.type);
                    if (paramType->kind == TypeKind::UNKNOWN)
                    {
                        reportError("Foreign function '" + func->name + "' parameter '" +
                                    param.name + "' has unknown type", func->location);
                    }
                }
            }

            symbolTable.define(func->name, func->returnType, false, true);
        }
    }

    std::string SemanticAnalyzer::resolveModulePath(const std::string& importPath)
    {
        namespace fs = std::filesystem;

        // If absolute path, use as-is
        if (fs::path(importPath).is_absolute())
        {
            return importPath;
        }

        // First try: resolve relative to current directory
        fs::path fullPath = fs::path(currentDirectory) / importPath;

        try
        {
            return fs::canonical(fullPath).string();
        }
        catch (const fs::filesystem_error&)
        {
            // If that fails, try library paths
            for (const auto& libPath : libraryPaths)
            {
                fs::path libFullPath = fs::path(libPath) / importPath;
                try
                {
                    return fs::canonical(libFullPath).string();
                }
                catch (const fs::filesystem_error&)
                {
                    // Continue to next library path
                }
            }

            // If all library paths fail, return the original path
            // (will fail later with better error message)
            return fullPath.string();
        }
    }

    std::shared_ptr<Program> SemanticAnalyzer::loadModule(const std::string& modulePath)
    {
        // Check if already loaded
        auto it = loadedModules.find(modulePath);
        if (it != loadedModules.end())
        {
            return it->second;
        }

        // Read the file
        std::ifstream file(modulePath);
        if (!file.is_open())
        {
            throw std::runtime_error("Failed to open module: " + modulePath);
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string source = buffer.str();
        file.close();

        // Lexically analyze
        Lexer lexer(source, modulePath);
        std::vector<Token> tokens = lexer.tokenize();

        // Parse
        Parser parser(tokens);
        auto program = parser.parse();

        // Cache the module
        loadedModules[modulePath] = program;

        // Analyze the module (recursive)
        std::string savedDir = currentDirectory;
        currentDirectory = std::filesystem::path(modulePath).parent_path().string();

        for (auto& decl : program->declarations)
        {
            if (decl)
            {
                decl->accept(*this);
            }
        }

        currentDirectory = savedDir;

        return program;
    }

    void SemanticAnalyzer::importSymbolsFrom(std::shared_ptr<Program> module,
                                             const std::vector<std::string>& symbols,
                                             const std::string& alias)
    {
        if (!module) return;

        for (auto& decl : module->declarations)
        {
            FunctionDecl* funcDecl = dynamic_cast<FunctionDecl*>(decl.get());
            if (funcDecl)
            {
                if (symbols.empty() || std::find(symbols.begin(), symbols.end(), funcDecl->name) != symbols.end())
                {
                    std::string importName = alias.empty() ? funcDecl->name : alias + "." + funcDecl->name;
                    symbolTable.define(importName, funcDecl->returnType, false, true);
                    std::cout << "  Imported function: " << importName << std::endl;
                }
            }

            StructDecl* structDecl = dynamic_cast<StructDecl*>(decl.get());
            if (structDecl)
            {
                if (symbols.empty() || std::find(symbols.begin(), symbols.end(), structDecl->name) != symbols.end())
                {
                    std::string importName = alias.empty() ? structDecl->name : alias + "." + structDecl->name;
                    auto structType = std::make_shared<Type>(TypeKind::STRUCT, importName);
                    symbolTable.define(importName, structType, false);

                    std::map<std::string, std::shared_ptr<Type>> fields;
                    for (const auto& field : structDecl->fields)
                    {
                        fields[field.name] = field.type;
                    }
                    structFields[importName] = fields;
                    std::cout << "  Imported struct: " << importName << std::endl;
                }
            }

            TypeDefDecl* typedefDecl = dynamic_cast<TypeDefDecl*>(decl.get());
            if (typedefDecl)
            {
                if (symbols.empty() || std::find(symbols.begin(), symbols.end(), typedefDecl->name) != symbols.end())
                {
                    std::string importName = alias.empty() ? typedefDecl->name : alias + "." + typedefDecl->name;
                    typeAliases[importName] = typedefDecl->aliasedType;
                    std::cout << "  Imported type alias: " << importName << std::endl;
                }
            }

            LinkDecl* linkDecl = dynamic_cast<LinkDecl*>(decl.get());
            if (linkDecl)
            {
                for (auto& func : linkDecl->functions)
                {
                    if (!func) continue;
                    if (symbols.empty() || std::find(symbols.begin(), symbols.end(), func->name) != symbols.end())
                    {
                        std::string importName = alias.empty() ? func->name : alias + "." + func->name;
                        symbolTable.define(importName, func->returnType, false, true);
                        std::cout << "  Imported foreign function: " << importName << std::endl;
                    }
                }
            }
        }
    }

    void SemanticAnalyzer::visit(ImportDecl& node)
    {
        std::cout << "Import: " << node.modulePath << std::endl;

        try
        {
            // Resolve the module path
            std::string resolvedPath = resolveModulePath(node.modulePath);

            // Load and parse the module
            auto module = loadModule(resolvedPath);

            // Import specified symbols (or all if none specified)
            importSymbolsFrom(module, node.imports, node.alias);

            std::cout << "  Module loaded successfully: " << resolvedPath << std::endl;
        }
        catch (const std::exception& e)
        {
            reportError("Failed to import module '" + node.modulePath + "': " + e.what(), node.location);
        }
    }

    void SemanticAnalyzer::visit(ModuleDecl& node)
    {
        // This could be used for namespacing in larger projects
        std::cout << "Module: " << node.name << std::endl;
    }

    void SemanticAnalyzer::visit(Program& node)
    {
        for (auto& decl : node.declarations)
        {
            if (decl)
            {
                decl->accept(*this);
            }
        }
    }
} // namespace flow