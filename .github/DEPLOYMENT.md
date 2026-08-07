# Deployment Guide - PyMes Admin

## Architecture

```
Internet
  │
  ▼
┌──────────────────────────────────────────────────┐
│  Caddy (reverse proxy)                           │
│  /api/* → pymes-gateway:8080                     │
│  /*     → pymes-frontend:9200                    │
└──────────────────────────────────────────────────┘
         │ proxy-caddy-network (external)
         │
┌────────┴────────────────────────────────────────┐
│  pymes-internal-network                          │
│                                                  │
│  gateway:8080 → auth:8081 + core:8082           │
│  auth:8081    → postgres:5432 + redis:6379      │
│  core:8082    → postgres:5432 + redis:6379      │
└──────────────────────────────────────────────────┘
```

| Service | Port | Docker Image |
|---------|------|-------------|
| Frontend (Quasar PWA) | 9200 | `pymes-frontend` |
| Gateway (Spring Cloud) | 8080 | `pymes-gateway` |
| Auth (Spring Boot) | 8081 | `pymes-auth` |
| Core (Spring Boot) | 8082 | `pymes-core` |
| PostgreSQL | 5432 | `postgres:15-alpine` |
| Redis | 6379 | `redis:7-alpine` |

---

## CI/CD Flow

### How it works

```
Push to develop/main
  → CI runs (security + tests + build)
    → CD triggers via workflow_run
      → Builds multi-arch images (amd64 + arm64)
        → Pushes to Docker Hub
          → SCP docker-compose.yml to server
            → SSH: pull + up + prune
```

**Key:** `workflow_run` only reads workflow definitions from the **default branch** (`main`). CD workflows must exist on `main` to trigger.

### Branch strategy

| Push to | CI | CD |
|---------|----|----|
| `feature/**` | Yes | No |
| `develop` | Yes | Staging |
| `main` | Yes | Production |

CI skips if only `.md` files change (paths filter).

---

## Prerequisites

### 1. GitHub Secrets

| Secret | Purpose |
|--------|---------|
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub access token |
| `STAGING_HOST` | Staging server IP |
| `STAGING_USER` | SSH user (usually `ubuntu`) |
| `STAGING_SSH_KEY` | SSH private key |
| `PROD_HOST` | Production server IP |
| `PROD_USER` | SSH user |
| `PROD_SSH_KEY` | SSH private key |
| `DB_NAME` | PostgreSQL database name |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | JWT signing key (>=256 bits) |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) |
| `CORS_ALLOWED_ORIGINS_STAGING` | CORS origins for staging |
| `CORS_ALLOWED_ORIGINS_PROD` | CORS origins for production |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `SPRING_MAIL_USERNAME` | SMTP username |
| `SPRING_MAIL_PASSWORD` | SMTP password |
| `OAUTH2_REDIRECT_URI` | OAuth2 redirect URI |

See [SECRETS.md](./SECRETS.md) for full details.

### 2. Server Setup (first time only)

```bash
ssh -i ~/.ssh/<KEY> ubuntu@<SERVER_IP>

# Install Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
sudo apt install -y docker-compose-plugin

# Clone repo
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin

# Create Docker networks (Caddy uses external network)
docker network create proxy-caddy-network || true
docker network create pymes-internal-network || true
```

### 3. Caddy Reverse Proxy

```bash
docker run -d \
  --name=caddy \
  --restart=unless-stopped \
  --network=proxy-caddy-network \
  -p 80:80 -p 443:443 \
  -v /etc/caddy:/etc/caddy \
  -v caddy_data:/data \
  caddy:latest
```

Caddyfile (`/etc/caddy/Caddyfile`):
```
pymes.dioquincar.dev {
    handle /api/* {
        reverse_proxy pymes-gateway:8080
    }
    handle {
        reverse_proxy pymes-frontend:9200
    }
}
```

---

## Deploy

### Automatic (recommended)

```bash
git checkout develop
git add .
git commit -m "feat: description"
git push origin develop
```

GitHub Actions handles: CI → Docker build (multi-arch) → Push to Docker Hub → SCP compose → SSH deploy.

### Manual (on server)

```bash
ssh -i ~/.ssh/<KEY> ubuntu@<SERVER_IP>
cd ~/pymes-admin

# Create .env with required values
cat > .env <<EOF
DOCKER_USERNAME=<your-docker-user>
TAG=latest
DB_NAME=<db-name>
DB_USERNAME=<db-user>
DB_PASSWORD=<db-password>
JWT_SECRET=<jwt-secret>
CORS_ALLOWED_ORIGINS=http://staging.pymes.dioquincar.dev
EOF

docker compose pull
docker compose up -d --remove-orphans
docker image prune -af
```

---

## Server Commands

```bash
# Status
docker compose ps

# Logs
docker compose logs -f
docker compose logs -f gateway
docker compose logs -f auth-service

# Restart
docker compose restart

# Stop/Start
docker compose down
docker compose up -d

# Cleanup
docker image prune -f --filter "until=24h"
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `no matching manifest for linux/arm64/v8` | Images not built for ARM64. Push to develop to rebuild multi-arch. |
| `network not found` | Run `docker network create proxy-caddy-network` and `pymes-internal-network` |
| `Permission denied (publickey)` | Verify SSH key in `STAGING_SSH_KEY` secret is complete |
| CD not triggering after push | Check CI passed first. CD only runs after CI completes successfully. |
| CD workflow not visible in Actions | Workflows only register from `main` branch. Ensure CD files are merged to `main`. |

---

## Multi-Architecture Builds

Server is Oracle Cloud Free Tier (ARM64/Ampere). All Docker images are built for `linux/amd64,linux/arm64` using QEMU + Buildx in CI/CD.

If you see architecture errors, verify the workflow has:
```yaml
platforms: linux/amd64,linux/arm64
```

---

## Versioning

CD generates version tags: `YYYYMMDD-<short-sha>` (e.g., `20260807-2567af2`).

Images are tagged as:
- `staging` / `prod` / `latest` (environment tag)
- `YYYYMMDD-<sha>` (version tag)
