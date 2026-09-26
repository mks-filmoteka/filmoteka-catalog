# Filmoteka Catalog

Backend REST API for Filmoteka, a movie catalog for managing films, actors and directors.

The service stores film metadata in PostgreSQL. Poster image files are handled outside this app; this service stores only the poster file name.

## Tech stack

- Java 25
- Spring Boot 4
- Spring Web, Validation and Actuator
- Spring Data JPA
- Spring Security with Keycloak JWT authentication
- Spring Kafka
- PostgreSQL
- Flyway
- MapStruct
- OpenAPI / Swagger
- JUnit, Mockito and Testcontainers

## Requirements

- JDK 25
- PostgreSQL
- Kafka
- Keycloak
- Docker, if you want to run the full test suite

## Run locally

PostgreSQL, Keycloak and Kafka can be started using the [shared Docker Compose setup](https://github.com/mks-filmoteka/filmoteka).

If you are not using the database from that setup, create it manually:

```sql
CREATE DATABASE filmoteka_catalog;
```

The `filmoteka` schema and tables are created by Flyway migrations on startup.

Set the environment variables. These examples match the default local Docker Compose settings; adjust credentials if you changed them:

```powershell
$env:DATASOURCE_URL = "jdbc:postgresql://localhost:5432/filmoteka_catalog"
$env:DATASOURCE_USERNAME = "filmoteka_catalog"
$env:DATASOURCE_PASSWORD = "filmoteka_catalog"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:AUTH_ISSUER_URI = "http://localhost:8180/realms/filmoteka"
$env:AUTH_JWK_SET_URI = "http://localhost:8180/realms/filmoteka/protocol/openid-connect/certs"
$env:AUTH_AUDIENCE = "filmoteka-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:KAFKA_FILM_DELETED_TOPIC = "filmoteka.film-deleted.v1"
$env:KAFKA_FILM_POSTER_CHANGED_TOPIC = "filmoteka.film-poster-changed.v1"
```

For bash:

```bash
export DATASOURCE_URL="jdbc:postgresql://localhost:5432/filmoteka_catalog"
export DATASOURCE_USERNAME="filmoteka_catalog"
export DATASOURCE_PASSWORD="filmoteka_catalog"
export CORS_ALLOWED_ORIGINS="http://localhost:5173"
export AUTH_ISSUER_URI="http://localhost:8180/realms/filmoteka"
export AUTH_JWK_SET_URI="http://localhost:8180/realms/filmoteka/protocol/openid-connect/certs"
export AUTH_AUDIENCE="filmoteka-api"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
export KAFKA_FILM_DELETED_TOPIC="filmoteka.film-deleted.v1"
export KAFKA_FILM_POSTER_CHANGED_TOPIC="filmoteka.film-poster-changed.v1"
```

For IDE runs, set these variables in the run configuration.

Start the app:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The app runs at:

```text
http://localhost:8080
```

Check readiness at [http://localhost:8080/actuator/health/readiness](http://localhost:8080/actuator/health/readiness).
Stop the app with `Ctrl+C`, or the IDE's Stop button.

## API docs

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```

## Main endpoints

```text
GET    /api/v1/films
GET    /api/v1/films/{id}
POST   /api/v1/films
PUT    /api/v1/films/{id}
DELETE /api/v1/films/{id}

POST   /api/v1/films/collection
POST   /api/v1/films/existence

GET    /api/v1/actors/{id}
PUT    /api/v1/actors/{id}
DELETE /api/v1/actors/{id}

GET    /api/v1/directors/{id}
PUT    /api/v1/directors/{id}
DELETE /api/v1/directors/{id}
```

`GET /api/v1/films` supports filters for `title`, `yearFrom`, `yearTo`, `genres` and `countries`.

`POST /api/v1/films/collection` searches within supplied film IDs. `POST /api/v1/films/existence` returns the requested IDs that are missing from catalog.

Include the film response's `version` when updating a film; stale updates return `409 Conflict`.

## Kafka events

Film deletion and poster changes are recorded in an outbox in the same database transaction. A background publisher sends the events to Kafka. User and media services then remove deleted films from lists and delete old posters asynchronously.

## Build

Build the JAR and run the tests with Docker running:

```bash
./mvnw clean verify
```

The JAR is written to `target/`. Remove generated build files with `./mvnw clean`.
On Windows, use `.\mvnw.cmd` instead of `./mvnw` for these commands and the test command below.

## Tests

```bash
./mvnw test
```

Some tests use Testcontainers, so Docker should be running.

## Notes

- With the example settings, PostgreSQL runs on `localhost:5432` and CORS allows `http://localhost:5173`.
- GET endpoints and the collection and existence POST endpoints are public.
- Catalog changes require a Keycloak access token with the `ADMIN` realm role, sent as `Authorization: Bearer <token>`.
- Health details, `/actuator/info` and `/actuator/metrics` also require `ADMIN`.
- Run one catalog publisher at a time; the current outbox implementation does not coordinate multiple instances.