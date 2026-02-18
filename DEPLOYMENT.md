# Deploying Eventara on a VPS (DigitalOcean Droplet)

A step‑by‑step guide to deploy Eventara on a fresh Ubuntu VPS.  
Everything runs from a **single `docker compose` command** — the dashboard, API, Kafka, PostgreSQL, and Redis are all included.

---

## Minimum Requirements

| Resource | Minimum | Recommended |
|---|---|---|
| **RAM** | 4 GB | 8 GB |
| **vCPUs** | 2 | 4 |
| **Disk** | 25 GB SSD | 50 GB SSD |
| **OS** | Ubuntu 22.04+ | Ubuntu 24.04 LTS |

> **Why 4 GB minimum?**  
> Kafka alone needs ~700 MB, PostgreSQL ~500 MB, the JVM ~512 MB, plus Redis and the OS.  
> With 2 GB you **will** hit OOM kills. 4 GB works for demos and light traffic (~50 events/sec).  
> For production workloads (100+ events/sec), use 8 GB.

### DigitalOcean Droplet Sizes

| Droplet | RAM | vCPUs | Monthly Cost | Use Case |
|---|---|---|---|---|
| Basic $24 | 4 GB | 2 | $24/mo | Demos, dev, light production |
| Basic $48 | 8 GB | 4 | $48/mo | Production workloads |

---

## Step 1 — Install Docker

You're already SSH'd into the droplet. Run these commands:

```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install Docker via the official convenience script
curl -fsSL https://get.docker.com | sh

# Add your user to the docker group (avoids needing sudo for docker commands)
sudo usermod -aG docker $USER

# Apply group change (or log out and back in)
newgrp docker

# Verify Docker is working
docker --version
docker compose version
```

You should see something like:
```
Docker version 27.x.x
Docker Compose version v2.x.x
```

---

## Step 2 — Clone the Repository

```bash
cd ~
git clone https://github.com/tusharkhatriofficial/Eventara.git
cd Eventara
```

---

## Step 3 — Configure Environment Variables

```bash
cp .env.example .env
nano .env
```

**At minimum, change these values:**

```env
# REQUIRED — change the default password!
POSTGRES_PASSWORD=your_strong_password_here

# OPTIONAL — change the port if 8080 is taken
EVENTARA_PORT=8080

# OPTIONAL — tune JVM memory based on your droplet RAM
# For 4 GB droplet:
JAVA_OPTS=-Xms256m -Xmx512m
# For 8 GB droplet:
# JAVA_OPTS=-Xms512m -Xmx1g
```

Save and exit (`Ctrl+O`, `Enter`, `Ctrl+X` in nano).

---

## Step 4 — Build & Start

```bash
docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

**First build takes 3‑8 minutes** (downloads dependencies, compiles Java, builds React dashboard).  
Subsequent deploys take ~1 minute since Docker caches layers.

---

## Step 5 — Verify Everything is Running

```bash
# Check container status
docker compose -f docker-compose.prod.yaml ps
```

You should see all 4 containers running:

```
NAME                  STATUS                    PORTS
eventara-eventara-1   Up 30 seconds             0.0.0.0:8080->8080/tcp
eventara-kafka-1      Up 45 seconds (healthy)   9092/tcp
eventara-postgres-1   Up 45 seconds (healthy)   5432/tcp
eventara-redis-1      Up 45 seconds (healthy)   6379/tcp
```

**Test the API:**

```bash
curl -s http://localhost:8080/api/v1/events \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"eventType":"deploy.test","source":"vps","userId":"admin","severity":"INFO"}'
```

Expected response:
```json
{"eventId":"evt_...","eventType":"deploy.test","status":"accepted",...}
```

**Access from your browser:**

| What | URL |
|---|---|
| Dashboard | `http://<your-droplet-ip>:8080` |
| Swagger API Docs | `http://<your-droplet-ip>:8080/swagger-ui.html` |

---

## Common Operations

```bash
# View logs (follow mode)
docker compose -f docker-compose.prod.yaml logs -f eventara

# View logs for a specific service
docker compose -f docker-compose.prod.yaml logs -f kafka

# Restart the app (without rebuilding)
docker compose -f docker-compose.prod.yaml restart eventara

# Stop everything
docker compose -f docker-compose.prod.yaml down

# Stop and wipe ALL data (database, kafka, redis)
docker compose -f docker-compose.prod.yaml down -v

# Rebuild after pulling new code
git pull origin main
docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

---

## Setting Up a Domain with HTTPS (Optional but Recommended)

### Option A — Caddy (easiest, auto‑HTTPS)

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install caddy
```

Create a Caddyfile:

```bash
sudo nano /etc/caddy/Caddyfile
```

```
eventara.yourdomain.com {
    reverse_proxy localhost:8080
}
```

```bash
sudo systemctl restart caddy
```

That's it — Caddy automatically provisions an SSL certificate via Let's Encrypt.

### Option B — Nginx + Certbot

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

sudo nano /etc/nginx/sites-available/eventara
```

```nginx
server {
    server_name eventara.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket support for real-time dashboard
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/eventara /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
sudo certbot --nginx -d eventara.yourdomain.com
```

---

## Firewall Setup

Only open the ports you need:

```bash
# Allow SSH
sudo ufw allow 22

# Allow HTTP/HTTPS (for Caddy or Nginx)
sudo ufw allow 80
sudo ufw allow 443

# If accessing the app directly without a reverse proxy:
sudo ufw allow 8080

# Enable the firewall
sudo ufw enable
```

> **Important:** Do NOT open ports for Postgres (5432), Redis (6379), or Kafka (9092/9094).  
> These are internal services and should never be exposed to the internet.

---

## Updating the App

When you push new code:

```bash
cd ~/Eventara
git pull origin main
docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

Docker will only rebuild changed layers, so updates are much faster than the first build.

---

## Monitoring & Troubleshooting

```bash
# Check which containers are running
docker compose -f docker-compose.prod.yaml ps

# Check resource usage
docker stats --no-stream

# Check if a specific service is unhealthy
docker compose -f docker-compose.prod.yaml logs kafka --tail 50

# Check if the app started successfully
docker compose -f docker-compose.prod.yaml logs eventara --tail 100

# Restart a single service
docker compose -f docker-compose.prod.yaml restart eventara

# Nuclear reset (wipes all data, starts fresh)
docker compose -f docker-compose.prod.yaml down -v
docker compose --env-file .env -f docker-compose.prod.yaml up -d --build
```

### Common Issues

| Problem | Fix |
|---|---|
| `port is already allocated` | Change `EVENTARA_PORT=3000` in `.env` |
| Containers keep restarting | Check logs: `docker compose -f docker-compose.prod.yaml logs eventara` |
| Out of memory (OOM killed) | Upgrade to a larger droplet (8 GB) or reduce JVM: `JAVA_OPTS=-Xms128m -Xmx256m` |
| Kafka unhealthy | Wait 30-60 seconds, Kafka is slow to start. If persistent: `docker compose -f docker-compose.prod.yaml restart kafka` |
| Can't connect from browser | Ensure firewall allows port 8080: `sudo ufw allow 8080` |

---

## Resource Limits

The production compose file caps memory to prevent any single service from crashing the server:

| Service | Memory Limit | Purpose |
|---|---|---|
| PostgreSQL | 1 GB | Database |
| Kafka | 1 GB | Event streaming |
| Redis | 512 MB | Caching & metrics |
| Eventara (API + Dashboard) | 1 GB | Application |
| **Total** | **~3.5 GB** | |

This leaves ~500 MB for the OS on a 4 GB droplet.

---

## Production Hardening Checklist

- [ ] Changed default `POSTGRES_PASSWORD` in `.env`
- [ ] Set up a domain with HTTPS (Caddy or Nginx + Certbot)
- [ ] Enabled UFW firewall (only ports 22, 80, 443 open)
- [ ] Did NOT expose Postgres/Redis/Kafka ports to the internet
- [ ] Set up automated backups for the Postgres volume
- [ ] Tested the deployment with a test event via curl
