# Etapa 1: build estático com Node.
FROM node:25-alpine AS build
WORKDIR /app

# Dependências primeiro: a camada fica em cache enquanto package*.json não mudar.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# O Vite embute a URL da API no bundle em tempo de build, por isso é ARG (e não ENV de runtime).
# É a URL vista pelo navegador do usuário, não pela rede do Docker.
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Etapa 2: só os arquivos gerados, servidos pelo nginx sem root (por isso a porta 8080, e não a 80).
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080

# 127.0.0.1, e não localhost: no container, localhost resolve primeiro para ::1, e o nginx só escuta IPv4
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/ > /dev/null || exit 1
