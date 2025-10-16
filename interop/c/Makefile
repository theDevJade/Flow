# C Library for Flow - Wraps FlowAPI from flowbase
CC = gcc
CXX = g++
BASE_CFLAGS = -Wall -Wextra -fPIC -O2 -I../../flowbase/include
LDFLAGS = -shared

# LLVM configuration - use LLVM_DIR from environment if available
ifdef LLVM_DIR
    LLVM_CONFIG = $(LLVM_DIR)/bin/llvm-config
else
    LLVM_CONFIG ?= $(shell which llvm-config-21 2>/dev/null || which llvm-config 2>/dev/null || echo llvm-config)
endif

# Get LLVM libraries and flags
LLVM_CXXFLAGS = $(shell $(LLVM_CONFIG) --cxxflags 2>/dev/null || echo "")
LLVM_LDFLAGS = $(shell $(LLVM_CONFIG) --ldflags 2>/dev/null || echo "")
LLVM_LIBS = $(shell $(LLVM_CONFIG) --libs core support irreader executionengine mcjit interpreter native aarch64codegen aarch64asmparser aarch64desc aarch64info aarch64utils x86codegen x86asmparser x86desc x86info 2>/dev/null || echo "")
LLVM_SYSTEM_LIBS = $(shell $(LLVM_CONFIG) --system-libs 2>/dev/null || echo "")

# Find libffi and determine target platform
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    # macOS
    FFI_CFLAGS = -I/opt/homebrew/include -I/usr/local/include
    FFI_LDFLAGS = -L/opt/homebrew/lib -L/usr/local/lib -lffi
    TARGET = libflow.dylib
else ifeq ($(OS),Windows_NT)
    # Windows - use vcpkg installed libffi
    VCPKG_ROOT ?= C:/vcpkg/installed/x64-windows
    FFI_CFLAGS = -I$(VCPKG_ROOT)/include
    # Try both libffi.lib and ffi.lib naming conventions
    FFI_LIB := $(shell if [ -f "$(VCPKG_ROOT)/lib/libffi.lib" ]; then echo "$(VCPKG_ROOT)/lib/libffi.lib"; elif [ -f "$(VCPKG_ROOT)/lib/ffi.lib" ]; then echo "$(VCPKG_ROOT)/lib/ffi.lib"; else echo "-lffi"; fi)
    FFI_LDFLAGS = $(FFI_LIB)
    TARGET = flow.dll
else
    # Linux
    FFI_CFLAGS = 
    FFI_LDFLAGS = -lffi
    TARGET = libflow.so
endif

# Combine base CFLAGS with platform-specific FFI flags
CFLAGS = $(BASE_CFLAGS) $(FFI_CFLAGS)

# Use pre-compiled object files from flowbase build
# Object file paths differ between Unix (nested) and Windows (flat)
ifeq ($(OS),Windows_NT)
    # Windows MSBuild: flat structure in flowjni.dir/Release/
    FLOWBASE_BUILD = ../../flowbase/build/CMakeFiles/flowjni.dir
    OBJ_EXT = .obj
    FLOWBASE_OBJECTS = \
	$(FLOWBASE_BUILD)/FlowAPI$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/Lexer$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/Token$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/Parser$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/AST$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/SemanticAnalyzer$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/CodeGenerator$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/FFIBridge$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/Interop$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/IPC$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/JVMInterop$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/ErrorReporter$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/Builtins$(OBJ_EXT)
else
    # Unix Makefiles: nested structure with src/ subdirectories
    FLOWBASE_BUILD = ../../flowbase/build/CMakeFiles/flowjni.dir
    OBJ_EXT = .o
    FLOWBASE_OBJECTS = \
	$(FLOWBASE_BUILD)/src/Embedding/FlowAPI.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Lexer/Lexer.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Lexer/Token.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Parser/Parser.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/AST/AST.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Sema/SemanticAnalyzer.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Codegen/CodeGenerator.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Runtime/FFIBridge.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Runtime/Interop.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Runtime/IPC.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Runtime/JVMInterop.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Common/ErrorReporter.cpp$(OBJ_EXT) \
	$(FLOWBASE_BUILD)/src/Stdlib/Builtins.cpp$(OBJ_EXT)
endif

C_SOURCES = flow.c flow_reflect.c
C_OBJECTS = $(C_SOURCES:.c=.o)

all: check_flowbase $(TARGET)

check_flowbase:
ifeq ($(OS),Windows_NT)
	@echo "Checking for flowbase objects on Windows..."
	@if [ ! -f $(FLOWBASE_BUILD)/FlowAPI$(OBJ_EXT) ]; then \
		echo "ERROR: flowbase objects not found. Please run:"; \
		echo "  cd ../../flowbase/build && cmake .. && cmake --build ."; \
		exit 1; \
	fi
else
	@if [ ! -f $(FLOWBASE_BUILD)/src/Embedding/FlowAPI.cpp$(OBJ_EXT) ]; then \
		echo "ERROR: flowbase not built. Please run:"; \
		echo "  cd ../../flowbase/build && cmake .. && cmake --build ."; \
		exit 1; \
	fi
endif

$(TARGET): $(C_OBJECTS) $(FLOWBASE_OBJECTS)
	$(CXX) $(LDFLAGS) -o $@ $^ $(LLVM_LDFLAGS) $(LLVM_LIBS) $(LLVM_SYSTEM_LIBS) $(FFI_LDFLAGS)

%.o: %.c flow.h
	$(CC) $(CFLAGS) $(FFI_CFLAGS) -c -o $@ $<

clean:
	rm -f $(C_OBJECTS) $(TARGET) test_flow

install: $(TARGET)
	cp $(TARGET) /usr/local/lib/
	cp flow.h /usr/local/include/

test: $(TARGET) test.c
	$(CC) $(CFLAGS) -o test_flow test.c -L. -lflow $(FFI_CFLAGS)
	@echo "Run with: LD_LIBRARY_PATH=. ./test_flow (Linux) or DYLD_LIBRARY_PATH=. ./test_flow (macOS)"

debug-llvm:
	@echo "LLVM_CONFIG: $(LLVM_CONFIG)"
	@echo "LLVM_CXXFLAGS: $(LLVM_CXXFLAGS)"
	@echo "LLVM_LDFLAGS: $(LLVM_LDFLAGS)"
	@echo "LLVM_LIBS: $(LLVM_LIBS)"
	@echo "LLVM_SYSTEM_LIBS: $(LLVM_SYSTEM_LIBS)"

.PHONY: all clean install test check_flowbase debug-llvm

