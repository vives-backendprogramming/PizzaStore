# PizzaStore: upgrade naar Spring Boot 4 / Java 25 + cursus-gap-analyse

Datum: 2026-09-09
Status: goedgekeurd door gebruiker (brainstorming-sessie), klaar voor implementatieplan

## Context

`pizzastore` is de referentie-eindoplossing voor het vak Java Backend Programming
(3de jaar bachelor Toegepaste Informatica, VIVES Kortrijk). Het vak bestaat uit 13
lessen (`lesson-01-servlets` t/m `lesson-13-swagger-openapi`); vanaf les 2 bouwt elke
les verder op hetzelfde pizzastore-project. Studenten leveren aan het einde van het
vak een gelijkaardig project in en worden beoordeeld met de rubric in
`SELFEVAL_INSTRUCTION.md` / `SELFEVALUATION.md`.

Er is een nieuw naslagwerk beschikbaar: *Pro Spring Boot 4* (Gutierrez & Carter,
Apress, 4th edition, 2026), dat Spring Boot 4 / Spring Framework 7 / Jakarta EE 11
behandelt. Het huidige pizzastore-project draait nog op Spring Boot 3.5.7 (Java was
al op 25 gezet). Doel: pizzastore optrekken naar Spring Boot 4, zodat het boek als
naslagwerk kan dienen voor de overlappende hoofdstukken, én een volledige inventaris
maken van wat er in de huidige oplossing ontbreekt of verbeterd kan worden.

## Bevindingen uit de audit

### Technische staat van pizzastore (huidig)
- Spring Boot 3.5.7, Java 25, Maven.
- Domeinmodel: `Pizza`, `Customer`, `Order`, `OrderLine`, `NutritionalInfo`, `Role`,
  `OrderStatus` — JPA-entities met correcte relaties (OneToMany, ManyToOne,
  ManyToMany, OneToOne), auditing via `@CreatedDate`/`@LastModifiedDate` en een
  eigen `AuditorAwareImpl`.
- DTO's zijn Java records met Jakarta Bean Validation-annotaties; mapping via
  MapStruct (`componentModel = "spring"`).
- REST-laag: 4 controllers (Pizza, Order, Customer, Auth), correcte HTTP-methodes/
  statuscodes, paginatie via `Pageable`, filtering, file upload.
- Security: custom JWT-filter (`JwtAuthenticationFilter` + `JwtUtil` met jjwt 0.13.0),
  rolgebaseerde autorisatie (CUSTOMER/ADMIN), BCrypt-hashing, stateless sessions.
- Documentatie: springdoc-openapi 2.8.13 (Swagger UI), volledig geannoteerd.
- Testen: 132 tests (repository/service/controller/integration), MockMvc via
  `@WebMvcTest` + `@Import(SecurityConfig, GlobalExceptionHandler)`, 1
  `@SpringBootTest` integratietest met `@AutoConfigureMockMvc`. Geen gebruik van
  verwijderde SB4-testannotaties (`@MockBean`/`@SpyBean`) — alle service-tests
  gebruiken pure Mockito `@Mock`.
- Profielen: `dev` (H2, `create-drop`) en `prod` (PostgreSQL, `validate`).
- **Bevestigd probleem**: `application-prod.properties` bevat een hardcoded
  productie-wachtwoord in plaintext, en `application.properties` bevat een
  hardcoded JWT-secret — beide gecommit in git. Dit is al gekend via
  `SELFEVALUATION.md` (checklist-item "env variabelen" staat op ❌).

### Wat het boek dekt (en niet dekt) van de 13 lessen
Volledige inhoudsopgave doorgenomen (554 pagina's, 18 hoofdstukken + 1 appendix).
Relevante overlap met lesson-02 t/m lesson-13:

| Les | Boek-hoofdstuk(ken) | Overlap |
|---|---|---|
| 02 DI/IoC | H1–H2 | Volledig — constructor injection, `@SpringBootApplication` internals |
| 03 properties/profiles | H2 (Externalized Configuration, Profiles) | Volledig |
| 04 Spring Boot intro | H1 | Volledig |
| 05 Spring MVC | H3 (Request Lifecycle, annotaties) | Volledig |
| 06 JPA | H6 (Spring Data JPA-gedeelte) | Volledig |
| 07 DTO's/mappers | H3 (records als DTO), **geen MapStruct in het boek** | Gedeeltelijk — boek mapt manueel via records, pizzastore gebruikt MapStruct (prima, iets geavanceerder) |
| 08 REST-principes | H3 (Core REST Principles, **native API versioning**) | Gedeeltelijk — pizzastore heeft geen API-versioning |
| 09 complete REST API | H3 (Implementing Full CRUD) | Volledig |
| 10 validatie | H3 (Jakarta Bean Validation 3.1) | Volledig |
| 11 testing | H10 (Advanced Testing), **RestTestClient vervangt MockMvc/WebTestClient** | Gedeeltelijk — pizzastore gebruikt nog uitsluitend MockMvc (nog steeds ondersteund, niet fout) |
| 12 JWT auth | H11 (Securing Spring Boot) — boek focust op OAuth2/OIDC/OTT/2FA, **geen eigen JWT-filter-implementatie** | Pizzastore's aanpak (zelf een JWT-filter bouwen) is pedagogisch waardevoller voor dit doel — bewust behouden |
| 13 Swagger/OpenAPI | **Niet in het boek** (0 vermeldingen van "OpenAPI"/"Swagger"/"springdoc" in de volledige tekst) | Geen overlap — pizzastore's springdoc-aanpak blijft de referentie hiervoor |

Belangrijkste architecturale wijzigingen in Spring Boot 4 die dit boek beschrijft en die
pizzastore raken (Hoofdstuk 18, "What's New in Spring Boot 4"):
- `spring-boot-starter-web` → `spring-boot-starter-webmvc` (verplichte hernoeming)
- Jackson 3 (package `tools.jackson`, `JsonMapper` i.p.v. `ObjectMapper`)
- RFC 7807 **ProblemDetail** als eerste-klasse standaard voor foutresponses
- `RestClient`/`@HttpExchange` als vervanger voor `RestTemplate`
- `RestTestClient` als unified test-client (vervangt `MockMvc`/`WebTestClient`)
- Virtual threads via één property (`spring.threads.virtual.enabled=true`)
- Declaratieve mTLS via SSL bundles, vereenvoudigde 2FA in Spring Security 7
- Dedicated OTLP-observability-starter

### Gekende Spring Boot 4.0 breaking changes die pizzastore raken
(bevestigd via de officiële migratiegids en release-aankondiging, Spring Boot 4.0.0
GA sinds november 2025, huidige laatste patch 4.0.4)
- `spring-boot-starter-web` → `spring-boot-starter-webmvc`
- Jackson 3 upgrade (package-verhuis, mogelijk customizer-aanpassingen)
- `@SpringBootTest` configureert `MockMvc`/`TestRestTemplate` niet langer automatisch
  (niet van toepassing hier: pizzastore's enige `@SpringBootTest` heeft al expliciet
  `@AutoConfigureMockMvc`; de `@WebMvcTest`-tests blijven ongewijzigd werken)
- `@MockBean`/`@SpyBean` verwijderd → niet van toepassing, pizzastore gebruikt ze niet
- Servlet 6.1 / Jakarta EE 11 baseline
- Afhankelijkheden die van de Spring Boot BOM overerven (Hibernate 7, Jackson 3,
  Tomcat, Micrometer, Spring Security 7) schuiven automatisch mee op

## Scope van deze wijziging

### In scope — wordt geïmplementeerd
1. **pom.xml-upgrade**: parent `spring-boot-starter-parent` 3.5.7 → **4.0.4**.
   - `spring-boot-starter-web` → `spring-boot-starter-webmvc`
   - `springdoc-openapi-starter-webmvc-ui` 2.8.13 → laatste 3.x die SB4 ondersteunt
     (geverifieerd: 3.0.3+ ondersteunt Spring Boot 4.0.5)
   - `mapstruct`/`mapstruct-processor` → laatste **stabiele** 1.6.x (1.7 is nog beta,
     niet gebruiken)
   - `jjwt-*` blijft op 0.13.0 (al actueel, geen SB4-conflict verwacht)
   - `maven-compiler-plugin` → laatste stabiele versie indien nodig voor Java 25
   - overige compile-fixes die volgen uit Jackson 3 / Jakarta EE 11 (pas uit te
     zoeken bij het effectief bouwen — niet vooraf te voorspellen zonder de build
     te draaien)
2. **Foutafhandeling → RFC 7807 ProblemDetail**:
   - `GlobalExceptionHandler` herschrijven om `ProblemDetail` te retourneren i.p.v.
     de eigen `ErrorResponse`-DTO, per het patroon uit boek-hoofdstuk 3
     (`ProblemDetail.forStatusAndDetail(...)`, custom `type`-URI's per foutsoort,
     validatiefouten als `errors`-extension-property).
   - `ErrorResponse.java` (en de nested `ValidationError`) verwijderen als hij nergens
     anders meer gebruikt wordt.
   - De 4 tests die op de huidige JSON-vorm assert'en (`CustomerControllerTest`,
     `OrderControllerTest`, `PizzaControllerTest`, en validatie-gerelateerde
     assertions) aanpassen naar de nieuwe ProblemDetail-velden (`type`, `title`,
     `status`, `detail`, `instance`, en de custom `errors`-extension).
3. **Verificatie**: `mvn clean verify` moet slagen met alle (aangepaste) tests
   groen, vóór deze wijziging als afgerond beschouwd wordt.

### Expliciet niet in scope (wel gedocumenteerd als aanbeveling)
- Virtual threads inschakelen, `@EnableSpringDataWebSupport`
  (`PageSerializationMode.VIA_DTO`), env-var-gebaseerde secrets voor
  dev/prod-properties.
- Actuator/observability, URL-based API-versioning, Docker Compose voor Postgres,
  RestTestClient als (aanvullende of vervangende) testaanpak.
- Elke andere boek-topic die niet bij lesson-02 t/m lesson-13 hoort (NoSQL, NewSQL,
  reactive, messaging, GraalVM/native, Spring Cloud, Spring AI, custom starters).

### Apart deliverable: tekorten- en aanbevelingenrapport
Een los document (niet in deze spec, wordt na de code-upgrade opgeleverd) met:
1. Volledige lesson-by-lesson dekkingstabel (zoals hierboven, uitgebreid) met
   concrete bestandsverwijzingen.
2. De "quick wins" die de gebruiker koos om NIET nu te implementeren, als
   concrete aanbevelingen met motivatie en verwijzing naar het boek-hoofdstuk.
3. De grotere, cursus-uitbreidende voorstellen (Actuator, API-versioning, Docker
   Compose, RestTestClient) als mogelijke inhoud voor een toekomstige les 14 of
   als verdieping binnen bestaande lessen.

## Risico's / aandachtspunten
- Springdoc 3.x en MapStruct-versienummers zijn gebaseerd op websearch-resultaten
  (kennis-cutoff-onafhankelijk); de exacte werkende combinatie wordt bevestigd door
  de Maven-build effectief te draaien tijdens implementatie, met ruimte om een patch-
  versie bij te stellen indien een dependency-conflict optreedt.
- Jackson 3's package-migratie kan settings in `application.properties`
  (`spring.jackson.serialization.indent-output`, `spring.jackson.time-zone`) raken
  als die properties hernoemd zijn naar `spring.jackson.json.*`. Dit wordt tijdens
  implementatie geverifieerd en zo nodig aangepast.
- `jjwt-jackson` trekt intern nog Jackson 2 (`com.fasterxml.jackson.core`) binnen;
  dit leeft naast Jackson 3 zonder conflict omdat jjwt dit enkel intern gebruikt,
  niet via Spring's `HttpMessageConverter`-keten.
