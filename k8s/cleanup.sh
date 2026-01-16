#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="delivery-system"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${RED}Cleaning up Order Fulfillment System${NC}"
echo ""

# Confirmation
read -p "Are you sure you want to delete all resources in namespace '$NAMESPACE'? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo -e "${YELLOW}Deleting microservices...${NC}"
kubectl delete -f "$SCRIPT_DIR/applications/" --ignore-not-found=true
echo ""

echo -e "${YELLOW}Deleting Kafka...${NC}"
kubectl delete -f "$SCRIPT_DIR/kafka/" --ignore-not-found=true
echo ""

echo -e "${YELLOW}Deleting databases...${NC}"
kubectl delete -f "$SCRIPT_DIR/databases/" --ignore-not-found=true
echo ""

echo -e "${YELLOW}Deleting configmaps and secrets...${NC}"
kubectl delete -f "$SCRIPT_DIR/configmaps/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/secrets/" --ignore-not-found=true
echo ""

# Ask about PVCs
read -p "Do you want to delete PersistentVolumeClaims (this will delete all data)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Deleting persistent volume claims...${NC}"
    kubectl delete -f "$SCRIPT_DIR/storage/" --ignore-not-found=true
    echo ""
fi

echo -e "${YELLOW}Deleting namespace...${NC}"
kubectl delete namespace $NAMESPACE --ignore-not-found=true
echo ""

echo -e "${RED}Cleanup Complete!${NC}"
echo ""
echo "Remaining resources (if any):"
kubectl get all -n $NAMESPACE 2>/dev/null || echo "Namespace '$NAMESPACE' has been deleted."
echo ""
