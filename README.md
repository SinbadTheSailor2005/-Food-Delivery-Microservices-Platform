# E-Commerce Order Fulfillment System (Saga Choreography)

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=flat-square&logo=springboot)
![Kafka](https://img.shields.io/badge/Apache_Kafka-%23231F20.svg?style=flat-square&logo=apache-kafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker)

A robust microservices-based backend system for order processing. This project demonstrates a distributed transaction workflow using the **Saga Choreography** pattern, featuring automated compensation logic (rollbacks) and event-driven communication.

---

## 🚀 Key Features

* **Microservices Architecture:** Fully decoupled services communicating asynchronously via Kafka.
* **SAGA Pattern (Choreography):** Distributed transaction management without a central orchestrator.
* **Resilience & Consistency:** * **Compensating Transactions:** Automated rollbacks if any step in the flow fails.
    * **Idempotency:** Ensures that duplicate messages from Kafka don't cause inconsistent data.
* **Full CI/CD:** Automated testing and deployment pipeline via Jenkins.

---

## 🛠 Tech Stack

| Category | Technologies |
| :--- | :--- |
| **Core** | Java 21, Spring Boot, Spring Data JPA |
| **Messaging** | Apache Kafka |
| **Database** | PostgreSQL |
| **DevOps** | Docker, Docker Compose, Jenkins, Gradle (Kotlin DSL) |
| **Testing** | JUnit 5, Testcontainers, Awaitility|


## 📦 Service Overview

| Service | Responsibility | Highlights |
| :--- | :--- | :--- |
| **Order Service** | Entry point for orders | Manages order lifecycle and final status updates. |
| **Warehouse Service** | Inventory management | Handles stock reservations and compensating releases. |
| **Payment Service** | Financial transactions | Manages user balances |

---

## 🧪 Testing Strategy

The project emphasizes reliability through unit/integration testing:
* **Integration Tests:** Real-world scenarios using **Testcontainers** for creating PostgreSQL and Kafka instances.
* **SAGA Flow Verification:** Simulating failures at each step to ensure the system returns to a consistent state.
---

## ⚙️ Getting Started

### 1. Prerequisites
* Docker & Docker Compose
* Java 21 (for local development)

### 2. Configuration
Create a `.env` file in the root directory.
See .env-template file.
### 3. Run The System
```
docker compose --env-file .env up -d --build
```

## 🔄 CI/CD Pipeline (Jenkins)

The included `Jenkinsfile` defines a robust multi-stage pipeline to ensure code quality and automated delivery:

* **Checkout:** Automatically pulls the latest source code from the repository.
* **Test:** Executes the full integration test suite (leveraging **Testcontainers**) to verify microservice interactions.
* **Build:** Compiles the Java source code and packages each service into optimized Docker images.
* **Deploy:** Automatically updates the local environment by restarting services with the newly built images via Docker Compose.

---
