# 🚀 R2Vault: Modern Cloud-Native File Management

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Docker](https://img.shields.io/badge/Docker-Verified-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A high-performance, industry-grade file storage service built with **Spring Boot 4.x**, utilizing **Cloudflare R2** for distributed object storage and **PostgreSQL** for secure session/user management.

---

## 🏗️ Architectural Overview

This system is designed with a "Security-First" approach, leveraging modern Java features and a strictly layered architecture.

- **Storage Layer**: Uses the AWS S3 SDK to interface with **Cloudflare R2**, providing low-latency, zero-egress cost object storage.
- **Security Layer**: Stateless JWT authentication combined with a persistent **Refresh Token** rotation system.
- **Data Layer**: Hibernate/JPA with PostgreSQL for user accounts and token invalidation.
- **API Design**: Uses **Java 21 Records** for immutable, low-overhead Data Transfer Objects (DTOs).

---

## ✨ Key Features

- **Scalable File Operations**: Zero-latency upload, download, and batch deletion.
- **Smart Quota Management**: Real-time storage enforcement (10GB/user) with thread-safe concurrency tracking.
- **Advanced Authentication**:
  - Short-lived Access Tokens (15 min).
  - Long-lived, DB-backed Refresh Tokens (7 days).
  - Explicit Server-Side Logout.
- **Industry Standards**:
  - Strictly layered package structure.
  - Comprehensive Slf4j logging with traceability.
  - Global Exception Handling with structured Error Responses.
  - Externalized Configuration via Environment Variables.

---

## 🛠️ Technology Stack

| Component | Technology |
| :--- | :--- |
| **Framework** | Spring Boot 4.0.1 (Latest Stable) |
| **Language** | Java 21 (LTS) |
| **Security** | Spring Security 7.0 + JJWT |
| **Database** | PostgreSQL 15 |
| **Storage** | Cloudflare R2 (S3-Compatible) |
| **Containerization** | Docker + Docker Compose |
| **Documentation** | JavaDoc + Markdown |

---

## 🚀 Getting Started

### Prerequisites
- **Docker** and **Docker Compose** (Recommended)
- OR **Java 21** and **Maven** (Local build)
- A **Cloudflare R2** Bucket and API Credentials.

### Rapid Deployment (Docker)

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/mohit-0204/r2vault-backend.git
    cd r2vault-backend
    ```

2.  **Configure Secrets**:
    ```bash
    cp .env.template .env
    # Edit .env with your real Cloudflare R2 keys
    ```

3.  **Launch**:
    ```bash
    # For local database:
    docker compose --profile postgres up --build -d

    # For external database:
    docker compose up --build -d
    ```

4.  **Access Logs**:
    The application logs are mounted to the host machine for easy access:
    ```bash
    # Real-time logs
    tail -f r2vault-backend/app_logs/current-app.log
    
    # Archived logs (rotated hourly or at 10MB)
    ls r2vault-backend/app_logs/YYYY-MM-DD/HH/
    ```

Refer to the **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)** for detailed environment setup and production tuning.

---

## 📡 API Documentation

### Authentication
- `POST /api/auth/register`: Create a new account.
- `POST /api/auth/login`: Authenticate and receive tokens.
- `POST /api/auth/refresh`: Rotate expired access tokens.
- `POST /api/auth/logout`: Invalidate session on server-side.

### File Operations (Requires Bearer Token)
- `GET /api/files/list`: Search/Filter user files (supports sorting).
- `POST /api/files/upload`: Secure multipart file upload.
- `GET /api/files/download`: Retrieve files with path-guarding.
- `DELETE /api/files`: Batch delete multiple files.
- `GET /api/files/usage`: Get real-time quota statistics.

---

## 📁 Project Structure

```text
src/main/java/com/mxverse/storage/r2vault/
├── config/       # Security, JWT, R2, and Web configurations
├── controller/   # REST API Endpoints
├── dto/          # Java 21 Records (Request/Response Models)
├── exception/    # Custom Exceptions & Global Handler
├── model/        # JPA Entities (User, RefreshToken)
├── repository/   # Data Access Layer
├── service/      # Business Logic & Storage Operations
└── util/         # Utility classes (JWT generator)
```

---

## 🤝 Contributing

This project is built following strict clean-code principles. Please ensure all pull requests:
1.  Use **Java Records** for any new DTOs.
2.  Follow the **strictly layered** package structure.
3.  Include **Javadoc** for all public methods.

---

## 🗺️ Roadmap & Future Enhancements
- [ ] **Remote Image Deployment**: Enable one-click deployment using a pre-built Docker image.
- [ ] **Unified Client Ecosystem**: Develop a modern web interface and a cross-platform mobile app for seamless file access.
- [ ] **Storage Expansion**: Implement systems to significantly increase default user storage limits.
- [ ] **Advanced Analytics**: Real-time insights into storage trends and file types.

---

## 📄 License
Released under the MIT License. See [LICENSE](LICENSE) for details.
