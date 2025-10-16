# C Library for Flow - Wraps FlowAPI from flowbase
CC = gcc
CXX = g++
CFLAGS = -Wall -Wextra -fPIC -O2 -I../../flowbase/include
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
    # Windows
    FFI_CFLAGS = 
    FFI_LDFLAGS = -lffi
    TARGET = flow.dll
else
    # Linux
    FFI_CFLAGS = 
    FFI_LDFLAGS = -lffi
    TARGET = libflow.so
endif

# Use pre-compiled object files from flowbase build
FLOWBASE_BUILD = ../../flowbase/build/CMakeFiles/flowjni.dir

# Object file extension varies by platform
ifeq ($(OS),Windows_NT)
    OBJ_EXT = .obj
else
    OBJ_EXT = .o
endif

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

C_SOURCES = flow.c flow_reflect.c
C_OBJECTS = $(C_SOURCES:.c=.o)

all: check_flowbase $(TARGET)

check_flowbase:
ifeq ($(OS),Windows_NT)
	@if not exist "..\..\flowbase\build\CMakeFiles\flowjni.dir\src\Embedding\FlowAPI.cpp.obj" ( \
		echo ERROR: flowbase not built. Please run: && \
		echo   cd ..\..\flowbase\build ^&^& cmake .. ^&^& cmake --build . && \
		exit /b 1 \
	)
else
	@if [ ! -f $(FLOWBASE_BUILD)/src/Embedding/FlowAPI.cpp$(OBJ_EXT) ]; then \
		echo "ERROR: flowbase not built. Please run:"; \
		echo "  cd ../../flowbase/build && cmake .. && make"; \
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

