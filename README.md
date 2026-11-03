# Tic Tac Toe Tournament

A full-stack Tic Tac Toe tournament application built with **Angular 22** and **Java Spring Boot 4.1.2**.

The application allows two players to compete in a series of Tic Tac Toe games using Best of 1, Best of 3, or Best of 5 tournament formats. It also provides tournament history and a player leaderboard.

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
- Java 25
- Spring Boot 4.1.2
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- JUnit 5 and Mockito

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

| Tool | Required version |
|---|---|
| Node.js | 24.22.2 |
| npm | 12.2.1 |
| Angular CLI | 22.2.5 |
| Java JDK | 25 |
| Spring Boot | 4.1.2 |
| PostgreSQL | 13 or later |

Maven is included through the Maven Wrapper, so a separate Maven installation is not required.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Halit-Lala/tic-tac-toe-tournament.git
cd tic-tac-toe-tournament
```

### 2. Configure the database

Make sure PostgreSQL is running and create a database for the application:

```sql
CREATE DATABASE tic_tac_toe_tournament;
```

Configure the datasource in the backend's `application.properties` (or through the corresponding environment variables):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tic_tac_toe_tournament
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Flyway automatically applies the database schema migrations when the backend starts.

### 3. Run the backend

Open a terminal in the `backend` directory.

**Windows:**

```bash
mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

The backend starts at:

http://localhost:8080

### 4. Run the frontend

Open another terminal in the `frontend` directory.

Install dependencies:

```bash
npm install
```

Start the Angular development server:

```bash
npm start
```

Alternatively:

```bash
ng serve
```

The frontend is available at:

http://localhost:4200

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
- Validate player names and prevent duplicate names (case-insensitive).
- Autocomplete player names using previously registered players.

### Tournament Gameplay
- Play Tic Tac Toe on a shared screen.
- Alternate turns between players.
- Automatically detect wins and draws on the backend.
- Reset the board between games.
- Alternate the starting player between games.
- Display the current score and tournament progress.
- Prevent leaving an unfinished tournament without confirmation.
- Persist tournament and game state in the database.

### Tournament Results and History
- Display the final tournament outcome.
- Show the results of individual games.
- Track player wins and draws.
- Browse completed tournaments through a paginated history.
- Display tournament creation date and time.
- Navigate from a history entry to its tournament results.

### Leaderboard
- Rank players by tournament wins.
- Display tournaments played and win rate.
- Support pagination.

## Architecture and Design Decisions

### Backend as the Source of Truth

The backend owns the game and tournament business logic.

It is responsible for:
- Validating moves and player turns.
- Preventing invalid moves and operations after completion.
- Detecting wins and draws.
- Managing game and tournament states.
- Calculating tournament results and leaderboard statistics.

The frontend renders the state returned by the backend rather than independently determining game outcomes. This avoids duplicating business logic across the applications and ensures that the server remains authoritative.

### Separation of Responsibilities

The backend separates controllers, services, repositories, entities, DTOs, mappers, and exception handling.

- **Controllers** expose REST endpoints and handle HTTP requests.
- **Services** coordinate application operations and business workflows.
- **Repositories** handle database access through Spring Data JPA.
- **Entities** represent the persisted domain model.
- **DTOs and mappers** separate API contracts from database entities.
- **GameEngine** contains the core Tic Tac Toe game logic independently of the Spring application layer.
- **GlobalExceptionHandler** centralizes exception handling and returns consistent ProblemDetail responses with appropriate HTTP status codes.

The frontend follows a feature-based structure, separating components, models, API services, and state management.

API services are responsible for HTTP communication, while the tournament store coordinates API operations and exposes application state through Angular Signals.

### State Management

Angular Signals are used to expose and manage frontend state, including the current tournament, game state, loading indicators, and error states.

RxJS is used to orchestrate asynchronous operations and HTTP requests. Keeping API communication in dedicated services and state coordination in the store helps separate responsibilities and keeps components focused on presentation and user interactions.

### Reactive Programming

RxJS is used for asynchronous operations and request handling.

Examples include:
- Debounced player autocomplete.
- Cancelling stale autocomplete requests with `switchMap`.
- Preventing duplicate tournament operations with `exhaustMap`.
- Managing loading and error states.
- Cleaning up subscriptions with `takeUntilDestroyed`.

Reactive Forms with FormBuilder are used for tournament setup and form validation.

### API Design and Error Handling

The backend exposes REST endpoints using appropriate HTTP methods and status codes.

Request and response DTOs keep the API independent of the persistence model.

Centralized exception handling provides consistent error responses:
- `400 Bad Request` for invalid input.
- `404 Not Found` for missing resources.
- `409 Conflict` for invalid game or tournament state and concurrent updates.
- `500 Internal Server Error` for unexpected errors.

The frontend uses a centralized HTTP interceptor to handle API errors and display appropriate notifications.

### Database Migrations and Persistence

PostgreSQL is used for persistent storage, with Spring Data JPA for database access.

Flyway manages database schema changes through versioned migrations, keeping the database structure consistent across environments.

Tournament and game creation timestamps are persisted to support chronological tournament history.

### Pagination

Tournament history and leaderboard data are paginated on the backend.

This avoids loading all records into the frontend and allows the application to handle larger datasets more efficiently.

## Running Tests

### Backend

Run all tests without starting the application:

**Windows:**

```bash
mvnw.cmd test
```

**Linux / macOS:**

```bash
./mvnw test
```

Run a single test class:

```bash
mvnw.cmd test -Dtest=GameEngineTest
```

The backend tests cover game engine logic, tournament behavior, service functionality, and web-layer behavior.

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

Frontend tests cover form validation, state management, and component behavior.

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/tournaments` | Create a tournament |
| GET | `/api/tournaments` | Retrieve paginated tournament history |
| GET | `/api/tournaments/{id}` | Retrieve tournament details |
| POST | `/api/tournaments/{id}/games/{gameNumber}/moves` | Submit a move |
| POST | `/api/tournaments/{id}/games/next` | Start the next game |
| GET | `/api/players?search=` | Search players |
| GET | `/api/leaderboard?page=&size=` | Retrieve paginated leaderboard |

## Assumptions

- Tournament formats represent Best of 1, Best of 3, and Best of 5 games.
- The tournament finishes early when one player has secured more wins than the opponent can still achieve.
- Draws count towards the total number of games but do not contribute to either player's wins.
- Player names are considered identical when they match after trimming whitespace and ignoring letter case.
- The player selecting X starts the first game. The starting symbol alternates between X and O in subsequent games.
- Tournament history displays completed tournaments, ordered by creation date, newest first.
- Existing tournaments created before the introduction of the `createdAt` field receive a timestamp when the database migration runs. Their original creation dates cannot be recovered.
- Both players share the same screen and take turns on the same device. User accounts and authentication are outside the scope of the assignment.

## Possible Improvements

With more time, I would consider:

1. Player Profiles: Add profiles with match history, tournament records, win rates, and personal bests.
2. Real-Time Multiplayer: Support games across different browsers and devices with live updates using WebSockets.
3. Enhanced UI/UX: Improve the interface with animations, responsive layouts, and clearer gameplay feedback.
4. Tournament Management: Support more players and additional formats, such as single-elimination and round-robin.
5. Player Authentication: Add user registration, login, and persistent player profiles and history.
6. Advanced Statistics: Expand leaderboards with rankings, performance trends, head-to-head records, and filters.

## AI Tools

I used **ChatGPT** and **Claude** as development assistants throughout the project.

They were mainly used for:
- Creating and reviewing test cases for the backend and frontend.
- Assisting with the preparation and structure of this README.
- General coding support, discussing implementation approaches, and troubleshooting issues.
- Helping speed up frontend development, particularly when working on HTML templates and their structure.

AI-generated suggestions and code were reviewed and adapted to the project. I made the final implementation decisions and can explain the architecture, code, and technical choices made throughout the application.

## Additional Notes

- The backend must be running and connected to PostgreSQL before using the frontend.
- The Angular development proxy is intended for local development.
- Database connection details should be configured locally and should not be committed with real credentials.
- No Docker setup is required; both applications and PostgreSQL are run locally.