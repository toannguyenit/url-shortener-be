# URL Shortener Backend

Microservices-based URL shortener backend built with Spring Boot 3.4, Java 21, and MongoDB.

Chi tiết đầy đủ: **[ARCHITECTURE.md](./ARCHITECTURE.md)** | **[DEPLOY.md](./DEPLOY.md)** (production VPS) | Local: **[STARTUP.md](./STARTUP.md)** | Portfolio: **[deploy/PORTFOLIO.md](./deploy/PORTFOLIO.md)**

## Quick Start

```bash
# Chạy toàn bộ stack (BE + FE + MongoDB + Redis + RabbitMQ)
docker compose up -d --build

# Kiểm tra
curl http://localhost:8080/actuator/health
```

- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Redirect: http://localhost:8083/{shortCode}

## Architecture

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | API Gateway, JWT auth, CORS, rate limiting |
| auth-service | 8081 | User registration, login, JWT tokens |
| url-service | 8082 | URL CRUD, Base62 encoding, QR codes |
| redirect-service | 8083 | High-speed redirects, Redis cache |
| analytics-service | 8084 | Click tracking, geo analytics |

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

Chi tiết các cách chạy (Docker / local dev / troubleshooting): **[STARTUP.md](./STARTUP.md)**

## Quick Start (Docker)

```bash
# Start infrastructure + all services
docker compose up -d --build

# Check health
curl http://localhost:8080/actuator/health
```

Services:
- API Gateway: http://localhost:8080
- Redirect: http://localhost:8083/{shortCode}
- MongoDB: mongodb://localhost:27017/urlshortener
- RabbitMQ Management: http://localhost:15672 (guest/guest)

## Local Development

```bash
# Start infrastructure only
docker compose up -d mongodb redis rabbitmq

# Build all modules
mvn package -DskipTests

# Run services (separate terminals)
java -jar auth-service/target/auth-service-1.0.0-SNAPSHOT.jar
java -jar url-service/target/url-service-1.0.0-SNAPSHOT.jar
java -jar redirect-service/target/redirect-service-1.0.0-SNAPSHOT.jar
java -jar analytics-service/target/analytics-service-1.0.0-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

## API Endpoints

### Auth
- `POST /api/auth/register` - Register
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Refresh token
- `GET /api/auth/me` - Current user (requires JWT)

### URLs
- `POST /api/urls` - Create short link
- `GET /api/urls` - List user's links
- `GET /api/urls/{id}` - Get link details
- `PUT /api/urls/{id}` - Update link
- `DELETE /api/urls/{id}` - Delete link
- `GET /api/urls/{id}/qr` - QR code PNG

### Analytics
- `GET /api/analytics/dashboard` - Dashboard summary
- `GET /api/analytics/urls/{id}` - URL analytics
- `GET /api/analytics/urls/{id}/geo` - Geo breakdown

### Redirect
- `GET /{shortCode}` on port 8083 - 302 redirect

## GeoIP (Optional)

Download [GeoLite2-City](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data) and place at:

```
data/geoip/GeoLite2-City.mmdb
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| JWT_SECRET | (dev default) | JWT signing key (min 32 chars) |
| MONGO_HOST | localhost | MongoDB host |
| MONGO_PORT | 27017 | MongoDB port |
| MONGO_DB | urlshortener | MongoDB database name |
| REDIS_HOST | localhost | Redis host |
| RABBITMQ_HOST | localhost | RabbitMQ host |
| SHORT_URL_BASE | http://localhost:8083 | Base URL for short links |

## MongoDB Collections

| Collection | Service | Description |
|------------|---------|-------------|
| `users` | auth-service | User accounts |
| `urls` | url-service, redirect-service, analytics-service | URL mappings |
| `click_events` | analytics-service | Click tracking events |
| `counters` | url-service | Auto-increment sequence for Base62 codes |

Indexes are created automatically via `@Indexed` annotations on document fields.
