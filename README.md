# 🍕 PizzaStore – MongoDB version 🍕

This is the **MongoDB version** of the PizzaStore reference project (branch `mongodb`).

It is the same application as the final PizzaStore on the `main` branch: the same REST API, JWT security, validation, error handling, Open Food Facts client and OpenAPI documentation. Only the database changes: **MongoDB with Spring Data MongoDB** instead of Spring Data JPA with H2. See [Differences from the JPA version](#differences-from-the-jpa-version-branch-main) for what that changes in the code.

## Technologies

- **Java 25**
- **Spring Boot 4.0.8**
- **Spring Data MongoDB** with a MongoDB 8 server
- **Spring Security** with JWT authentication
- **MapStruct** for object mapping
- **SpringDoc OpenAPI** for the API documentation
- **Testcontainers** for a real MongoDB in the tests
- **Maven** for dependency management

## Features

### Pizza management
- CRUD operations for pizzas
- Filtering by price (less than, between) and by name
- Pagination and sorting
- Image upload
- Nutritional information, also importable from Open Food Facts by barcode

### Order management
- Placing orders (always for the logged-in customer, taken from the JWT)
- Order status tracking (PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED)
- Several pizzas per order

### Customer management
- Customer data (admin) and the customer's own account (`/api/customers/me`)
- Favorite pizzas
- Order history

### Security
- JWT authentication
- Role-based access control (CUSTOMER, ADMIN)
- Method security (`@PreAuthorize`, `@IsAdminOrOrderOwner`): a customer only sees and cancels their own orders
- Passwords stored as BCrypt hashes via `DelegatingPasswordEncoder` (`{bcrypt}` prefix)
- CORS for browser front-ends (`pizzastore.cors.allowed-origins`)

## Project structure

```
src/main/java/be/vives/pizzastore/
├── client/             # Open Food Facts HTTP client (@HttpExchange)
├── config/             # MongoConfig (auditing), DataSeeder (seed data), OpenAPI, HTTP client, web config
├── controller/         # REST API endpoints
├── domain/             # MongoDB documents (Pizza, Order, Customer) and embedded objects (NutritionalInfo, OrderLine)
├── dto/                # Request/response DTOs
├── exception/          # Custom exceptions and the global exception handler (ProblemDetail)
├── mapper/             # MapStruct mappers
├── repository/         # Spring Data MongoDB repositories
├── security/           # JWT and security configuration
└── service/            # Business logic
```

## Running the application

### 1. Start MongoDB

The application needs a MongoDB server on `localhost:27017` (profile `dev`, see `application-dev.properties`). The easiest way is Docker:

```bash
docker run -d --name pizzastore-mongo -p 27017:27017 mongo:8.0
```

Next time, start the existing container again:

```bash
docker start pizzastore-mongo
```

### 2. Start the application

```bash
mvn spring-boot:run
```

The `dev` profile is active by default. The application runs on http://localhost:8080.

### 3. Seed data

On the first start, `DataSeeder` (profile `dev` only) fills the empty database `pizzastore_dev` with the same data as `data.sql` on `main`: 12 pizzas, 6 customers and 10 orders. Unlike an in-memory H2 database, MongoDB keeps its data after a restart, so this happens only once.

Accounts to log in with (`POST /api/auth/login`), all with password `password123`:

| E-mail | Role |
|---|---|
| `admin@pizzastore.be` | ADMIN |
| `emma.johnson@example.com` | CUSTOMER |
| `liam.smith@example.com` | CUSTOMER |

To start over with fresh seed data, drop the database and restart the application:

```bash
docker exec pizzastore-mongo mongosh pizzastore_dev --eval "db.dropDatabase()"
```

### Production

In the `prod` profile the complete connection string comes from the environment variable `MONGODB_URI`, for example `mongodb://user:secret@db.example.com:27017/pizzastore_prod`. The JWT secret comes from `JWT_SECRET`. Seed data is not loaded in production.

## API documentation

Once the application runs, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Ids are MongoDB ObjectIds as strings, for example `GET /api/pizzas/6717a1c2e4b0a1b2c3d4e5f6`. Get a real id from `GET /api/pizzas` first.

## Testing

**Docker must be running.** MongoDB has no in-memory mode like H2, so every test that needs the database starts a real MongoDB in a Docker container with Testcontainers.

```bash
mvn test
```

How the tests are set up:

- **`MongoTestcontainersConfiguration`** (`src/test/java/be/vives/pizzastore`) defines a `MongoDBContainer` bean (`mongo:8.0`) annotated with `@ServiceConnection`. Spring Boot then connects the application to that container on its random port, instead of to `spring.mongodb.uri`. Every test that needs the database imports it with `@Import(MongoTestcontainersConfiguration.class)`.
- The container lives as long as the Spring application context. Test classes with the same configuration share the cached context, and therefore the container and its data. The first run downloads the `mongo:8.0` image, which takes a little longer.
- **There is no rollback after a test**, unlike `@DataJpaTest` or `@Transactional` tests with JPA. Tests clean up themselves: the repository tests empty their collections in `@BeforeEach`, and the full-stack tests use unique data (random e-mail addresses and pizza names) and never assume an empty database.

The kinds of tests:

| Kind | Annotation | Database | Examples |
|---|---|---|---|
| Unit tests | Mockito (`@Mock`, `@InjectMocks`) | no | `PizzaServiceTest`, `OrderServiceTest`, `CustomerServiceTest` |
| Repository slice tests | `@DataMongoTest` | MongoDB container | `PizzaRepositoryTest`, `OrderRepositoryTest`, `CustomerRepositoryTest` |
| Web slice tests | `@WebMvcTest` + `RestTestClient`/`MockMvc` | no (services mocked) | `PizzaControllerTest`, `OrderControllerTest` |
| Other slices | `@RestClientTest`, `@JsonTest` | no | `OpenFoodFactsClientTest`, `PizzaJsonTest` |
| Integration tests | `@SpringBootTest` | MongoDB container | `PizzaStoreApiIntegrationTest`, `SecurityIntegrationTest` |

The repository tests insert their test data with `MongoTemplate`, the counterpart of `TestEntityManager` with JPA. Some tests also read the raw BSON document to show how the data is stored, for example that the order lines are embedded in the order document and that prices are stored as `Decimal128`.

Run one test class:

```bash
mvn test -Dtest=OrderRepositoryTest
```

The tests that need no database (unit, web slice, JSON and REST client tests) also run without Docker. The ones that do need it fail with "Could not find a valid Docker environment".

## API endpoints (overview)

- `POST /api/auth/register`, `POST /api/auth/login` – register and log in (returns a JWT)
- `GET /api/pizzas`, `GET /api/pizzas/{id}` – pizzas (public)
- `POST`, `PUT`, `DELETE /api/pizzas/{id}`, `POST /api/pizzas/{id}/image`, `POST /api/pizzas/{id}/nutritional-info/import` – manage pizzas (ADMIN)
- `POST /api/orders` – place an order (CUSTOMER)
- `GET /api/orders/{id}`, `DELETE /api/orders/{id}` – read or cancel one order (ADMIN or the customer who placed it)
- `GET /api/orders`, `PATCH /api/orders/{id}/status` – all orders, status changes (ADMIN)
- `GET`, `PUT /api/customers/me`, `/api/customers/me/orders`, `/api/customers/me/favorites` – the logged-in customer's own account
- `/api/customers/**` – customer management (ADMIN)

## Differences from the JPA version (branch `main`)

The REST API, the security and the business rules are the same. What changes is the repository layer and what depends directly on it:

| | `main` (Spring Data JPA + H2) | `mongodb` (Spring Data MongoDB) |
|---|---|---|
| Domain classes | `@Entity`, `@Table`, `@Column` | `@Document`, `@Indexed` (unique e-mail and order number) |
| Id | `Long`, `IDENTITY` (1, 2, 3, ...) | `String` holding an `ObjectId` (e.g. `"6717a1c2e4b0a1b2c3d4e5f6"`), also in the DTOs and URLs |
| `NutritionalInfo` | own table, `@OneToOne` | **embedded** in the pizza document |
| `OrderLine` | own table, `@ManyToOne` to order and pizza | **embedded** in the order document, storing `pizzaId` plus a copy of the name and price. That is why `OrderLineResponse` has no `id` |
| Order → customer | `@ManyToOne Customer` | `customerId` plus a copy of `customerName` (the *extended reference* pattern) |
| Favorites | `@ManyToMany` with a join table | `Set<String> favoritePizzaIds` in the customer document |
| `JOIN FETCH` queries | `findByIdWithOrderLines`, `findByIdWithFavorites`, ... | not needed: embedded data comes along with `findById` |
| Sales statistics | JPQL `SELECT new PizzaSalesStatistics(...) GROUP BY` | `@Aggregation` pipeline (`$unwind`, `$group`, `$project`, `$sort`) |
| Deleting a customer | `cascade = ALL` deletes the orders | `CustomerService` deletes the orders itself |
| Deleting a pizza still in use | foreign key → 409 | `PizzaService` checks it itself → 409 |
| Transactions | `@Transactional` on the services | none: multi-document transactions need a replica set; saving one document is always atomic |
| Owner of an order (`OrderSecurity`) | one query with a join on `customer.email` | first the customer by e-mail, then `existsByIdAndCustomerId` |
| Seed data | `data.sql` | `DataSeeder` (`CommandLineRunner`, profile `dev`) |
| Repository tests | `@DataJpaTest` + `TestEntityManager`, rollback after each test | `@DataMongoTest` + `MongoTemplate`, cleanup in `@BeforeEach` |
| Test database | H2 in memory | MongoDB in Docker via Testcontainers (`@ServiceConnection`) |
| Configuration | `spring.datasource.*`, `spring.jpa.*` | `spring.mongodb.uri`, `spring.data.mongodb.auto-index-creation=true`, `spring.data.mongodb.representation.big-decimal=decimal128` |

The JWT code itself (`JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`) is unchanged: the user is still looked up by e-mail address.

Two configuration details that are easy to miss:

- **`spring.mongodb.uri`** is the Spring Boot 4 name of the connection property. `spring.data.mongodb.uri` is the deprecated Spring Boot 3 name.
- **`spring.data.mongodb.representation.big-decimal=decimal128`** stores prices as real decimal numbers (BSON `Decimal128`). Queries such as `findByPriceLessThan` then compare numbers, and the aggregation can `$sum` them.

## Development features

- Spring Boot DevTools for hot reload
- Spring Data MongoDB auditing (`@EnableMongoAuditing`) for createdAt/createdBy/updatedAt/updatedBy
- Validation with Jakarta Bean Validation
- Error responses as RFC 9457 `ProblemDetail`
- MongoDB queries logged in the `dev` profile (`MongoTemplate` at DEBUG level)
