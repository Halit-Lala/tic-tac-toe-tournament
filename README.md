# Tic Tac Toe Tournament

A full-stack Tic Tac Toe tournament application built with **Angular 22** and **Java Spring Boot 4.1.2**.

The application allows two players to compete in a series of Tic Tac Toe games using Best of 1, Best of 3, or Best of 5 tournament formats. It also provides player leaderboard.

## Tech Stack

### Frontend
- Angular 22
- TypeScript
- Angular Material
- Angular Signals and RxJS
- Reactive Forms
- Vitest
- SCSS

### Backend
- Java
- Spring Boot 4.1.2
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- JUnit and Mockito

## Project Structure

The repository contains both applications:

```text
tic-tac-toe-tournament/
├── frontend/
│   └── Angular application
│
├── backend/
│   └── Spring Boot application
│
└── README.md
```

## Prerequisites

Make sure the following tools are installed:

- Node.js 24+
- npm 12+
- Java 21+
- Maven
- PostgreSQL 13+

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Halit-Lala/tic-tac-toe-tournament.git
cd tic-tac-toe-tournament
```

### 2. Configure the database

Create a PostgreSQL database for the application.

Configure the datasource in the backend's `application.properties` (or the corresponding environment variables):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tic_tac_toe_tournament
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Flyway handles database schema migrations when the backend starts.

### 3. Run the backend

Open a terminal in the `backend` directory:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend starts at:

```text
http://localhost:8080
```

### 4. Run the frontend

Open another terminal in the `frontend` directory:

```bash
npm install
npm start
```

Or run Angular directly:

```bash
ng serve
```

The frontend is available at:

```text
http://localhost:4200
```

## API Communication

The frontend communicates with the backend through REST API endpoints.

During local development, the Angular development server proxy forwards requests matching `/api/**` to:

```text
http://localhost:8080
```

This allows the frontend to use relative API URLs and avoids requiring backend CORS configuration for local development.

The proxy configuration is located at:

```text
frontend/src/proxy.conf.json
```

## Main Features

### Tournament Setup
- Create a tournament with two players.
- Select Best of 1, Best of 3, or Best of 5.
- Validate player names and prevent duplicate names.
- Autocomplete player names using previously registered players.

### Tournament Gameplay
- Play Tic Tac Toe on a shared screen.
- Alternate turns between players.
- Automatically detect wins and draws on the backend.
- Reset the board between games.
- Alternate the starting player between games.
- Display the current score and tournament progress.
- Prevent leaving an unfinished tournament without confirmation.

### Tournament Results
- Display the final tournament outcome.
- Show the results of individual games.
- Track player wins and draws.

### Leaderboard
- Rank players by tournament wins.
- Display tournaments played and win rate.
- Support pagination.

## Architecture and Design Decisions

### Backend as the Source of Truth

The backend owns the game and tournament business logic.

It is responsible for:
- Validating moves.
- Managing player turns.
- Detecting wins and draws.
- Updating game and tournament states.
- Calculating tournament results and leaderboard statistics.

The frontend renders the state returned by the backend rather than independently determining game outcomes. This prevents business logic from being duplicated across the applications.

### Separation of Responsibilities

The backend separates controllers, services, repositories, DTOs, entities, and exception handling.

The frontend uses a feature-based structure, separating components, models, API services, and state management.

API services are responsible for HTTP communication, while the tournament state service coordinates API operations and exposes application state through Angular Signals.

### Reactive Programming

RxJS is used for asynchronous operations and request handling.

Examples include:
- Debounced player autocomplete.
- Cancelling stale autocomplete requests with `switchMap`.
- Preventing duplicate tournament operations with `exhaustMap`.
- Managing loading and error states.
- Cleaning up subscriptions with `takeUntilDestroyed`.

### Error Handling

The backend uses centralized exception handling to return consistent API error responses.

The frontend uses a centralized HTTP interceptor to handle API errors and display appropriate notifications.

### Database Migrations

Flyway manages database schema changes through versioned migrations, keeping the database structure consistent across environments.

## Running Tests

### Backend

Run all tests without starting the application:

```bash
./mvnw test
```

On Windows:

```bash
mvnw.cmd test
```

Run a single test class:

```bash
./mvnw test -Dtest=GameEngineTest
```

### Frontend

Run all tests:

```bash
npm test
```

Run tests once without watch mode:

```bash
npx ng test --watch=false
```

Run a specific test file:

```bash
npx ng test --include="src/app/features/setup/validators/name-validators.spec.ts" --watch=false
```

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/tournaments` | Create a tournament |
| GET | `/api/tournaments` | Retrieve tournament history |
| GET | `/api/tournaments/{id}` | Retrieve tournament details |
| POST | `/api/tournaments/{id}/games/{gameNumber}/moves` | Submit a move |
| POST | `/api/tournaments/{id}/games/next` | Start the next game |
| GET | `/api/players?search=` | Search players |
| GET | `/api/leaderboard?page=&size=` | Retrieve paginated leaderboard |

## Additional Notes

- The backend must be running and connected to PostgreSQL before using the frontend.
- The Angular development proxy is intended for local development.
- Database connection details should be configured locally and should not be committed with real credentials.