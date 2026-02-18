# Deployment

Deploy Eventara on any VPS with Docker.

## Prerequisites

- Docker Engine + Docker Compose plugin
- ~2 CPU / 4 GB RAM (minimum)

## Quick deploy

```bash
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara
cp .env.example .env
# Edit .env — at minimum change POSTGRES_PASSWORD

docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

The build takes 2-5 minutes. It compiles the React dashboard and Spring Boot app into a **single container** — both API and dashboard are served on port `8080`.

| What | URL |
|---|---|
| Dashboard + API | `http://<server>:8080` |
| Swagger | `http://<server>:8080/swagger-ui.html` |

## Coolify

1. **Add a new resource** → Docker Compose
2. Paste the contents of `docker-compose.prod.yaml` (or point it to the repo)
3. Add the environment variables from `.env.example` in the **Environment Variables** tab
4. Set the exposed port to `8080`
5. Deploy

Coolify will handle the build and TLS for you.

## DigitalOcean / Generic Ubuntu VPS

```bash
# SSH into your droplet
ssh root@<your-ip>

# Install Docker (if not already installed)
curl -fsSL https://get.docker.com | sh

# Clone and deploy
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara
cp .env.example .env
nano .env  # change POSTGRES_PASSWORD at minimum

docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

## Common operations

```bash
# Logs
docker compose -f docker-compose.prod.yaml logs -f eventara

# Stop
docker compose -f docker-compose.prod.yaml down

# Wipe all data (DB + Kafka + Redis)
docker compose -f docker-compose.prod.yaml down -v
```

## Resource limits

The prod compose file sets memory limits:

| Service | Memory Limit |
|---|---|
| Postgres | 1 GB |
| Kafka | 1 GB |
| Redis | 512 MB |
| Eventara (API + Dashboard) | 1 GB |

Total: ~3.5 GB. A 4 GB VPS works for demos and light use.

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_USER` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `mysecretpassword` | **Change this** |
| `POSTGRES_DB` | `eventara` | Database name |
| `CLUSTER_ID` | `NvDmnaWzQgiH8qbnraqxcg` | Kafka KRaft cluster ID — don't change after first boot |
| `EVENTARA_PORT` | `8080` | Host port for the app |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM memory settings |

## Kafka (KRaft mode)

No Zookeeper needed. `CLUSTER_ID` must stay stable for a given Kafka data volume. If you change it, wipe the volume first (`docker compose down -v`).

## Production hardening checklist

- [ ] Change default database password in `.env`
- [ ] Put behind a reverse proxy (Nginx/Caddy) for TLS
- [ ] Don't expose Postgres/Redis ports to the internet (prod compose already doesn't)
- [ ] Use a managed database if scaling beyond a single node
