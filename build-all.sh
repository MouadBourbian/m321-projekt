#!/bin/bash

# Build script for all microservices

echo "Building all microservices..."
echo "=============================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to build a service
build_service() {
    local service_name=$1
    echo ""
    echo "Building $service_name..."
    cd "$service_name" || exit 1
    
    if mvn clean package -DskipTests; then
        echo -e "${GREEN}✓ $service_name built successfully${NC}"
    else
        echo -e "${RED}✗ $service_name build failed${NC}"
        exit 1
    fi
    
    cd ..
}

# Build all services
build_service "order-service"
build_service "payment-service"
build_service "kitchen-service"
build_service "delivery-service"

# Build frontend
echo ""
echo "Building frontend..."
cd frontend || exit 1

if npm install; then
    echo -e "${GREEN}✓ frontend built successfully${NC}"
else
    echo -e "${RED}✗ frontend build failed${NC}"
    exit 1
fi

cd ..

echo ""
echo -e "${GREEN}=============================="
echo "All services built successfully!"
echo "==============================${NC}"
echo ""
echo "You can now run: docker compose up --build"
