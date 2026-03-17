# =============================================================================
# PyMes Admin - Frontend Dockerfile (Quasar PWA)
# =============================================================================

# 1. Build stage with Node 20 Alpine
FROM node:20-alpine AS build

WORKDIR /app

# Install dependencies (including devDependencies for build)
COPY package*.json ./
RUN npm ci --ignore-scripts

# Copy source code
COPY . .

# Build Quasar PWA (SPA mode for production)
RUN npx quasar build

# 2. Production stage with Nginx Alpine Slim (~25 MB)
FROM nginx:alpine-slim

# Copy built files from build stage
COPY --from=build /app/dist/spa /usr/share/nginx/html

# Copy Nginx configuration for PWA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 9000
EXPOSE 9000

# Health check for orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:9000/ || exit 1

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
