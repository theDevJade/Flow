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

# Find libffi (required for FFI interop)
if(WIN32)
    # On Windows with vcpkg, use find_package
    find_package(libffi CONFIG REQUIRED)
    set(FFI_LIBRARIES libffi)
else()
    # On Unix systems, libffi is typically available system-wide
    if(APPLE)
        # macOS has libffi in /usr/local or via Homebrew
        find_path(FFI_INCLUDE_DIR ffi/ffi.h PATHS /usr/local/include /opt/homebrew/include)
        find_library(FFI_LIBRARY ffi PATHS /usr/local/lib /opt/homebrew/lib)
    else()
        # Linux typically has libffi in standard locations
        find_path(FFI_INCLUDE_DIR ffi.h)
        find_library(FFI_LIBRARY ffi)
    endif()
    
    if(FFI_INCLUDE_DIR AND FFI_LIBRARY)
        message(STATUS "Found libffi: ${FFI_LIBRARY}")
        include_directories(${FFI_INCLUDE_DIR})
        set(FFI_LIBRARIES ${FFI_LIBRARY})
    else()
        message(WARNING "libffi not found - FFI interop may not work")
        set(FFI_LIBRARIES "")
    endif()
endif()

# Find nlohmann/json
find_package(nlohmann_json QUIET)
if(NOT nlohmann_json_FOUND)
    # If not found via package manager, try to find it manually
    find_path(NLOHMANN_JSON_INCLUDE_DIR nlohmann/json.hpp
        PATHS /usr/include /usr/local/include /opt/homebrew/include
    )
    if(NLOHMANN_JSON_INCLUDE_DIR)
        message(STATUS "Found nlohmann/json: ${NLOHMANN_JSON_INCLUDE_DIR}")
        include_directories(${NLOHMANN_JSON_INCLUDE_DIR})
    else()
        message(FATAL_ERROR "nlohmann/json not found. Please install it via package manager or download the header file.")
    endif()
else()
    message(STATUS "Found nlohmann/json via package manager")
endif()

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
        src/Driver/MultiFileBuilder.cpp
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

target_link_libraries(flowbase ${llvm_libs} ${FFI_LIBRARIES})
target_link_libraries(flow-lsp ${llvm_libs} ${FFI_LIBRARIES})


find_package(JNI)
if (JNI_FOUND)
    message(STATUS "Found JNI: building Java bindings library")
    include_directories(${JNI_INCLUDE_DIRS})

    add_library(flowjni SHARED
            src/Embedding/FlowAPI.cpp
            src/Embedding/FlowJNI.cpp
            ${FLOW_COMMON_SOURCES}
    )

    target_link_libraries(flowjni ${llvm_libs} ${JNI_LIBRARIES} ${FFI_LIBRARIES})

    # Set output directory for JNI library
    set_target_properties(flowjni PROPERTIES
            LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../javabindings/src/main/resources/native"
            RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../javabindings/src/main/resources/native"
    )

    if (APPLE)
        set_target_properties(flowjni PROPERTIES
                SUFFIX ".dylib"
        )
    endif ()
else ()
    message(STATUS "JNI not found, skipping Java bindings library")
endif ()

# Enable warnings (but not for LLVM headers)
if (MSVC)
    target_compile_options(flowbase PRIVATE /W4)
    target_compile_options(flow-lsp PRIVATE /W4)
    if (JNI_FOUND)
        target_compile_options(flowjni PRIVATE /W4)
    endif ()
else ()
    # Use -Wall but disable specific warnings from LLVM headers
    target_compile_options(flowbase PRIVATE -Wall -Wno-unused-parameter)
    target_compile_options(flow-lsp PRIVATE -Wall -Wno-unused-parameter)
    if (JNI_FOUND)
        target_compile_options(flowjni PRIVATE -Wall -Wno-unused-parameter)
    endif ()
endif ()
