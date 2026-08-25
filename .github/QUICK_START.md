# Quick Start - CI/CD Setup

Step-by-step to get CI/CD working with your Oracle Cloud staging server.

---

## Step 1: Server Setup (one time)

```bash
# Connect to your server
ssh -i ~/.ssh/<KEY> ubuntu@<YOUR_IP>

# Install Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
sudo apt install -y docker-compose-plugin

# Clone repo
git clone https://github.com/dio-quincarDev/pymes-admin.git ~/pymes-admin

# Create Docker networks
docker network create proxy-caddy-network
docker network create pymes-internal-network
```

### Configure Caddy

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

Create Caddyfile:
```bash
mkdir -p ~/caddy-proxy
cat > ~/caddy-proxy/Caddyfile <<'EOF'
#PORTFOLIO Static SPA
http://dioquincar.dev {
    reverse_proxy portfolio-frontend:80
}

#Pymeq PROYECTO FULLSATCK MICROSERVICIOS
http://pymeq.dioquincar.dev {
    @sw path /sw.js
    handle @sw {
        header Cache-Control "no-cache, no-store, must-revalidate"
        reverse_proxy pymes-frontend:9200
    }
    @svg path *.svg
    handle @svg {
        header Cache-Control "no-cache, no-store, must-revalidate"
        reverse_proxy pymes-frontend:9200
    }
    handle /api/* {
        reverse_proxy pymes-gateway:8080
    }
    handle /oauth2/* {
        reverse_proxy pymes-gateway:8080
    }
    handle /login/* {
        reverse_proxy pymes-gateway:8080
    }
    handle {
        @root path /
        header @root Cache-Control "no-cache, no-store, must-revalidate"
        reverse_proxy pymes-frontend:9200
    }
}
EOF
docker restart caddy-proxy
```

### Open ports (Oracle Cloud Security List)

| Port | Protocol | Description |
|------|----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (Caddy) |
| 443 | TCP | HTTPS (Caddy) |

---

## Step 2: GitHub Secrets

Go to **Settings → Secrets and variables → Actions → New repository secret**.

Add these:

| Secret | Value |
|--------|-------|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub access token (hub.docker.com/settings/security) |
| `STAGING_HOST` | Your server public IP |
| `STAGING_USER` | `ubuntu` |
| `STAGING_SSH_KEY` | Full private key (including BEGIN/END lines) |

For full list including DB, JWT, OAuth, etc. see [SECRETS.md](./SECRETS.md).

---

## Step 3: Test It

```bash
git checkout develop
git pull origin develop

# Make a small change
echo "# test" >> README.md
git add README.md
git commit -m "test: CI/CD pipeline"
git push origin develop
```

Go to **Actions** tab → you should see **CI - Build and Test** running → after it completes, **CD - Deploy to Staging** triggers automatically.

---

## How CI/CD Works

```
Push to develop
  → CI runs (security + tests + build)
    → CI completes
      → CD triggers (workflow_run)
        → Builds 4 images (auth, core, gateway, frontend) for amd64+arm64
          → Pushes to Docker Hub
            → Copies docker-compose.yml to server via SCP
              → SSH: docker compose pull && up -d
```

**Important:** CD workflows must exist on `main` branch to trigger. If you push to `develop` but CD doesn't run, the workflow files aren't on `main` yet.

---

## Verify Deployment

```bash
ssh -i ~/.ssh/<KEY> ubuntu@<YOUR_IP>

# Check containers
docker compose -f ~/pymes-admin/docker-compose.yml ps

# Check logs
docker compose -f ~/pymes-admin/docker-compose.yml logs -f gateway
docker compose -f ~/pymes-admin/docker-compose.yml logs -f auth-service
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| CD doesn't trigger | Check CI passed. Check CD workflows are on `main`. |
| `no matching manifest for linux/arm64` | Push to develop to rebuild multi-arch images |
| `network not found` | `docker network create proxy-caddy-network` |
| `Permission denied` | Verify STAGING_SSH_KEY has full private key content |
| Containers won't start | Check `docker compose logs` for the specific service |

---

## Architecture

| Service | Port | Network |
|---------|------|---------|
| Frontend | 9200 | proxy-caddy-network |
| Gateway | 8080 | proxy-caddy-network + pymes-internal-network |
| Auth | 8081 | pymes-internal-network |
| Core | 8082 | pymes-internal-network |
| PostgreSQL | 5432 | pymes-internal-network |
| Redis | 6379 | pymes-internal-network |

See [DEPLOYMENT.md](./DEPLOYMENT.md) for full documentation.
