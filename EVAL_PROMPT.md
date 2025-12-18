# Zelfevaluatie checklist voor Java Spring Boot applicatie

Beoordeel uitsluitend de Java Spring Boot backend volgens onderstaand sjabloon. Geef de ingevulde tabel, testcoverage en aanbevelingen zoals gespecificeerd, zonder scores of totaaloordeel. Resultaat is een markdown bestand met de naam SELFEVALUATION.md. 

**Deel 1: Technisch overzicht (vooraf aan de checklist):**

1. Geef een korte beschrijving (maximum 200 woorden) waar de applicatie over gaat.
2. Vermeld de gebruikte Java- en Spring Boot-versie.
3. **URL van de gedeployde Swagger/OpenAPI-documentatie** (productie, indien beschikbaar)
4. **Alle testgebruikers en rollen**: Lijst op (rolnaam, gebruikersnaam/wachtwoord)
5. **Database**: Welk type database is gebruikt en hoe is deze opgezet? (geen ERD)
6. Geef in tabelvorm een overzicht van alle controllers met bijhorende services, repositories, en entities. Orden de controllers van meest naar minst uitgewerkte functionaliteit.
7. Beschrijf hoe authenticatie is voorzien.

---

**Deel 2: Checklist (alleen Spring Boot/backend):**

- **Enkel beoordelen op basis van de Java & Spring Boot applicatie in deze repo; andere technologieën/folders negeren**
- Per punt:
    - ✅ aanwezig/correct geïmplementeerd
    - ⚠️ gedeeltelijk geïmplementeerd
    - ❌ ontbreekt/niet correct
    - Korte commentaar bij gedeeltelijk/niet aanwezig
- **Helemaal geen percentages, scores of eindconclusies**
- Geef je beoordeling in een tabel als hieronder:

| Nr | Categorie                | Vereiste                                                         | ✅/⚠️/❌ | Opmerking/commentaar |
|----|--------------------------|------------------------------------------------------------------|----------|---------------------|
| 0  | Algemeen                 | Alle code compileert                                             |          |                     |
| 0  | Algemeen                 | Maven build succesvol                                            |          |                     |
| 0  | Algemeen                 | Dependency Injection overal correct gebruikt                     |          |                     |
| 0  | Algemeen                 | Java versie is 25 en minstens Spring Boot 3                      |          |                     |
| 1  | Functionele werking      | App start vlot op met dev-profiel                                |          |                     |
| 1  | Functionele werking      | Alle endpoints functioneren foutloos (CRUD, filtering, security) |          |                     |
| 1  | Functionele werking      | Geen 5xx-fouten bij gebruik                                      |          |                     |
| 2  | Persistentie             | Entiteiten correct geconfigureerd                                |          |                     |
| 2  | Persistentie             | Relaties correct geïmplementeerd                                 |          |                     |
| 2  | Persistentie             | Geen FetchType.EAGER op veel-relaties                            |          |                     |
| 3  | Testen                   | Unit tests voor controllers aanwezig                             |          |                     |
| 3  | Testen                   | Unit tests voor repository-methodes aanwezig                     |          |                     |
| 3  | Testen                   | (Indien van toepassing) Service-lagen getest                     |          |                     |
| 3  | Testen                   | “Happy flows” voor alle endpoints getest                         |          |                     |
| 3  | Testen                   | Edge cases & foutafhandeling getest                              |          |                     |
| 3  | Testen                   | Beveiligde endpoints (verschillende rollen) getest               |          |                     |
| 4  | REST API & documentatie  | Minstens 2 controllers geïmplementeerd                           |          |                     |
| 4  | REST API & documentatie  | Minstens 1 controller met volledige CRUD-functionaliteit         |          |                     |
| 4  | REST API & documentatie  | Minstens 1 controller met child-records benaderbaar              |          |                     |
| 4  | REST API & documentatie  | Correcte HTTP-methodes en statuscodes gebruikt                   |          |                     |
| 4  | REST API & documentatie  | Consistente, duidelijke endpoint-structuur                       |          |                     |
| 4  | REST API & documentatie  | Paginatie aanwezig indien relevant                               |          |                     |
| 4  | REST API & documentatie  | RESTful URL-patterns gevolgd                                     |          |                     |
| 4  | REST API & documentatie  | OpenAPI/Swagger-documentatie volledig & beschikbaar              |          |                     |
| 4  | REST API & documentatie  | Controllers voldoende gedocumenteerd                             |          |                     |
| 4  | REST API & documentatie  | Schema-documentatie voor request/response DTO’s aanw.            |          |                     |
| 4  | REST API & documentatie  | Documentatie toont endpoints met beveiliging/rollen              |          |                     |
| 4  | REST API & documentatie  | Communicatie via DTO’s (nooit directe entities)                  |          |                     |
| 4  | REST API & documentatie  | Request DTO’s steeds voorzien van validatie                      |          |                     |
| 4  | REST API & documentatie  | Inkomende data steeds gevalideerd                                |          |                     |
| 4  | REST API & documentatie  | Foutafhandeling (error handling) geregeld                        |          |                     |
| 5  | Security                 | Authenticatie en autorisatie correct geïmplementeerd             |          |                     |
| 5  | Security                 | Minstens twee rollen voorzien                                    |          |                     |
| 5  | Security                 | Rolgebaseerde autorisatie aanwezig                               |          |                     |
| 5  | Security                 | Standaardgebruikers voor beide rollen aanwezig                   |          |                     |
| 5  | Security                 | Geen plaintext passwords (alle passwords gehashed)               |          |                     |
| 5  | Security                 | Endpoint voor registratie en authenticatie aanwezig              |          |                     |
| 5  | Security                 | Geen conflicterende security rules                               |          |                     |
| 6  | Profielen                | Minstens 2 profielen (`dev`/`prod`) met aparte DB                |          |                     |
| 6  | Profielen                | Correcte profiel-specifieke configuratie                         |          |                     |
| 6  | Profielen                | Gebruik env variabelen voor gevoelige prod-properties            |          |                     |
| 7  | Clean code               | Duidelijke package-structuur, betekenisvolle namen               |          |                     |
| 7  | Clean code               | Correcte laag-scheiding (controller, service, repo, model)       |          |                     |
| 7  | Clean code               | Code voldoet aan Java & Spring Boot best practices               |          |                     |

---

**Deel 3: Testcoverage-overzicht**
- Geef een overzicht van de testcoverage van de Java backend.
- Vermeld, liefst uit Jacoco of een soortgelijk tool, het aantal/geteste klassen & methodes per laag/type.

Voorbeeld output:
Test Coverage:
Repository Tests: 3 classes
  - CustomerRepositoryTest
  - OrderRepositoryTest  
  - PizzaRepositoryTest

Service Tests: 3 classes (47 tests)
  - CustomerServiceTest (14 tests)
  - OrderServiceTest (15 tests)
  - PizzaServiceTest (18 tests)

Controller Tests: 3 classes (43 tests)
  - CustomerControllerTest (9 tests)
  - OrderControllerTest (14 tests)
  - PizzaControllerTest (20 tests)

Integration Tests: 1 class (4 tests)
  - PizzaIntegrationTest (4 tests)

Total: 132 tests - 100% success rate

---

### ⭐️ **Aanbevelingen (max. 5, backend/Spring Boot):**
- Geef concrete suggesties voor verbetering, alleen over de Java-backend.
- Benoem sterke punten en verbeterpunten.
- Geef **geen** algemene conclusie.

---

**Let op:**
- Beoordeel **uitsluitend** de Java/Spring Boot backend.
- Geen scores of totaaloordelen, enkel checken wat aanwezig is of ontbreekt.
