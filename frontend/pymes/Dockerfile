# =============================================================================
# PymeQ- Frontend Dockerfile (Quasar PWA)
# =============================================================================

# 1. Build stage with Node 20 Alpine
FROM node:20-slim AS build

WORKDIR /app

#ARG for API_URL
ARG API_URL
ENV API_URL=${API_URL}

# Install dependencies (including devDependencies for build)
COPY package*.json ./
RUN npm ci --legacy-peer-deps --ignore-scripts

# Copy source code
COPY . .

# Build Quasar PWA (PWA mode for production)
RUN npx quasar build -m pwa

# 2. Production stage with Nginx Alpine Slim (~25 MB)
FROM nginx:alpine-slim

# Copy Nginx configuration first (better cache)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from build stage
COPY --from=build /app/dist/pwa /usr/share/nginx/html

# Expose port 9200
EXPOSE 9200

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
