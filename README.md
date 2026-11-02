# 🍕 PizzaStore 🍕

Een complete REST API voor pizzabeheer gebouwd met Spring Boot, inclusief JWT authenticatie en OpenAPI/Swagger documentatie.

## Technologieën

- **Java 25**
- **Spring Boot 4.0.4**
- **Spring Security** met JWT authenticatie
- **Spring Data JPA** met H2 database
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
├── domain/             # Entity models (Pizza, Order, Customer)
├── dto/                # Request/Response DTOs
├── mapper/             # MapStruct mappers
├── exception/          # Custom exceptions
└── security/           # JWT & Security configuratie
```

## API Documentatie

Na het starten van de applicatie is de Swagger UI beschikbaar op: 
```
http://localhost:8080/swagger-ui.html
```

## Starten van de Applicatie

```bash
# Compileren en starten
mvn spring-boot:run

# Of met Maven wrapper
./mvnw spring-boot:run
```

## Database

De applicatie gebruikt een H2 in-memory database. De H2 console is beschikbaar voor development.

## Testing

De applicatie bevat unit tests voor de service laag met Mockito. 

```bash
# Tests uitvoeren
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
- JPA Auditing voor created/updated tracking
- Validation met Jakarta Bean Validation
- Exception handling volgens RFC 7807 (ProblemDetail)
