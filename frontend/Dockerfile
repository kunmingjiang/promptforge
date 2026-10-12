# ---- 编译阶段 ----
FROM node:20-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- 运行阶段：Nginx 托管 SPA ----
FROM nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
