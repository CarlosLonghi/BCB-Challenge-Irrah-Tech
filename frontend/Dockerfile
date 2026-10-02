# Etapa 1: build estático com Node.
FROM node:24-alpine AS build
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

# Etapa 2: só os arquivos gerados, servidos pelo nginx.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
