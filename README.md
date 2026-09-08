# eshop

A contract-first e-commerce backend built with Spring Boot 4, demonstrating API-first design via OpenAPI code generation, layered architecture (Controller → Service → Repository → Entity), and a Vaadin-based frontend shell. Also doubles as a hands-on playground for diagnosing and fixing query performance at scale (10M+ row tables).

## Tech Stack

- **Java 21**, **Spring Boot 4.0**
- **Spring Data JPA** + **MySQL** (via `mysql-connector-j`)
- **OpenAPI Generator (Maven plugin)** — REST interfaces generated from `api.yml`, controllers implement the generated contract
- **Vaadin** — frontend framework, with client-side validation via `BeanValidationBinder`
- **JUnit 5 / AssertJ** — `@DataJpaTest` repository tests, controller tests against the service layer

## Architecture

The API contract lives in `src/main/resources/api.yml` (OpenAPI 3.0). The Maven build generates the `ProductsApi` interface and `Product` DTO at compile time (`target/generated-sources/openapi`); `ProductController` implements that interface, keeping the REST contract and implementation in sync automatically. A `ProductService` sits between the controller/Vaadin view and the repository so both entry points share the same business logic.

```
Client → ProductController (implements generated ProductsApi)  ─┐
Vaadin MainView ────────────────────────────────────────────────┼→ ProductService → ProductRepository (Spring Data JPA)
                                                                  ┘                  → ProductEntity → MySQL
```

### Domain model
`ProductEntity` now belongs to a `CategoryEntity` (lazy `@ManyToOne`), and `OrderEntity` / `OrderItemEntity` model orders placed against products. The FK from `products` to `categories` is intentionally left unindexed for now — see "Known issue" below.

### Data seeding
`DataSeeder` (`app.seed.enabled=true`) populates millions of rows via raw JDBC batch inserts (bypassing JPA's per-row persistence context, which would be impractically slow at this volume). Used to reproduce realistic query costs — current dev DB: ~10M products, ~1M orders, ~3M order items.

## API Endpoints

| Method | Endpoint                | Description                                  |
|--------|--------------------------|-----------------------------------------------|
| GET    | `/products`              | List all products                             |
| POST   | `/products`              | Create a product                              |
| GET    | `/products/{id}`         | Get a product by ID                           |
| PUT    | `/products/{id}`         | Update a product                              |
| DELETE | `/products/{id}`         | Delete a product                              |
| GET    | `/api/products/search`   | Filtered/paginated search (`name`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size`) — **load-test target, not part of `api.yml`**, kept separate on purpose so it's obvious which endpoint is under profiling |

Validation errors and not-found cases return consistent JSON via `GlobalExceptionHandler`; Vaadin's `MainView` surfaces the same constraint violations client-side before they hit the service.

## Known issue: `/api/products/search` is slow on large filtered ranges

Under the seeded 10M-row dataset, filtered searches (e.g. `price BETWEEN 50 AND 200`, ~1.5M matching rows) take 5–6s; `name LIKE '%phone%'` cases run 9–11s. Root cause, confirmed via `EXPLAIN ANALYZE` and `hibernate.show-sql`:

1. **`Page<T>` triggers a second query** — Spring Data derives a `COUNT(*)` from the same JPQL to compute `totalElements`. Indexes make MySQL fast at *finding* matching rows, not at *counting* ~1.5M of them — this accounts for most of the latency.
2. **Non-covering data query** — `SELECT p FROM ProductEntity p ...` pulls full entities, forcing an extra per-row lookup from the index into the clustered (PK) index for columns like `name`/`description`.
3. **N+1 on `category`** — the lazy proxy gets touched during JSON serialization (open-session-in-view), firing one `SELECT` per row instead of a join. Cheap on localhost, will matter over a real network.
4. **Parameterized "kitchen sink" `WHERE (:x IS NULL OR ...)`** — MySQL compiles one query plan that must stay valid regardless of which filters are bound at runtime, so it can't specialize around the filters actually present in a given call.

Fix plan (in order of expected impact) — see Roadmap.

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

To seed a large dataset for load testing:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=true --app.seed.product-count=10000000"
```

### Test

```bash
./mvnw test
```

## Roadmap

- [ ] Replace `Page<ProductEntity>` with `Slice<ProductEntity>` on the search endpoint — drops the `COUNT(*)` query entirely
- [ ] `JOIN FETCH p.category` on the search query — eliminates the N+1
- [ ] Project search results to a DTO (`id`, `name`, `price`, `categoryName`) instead of the full entity — avoids leaking the lazy proxy and reduces the non-covering lookup
- [ ] Approximate/cache exact counts for filtered searches (e.g. `information_schema.TABLE_ROWS` or a periodically refreshed count) rather than computing them live per request
- [ ] Normalize line endings to LF via `.gitattributes` (editor is currently writing CRLF against an LF repo, causing spurious full-file diffs on untouched code)
- [ ] Index the `products.category_id` FK once the join-based fix above is measured against the current unindexed baseline
- [ ] Dockerize (app + MySQL via `docker-compose`)
