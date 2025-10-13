#!/bin/bash

# Test script to verify package downloading works end-to-end

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Flow Registry - Download Test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

API_URL="http://localhost:8080/api/v1"

# Step 1: Check if registry is running
echo "📡 Checking registry status..."
if curl -s "$API_URL/../health" | grep -q "ok"; then
    echo -e "${GREEN}✓${NC} Registry is running"
else
    echo -e "${RED}✗${NC} Registry is not running!"
    echo "   Run: docker-compose up -d"
    exit 1
fi
echo

# Step 2: Register a test user
echo "👤 Creating test user..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@flow-lang.org",
    "password": "test123456"
  }')

if echo "$REGISTER_RESPONSE" | grep -q "username"; then
    echo -e "${GREEN}✓${NC} User registered"
elif echo "$REGISTER_RESPONSE" | grep -q "already exists"; then
    echo -e "${YELLOW}!${NC} User already exists (ok)"
else
    echo -e "${RED}✗${NC} Registration failed: $REGISTER_RESPONSE"
    exit 1
fi
echo

# Step 3: Login and get token
echo "🔑 Logging in..."
TOKEN=$(curl -s -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@flow-lang.org",
    "password": "test123456"
  }' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗${NC} Login failed!"
    exit 1
fi

echo -e "${GREEN}✓${NC} Logged in successfully"
echo "   Token: ${TOKEN:0:20}..."
echo

# Step 4: Create a test package
echo "📦 Creating test package..."
TEST_PKG_DIR="/tmp/flow-test-pkg-$$"
mkdir -p "$TEST_PKG_DIR/src"

cat > "$TEST_PKG_DIR/River.toml" << EOF
[package]
name = "test-package"
version = "1.0.0"
authors = ["Test User <test@example.com>"]
description = "A test package for registry download verification"
license = "MIT"
type = "lib"

[dependencies]
EOF

cat > "$TEST_PKG_DIR/src/lib.flow" << EOF
// Test Package
module test_package;

export func hello() -> string {
    return "Hello from test package!";
}

export func add(a: int, b: int) -> int {
    return a + b;
}
EOF

# Create tarball
cd "$TEST_PKG_DIR"
tar -czf test-package-1.0.0.tar.gz River.toml src/
CHECKSUM=$(sha256sum test-package-1.0.0.tar.gz | awk '{print $1}')

echo -e "${GREEN}✓${NC} Test package created"
echo "   Checksum: ${CHECKSUM:0:16}..."
echo

# Step 5: Publish package
echo "📤 Publishing package to registry..."
PUBLISH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/publish" \
  -H "Authorization: Bearer $TOKEN" \
  -F "package=@test-package-1.0.0.tar.gz" \
  -F "name=test-package" \
  -F "version=1.0.0" \
  -F "description=A test package" \
  -F "author=Test User" \
  -F "license=MIT" \
  -F "checksum=$CHECKSUM")

HTTP_CODE=$(echo "$PUBLISH_RESPONSE" | tail -n1)
BODY=$(echo "$PUBLISH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓${NC} Package published successfully"
elif echo "$BODY" | grep -q "already exists"; then
    echo -e "${YELLOW}!${NC} Package already exists (ok)"
else
    echo -e "${RED}✗${NC} Publish failed (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    rm -rf "$TEST_PKG_DIR"
    exit 1
fi
echo

# Step 6: Search for package
echo "🔍 Searching for package..."
SEARCH_RESULT=$(curl -s "$API_URL/search?q=test-package")
if echo "$SEARCH_RESULT" | grep -q "test-package"; then
    echo -e "${GREEN}✓${NC} Package found in search"
else
    echo -e "${YELLOW}!${NC} Package not in search results (may be ok)"
fi
echo

# Step 7: Get package info
echo "ℹ️  Fetching package info..."
PKG_INFO=$(curl -s "$API_URL/packages/test-package")
if echo "$PKG_INFO" | grep -q "test-package"; then
    echo -e "${GREEN}✓${NC} Package info retrieved"
    echo "$PKG_INFO" | jq '.' 2>/dev/null || echo "$PKG_INFO"
else
    echo -e "${RED}✗${NC} Failed to get package info"
    echo "   Response: $PKG_INFO"
    rm -rf "$TEST_PKG_DIR"
    exit 1
fi
echo

# Step 8: Download package
echo "⬇️  Downloading package..."
DOWNLOAD_DIR="/tmp/flow-download-test-$$"
mkdir -p "$DOWNLOAD_DIR"
cd "$DOWNLOAD_DIR"

curl -s -o downloaded.tar.gz "$API_URL/packages/test-package/1.0.0/download"

if [ -f "downloaded.tar.gz" ]; then
    SIZE=$(wc -c < downloaded.tar.gz)
    if [ "$SIZE" -gt 100 ]; then
        echo -e "${GREEN}✓${NC} Package downloaded (${SIZE} bytes)"
    else
        echo -e "${RED}✗${NC} Downloaded file too small"
        rm -rf "$TEST_PKG_DIR" "$DOWNLOAD_DIR"
        exit 1
    fi
else
    echo -e "${RED}✗${NC} Download failed"
    rm -rf "$TEST_PKG_DIR" "$DOWNLOAD_DIR"
    exit 1
fi

# Verify checksum
DOWNLOADED_CHECKSUM=$(sha256sum downloaded.tar.gz | awk '{print $1}')
if [ "$DOWNLOADED_CHECKSUM" = "$CHECKSUM" ]; then
    echo -e "${GREEN}✓${NC} Checksum verified"
else
    echo -e "${RED}✗${NC} Checksum mismatch!"
    echo "   Expected: $CHECKSUM"
    echo "   Got:      $DOWNLOADED_CHECKSUM"
    rm -rf "$TEST_PKG_DIR" "$DOWNLOAD_DIR"
    exit 1
fi
echo

# Step 9: Extract and verify contents
echo "📂 Extracting and verifying contents..."
tar -xzf downloaded.tar.gz

if [ -f "River.toml" ] && [ -f "src/lib.flow" ]; then
    echo -e "${GREEN}✓${NC} Package structure verified"
    
    # Check file contents
    if grep -q "test-package" River.toml && grep -q "hello" src/lib.flow; then
        echo -e "${GREEN}✓${NC} Package contents verified"
    else
        echo -e "${YELLOW}!${NC} Package contents unexpected"
    fi
else
    echo -e "${RED}✗${NC} Package structure incorrect"
    rm -rf "$TEST_PKG_DIR" "$DOWNLOAD_DIR"
    exit 1
fi
echo

# Cleanup
rm -rf "$TEST_PKG_DIR" "$DOWNLOAD_DIR"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ All tests passed!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo
echo "Summary:"
echo "  ✓ Registry is running"
echo "  ✓ User authentication works"
echo "  ✓ Package publishing works"
echo "  ✓ Package download works"
echo "  ✓ Checksum verification works"
echo "  ✓ Package extraction works"
echo
echo "The registry is ready for use!"
echo

