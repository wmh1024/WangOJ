# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# 同源部署：接口走 /api，由 Nginx 反代，不需要注入绝对地址
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
# 与 CI 保持一致：build = type-check + build-only，避免镜像构建跳过类型检查
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

# 注意：构建上下文是 ./web，因此 nginx 配置放在 web/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz | grep -q ok || exit 1
