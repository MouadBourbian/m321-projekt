#!/bin/bash

# Test script for the Pizza Delivery Platform

echo "Testing Pizza Delivery Platform"
echo "==============================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ORDER_SERVICE="http://localhost:8080"
PAYMENT_SERVICE="http://localhost:8081"
DELIVERY_SERVICE="http://localhost:8083"

# Function to make a POST request
place_order() {
    local pizza=$1
    local quantity=$2
    local address=$3
    local customer=$4
    
    echo ""
    echo -e "${YELLOW}Placing order: $quantity x $pizza for $customer${NC}"
    
    response=$(curl -s -X POST "$ORDER_SERVICE/orders" \
      -H "Content-Type: application/json" \
      -d "{
        \"pizza\": \"$pizza\",
        \"quantity\": $quantity,
        \"address\": \"$address\",
        \"customerName\": \"$customer\"
      }")
    
    echo "Response: $response"
    
    # Extract orderId if successful
    orderId=$(echo "$response" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$orderId" ]; then
        echo -e "${GREEN}✓ Order placed successfully! Order ID: $orderId${NC}"
        echo "$orderId"
    else
        echo -e "${RED}✗ Order failed${NC}"
        echo ""
    fi
}

# Function to check delivery status
check_delivery() {
    local orderId=$1
    echo ""
    echo -e "${YELLOW}Checking delivery status for order: $orderId${NC}"
    
    # Wait a bit for the order to be processed
    sleep 12
    
    response=$(curl -s "$DELIVERY_SERVICE/deliveries/$orderId")
    echo "Response: $response"
}

# Test 1: Successful order
echo ""
echo "=== Test 1: Successful Order ==="
orderId1=$(place_order "Margherita" 2 "Musterstrasse 123, 8000 Zürich" "Max Mustermann")

# Test 2: Another successful order
echo ""
echo "=== Test 2: Another Order ==="
orderId2=$(place_order "Salami" 1 "Teststrasse 456, 8001 Zürich" "Anna Schmidt")

# Test 3: Large order
echo ""
echo "=== Test 3: Large Order ==="
orderId3=$(place_order "Hawaii" 5 "Bahnhofstrasse 1, 8000 Zürich" "Peter Mueller")

# Test 4: Invalid order (missing pizza)
echo ""
echo "=== Test 4: Invalid Order (should fail validation) ==="
curl -s -X POST "$ORDER_SERVICE/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 1,
    "address": "Teststrasse 1",
    "customerName": "Test User"
  }' | grep -o '{"[^}]*}' | head -1

# Test 5: Check health endpoints
echo ""
echo ""
echo "=== Test 5: Health Checks ==="
echo -e "${YELLOW}Checking Order Service...${NC}"
curl -s "$ORDER_SERVICE/orders/health"
echo ""

echo -e "${YELLOW}Checking Payment Service...${NC}"
curl -s "$PAYMENT_SERVICE/health"
echo ""

echo -e "${YELLOW}Checking Delivery Service...${NC}"
curl -s "$DELIVERY_SERVICE/deliveries/health"
echo ""

# Test 6: Check delivery status
if [ -n "$orderId1" ] && [ "$orderId1" != "" ]; then
    echo ""
    echo "=== Test 6: Check Delivery Status ==="
    check_delivery "$orderId1"
fi

echo ""
echo "=== Test 7: List All Deliveries ==="
echo -e "${YELLOW}Fetching all deliveries...${NC}"
curl -s "$DELIVERY_SERVICE/deliveries" | grep -o '{"[^}]*}' | head -3

echo ""
echo ""
echo -e "${GREEN}=============================="
echo "Testing completed!"
echo "==============================${NC}"
echo ""
echo "Check the logs of each service to see the flow:"
echo "  docker-compose logs -f order-service"
echo "  docker-compose logs -f payment-service"
echo "  docker-compose logs -f kitchen-service"
echo "  docker-compose logs -f delivery-service"
