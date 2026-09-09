# PizzaStore: tekorten- en aanbevelingenrapport

Datum: 2026-09-09
Bijbehorend ontwerpdocument: `docs/superpowers/specs/2026-09-09-pizzastore-spring-boot-4-upgrade-design.md`
Status: opgeleverd na afronding van de Spring Boot 4-upgrade (`mvn clean verify`: 137/137 tests slagen)

## Inleiding

Dit document is het "aparte deliverable" dat werd aangekondigd in het ontwerpdocument
voor de Spring Boot 4-upgrade van `pizzastore`. Het bevat vier onderdelen:

1. Een volledige dekkingstabel per les (02–13) met concrete bestandsverwijzingen in
   `pizzastore`, uitgebreid met de overlap-analyse tegen *Pro Spring Boot 4*
   (Gutierrez & Carter, Apress, 4th edition, 2026).
2. Een samenvatting van wat de Spring Boot 4-upgrade zelf (taken 1–5) heeft
   opgeleverd en gecorrigeerd.
3. Concrete "quick wins" die bewust **niet** in deze upgrade zijn meegenomen.
4. Grotere, cursus-uitbreidende voorstellen voor een eventuele les 14 of als
   verdieping van bestaande lessen.

Alle bestandspaden zijn relatief aan de root van het `pizzastore`-project.

---

## 1. Dekkingstabel per les (02–13)

| Les | Onderwerp | Boek-hoofdstuk(ken) | Overlap met boek | Concrete bestanden in pizzastore |
|---|---|---|---|---|
| 02 | DI/IoC | H1–H2 | Volledig — constructor injection, `@SpringBootApplication`-internals | `src/main/java/be/vives/pizzastore/PizzaStoreApplication.java`; constructor injection in alle controllers en services, bv. `controller/PizzaController.java`, `service/PizzaService.java`, `security/SecurityConfig.java` (constructor met `JwtAuthenticationFilter`) |
| 03 | Properties/profiles | H2 (Externalized Configuration, Profiles) | Volledig | `src/main/resources/application.properties` (gedeelde config, `spring.profiles.active=dev`), `application-dev.properties` (H2), `application-prod.properties` (PostgreSQL) |
| 04 | Spring Boot intro | H1 | Volledig | `pom.xml` (parent `spring-boot-starter-parent` 4.0.4), `PizzaStoreApplication.java` |
| 05 | Spring MVC | H3 (Request Lifecycle, annotaties) | Volledig | `controller/PizzaController.java`, `controller/OrderController.java`, `controller/CustomerController.java`, `controller/AuthController.java` — `@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestParam` |
| 06 | JPA | H6 (Spring Data JPA-gedeelte) | Volledig | `domain/Pizza.java`, `domain/Customer.java`, `domain/Order.java`, `domain/OrderLine.java`, `domain/NutritionalInfo.java`, `repository/PizzaRepository.java`, `repository/OrderRepository.java`, `repository/CustomerRepository.java`, `config/JpaConfig.java` (`@EnableJpaAuditing`), `config/AuditorAwareImpl.java` |
| 07 | DTO's/mappers | H3 (records als DTO), **geen MapStruct in het boek** | Gedeeltelijk — boek mapt manueel via records, pizzastore gebruikt MapStruct (prima, iets geavanceerder) | `dto/request/*.java`, `dto/response/*.java` (Java records), `mapper/PizzaMapper.java`, `mapper/OrderMapper.java`, `mapper/CustomerMapper.java`, `mapper/NutritionalInfoMapper.java` (MapStruct, `componentModel = "spring"`) |
| 08 | REST-principes | H3 (Core REST Principles, **native API versioning**) | Gedeeltelijk — pizzastore heeft geen API-versioning | Alle controllers in `controller/`; HTTP-methodes en statuscodes correct toegepast, maar geen `/api/v1/...`-structuur — zie aanbeveling 2 in sectie 4 |
| 09 | Complete REST API | H3 (Implementing Full CRUD) | Volledig | `controller/PizzaController.java`, `controller/OrderController.java`, `controller/CustomerController.java` — volledige CRUD, paginatie via `Pageable`, filtering, file upload |
| 10 | Validatie | H3 (Jakarta Bean Validation 3.1) | Volledig | `dto/request/CreatePizzaRequest.java` en overige `dto/request/*.java` (`@NotBlank`, `@NotNull`, `@DecimalMin`), `@Valid @RequestBody` op alle 6 create/update-methodes in `controller/PizzaController.java`, `controller/OrderController.java`, `controller/CustomerController.java` (zie bevinding hieronder), `exception/GlobalExceptionHandler.handleValidationException` |
| 11 | Testing | H10 (Advanced Testing), **`RestTestClient` vervangt MockMvc/WebTestClient** | Gedeeltelijk — pizzastore gebruikt nog uitsluitend MockMvc (nog steeds ondersteund, niet fout) | `src/test/java/be/vives/pizzastore/controller/*Test.java` (`@WebMvcTest` + MockMvc), `src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java` (`@SpringBootTest` + `@AutoConfigureMockMvc`), `src/test/java/be/vives/pizzastore/service/*Test.java` (Mockito), `src/test/java/be/vives/pizzastore/repository/*Test.java` — zie ook aanbeveling 4 in sectie 4 |
| 12 | JWT auth | H11 (Securing Spring Boot) — boek focust op OAuth2/OIDC/OTT/2FA, **geen eigen JWT-filter-implementatie** | Pizzastore's aanpak (zelf een JWT-filter bouwen) is pedagogisch waardevoller voor dit doel — bewust behouden | `security/JwtAuthenticationFilter.java`, `security/JwtUtil.java`, `security/SecurityConfig.java`, `security/CustomUserDetailsService.java`, `controller/AuthController.java` (`/api/auth/register`, `/api/auth/login`) |
| 13 | Swagger/OpenAPI | **Niet in het boek** (0 vermeldingen van "OpenAPI"/"Swagger"/"springdoc" in de volledige tekst) | Geen overlap — pizzastore's springdoc-aanpak blijft de referentie hiervoor | `config/OpenApiConfig.java` (`@OpenAPIDefinition`, `@SecurityScheme`), `@Operation`/`@Schema`-annotaties in alle controllers en DTO's, `springdoc.*`-properties in `application.properties`, bereikbaar op `/swagger-ui/index.html` |

**Belangrijkste architecturale wijzigingen in Spring Boot 4** die het boek beschrijft
(hoofdstuk 18, "What's New in Spring Boot 4") en die pizzastore rechtstreeks raken:
`spring-boot-starter-web` → `spring-boot-starter-webmvc`, Jackson 3
(`tools.jackson`, `JsonMapper`), RFC 7807 `ProblemDetail` als standaard voor
foutresponses, `RestClient`/`@HttpExchange` i.p.v. `RestTemplate`, `RestTestClient`
als unified test-client, virtual threads via één property, declaratieve mTLS via
SSL bundles en een dedicated OTLP-observability-starter. De eerste drie zijn in deze
upgrade daadwerkelijk verwerkt (zie sectie 2); de overige zijn (nog) niet van
toepassing of worden behandeld als aanbeveling in secties 3–4.

---

## 2. Bevindingen uit deze upgrade

Deze upgrade (taken 1–5 van het implementatieplan) heeft pizzastore van Spring Boot
3.5.7 naar **Spring Boot 4.0.4** getild. Alle 137 tests slagen (`mvn clean verify`)
en de applicatie start foutloos op met Swagger UI bereikbaar op
`/swagger-ui/index.html`.

### 2.1 De Spring Boot 4-upgrade zelf (taak 1)

`pom.xml` is aangepast: parent `spring-boot-starter-parent` 3.5.7 → **4.0.4**,
`spring-boot-starter-web` → **`spring-boot-starter-webmvc`** (verplichte hernoeming
in Spring Boot 4), springdoc-openapi 2.8.13 → **3.0.3** (eerste 3.x-lijn die Spring
Boot 4 ondersteunt), en MapStruct 1.5.5.Final → **1.6.3** (laatste stabiele 1.6.x-lijn;
1.7 was op het moment van de upgrade nog beta en is bewust vermeden).

### 2.2 Testinfrastructuur-breuk door Spring Boot 4 (taak 2)

Spring Boot 4 heeft `spring-boot-test-autoconfigure` opgesplitst in aparte
per-technologie-artefacten met nieuwe pakketnamen, wat de bestaande testsuite deed
breken. Dit is opgelost door de **smalle** afhankelijkheden
`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` en
`spring-boot-starter-security-test` toe te voegen — bewust **niet** de brede
`spring-boot-starter-test-classic`-migratieshim, zodat de afhankelijkhedenlijst zelf
instructief en minimaal blijft voor een cursus-referentieproject. Daarnaast is
`ObjectMapper` vervangen door **`JsonMapper`** (Jackson 3) in de vier MockMvc-testbestanden
die deze injecteren (`CustomerControllerTest`, `OrderControllerTest`,
`PizzaControllerTest`, `PizzaIntegrationTest`).

### 2.3 Ontbrekende `@Valid` — een echte bug (taak 3)

Tijdens de upgrade is een reële validatiebug gevonden en gecorrigeerd: zes
controller-methodes accepteerden hun `@RequestBody` **zonder** `@Valid`, waardoor de
Jakarta Bean Validation-annotaties op de request-DTO's nooit effectief werden
uitgevoerd:

- `CustomerController.createCustomer` en `CustomerController.updateCustomer`
- `OrderController.createOrder` en `OrderController.updateOrderStatus`
- `PizzaController.createPizza` en `PizzaController.updatePizza`

Twee tests hadden dit gedrag zelfs gedocumenteerd als **bedoeld** gedrag, met
commentaar als *"Note: Validation is not enforced at controller level without
@Valid annotation"*:

- `PizzaControllerTest.createPizza_InvalidRequest_StillCreates`
- `PizzaIntegrationTest.createPizza_WithInvalidData_StillCreates`

Beide tests zijn gecorrigeerd om nu **400 Bad Request** te verwachten (hernoemd naar
`createPizza_InvalidRequest_ReturnsBadRequest` respectievelijk
`createPizza_WithInvalidData_ReturnsBadRequest`), en `@Valid` is toegevoegd aan alle
zes methodes.

### 2.4 Migratie naar RFC 7807 ProblemDetail (taak 4)

`GlobalExceptionHandler` (`src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java`)
is herschreven van een eigen `ErrorResponse`-DTO naar Spring's ingebouwde
**`ProblemDetail`** (RFC 7807) — exact volgens het patroon uit hoofdstuk 3 van *Pro
Spring Boot 4*. Elke exception-handler retourneert nu een `ProblemDetail` met een
custom `type`-URI per foutsoort (bv. `.../errors/not-found`,
`.../errors/validation`), en validatiefouten worden toegevoegd als een
`errors`-extension-property (`List<Map<String,String>>` met `field`/`message`).
Er zijn 5 nieuwe tests toegevoegd die als eerste ooit het gedrag van
`GlobalExceptionHandler` end-to-end controleren — voordien werd in de hele
testsuite nergens de body van een foutresponse geasserteerd, enkel de HTTP-statuscode.

### 2.5 Kleine, niet-blokkerende vaststellingen

Tijdens de codereview van deze upgrade zijn een aantal kleine, niet-kritieke
observaties genoteerd — geen van alle vereist onmiddellijke actie:

- **`DuplicateResourceException` wordt nooit geworpen.** De klasse
  (`src/main/java/be/vives/pizzastore/exception/DuplicateResourceException.java`)
  bestaat en heeft een handler in `GlobalExceptionHandler`
  (`handleDuplicateResourceException`, retourneert 409 Conflict), maar wordt nergens
  in de hoofdcode effectief gegooid. Dode code, of een gemiste kans — bv. bij
  dubbele e-mailregistratie in `AuthController.register` zou deze exception logisch
  op zijn plaats zijn in plaats van de huidige afhandeling.
- **`GlobalExceptionHandler.handleAccessDeniedException` is in de praktijk
  onbereikbaar.** Er is nergens in de codebase een `@PreAuthorize` of
  `@EnableMethodSecurity` aanwezig; alle 403-responses ontstaan rechtstreeks uit de
  `authorizeHttpRequests`-regels in `security/SecurityConfig.java` en worden
  afgehandeld door Spring Security's eigen filter-keten, vóór de request ooit een
  controller of `@RestControllerAdvice` bereikt. De handler-methode is dus
  functioneel dode code zolang er geen method security wordt toegevoegd.
- **Cosmetisch: import-volgorde.** De `jakarta.validation.Valid`-imports die in
  taak 3 zijn toegevoegd, staan niet alfabetisch geordend ten opzichte van de
  naburige `io.swagger.*`-imports in de drie betrokken controllers. Zuiver
  esthetisch, geen functioneel effect.
- **Deprecated constante in `BusinessException`-handler.** De
  `BusinessException`-handler in `GlobalExceptionHandler` gebruikt nog
  `HttpStatus.UNPROCESSABLE_ENTITY`, die sinds Spring Framework 7 is gemarkeerd als
  `@Deprecated(since="7.0")` — Spring Framework 7 introduceerde de nieuwe canonieke
  `HttpStatus.UNPROCESSABLE_CONTENT`-constante volgens RFC 9110's hernoeming. Dit
  geeft enkel een onschuldige compiler-deprecation-warning; het gedrag is identiek
  in beide gevallen omdat `ProblemDetail` de reason phrase afleidt uit de numerieke
  statuscode, niet uit de gebruikte enum-constante.
- **Paginatie-serialisatiewaarschuwing (niet gerelateerd aan deze upgrade).**
  Tijdens de baseline-testrun (vóór de Spring Boot 4-upgrade, dus reeds aanwezig
  onder Spring Boot 3.5.7) werd volgende waarschuwing opgevangen: *"Serializing
  PageImpl instances as-is is not supported, meaning that there is no guarantee
  about the stability of the resulting JSON structure! For a stable JSON structure,
  please use Spring Data's PagedModel (globally via
  @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))."* Dit is één van
  de "quick wins" die de opdrachtgever bewust niet in deze pass heeft laten
  implementeren — zie sectie 3.

---

## 3. Quick wins (niet geïmplementeerd, aanbevolen)

Onderstaande verbeteringen zijn elk kleinschalig (enkele regels code/configuratie),
maar zijn bewust **niet** meegenomen in deze upgrade om de scope beperkt te houden
tot de Spring Boot 4-migratie zelf. Ze worden hier gedocumenteerd als concrete
aanbevelingen voor een vervolgstap.

### 3.1 Virtual threads inschakelen

**Wat**: één property, `spring.threads.virtual.enabled=true`, toevoegen aan
`application.properties`.
**Waarom**: dit is de vlaggenschip-functionaliteit van Spring Boot 4 zoals
gepresenteerd in hoofdstuk 18 van *Pro Spring Boot 4* ("What's New in Spring Boot
4"), en werkt uitstekend samen met de bestaande Java 25-baseline van dit project —
geen codewijzigingen nodig, enkel de property.
**Boek-referentie**: hoofdstuk 18.

### 3.2 Stabiele paginatie-serialisatie

**Wat**: `@EnableSpringDataWebSupport(pageSerializationMode =
EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)` toevoegen op
`PizzaStoreApplication.java` of op een aparte `@Configuration`-klasse.
**Waarom**: lost de waarschuwing op die tijdens de Task 2-baselinerun is opgevangen
(zie sectie 2.5) — `PageImpl` wordt vandaag rechtstreeks geserialiseerd zonder
garantie op een stabiele JSON-structuur. Dit is een bestaand, van de Spring
Boot 4-upgrade onafhankelijk probleem (al aanwezig onder Spring Boot 3.5.7), en
staat ook al genoteerd als openstaand verbeterpunt in `SELFEVALUATION.md`.
**Boek-referentie**: hoofdstuk 6 (Spring Data JPA / REST-paginatie).

### 3.3 Hardcoded secrets vervangen door environment-variabelen

**Wat**: `application-prod.properties` bevat vandaag een letterlijk, gecommit
productie-databasewachtwoord (`spring.datasource.password=P1zz@St0r3Pr0d2025!`), en
`application.properties` bevat een letterlijk, gecommit JWT-signing-secret
(`jwt.secret=MySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong`) dat
gedeeld wordt over alle profielen. Beide moeten vervangen worden door
environment-variabele-placeholders, bv. `spring.datasource.password=${DB_PASSWORD}`
en `jwt.secret=${JWT_SECRET}`.
**Waarom**: dit is al gekend als openstaand gap — het checklist-item "Gebruik env
variabelen voor gevoelige prod-properties" staat in `SELFEVALUATION.md` reeds op
❌. Het is bovendien de meest concrete security-tekortkoming in het project.
**Boek-referentie**: hoofdstuk 2 (Externalized Configuration).

---

## 4. Voorstellen voor cursusuitbreiding (niet geïmplementeerd)

Onderstaande onderwerpen zijn groter van omvang dan de quick wins hierboven en
lenen zich beter als inhoud voor een toekomstige **les 14**, of als verdieping
binnen een bestaande les, dan als stille wijziging aan het referentieproject.

### 4.1 Spring Boot Actuator + basale observability

Health-checks, `/actuator/info` en basismetrics toevoegen via
`spring-boot-starter-actuator`. Dit is een klassiek, laagdrempelig onderwerp dat
goed aansluit op de bestaande profielenstructuur (dev/prod) en meteen zichtbaar
maakt waarom monitoring/observability relevant is voor een productie-applicatie.
Kandidaat voor een nieuwe les 14, of als verdieping van les 03
(properties/profiles).
**Boek-referentie**: hoofdstuk 13.

### 4.2 URL-based API versioning

Een expliciete versioning-strategie invoeren (bv. `/api/v1/...` naast een
toekomstige `/api/v2/...`) als REST-ontwerp-onderwerp. Het boek behandelt native
API-versioning in Spring Framework 7 als onderdeel van de Core REST
Principles — dit is precies het stuk van hoofdstuk 3 dat pizzastore vandaag nog
niet dekt (zie de dekkingstabel, les 08). Kandidaat als verdieping van les 08
(REST-principes) of les 09 (complete REST API).
**Boek-referentie**: hoofdstuk 3.

### 4.3 Docker Compose voor de PostgreSQL-productieafhankelijkheid

Het project heeft vandaag al een `prod`-profiel dat PostgreSQL target
(`application-prod.properties`), maar geen `docker-compose.yml` om lokaal
daadwerkelijk een PostgreSQL-instantie te draaien. Een minimale Compose-file
(één service, met environment-variabelen die aansluiten bij de aanbeveling in
sectie 3.3) zou studenten toelaten het `prod`-profiel ook effectief lokaal te
testen. Kandidaat voor een nieuwe les 14 (deployment/containerisatie), of als
verdieping van les 03.
**Boek-referentie**: hoofdstuk 18.

### 4.4 RestTestClient als moderne testaanpak

`RestTestClient` (nieuw in Spring Framework 7 / Spring Boot 4) als alternatief of
aanvulling naast de bestaande MockMvc-gebaseerde tests introduceren. Pizzastore's
testsuite gebruikt vandaag uitsluitend MockMvc (nog volledig ondersteund, dus geen
fout, maar niet meer de "unified" aanpak die het boek voorstelt). Een goede
kandidaat om te introduceren in les 11 (testing) naast — niet in vervanging van —
de bestaande MockMvc-tests, zodat studenten beide aanpakken leren kennen.
**Boek-referentie**: hoofdstukken 3 en 10.

---

## Samenvatting

Deze Spring Boot 4-upgrade heeft pizzastore succesvol opgetrokken naar Spring Boot
4.0.4 / Spring Framework 7, heeft onderweg een reële validatiebug gevonden en
gecorrigeerd, en heeft de foutafhandeling gemoderniseerd naar het RFC 7807-patroon
uit *Pro Spring Boot 4*. Het project blijft daarmee een sterke, actuele
referentie-oplossing voor lessen 02–13, met volledige of grotendeels volledige
dekking van de overeenkomstige boek-hoofdstukken. De resterende gaps — quick wins
zoals virtual threads, stabiele paginatie en environment-variabelen voor secrets,
en grotere voorstellen zoals Actuator, API-versioning, Docker Compose en
RestTestClient — zijn stuk voor stuk kleinschalig te implementeren en vormen
concrete, boek-onderbouwde kandidaten voor een volgende iteratie van de cursus.
