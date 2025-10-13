#include "../../include/Codegen/CodeGenerator.h"
#include "../../include/Lexer/Lexer.h"
#include "../../include/Parser/Parser.h"
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/Verifier.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/TargetSelect.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/Target/TargetOptions.h>
#include <llvm/MC/TargetRegistry.h>
#include <llvm/TargetParser/Triple.h>
#include <fstream>
#include <sstream>
#include <filesystem>
#include <iostream>
#include <set>

namespace flow {
    CodeGenerator::CodeGenerator(const std::string &moduleName)
        : currentValue(nullptr), currentDirectory(".") {
        context = std::make_unique<llvm::LLVMContext>();
        module = std::make_unique<llvm::Module>(moduleName, *context);
        builder = std::make_unique<llvm::IRBuilder<> >(*context);


        declareBuiltinFunctions();
    }

    void CodeGenerator::declareBuiltinFunctions() {
        // We use weak linkage to allow multiple definitions without conflicts

        std::vector<llvm::Type *> printfArgs = {llvm::PointerType::get(*context, 0)};
        llvm::FunctionType *printfType = llvm::FunctionType::get(
            llvm::Type::getInt32Ty(*context),
            printfArgs,
            true // variadic
        );
        llvm::Function::Create(printfType, llvm::Function::ExternalLinkage, "printf", module.get());


        llvm::FunctionType *sprintfType = llvm::FunctionType::get(
            llvm::Type::getInt32Ty(*context),
            {llvm::PointerType::get(*context, 0), llvm::PointerType::get(*context, 0)},
            true // variadic
        );
        llvm::Function::Create(sprintfType, llvm::Function::ExternalLinkage, "sprintf", module.get());


        llvm::FunctionType *strlenType = llvm::FunctionType::get(
            llvm::Type::getInt64Ty(*context),
            {llvm::PointerType::get(*context, 0)},
            false
        );
        llvm::Function::Create(strlenType, llvm::Function::ExternalLinkage, "strlen", module.get());

        llvm::FunctionType *mallocType = llvm::FunctionType::get(
            llvm::PointerType::get(*context, 0),
            {llvm::Type::getInt64Ty(*context)},
            false
        );
        llvm::Function::Create(mallocType, llvm::Function::ExternalLinkage, "malloc", module.get());

        llvm::FunctionType *strcpyType = llvm::FunctionType::get(
            llvm::PointerType::get(*context, 0),
            {llvm::PointerType::get(*context, 0), llvm::PointerType::get(*context, 0)},
            false
        );
        llvm::Function::Create(strcpyType, llvm::Function::ExternalLinkage, "strcpy", module.get());

        llvm::FunctionType *strcatType = llvm::FunctionType::get(
            llvm::PointerType::get(*context, 0),
            {llvm::PointerType::get(*context, 0), llvm::PointerType::get(*context, 0)},
            false
        );
        llvm::Function::Create(strcatType, llvm::Function::ExternalLinkage, "strcat", module.get());


        llvm::FunctionType *printType = llvm::FunctionType::get(
            llvm::Type::getVoidTy(*context),
            {llvm::PointerType::get(*context, 0)},
            false
        );
        llvm::Function *printFunc = llvm::Function::Create(
            printType,
            llvm::Function::WeakODRLinkage, // Weak linkage for multi-file compilation
            "print",
            module.get()
        );


        llvm::BasicBlock *entry = llvm::BasicBlock::Create(*context, "entry", printFunc);
        builder->SetInsertPoint(entry);


        llvm::Argument *arg = printFunc->arg_begin();
        arg->setName("str");


        llvm::Function *printfFunc = module->getFunction("printf");
        builder->CreateCall(printfFunc, {arg});

        // Return void
        builder->CreateRetVoid();

        // Create Flow's println function (print with newline)
        llvm::Function *printlnFunc = llvm::Function::Create(
            printType,
            llvm::Function::WeakODRLinkage, // Weak linkage for multi-file compilation
            "println",
            module.get()
        );


        llvm::BasicBlock *printlnEntry = llvm::BasicBlock::Create(*context, "entry", printlnFunc);
        builder->SetInsertPoint(printlnEntry);


        llvm::Argument *printlnArg = printlnFunc->arg_begin();
        printlnArg->setName("str");


        llvm::Value *formatStr = builder->CreateGlobalString("%s\n", "", 0, module.get());


        builder->CreateCall(printfFunc, {formatStr, printlnArg});

        // Return void
        builder->CreateRetVoid();


        // @TODO actually support length

        llvm::FunctionType *lenType = llvm::FunctionType::get(
            llvm::Type::getInt32Ty(*context),
            {llvm::PointerType::get(*context, 0)},
            false
        );
        llvm::Function *lenFunc = llvm::Function::Create(
            lenType,
            llvm::Function::WeakODRLinkage, // Weak linkage for multi-file compilation
            "len",
            module.get()
        );

        // For now, len() just returns 0 (placeholder)

        llvm::BasicBlock *lenEntry = llvm::BasicBlock::Create(*context, "entry", lenFunc);
        builder->SetInsertPoint(lenEntry);
        llvm::Value *lenResult = llvm::ConstantInt::get(*context, llvm::APInt(32, 0));
        builder->CreateRet(lenResult);




        // String: strlen
        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt32Ty(*context), {llvm::PointerType::get(*context, 0)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib11strlen_implEPKc", module.get());

        // String: substr
        llvm::Function::Create(
            llvm::FunctionType::get(llvm::PointerType::get(*context, 0),
                                    {
                                        llvm::PointerType::get(*context, 0), llvm::Type::getInt32Ty(*context),
                                        llvm::Type::getInt32Ty(*context)
                                    }, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib11substr_implEPKcii", module.get());

        // String: concat
        llvm::Function::Create(
            llvm::FunctionType::get(llvm::PointerType::get(*context, 0),
                                    {llvm::PointerType::get(*context, 0), llvm::PointerType::get(*context, 0)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib11concat_implEPKcS2_", module.get());

        // Math: abs, sqrt, pow, min, max
        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt32Ty(*context), {llvm::Type::getInt32Ty(*context)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib8abs_implEi", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getDoubleTy(*context), {llvm::Type::getDoubleTy(*context)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib9sqrt_implEd", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getDoubleTy(*context),
                                    {llvm::Type::getDoubleTy(*context), llvm::Type::getDoubleTy(*context)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib8pow_implEdd", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt32Ty(*context),
                                    {llvm::Type::getInt32Ty(*context), llvm::Type::getInt32Ty(*context)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib8min_implEii", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt32Ty(*context),
                                    {llvm::Type::getInt32Ty(*context), llvm::Type::getInt32Ty(*context)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib8max_implEii", module.get());

        // I/O: readLine, readInt, writeFile, readFile
        llvm::Function::Create(
            llvm::FunctionType::get(llvm::PointerType::get(*context, 0), {}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib13readLine_implEv", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt32Ty(*context), {}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib12readInt_implEv", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::Type::getInt1Ty(*context),
                                    {llvm::PointerType::get(*context, 0), llvm::PointerType::get(*context, 0)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib14writeFile_implEPKcS2_", module.get());

        llvm::Function::Create(
            llvm::FunctionType::get(llvm::PointerType::get(*context, 0), {llvm::PointerType::get(*context, 0)}, false),
            llvm::Function::ExternalLinkage, "_ZN4flow6stdlib13readFile_implEPKc", module.get());
    }

    llvm::Type *CodeGenerator::getLLVMType(std::shared_ptr<Type> flowType) {
        if (!flowType) {
            return llvm::Type::getVoidTy(*context);
        }

        flowType = resolveTypeAlias(flowType);

        switch (flowType->kind) {
            case TypeKind::INT:
                return llvm::Type::getInt32Ty(*context);
            case TypeKind::FLOAT:
                return llvm::Type::getDoubleTy(*context);
            case TypeKind::BOOL:
                return llvm::Type::getInt1Ty(*context);
            case TypeKind::STRING:
                return llvm::PointerType::get(*context, 0);
            case TypeKind::VOID:
                return llvm::Type::getVoidTy(*context);
            case TypeKind::STRUCT:
                // Special handling for builtin Option<T> struct
                if (flowType->name == "Option" && !flowType->typeParams.empty()) {
                    std::string optionKey = "Option<" + flowType->typeParams[0]->toString() + ">";
                    if (structTypes.find(optionKey) != structTypes.end()) {
                        return structTypes[optionKey];
                    }
                    // Create Option<T> as { i1 hasValue, T value }
                    std::vector<llvm::Type *> optionFields;
                    optionFields.push_back(llvm::Type::getInt1Ty(*context));
                    optionFields.push_back(getLLVMType(flowType->typeParams[0]));
                    llvm::StructType *optionType = llvm::StructType::create(*context, optionFields, optionKey);
                    structTypes[optionKey] = optionType;
                    // Register field indices for Option<T>
                    structFieldIndices[optionKey]["hasValue"] = 0;
                    structFieldIndices[optionKey]["value"] = 1;
                    return optionType;
                }

                if (structTypes.find(flowType->name) != structTypes.end()) {
                    return structTypes[flowType->name];
                }
                return llvm::StructType::create(*context, flowType->name);
            case TypeKind::ARRAY:
                if (!flowType->typeParams.empty()) {
                    llvm::Type *elemType = getLLVMType(flowType->typeParams[0]);
                    return llvm::PointerType::get(elemType, 0);
                }
                return llvm::PointerType::get(llvm::Type::getInt8Ty(*context), 0);
            case TypeKind::FUNCTION:
                return llvm::PointerType::get(*context, 0);
            case TypeKind::UNKNOWN:
            default:
                return llvm::Type::getVoidTy(*context);
        }
    }

    std::shared_ptr<Type> CodeGenerator::resolveTypeAlias(std::shared_ptr<Type> type) {
        if (!type) return type;

        // Check if this type name is an alias
        auto it = typeAliases.find(type->name);
        if (it != typeAliases.end()) {
            // Recursively resolve in case of chained aliases
            return resolveTypeAlias(it->second);
        }

        return type;
    }

    llvm::FunctionType *CodeGenerator::getFunctionType(FunctionDecl &funcDecl) {
        llvm::Type *returnType = getLLVMType(funcDecl.returnType);

        std::vector<llvm::Type *> paramTypes;
        for (const auto &param: funcDecl.parameters) {
            paramTypes.push_back(getLLVMType(param.type));
        }

        return llvm::FunctionType::get(returnType, paramTypes, false);
    }

    void CodeGenerator::declareExternalFunction(FunctionDecl &funcDecl) {
        // Check if function already exists
        if (module->getFunction(funcDecl.name)) {
            return; // Already declared
        }

        // Create LLVM function type
        llvm::FunctionType *FT = getFunctionType(funcDecl);

        // Create external function declaration (no body)
        llvm::Function::Create(
            FT,
            llvm::Function::ExternalLinkage,
            funcDecl.name,
            module.get()
        );
    }

    void CodeGenerator::generate(std::shared_ptr<Program> program) {
        if (program) {
            // Set current directory to the program's source file directory if available
            if (!program->declarations.empty() && program->declarations[0]) {
                if (!program->declarations[0]->location.filename.empty()) {
                    namespace fs = std::filesystem;
                    currentDirectory = fs::path(program->declarations[0]->location.filename)
                            .parent_path().string();
                    if (currentDirectory.empty()) {
                        currentDirectory = ".";
                    }
                }
            }
            program->accept(*this);
        }
    }

    void CodeGenerator::dumpIR() {
        module->print(llvm::outs(), nullptr);
    }

    void CodeGenerator::writeIRToFile(const std::string &filename) {
        std::error_code EC;
        llvm::raw_fd_ostream dest(filename, EC, llvm::sys::fs::OF_None);

        if (EC) {
            std::cerr << "Could not open file: " << EC.message() << std::endl;
            return;
        }

        module->print(dest, nullptr);
    }

    void CodeGenerator::compileToObject(const std::string &filename) {

        llvm::InitializeNativeTarget();
        llvm::InitializeNativeTargetAsmPrinter();
        llvm::InitializeNativeTargetAsmParser();

        
        std::string targetTripleStr = module->getTargetTriple().getTriple();
        
        if (targetTripleStr.empty()) {
#ifdef __APPLE__
            targetTripleStr = "arm64-apple-darwin"; // macOS ARM
#elif __linux__
            targetTripleStr = "x86_64-unknown-linux-gnu";
#elif _WIN32
            targetTripleStr = "x86_64-pc-windows-msvc";
#else
            targetTripleStr = "x86_64-unknown-unknown";
#endif
            module->setTargetTriple(targetTripleStr);
        }
        
        // Create Triple object for target lookup
        llvm::Triple targetTriple(targetTripleStr);

        // Look up the target
        std::string error;
        const llvm::Target *target = llvm::TargetRegistry::lookupTarget(targetTripleStr, error);

        if (!target) {
            std::cerr << "Error: " << error << std::endl;
            return;
        }


        llvm::TargetOptions opt;
        llvm::TargetMachine *targetMachine = target->createTargetMachine(
            targetTriple,
            "generic",
            "",
            opt,
            llvm::Reloc::PIC_
        );

        module->setDataLayout(targetMachine->createDataLayout());

        // Open output file
        std::error_code EC;
        llvm::raw_fd_ostream dest(filename, EC, llvm::sys::fs::OF_None);

        if (EC) {
            std::cerr << "Could not open file: " << EC.message() << std::endl;
            return;
        }

        // Emit object file
        llvm::legacy::PassManager pass;
        if (targetMachine->addPassesToEmitFile(pass, dest, nullptr, llvm::CodeGenFileType::ObjectFile)) {
            std::cerr << "TargetMachine can't emit a file of this type" << std::endl;
            return;
        }

        pass.run(*module);
        dest.flush();

        delete targetMachine;
    }





    void CodeGenerator::visit(IntLiteralExpr &node) {
        currentValue = llvm::ConstantInt::get(*context, llvm::APInt(32, node.value, true));
    }

    void CodeGenerator::visit(FloatLiteralExpr &node) {
        currentValue = llvm::ConstantFP::get(*context, llvm::APFloat(node.value));
    }

    void CodeGenerator::visit(StringLiteralExpr &node) {

        currentValue = builder->CreateGlobalString(node.value, "", 0, module.get());
    }

    void CodeGenerator::visit(BoolLiteralExpr &node) {
        currentValue = llvm::ConstantInt::get(*context, llvm::APInt(1, node.value, false));
    }

    void CodeGenerator::visit(IdentifierExpr &node) {
        auto it = namedValues.find(node.name);
        if (it != namedValues.end()) {
            currentValue = builder->CreateLoad(getLLVMType(node.type), it->second, node.name);
        } else {
            std::cerr << "Unknown variable: " << node.name << std::endl;
            currentValue = nullptr;
        }
    }

    void CodeGenerator::visit(BinaryExpr &node) {
        // Handle string concatenation specially
        if (node.op == TokenType::PLUS) {
            // Resolve type aliases before checking types
            auto leftType = resolveTypeAlias(node.left->type);
            auto rightType = resolveTypeAlias(node.right->type);

            // Check if either operand is a string type
            bool leftIsString = (leftType && leftType->kind == TypeKind::STRING);
            bool rightIsString = (rightType && rightType->kind == TypeKind::STRING);

            if (leftIsString || rightIsString) {
                // Generate string concatenation code
                node.left->accept(*this);
                llvm::Value *L = currentValue;
                llvm::Type *LType = L->getType();

                node.right->accept(*this);
                llvm::Value *R = currentValue;
                llvm::Type *RType = R->getType();

                // Get format string and call sprintf
                llvm::Function *sprintfFunc = module->getFunction("sprintf");
                llvm::Function *mallocFunc = module->getFunction("malloc");
                llvm::Function *strlenFunc = module->getFunction("strlen");

                // Allocate buffer for result (512 bytes should be enough)
                llvm::Value *bufferSize = llvm::ConstantInt::get(*context, llvm::APInt(64, 512));
                llvm::Value *buffer = builder->CreateCall(mallocFunc, {bufferSize}, "strbuf");

                // Create format string based on types
                std::string formatStr;
                std::vector<llvm::Value *> sprintfArgs;
                sprintfArgs.push_back(buffer);

                if (LType->isPointerTy() && RType->isPointerTy()) {
                    // String + String
                    formatStr = "%s%s";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                } else if (LType->isPointerTy() && RType->isIntegerTy(32)) {
                    // String + Int
                    formatStr = "%s%d";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                } else if (LType->isPointerTy() && RType->isDoubleTy()) {
                    // String + Float
                    formatStr = "%s%f";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                } else if (LType->isIntegerTy(32) && RType->isPointerTy()) {
                    // Int + String
                    formatStr = "%d%s";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                } else if (LType->isDoubleTy() && RType->isPointerTy()) {
                    // Float + String
                    formatStr = "%f%s";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                } else {
                    // Fallback
                    formatStr = "%s%s";
                    sprintfArgs.push_back(builder->CreateGlobalString(formatStr, "", 0, module.get()));
                    sprintfArgs.push_back(L);
                    sprintfArgs.push_back(R);
                }

                // Rearrange args: buffer, format, then values
                std::vector<llvm::Value *> finalArgs;
                finalArgs.push_back(buffer);
                finalArgs.push_back(sprintfArgs[1]); // format string
                for (size_t i = 2; i < sprintfArgs.size(); i++) {
                    finalArgs.push_back(sprintfArgs[i]);
                }

                builder->CreateCall(sprintfFunc, finalArgs);
                currentValue = buffer;
                return;
            }
        }

        // Regular numeric operations
        node.left->accept(*this);
        llvm::Value *L = currentValue;

        node.right->accept(*this);
        llvm::Value *R = currentValue;

        if (!L || !R) {
            currentValue = nullptr;
            return;
        }

        // Basic arithmetic operations
        bool isFloat = L->getType()->isFloatingPointTy();

        switch (node.op) {
            case TokenType::PLUS:
                currentValue = isFloat ? builder->CreateFAdd(L, R, "addtmp") : builder->CreateAdd(L, R, "addtmp");
                break;
            case TokenType::MINUS:
                currentValue = isFloat ? builder->CreateFSub(L, R, "subtmp") : builder->CreateSub(L, R, "subtmp");
                break;
            case TokenType::STAR:
                currentValue = isFloat ? builder->CreateFMul(L, R, "multmp") : builder->CreateMul(L, R, "multmp");
                break;
            case TokenType::SLASH:
                currentValue = isFloat ? builder->CreateFDiv(L, R, "divtmp") : builder->CreateSDiv(L, R, "divtmp");
                break;
            case TokenType::PERCENT:
                currentValue = isFloat ? builder->CreateFRem(L, R, "modtmp") : builder->CreateSRem(L, R, "modtmp");
                break;
            case TokenType::LT:
                currentValue = builder->CreateICmpSLT(L, R, "cmptmp");
                break;
            case TokenType::LE:
                currentValue = builder->CreateICmpSLE(L, R, "cmptmp");
                break;
            case TokenType::GT:
                currentValue = builder->CreateICmpSGT(L, R, "cmptmp");
                break;
            case TokenType::GE:
                currentValue = builder->CreateICmpSGE(L, R, "cmptmp");
                break;
            case TokenType::EQ:
                currentValue = builder->CreateICmpEQ(L, R, "cmptmp");
                break;
            case TokenType::NE:
                currentValue = builder->CreateICmpNE(L, R, "cmptmp");
                break;
            case TokenType::AND:
                // Logical AND - convert operands to boolean
                if (L->getType()->isIntegerTy(32)) {
                    L = builder->CreateICmpNE(L, llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "tobool");
                }
                if (R->getType()->isIntegerTy(32)) {
                    R = builder->CreateICmpNE(R, llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "tobool");
                }
                currentValue = builder->CreateAnd(L, R, "andtmp");
                break;
            case TokenType::OR:
                // Logical OR - convert operands to boolean
                if (L->getType()->isIntegerTy(32)) {
                    L = builder->CreateICmpNE(L, llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "tobool");
                }
                if (R->getType()->isIntegerTy(32)) {
                    R = builder->CreateICmpNE(R, llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "tobool");
                }
                currentValue = builder->CreateOr(L, R, "ortmp");
                break;
            default:
                std::cerr << "Unknown binary operator" << std::endl;
                currentValue = nullptr;
        }
    }

    void CodeGenerator::visit(UnaryExpr &node) {
        // Generate code for the operand
        node.operand->accept(*this);
        llvm::Value *operand = currentValue;

        if (!operand) {
            currentValue = nullptr;
            return;
        }

        // Apply unary operator
        switch (node.op) {
            case TokenType::MINUS:
                // Negate the value
                if (operand->getType()->isIntegerTy()) {
                    currentValue = builder->CreateNeg(operand, "negtmp");
                } else if (operand->getType()->isDoubleTy()) {
                    currentValue = builder->CreateFNeg(operand, "negtmp");
                } else {
                    std::cerr << "Cannot negate non-numeric type" << std::endl;
                    currentValue = nullptr;
                }
                break;
            case TokenType::NOT:
                // Logical NOT
                if (operand->getType()->isIntegerTy(1)) {
                    // Already boolean
                    currentValue = builder->CreateNot(operand, "nottmp");
                } else if (operand->getType()->isIntegerTy(32)) {
                    // Convert to boolean first
                    llvm::Value *boolVal = builder->CreateICmpNE(operand,
                                                                 llvm::ConstantInt::get(*context, llvm::APInt(32, 0)),
                                                                 "tobool");
                    currentValue = builder->CreateNot(boolVal, "nottmp");
                } else {
                    std::cerr << "Cannot apply NOT to non-boolean type" << std::endl;
                    currentValue = nullptr;
                }
                break;
            default:
                std::cerr << "Unknown unary operator" << std::endl;
                currentValue = nullptr;
        }
    }

    void CodeGenerator::visit(CallExpr &node) {
        // Get function name
        std::string funcName;
        if (auto *idExpr = dynamic_cast<IdentifierExpr *>(node.callee.get())) {
            funcName = idExpr->name;
        } else {
            std::cerr << "Complex function calls not yet supported" << std::endl;
            currentValue = nullptr;
            return;
        }

        // Check if this is a foreign function call
        auto foreignIt = foreignFunctions.find(funcName);
        if (foreignIt != foreignFunctions.end()) {



            // Evaluate arguments
            std::vector<llvm::Value *> args;
            for (auto &arg: node.arguments) {
                arg->accept(*this);
                if (currentValue) {
                    args.push_back(currentValue);
                }
            }

            // Look up the foreign function in the module
            llvm::Function *foreignFunc = module->getFunction(funcName);
            if (!foreignFunc) {
                std::cerr << "Error: Foreign function '" << funcName << "' not declared in module" << std::endl;
                currentValue = nullptr;
                return;
            }



            if (foreignFunc->getReturnType()->isVoidTy()) {
                builder->CreateCall(foreignFunc, args);
                currentValue = nullptr;
            } else {
                currentValue = builder->CreateCall(foreignFunc, args, funcName + "_result");
            }
            return;
        }

        // Special handling for len() function
        if (funcName == "len" && node.arguments.size() == 1) {
            node.arguments[0]->accept(*this);
            llvm::Value *arrayValue = currentValue;

            // If it's a variable, load it first
            if (auto *idExpr = dynamic_cast<IdentifierExpr *>(node.arguments[0].get())) {
                auto it = namedValues.find(idExpr->name);
                if (it != namedValues.end()) {
                    arrayValue = it->second; // Use the alloca pointer
                }
            }

            // Look up the length from our tracking map
            auto lengthIt = arrayLengths.find(arrayValue);
            if (lengthIt != arrayLengths.end()) {
                currentValue = llvm::ConstantInt::get(*context, llvm::APInt(32, lengthIt->second));
                return;
            } else {

                std::cerr << "Warning: Array length not tracked for len() call" << std::endl;
                currentValue = llvm::ConstantInt::get(*context, llvm::APInt(32, 0));
                return;
            }
        }

        // Map Flow stdlib names to C++ mangled names
        static std::map<std::string, std::string> stdlibMap = {
            {"strlen", "_ZN4flow6stdlib11strlen_implEPKc"},
            {"substr", "_ZN4flow6stdlib11substr_implEPKcii"},
            {"concat", "_ZN4flow6stdlib11concat_implEPKcS2_"},
            {"abs", "_ZN4flow6stdlib8abs_implEi"},
            {"sqrt", "_ZN4flow6stdlib9sqrt_implEd"},
            {"pow", "_ZN4flow6stdlib8pow_implEdd"},
            {"min", "_ZN4flow6stdlib8min_implEii"},
            {"max", "_ZN4flow6stdlib8max_implEii"},
            {"readLine", "_ZN4flow6stdlib13readLine_implEv"},
            {"readInt", "_ZN4flow6stdlib12readInt_implEv"},
            {"writeFile", "_ZN4flow6stdlib14writeFile_implEPKcS2_"},
            {"readFile", "_ZN4flow6stdlib13readFile_implEPKc"}
        };

        std::string lookupName = funcName;
        if (stdlibMap.find(funcName) != stdlibMap.end()) {
            lookupName = stdlibMap[funcName];
        }

        // Look up the function
        llvm::Function *function = module->getFunction(lookupName);
        if (!function) {
            std::cerr << "Unknown function: " << funcName << std::endl;
            currentValue = nullptr;
            return;
        }

        // Evaluate arguments
        std::vector<llvm::Value *> args;
        for (auto &arg: node.arguments) {
            arg->accept(*this);
            if (currentValue) {
                args.push_back(currentValue);
            }
        }

        // Create the call
        if (function->getReturnType()->isVoidTy()) {
            currentValue = builder->CreateCall(function, args);
        } else {
            currentValue = builder->CreateCall(function, args, "calltmp");
        }
    }

    void CodeGenerator::visit(MemberAccessExpr &node) {
        // Generate code for the object
        node.object->accept(*this);
        llvm::Value *objectPtr = currentValue;

        // Get the struct type
        if (!node.object->type || node.object->type->kind != TypeKind::STRUCT) {
            std::cerr << "Member access on non-struct type" << std::endl;
            currentValue = nullptr;
            return;
        }

        std::string structName = node.object->type->name;

        // Look up the struct type
        if (structTypes.find(structName) == structTypes.end()) {
            std::cerr << "Unknown struct type: " << structName << std::endl;
            currentValue = nullptr;
            return;
        }

        llvm::StructType *structType = structTypes[structName];

        // Find the field index
        int fieldIndex = -1;
        if (structFieldIndices.find(structName) != structFieldIndices.end()) {
            auto &fields = structFieldIndices[structName];
            if (fields.find(node.member) != fields.end()) {
                fieldIndex = fields[node.member];
            }
        }

        if (fieldIndex == -1) {
            std::cerr << "Unknown field: " << node.member << " in struct " << structName << std::endl;
            currentValue = nullptr;
            return;
        }

        // Get pointer to field using GEP
        llvm::Value *indices[] = {
            llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), // Struct index
            llvm::ConstantInt::get(*context, llvm::APInt(32, fieldIndex)) // Field index
        };

        llvm::Value *fieldPtr = builder->CreateGEP(structType, objectPtr, indices, "fieldptr");

        // Load the field value
        llvm::Type *fieldType = structType->getElementType(fieldIndex);
        currentValue = builder->CreateLoad(fieldType, fieldPtr, "fieldval");
    }

    void CodeGenerator::visit(StructInitExpr &node) {
        // Look up struct type
        auto it = structTypes.find(node.structName);
        if (it == structTypes.end()) {
            std::cerr << "Error: Unknown struct type: " << node.structName << std::endl;
            currentValue = nullptr;
            return;
        }

        llvm::StructType *structType = it->second;

        // Allocate struct on stack
        llvm::AllocaInst *structAlloca = builder->CreateAlloca(structType, nullptr, "struct");

        // Initialize fields
        auto &fieldIndices = structFieldIndices[node.structName];
        for (size_t i = 0; i < node.fieldValues.size() && i < fieldIndices.size(); i++) {
            // Evaluate field value
            node.fieldValues[i]->accept(*this);
            llvm::Value *fieldVal = currentValue;

            // Get pointer to field
            llvm::Value *fieldPtr = builder->CreateStructGEP(structType, structAlloca, i, "field");

            // Store value
            builder->CreateStore(fieldVal, fieldPtr);
        }

        // Load the struct value
        currentValue = builder->CreateLoad(structType, structAlloca, "structval");
    }

    void CodeGenerator::visit(ArrayLiteralExpr &node) {
        // Get element type from node type
        llvm::Type *elemType = nullptr;
        if (node.type && !node.type->typeParams.empty()) {
            elemType = getLLVMType(node.type->typeParams[0]);
        } else {
            elemType = llvm::Type::getInt32Ty(*context); // Default to int
        }

        // Allocate array on the stack
        int arrayLength = static_cast<int>(node.elements.size());
        llvm::Value *arraySize = llvm::ConstantInt::get(*context, llvm::APInt(32, arrayLength));
        llvm::AllocaInst *array = builder->CreateAlloca(elemType, arraySize, "array");

        // Track array length for len() function
        arrayLengths[array] = arrayLength;

        // Initialize array elements
        for (size_t i = 0; i < node.elements.size(); i++) {
            if (node.elements[i]) {
                node.elements[i]->accept(*this);
                llvm::Value *elemValue = currentValue;

                // Calculate element pointer
                llvm::Value *indices[] = {llvm::ConstantInt::get(*context, llvm::APInt(32, i))};
                llvm::Value *elemPtr = builder->CreateGEP(elemType, array, indices, "elemptr");

                // Store element value
                builder->CreateStore(elemValue, elemPtr);
            }
        }

        currentValue = array;
    }

    void CodeGenerator::visit(IndexExpr &node) {
        // Generate code for the array
        node.array->accept(*this);
        llvm::Value *arrayPtr = currentValue;

        // If it's a variable reference, get the alloca for bounds checking
        llvm::Value *arrayAlloca = arrayPtr;
        if (auto *idExpr = dynamic_cast<IdentifierExpr *>(node.array.get())) {
            auto it = namedValues.find(idExpr->name);
            if (it != namedValues.end()) {
                arrayAlloca = it->second;
            }
        }

        // Generate code for the index
        node.index->accept(*this);
        llvm::Value *indexValue = currentValue;

        // Array bounds checking
        auto lengthIt = arrayLengths.find(arrayAlloca);
        if (lengthIt != arrayLengths.end()) {
            int arrayLength = lengthIt->second;
            llvm::Value *lengthValue = llvm::ConstantInt::get(*context, llvm::APInt(32, arrayLength));

            // Check if index < 0
            llvm::Value *isNegative = builder->CreateICmpSLT(
                indexValue, llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "isneg");

            // Check if index >= length
            llvm::Value *isTooLarge = builder->CreateICmpSGE(indexValue, lengthValue, "istoolarge");

            // Combine checks: out of bounds = negative OR too large
            llvm::Value *isOutOfBounds = builder->CreateOr(isNegative, isTooLarge, "oob");

            // Create basic blocks for the bounds check
            llvm::Function *currentFunc = builder->GetInsertBlock()->getParent();
            llvm::BasicBlock *trapBlock = llvm::BasicBlock::Create(*context, "trap", currentFunc);
            llvm::BasicBlock *okBlock = llvm::BasicBlock::Create(*context, "indexok", currentFunc);

            // Branch based on bounds check
            builder->CreateCondBr(isOutOfBounds, trapBlock, okBlock);

            // Trap block: abort execution
            builder->SetInsertPoint(trapBlock);

            // Print error message using printf
            llvm::Function *printfFunc = module->getFunction("printf");
            if (printfFunc) {
                llvm::Value *errorMsg = builder->CreateGlobalString("Runtime Error: Array index out of bounds!\n",
                                                                     "errmsg");
                builder->CreateCall(printfFunc, {errorMsg});
            }

            // Call trap intrinsic to abort
            llvm::Function *trapFunc = llvm::Intrinsic::getDeclaration(module.get(), llvm::Intrinsic::trap);
            builder->CreateCall(trapFunc);
            builder->CreateUnreachable();

            // Continue in OK block
            builder->SetInsertPoint(okBlock);
        }

        // Get element type
        llvm::Type *elemType = nullptr;
        if (node.type) {
            elemType = getLLVMType(node.type);
        } else if (node.array->type && !node.array->type->typeParams.empty()) {
            elemType = getLLVMType(node.array->type->typeParams[0]);
        } else {
            elemType = llvm::Type::getInt32Ty(*context);
        }

        // Calculate element pointer using GEP
        llvm::Value *indices[] = {indexValue};
        llvm::Value *elemPtr = builder->CreateGEP(elemType, arrayPtr, indices, "indexptr");

        // Load the element value
        currentValue = builder->CreateLoad(elemType, elemPtr, "indexval");
    }

    void CodeGenerator::visit(ExprStmt &node) {
        if (node.expression) {
            node.expression->accept(*this);
        }
    }

    void CodeGenerator::visit(VarDeclStmt &node) {
        // Determine type: use declared type or infer from initializer
        llvm::Type *varType = nullptr;
        llvm::Value *initValue = nullptr;

        if (node.declaredType) {
            varType = getLLVMType(node.declaredType);
        } else if (node.initializer) {
            // Evaluate initializer first to get its type
            node.initializer->accept(*this);
            if (currentValue) {
                varType = currentValue->getType();
                initValue = currentValue;
            }
        }

        if (!varType) {
            std::cerr << "Error: Cannot determine type for variable: " << node.name << std::endl;
            return;
        }

        llvm::AllocaInst *alloca = builder->CreateAlloca(varType, nullptr, node.name);

        if (node.initializer) {
            // If we already evaluated it for type inference, use that value
            // Otherwise evaluate it now
            if (!initValue || node.declaredType) {
                node.initializer->accept(*this);
                initValue = currentValue;
            }
            if (initValue) {
                builder->CreateStore(initValue, alloca);

                // If the initializer was an array, track its length for this variable
                if (arrayLengths.find(initValue) != arrayLengths.end()) {
                    arrayLengths[alloca] = arrayLengths[initValue];
                }
            }
        }

        namedValues[node.name] = alloca;
    }

    void CodeGenerator::visit(AssignmentStmt &node) {
        // Look up the variable
        auto it = namedValues.find(node.target);
        if (it == namedValues.end()) {
            std::cerr << "Error: Undefined variable in assignment: " << node.target << std::endl;
            currentValue = nullptr;
            return;
        }

        // Generate the value expression
        if (node.value) {
            node.value->accept(*this);
            if (currentValue) {
                builder->CreateStore(currentValue, it->second);
            }
        }
    }

    void CodeGenerator::visit(ReturnStmt &node) {
        if (node.value) {
            node.value->accept(*this);
            builder->CreateRet(currentValue);
        } else {
            builder->CreateRetVoid();
        }
    }

    void CodeGenerator::visit(IfStmt &node) {
        // Generate condition
        node.condition->accept(*this);
        llvm::Value *condValue = currentValue;

        if (!condValue) {
            return;
        }

        // Convert condition to boolean if needed
        if (condValue->getType()->isIntegerTy(32)) {
            condValue = builder->CreateICmpNE(condValue,
                                              llvm::ConstantInt::get(*context, llvm::APInt(32, 0)), "ifcond");
        } else if (condValue->getType()->isDoubleTy()) {
            condValue = builder->CreateFCmpONE(condValue,
                                               llvm::ConstantFP::get(*context, llvm::APFloat(0.0)), "ifcond");
        }

        llvm::Function *function = builder->GetInsertBlock()->getParent();

        // Create blocks for then, else, and merge
        llvm::BasicBlock *thenBB = llvm::BasicBlock::Create(*context, "then", function);
        llvm::BasicBlock *elseBB = llvm::BasicBlock::Create(*context, "else");
        llvm::BasicBlock *mergeBB = llvm::BasicBlock::Create(*context, "ifcont");

        // Branch to then or else based on condition
        if (!node.elseBranch.empty()) {
            builder->CreateCondBr(condValue, thenBB, elseBB);
        } else {
            builder->CreateCondBr(condValue, thenBB, mergeBB);
        }

        // Emit then block
        builder->SetInsertPoint(thenBB);
        for (auto &stmt: node.thenBranch) {
            if (stmt) {
                stmt->accept(*this);
            }
        }

        // Branch to merge if no terminator was added
        if (!builder->GetInsertBlock()->getTerminator()) {
            builder->CreateBr(mergeBB);
        }

        // Emit else block if it exists
        if (!node.elseBranch.empty()) {
            function->insert(function->end(), elseBB);
            builder->SetInsertPoint(elseBB);
            for (auto &stmt: node.elseBranch) {
                if (stmt) {
                    stmt->accept(*this);
                }
            }

            // Branch to merge if no terminator was added
            if (!builder->GetInsertBlock()->getTerminator()) {
                builder->CreateBr(mergeBB);
            }
        }

        // Emit merge block
        function->insert(function->end(), mergeBB);
        builder->SetInsertPoint(mergeBB);
    }

    void CodeGenerator::visit(ForStmt &node) {
        llvm::Function *function = builder->GetInsertBlock()->getParent();

        // For now, only handle range-based for loops (i in start..end)
        if (auto *rangeExpr = dynamic_cast<BinaryExpr *>(node.iterable.get())) {
            if (rangeExpr->op == TokenType::DOUBLE_DOT) {
                // Create loop variable
                llvm::AllocaInst *loopVar = builder->CreateAlloca(
                    llvm::Type::getInt32Ty(*context), nullptr, node.iteratorVar);

                // Evaluate start value
                rangeExpr->left->accept(*this);
                llvm::Value *startVal = currentValue;
                builder->CreateStore(startVal, loopVar);

                // Evaluate end value
                rangeExpr->right->accept(*this);
                llvm::Value *endVal = currentValue;

                // Create loop blocks
                llvm::BasicBlock *loopBB = llvm::BasicBlock::Create(*context, "loop", function);
                llvm::BasicBlock *bodyBB = llvm::BasicBlock::Create(*context, "loopbody", function);
                llvm::BasicBlock *afterBB = llvm::BasicBlock::Create(*context, "afterloop", function);

                // Branch to loop
                builder->CreateBr(loopBB);

                // Loop condition block
                builder->SetInsertPoint(loopBB);
                llvm::Value *currentVal = builder->CreateLoad(
                    llvm::Type::getInt32Ty(*context), loopVar, "loopvar");
                llvm::Value *cond = builder->CreateICmpSLT(currentVal, endVal, "loopcond");
                builder->CreateCondBr(cond, bodyBB, afterBB);

                // Loop body
                builder->SetInsertPoint(bodyBB);

                // Add loop variable to scope
                auto oldVal = namedValues[node.iteratorVar];
                namedValues[node.iteratorVar] = loopVar;

                // Generate body
                for (auto &stmt: node.body) {
                    if (stmt) {
                        stmt->accept(*this);
                    }
                }

                // Increment loop variable
                llvm::Value *stepVal = llvm::ConstantInt::get(*context, llvm::APInt(32, 1));
                llvm::Value *nextVal = builder->CreateAdd(currentVal, stepVal, "nextvar");
                builder->CreateStore(nextVal, loopVar);

                // Branch back to loop condition
                builder->CreateBr(loopBB);

                // Restore old variable if it existed
                if (oldVal) {
                    namedValues[node.iteratorVar] = oldVal;
                } else {
                    namedValues.erase(node.iteratorVar);
                }

                // Continue after loop
                builder->SetInsertPoint(afterBB);
            } else {
                std::cerr << "Only range-based for loops are supported (i in 0..10)" << std::endl;
            }
        } else {
            std::cerr << "Only range-based for loops are supported" << std::endl;
        }
    }

    void CodeGenerator::visit(WhileStmt &node) {
        llvm::Function *function = builder->GetInsertBlock()->getParent();

        // Create basic blocks
        llvm::BasicBlock *condBB = llvm::BasicBlock::Create(*context, "whilecond", function);
        llvm::BasicBlock *bodyBB = llvm::BasicBlock::Create(*context, "whilebody", function);
        llvm::BasicBlock *afterBB = llvm::BasicBlock::Create(*context, "afterwhile", function);

        // Branch to condition
        builder->CreateBr(condBB);

        // Condition block
        builder->SetInsertPoint(condBB);
        if (node.condition) {
            node.condition->accept(*this);
            llvm::Value *condVal = currentValue;

            // Convert to i1 if needed
            if (condVal->getType()->isIntegerTy() && condVal->getType()->getIntegerBitWidth() != 1) {
                condVal = builder->CreateICmpNE(condVal,
                                                llvm::ConstantInt::get(condVal->getType(), 0), "whilecond");
            }

            builder->CreateCondBr(condVal, bodyBB, afterBB);
        } else {
            builder->CreateBr(afterBB);
        }

        // Body block
        builder->SetInsertPoint(bodyBB);
        for (auto &stmt: node.body) {
            if (stmt) {
                stmt->accept(*this);
            }
        }

        // Branch back to condition
        builder->CreateBr(condBB);

        // Continue after loop
        builder->SetInsertPoint(afterBB);
    }

    void CodeGenerator::visit(BlockStmt &node) {
        for (auto &stmt: node.statements) {
            if (stmt) {
                stmt->accept(*this);
            }
        }
    }

    void CodeGenerator::visit(FunctionDecl &node) {
        llvm::FunctionType *FT = getFunctionType(node);

        // For multi-file compilation, all functions need external linkage
        // so they can be called from other modules
        llvm::Function::LinkageTypes linkage = llvm::Function::ExternalLinkage;

        llvm::Function *F = llvm::Function::Create(
            FT,
            linkage,
            node.name,
            module.get()
        );

        // Set parameter names
        unsigned idx = 0;
        for (auto &arg: F->args()) {
            arg.setName(node.parameters[idx++].name);
        }

        // Create entry block
        llvm::BasicBlock *BB = llvm::BasicBlock::Create(*context, "entry", F);
        builder->SetInsertPoint(BB);

        // Add function parameters to scope
        namedValues.clear();
        for (auto &arg: F->args()) {
            llvm::AllocaInst *alloca = builder->CreateAlloca(arg.getType(), nullptr, arg.getName());
            builder->CreateStore(&arg, alloca);
            namedValues[std::string(arg.getName())] = alloca;
        }

        // Generate function body
        for (auto &stmt: node.body) {
            if (stmt) {
                stmt->accept(*this);
            }
        }

        llvm::BasicBlock *currentBlock = builder->GetInsertBlock();
        if (currentBlock && !currentBlock->getTerminator()) {
            if (node.returnType->isVoid()) {
                builder->CreateRetVoid();
            } else {
                // Fallback return for paths without explicit return
                // Semantic analysis should catch missing returns; this ensures valid LLVM IR
                builder->CreateRet(llvm::Constant::getNullValue(getLLVMType(node.returnType)));
            }
        }

        // Verify function
        std::string errStr;
        llvm::raw_string_ostream err(errStr);
        if (llvm::verifyFunction(*F, &err)) {
            std::cerr << "Function verification failed: " << errStr << std::endl;
        }
    }

    void CodeGenerator::visit(StructDecl &node) {
        // Create LLVM struct type
        std::vector<llvm::Type *> fieldTypes;
        std::map<std::string, int> fieldIndices;

        int index = 0;
        for (const auto &field: node.fields) {
            fieldTypes.push_back(getLLVMType(field.type));
            fieldIndices[field.name] = index++;
        }

        llvm::StructType *structType = llvm::StructType::create(*context, fieldTypes, node.name);
        structTypes[node.name] = structType;
        structFieldIndices[node.name] = fieldIndices;
    }

    void CodeGenerator::visit(TypeDefDecl &node) {
        // Register type alias for type resolution
        typeAliases[node.name] = node.aliasedType;
    }

    void CodeGenerator::visit(LinkDecl &node) {
        // Register foreign functions for IPC dispatch or direct linking (C adapter)
        for (auto &func: node.functions) {
            if (func) {
                // Track this as a foreign function
                ForeignFunctionInfo info;
                info.adapter = node.adapter;
                info.module = node.module;
                foreignFunctions[func->name] = info;

                // Create LLVM function declaration with external linkage
                // For C adapter, these will be resolved at link time
                // For other adapters (Python, JS), these will go through IPC at runtime
                llvm::FunctionType *FT = getFunctionType(*func);
                llvm::Function::Create(FT, llvm::Function::ExternalLinkage, func->name, module.get());
            }
        }
    }

    std::vector<std::string> CodeGenerator::getLinkedLibraries() const {
        std::vector<std::string> libraries;
        std::set<std::string> seen; // Avoid duplicates

        for (const auto &pair: foreignFunctions) {
            const ForeignFunctionInfo &info = pair.second;
            // Only include C adapter libraries (others use IPC at runtime)
            if (info.adapter == "c" && !info.module.empty() && seen.find(info.module) == seen.end()) {
                libraries.push_back(info.module);
                seen.insert(info.module);
            }
        }

        return libraries;
    }

    std::string CodeGenerator::resolveModulePath(const std::string &importPath) {
        namespace fs = std::filesystem;

        if (fs::path(importPath).is_absolute()) {
            return importPath;
        }

        // First try: resolve relative to current directory
        fs::path fullPath = fs::path(currentDirectory) / importPath;

        try {
            return fs::canonical(fullPath).string();
        } catch (const fs::filesystem_error &) {
            // If that fails, try library paths
            for (const auto &libPath : libraryPaths) {
                fs::path libFullPath = fs::path(libPath) / importPath;
                try {
                    return fs::canonical(libFullPath).string();
                } catch (const fs::filesystem_error &) {
                    // Continue to next library path
                }
            }
            
            // If all library paths fail, return the original path
            return fullPath.string();
        }
    }

    std::shared_ptr<Program> CodeGenerator::loadModule(const std::string &modulePath) {
        auto it = processedModules.find(modulePath);
        if (it != processedModules.end()) {
            return it->second;
        }

        std::ifstream file(modulePath);
        if (!file.is_open()) {
            throw std::runtime_error("Failed to open module: " + modulePath);
        }

        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string source = buffer.str();
        file.close();

        Lexer lexer(source, modulePath);
        std::vector<Token> tokens = lexer.tokenize();

        Parser parser(tokens);
        auto program = parser.parse();

        // Don't add to processedModules here - done in processImportedModule

        return program;
    }

    void CodeGenerator::processImportedModule(const std::string &modulePath) {
        // Check if already processed to prevent circular imports
        if (processedModules.find(modulePath) != processedModules.end()) {
            std::cerr << "[CODEGEN] Module already processed: " << modulePath << std::endl;
            return; // Already processed or currently being processed
        }

        try {
            std::cerr << "[CODEGEN] Loading module: " << modulePath << std::endl;
            // Mark as being processed BEFORE loading to prevent infinite loops
            processedModules[modulePath] = nullptr;

            // Load and parse the module
            std::cerr << "[CODEGEN] Opening file..." << std::endl;
            std::ifstream file(modulePath);
            if (!file.is_open()) {
                throw std::runtime_error("Failed to open module: " + modulePath);
            }
            std::cerr << "[CODEGEN] Reading file..." << std::endl;

            std::stringstream buffer;
            buffer << file.rdbuf();
            std::string source = buffer.str();
            file.close();
            std::cerr << "[CODEGEN] File read, starting lexer..." << std::endl;

            Lexer lexer(source, modulePath);
            std::cerr << "[CODEGEN] Tokenizing..." << std::endl;
            std::vector<Token> tokens = lexer.tokenize();
            std::cerr << "[CODEGEN] Tokens: " << tokens.size() << ", starting parser..." << std::endl;

            Parser parser(tokens);
            std::cerr << "[CODEGEN] Parsing..." << std::endl;
            auto program = parser.parse();
            std::cerr << "[CODEGEN] Parsed successfully" << std::endl;

            // Update with actual program
            processedModules[modulePath] = program;

            // Generate full code for imported module
            // This works because we're doing it BEFORE generating main module code
            std::cerr << "[CODEGEN] Generating code for " << program->declarations.size() << " items..." << std::endl;
            std::string savedDir = currentDirectory;
            currentDirectory = std::filesystem::path(modulePath).parent_path().string();

            for (auto &decl: program->declarations) {
                if (decl && !dynamic_cast<ImportDecl *>(decl.get())) {
                    decl->accept(*this);
                }
            }

            std::cerr << "[CODEGEN] Code generation complete for imported module" << std::endl;
            currentDirectory = savedDir;
        } catch (const std::exception &e) {
            std::cerr << "Error loading module " << modulePath << ": " << e.what() << std::endl;
        }
    }

    void CodeGenerator::visit(ImportDecl &node) {
        // Multi-file compilation note:
        // For proper multi-file support, each Flow module should be compiled separately
        // to an object file, then linked together. For now, imports are handled by
        // semantic analysis, and imported symbols become external references.
        std::cerr << "Note: Multi-file compilation is not fully supported yet." << std::endl;
        std::cerr << "      Each module should be compiled separately and linked." << std::endl;
        std::cerr << "      Import: " << node.modulePath << std::endl;
        (void) node;
    }

    void CodeGenerator::visit(ModuleDecl &node) {
        // Set the module name in LLVM module
        module->setModuleIdentifier(node.name);
    }

    void CodeGenerator::visit(Program &node) {
        for (auto &decl: node.declarations) {
            if (decl) {
                decl->accept(*this);
            }
        }
    }
} // namespace flow