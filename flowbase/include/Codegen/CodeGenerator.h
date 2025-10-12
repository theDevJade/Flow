#ifndef FLOW_CODEGEN_H
#define FLOW_CODEGEN_H

#include "../AST/AST.h"
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/Value.h>
#include <map>
#include <string>
#include <memory>

namespace flow {
    class CodeGenerator : public ASTVisitor {
    private:
        std::unique_ptr<llvm::LLVMContext> context;
        std::unique_ptr<llvm::Module> module;
        std::unique_ptr<llvm::IRBuilder<> > builder;

        std::map<std::string, llvm::Value *> namedValues;
        std::map<std::string, llvm::StructType *> structTypes;
        std::map<std::string, std::map<std::string, int> > structFieldIndices;


        std::map<std::string, std::shared_ptr<Type> > typeAliases;


        std::map<llvm::Value *, int> arrayLengths;

        // Foreign function tracking for IPC
        struct ForeignFunctionInfo {
            std::string adapter;
            std::string module;
        };

        std::map<std::string, ForeignFunctionInfo> foreignFunctions;

        // Track processed modules to avoid duplicates
        std::map<std::string, std::shared_ptr<Program> > processedModules;
        std::string currentDirectory;

        llvm::Value *currentValue; // Hold the result of the last visited expression

        llvm::Type *getLLVMType(std::shared_ptr<Type> flowType);

        llvm::FunctionType *getFunctionType(FunctionDecl &funcDecl);

        void declareBuiltinFunctions();

        std::shared_ptr<Type> resolveTypeAlias(std::shared_ptr<Type> type);

        // Module loading for imports
        std::string resolveModulePath(const std::string &importPath);

        std::shared_ptr<Program> loadModule(const std::string &modulePath);

        void processImportedModule(const std::string &modulePath);

    public:
        CodeGenerator(const std::string &moduleName);


        void generate(std::shared_ptr<Program> program);

        // Declare external function (for multi-file compilation)
        void declareExternalFunction(FunctionDecl &funcDecl);

        // Output methods
        void dumpIR();

        void writeIRToFile(const std::string &filename);

        void compileToObject(const std::string &filename);

        llvm::Module *getModule() { return module.get(); }

        // Get list of linked libraries (for Driver to pass to linker)
        std::vector<std::string> getLinkedLibraries() const;

        // Expression visitors
        void visit(IntLiteralExpr &node) override;

        void visit(FloatLiteralExpr &node) override;

        void visit(StringLiteralExpr &node) override;

        void visit(BoolLiteralExpr &node) override;

        void visit(IdentifierExpr &node) override;

        void visit(BinaryExpr &node) override;

        void visit(UnaryExpr &node) override;

        void visit(CallExpr &node) override;

        void visit(MemberAccessExpr &node) override;

        void visit(StructInitExpr &node) override;

        void visit(ArrayLiteralExpr &node) override;

        void visit(IndexExpr &node) override;

        // Statement visitors
        void visit(ExprStmt &node) override;

        void visit(VarDeclStmt &node) override;

        void visit(AssignmentStmt &node) override;

        void visit(ReturnStmt &node) override;

        void visit(IfStmt &node) override;

        void visit(ForStmt &node) override;

        void visit(WhileStmt &node) override;

        void visit(BlockStmt &node) override;

        // Declaration visitors
        void visit(FunctionDecl &node) override;

        void visit(StructDecl &node) override;

        void visit(TypeDefDecl &node) override;

        void visit(LinkDecl &node) override;

        void visit(ImportDecl &node) override;

        void visit(ModuleDecl &node) override;

        void visit(Program &node) override;
    };
} // namespace flow

#endif // FLOW_CODEGEN_H