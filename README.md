# Desafio Backend - BCB Chat API

---

## Descrição do Projeto

A **BCB Chat API** é um sistema backend em Java (Spring Boot) para gerenciamento de clientes, conversas e mensagens com sistema de filas prioritárias.

Funcionalidades implementadas:

* **Autenticação** simples via CPF/CNPJ com token Bearer (UUID).
* CRUD de **Clientes** (pré-pago/pós-pago) e consulta de saldo/limite.
* CRUD de **Conversas**, listagem e obtenção de mensagens por conversa.
* Envio e recepção de **Mensagens** (FIFO com priorização URGENT/NORMAL).
* **Sistema de Fila** em memória (PriorityBlockingQueue) com processamento automático em background.
* Endpoint de **estatísticas da fila** (`GET /queue/status`).

## Tecnologias Utilizadas

* **Java 21**
* **Spring Boot** (Web, Security, Data JPA, Validation)
* **PostgreSQL**
* **Docker & Docker Compose**
* **Springdoc OpenAPI** (Swagger UI)
* **Lombok**

## Organização do Código

```text
src/main/java/com/bcb/
├── config/
│   └── docs/          # Interfaces de documentação Swagger
├── controller/        # Controllers REST
├── dto/               # Data Transfer Objects
├── mapper/            # Mappers entre DTOs e Entities
├── model/             # Entidades JPA e enums
├── repository/        # Repositórios Spring Data
├── service/           # Regras de negócio
└── infra/             # Fila e worker em background
```

## Docker Compose

O projeto inclui um `docker-compose.yml` que orquestra:

* **app**: a aplicação Spring Boot
* **db**: container PostgreSQL

### Executando aplicação

1. Clone o repositório:

   ```bash
   git clone https://github.com/seu-usuario/bcb-chat-api.git
   cd bcb-chat-api
   ```
2. Inicie os containers:

   ```bash
   docker-compose up -d --build
   ```
3. Acesse a API em `http://localhost:8080` e o Swagger UI em `http://localhost:8080/swagger-ui/index.html`.

## Endpoints Principais

### Autenticação

* `POST /auth` – Gera token bearer com **CPF/CNPJ** do Cliente. **(endpoint público)**

### Clientes

* `POST /clients` – Cria cliente. **(endpoint público)**
* `GET /clients` – Lista clientes.
* `GET /clients/:id` – Obtém detalhes de um cliente.
* `PUT /clients/:id` – Atualiza dados do cliente.
* `GET /clients/:id/balance` – Consulta saldo/limite do cliente.

### Mensagens

* `POST /messages` – Envia nova mensagem (cria conversa se necessário).
* `GET /messages` – Lista mensagens do usuário autenticado.
* `GET /messages/:id` – Obtém detalhes de uma mensagem.
* `GET /messages/:id/status` – Verifica status de envio de uma mensagem.

### Conversas

* `GET /conversations` – Lista conversas do cliente autenticado.
* `GET /conversations/:id` – Obtém detalhes de uma conversa.
* `GET /conversations/:id/messages` – Lista mensagens de uma conversa.

### Fila

* `GET /queue/status` – Estatísticas da fila (quantidade por status e prioridade).

## Decisões de Design e Limitações

* **Autenticação**: uso de tokens UUID e Spring Security para simplicidade.
* **DTO vs. Model**: separação clara entre entidades JPA e objetos de API.
* **Fila em Memória**: prioridade URGENT > NORMAL, sem persistência de fila.
* **Processamento**: sincronizado em background, sem sistema de retry avançado.

Limitações / Trabalho Futuro:

* Implementar **JWT** em vez de UUID.
* Persistir a fila em banco (para tolerância a falhas).
* Adicionar **paginação** e filtros avançados nas listagens.
* Testes automatizados (unitários e de integração).
* Documentar **OpenAPI** completo com exemplos completos.
* Configurar **CI/CD** e métricas de performance.

---

*Desenvolvido por Carlos Longhi*
