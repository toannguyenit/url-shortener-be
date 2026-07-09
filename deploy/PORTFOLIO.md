# Portfolio VPS — Mẫu tổ chức nhiều dự án (Option A)

Mỗi dự án có **infra riêng** (MongoDB, Redis, RabbitMQ nếu cần), không share data.

```
/opt/
├── url-shortener/          # Dự án này
│   ├── .env
│   ├── infra/
│   │   └── docker-compose.yml    → network: urlshortener-net
│   ├── app/
│   │   └── docker-compose.yml
│   ├── infra-up.sh
│   ├── app-deploy.sh
│   └── deploy.sh
│
├── blog-app/               # Dự án portfolio khác (ví dụ)
│   ├── infra/
│   │   └── docker-compose.yml    → network: blog-net
│   └── app/
│       └── docker-compose.yml
│
└── task-api/               # Dự án khác chỉ cần Postgres
    └── ...
```

## URL Shortener — lệnh deploy

```bash
cd /opt/url-shortener
cp .env.example .env   # sửa secrets + domain

# Lần đầu: khởi động infra
chmod +x infra-up.sh app-deploy.sh deploy.sh
./infra-up.sh

# Deploy / cập nhật app (CI/CD chạy bước này)
./app-deploy.sh

# Hoặc cả hai
./deploy.sh
```

## Tài nguyên riêng của dự án này

| Thành phần | Tên container | Network | Volume |
|------------|---------------|---------|--------|
| MongoDB | `urlshortener-mongodb` | `urlshortener-net` | `urlshortener-mongodata` |
| Redis | `urlshortener-redis` | `urlshortener-net` | `urlshortener-redisdata` |
| RabbitMQ | `urlshortener-rabbitmq` | `urlshortener-net` | `urlshortener-rabbitmqdata` |
| RabbitMQ vhost | `urlshortener` | — | — |
| MongoDB database | `urlshortener` | — | — |

## Dừng / xóa

```bash
# Chỉ dừng app (giữ data infra)
docker compose -f app/docker-compose.yml down

# Dừng cả infra (data vẫn trong volumes)
docker compose -f infra/docker-compose.yml down

# XÓA HẾT DATA (cẩn thận!)
docker compose -f app/docker-compose.yml down
docker compose -f infra/docker-compose.yml down -v
```

## CI/CD

- Push `main` BE → chỉ `app-deploy.sh` logic (pull + up app)
- Push `main` FE → pull frontend + nginx
- Infra **không** redeploy mỗi lần push code

## RAM ước tính (dự án này riêng)

~2–2.5 GB cho infra + ~3–4 GB cho app ≈ **5–6.5 GB** tổng.

VPS 8GB chạy 1 dự án này + 1 dự án nhỏ khác vẫn được; 2 dự án full microservices cần 16GB.
