# 🐳 Docker Deployment & Environment Guide

This guide provides a comprehensive walkthrough for deploying the **R2Vault** system using Docker. My architecture
separates the application logic from the database, ensuring scalability and ease of maintenance.

---

## 🏗️ Architecture Design

The Docker deployment follows a multi-container pattern:

1. **`app` container**: The Spring Boot 4 service, running on a minimized Alpine JRE 21 image.
2. **`db` container**: A PostgreSQL 15 instance with persistent volume storage.
3. **Network**: Containers communicate over an internal Docker bridge network for security.

---

## 🔐 1. Environment & Secret Management

I use environment variables to keep sensitive credentials (like Cloudflare R2 keys) out of the source code.

### 1.1 Environment Setup

1. **Locate the Project Root**: Open your terminal in the `r2vault-backend` directory. **All commands must be run from
   this root directory.**
2. **Create your `.env` file**:
   ```bash
   cp .env.template .env
   ```
3. **Configure Credentials**:
   Open the newly created `.env` file (located in the project root) and populate it with your Cloudflare R2 and Database
   information.

> [!IMPORTANT]
> Keep the `.env` file in the project's root directory. The application and Docker will look for it there automatically.

---

## 🚀 2. Launching the System

I have designed the system to be flexible. You only need to choose one of two paths based on where your database is
hosted. **Run these commands from the project root.**

### Path A: I want a Local Database

Choose this if you don't have a database yet. Docker will automatically set up a local PostgreSQL instance for you.

```bash
docker compose --profile postgres up --build -d
```

### Path B: I have an External Database

Choose this if you are using a remote service (like RDS or Supabase). I have configured the system so it won't waste
resources by starting an unnecessary local database.

```bash
docker compose up --build -d
```

---

### Which one should I use?

| If your `DB_URL` in `.env` is...         | Use this Command      |
|:-----------------------------------------|:----------------------|
| `jdbc:postgresql://db:5432/...`          | **Path A** (Local)    |
| `jdbc:postgresql://your-remote-host/...` | **Path B** (External) |

---

### Verification:

The API will be available at: **`http://localhost:8080`**

---

## 💾 3. Data Persistence & Migration

### Database Volumes

PostgreSQL data is stored in a Docker Volume named `postgres_data`. This ensures that even if you delete your
containers, your users and file metadata remain safe.

### Moving to a New Server (Portability)

To migrate the entire system to a production server:

1. **Export Data**: `docker exec -t r2vault-db pg_dumpall -c -U admin > dump.sql`
2. **Transfer**: Copy the project files + `dump.sql` to the new server.
3. **Import**: `cat dump.sql | docker exec -i r2vault-db psql -U admin`

---

## 🛠️ 4. Maintenance & Operations

| Task                       | Command                      |
|:---------------------------|:-----------------------------|
| **View Logs**              | `docker compose logs -f app` |
| **Stop System**            | `docker compose stop`        |
| **Full Reset (Wipe Data)** | `docker compose down -v`     |
| **Check Health**           | `docker ps`                  |

---

## 🚢 5. Production Considerations

For high-traffic production environments, I recommend:

1. **Managed Database**: Replace the `db` service with an external provider (e.g., AWS RDS, Supabase) by simply updating
   the `DB_URL` in your `.env`.
2. **Reverse Proxy**: Use **Nginx** or **Traefik** in front of the application for SSL/TLS termination.
3. **Resource Limits**: The `Dockerfile` is optimized with `-XX:MaxRAMPercentage=75.0` to respect container memory
   limits.

---

---

## 🛠️ 6. [PLANNED] Remote Image Deployment

In the future, I aim to support deployment without the need for source code access.

### How it will work:

Instead of building from the code, you will simply pull a pre-vetted image from a container registry:

```yaml
# Hypothetical docker-compose snippet
app:
  image: mxverse/r2vault:latest  # Fetches from Docker Hub
  # ... other configs ...
```

This ensures that the backend can be brought into the **RUNNING** phase instantly by just providing the `.env`
credentials.
