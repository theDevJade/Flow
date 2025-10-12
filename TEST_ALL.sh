#!/bin/bash

echo "╔══════════════════════════════════════════════════╗"
echo "║  Flow Programming Language - Complete Test Suite, and yes I am a git guru who happened to use AI just for this one thing cause I HATE bash scripts :3 ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

PASS=0
FAIL=0

# Test 1: Compiler exists
echo "[ 1] Testing compiler executable..."
if [ -f "flowbase/build/flowbase" ]; then
    echo "     ✓ Compiler found"
    ((PASS++))
else
    echo "     ✗ Compiler not found"
    ((FAIL++))
fi

# Test 2: LSP exists
echo "[ 2] Testing language server executable..."
if [ -f "flowbase/build/flow-lsp" ]; then
    echo "     ✓ Language server found"
    ((PASS++))
else
    echo "     ✗ Language server not found"
    ((FAIL++))
fi

# Test 3: JNI library exists
echo "[ 3] Testing JNI library..."
if [ -f "javabindings/src/main/resources/native/libflowjni.dylib" ]; then
    echo "     ✓ JNI library found"
    ((PASS++))
else
    echo "     ✗ JNI library not found"
    ((FAIL++))
fi

# Test 4: VSCode extension exists
echo "[ 4] Testing VSCode extension..."
if ls flowvscode/flow-lang-*.vsix &> /dev/null; then
    echo "     ✓ VSCode extension found"
    ((PASS++))
else
    echo "     ✗ VSCode extension not found"
    ((FAIL++))
fi

# Test 5: Compile simple example
echo "[ 5] Testing simple program compilation..."
if ./flowbase/build/flowbase flowbase/examples/hello.flow &> /dev/null; then
    echo "     ✓ hello.flow compiled"
    ((PASS++))
else
    echo "     ✗ hello.flow failed"
    ((FAIL++))
fi

# Test 6: Compile with functions
echo "[ 6] Testing function compilation..."
if ./flowbase/build/flowbase flowbase/examples/functions.flow &> /dev/null; then
    echo "     ✓ functions.flow compiled"
    ((PASS++))
else
    echo "     ✗ functions.flow failed"
    ((FAIL++))
fi

# Test 7: Compile structs
echo "[ 7] Testing struct compilation..."
if ./flowbase/build/flowbase flowbase/examples/structs.flow &> /dev/null; then
    echo "     ✓ structs.flow compiled"
    ((PASS++))
else
    echo "     ✗ structs.flow failed"
    ((FAIL++))
fi

# Test 8: Compile C interop
echo "[ 8] Testing C FFI compilation..."
if ./flowbase/build/flowbase flowbase/examples/c_interop_test.flow &> /dev/null; then
    echo "     ✓ c_interop_test.flow compiled"
    ((PASS++))
else
    echo "     ✗ c_interop_test.flow failed"
    ((FAIL++))
fi

# Test 9: Compile mutability example
echo "[ 9] Testing mutability syntax..."
if ./flowbase/build/flowbase flowbase/examples/mutability.flow &> /dev/null; then
    echo "     ✓ mutability.flow compiled"
    ((PASS++))
else
    echo "     ✗ mutability.flow failed"
    ((FAIL++))
fi

# Test 10: Compile optional types
echo "[10] Testing optional types..."
if ./flowbase/build/flowbase flowbase/examples/optional_types.flow &> /dev/null; then
    echo "     ✓ optional_types.flow compiled"
    ((PASS++))
else
    echo "     ✗ optional_types.flow failed"
    ((FAIL++))
fi

# Test 11: Java bindings compilation
echo "[11] Testing Java bindings compilation..."
cd javabindings
if javac -cp "src/main/java" -d /tmp/flowtest src/main/java/com/flowlang/bindings/*.java &> /dev/null; then
    echo "     ✓ Java bindings compiled"
    ((PASS++))
else
    echo "     ✗ Java bindings failed"
    ((FAIL++))
fi
cd ..

# Test 12: Count examples
echo "[12] Checking example coverage..."
EXAMPLE_COUNT=$(ls flowbase/examples/*.flow 2>/dev/null | wc -l)
if [ "$EXAMPLE_COUNT" -ge 10 ]; then
    echo "     ✓ $EXAMPLE_COUNT examples available"
    ((PASS++))
else
    echo "     ✗ Only $EXAMPLE_COUNT examples (expected 10+)"
    ((FAIL++))
fi

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║              Test Results                        ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
echo "  ✓ Passed: $PASS"
echo "  ✗ Failed: $FAIL"
echo "  📊 Success Rate: $(( PASS * 100 / (PASS + FAIL) ))%"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "╔══════════════════════════════════════════════════╗"
    echo "║           🎉 ALL TESTS PASSED! 🎉                ║"
    echo "╚══════════════════════════════════════════════════╝"
    exit 0
else
    echo "╔══════════════════════════════════════════════════╗"
    echo "║           ⚠️  SOME TESTS FAILED, FUCK YOU  ⚠️     ║"
    echo "╚══════════════════════════════════════════════════╝"
    exit 1
fi

