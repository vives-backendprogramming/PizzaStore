# Zelfevaluatie - PizzaStore Java Spring Boot Applicatie

## Deel 1: Technisch overzicht

### 1. Beschrijving van de applicatie
PizzaStore is een complete REST API voor het beheren van een pizzarestaurant. De applicatie biedt functionaliteit voor het beheren van pizza's (menu), klanten, bestellingen en orderregels. Klanten kunnen pizza's browsen, favorieten toevoegen en bestellingen plaatsen. Administrators kunnen het volledige menu beheren, bestellingen updaten en alle klantgegevens inzien. De applicatie gebruikt JWT-authenticatie voor beveiliging en biedt uitgebreide OpenAPI/Swagger documentatie. Nutritionele informatie per pizza is beschikbaar via een embedded entiteit.

### 2. Java- en Spring Boot-versie
- **Java-versie**: 25
- **Spring Boot-versie**: 3.5.7

### 3. URL van de gedeployde Swagger/OpenAPI-documentatie
- Lokaal (dev): http://localhost:8080/swagger-ui.html
- Productie: Niet gedeployed (configuratie voorzien voor https://pizzastore.example.com)

### 4. Testgebruikers en rollen

| Rol      | Gebruikersnaam            | Wachtwoord  |
|----------|---------------------------|-------------|
| ADMIN    | admin@pizzastore.be       | password123 |
| CUSTOMER | emma.johnson@example.com  | password123 |
| CUSTOMER | liam.smith@example.com    | password123 |
| CUSTOMER | olivia.brown@example.com  | password123 |
| CUSTOMER | noah.davis@example.com    | password123 |
| CUSTOMER | ava.wilson@example.com    | password123 |

### 5. Database
**Type**: H2 in-memory database (dev) / PostgreSQL (prod, configuratie voorzien)

**Opzet**:
- **Dev profiel**: H2 in-memory database met automatische schema-creatie (create-drop) en data.sql initialisatie
- **Prod profiel**: PostgreSQL database met validate modus (schema moet vooraf bestaan)
- JPA Auditing is geconfigureerd voor automatische timestamps en user tracking
- H2 Console beschikbaar op `/h2-console` in dev mode

### 6. Overzicht controllers, services, repositories en entities

| Controller          | Service            | Repository          | Entity                    | Functionaliteit                                          |
|---------------------|-------------------|---------------------|---------------------------|----------------------------------------------------------|
| PizzaController     | PizzaService      | PizzaRepository     | Pizza, NutritionalInfo    | Volledige CRUD, filtering, paginatie, image upload       |
| OrderController     | OrderService      | OrderRepository     | Order, OrderLine          | CRUD bestellingen, status updates, filtering, nested resources |
| CustomerController  | CustomerService   | CustomerRepository  | Customer                  | CRUD klanten, favorieten beheer, orders per klant        |
| AuthController      | CustomUserDetailsService | CustomerRepository | Customer             | Registratie, login, JWT authenticatie                    |

**Notitie**: Controllers geordend van meest naar minst uitgewerkte functionaliteit.

### 7. Authenticatie
De applicatie gebruikt **JWT (JSON Web Token) authenticatie** met Spring Security:

- **PasswordEncoder**: BCrypt voor wachtwoordhashing
- **JWT generatie**: Bij succesvolle login via `/api/auth/login`
- **JWT validatie**: Via custom `JwtAuthenticationFilter` die elke request intercepteert
- **Token expiratie**: 24 uur (86400000 ms)
- **Rollen**: CUSTOMER en ADMIN met role-based access control
- **Security configuration**: Stateless session management, verschillende endpoints toegankelijk per rol
- **Registratie endpoint**: `/api/auth/register` voor nieuwe gebruikers (standaard CUSTOMER rol)

---

## Deel 2: Checklist

| Nr | Categorie                | Vereiste                                                                                | ✅/⚠️/❌ | Opmerking/commentaar |
|----|--------------------------|-----------------------------------------------------------------------------------------|----------|---------------------|
| 0  | Algemeen                 | Alle code compileert                                                                    | ✅       | Build succesvol met enkele MapStruct warnings (geen blocking issues) |
| 0  | Algemeen                 | Maven build succesvol met \`./mvnw clean package\`                                        | ⚠️       | Maven wrapper (mvnw) ontbreekt, maar \`mvn clean package\` werkt perfect |
| 0  | Algemeen                 | Dependency Injection overal correct gebruikt                                            | ✅       | Constructor injection consistent toegepast in alle lagen |
| 1  | Functionele werking      | App start vlot op met dev-profiel                                                       | ✅       | Start zonder problemen (getest via test execution) |
| 1  | Functionele werking      | Alle endpoints functioneren foutloos (CRUD, filtering, security)                        | ✅       | 132 tests slagen allemaal, inclusief security tests |
| 1  | Functionele werking      | Geen 5xx-fouten bij gebruik                                                             | ✅       | Alle tests passeren met correcte HTTP status codes |
| 2  | Persistentie             | Entiteiten correct geconfigureerd                                                       | ✅       | JPA annotations correct, validaties aanwezig |
| 2  | Persistentie             | Relaties correct geïmplementeerd                                                        | ✅       | OneToMany, ManyToOne, ManyToMany correct gebruikt |
| 2  | Persistentie             | Geen FetchType.EAGER op veel-relaties                                                   | ✅       | Geen EAGER fetching gevonden in codebase |
| 3  | Testen                   | Unit tests voor controllers aanwezig                                                    | ✅       | 3 controller test classes (43 tests totaal) |
| 3  | Testen                   | Unit tests voor repository-methodes aanwezig                                            | ✅       | 3 repository test classes (38 tests totaal) |
| 3  | Testen                   | (Indien van toepassing) Service-lagen getest                                            | ✅       | 3 service test classes (47 tests totaal) |
| 3  | Testen                   | "Happy flows" voor alle endpoints getest                                                | ✅       | CRUD operaties voor alle resources getest |
| 3  | Testen                   | Edge cases & foutafhandeling getest                                                     | ✅       | Tests voor 404, validation errors, duplicates aanwezig |
| 3  | Testen                   | Beveiligde endpoints (verschillende rollen) getest                                      | ✅       | @WithMockUser tests met ADMIN/CUSTOMER rollen |
| 3  | Testen                   | Overzicht testcoverage (percentage &/of aantal testen vermelen)                        | ✅       | 132 tests totaal - 100% success rate |
| 4  | REST API & documentatie  | Minstens 2 controllers geïmplementeerd                                                  | ✅       | 4 controllers: Pizza, Order, Customer, Auth |
| 4  | REST API & documentatie  | Minstens 1 controller met volledige CRUD-functionaliteit                                | ✅       | Alle 3 resource controllers hebben volledige CRUD |
| 4  | REST API & documentatie  | Minstens 1 controller met child-records benaderbaar                                     | ✅       | OrderController met OrderLines, CustomerController met Orders/Favorites |
| 4  | REST API & documentatie  | Correcte HTTP-methodes en statuscodes gebruikt                                         | ✅       | GET/POST/PUT/PATCH/DELETE met correcte 200/201/204/400/404 responses |
| 4  | REST API & documentatie  | Consistente, duidelijke endpoint-structuur                                              | ✅       | RESTful patterns: /api/pizzas, /api/orders, /api/customers |
| 4  | REST API & documentatie  | Paginatie aanwezig indien relevant                                                      | ✅       | Pageable gebruikt in alle list endpoints |
| 4  | REST API & documentatie  | RESTful URL-patterns gevolgd                                                            | ✅       | Correcte resource nesting en gebruik van HTTP verbs |
| 4  | REST API & documentatie  | OpenAPI/Swagger-documentatie volledig & beschikbaar                                    | ✅       | SpringDoc OpenAPI volledig geconfigureerd op /swagger-ui.html |
| 4  | REST API & documentatie  | Controllers voldoende gedocumenteerd                                                    | ✅       | @Operation, @ApiResponse annotations op alle endpoints |
| 4  | REST API & documentatie  | Schema-documentatie voor request/response DTO's aanw.                                   | ✅       | @Schema annotations op alle DTO's |
| 4  | REST API & documentatie  | Documentatie toont endpoints met beveiliging/rollen                                     | ✅       | @SecurityRequirement annotations aanwezig |
| 4  | REST API & documentatie  | Communicatie via DTO's (nooit directe entities)                                         | ✅       | MapStruct mappers voor alle entity-DTO conversies |
| 4  | REST API & documentatie  | DTO's correct gestructureerd (request/response scheiding)                               | ✅       | Aparte packages: dto/request en dto/response |
| 4  | REST API & documentatie  | Inkomende data steeds gevalideerd                                                       | ✅       | @Valid annotation + Jakarta validation constraints |
| 4  | REST API & documentatie  | Foutafhandeling (error handling) geregeld                                               | ✅       | GlobalExceptionHandler met custom ErrorResponse |
| 5  | Security                 | Authenticatie en autorisatie correct geïmplementeerd                                   | ✅       | JWT + Spring Security met role-based access |
| 5  | Security                 | Minstens twee rollen voorzien                                                           | ✅       | ADMIN en CUSTOMER rollen |
| 5  | Security                 | Rolgebaseerde autorisatie aanwezig                                                     | ✅       | hasRole() checks in SecurityConfig |
| 5  | Security                 | Standaardgebruikers voor beide rollen aanwezig                                         | ✅       | data.sql bevat users met beide rollen |
| 5  | Security                 | Geen plaintext passwords (alle passwords gehashed)                                      | ✅       | BCrypt hashing, passwords in data.sql gehashed |
| 5  | Security                 | Endpoint voor registratie en authenticatie aanwezig                                    | ✅       | /api/auth/register en /api/auth/login |
| 5  | Security                 | Geen conflicterende security rules                                                      | ✅       | Security configuratie logisch opgebouwd |
| 6  | Profielen                | Minstens 2 profielen (\`dev\`/\`prod\`) met aparte DB                                      | ✅       | dev (H2) en prod (PostgreSQL) profielen |
| 6  | Profielen                | Correcte profiel-specifieke configuratie                                                | ✅       | application-dev.properties en application-prod.properties |
| 6  | Profielen                | Gebruik env variabelen voor gevoelige prod-properties                                  | ❌       | Prod credentials hardcoded in application-prod.properties |
| 7  | Clean code               | Duidelijke package-structuur, betekenisvolle namen                                      | ✅       | Logische package indeling (controller, service, repository, domain, dto, etc.) |
| 7  | Clean code               | Correcte laag-scheiding (controller, service, repo, model)                             | ✅       | Strikte scheiding tussen lagen, geen business logic in controllers |
| 7  | Clean code               | Code voldoet aan Java & Spring Boot best practices                                     | ✅       | Records voor DTO's, constructor injection, proper exception handling |

---

## Deel 3: Testcoverage-overzicht

\`\`\`
Test Coverage:
Repository Tests: 3 classes (38 tests)
  - PizzaRepositoryTest (16 tests)
  - CustomerRepositoryTest (11 tests)
  - OrderRepositoryTest (11 tests)

Service Tests: 3 classes (47 tests)
  - PizzaServiceTest (18 tests)
  - OrderServiceTest (15 tests)
  - CustomerServiceTest (14 tests)

Controller Tests: 3 classes (43 tests)
  - PizzaControllerTest (20 tests)
  - OrderControllerTest (14 tests)
  - CustomerControllerTest (9 tests)

Integration Tests: 1 class (4 tests)
  - PizzaIntegrationTest (4 tests)

Total: 132 tests - 100% success rate
\`\`\`

---

## ⭐️ Aanbevelingen

### Sterke punten:
1. **Uitstekende testcoverage**: 132 tests over alle lagen (repository, service, controller, integration) met 100% success rate
2. **Professionele API documentatie**: Volledige OpenAPI/Swagger documentatie met gedetailleerde beschrijvingen, voorbeelden en schema's
3. **Clean architecture**: Strikte scheiding van verantwoordelijkheden, gebruik van DTO's met MapStruct, en proper dependency injection
4. **Goed beveiligingsmodel**: JWT authenticatie met role-based authorization, gehashte wachtwoorden en security tests
5. **Robuuste data layer**: Correcte JPA configuratie, geen eager loading op collections, audit tracking geïmplementeerd

### Verbeterpunten:
1. **Environment variables voor productie**: Gevoelige data zoals database credentials en JWT secret staan hardcoded in \`application-prod.properties\`. Gebruik \`\${ENV_VAR:default}\` syntax voor productie credentials.

2. **Maven wrapper ontbreekt**: Voeg Maven wrapper toe met \`mvn wrapper:wrapper\` zodat \`./mvnw\` commando's werken zonder lokale Maven installatie.

3. **Jacoco testcoverage plugin**: Voeg Jacoco Maven plugin toe aan pom.xml om automatische coverage rapporten te genereren (aanbevolen voor CI/CD).

4. **MapStruct warnings**: Los de unmapped target properties warnings op in PizzaMapper door expliciet \`@Mapping(target = "...", ignore = true)\` te gebruiken voor velden die niet gemapped moeten worden.

5. **PostgreSQL dependency**: Voeg PostgreSQL JDBC driver toe aan pom.xml voor daadwerkelijke productie deployment (momenteel alleen H2 aanwezig).
