```markdown
# Leaderboard Service

A simple, extensible leaderboard backend built with Spring Boot and PostgreSQL.  
This repo implements three core APIs:

- Submit Score: record a game session and update leaderboard totals & ranks
- Get Leaderboard: return top players by total score
- Get Player Rank: return a specific player's total and rank

This README explains the API, database schema, indexes, relations, setup steps (IntelliJ + PostgreSQL), Postman usage, design rationale, and FAQs.

---

## Table of contents

- Project description
- APIs (documentation + examples)
- Postman (import & usage)
- Database schema, relations, indexes, definitions and usage
- ER Diagram
- Setup & run (IntelliJ + PostgreSQL)
- Why PostgreSQL?
- Design rationale (indexes, API choices)
- FAQs

---

## Project description

The leaderboard service captures game sessions and aggregates scores per user to compute rankings. It is intended as a small backend that demonstrates:

- recording per-session scores
- computing cumulative totals per user
- exposing leaderboard and per-user rank endpoints
- storing data in PostgreSQL with sensible indexes for read/write patterns

This base can be extended with features such as multiple game modes, time-decayed leaderboards, caching, sharding, and eventual consistency strategies for high throughput.

---

## APIs

Base path: /api/leaderboard

All requests and responses use JSON.

1) Submit Score
- Method: POST
- Path: /api/leaderboard/submit
- Body:
  {
    "userId": 1,
    "score": 150
  }
- Behavior:
  - Inserts a new row into game_sessions for the user and score.
  - Recomputes total scores (sum of all game_sessions per user) and updates/creates entries in leaderboard.
  - Recomputes ranks (dense ranking: equal totals share the same rank).
- Success response: 200 OK
  {
    "userId": 1,
    "username": "alice",
    "totalScore": 350,
    "rank": 2
  }
- Errors:
  - 400 Bad Request if missing fields
  - 404/400 if user not found (depends on implementation)

2) Get Leaderboard (Top N, default 10)
- Method: GET
- Path: /api/leaderboard/top
- Query params: none (implementation returns top 10)
- Success response: 200 OK
  [
    { "userId": 3, "username": "carol", "totalScore": 900, "rank": 1 },
    { "userId": 1, "username": "alice", "totalScore": 350, "rank": 2 },
    ...
  ]

3) Get Player Rank
- Method: GET
- Path: /api/leaderboard/rank/{userId}
- Path param: userId
- Success response: 200 OK
  {
    "userId": 1,
    "username": "alice",
    "totalScore": 350,
    "rank": 2
  }
- If the player has no scores yet, totalScore may be 0 and rank may be null.

---

## Postman

Files included:
- postman_collection.json (import this collection into Postman)

How to use:
1. Import `postman_collection.json` (File → Import → choose the JSON file).
2. The collection includes variables:
   - baseUrl (default: http://localhost:8080)
   - userId (default: 1)
3. If you prefer environments, create a Postman environment with `baseUrl` and `userId` and select it.
4. Typical workflow:
   - Create user (via SQL or an optional user endpoint if you add one).
   - Update collection variable `userId` to the created user's ID.
   - Use "Submit Score" to add a session.
   - Use "Get Top 10" and "Get Player Rank" to view results.

The `postman_collection.json` file is included in the repo root for easy import.

---

## Database schema

Use the `schema.sql` file provided in the repo to create tables and optional seed data.

Tables (PostgreSQL types shown):

1) users
- id        | integer | not null | default nextval('users_id_seq'::regclass)
- username  | character varying(255) | not null | UNIQUE
- join_date | timestamp without time zone | default CURRENT_TIMESTAMP

Indexes & constraints:
- PRIMARY KEY: users_pkey (id)
- UNIQUE: users_username_key (username)
- Referenced by game_sessions.user_id and leaderboard.user_id (ON DELETE CASCADE)

2) game_sessions
- id        | integer | not null | default nextval('game_sessions_id_seq'::regclass)
- user_id   | integer | references users(id) ON DELETE CASCADE
- score     | integer | not null
- game_mode | character varying(50) | not null
- timestamp | timestamp without time zone | default CURRENT_TIMESTAMP

Indexes:
- PRIMARY KEY: game_sessions_pkey (id)
- idx_game_sessions_timestamp: btree (timestamp)
- idx_game_sessions_user_id: btree (user_id)

Usage:
- Records every played session (append-only).
- Aggregated (SUM(score) grouped by user_id) to compute leaderboard totals.
- Indexed on user_id for efficient per-user lookups and timestamp for queries such as recent sessions.

3) leaderboard
- id         | integer | primary key
- user_id    | integer | references users(id) ON DELETE CASCADE, UNIQUE
- total_score| integer | not null
- rank       | integer | nullable

Indexes & constraints:
- PRIMARY KEY: leaderboard.id
- UNIQUE: leaderboard_user_id (user_id) — one leaderboard row per user
- Foreign key to users ensures cleanup when a user is deleted.

Usage:
- Stores aggregated totals and computed rank for quick reads (top N or user rank).
- Kept in sync by recalculating from game_sessions or incrementally updating after each session.

Notes on the provided table dumps:
- The README mirrors the table layouts you showed (Postgres `\d` style).
- Use the schema.sql to create identical tables.

---

## ER Diagram

Below is an ASCII ER diagram showing tables and relationships.

  +----------------+         +------------------+         +-------------+
  |     users      | 1     N |   game_sessions  |    1  1 | leaderboard |
  |----------------|---------|------------------|---------|-------------|
  | id (PK)        |<---+    | id (PK)          |         | id (PK)     |
  | username (U)   |    +--< | user_id (FK)     |         | user_id (FK, U) |
  | join_date      |         | score             |         | total_score |
  +----------------+         | game_mode         |         | rank        |
                             | timestamp         |         +-------------+
                             +-------------------+

Meaning:
- users.id is PK; username is UNIQUE.
- game_sessions.user_id → users.id (many sessions per user).
- leaderboard.user_id → users.id (one leaderboard entry per user).
- Deletion cascade: removing a user deletes their sessions and leaderboard entry.

(If you prefer a mermaid diagram, add the following in Markdown-enabled viewers that support mermaid:)

```mermaid
erDiagram
    USERS ||--o{ GAME_SESSIONS : has
    USERS ||--|| LEADERBOARD : "has (1:1)"
    USERS {
        integer id PK
        varchar username U
        timestamp join_date
    }
    GAME_SESSIONS {
        integer id PK
        integer user_id FK
        integer score
        varchar game_mode
        timestamp timestamp
    }
    LEADERBOARD {
        integer id PK
        integer user_id FK U
        integer total_score
        integer rank
    }
```

---

## Indexes chosen — rationale

- users.username UNIQUE:
    - Provide fast lookups by username (if later you provide login/lookup endpoints).
    - Enforce uniqueness at DB level.

- game_sessions.user_id btree:
    - Frequent access pattern: fetch sessions for a user or aggregate by user — this index speeds those queries.
    - Also used when deleting or joining on user.

- game_sessions.timestamp btree:
    - Useful for queries like "recent sessions", time-windowed leaderboards, auditing, or pruning old sessions.

- leaderboard.user_id UNIQUE:
    - Ensures only one aggregated row per user (fast lookup for per-user rank/score).

Why these indexes:
- They match the current read/write patterns: frequent writes to game_sessions, frequent reads of aggregated totals and top-N queries from leaderboard. Avoid adding indexes on frequently-written columns that are not used by queries to limit write overhead.

---

## Setup (IntelliJ + PostgreSQL)

Prerequisites
- Java 17+
- Maven
- PostgreSQL (12+ recommended)
- IntelliJ IDEA

Steps
1. Clone the repo into your machine.
2. Create PostgreSQL database:
    - Example:
        - createdb leaderboard_db
        - psql -d leaderboard_db -c "CREATE USER leaderboard_user WITH PASSWORD 'securepassword';"
        - psql -d leaderboard_db -c "GRANT ALL PRIVILEGES ON DATABASE leaderboard_db TO leaderboard_user;"
3. Run SQL schema:
    - psql -d leaderboard_db -f schema.sql
    - Or rely on Hibernate in dev by setting spring.jpa.hibernate.ddl-auto=create (not recommended for production).
4. Configure application.properties:
    - Update spring.datasource.url, username, and password to match your DB.
5. Import the project into IntelliJ as a Maven project.
6. Run the application: main class com.example.leaderboard.LeaderboardApplication
7. Create some users (via SQL or add a user endpoint):
    - Example SQL:
      INSERT INTO users (username) VALUES ('alice'), ('bob'), ('carol');
8. Use Postman (import postman_collection.json) to call the endpoints.

Sample curl:
- Submit score:
  curl -X POST -H "Content-Type: application/json" -d '{"userId":1,"score":200}' http://localhost:8080/api/leaderboard/submit

- Get top:
  curl http://localhost:8080/api/leaderboard/top

- Get rank:
  curl http://localhost:8080/api/leaderboard/rank/1

---

## Why PostgreSQL?

- Reliable, ACID-compliant relational database with strong support for transactions and integrity constraints (foreign keys, unique constraints).
- Powerful SQL: window functions (rank/dense_rank), aggregates, and CTEs make it straightforward to compute leaderboard rankings efficiently in SQL if you choose that path.
- Mature ecosystem, great tooling, and good performance for moderate loads.
- If later you need to scale read traffic, you can add read replicas, or use caching (Redis) for top-N queries.

---

## Design rationale & notes

- Recalculation strategy (current implementation):
    - On every submit, the service recalculates totals by aggregating game_sessions and updates the leaderboard table, then recomputes dense ranks.
    - Pros: correct, simple, easy to reason about.
    - Cons: full aggregation each write is O(number of users) and could be costly at scale.

- Incremental strategy (alternative) — recommended for scale:
    - When inserting a session, update leaderboard.total_score = total_score + new_score for that user (UPSERT).
    - Use a database window function to compute rankings on-demand for top-N queries:
      SELECT user_id, total_score, dense_rank() OVER (ORDER BY total_score DESC) as rank FROM leaderboard;
    - Optionally compute/increment rank asynchronously if eventual consistency is acceptable.

- Ranking approach:
    - Current: dense ranking (1,2,2,3). You can change to standard SQL RANK() if you prefer gaps (1,2,2,4).

- Concurrency:
    - Use transactions when updating leaderboard values to avoid lost updates.
    - Use DB-level constraints and optimistic locking if required.

---

## Files in this repo

- README.md (this file)
- schema.sql (DB creation + optional seed)
- postman_collection.json (Postman collection for API testing)
- src/main/... (Spring Boot app code, models, controllers, services — provided earlier)

---

## FAQs (Interview revision style)

Q: How do you handle ties in ranking?
A: We use dense ranking in the example — tied scores receive the same rank and the next distinct score gets the next rank number. You can switch to SQL RANK() if you want gaps.

Q: Why store leaderboard separately if you can compute totals from game_sessions?
A: Storing leaderboard entries (aggregates) speeds reads (top-N and per-user rank) and avoids repeated heavy aggregations on read. For high ingestion, incrementally update leaderboard rather than full recalculation.

Q: How would you scale the leaderboard?
A: Options include:
- Incremental updates to leaderboard table.
- Use Redis sorted sets for very high-read, high-write scenarios (fast top-N and rank operations).
- Shard by user or game mode.
- Use background workers to compute expensive queries and cache results.

Q: What happens on user deletion?
A: ON DELETE CASCADE ensures game_sessions and leaderboard entries are removed when a user is deleted.

Q: How would you compute time-windowed leaderboards (e.g., weekly)?
A: Keep timestamps in game_sessions and either:
- maintain separate leaderboard aggregates per window, or
- compute using WHERE timestamp >= ... and aggregate on demand (with indexes on timestamp to help performance).

Q: Why index game_sessions.timestamp and game_sessions.user_id?
A: user_id speeds joins and per-user aggregations; timestamp speeds queries limited by time window (recent sessions).

---

## Next steps & suggestions

- Add a user creation API (I can add this quickly).
- Replace full recalculation with incremental updates + SQL window function for rank queries for better performance.
- Add tests, Docker Compose (Postgres + app), and CI pipeline (Newman runner for Postman collection).
- Add metrics (Prometheus) and health endpoints for production readiness.

---
```