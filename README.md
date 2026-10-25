# 🍕 PizzaStore 🍕

Een complete REST API voor pizzabeheer gebouwd met Spring Boot, inclusief JWT authenticatie en OpenAPI/Swagger documentatie.

> **Branch `mongodb`**: exact dezelfde applicatie als op `main`, maar met **MongoDB** (Spring Data MongoDB) als databank in plaats van Spring Data JPA met H2. Zie [Verschillen met de JPA-versie](#verschillen-met-de-jpa-versie-branch-main).

## Technologieën

- **Java 25**
- **Spring Boot 4.0.8**
- **Spring Security** met JWT authenticatie
- **Spring Data MongoDB** met een MongoDB-server (MongoDB 8)
- **MapStruct** voor object mapping
- **SpringDoc OpenAPI** voor API documentatie
- **Maven** voor dependency management

## Functionaliteiten

### Pizza Management
- CRUD operaties voor pizza's
- Zoeken op prijs (minder dan, tussen waarden)
- Zoeken op naam
- Paginering en filtering
- Image upload voor pizza's
- Nutritional information

### Order Management
- Bestellingen plaatsen
- Order status tracking (PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED)
- Meerdere pizza's per bestelling

### Customer Management
- Klantgegevens beheer
- Favoriete pizza's
- Bestelgeschiedenis

### Security
- JWT authenticatie
- Role-based access control (CUSTOMER, ADMIN)
- Method security (`@PreAuthorize`, `@IsAdmin`, `@IsAdminOrOwner`): een klant ziet en wijzigt enkel zijn eigen account en bestelt enkel voor zichzelf; `GET /api/customers/me`
- Wachtwoorden als BCrypt-hash via `DelegatingPasswordEncoder` (`{bcrypt}`-prefix)
- CORS voor browser-front-ends (`pizzastore.cors.allowed-origins`)
- Beveiligde endpoints

## Project Structuur

```
src/main/java/be/vives/pizzastore/
├── controller/          # REST API endpoints
├── service/            # Business logic
├── repository/         # Data access layer
├── domain/             # MongoDB documents (Pizza, Order, Customer) en embedded objecten (NutritionalInfo, OrderLine)
├── dto/                # Request/Response DTOs
├── mapper/             # MapStruct mappers
├── exception/          # Custom exceptions
├── security/           # JWT & Security configuratie
└── config/             # o.a. MongoConfig (auditing) en DataSeeder (testdata)
```

## API Documentatie

Na het starten van de applicatie is de Swagger UI beschikbaar op: 
```
http://localhost:8080/swagger-ui.html
```

## Starten van de Applicatie

De applicatie heeft een draaiende MongoDB-server nodig op `localhost:27017` (profiel `dev`, zie `application-dev.properties`). Met Docker:

```bash
docker run -d --name pizzastore-mongo -p 27017:27017 mongo:8.0
```

Daarna:

```bash
mvn spring-boot:run
```

## Database

MongoDB, databank `pizzastore_dev`. Bij de eerste start vult `DataSeeder` (enkel profiel `dev`) een lege databank met dezelfde testdata als `data.sql` op `main`: 12 pizza's, 6 klanten (wachtwoord `password123`, admin: `admin@pizzastore.be`) en 10 bestellingen. Omdat MongoDB, anders dan H2 in-memory, een herstart overleeft, gebeurt dat maar één keer. Opnieuw beginnen:

```bash
mongosh pizzastore_dev --eval "db.dropDatabase()"
```

In productie (profiel `prod`) komt de volledige connection string uit de omgevingsvariabele `MONGODB_URI`.

## Testing

Unit tests (Mockito), slice tests (`@WebMvcTest`, `@DataMongoTest`, `@RestClientTest`, `@JsonTest`) en integratietests (`@SpringBootTest`). Elke test die de databank nodig heeft, start via **Testcontainers** een echte MongoDB in een Docker-container (`MongoTestcontainersConfiguration`), dus **Docker moet draaien**.

```bash
mvn test
```

## API Endpoints

- `GET /api/pizzas` - Lijst van alle pizza's (gepagineerd)
- `GET /api/pizzas/{id}` - Pizza details
- `POST /api/pizzas` - Nieuwe pizza aanmaken
- `PUT /api/pizzas/{id}` - Pizza updaten
- `DELETE /api/pizzas/{id}` - Pizza verwijderen
- `GET /api/pizzas/search` - Pizza's zoeken
- `POST /api/orders` - Bestelling plaatsen
- `GET /api/customers` - Klanten beheren

## Development Features

- Spring Boot DevTools voor hot reload
- Spring Data MongoDB Auditing (`@EnableMongoAuditing`) voor created/updated tracking
- Validation met Jakarta Bean Validation
- Exception handling volgens RFC 7807 (ProblemDetail)

## Verschillen met de JPA-versie (branch `main`)

De REST API, de security en de business rules zijn dezelfde. Wat verandert, zit in de repositorylaag en wat daar rechtstreeks van afhangt:

| | `main` (Spring Data JPA + H2) | `mongodb` (Spring Data MongoDB) |
|---|---|---|
| Domeinklassen | `@Entity`, `@Table`, `@Column` | `@Document`, `@Indexed` (unieke e-mail en ordernummer) |
| Id | `Long`, `IDENTITY` (1, 2, 3, ...) | `String` met een `ObjectId` (bv. `"6717a1c2e4b0a1b2c3d4e5f6"`), ook in de DTO's en URL's |
| `NutritionalInfo` | eigen tabel, `@OneToOne` | **embedded** in het pizza-document |
| `OrderLine` | eigen tabel, `@ManyToOne` naar order en pizza | **embedded** in het order-document; bewaart `pizzaId` + kopie van naam en prijs. `OrderLineResponse` heeft daarom geen `id` meer |
| Order → klant | `@ManyToOne Customer` | `customerId` + kopie van `customerName` (*extended reference*) |
| Favorieten | `@ManyToMany` met join table | `Set<String> favoritePizzaIds` in het klant-document |
| `JOIN FETCH`-queries | `findByIdWithOrderLines`, `findByIdWithFavorites`, ... | niet nodig: wat embedded is, komt mee met `findById` |
| Statistieken | JPQL `SELECT new PizzaSalesStatistics(...) GROUP BY` | `@Aggregation`-pipeline (`$unwind`, `$group`, `$project`, `$sort`) |
| Klant verwijderen | `cascade = ALL` verwijdert de bestellingen | `CustomerService` verwijdert de bestellingen zelf |
| Pizza in gebruik verwijderen | foreign key → 409 | `PizzaService` controleert het zelf → 409 |
| Transacties | `@Transactional` op de services | geen: multi-document transacties vragen een replica set; één document opslaan is altijd atomair |
| Eigenaar van een bestelling (`OrderSecurity`) | één query met join op `customer.email` | eerst de klant op e-mail, dan `existsByIdAndCustomerId` |
| Testdata | `data.sql` | `DataSeeder` (`CommandLineRunner`, profiel `dev`) |
| Repository-tests | `@DataJpaTest` + `TestEntityManager`, rollback na elke test | `@DataMongoTest` + `MongoTemplate`, opkuisen in `@BeforeEach` |
| Testdatabank | H2 in het geheugen | MongoDB in Docker via Testcontainers (`@ServiceConnection`) |
| Configuratie | `spring.datasource.*`, `spring.jpa.*` | `spring.mongodb.uri`, `spring.data.mongodb.auto-index-creation=true`, `spring.data.mongodb.representation.big-decimal=decimal128` |

JWT zelf (`JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`) is ongewijzigd: de gebruiker wordt nog altijd op e-mailadres opgezocht.
