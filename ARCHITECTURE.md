# Architektur-Dokumentation: Distributed Pizza Platform

## Übersicht

Die Distributed Pizza Platform ist eine Microservices-Architektur, die einen kompletten Pizza-Lieferdienst demonstriert. Das System verwendet moderne Patterns wie asynchrone Kommunikation, Event-Driven Architecture und Competing Consumers.

## System-Architektur

### Komponenten-Übersicht

```mermaid
graph TB
    Client[Client / API]
    Order[Order Service Port 8080<br/>• REST API Endpoint<br/>• Input Validation<br/>• Sync Payment<br/>• Async Queueing]
    Payment[Payment Service Port 8081<br/>• Payment Processing<br/>• Random Failures<br/>• Delay Simulation]
    RabbitMQ[(RabbitMQ Broker<br/>• order.placed queue<br/>• order.ready queue<br/>• Durable queues)]
    Kitchen[Kitchen Service Port 8082<br/>• Order Consumer<br/>• Cooking Simulation<br/>• Ready Event Publisher<br/>• Scalable n instances]
    Delivery[Delivery Service Port 8083<br/>• Ready Event Consumer<br/>• Driver Assignment<br/>• Status REST API<br/>• Notifications]

    Client -->|HTTP| Order
    Order -->|REST sync| Payment
    Order -->|AMQP async| RabbitMQ
    RabbitMQ -->|order.placed| Kitchen
    Kitchen -->|order.ready| RabbitMQ
    RabbitMQ -->|order.ready| Delivery
```

## Kommunikations-Patterns

### Bestellablauf

```mermaid
sequenceDiagram
    participant Client
    participant Order as Order Service
    participant Payment as Payment Service
    participant RabbitMQ as Message Broker
    participant Kitchen as Kitchen Service
    participant Delivery as Delivery Service

    Client->>Order: POST /orders
    Order->>Order: Validierung
    Order->>Payment: POST /pay
    Payment-->>Order: 200 OK (transactionId)
    Order->>RabbitMQ: order.placed
    Order-->>Client: 200 OK (orderId)

    RabbitMQ->>Kitchen: order.placed
    Kitchen->>Kitchen: Zubereitung (5-10s)
    Kitchen->>RabbitMQ: order.ready

    RabbitMQ->>Delivery: order.ready
    Delivery->>Delivery: Fahrerzuweisung
    Delivery->>Delivery: Benachrichtigung
```

### 1. Synchrone Kommunikation (REST)

**Order Service → Payment Service**

```
POST /pay
Content-Type: application/json

{
  "orderId": "uuid",
  "customerName": "Max Mustermann",
  "amount": 31.98
}

Response:
{
  "transactionId": "uuid",
  "success": true/false,
  "message": "Payment processed successfully"
}
```

**Eigenschaften:**

- Timeout: 5 Sekunden
- Error Handling: Circuit Breaker Pattern (Basis)
- Resilience: Graceful Degradation bei Service-Ausfall

### 2. Asynchrone Kommunikation (RabbitMQ)

**Order Service → Kitchen Service (order.placed Queue)**

```json
{
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "pizza": "Margherita",
  "quantity": 2,
  "address": "Musterstrasse 123, 8000 Zürich",
  "customerName": "Max Mustermann",
  "timestamp": "2026-01-09T10:30:00"
}
```

**Kitchen Service → Delivery Service (order.ready Queue)**

```json
{
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "pizza": "Margherita",
  "quantity": 2,
  "address": "Musterstrasse 123, 8000 Zürich",
  "customerName": "Max Mustermann",
  "preparedAt": "2026-01-09T10:35:00"
}
```

**Eigenschaften:**

- Durable Queues: Nachrichten überleben Broker-Neustart
- At-Least-Once Delivery: Garantierte Nachrichtenzustellung
- Competing Consumers: Lastverteilung über mehrere Instanzen

## Design Patterns

### 1. Contract-First Design

- Alle Schnittstellen sind vor der Implementierung definiert
- JSON-Schemas für alle API-Endpunkte
- Message-Schemas für alle Events

### 2. Loose Coupling

- Services kennen sich nicht direkt
- Kommunikation über Message Broker
- Keine direkten Abhängigkeiten zwischen Services

### 3. Event-Driven Architecture

- Events statt direkte Aufrufe
- Publish-Subscribe Pattern
- Asynchrone Verarbeitung

### 4. Competing Consumers

- Mehrere Kitchen Service Instanzen
- Automatische Lastverteilung durch RabbitMQ
- Horizontale Skalierbarkeit

### 5. Circuit Breaker (Basis)

- Timeout-Handling bei REST-Calls
- Fehlerbehandlung ohne Crash
- Graceful Degradation

## Resilience & Hochverfügbarkeit

```mermaid
sequenceDiagram
    participant Order as Order Service
    participant Payment as Payment Service
    participant RabbitMQ as Message Broker
    participant Kitchen as Kitchen Service

    Note over Order,Kitchen: Szenario 1: Payment Service nicht erreichbar
    Order->>Payment: POST /pay (Timeout/Error)
    Payment--xOrder: Connection refused
    Order->>Order: Freundliche Fehlermeldung
    Note right of Order: "Payment system is<br/>currently unavailable"

    Note over Order,Kitchen: Szenario 2: Kitchen Service offline
    Order->>Payment: POST /pay
    Payment-->>Order: 200 OK
    Order->>RabbitMQ: order.placed
    Note right of RabbitMQ: Nachricht gepuffert
    Kitchen->>RabbitMQ: Kitchen connects
    RabbitMQ->>Kitchen: Consume buffered messages
```

### Verfügbarkeitsmerkmale

- **Durable Queues:** RabbitMQ Persistence verhindert Datenverluste
- **Async Processing:** Message Broker entkoppelt Services
- **Retry Logic:** Spring AMQP mit automatischen Wiederholungen
- **Timeout Handling:** RestTemplate Config verhindert hängende Requests
- **Horizontal Scaling:** Stateless Services ermöglichen beliebige Skalierung

## Technologie-Stack

### Message Broker

- **RabbitMQ 3.12**: AMQP Message Broker
- **Management UI**: Monitoring und Administration
- **Durable Queues**: Persistente Nachrichtenspeicherung

### Build & Deployment

- **Docker**: Containerisierung
- **Docker Compose**: Multi-Container Orchestrierung

## Deployment-Architektur

### Docker Compose Setup

```yaml
services:
  - rabbitmq: Port 5672 (AMQP), 15672 (Management UI)
  - order-service: Port 8080
  - payment-service: Port 8081
  - kitchen-service: Port 8082 (skalierbar)
  - delivery-service: Port 8083

networks:
  - pizza-network (bridge)
```

### Skalierung

```bash
# Standard: 1 Kitchen Instance
docker-compose up

# Skaliert: 3 Kitchen Instances
docker-compose up --scale kitchen-service=3
```

## Monitoring & Observability

### Health Endpoints

Alle Services bieten Health-Checks:

- `GET /orders/health` (Order Service)
- `GET /health` (Payment Service)
- `GET /deliveries/health` (Delivery Service)

### RabbitMQ Management UI

Zugriff: http://localhost:15672

- Username: guest
- Password: guest

**Features:**

- Queue Monitoring
- Message Rates
- Consumer Status
- Memory Usage

### Logging

**Strukturiertes Logging mit SLF4J:**

- INFO Level: Normale Operations
- DEBUG Level: RabbitMQ-Details
- ERROR Level: Fehler und Exceptions

**Log-Ausgaben enthalten:**

- Order IDs für Tracing
- Instance IDs (Kitchen Service)
- Timestamps
- Service Names
