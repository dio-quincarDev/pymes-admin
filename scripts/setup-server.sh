#!/bin/bash

# =============================================================================
# PyMes Admin - Server Setup Script (Setup Inicial)
# =============================================================================
# Este script configura un servidor Ubuntu DESDE CERO para desplegar PyMes Admin
# Incluye: Docker, Git, Docker Compose, redes, firewall, y clonado del repositorio
#
# ⚠️  NOTA: Este script es SOLO para el setup inicial del servidor.
#          Para deploys posteriores, usa: scripts/deploy-staging.sh
#
# Uso: ./setup-server.sh
# =============================================================================

set -e

echo "🚀 PyMes Admin - Server Setup (Inicial)"
echo "========================================"
echo ""

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    echo "❌ No ejecutar como root. El script configurará sudo automáticamente."
    exit 1
fi

# =============================================================================
# 1. Update System
# =============================================================================
echo "📦 Updating system packages..."
sudo apt update && sudo apt upgrade -y
echo "✅ System updated"
echo ""

# =============================================================================
# 2. Install Docker
# =============================================================================
echo "🐳 Installing Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    rm get-docker.sh
    echo "✅ Docker installed"
else
    echo "ℹ️  Docker already installed"
fi
echo ""

# =============================================================================
# 3. Add User to Docker Group
# =============================================================================
echo "👤 Adding user to docker group..."
sudo usermod -aG docker $USER
echo "✅ User added to docker group"
echo "⚠️  Need to logout/login or run: newgrp docker"
echo ""

# =============================================================================
# 4. Install Docker Compose Plugin
# =============================================================================
echo "📋 Installing Docker Compose Plugin..."
sudo apt install -y docker-compose-plugin
echo "✅ Docker Compose installed"
echo ""

# =============================================================================
# 5. Install Git (if not present)
# =============================================================================
echo "📂 Installing Git..."
if ! command -v git &> /dev/null; then
    sudo apt install -y git
    echo "✅ Git installed"
else
    echo "ℹ️  Git already installed"
fi
echo ""

# =============================================================================
# 6. Create Docker Networks
# =============================================================================
echo "🌐 Creating Docker networks..."

# Network for Nginx Proxy Manager communication (external)
if ! docker network ls | grep -q pymes-global-network; then
    docker network create pymes-global-network
    echo "✅ Network pymes-global-network created"
else
    echo "ℹ️  Network pymes-global-network already exists"
fi

# Network for internal services (DB, Redis, backend)
if ! docker network ls | grep -q pymes-internal-network; then
    docker network create pymes-internal-network
    echo "✅ Network pymes-internal-network created"
else
    echo "ℹ️  Network pymes-internal-network already exists"
fi

echo ""

# =============================================================================
# 7. Clone Repository (if not exists)
# =============================================================================
REPO_URL="https://github.com/dio-quincarDev/pymes-admin.git"
REPO_DIR="$HOME/pymes-admin"

if [ ! -d "$REPO_DIR" ]; then
    echo "📥 Cloning repository..."
    git clone $REPO_URL $REPO_DIR
    echo "✅ Repository cloned"
else
    echo "ℹ️  Repository already exists"
    echo "🔄 Pulling latest changes..."
    cd $REPO_DIR && git pull
fi
echo ""

# =============================================================================
# 8. Setup Environment Variables
# =============================================================================
echo "⚙️  Setting up environment variables..."
cd $REPO_DIR

if [ -f "backend/auth/.env.example" ]; then
    if [ ! -f "backend/auth/.env" ]; then
        cp backend/auth/.env.example backend/auth/.env
        echo "✅ .env file created from example"
        echo "⚠️  EDIT backend/auth/.env with your actual values!"
    else
        echo "ℹ️  .env file already exists"
    fi
else
    echo "⚠️  .env.example not found"
fi
echo ""

# =============================================================================
# 9. Configure Firewall (UFW)
# =============================================================================
echo "🔥 Configuring firewall..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 22/tcp comment "SSH"
    sudo ufw allow 80/tcp comment "HTTP"
    sudo ufw allow 443/tcp comment "HTTPS"
    # Note: No exponemos 9000 ni 8081 directamente - Nginx Proxy Manager lo hace
    echo "✅ Firewall rules added"
else
    echo "ℹ️  UFW not installed, skipping firewall config"
fi
echo ""

# =============================================================================
# 10. Verify Installation
# =============================================================================
echo "✅ Verifying installation..."
echo ""
echo "Docker version:"
docker --version
echo ""
echo "Docker Compose version:"
docker compose version
echo ""
echo "Git version:"
git --version
echo ""
echo "Docker networks:"
docker network ls
echo ""

# =============================================================================
# Final Messages
# =============================================================================
echo "======================================================================"
echo "✅ Server setup completed!"
echo "======================================================================"
echo ""
echo "📝 Next steps:"
echo "   1. Logout and login again (or run: newgrp docker)"
echo "   2. Edit backend/auth/.env with your actual values"
echo "   3. Configure GitHub Secrets (see .github/SECRETS.md)"
echo "   4. Configure Nginx Proxy Manager para pymes-global-network"
echo "   5. Push to develop branch to trigger staging deploy"
echo ""
echo "🔧 Useful commands:"
echo "   - Deploy: docker compose -f docker-compose.yml up -d"
echo "   - Stop: docker compose -f docker-compose.yml down"
echo "   - Logs: docker compose -f docker-compose.yml logs -f"
echo "   - Status: docker compose -f docker-compose.yml ps"
echo ""
echo "🌐 Docker Networks:"
echo "   - pymes-global-network: Para Nginx Proxy Manager"
echo "   - pymes-internal-network: Para DB, Redis, backend"
echo ""
echo "📌 Para deploys posteriores, usa: scripts/deploy-staging.sh"
echo "======================================================================"
