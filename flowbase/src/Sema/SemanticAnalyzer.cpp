#include "../../include/Sema/SemanticAnalyzer.h"
#include "../../include/Common/ErrorReporter.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include <algorithm>
#include <iostream>
#include <fstream>
#include <sstream>
#include <filesystem>

namespace flow {

// ============================================================
// SYMBOL TABLE
// ============================================================

void SymbolTable::enterScope() {
    scopes.emplace_back();
}

void SymbolTable::exitScope() {
    if (!scopes.empty()) {
        scopes.pop_back();
    }
}

void SymbolTable::define(const std::string& name, std::shared_ptr<Type> type, bool isMutable, bool isFunction) {
    if (scopes.empty()) return;
    scopes.back()[name] = Symbol(name, type, isMutable, isFunction);
}

SymbolTable::Symbol* SymbolTable::lookup(const std::string& name) {
    // Search from innermost to outermost scope
    for (auto it = scopes.rbegin(); it != scopes.rend(); ++it) {
        auto found = it->find(name);
        if (found != it->end()) {
            return &found->second;
        }
    }
    return nullptr;
}

bool SymbolTable::isDefined(const std::string& name) {
    return lookup(name) != nullptr;
}

bool SymbolTable::isMutable(const std::string& name) {
    Symbol* symbol = lookup(name);
    return symbol ? symbol->isMutable : false;
}

// ============================================================
// SEMANTIC ANALYZER
// ============================================================

void SemanticAnalyzer::reportError(const std::string& message, const SourceLocation& loc) {
    ErrorReporter::instance().reportError("Semantic", message, loc);
    errors.push_back(message);
}

bool SemanticAnalyzer::typesMatch(std::shared_ptr<Type> t1, std::shared_ptr<Type> t2) {
    if (!t1 || !t2) return false;
    // TODO: Implement proper type checking including coercion rules
    return t1->kind == t2->kind && t1->name == t2->name;
}

void SemanticAnalyzer::setCurrentFile(const std::string& filePath) {
    namespace fs = std::filesystem;
    currentDirectory = fs::path(filePath).parent_path().string();
    if (currentDirectory.empty()) {
        currentDirectory = ".";
    }
}

void SemanticAnalyzer::analyze(std::shared_ptr<Program> program) {
    // Register built-in functions
    auto voidType = std::make_shared<Type>(TypeKind::VOID, "void");
    auto intType = std::make_shared<Type>(TypeKind::INT, "int");
    auto floatType = std::make_shared<Type>(TypeKind::FLOAT, "float");
    auto stringType = std::make_shared<Type>(TypeKind::STRING, "string");
    auto boolType = std::make_shared<Type>(TypeKind::BOOL, "bool");
    
    // Register print function
    symbolTable.define("print", voidType, false, true);
    
    // Register println function
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
    
    if (program) {
        program->accept(*this);
    }
}

// ============================================================
// VISITOR IMPLEMENTATIONS - TODO: Implement semantic checks
// ============================================================

void SemanticAnalyzer::visit(IntLiteralExpr& node) {
    node.type = std::make_shared<Type>(TypeKind::INT, "int");
}

void SemanticAnalyzer::visit(FloatLiteralExpr& node) {
    node.type = std::make_shared<Type>(TypeKind::FLOAT, "float");
}

void SemanticAnalyzer::visit(StringLiteralExpr& node) {
    node.type = std::make_shared<Type>(TypeKind::STRING, "string");
}

void SemanticAnalyzer::visit(BoolLiteralExpr& node) {
    node.type = std::make_shared<Type>(TypeKind::BOOL, "bool");
}

void SemanticAnalyzer::visit(IdentifierExpr& node) {
    // TODO: Look up identifier in symbol table
    auto* symbol = symbolTable.lookup(node.name);
    if (symbol) {
        node.type = symbol->type;
    } else {
        reportError("Undefined identifier: " + node.name, node.location);
        node.type = std::make_shared<Type>(TypeKind::UNKNOWN);
    }
}

void SemanticAnalyzer::visit(BinaryExpr& node) {
    // Type check operands
    if (node.left) node.left->accept(*this);
    if (node.right) node.right->accept(*this);
    
    // Infer result type
    if (node.left && node.left->type) {
        // For arithmetic and comparison operators, result follows left operand type
        // (simplified - real implementation would check operator and do coercion)
        switch (node.op) {
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

void SemanticAnalyzer::visit(UnaryExpr& node) {
    // Type check operand
    if (node.operand) {
        node.operand->accept(*this);
        // Result type is same as operand for -, !
        if (node.operand->type) {
            node.type = node.operand->type;
        }
    }
}

void SemanticAnalyzer::visit(CallExpr& node) {
    // Type check function call
    if (node.callee) node.callee->accept(*this);
    for (auto& arg : node.arguments) {
        if (arg) arg->accept(*this);
    }
    
    // Infer return type from function
    if (auto* idExpr = dynamic_cast<IdentifierExpr*>(node.callee.get())) {
        auto* symbol = symbolTable.lookup(idExpr->name);
        if (symbol && symbol->isFunction) {
            node.type = symbol->type;
        }
    }
}

void SemanticAnalyzer::visit(MemberAccessExpr& node) {
    // Type check the object
    if (node.object) {
        node.object->accept(*this);
        
        // Check that it's a struct
        if (node.object->type && node.object->type->kind == TypeKind::STRUCT) {
            // Look up the field type from struct definition
            auto structIt = structFields.find(node.object->type->name);
            if (structIt != structFields.end()) {
                auto fieldIt = structIt->second.find(node.member);
                if (fieldIt != structIt->second.end()) {
                    node.type = fieldIt->second;
                } else {
                    reportError("Unknown field '" + node.member + "' in struct '" + node.object->type->name + "'", node.location);
                    node.type = std::make_shared<Type>(TypeKind::UNKNOWN, "unknown");
                }
            } else {
                reportError("Unknown struct type: " + node.object->type->name, node.location);
                node.type = std::make_shared<Type>(TypeKind::UNKNOWN, "unknown");
            }
        } else if (node.object->type) {
            reportError("Member access on non-struct type", node.location);
        }
    }
}

void SemanticAnalyzer::visit(StructInitExpr& node) {
    // TODO: Implement struct initialization type checking
    for (auto& field : node.fieldValues) {
        if (field) field->accept(*this);
    }
}

void SemanticAnalyzer::visit(ArrayLiteralExpr& node) {
    // Type check all elements
    std::shared_ptr<Type> elementType = nullptr;
    
    for (auto& elem : node.elements) {
        if (elem) {
            elem->accept(*this);
            
            // Infer array element type from first element
            if (!elementType && elem->type) {
                elementType = elem->type;
            }
            
            // Check that all elements have the same type
            if (elementType && elem->type && !typesMatch(elem->type, elementType)) {
                reportError("Array elements must all have the same type", node.location);
            }
        }
    }
    
    // Set the array type
    if (elementType) {
        auto arrayType = std::make_shared<Type>(TypeKind::ARRAY, "array");
        arrayType->typeParams.push_back(elementType);
        node.type = arrayType;
    } else {
        node.type = std::make_shared<Type>(TypeKind::ARRAY, "array");
    }
}

void SemanticAnalyzer::visit(IndexExpr& node) {
    // Type check the array expression
    if (node.array) {
        node.array->accept(*this);
        
        // Check that it's actually an array
        if (node.array->type && node.array->type->kind != TypeKind::ARRAY) {
            reportError("Cannot index non-array type", node.location);
        }
        
        // Set the result type to the element type
        if (node.array->type && !node.array->type->typeParams.empty()) {
            node.type = node.array->type->typeParams[0];
        }
    }
    
    // Type check the index expression
    if (node.index) {
        node.index->accept(*this);
        
        // Check that index is an integer
        if (node.index->type && node.index->type->kind != TypeKind::INT) {
            reportError("Array index must be an integer", node.location);
        }
    }
}

void SemanticAnalyzer::visit(ExprStmt& node) {
    if (node.expression) {
        node.expression->accept(*this);
    }
}

void SemanticAnalyzer::visit(VarDeclStmt& node) {
    // Type check initializer
    if (node.initializer) {
        node.initializer->accept(*this);
    }
    
    // Type inference: if no explicit type, infer from initializer
    std::shared_ptr<Type> varType = node.declaredType;
    if (!varType && node.initializer && node.initializer->type) {
        varType = node.initializer->type;
    }
    
    // Check for redefinition
    if (symbolTable.isDefined(node.name)) {
        reportError("Redefinition of variable: " + node.name, node.location);
    } else {
        if (varType) {
            symbolTable.define(node.name, varType, node.isMutable);
        } else {
            reportError("Cannot infer type for variable: " + node.name, node.location);
        }
    }
}

void SemanticAnalyzer::visit(AssignmentStmt& node) {
    // Check that the variable exists and is mutable
    if (!symbolTable.isDefined(node.target)) {
        reportError("Assignment to undefined variable: " + node.target, node.location);
        return;
    }
    
    if (!symbolTable.isMutable(node.target)) {
        reportError("Cannot assign to immutable variable: " + node.target, node.location);
        return;
    }
    
    // Type check the value
    if (node.value) {
        node.value->accept(*this);
    }
}

void SemanticAnalyzer::visit(ReturnStmt& node) {
    // TODO: Check return type matches function return type
    if (node.value) {
        node.value->accept(*this);
    }
}

void SemanticAnalyzer::visit(IfStmt& node) {
    if (node.condition) {
        node.condition->accept(*this);
    }
    
    symbolTable.enterScope();
    for (auto& stmt : node.thenBranch) {
        if (stmt) stmt->accept(*this);
    }
    symbolTable.exitScope();
    
    if (!node.elseBranch.empty()) {
        symbolTable.enterScope();
        for (auto& stmt : node.elseBranch) {
            if (stmt) stmt->accept(*this);
        }
        symbolTable.exitScope();
    }
}

void SemanticAnalyzer::visit(ForStmt& node) {
    // Check range expressions or iterable
    if (node.rangeStart) {
        node.rangeStart->accept(*this);
    }
    if (node.rangeEnd) {
        node.rangeEnd->accept(*this);
    }
    if (node.iterable) {
        node.iterable->accept(*this);
    }
    
    // Enter new scope for loop body
    symbolTable.enterScope();
    
    // Add iterator variable to scope (defaults to int for ranges)
    auto iterType = std::make_shared<Type>(TypeKind::INT, "int");
    symbolTable.define(node.iteratorVar, iterType, false); // false = immutable
    
    // Check body statements
    for (auto& stmt : node.body) {
        if (stmt) stmt->accept(*this);
    }
    
    symbolTable.exitScope();
}

void SemanticAnalyzer::visit(WhileStmt& node) {
    // Check condition is boolean
    if (node.condition) {
        node.condition->accept(*this);
        // TODO: Verify condition type is boolean
    }
    
    // Check body statements
    symbolTable.enterScope();
    for (auto& stmt : node.body) {
        if (stmt) stmt->accept(*this);
    }
    symbolTable.exitScope();
}

void SemanticAnalyzer::visit(BlockStmt& node) {
    symbolTable.enterScope();
    for (auto& stmt : node.statements) {
        if (stmt) stmt->accept(*this);
    }
    symbolTable.exitScope();
}

void SemanticAnalyzer::visit(FunctionDecl& node) {
    // TODO: Implement function declaration checking
    symbolTable.define(node.name, node.returnType, false, true);
    
    symbolTable.enterScope();
    currentFunctionReturnType = node.returnType;
    
    // Add parameters to scope
    for (const auto& param : node.parameters) {
        symbolTable.define(param.name, param.type, false);
    }
    
    // Check body
    for (auto& stmt : node.body) {
        if (stmt) stmt->accept(*this);
    }
    
    currentFunctionReturnType = nullptr;
    symbolTable.exitScope();
}

void SemanticAnalyzer::visit(StructDecl& node) {
    // Register struct type
    auto structType = std::make_shared<Type>(TypeKind::STRUCT, node.name);
    symbolTable.define(node.name, structType, false);
    
    // Register field types for member access
    std::map<std::string, std::shared_ptr<Type>> fields;
    for (const auto& field : node.fields) {
        fields[field.name] = field.type;
    }
    structFields[node.name] = fields;
}

void SemanticAnalyzer::visit(TypeDefDecl& node) {
    // Register type alias
    typeAliases[node.name] = node.aliasedType;
}

std::shared_ptr<Type> SemanticAnalyzer::resolveTypeAlias(std::shared_ptr<Type> type) {
    if (!type) return type;
    
    // Check if this type name is an alias
    auto it = typeAliases.find(type->name);
    if (it != typeAliases.end()) {
        // Recursively resolve in case of chained aliases
        return resolveTypeAlias(it->second);
    }
    
    return type;
}

void SemanticAnalyzer::visit(LinkDecl& node) {
    // TODO: Implement link declaration checking
    // Register foreign functions
    for (auto& func : node.functions) {
        if (func) func->accept(*this);
    }
}

std::string SemanticAnalyzer::resolveModulePath(const std::string& importPath) {
    namespace fs = std::filesystem;
    
    // If absolute path, use as-is
    if (fs::path(importPath).is_absolute()) {
        return importPath;
    }
    
    // Otherwise, resolve relative to current directory
    fs::path fullPath = fs::path(currentDirectory) / importPath;
    
    // Normalize the path
    return fs::canonical(fullPath).string();
}

std::shared_ptr<Program> SemanticAnalyzer::loadModule(const std::string& modulePath) {
    // Check if already loaded
    auto it = loadedModules.find(modulePath);
    if (it != loadedModules.end()) {
        return it->second;
    }
    
    // Read the file
    std::ifstream file(modulePath);
    if (!file.is_open()) {
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
    
    for (auto& decl : program->declarations) {
        if (decl) {
            decl->accept(*this);
        }
    }
    
    currentDirectory = savedDir;
    
    return program;
}

void SemanticAnalyzer::importSymbolsFrom(std::shared_ptr<Program> module, 
                                          const std::vector<std::string>& symbols,
                                          const std::string& alias) {
    if (!module) return;
    
    // If alias is provided, we need to track imported symbols under that namespace
    // For now, implement simple direct import of specified symbols
    
    for (auto& decl : module->declarations) {
        FunctionDecl* funcDecl = dynamic_cast<FunctionDecl*>(decl.get());
        if (funcDecl) {
            // Check if this symbol should be imported
            if (symbols.empty() || std::find(symbols.begin(), symbols.end(), funcDecl->name) != symbols.end()) {
                std::string importName = alias.empty() ? funcDecl->name : alias + "." + funcDecl->name;
                symbolTable.define(importName, funcDecl->returnType, false, true);
                
                std::cout << "  Imported function: " << importName << std::endl;
            }
        }
        
        // TODO: Handle struct declarations, type definitions, etc.
    }
}

void SemanticAnalyzer::visit(ImportDecl& node) {
    std::cout << "Import: " << node.modulePath << std::endl;
    
    try {
        // Resolve the module path
        std::string resolvedPath = resolveModulePath(node.modulePath);
        
        // Load and parse the module
        auto module = loadModule(resolvedPath);
        
        // Import specified symbols (or all if none specified)
        importSymbolsFrom(module, node.imports, node.alias);
        
        std::cout << "  Module loaded successfully: " << resolvedPath << std::endl;
        
    } catch (const std::exception& e) {
        reportError("Failed to import module '" + node.modulePath + "': " + e.what(), node.location);
    }
}

void SemanticAnalyzer::visit(ModuleDecl& node) {
    // Register the module name
    // This could be used for namespacing in larger projects
    std::cout << "Module: " << node.name << std::endl;
}

void SemanticAnalyzer::visit(Program& node) {
    for (auto& decl : node.declarations) {
        if (decl) {
            decl->accept(*this);
        }
    }
}

} // namespace flow

