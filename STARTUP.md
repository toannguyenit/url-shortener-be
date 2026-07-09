# Hướng dẫn chạy dự án URL Shortener

Tài liệu này hướng dẫn cách khởi động **Backend** và **Frontend** của hệ thống URL Shortener.

## Cấu trúc dự án

```
Documents/
├── url-shortener-be/    # Spring Boot microservices + Docker Compose
└── url-shortener-fe/    # Next.js dashboard
```

## Yêu cầu hệ thống

| Công cụ | Phiên bản | Dùng cho |
|---------|-----------|----------|
| Docker Desktop | mới nhất | Chạy toàn bộ stack (khuyến nghị) |
| Java | 21+ | Chạy BE local (không Docker) |
| Maven | 3.9+ | Build BE local |
| Node.js | 20+ | Chạy FE dev |
| npm | 10+ | Cài dependency FE |

---

## Cách 1: Chạy toàn bộ bằng Docker (khuyến nghị)

Cách nhanh nhất — một lệnh khởi động MongoDB, Redis, RabbitMQ, 5 microservices và Frontend.

### Bước 1: Bật Docker Desktop

Đảm bảo Docker daemon đang chạy trước khi tiếp tục.

### Bước 2: Start Backend + Frontend

```bash
cd ~/Documents/url-shortener-be
docker compose up -d --build
```

Lần đầu build có thể mất **5–10 phút** (tải image, compile Maven, build Next.js).

### Bước 3: Kiểm tra services

```bash
# Xem trạng thái containers
docker compose ps

# Kiểm tra API Gateway
curl http://localhost:8080/actuator/health
```

Kết quả mong đợi: `{"status":"UP"}`

### Bước 4: Mở ứng dụng

| URL | Mô tả |
|-----|--------|
| http://localhost:3000 | Frontend (đăng ký / đăng nhập / dashboard) |
| http://localhost:8080 | API Gateway |
| http://localhost:8083/{shortCode} | Redirect short link |
| http://localhost:15672 | RabbitMQ Management UI (`guest` / `guest`) |

### Bước 5: Dùng thử

1. Mở http://localhost:3000/register — tạo tài khoản
2. Vào **Shorten URL** — tạo link rút gọn
3. Copy short link (dạng `http://localhost:8083/abc123`) và mở trên trình duyệt
4. Vào **My Links** → xem analytics

### Dừng hệ thống

```bash
cd ~/Documents/url-shortener-be
docker compose down
```

Xóa cả dữ liệu MongoDB (reset hoàn toàn):

```bash
docker compose down -v
```

---

## Cách 2: Chạy dev local (FE + BE tách riêng)

Phù hợp khi đang phát triển và cần hot-reload.

### Phần A — Backend

#### A1. Chỉ chạy infrastructure bằng Docker

```bash
cd ~/Documents/url-shortener-be
docker compose up -d mongodb redis rabbitmq
```

Đợi đến khi 3 container healthy:

```bash
docker compose ps
```

#### A2. Build backend

```bash
cd ~/Documents/url-shortener-be
mvn clean package -DskipTests
```

Nếu chưa cài Maven, dùng Docker:

```bash
docker run --rm \
  -v "$(pwd)":/app -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests
```

#### A3. Chạy từng microservice (5 terminal riêng)

```bash
# Terminal 1 — Auth
java -jar auth-service/target/auth-service-1.0.0-SNAPSHOT.jar

# Terminal 2 — URL
java -jar url-service/target/url-service-1.0.0-SNAPSHOT.jar

# Terminal 3 — Redirect
java -jar redirect-service/target/redirect-service-1.0.0-SNAPSHOT.jar

# Terminal 4 — Analytics
java -jar analytics-service/target/analytics-service-1.0.0-SNAPSHOT.jar

# Terminal 5 — API Gateway (chạy sau cùng)
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

Thứ tự quan trọng: **auth → url → redirect → analytics → gateway**.

#### A4. Kiểm tra backend

```bash
curl http://localhost:8080/actuator/health
```

### Phần B — Frontend

#### B1. Cài dependency

```bash
cd ~/Documents/url-shortener-fe
npm install
```

#### B2. Tạo file môi trường

```bash
cp .env.local.example .env.local
```

Nội dung `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_SHORT_URL_BASE=http://localhost:8083
```

#### B3. Chạy dev server

```bash
npm run dev
```

Mở http://localhost:3000

---

## Cách 3: Chỉ chạy Frontend (BE đã chạy sẵn)

Khi backend đã up (Docker hoặc local):

```bash
cd ~/Documents/url-shortener-fe
npm install
cp .env.local.example .env.local   # chỉ lần đầu
npm run dev
```

---

## Ports tổng hợp

| Port | Service |
|------|---------|
| 3000 | Frontend (Next.js) |
| 8080 | API Gateway |
| 8081 | Auth Service |
| 8082 | URL Service |
| 8083 | Redirect Service |
| 8084 | Analytics Service |
| 27017 | MongoDB |
| 6379 | Redis |
| 5672 | RabbitMQ |
| 15672 | RabbitMQ Management UI |

---

## Xử lý lỗi thường gặp

### `Cannot connect to Docker daemon`

→ Bật **Docker Desktop** rồi chạy lại `docker compose up`.

### Frontend báo lỗi network / CORS

→ Kiểm tra API Gateway đã chạy:

```bash
curl http://localhost:8080/actuator/health
```

→ Kiểm tra `.env.local` trỏ đúng `http://localhost:8080`.

### `401 Unauthorized` sau khi login

→ Xóa localStorage trong DevTools (Application → Local Storage) và đăng nhập lại.

### Short link không redirect

→ Kiểm tra redirect-service:

```bash
curl -I http://localhost:8083/{shortCode}
```

→ Phải trả `302 Found` với header `Location`.

### Service không start — port bị chiếm

```bash
# macOS — tìm process đang dùng port 8080
lsof -i :8080

# Kill process (thay PID)
kill -9 <PID>
```

### MongoDB connection refused (chạy local)

→ Đảm bảo container MongoDB đang chạy:

```bash
cd ~/Documents/url-shortener-be
docker compose up -d mongodb
```

### Build Maven thất bại

→ Dùng Java 21:

```bash
java -version   # phải là 21.x
```

---

## GeoIP (tùy chọn)

Để có analytics theo quốc gia/thành phố:

1. Tải [GeoLite2-City](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data)
2. Đặt file tại `url-shortener-be/data/geoip/GeoLite2-City.mmdb`
3. Restart analytics-service:

```bash
docker compose restart analytics-service
```

Không có file này hệ thống vẫn chạy bình thường — chỉ thiếu dữ liệu geo.

---

## Quy trình dev hàng ngày (gợi ý)

```bash
# Sáng — bật infra + BE
cd ~/Documents/url-shortener-be
docker compose up -d mongodb redis rabbitmq
# ... chạy 5 jar hoặc docker compose up -d (không --build nếu đã build)

# Terminal FE
cd ~/Documents/url-shortener-fe
npm run dev

# Tối — tắt
docker compose down   # trong thư mục BE
```

---

## API nhanh (test bằng curl)

```bash
# Đăng ký
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"123456"}'

# Đăng nhập (lấy accessToken)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"123456"}'

# Tạo short link (thay <TOKEN>)
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"longUrl":"https://google.com"}'
```
