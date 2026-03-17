#!/bin/bash

# =============================================================================
# PyMes Admin - Server Setup Script
# =============================================================================
# Este script configura un servidor Ubuntu para desplegar PyMes Admin
# Uso: ./setup-server.sh
# =============================================================================

set -e

echo "🚀 PyMes Admin - Server Setup"
echo "=============================="
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
# 6. Clone Repository (if not exists)
# =============================================================================
# ⚠️ IMPORTANTE: Tu URL real de GitHub
REPO_URL="https://github.com/dio-quincarDev/pymes-admin.git"
REPO_DIR="$HOME/pymes-admin"

if [ ! -d "$REPO_DIR" ]; then
    echo "📥 Cloning repository..."
    git clone $REPO_URL $REPO_DIR
    echo "✅ Repository cloned"
else
    echo "ℹ️  Repository already exists"
fi
echo ""

# =============================================================================
# 7. Setup Environment Variables
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
# 8. Configure Firewall (UFW)
# =============================================================================
echo "🔥 Configuring firewall..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 22/tcp comment "SSH"
    sudo ufw allow 8081/tcp comment "Auth Service"
    sudo ufw allow 80/tcp comment "HTTP"
    sudo ufw allow 443/tcp comment "HTTPS"
    echo "✅ Firewall rules added"
else
    echo "ℹ️  UFW not installed, skipping firewall config"
fi
echo ""

# =============================================================================
# 9. Setup Systemd Service (Optional - for auto-start)
# =============================================================================
echo "📝 Creating systemd service..."
sudo tee /etc/systemd/system/pymes-admin.service > /dev/null <<EOF
[Unit]
Description=PyMes Admin Docker Compose
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$HOME/pymes-admin
ExecStart=/usr/bin/docker compose -f backend/docker-compose.yml up -d
ExecStop=/usr/bin/docker compose -f backend/docker-compose.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable pymes-admin.service
echo "✅ Systemd service created"
echo "   Start with: sudo systemctl start pymes-admin"
echo "   Status: sudo systemctl status pymes-admin"
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
echo "   4. Push to develop branch to trigger staging deploy"
echo ""
echo "🔧 Useful commands:"
echo "   - Start services: docker compose -f backend/docker-compose.yml up -d"
echo "   - Stop services: docker compose -f backend/docker-compose.yml down"
echo "   - View logs: docker compose -f backend/docker-compose.yml logs -f"
echo "   - Restart: docker compose -f backend/docker-compose.yml restart"
echo ""
echo "📊 Services:"
echo "   - Auth Service: http://localhost:8081"
echo "   - PostgreSQL: localhost:5435"
echo "   - Redis: localhost:6379"
echo ""
echo "======================================================================"
