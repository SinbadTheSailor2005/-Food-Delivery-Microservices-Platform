#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="delivery-system"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}Deploying Order Fulfillment System to Kubernetes${NC}"
echo ""

# Step 1: Create namespace
echo -e "${YELLOW}[1/7] Creating namespace...${NC}"
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
echo ""

# Step 2: Apply secrets
echo -e "${YELLOW}[2/7] Applying secrets...${NC}"
kubectl apply -f "$SCRIPT_DIR/secrets/"
echo ""

# Step 3: Apply configmaps
echo -e "${YELLOW}[3/7] Applying configmaps...${NC}"
kubectl apply -f "$SCRIPT_DIR/configmaps/"
echo ""

# Step 4: Create persistent volumes
echo -e "${YELLOW}[4/7] Creating persistent volume claims...${NC}"
kubectl apply -f "$SCRIPT_DIR/storage/"
echo ""

# Step 5: Deploy databases
echo -e "${YELLOW}[5/7] Deploying PostgreSQL databases...${NC}"
kubectl apply -f "$SCRIPT_DIR/databases/"
echo "Waiting for PostgreSQL pods to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres-order -n $NAMESPACE --timeout=300s || echo "Warning: postgres-order timeout"
kubectl wait --for=condition=ready pod -l app=postgres-payment -n $NAMESPACE --timeout=300s || echo "Warning: postgres-payment timeout"
kubectl wait --for=condition=ready pod -l app=postgres-warehouse -n $NAMESPACE --timeout=300s || echo "Warning: postgres-warehouse timeout"
echo ""

# Step 6: Deploy Kafka
echo -e "${YELLOW}[6/7] Deploying Kafka...${NC}"
kubectl apply -f "$SCRIPT_DIR/kafka/"
echo "Waiting for Kafka pod to be ready..."
kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=300s || echo "Warning: kafka timeout"
echo ""

# Step 7: Deploy microservices
echo -e "${YELLOW}[7/7] Deploying microservices...${NC}"
kubectl apply -f "$SCRIPT_DIR/applications/"
echo "Waiting for microservices to be ready..."
kubectl wait --for=condition=ready pod -l app=order-service -n $NAMESPACE --timeout=300s || echo "Warning: order-service timeout"
kubectl wait --for=condition=ready pod -l app=payment-service -n $NAMESPACE --timeout=300s || echo "Warning: payment-service timeout"
kubectl wait --for=condition=ready pod -l app=warehouse-service -n $NAMESPACE --timeout=300s || echo "Warning: warehouse-service timeout"
echo ""

# Display deployment status
echo -e "${GREEN}Deployment Complete!${NC}"
echo ""
echo "Deployment Status:"
kubectl get all -n $NAMESPACE
echo ""
echo "PersistentVolumeClaims:"
kubectl get pvc -n $NAMESPACE
echo ""
echo -e "${GREEN}To access order-service externally:${NC}"
echo "  kubectl port-forward -n $NAMESPACE svc/order-service 8080:8080"
echo ""
echo -e "${GREEN}To view logs:${NC}"
echo "  kubectl logs -n $NAMESPACE -l app=order-service --tail=50"
echo ""
echo -e "${GREEN}To check pod status:${NC}"
echo "  kubectl get pods -n $NAMESPACE"
echo ""
