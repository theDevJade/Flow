cmake_minimum_required(VERSION 3.20)
project(flowbase VERSION 0.1.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Find LLVM
find_package(LLVM REQUIRED CONFIG)
message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

include_directories(${LLVM_INCLUDE_DIRS})
separate_arguments(LLVM_DEFINITIONS_LIST NATIVE_COMMAND ${LLVM_DEFINITIONS})
add_definitions(${LLVM_DEFINITIONS_LIST})

# Include directories
include_directories(${CMAKE_SOURCE_DIR}/include)

set(FLOW_COMMON_SOURCES
    src/Lexer/Lexer.cpp
    src/Lexer/Token.cpp
    src/Parser/Parser.cpp
    src/AST/AST.cpp
    src/Sema/SemanticAnalyzer.cpp
    src/Codegen/CodeGenerator.cpp
    src/Driver/Driver.cpp
    src/Runtime/IPC.cpp
    src/Runtime/Interop.cpp
    src/Runtime/JVMInterop.cpp
    src/Runtime/FFIBridge.cpp
    src/Common/ErrorReporter.cpp
    src/Stdlib/Builtins.cpp
)

# Compiler executable
set(FLOW_COMPILER_SOURCES
    ${FLOW_COMMON_SOURCES}
    main.cpp
)

# Language Server executable
set(FLOW_LSP_SOURCES
    ${FLOW_COMMON_SOURCES}
    src/LSP/LanguageServer.cpp
    lsp_main.cpp
)

# Create compiler executable
add_executable(flowbase ${FLOW_COMPILER_SOURCES})

# Create LSP server executable
add_executable(flow-lsp ${FLOW_LSP_SOURCES})

# Link LLVM libraries
llvm_map_components_to_libnames(llvm_libs 
    core 
    support 
    irreader
    executionengine
    mcjit
    interpreter
    orcjit
    native
)

target_link_libraries(flowbase ${llvm_libs})
target_link_libraries(flow-lsp ${llvm_libs})


find_package(JNI)
if(JNI_FOUND)
    message(STATUS "Found JNI: building Java bindings library")
    include_directories(${JNI_INCLUDE_DIRS})
    
    add_library(flowjni SHARED
        src/Embedding/FlowAPI.cpp
        src/Embedding/FlowJNI.cpp
        ${FLOW_COMMON_SOURCES}
    )
    
    target_link_libraries(flowjni ${llvm_libs} ${JNI_LIBRARIES})
    
    # Set output directory for JNI library
    set_target_properties(flowjni PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../javabindings/src/main/resources/native"
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../javabindings/src/main/resources/native"
    )
    
    if(APPLE)
        set_target_properties(flowjni PROPERTIES
            SUFFIX ".dylib"
        )
    endif()
else()
    message(STATUS "JNI not found, skipping Java bindings library")
endif()

# Enable warnings (but not for LLVM headers)
if(MSVC)
    target_compile_options(flowbase PRIVATE /W4)
    target_compile_options(flow-lsp PRIVATE /W4)
    if(JNI_FOUND)
        target_compile_options(flowjni PRIVATE /W4)
    endif()
else()
    # Use -Wall but disable specific warnings from LLVM headers
    target_compile_options(flowbase PRIVATE -Wall -Wno-unused-parameter)
    target_compile_options(flow-lsp PRIVATE -Wall -Wno-unused-parameter)
    if(JNI_FOUND)
        target_compile_options(flowjni PRIVATE -Wall -Wno-unused-parameter)
    endif()
endif()
