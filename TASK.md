# **Projektauftrag: Distributed Pizza Platform**

**Ziel:** Entwicklung eines verteilten Systems für einen Lieferdienst. Das System besteht aus vier unabhängigen Komponenten, die über definierte Schnittstellen (REST & Messaging) miteinander kommunizieren.

**Teamgrösse:** 4 Personen **Fokus:** Verteiltheit, Schnittstellen-Design, Hochverfügbarkeit (Resilience).

---

## 1\. Die Architektur

Das System simuliert den Ablauf einer Bestellung von der Aufgabe bis zur Auslieferung. Es muss demonstriert werden, dass Teile des Systems ausfallen können, ohne dass der gesamte Prozess stoppt.

### Die 4 Komponenten (Rollenverteilung)

Jedes Teammitglied wählt eine Komponente und implementiert diese in einer **Sprache/Framework nach Wahl** (z. B. Node.js, Python, Java, Go, C#).

### Rolle 1: Order Service

Dies ist der Einstiegspunkt für den Kunden.

- **Aufgabe:** Bereitstellung einer REST-Schnittstelle (`POST /orders`), um Bestellungen entgegenzunehmen.
- **Verantwortung:**
  - Validierung der Eingabedaten (Ist eine Pizza gewählt? Ist die Adresse da?).
  - Kommunikation mit dem **Payment Service** (synchron).
  - Bei Erfolg: Weiterleitung der Bestellung an den **Kitchen Service** (asynchron via Message Broker).
- **Herausforderung:** Muss dem Kunden sofort Feedback geben, auch wenn die Küche gerade überlastet ist.

### Rolle 2: Payment Service

Ein kritischer, synchroner Service.

- **Aufgabe:** Bereitstellung einer REST-Schnittstelle (`POST /pay`), die eine Zahlung verarbeitet.
- **Verantwortung:**
  - Entscheidet (simuliert), ob die Zahlung erfolgreich ist oder abgelehnt wird.
  - Sollte künstlich eine kleine Verzögerung oder gelegentliche Fehler simulieren, um die Robustheit des Order Services zu testen.
- **Herausforderung:** Muss hochverfügbar sein oder der Order Service muss den Ausfall sauber abfangen.

### Rolle 3: Kitchen Service

Der asynchrone Verarbeiter.

- **Aufgabe:** Konsumiert Bestellungen aus einer Message Queue (z. B. RabbitMQ).
- **Verantwortung:**
  - Simuliert die Zubereitungszeit (z. B. `sleep` für 5–10 Sekunden).
  - Nach Abschluss der Zubereitung: Veröffentlicht ein Event (`order.ready`), dass das Essen fertig ist.
- **Herausforderung:** Muss skalierbar sein. Wenn man 3 Kitchen-Instanzen startet, müssen sie sich die Arbeit teilen (Competing Consumers Pattern).

### Rolle 4: Delivery & Notification Service

Der reagierende Service.

- **Aufgabe:** Hört auf Events vom Kitchen Service (`order.ready`).
- **Verantwortung:**
  - Simuliert die Zuweisung eines Fahrers.
  - Bietet eine Schnittstelle (REST oder Log-Output), um den Status einer Bestellung einzusehen.
  - Sendet (simuliert) eine Benachrichtigung an den Kunden ("Dein Essen kommt!").
- **Herausforderung:** Darf die Events nicht verpassen und muss unabhängig vom Order Service laufen.

---

## 2\. Technische Anforderungen

Damit die Kriterien für LB2 erfüllt sind, müssen folgende Regeln beachtet werden:

1.  Contract First: Bevor programmiert wird, einigt ihr euch auf die Schnittstellen.
    - Wie sieht das JSON für den REST-Call aus?
    - Wie sieht das JSON-Event in der Message Queue aus?
2.  Lose Kopplung:
    - Der **Kitchen Service** darf den **Order Service** technisch nicht kennen. Er kennt nur die Queue.
    - Der **Delivery Service** darf die **Kitchen** nicht direkt aufrufen. Er reagiert nur auf Events.
3.  Resilience (Fehlerbehandlung):
    - **Szenario A (Synchroner Fehler):** Wenn der _Payment Service_ offline ist, darf der _Order Service_ nicht abstürzen. Er muss dem User eine freundliche Fehlermeldung geben (z. B. "Zahlungssystem antwortet nicht, bitte später versuchen").
    - **Szenario B (Asynchroner Puffer):** Wenn der _Kitchen Service_ offline ist, müssen Bestellungen im _Order Service_ trotzdem angenommen werden. Sie landen in der Queue und werden verarbeitet, sobald die Küche wieder online ist (Durable Queues).

---

## 3\. Umsetzung der Hochverfügbarkeit (HA)

Für die maximale Punktzahl solltet ihr folgende Szenarien demonstrieren können:

- **Pufferung:** Stoppt den _Kitchen Service_. Sendet 5 Bestellungen ab. Startet den _Kitchen Service_ wieder -> Alle 5 Pizzen müssen jetzt automatisch gebacken werden.
- **Skalierung:** Startet den _Kitchen Service_ in 2 Instanzen. Sendet viele Bestellungen. Zeigt in den Logs, dass beide Instanzen abwechselnd arbeiten (Lastverteilung).

---

## 4\. Nächste Schritte für die Gruppe

- **Rollenwahl:** Wer macht welchen Service? (Jeder ist für seinen Code & sein Dockerfile verantwortlich) .
- **Schnittstellen-Meeting:** Definiert gemeinsam die JSON-Struktur für `Order` und `Payment`. Dokumentiert dies (z. B. kurzes Swagger-File oder Markdown-Tabelle).
- **Setup:** Erstellt ein gemeinsames `docker-compose.yml`, in dem der Message Broker (z. B. RabbitMQ) definiert ist, damit jeder lokal entwickeln kann.

---

Hier ist eine Zusammenfassung der relevanten Modulinhalte und deren direkte Anwendung auf Ihre spezifische Architektur (Order-, Payment-, Kitchen- und Delivery-Service).

### Teil 1: Zusammenfassung des Moduls "Verteilte Systeme"

Das Modul behandelt den Übergang von Monolithen zu verteilten Systemen (Microservices) und die damit verbundenen Herausforderungen in Kommunikation, Datenhaltung, Sicherheit und Betrieb.

**1. Architektur & Design**

- **Vom Monolith zu Microservices:** Monolithen vereinen alle Funktionen in einer Einheit. Verteilte Systeme teilen diese in autonome Services auf, um Skalierbarkeit und Flexibilität zu erhöhen.
- **Schnittstellen-Design (Vertical Slicing):** Services sollten vertikal nach Fachlichkeit (Features) geschnitten werden (z. B. Order, Payment), inklusive eigener Datenhaltung und Business-Logik, statt horizontal nach technischen Schichten.
- **Kontrakte:** Schnittstellen müssen klar definiert sein (z. B. via OpenAPI für REST), um Änderungen ohne "Breaking Changes" zu ermöglichen.

**2. Kommunikation**

- **Synchron (REST/gRPC):** Der Aufrufer wartet auf eine Antwort. Gut für direkte Abfragen, erzeugt aber eine enge zeitliche Kopplung.
- **Asynchron (Messaging):** Nutzung eines Message Brokers (z. B. RabbitMQ, Kafka) zur Entkopplung. Der Sender (Publisher) muss den Empfänger (Subscriber) nicht kennen. Dies erhöht die Resilienz: Fällt ein Consumer aus, bleibt die Nachricht in der Queue.

**3. Datenhaltung & Konsistenz**

- **Eigene Persistenz:** Jeder Service sollte seine eigene Datenbank besitzen, um Unabhängigkeit zu wahren.
- **CAP-Theorem:** In verteilten Systemen muss man oft zwischen Verfügbarkeit (Availability) und strenger Konsistenz (Consistency) abwägen, wenn Partitionen (Netzwerkausfälle) auftreten.
- **Transaktionen:** Klassische ACID-Transaktionen über mehrere Services hinweg sind schwierig. Es gilt "Eventual Consistency" (die Daten werden _schließlich_ konsistent).

**4. Sicherheit**

- **IAM & Authentifizierung:** Zentrale Identitätsverwaltung (Identity Provider) mittels Protokollen wie OAuth 2.0 und OpenID Connect (OIDC).
- **Tokens:** Nutzung von JWT (JSON Web Tokens) zur Weitergabe von Identität und Berechtigungen zwischen Services.

**5. Betrieb & Observability**

- **Container & Orchestrierung:** Einsatz von Docker und Kubernetes zur Verwaltung und Skalierung der Services.
- **Monitoring & Tracing:** Da Fehler schwerer zu finden sind, benötigt man zentrales Logging (z. B. ELK-Stack) und Distributed Tracing (z. B. Jaeger), um einen Request über Service-Grenzen hinweg zu verfolgen.

---

### Teil 2: Anwendung auf Ihr Architektur-Diagramm

Hier sehen Sie, wie die Theorie konkret auf Ihren **Order -> Payment / Kitchen / Delivery** Graphen anzuwenden ist:

#### 1. Der synchrone Pfad: Order Service (OS) → Payment Service (PS)

Im Diagramm kommunizieren OS und PS synchron via `REST POST /pay`.

- **Warum synchron?** Bei Zahlungen will der Nutzer (und der Order Service) sofort wissen, ob die Transaktion erfolgreich war, bevor die Bestellung bestätigt wird.
- **Anwendbare Konzepte:**
  - **API-Kontrakt:** Definieren Sie die Schnittstelle mit einer **OpenAPI Spec**. Dies stellt sicher, dass OS genau weiß, wie die Zahlungsdaten an PS zu senden sind.
  - **Sicherheit:** Der Order Service sollte sich gegenüber dem Payment Service authentifizieren. Hierbei ist **OAuth 2.0** (Machine-to-Machine) sinnvoll, wobei der OS ein Access Token (z. B. **JWT**) mitsendet, das PS validiert.
  - **Fehlerbehandlung:** Da die Kommunikation synchron ist, muss OS mit Timeouts umgehen können, falls PS nicht antwortet.

#### 2. Der asynchrone Pfad: Order Service (OS) → Message Broker (MB)

Nach erfolgreicher Zahlung sendet OS das Event `order.placed` an den Broker.

- **Entkopplung:** Der OS muss nicht wissen, wer die Nachricht konsumiert. Er legt die Nachricht "fire and forget" in den Broker (Queue) und kann dem Kunden sofort "Bestellung erfolgreich" anzeigen.
- **Resilienz:** Wenn der **Kitchen Service (KS)** gerade neu gestartet wird oder überlastet ist, geht die Bestellung nicht verloren. Sie bleibt im **Message Broker** (in der Queue persistiert), bis KS wieder verfügbar ist. Dies garantiert "At-Least-Once Delivery".

#### 3. Verarbeitung: Kitchen Service (KS) und Delivery Service (DS)

KS konsumiert `order.placed`, kocht und publiziert `order.ready`. DS konsumiert `order.ready`.

- **Pub/Sub Pattern:** Der Message Broker fungiert als Vermittler. KS ist Publisher für `order.ready` und DS ist Subscriber.
- **Skalierung:** Wenn zur Mittagszeit hunderte Bestellungen (`order.placed`) eingehen, können Sie den **Kitchen Service horizontal skalieren** (mehrere Instanzen starten). Der Message Broker verteilt die Nachrichten via Round-Robin oder Load Balancing auf die verschiedenen KS-Instanzen, sodass jede Bestellung nur einmal gekocht wird.
- **Datenhaltung:** KS hat vermutlich eine eigene Datenbank für Rezepte und aktuelle Kochaufträge, völlig getrennt von den Bestelldaten im OS.

#### 4. Betrieb & Monitoring (Das "Ganze" zusammenhalten)

Da ein einziger User-Request (Bestellung aufgeben) nun durch vier Systeme fließt (OS -> PS -> MB -> KS ...), ist Debugging schwierig.

- **Distributed Tracing:** Sie sollten eine **Trace ID** (z. B. via Jaeger) generieren, sobald der Request im OS eingeht. Diese ID muss an den PS (im HTTP Header) und in die Messages an den Broker weitergegeben werden. So können Sie später in einer Visualisierung sehen, wie lange das Payment dauerte und wie lange die Bestellung in der Queue lag.
- **Container:** Jeder dieser Services (OS, PS, KS, DS) läuft idealerweise in einem eigenen **Pod** in einem **Kubernetes Cluster**, definiert durch Declarative Infrastructure (YAML).

---

**Analogie zum besseren Verständnis:**
Stellen Sie sich Ihr System wie ein **Restaurant** vor.

- **OS -> PS (Synchron):** Das ist der Kellner, der mit dem Kartenlesegerät am Tisch steht. Er wartet direkt neben dem Gast, bis die Zahlung "Ok" ist. Er geht nicht weg, bevor das erledigt ist (Blockierend).
- **OS -> Küche (Asynchron):** Sobald bezahlt ist, ruft der Kellner die Bestellung nicht direkt dem Koch ins Ohr (das wäre synchrone Kopplung und würde den Koch stören). Stattdessen pinnt er den Bon an eine **Leiste (Message Broker)**.
- **Kitchen Service:** Der Koch nimmt den Bon, wann er Zeit hat (Entkopplung). Fällt der Koch kurz aus (Pause/Absturz), bleibt der Bon an der Leiste hängen (Resilienz), bis ein anderer Koch übernimmt.
- **Delivery Service:** Wenn das Essen fertig ist, stellt der Koch es auf den Tresen und drückt eine Klingel (`order.ready`). Der Lieferant (DS) reagiert auf die Klingel und holt das Essen ab.
