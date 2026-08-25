# eshop

A contract-first e-commerce backend built with Spring Boot 4, demonstrating API-first design via OpenAPI code generation, layered architecture (Controller → Repository → Entity), and a Vaadin-based frontend shell.

## Tech Stack

- **Java 21**, **Spring Boot 4.0**
- **Spring Data JPA** + **MySQL** (via `mysql-connector-j`)
- **OpenAPI Generator (Maven plugin)** — REST interfaces generated from `api.yml`, controllers implement the generated contract
- **Vaadin** — frontend framework starter
- **JUnit 5 / AssertJ** — `@DataJpaTest` repository tests

## Architecture

The API contract lives in `src/main/resources/api.yml` (OpenAPI 3.0). The Maven build generates the `ProductsApi` interface and `Product` DTO at compile time (`target/generated-sources/openapi`); `ProductController` implements that interface, keeping the REST contract and implementation in sync automatically.

```
Client → ProductController (implements generated ProductsApi)
       → ProductRepository (Spring Data JPA)
       → ProductEntity → MySQL
```

## API Endpoints

| Method | Endpoint         | Description          |
|--------|------------------|-----------------------|
| GET    | `/products`      | List all products     |
| POST   | `/products`      | Create a product      |
| GET    | `/products/{id}` | Get a product by ID   |
| PUT    | `/products/{id}` | Update a product      |
| DELETE | `/products/{id}` | Delete a product      |

## Getting Started

### Prerequisites
- JDK 21+
- MySQL running locally with a `webshop` database

### Configuration
Database credentials are read from environment variables (not committed to source):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
```

### Run

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Test

```bash
./mvnw test
```

## Roadmap

- [ ] Input validation on `Product` (name/price constraints)
- [ ] Global exception handling for consistent error responses
- [ ] Vaadin UI wired to the product API
- [ ] Dockerize (app + MySQL via `docker-compose`)
