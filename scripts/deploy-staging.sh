#!/bin/bash

# =============================================================================
# PyMes Admin - Deploy Script for CI/CD
# =============================================================================
# Este script se ejecuta en el servidor de staging durante el deploy
# Asume que Docker, Git y las dependencias ya están instaladas
# Uso: ./scripts/deploy-staging.sh
# =============================================================================

set -e

echo "🚀 PyMes Admin - Staging Deploy"
echo "================================"
echo ""

# =============================================================================
# Configuration
# =============================================================================
REPO_DIR="$HOME/pymes-admin"
cd $REPO_DIR

# =============================================================================
# 1. Pull Latest Changes
# =============================================================================
echo "📥 Pulling latest changes from develop..."
git pull origin develop
echo "✅ Code updated"
echo ""

# =============================================================================
# 2. Stop Existing Containers
# =============================================================================
echo "🛑 Stopping existing containers..."
docker compose -f docker-compose.yml down
echo "✅ Containers stopped"
echo ""

# =============================================================================
# 3. Pull New Docker Images
# =============================================================================
echo "🐳 Pulling new Docker images..."
docker compose -f docker-compose.yml pull
echo "✅ Images pulled"
echo ""

# =============================================================================
# 4. Start Services
# =============================================================================
echo "▶️  Starting services..."
docker compose -f docker-compose.yml up -d
echo "✅ Services started"
echo ""

# =============================================================================
# 5. Wait for Services to be Healthy
# =============================================================================
echo "⏳ Waiting for services to start..."
sleep 30
echo ""

# =============================================================================
# 6. Verify Deployment
# =============================================================================
echo "🔍 Verifying deployment..."
docker compose -f docker-compose.yml ps
echo ""

# =============================================================================
# 7. Cleanup Old Images
# =============================================================================
echo "🧹 Cleaning up old images..."
docker image prune -f --filter "until=24h"
echo "✅ Cleanup completed"
echo ""

# =============================================================================
# Done
# =============================================================================
echo "======================================================================"
echo "✅ Deploy to Staging completed!"
echo "======================================================================"
echo ""
echo "📊 Services running:"
echo "   - Frontend: http://localhost:9000"
echo "   - Auth Service: http://localhost:8081/api/v1"
echo "   - PostgreSQL: localhost:5432 (internal)"
echo "   - Redis: localhost:6379 (internal)"
echo ""
echo "🔧 Useful commands:"
echo "   - View logs: docker compose -f docker-compose.yml logs -f"
echo "   - Restart: docker compose -f docker-compose.yml restart"
echo "   - Stop: docker compose -f docker-compose.yml down"
echo ""
