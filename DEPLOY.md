# Hướng dẫn Deploy Backend lên VPS (Production)

Tài liệu triển khai đầy đủ cho **url-shortener-be** — Docker, Nginx, SSL, GHCR, CI/CD.

**Repo liên quan:** [url-shortener-fe](https://github.com/toannguyenit/url-shortener-fe) | [ARCHITECTURE.md](./ARCHITECTURE.md) (kiến trúc full-stack)

**Kiến trúc:** [ARCHITECTURE.md](./ARCHITECTURE.md) | **Deploy:** [DEPLOY.md](./DEPLOY.md) | **Local:** [STARTUP.md](./STARTUP.md)

---

## Mục lục

1. [Kiến trúc production](#1-kiến-trúc-production)
2. [Yêu cầu](#2-yêu-cầu)
3. [Cấu hình DNS](#3-cấu-hình-dns)
4. [Chuẩn bị VPS](#4-chuẩn-bị-vps)
5. [Copy file deploy lên VPS](#5-copy-file-deploy-lên-vps)
6. [Cấu hình `.env` và Nginx](#6-cấu-hình-env-và-nginx)
7. [SSL (Let's Encrypt)](#7-ssl-lets-encrypt)
8. [Build & push Docker images (GHCR)](#8-build--push-docker-images-ghcr)
9. [Khởi động lần đầu](#9-khởi-động-lần-đầu)
10. [CI/CD tự động (GitHub Actions)](#10-cicd-tự-động-github-actions)
11. [Vận hành hàng ngày](#11-vận-hành-hàng-ngày)
12. [Portfolio — nhiều dự án trên 1 VPS](#12-portfolio--nhiều-dự-án-trên-1-vps)
13. [Troubleshooting](#13-troubleshooting)
14. [Checklist go-live](#14-checklist-go-live)

---

## 1. Kiến trúc production

### URL production (ví dụ thực tế)

| Subdomain | Service | URL |
|-----------|---------|-----|
| Dashboard (FE) | Next.js | `https://urlshort.toannguyenit.cloud` |
| API Gateway | Spring Boot | `https://api-urlshort.toannguyenit.cloud` |
| Short link | redirect-service | `https://go-urlshort.toannguyenit.cloud/{code}` |

### Sơ đồ

```
Internet
    │
    ▼
┌──────────────────────────────────────────────────┐
│  VPS Ubuntu 22.04                                  │
│                                                    │
│  nginx (container) :80 / :443                      │
│    ├── urlshort.*        → frontend:3000           │
│    ├── api-urlshort.*    → api-gateway:8080      │
│    └── go-urlshort.*     → redirect-service:8083  │
│                                                    │
│  /opt/url-shortener/                               │
│    infra/  → MongoDB, Redis, RabbitMQ              │
│              network: urlshortener-net             │
│    app/    → 5 microservices + FE + nginx          │
└──────────────────────────────────────────────────┘
```

### Microservices (backend)

| Service | Port nội bộ | Vai trò |
|---------|-------------|---------|
| api-gateway | 8080 | JWT, CORS, rate limit, routing |
| auth-service | 8081 | Đăng ký, đăng nhập |
| url-service | 8082 | CRUD link, QR code |
| redirect-service | 8083 | Redirect 302, Redis cache, RabbitMQ publish |
| analytics-service | 8084 | Consumer RabbitMQ, GeoIP, dashboard API |

### Option A — Infra tách riêng (portfolio)

MongoDB, Redis, RabbitMQ **không share** với dự án khác. Chi tiết: [`deploy/PORTFOLIO.md`](deploy/PORTFOLIO.md).

### Cấu trúc thư mục trên VPS

```
/opt/url-shortener/
├── .env                          # Secrets + domain (KHÔNG commit)
├── infra/docker-compose.yml      # MongoDB, Redis, RabbitMQ
├── app/docker-compose.yml        # Microservices + FE + nginx
├── nginx/nginx.conf
├── certbot/conf/                 # SSL certificates
├── certbot/www/
├── data/geoip/                   # GeoLite2 (tùy chọn)
├── infra-up.sh
├── app-deploy.sh
└── deploy.sh
```

---

## 2. Yêu cầu

| Resource | Khuyến nghị |
|----------|-------------|
| CPU | 2 vCPU |
| RAM | 8 GB (microservices + MongoDB + Redis) |
| Disk | 40–60 GB SSD |
| OS | Ubuntu 22.04 LTS |
| Domain | 3 subdomain (hoặc dùng pattern `api-{project}`, `go-{project}`) |

---

## 3. Cấu hình DNS

Tại panel DNS (Cloudflare, Namecheap, ...), tạo **bản ghi A** trỏ về **IP VPS**:

| Type | Host / Name | Value |
|------|-------------|-------|
| A | `urlshort` | `103.252.93.178` |
| A | `api-urlshort` | `103.252.93.178` |
| A | `go-urlshort` | `103.252.93.178` |

> Thay IP bằng IP VPS thật. Host chỉ gõ phần subdomain, **không** gõ full domain.

Kiểm tra sau 5–30 phút:

```bash
dig +short urlshort.toannguyenit.cloud
dig +short api-urlshort.toannguyenit.cloud
dig +short go-urlshort.toannguyenit.cloud
```

**Cloudflare:** tắt proxy (mây xám) khi lấy SSL lần đầu bằng certbot standalone.

---

## 4. Chuẩn bị VPS

### 4.1 Cài Docker

```bash
ssh root@<IP_VPS>

curl -fsSL https://get.docker.com | sh
apt update && apt install -y docker-compose-plugin

docker --version
docker compose version
```

### 4.2 Tạo thư mục

```bash
mkdir -p /opt/url-shortener/{certbot/conf,certbot/www,data/geoip}
```

### 4.3 Firewall

```bash
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

**Không mở** port 27017, 6379, 8080–8084 ra internet.

---

## 5. Copy file deploy lên VPS

### Cách 1 — Clone từ GitHub (khuyến nghị)

Trên VPS:

```bash
cd /tmp
git clone https://github.com/toannguyenit/url-shortener-be.git
cp -r url-shortener-be/deploy/. /opt/url-shortener/
cd /opt/url-shortener
cp .env.example .env
chmod +x infra-up.sh app-deploy.sh deploy.sh
```

> **Lưu ý:** Không copy nguyên chữ `<github-username>` — dùng username thật.

Hoặc clone repo backend (**không** gõ nguyên chữ `<username>`):

```bash
cd /tmp
git clone https://github.com/toannguyenit/url-shortener-be.git
cp -r url-shortener-be/deploy/. /opt/url-shortener/
```

### Cách 2 — rsync từ Mac

> macOS có thể chặn đọc `~/Documents` → lỗi `Operation not permitted`. Dùng **Cách 1** (clone trên VPS) hoặc nén folder bằng Finder rồi `scp`.

```bash
export VPS=root@103.252.93.178   # dùng $VPS, không gõ chữ IP_VPS

rsync -avz --progress \
  /path/to/url-shortener-be/deploy/ \
  $VPS:/opt/url-shortener/
```

### Cách 3 — scp file zip (Mac bị chặn Documents)

---

## 6. Cấu hình `.env` và Nginx

### 6.1 File `.env`

```bash
nano /opt/url-shortener/.env
```

```env
# Domain
DOMAIN=urlshort.toannguyenit.cloud
API_URL=https://api-urlshort.toannguyenit.cloud
SHORT_URL_BASE=https://go-urlshort.toannguyenit.cloud
FRONTEND_URL=https://urlshort.toannguyenit.cloud

# Secrets
JWT_SECRET=<openssl rand -base64 48>
GHCR_OWNER=toannguyenit
IMAGE_TAG=latest

# Database
MONGO_DB=urlshortener

# RabbitMQ
RABBITMQ_USER=urlshortener
RABBITMQ_PASSWORD=<password-mạnh>
RABBITMQ_VHOST=urlshortener
```

`FRONTEND_URL` được map sang `CORS_ALLOWED_ORIGINS` cho api-gateway.

### 6.2 Nginx

```bash
cd /opt/url-shortener/nginx

sed -i \
  -e 's/yourdomain\.com/urlshort.toannguyenit.cloud/g' \
  -e 's/api\.urlshort\.toannguyenit\.cloud/api-urlshort.toannguyenit.cloud/g' \
  -e 's/s\.urlshort\.toannguyenit\.cloud/go-urlshort.toannguyenit.cloud/g' \
  nginx.conf

grep server_name nginx.conf
```

### 6.3 RabbitMQ password (đọc kỹ)

`RABBITMQ_PASSWORD` **do bạn tự đặt** trong `.env`. RabbitMQ chỉ lấy password **lần đầu** khi tạo volume.

| Tình huống | Hành động |
|------------|-----------|
| Chưa chạy `./infra-up.sh` | Sửa `.env` → `./infra-up.sh` |
| Đã chạy infra với password cũ | Dùng lại password cũ **hoặc** xóa volume `urlshortener-rabbitmqdata` rồi `./infra-up.sh` lại |

Kiểm tra password khớp:

```bash
grep RABBITMQ /opt/url-shortener/.env
docker exec urlshortener-redirect env | grep RABBIT
docker exec urlshortener-rabbitmq rabbitmqctl authenticate_user urlshortener '<password>'
```

---

## 7. SSL (Let's Encrypt)

### Tắt nginx hệ thống (nếu chiếm port 80)

```bash
systemctl stop nginx
systemctl disable nginx
ss -tlnp | grep ':80 '   # không có output = OK
```

### Lấy certificate

```bash
cd /opt/url-shortener

docker run -it --rm \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -p 80:80 \
  certbot/certbot certonly --standalone \
  --email toannguyenit@gmail.com \
  --agree-tos \
  --no-eff-email \
  -d urlshort.toannguyenit.cloud \
  -d api-urlshort.toannguyenit.cloud \
  -d go-urlshort.toannguyenit.cloud
```

Kiểm tra:

```bash
ls certbot/conf/live/urlshort.toannguyenit.cloud/
```

Cert tự renew qua container `urlshortener-certbot` khi app chạy.

---

## 8. Build & push Docker images (GHCR)

### 8.1 Package visibility (quan trọng)

> **Repo public ≠ Package public.** Đổi visibility repo trên GitHub **không** tự public Docker image.

GitHub → **Packages** → từng package → **Package settings** → **Change visibility** → **Public**:

- `url-shortener-auth`, `url-shortener-url`, `url-shortener-redirect`
- `url-shortener-analytics`, `url-shortener-gateway`, `url-shortener-fe`

### 8.3 Push code GitHub

```bash
git remote add origin git@github.com:toannguyenit/url-shortener-be.git
git push -u origin main
```

### 8.4 Images được build

| Image GHCR | Dockerfile |
|------------|------------|
| `ghcr.io/toannguyenit/url-shortener-auth` | `auth-service/Dockerfile` |
| `ghcr.io/toannguyenit/url-shortener-url` | `url-service/Dockerfile` |
| `ghcr.io/toannguyenit/url-shortener-redirect` | `redirect-service/Dockerfile` |
| `ghcr.io/toannguyenit/url-shortener-analytics` | `analytics-service/Dockerfile` |
| `ghcr.io/toannguyenit/url-shortener-gateway` | `api-gateway/Dockerfile` |

Push `main` → workflow `.github/workflows/deploy.yml` tự build.

### 8.5 Login GHCR trên VPS (package private)

```bash
echo <GITHUB_PAT> | docker login ghcr.io -u toannguyenit --password-stdin
```

---

## 9. Khởi động lần đầu

```bash
cd /opt/url-shortener

# 1. Infra — chạy 1 lần (hoặc khi restart DB)
./infra-up.sh

# 2. Pull + chạy app (LUÔN dùng --env-file)
docker compose --env-file .env -f app/docker-compose.yml pull
./app-deploy.sh

# 3. Kiểm tra
docker compose --env-file .env -f infra/docker-compose.yml ps
docker compose --env-file .env -f app/docker-compose.yml ps
```

> **Quan trọng:** `docker compose -f app/docker-compose.yml` **không** tự đọc `.env` ở thư mục cha. Luôn thêm `--env-file .env`.

### Test

```bash
curl -s https://api-urlshort.toannguyenit.cloud/actuator/health
# {"status":"UP",...}
```

---

## 10. CI/CD tự động (GitHub Actions)

### Luồng

```
push main → build 5 BE images → push GHCR → SSH VPS → pull + up services
```

Workflow: `.github/workflows/deploy.yml`

### GitHub Secrets (repo url-shortener-be)

**Settings → Secrets and variables → Actions → Secrets**

| Secret | Ví dụ | Mô tả |
|--------|-------|-------|
| `VPS_HOST` | `103.252.93.178` | IP VPS |
| `VPS_USER` | `root` | User SSH |
| `VPS_PASSWORD` | `***` | Mật khẩu SSH |

### Bật login password trên VPS (nếu cần)

```bash
grep PasswordAuthentication /etc/ssh/sshd_config
# Phải là yes
sed -i 's/^#*PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
systemctl restart sshd
```

### Deploy thủ công sau push

```bash
cd /opt/url-shortener
./app-deploy.sh
```

---

## 11. Vận hành hàng ngày

```bash
cd /opt/url-shortener
COMPOSE="docker compose --env-file .env -f app/docker-compose.yml"

# Logs
$COMPOSE logs -f api-gateway
$COMPOSE logs -f redirect-service

# Restart 1 service
$COMPOSE restart url-service

# Deploy image mới
./app-deploy.sh

# Backup MongoDB
docker exec urlshortener-mongodb mongodump --db urlshortener --out /data/db/backup
docker cp urlshortener-mongodb:/data/db/backup ./backup-$(date +%Y%m%d)
```

---

## 12. Portfolio — nhiều dự án trên 1 VPS

Mỗi dự án: `/opt/<tên-dự-án>/` với network + DB riêng.

```
urlshort.toannguyenit.cloud      → /opt/url-shortener/
blog.toannguyenit.cloud          → /opt/blog-app/     (sau này)
```

Xem [`deploy/PORTFOLIO.md`](deploy/PORTFOLIO.md).

---

## 13. Troubleshooting

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|-------------|-----------|
| `GHCR_OWNER variable is not set` | Thiếu `--env-file .env` | `docker compose --env-file .env -f app/...` |
| `invalid reference format` `ghcr.io//url-...` | `GHCR_OWNER` trống | Sửa `.env`, thêm `--env-file` |
| `403 Forbidden` pull image | Package private | Public **từng** package GHCR (không chỉ repo) |
| Certbot `port 80 already in use` | nginx hệ thống | `systemctl stop nginx && systemctl disable nginx` |
| `missing server host` (Actions) | Thiếu `VPS_HOST` secret | Thêm Secrets trên GitHub |
| `missing server host` + dùng `IP_VPS` literal | Gõ placeholder thay vì IP/`$VPS` | Dùng `root@103.252.93.178` hoặc `$VPS` |
| FE "Cannot connect port 8080" | FE build sai API URL | Rebuild FE — xem `DEPLOY.md` repo FE |
| Short link **500** | RabbitMQ publish lỗi (image cũ) | `pull redirect-service` image mới; kiểm tra `RABBITMQ_VHOST` |
| Short link **404** | Code không tồn tại | Bình thường |
| Short link **410** | Link hết hạn / inactive | Tạo link mới hoặc gia hạn |
| Short link **502** | redirect-service down | `docker logs urlshortener-redirect` |
| CORS error | Sai `FRONTEND_URL` | Sửa `.env`, restart gateway |
| RabbitMQ auth fail | Password `.env` ≠ lúc tạo infra | `rabbitmqctl authenticate_user` hoặc reset volume |
| Analytics không có click | RabbitMQ lỗi nhưng redirect đã fix | Xem logs `urlshortener-analytics` |
| Dashboard / Analytics **500** | `analytics-service` down (thường do RabbitMQ auth lúc startup) | `docker ps` + `docker logs urlshortener-analytics`; kiểm tra `RABBITMQ_PASSWORD` khớp infra; `pull` + `up -d analytics-service` |

---

## 14. Checklist go-live

- [ ] DNS 3 subdomain → IP VPS
- [ ] `.env` đầy đủ (`JWT_SECRET`, `GHCR_OWNER`, `FRONTEND_URL`)
- [ ] `nginx.conf` đúng domain
- [ ] SSL certificate OK
- [ ] `./infra-up.sh` healthy
- [ ] 6 GHCR packages public (5 BE + 1 FE)
- [ ] GitHub Secrets: `VPS_HOST`, `VPS_USER`, `VPS_PASSWORD`
- [ ] FE image build với `NEXT_PUBLIC_API_URL` production
- [ ] Test: đăng ký → tạo link → mở `go-urlshort.../code` → analytics

---

## Tóm tắt lệnh (copy nhanh)

```bash
# Trên VPS — lần đầu
cd /tmp && git clone https://github.com/toannguyenit/url-shortener-be.git
cp -r url-shortener-be/deploy/. /opt/url-shortener/
cd /opt/url-shortener && cp .env.example .env && nano .env
# sửa nginx, certbot, rồi:
./infra-up.sh
docker compose --env-file .env -f app/docker-compose.yml pull
./app-deploy.sh
```

Mỗi lần push `main` (BE): CI/CD build 5 services → deploy app. **Infra không redeploy.**
