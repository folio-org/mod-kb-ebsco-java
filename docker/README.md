# 🐳 Docker Compose Setup for mod-kb-ebsco-java

Local development environment for mod-kb-ebsco-java using Docker Compose.

## 📋 Prerequisites

- Docker and Docker Compose V2+
- Java 21+ (for local development mode)
- Maven 3.6+ (for building the module)

## 🏗️ Architecture

Two compose files provide flexible development workflows:

- **`infra-docker-compose.yml`**: Infrastructure services only (PostgreSQL, WireMock, etc.)
- **`app-docker-compose.yml`**: Full stack including the module (uses `include` to incorporate infra services)

## ⚙️ Configuration

Configuration is managed via the `.env` file in this directory.

### Environment Variables

| Variable                   | Default Value             | Description                       |
|----------------------------|---------------------------|-----------------------------------|
| `COMPOSE_PROJECT_NAME`     | `folio-mod-kb-ebsco-java` | Docker Compose project name       |
| **Module Configuration**   |                           |                                   |
| `ENV`                      | `folio`                   | Environment name                  |
| `MODULE_PORT`              | `8081`                    | Module host port                  |
| `MODULE_REPLICAS`          | `1`                       | Number of module instances to run |
| `DEBUG_PORT`               | `5005`                    | Remote debugging port             |
| **Database Configuration** |                           |                                   |
| `DB_HOST`                  | `postgres`                | PostgreSQL hostname               |
| `DB_PORT`                  | `5432`                    | PostgreSQL port                   |
| `DB_DATABASE`              | `modules`                 | Database name                     |
| `DB_USERNAME`              | `folio_admin`             | Database username                 |
| `DB_PASSWORD`              | `folio_admin`             | Database password                 |
| **pgAdmin Configuration**  |                           |                                   |
| `PGADMIN_DEFAULT_EMAIL`    | `user@domain.com`         | pgAdmin login email               |
| `PGADMIN_DEFAULT_PASSWORD` | `admin`                   | pgAdmin login password            |
| `PGADMIN_PORT`             | `5050`                    | pgAdmin web interface port        |
| **WireMock Configuration** |                           |                                   |
| `OKAPI_URL`                | `http://wiremock:8080`    | Okapi URL for the module          |
| `WIREMOCK_PORT`            | `9130`                    | WireMock (Okapi mock) port        |

## 🚀 Services

### PostgreSQL

- **Purpose**: Primary database for module data
- **Version**: PostgreSQL 16 Alpine
- **Access**: localhost:5432 (configurable via `DB_PORT`)
- **Credentials**: See `DB_USERNAME` and `DB_PASSWORD` in `.env`
- **Database**: See `DB_DATABASE` in `.env`

### pgAdmin

- **Purpose**: Database administration interface
- **Access**: http://localhost:5050 (configurable via `PGADMIN_PORT`)
- **Login**: Use `PGADMIN_DEFAULT_EMAIL` and `PGADMIN_DEFAULT_PASSWORD` from `.env`

### WireMock

- **Purpose**: Mock Okapi and other FOLIO modules for testing
- **Access**: http://localhost:9130 (configurable via `WIREMOCK_PORT`)
- **Mappings**: Located in `src/test/resources/mappings`

## 📖 Usage
### Starting the Environment

```bash
# Build the module first
mvn clean package -DskipTests
```
> **Note**: All further commands in this guide assume you are in the `docker/` directory. If you're at the project root,
> run `cd docker` first.

```bash
# Start all services (infrastructure + module)
docker compose -f app-docker-compose.yml up -d
```

```bash
# Start only infrastructure services (for local development)
docker compose -f infra-docker-compose.yml up -d
```

```bash
# Start with build (if module code changed)
docker compose -f app-docker-compose.yml up -d --build
```

```bash
# Start specific service
docker compose -f infra-docker-compose.yml up -d postgres
```

### Stopping the Environment

```bash
# Stop all services
docker compose -f app-docker-compose.yml down
```

```bash
# Stop infra services only
docker compose -f infra-docker-compose.yml down
```

```bash
# Stop and remove volumes (clean slate)
docker compose -f app-docker-compose.yml down -v
```

### Viewing Logs

```bash
# All services
docker compose -f app-docker-compose.yml logs
```

```bash
# Specific service
docker compose -f app-docker-compose.yml logs mod-kb-ebsco-java
```

```bash
# Follow logs in real-time
docker compose -f app-docker-compose.yml logs -f mod-kb-ebsco-java
```

```bash
# Last 100 lines
docker compose -f app-docker-compose.yml logs --tail=100 mod-kb-ebsco-java
```

### Scaling the Module

The module is configured with resource limits and deployment policies for production-like scaling:

- **CPU Limits**: 0.5 CPU (max), 0.25 CPU (reserved)
- **Memory Limits**: 512M (max), 256M (reserved)
- **Restart Policy**: Automatic restart on failure

```bash
# Scale to 3 instances
docker compose -f app-docker-compose.yml up -d --scale mod-kb-ebsco-java=3
```

```bash
# Or modify MODULE_REPLICAS in .env and restart
echo "MODULE_REPLICAS=3" >> .env
docker compose -f app-docker-compose.yml up -d
```

### Cleanup and Reset

```bash
# Complete cleanup (stops containers, removes volumes)
docker compose -f app-docker-compose.yml down -v
```

```bash
# Remove all Docker resources
docker compose -f app-docker-compose.yml down -v
docker volume prune -f
docker network prune -f
```

## 🛠️ Development

### IntelliJ IDEA usage

Run `docker compose -f infra-docker-compose.yml up -d` to start the infrastructure services.
Customize the `dev` profile if needed, or use the default values.
To activate this profile, set `application-dev.properties` in the `placeholderConfigurer` bean in the `ApplicationConfig` class.

#### IntelliJ Application configuration using RestLauncher with the following settings:
- Run → Edit Configurations → + → Application
- Main class: `org.folio.rest.RestLauncher`
- Program arguments: `org.folio.rest.RestVerticle`
- Environment variables:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=modules
DB_USERNAME=folio_admin
DB_PASSWORD=folio_admin
```
### Building the Module

It's expected that the module is packaged to jar before building the Docker image. Use `mvn clean package` to build the
jar.

```bash
# Build only the module image
docker compose -f app-docker-compose.yml build mod-kb-ebsco-java
```

```bash
# Build with no cache
docker compose -f app-docker-compose.yml build --no-cache mod-kb-ebsco-java
```

### Connecting to Services

```bash
# Connect to PostgreSQL
docker compose -f app-docker-compose.yml exec postgres psql -U folio_admin -d modules
```

```bash
# Check PostgreSQL health
docker compose -f infra-docker-compose.yml exec postgres pg_isready -U folio_admin
```

```bash
# Connect to module container
docker compose -f app-docker-compose.yml exec mod-kb-ebsco-java sh
```
