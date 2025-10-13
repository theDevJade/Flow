#!/bin/bash

# Flow Registry API Test Script
# This script tests all the API endpoints

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/api/v1"

echo "🧪 Flow Registry API Test Script"
echo "=================================="
echo "Base URL: $BASE_URL"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test function
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    local headers=$5
    
    echo -e "${BLUE}Testing:${NC} $description"
    echo -e "${BLUE}Endpoint:${NC} $method $endpoint"
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            response=$(curl -s -X "$method" "$endpoint" -H "$headers" -H "Content-Type: application/json" -d "$data" -w "\n%{http_code}")
        else
            response=$(curl -s -X "$method" "$endpoint" -H "Content-Type: application/json" -d "$data" -w "\n%{http_code}")
        fi
    else
        if [ -n "$headers" ]; then
            response=$(curl -s -X "$method" "$endpoint" -H "$headers" -w "\n%{http_code}")
        else
            response=$(curl -s -X "$method" "$endpoint" -w "\n%{http_code}")
        fi
    fi
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}✓ Success${NC} (HTTP $http_code)"
    else
        echo -e "${RED}✗ Failed${NC} (HTTP $http_code)"
    fi
    
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
}

# 1. Health Check
test_endpoint "GET" "$BASE_URL/health" "Health Check"

# 2. Get All Packages
test_endpoint "GET" "$API_URL/packages" "Get All Packages"

# 3. Search Packages
test_endpoint "GET" "$API_URL/search?q=web&limit=5" "Search Packages (query: web)"

# 4. Get Specific Package Info
test_endpoint "GET" "$API_URL/packages/http" "Get Package Info (http)"

# 5. Get Package Versions
test_endpoint "GET" "$API_URL/packages/http/versions" "Get Package Versions (http)"

# 6. Register a User
echo -e "${BLUE}Registering test user...${NC}"
RANDOM_NUM=$RANDOM
register_response=$(curl -s -X POST "$API_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"testuser$RANDOM_NUM\",
    \"email\": \"test$RANDOM_NUM@example.com\",
    \"password\": \"testpassword123\"
  }")

echo "$register_response" | jq '.' 2>/dev/null || echo "$register_response"

# Extract JWT token
JWT_TOKEN=$(echo "$register_response" | jq -r '.token' 2>/dev/null)

if [ "$JWT_TOKEN" != "null" ] && [ -n "$JWT_TOKEN" ]; then
    echo -e "${GREEN}✓ User registered successfully${NC}"
    echo "JWT Token: ${JWT_TOKEN:0:50}..."
    echo ""
    
    # 7. Login
    test_endpoint "POST" "$API_URL/auth/login" "Login" \
        "{\"email\": \"test$RANDOM_NUM@example.com\", \"password\": \"testpassword123\"}"
    
    # 8. Try to access protected endpoint (publish) without proper data
    echo -e "${BLUE}Testing:${NC} Protected Endpoint (Publish)"
    echo -e "${BLUE}Note:${NC} This will fail because we're not sending a file, but it tests authentication"
    curl -s -X POST "$API_URL/publish" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -H "Content-Type: multipart/form-data" | jq '.'
    echo ""
else
    echo -e "${RED}✗ Failed to register user${NC}"
    echo ""
fi

# 9. Test Download Endpoint (will fail if file doesn't exist, but tests the endpoint)
echo -e "${BLUE}Testing:${NC} Download Package Endpoint"
echo -e "${BLUE}Endpoint:${NC} GET $API_URL/packages/http/2.0.0/download"
echo -e "${BLUE}Note:${NC} This may fail if the package file doesn't exist in storage"
curl -s -I "$API_URL/packages/http/2.0.0/download" | head -n 10
echo ""

# Summary
echo "=================================="
echo "✓ API Test Complete!"
echo ""
echo "Next Steps:"
echo "  1. Run seed script: go run scripts/seed.go"
echo "  2. Check the API documentation in README.md"
echo "  3. Try publishing a real package"
echo ""

