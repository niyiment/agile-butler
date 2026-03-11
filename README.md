# Agile Butler

**Your team's intelligent assistant for async updates and real-time decisions.**

Agile Butler is a Spring Boot application designed to solve the two biggest challenges for remote and hybrid teams: streamlining asynchronous coordination and facilitating rapid, real-time collective decision-making.

---

## 🚀 Key Features

### 📅 Async Daily Standups
*   **Timezone-Aware Reminders**: Automated push notifications at customized times for each team member.
*   **Structured Updates**: Standardized format (Yesterday/Today/Blockers) with support for links and image attachments.
*   **Auto-Aggregation**: Backend logic automatically aggregates team updates at the deadline, flagging blockers prominently for Scrum Masters.
*   **Manager Dashboard**: A clean overview of team participation and highlighted impediments.

### 🗳️ Real-Time Decision Sessions
*   **Urgent Resolution**: Instantly initiate a decision session from a standup blocker or a technical proposal.
*   **Multiple Voting Modes**: Supports Single Choice, Yes/No, and Ranked Choice (Borda Count) voting.
*   **Live Collaboration**: Powered by WebSockets for real-time vote counting, presence tracking, and live discussion threads.
*   **Timed Polls**: Support for async "Timed Polls" (e.g., open for 24-48 hours) for less urgent decisions.

### 📄 Professional Reporting
*   **Automated Exports**: Generate high-quality PDF or Markdown reports of closed decision sessions.
*   **iText7 Integration**: Professional styling, result visualization, and decision records ready for team sharing or audit logs.

### 🔔 Unified Notification Center
*   **Cross-Channel Alerts**: Integration with Firebase Cloud Messaging (FCM) for mobile push and WebSocket for real-time in-app alerts.
*   **Event-Driven Architecture**: Clean decoupling using Spring's internal event bus.

---

## 🛠️ Technology Stack

*   **Backend**: Spring Boot 4.0.3 (Java 21)
*   **Messaging**: Spring Application Events (Async internal event bus)
*   **Real-time**: WebSocket with STOMP & SockJS
*   **Database**: PostgreSQL
*   **Migrations**: Flyway
*   **Caching**: Redis (Rate limiting & Result caching)
*   **Reporting**: iText7 & html2pdf
*   **Security**: JWT (Stateless) & Spring Security
*   **Documentation**: SpringDoc OpenAPI (Swagger UI)
*   **Build Tool**: Maven

---

## 🚦 Getting Started

### Prerequisites
*   Java 21 or higher
*   Docker & Docker Compose
*   A Firebase Service Account (for push notifications - optional for local dev)

### Installation
1.  **Clone the repository**:
    ```bash
    git clone <repository-url>
    cd agile-butler
    ```

2.  **Start Infrastructure**:
    The project uses Docker Compose to automatically provision PostgreSQL and Redis.
    ```bash
    docker-compose up -d
    ```

3.  **Run the application**:
    ```bash
    ./mvnw spring-boot:run
    ```

### Configuration
Key settings can be modified in `src/main/resources/application.yaml` or via environment variables:
*   `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`
*   `REDIS_HOST`, `REDIS_PORT`
*   `JWT_SECRET`
*   `FIREBASE_CREDENTIALS` (path to your `firebase-service-account.json`)

---

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation at:
*   **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 🏗️ Architecture Overview

The project follows a **Layered Clean Architecture** pattern:
*   `com.niyiment.agilebutler.user`: Identity and profile management.
*   `com.niyiment.agilebutler.team`: Team structure and invite system.
*   `com.niyiment.agilebutler.standup`: Async standup logic and aggregation.
*   `com.niyiment.agilebutler.decision`: Real-time and async decision engine.
*   `com.niyiment.agilebutler.notification`: Multi-channel notification dispatch.
*   `com.niyiment.agilebutler.common`: Shared configurations, filters (Rate limiting), and utilities.

---

## 📄 License
This project is licensed under the MIT License.

---
*Agile Butler - Synchronizing the rhythm of your agile team.*