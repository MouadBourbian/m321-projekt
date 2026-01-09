# Schnellstart Guide

Diese Anleitung hilft Ihnen, das System schnell zum Laufen zu bringen.

## Voraussetzungen

- Java 21 JDK installiert
- Maven 3.8+ installiert
- Docker und Docker Compose installiert

## Schritt 1: Services bauen

```bash
./build-all.sh
```

Oder manuell:
```bash
cd order-service && mvn clean package && cd ..
cd payment-service && mvn clean package && cd ..
cd kitchen-service && mvn clean package && cd ..
cd delivery-service && mvn clean package && cd ..
```

## Schritt 2: System starten

```bash
docker-compose up --build
```

Warten Sie, bis alle Services gestartet sind (ca. 30 Sekunden).

## Schritt 3: Erste Bestellung testen

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "pizza": "Margherita",
    "quantity": 2,
    "address": "Musterstrasse 123, 8000 Zürich",
    "customerName": "Max Mustermann"
  }'
```

## Schritt 4: Automatische Tests ausführen

```bash
./test-system.sh
```

## Schritt 5: Logs beobachten

In separaten Terminals:

```bash
# Order Service Logs
docker-compose logs -f order-service

# Payment Service Logs
docker-compose logs -f payment-service

# Kitchen Service Logs
docker-compose logs -f kitchen-service

# Delivery Service Logs
docker-compose logs -f delivery-service
```

## RabbitMQ Management UI

Öffnen Sie http://localhost:15672 im Browser
- Username: `guest`
- Password: `guest`

## Wichtige Endpoints

- Order Service: http://localhost:8080/orders
- Payment Service: http://localhost:8081/pay
- Delivery Service: http://localhost:8083/deliveries

## Resilience Tests

### Test 1: Kitchen Service offline (Pufferung)

```bash
# Kitchen stoppen
docker-compose stop kitchen-service

# 5 Bestellungen absenden
for i in {1..5}; do
  curl -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -d "{\"pizza\": \"Margherita\", \"quantity\": 1, \"address\": \"Test $i\", \"customerName\": \"User $i\"}"
done

# Kitchen wieder starten
docker-compose start kitchen-service

# Logs prüfen - alle 5 sollten verarbeitet werden
docker-compose logs -f kitchen-service
```

### Test 2: Payment Service offline (Fehlerbehandlung)

```bash
# Payment stoppen
docker-compose stop payment-service

# Bestellung versuchen - sollte freundliche Fehlermeldung geben
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"pizza": "Margherita", "quantity": 1, "address": "Test", "customerName": "User"}'

# Payment wieder starten
docker-compose start payment-service
```

### Test 3: Skalierung (Competing Consumers)

```bash
# 3 Kitchen Instanzen starten
docker-compose up --scale kitchen-service=3 -d

# 10 Bestellungen absenden
for i in {1..10}; do
  curl -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -d "{\"pizza\": \"Margherita\", \"quantity\": 1, \"address\": \"Test $i\", \"customerName\": \"User $i\"}"
done

# Logs zeigen, dass verschiedene Instanzen arbeiten
docker-compose logs kitchen-service | grep "Received order"
```

## Troubleshooting

### "Connection refused" Fehler
Warten Sie, bis alle Services hochgefahren sind. RabbitMQ braucht ca. 10-15 Sekunden.

### Port bereits belegt
```bash
# Prüfen, welcher Prozess den Port nutzt
lsof -i :8080
# Prozess beenden oder Port in docker-compose.yml ändern
```

### Services neu bauen
```bash
docker-compose down
./build-all.sh
docker-compose up --build
```

## Nächste Schritte

Lesen Sie die vollständige [README.md](README.md) für:
- Detaillierte Architektur
- API Contracts
- Erweiterte Test-Szenarien
- Erweiterungsmöglichkeiten
