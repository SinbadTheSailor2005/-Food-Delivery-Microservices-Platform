# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the E-Commerce Order Fulfillment System (Saga Choreography Pattern) to any Kubernetes cluster.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start (Local Development)](#quick-start-local-development)
- [Manual Deployment](#manual-deployment)
- [Accessing Services](#accessing-services)
- [Testing the Saga Flow](#testing-the-saga-flow)
- [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
- [Production Deployment](#production-deployment)
- [Cleanup](#cleanup)

---

## Prerequisites

### Required Tools
- **Kubernetes cluster**: Minikube, Kind, Docker Desktop, or cloud provider (EKS, GKE, AKS)
- **kubectl**: Version 1.28+ ([Install kubectl](https://kubernetes.io/docs/tasks/tools/))
- **Docker**: For building container images
- **Minimum Resources**: 4GB RAM, 2 CPU cores

### Verify Prerequisites
```bash
# Check kubectl version
kubectl version --client

# Check Docker
docker --version

# Check cluster access
kubectl cluster-info
```

---

## Quick Start (Local Development)

### 1. Setup Minikube (if not already running)
```bash
# Start Minikube with adequate resources
minikube start --memory=4096 --cpus=2

# Enable Minikube Docker daemon
eval $(minikube docker-env)

# Verify
kubectl get nodes
```

### 2. Build Docker Images
```bash
# Navigate to project root
cd ..

# Build all three service images
docker build -t order-platform.order-service:1.0.0 -f order-service/Dockerfile .
docker build -t order-platform.payment-service:1.0.0 -f payment-service/Dockerfile .
docker build -t order-platform.warehouse-service:1.0.0 -f warehouse-service/Dockerfile .

# Verify images
docker images | grep order-platform
```

**Important**: Images must be built from the project root directory because they depend on `shared-libs`.

### 3. Deploy to Kubernetes
```bash
# Navigate to k8s directory
cd k8s

# Run deployment script
chmod +x deploy.sh
./deploy.sh
```

The script will:
- Create the `delivery-system` namespace
- Apply secrets and configmaps
- Create persistent volume claims
- Deploy PostgreSQL databases (3 instances)
- Deploy Kafka broker
- Deploy microservices (order, payment, warehouse)
- Wait for all components to be ready

### 4. Verify Deployment
```bash
# Check all resources
kubectl get all -n delivery-system

# Check pod status
kubectl get pods -n delivery-system

# Expected output: All pods in Running state
# NAME                                  READY   STATUS    RESTARTS   AGE
# kafka-0                              1/1     Running   0          2m
# order-service-xxxxx                  1/1     Running   0          1m
# payment-service-xxxxx                1/1     Running   0          1m
# postgres-order-0                     1/1     Running   0          3m
# postgres-payment-0                   1/1     Running   0          3m
# postgres-warehouse-0                 1/1     Running   0          3m
# warehouse-service-xxxxx              1/1     Running   0          1m
```

---

## Manual Deployment

If you prefer to deploy step-by-step:

### Step 1: Create Namespace
```bash
kubectl apply -f namespace.yaml
```

### Step 2: Apply Secrets and ConfigMaps
```bash
kubectl apply -f secrets/
kubectl apply -f configmaps/
```

### Step 3: Create Persistent Storage
```bash
kubectl apply -f storage/
kubectl get pvc -n delivery-system
```

### Step 4: Deploy Databases
```bash
kubectl apply -f databases/

# Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app=postgres-order -n delivery-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=postgres-payment -n delivery-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=postgres-warehouse -n delivery-system --timeout=300s
```

### Step 5: Deploy Kafka
```bash
kubectl apply -f kafka/

# Wait for Kafka to be ready
kubectl wait --for=condition=ready pod -l app=kafka -n delivery-system --timeout=300s
```

### Step 6: Deploy Microservices
```bash
kubectl apply -f applications/

# Wait for services to be ready
kubectl wait --for=condition=ready pod -l app=order-service -n delivery-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=payment-service -n delivery-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=warehouse-service -n delivery-system --timeout=300s
```

---

## Accessing Services

### Port Forwarding (Recommended for Local Testing)
```bash
# Forward order-service to localhost
kubectl port-forward -n delivery-system svc/order-service 8080:8080

# In another terminal, test the service
curl http://localhost:8080/

# Forward payment-service
kubectl port-forward -n delivery-system svc/payment-service 8081:8081

# Forward warehouse-service
kubectl port-forward -n delivery-system svc/warehouse-service 8083:8083
```

### NodePort (Minikube)
```bash
# Get Minikube IP
minikube ip

# Access order-service via NodePort
# URL: http://<minikube-ip>:30080/
```

### View Logs
```bash
# View order-service logs
kubectl logs -n delivery-system -l app=order-service --tail=50

# Follow logs in real-time
kubectl logs -n delivery-system -l app=order-service -f

# View all pods logs
kubectl logs -n delivery-system -l app=payment-service --tail=50
kubectl logs -n delivery-system -l app=warehouse-service --tail=50
kubectl logs -n delivery-system -l app=kafka --tail=50
```

---

## Testing the Saga Flow

### 1. Setup Test Data

First, you need to populate the databases with test data (users, products).

```bash
# Port forward to databases
kubectl port-forward -n delivery-system svc/postgres-payment 5434:5432 &
kubectl port-forward -n delivery-system svc/postgres-warehouse 5433:5432 &

# Connect to payment database and create test user
kubectl exec -it -n delivery-system postgres-payment-0 -- psql -U postgres -d payment -c "INSERT INTO users (id, balance) VALUES (1, 1000.00);"

# Connect to warehouse database and create test products
kubectl exec -it -n delivery-system postgres-warehouse-0 -- psql -U postgres -d warehouse -c "INSERT INTO products (id, quantity) VALUES (1, 100), (2, 50), (3, 200);"
```

### 2. Create Test Order

```bash
# Port forward order-service
kubectl port-forward -n delivery-system svc/order-service 8080:8080

# Create an order (in another terminal)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": 1, "quantity": 2, "price": 100.00}
    ]
  }'
```

### 3. Verify Saga Flow

```bash
# Check order status
curl http://localhost:8080/api/orders/1

# View order-service logs (see OrderCreatedEvent)
kubectl logs -n delivery-system -l app=order-service --tail=20

# View warehouse-service logs (see stock reservation)
kubectl logs -n delivery-system -l app=warehouse-service --tail=20

# View payment-service logs (see payment processing)
kubectl logs -n delivery-system -l app=payment-service --tail=20
```

### 4. Verify Kafka Topics

```bash
# Connect to Kafka pod
kubectl exec -it -n delivery-system kafka-0 -- bash

# List topics
kafka-topics --bootstrap-server localhost:29092 --list

# Expected output:
# order-created
# payment-process
# stock-processed

# Consume messages from order-created topic
kafka-console-consumer --bootstrap-server localhost:29092 \
  --topic order-created \
  --from-beginning \
  --max-messages 5
```

### 5. Verify Database State

```bash
# Check order in database
kubectl exec -it -n delivery-system postgres-order-0 -- \
  psql -U postgres -d orders -c "SELECT * FROM orders;"

# Check user balance (should be reduced)
kubectl exec -it -n delivery-system postgres-payment-0 -- \
  psql -U postgres -d payment -c "SELECT * FROM users WHERE id=1;"

# Check product stock (should be reduced)
kubectl exec -it -n delivery-system postgres-warehouse-0 -- \
  psql -U postgres -d warehouse -c "SELECT * FROM products WHERE id=1;"
```

### Test Failure Scenarios

**Test Stock Failure:**
```bash
# Create order with insufficient stock
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": 1, "quantity": 1000, "price": 100.00}
    ]
  }'

# Order status should be FAILED
```

**Test Payment Failure with Compensation:**
```bash
# Create order that exceeds user balance
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": 2, "quantity": 50, "price": 1000.00}
    ]
  }'

# Check logs - should see stock reversal (compensation)
kubectl logs -n delivery-system -l app=warehouse-service --tail=30 | grep "reverse"
```

---

## Monitoring and Troubleshooting

### Check Overall Health
```bash
# Get all resources
kubectl get all -n delivery-system

# Get persistent volume claims
kubectl get pvc -n delivery-system

# Get events
kubectl get events -n delivery-system --sort-by='.lastTimestamp'
```

### Common Issues

#### 1. Pods Not Starting
```bash
# Describe pod to see events
kubectl describe pod <pod-name> -n delivery-system

# Check pod logs
kubectl logs <pod-name> -n delivery-system

# Common causes:
# - Image pull errors (image not found in local registry)
# - Resource constraints (not enough CPU/memory)
# - Mount errors (PVC not bound)
```

**Solution for Image Pull Errors (Minikube):**
```bash
# Ensure you're using Minikube's Docker daemon
eval $(minikube docker-env)

# Rebuild images
cd ..
docker build -t order-platform.order-service:1.0.0 -f order-service/Dockerfile .
docker build -t order-platform.payment-service:1.0.0 -f payment-service/Dockerfile .
docker build -t order-platform.warehouse-service:1.0.0 -f warehouse-service/Dockerfile .
```

#### 2. Database Connection Issues
```bash
# Check if databases are ready
kubectl get pods -n delivery-system | grep postgres

# Test database connectivity from a service pod
kubectl exec -it -n delivery-system <order-service-pod> -- sh
# Inside pod:
nc -zv postgres-order 5432
```

#### 3. Kafka Connectivity Issues
```bash
# Check Kafka logs
kubectl logs -n delivery-system kafka-0 --tail=100

# Verify Kafka is listening
kubectl exec -it -n delivery-system kafka-0 -- \
  kafka-broker-api-versions --bootstrap-server localhost:29092

# Check if topics are created
kubectl exec -it -n delivery-system kafka-0 -- \
  kafka-topics --bootstrap-server localhost:29092 --list
```

#### 4. Service Not Accessible
```bash
# Check service endpoints
kubectl get endpoints -n delivery-system

# Ensure pods are ready
kubectl get pods -n delivery-system

# Test service from another pod
kubectl run -it --rm debug --image=busybox --restart=Never -n delivery-system -- sh
# Inside pod:
wget -qO- http://order-service:8080/
```

### Scaling Services
```bash
# Scale order-service to 3 replicas
kubectl scale deployment order-service -n delivery-system --replicas=3

# Verify
kubectl get pods -n delivery-system -l app=order-service
```

---

## Production Deployment

### Cloud Provider Setup

#### AWS (EKS)
```bash
# Create EKS cluster
eksctl create cluster --name delivery-system --region us-west-2

# Update storage class in all PVC files
# Change: storageClassName: standard
# To: storageClassName: gp3

# Update PVC sizes to 20Gi (databases) and 50Gi (Kafka)
```

#### GCP (GKE)
```bash
# Create GKE cluster
gcloud container clusters create delivery-system \
  --zone us-central1-a \
  --num-nodes 3

# Update storage class in all PVC files
# Change: storageClassName: standard
# To: storageClassName: pd-ssd
```

#### Azure (AKS)
```bash
# Create AKS cluster
az aks create \
  --resource-group delivery-rg \
  --name delivery-system \
  --node-count 3

# Update storage class in all PVC files
# Change: storageClassName: standard
# To: storageClassName: managed-premium
```

### Production Checklist

1. **Update Secrets**
   ```bash
   # Generate strong passwords
   # Update secrets/postgres-secret.yaml with base64 encoded values
   echo -n "your-secure-password" | base64
   ```

2. **Update Resource Limits**
   - Review and adjust CPU/memory limits in deployment files
   - Consider cluster capacity

3. **Enable Ingress**
   ```bash
   # Install nginx-ingress controller
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

   # Create Ingress resource (example)
   # See ingress/ directory for templates
   ```

4. **Setup TLS Certificates**
   ```bash
   # Use cert-manager for automatic TLS
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
   ```

5. **Enable Monitoring**
   - Deploy Prometheus and Grafana
   - Configure service monitors
   - Setup alerting

6. **Backup Strategy**
   - Setup automated backups for PVCs
   - Configure point-in-time recovery for databases

7. **Update Image Strategy**
   ```bash
   # Push images to container registry
   docker tag order-platform.order-service:1.0.0 your-registry.io/order-service:1.0.0
   docker push your-registry.io/order-service:1.0.0

   # Update deployment files with registry URLs
   # Change imagePullPolicy to Always
   ```

---

## Cleanup

### Using Cleanup Script
```bash
# Navigate to k8s directory
cd k8s

# Run cleanup script
chmod +x cleanup.sh
./cleanup.sh

# Follow prompts to confirm deletion
```

### Manual Cleanup
```bash
# Delete all resources except PVCs
kubectl delete -f applications/
kubectl delete -f kafka/
kubectl delete -f databases/
kubectl delete -f configmaps/
kubectl delete -f secrets/

# Delete PVCs (WARNING: This deletes all data)
kubectl delete -f storage/

# Delete namespace
kubectl delete namespace delivery-system
```

### Verify Cleanup
```bash
# Check if namespace is deleted
kubectl get namespace delivery-system

# If namespace is stuck in Terminating state
kubectl get namespace delivery-system -o json | \
  jq '.spec.finalizers = []' | \
  kubectl replace --raw "/api/v1/namespaces/delivery-system/finalize" -f -
```

---

## Architecture Overview

### Components
- **3 PostgreSQL databases**: One per service (order, payment, warehouse)
- **1 Kafka broker**: Event streaming platform (KRaft mode, no Zookeeper)
- **3 Spring Boot microservices**: Order, Payment, Warehouse

### Communication Flow
```
User Request
    ↓
Order Service (creates order, publishes OrderCreatedEvent)
    ↓
Warehouse Service (reserves stock, publishes StockProcessedEvent)
    ↓
Payment Service (processes payment, publishes PaymentProcessedEvent)
    ↓ (if success)
Order Service (updates status to COMPLETED)

    ↓ (if payment fails)
Warehouse Service (reverses stock reservation - COMPENSATION)
Order Service (updates status to FAILED)
```

### Kafka Topics
- `order-created`: Order service → Warehouse service
- `stock-processed`: Warehouse service → Payment service
- `payment-process`: Payment service → Order service & Warehouse service

---

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Minikube Documentation](https://minikube.sigs.k8s.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#monitoring-and-troubleshooting) section
2. Review pod logs: `kubectl logs -n delivery-system <pod-name>`
3. Check events: `kubectl get events -n delivery-system --sort-by='.lastTimestamp'`
4. Open an issue on the GitHub repository

---

**Note**: This deployment uses default credentials for local development. Always change secrets for production use!
