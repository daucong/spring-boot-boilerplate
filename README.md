# Spring Boot Boilerplate

A personal starter template for Spring Boot projects with commonly used infrastructure pre-configured and ready to run via Docker Compose.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot, Spring Cloud |
| Messaging | Kafka, RabbitMQ |
| Cache | Redis |
| Database | PostgreSQL 16 |
| Container | Docker Compose |

## Infrastructure

Spins up the following services locally:

- **PostgreSQL** — primary database (port 5434)
- **Redis** — caching (port 6379) + RedisInsight UI (port 5540)
- **Kafka** + Zookeeper — event streaming (port 9092) + Kafka UI (port 8090)
- **RabbitMQ** — message queue (port 5672) + Management UI (port 15672)
- **app-java** — Spring Boot application (port 1010)

## Getting Started

**1. Clone the repo**
```bash
git clone https://github.com/daucong/spring-boot-boilerplate.git
cd spring-boot-boilerplate
```

**2. Create `.env` file**
```env
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

**3. Run**
```bash
docker compose up -d --build
```

## Project Structure

```
app-java/
├── config/       # Spring configuration (Kafka, RabbitMQ, Redis, etc.)
├── controller/   # REST API endpoints
├── event/        # Kafka/RabbitMQ event producers & consumers
├── model/        # Entity & DTO classes
├── repository/   # Spring Data JPA repositories
└── service/      # Business logic
```