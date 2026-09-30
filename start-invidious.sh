#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "==> 启动 Invidious (invidious + postgres)..."
docker compose up -d

echo ""
echo "==> 等待服务就绪（约 10s）..."
sleep 10

echo "==> 容器状态："
docker compose ps

echo ""
echo "本地访问:  http://localhost:3000"
echo "手机访问:  http://<本机局域网IP>:3000   (App 内长按页面可修改地址)"
echo ""
echo "查看日志:  docker compose logs -f invidious"
echo "停止服务:  docker compose down"
