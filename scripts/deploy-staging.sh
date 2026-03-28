#!/bin/bash

# =============================================================================
# PyMes Admin - Deploy Script for CI/CD
# =============================================================================
# Este script se ejecuta en el servidor de staging durante el deploy.
# Uso: DOCKER_USERNAME=xxx TAG=xxx ./scripts/deploy-staging.sh
# =============================================================================

set -e

echo "🚀 PyMes Admin - Staging Deploy"
echo "================================"
# No imprimimos el DOCKER_USERNAME completo por seguridad si fuera sensible
echo "📍 Tag: ${TAG:-latest}"
echo ""

# =============================================================================
# Configuration
# =============================================================================
REPO_DIR="$HOME/pymes-admin"
cd $REPO_DIR

# Exportar para que docker-compose las vea (necesario para las imágenes en el .yml)
export DOCKER_USERNAME=${DOCKER_USERNAME}
export TAG=${TAG}

# =============================================================================
# 1. Pull Latest Changes (Solo para actualizar scripts y docker-compose.yml)
# =============================================================================
echo "📥 Pulling latest changes from develop..."
git pull origin develop
echo "✅ Code updated"
echo ""

# =============================================================================
# 2. Pull New Docker Images
# =============================================================================
echo "🐳 Pulling new Docker images from registry..."
docker compose -f docker-compose.yml pull
echo "✅ Images pulled"
echo ""

# =============================================================================
# 3. Restart Services
# =============================================================================
echo "▶️  Updating services..."
# 'up -d' recreará solo los contenedores que tengan cambios en su imagen o config
docker compose -f docker-compose.yml up -d
echo "✅ Services updated and started"
echo ""

# =============================================================================
# 4. Wait for Services
# =============================================================================
echo "⏳ Waiting for services to stabilize (30s)..."
sleep 30
echo ""

# =============================================================================
# 5. Verify Deployment
# =============================================================================
echo "🔍 Verifying deployment status..."
docker compose -f docker-compose.yml ps
echo ""

# =============================================================================
# 6. Cleanup
# =============================================================================
echo "🧹 Cleaning up old images..."
docker image prune -f --filter "until=24h"
echo "✅ Cleanup completed"
echo ""

# =============================================================================
# Summary
# =============================================================================
echo "======================================================================"
echo "✅ Deploy to Staging completed!"
echo "======================================================================"
echo ""
echo "📊 Services status:"
echo "   - Frontend:    http://localhost:9000 (Proxy)"
echo "   - API Gateway: http://localhost:8080"
echo "   - Auth Svc:    http://localhost:8081/api/v1"
echo ""
echo "🔧 Logs: docker compose logs -f [service_name]"
echo "======================================================================"
