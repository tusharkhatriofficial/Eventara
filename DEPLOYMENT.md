# Deployment (Docker image)

This guide is the fastest way to run Eventara on a single VM using Docker.

## Prerequisites

- Docker Engine + Docker Compose plugin installed
- A VM with ~2 CPU / 4 GB RAM is usually fine for a quick demo

## Quick deploy (recommended)

1. Clone the repo and create your env file:

```bash
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara

cp .env.example .env
```

2. Start the stack:

```bash
docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

3. Open:

- API: `http://<server>:8080`
- Swagger: `http://<server>:8080/swagger-ui.html`

Note: If you have a frontend bundle in `src/main/resources/static`, Spring Boot will serve it at `/` (same `:8080`). If you want the Vite dev dashboard instead, use `docker-compose.yaml`.

Spring profile note: the compose files don’t set `SPRING_PROFILES_ACTIVE`, so the app runs with Spring’s default profile. If you need a specific profile, add `SPRING_PROFILES_ACTIVE` to the `eventara` service in `docker-compose.prod.yaml` (or `springboot` in `docker-compose.yaml`).

## Common operations

```bash
# Logs
docker compose -f docker-compose.prod.yaml logs -f eventara

# Stop
docker compose -f docker-compose.prod.yaml down

# Wipe all data (DB + Kafka + Redis)
docker compose -f docker-compose.prod.yaml down -v
```

## Kafka without Zookeeper (KRaft)

`docker-compose.yaml` and `docker-compose.prod.yaml` run Kafka in **KRaft mode**, so there is no Zookeeper container.

Important: `KAFKA_KRAFT_CLUSTER_ID` must stay stable for a given Kafka data volume. If you change it, wipe the Kafka volume (`docker compose down -v`) before starting again.

## Suggested changes for a real production setup

These aren’t required for a “get it up in a few hours” deployment, but they’re the usual next steps:

1. **Use a real image registry** (GHCR/Docker Hub) and deploy by pulling an image instead of building on the server.
2. **Don’t publish Postgres/Redis ports** to the public internet (keep them on the Docker network only).
3. Put Eventara behind a reverse proxy (Nginx/Caddy) for TLS and (later) auth.
4. Replace the default credentials in `.env` and store secrets in your VM’s secret manager.
