# PizzaStore Spring Boot 4 Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade `pizzastore` from Spring Boot 3.5.7 to Spring Boot 4.0.4 (Java stays 25), fix a real validation bug found during the audit, and migrate error handling to RFC 7807 `ProblemDetail` — all while keeping the existing 132 tests green (ending at 137 after the additions below).

**Architecture:** No architectural change. This is a dependency/version upgrade plus two targeted, scoped code changes: (1) add missing `@Valid` annotations so Bean Validation actually runs, (2) replace the custom `ErrorResponse` DTO in `GlobalExceptionHandler` with Spring's built-in `ProblemDetail` (RFC 7807), which is the standard Spring Boot 4 promotes and which the *Pro Spring Boot 4* book uses.

**Tech Stack:** Spring Boot 4.0.4, Java 25, Maven, Spring Data JPA, Spring Security 7, springdoc-openapi 3.0.3, MapStruct 1.6.3, jjwt 0.13.0, JUnit 5 / Mockito / MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-09-pizzastore-spring-boot-4-upgrade-design.md`

## Global Constraints

- Parent POM target: `org.springframework.boot:spring-boot-starter-parent:4.0.4`.
- `spring-boot-starter-web` → `spring-boot-starter-webmvc` (mandatory Spring Boot 4 rename).
- `springdoc-openapi-starter-webmvc-ui` → `3.0.3` (this line targets Spring Boot 4.0.x; do **not** use 3.1.x, which targets Spring Boot 4.1.x).
- `mapstruct` / `mapstruct-processor` → `1.6.3` (latest **stable**; 1.7.x is still beta — do not use it).
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson` stay at `0.13.0` (already current).
- Java stays `25`. **The machine's default `mvn` resolves to JDK 17** (`mvn -v` shows `Java version: 17`), so every Maven command in this plan must be run with `JAVA_HOME=$(/usr/libexec/java_home -v 25)` prefixed, or the build will fail to compile for release 25.
- Baseline before this plan: `132` tests, `0` failures (verified by running the suite before writing this plan). Track the running total as tasks add tests.
- Out of scope (do not implement, only mention in the Task 6 report): virtual threads, `@EnableSpringDataWebSupport`, env-var secrets, Actuator, API versioning, Docker Compose, RestTestClient.
- Every task ends with a passing `mvn test` (or narrower, where stated) before it is considered done.

---

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | Dependency/parent versions |
| `src/main/java/be/vives/pizzastore/controller/CustomerController.java` | Add `@Valid` to `createCustomer`, `updateCustomer` |
| `src/main/java/be/vives/pizzastore/controller/OrderController.java` | Add `@Valid` to `createOrder`, `updateOrderStatus` |
| `src/main/java/be/vives/pizzastore/controller/PizzaController.java` | Add `@Valid` to `createPizza`, `updatePizza` |
| `src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java` | Rewritten to return `ProblemDetail` instead of the custom `ErrorResponse` |
| `src/main/java/be/vives/pizzastore/dto/response/ErrorResponse.java` | Deleted (superseded by `ProblemDetail`) |
| `src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java` | +3 tests: not-found, validation, generic-error ProblemDetail shape |
| `src/test/java/be/vives/pizzastore/controller/OrderControllerTest.java` | +2 tests: business-rule, malformed-JSON ProblemDetail shape |
| `src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java` | 1 test corrected (`createPizza_InvalidRequest_StillCreates` → expects 400, not 201) |
| `src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java` | 1 test corrected (`createPizza_WithInvalidData_StillCreates` → expects 400, not 201) |
| `README.md` | Version references updated to Spring Boot 4.0.4 |
| `docs/tekortenrapport-en-aanbevelingen.md` | New deliverable: lesson-by-lesson gap analysis + recommendations (Task 6) |

---

### Task 1: Upgrade pom.xml to Spring Boot 4.0.4 and get the project compiling

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: a `pom.xml` whose dependency tree resolves against Spring Boot 4.0.4 and whose `mvn compile` succeeds. Later tasks assume this compiles cleanly.

- [ ] **Step 1: Edit pom.xml — bump the parent version**

Change (`pom.xml:11`):
```xml
        <version>3.5.7</version>
```
to:
```xml
        <version>4.0.4</version>
```

- [ ] **Step 2: Edit pom.xml — rename the web starter**

Change (`pom.xml:30-33`):
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```
to:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
```

- [ ] **Step 3: Edit pom.xml — bump springdoc and MapStruct versions**

Change (`pom.xml:23-25`):
```xml
        <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
        <jjwt.version>0.13.0</jjwt.version>
        <springdoc.version>2.8.13</springdoc.version>
```
to:
```xml
        <org.mapstruct.version>1.6.3</org.mapstruct.version>
        <jjwt.version>0.13.0</jjwt.version>
        <springdoc.version>3.0.3</springdoc.version>
```

- [ ] **Step 4: Attempt a clean compile**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q clean compile`

Expected: either `BUILD SUCCESS` with no output, or a `BUILD FAILURE` with dependency-resolution or compile errors printed.

- [ ] **Step 5: If dependency resolution fails (e.g. springdoc 3.0.3 or mapstruct 1.6.3 not resolvable), adjust the version and retry**

Check the exact available versions with:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q dependency:resolve -Dsilent=true 2>&1 | grep -i "could not\|could not resolve"
```
If `springdoc-openapi-starter-webmvc-ui:3.0.3` cannot be resolved, check `https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/` for the closest available `3.0.x` patch and use that instead (do not jump to `3.1.x`, which requires Spring Boot 4.1). If `mapstruct:1.6.3` cannot be resolved, check `https://repo.maven.apache.org/maven2/org/mapstruct/mapstruct/` for the latest `1.6.x` patch and use that.

- [ ] **Step 6: If compilation fails with errors referencing Jackson classes or `spring.jackson.*` properties**

This codebase's only direct Jackson usage is `com.fasterxml.jackson.databind.ObjectMapper` (autowired in 4 test files) and `com.fasterxml.jackson.annotation.JsonInclude` (in `ErrorResponse.java`, which Task 4 deletes anyway). If `mvn compile` (main sources only) fails on the `JsonInclude` import, that will resolve itself once `ErrorResponse.java` is deleted in Task 4 — for now, confirm `mvn compile` (main sources, not test sources) succeeds; test-source compilation is checked in Task 2.

- [ ] **Step 7: Confirm main-source compilation succeeds**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q clean compile`
Expected: no output (success). If it still fails, read the exact error, identify which class/annotation moved (cross-reference the "Jackson 3 Migration" section of the design spec at `docs/superpowers/specs/2026-09-09-pizzastore-spring-boot-4-upgrade-design.md`), and fix the specific import/property — do not guess broadly, fix only what the compiler actually flags.

- [ ] **Step 8: Commit**

```bash
git add pom.xml
git commit -m "$(cat <<'EOF'
Upgrade pizzastore to Spring Boot 4.0.4

Bumps the parent POM, renames spring-boot-starter-web to
spring-boot-starter-webmvc (mandatory Spring Boot 4 rename), and
updates springdoc-openapi and MapStruct to their Spring Boot
4-compatible versions.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Get the full test suite green on Spring Boot 4

**Files:**
- Modify: any test file only if the compiler/runtime specifically flags it (see steps below — do not preemptively rewrite tests)

**Interfaces:**
- Consumes: the compiling `pom.xml` from Task 1.
- Produces: a project where `mvn test` reports the same `132` tests, `0` failures, `0` errors as the pre-upgrade baseline (recorded in Global Constraints). This is the checkpoint before any intentional behavior changes (Tasks 3–4).

- [ ] **Step 1: Run the full test suite**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test`

Expected candidates:
- `BUILD SUCCESS` with `Tests run: 132, Failures: 0, Errors: 0, Skipped: 0` → skip to Step 5.
- `BUILD FAILURE` with test compilation errors or test failures → continue to Step 2.

- [ ] **Step 2: If test compilation fails on `ObjectMapper`**

The 4 test files (`CustomerControllerTest`, `OrderControllerTest`, `PizzaControllerTest`, `PizzaIntegrationTest`) autowire `com.fasterxml.jackson.databind.ObjectMapper`. If the compiler reports this type cannot be found or no bean of that type exists at runtime, Spring Boot 4's `spring-boot-starter-webmvc` still ships the classic Jackson 2 `ObjectMapper` auto-configuration for backward compatibility in the 4.0.x line, so this should compile and wire unchanged. If it genuinely fails, replace the field type in the affected file with `tools.jackson.databind.json.JsonMapper` (the Spring Boot 4 default) and use `objectMapper.writeValueAsString(...)` the same way (the API is source-compatible for this call). Only make this change in files where the compiler/test run actually reports the failure.

- [ ] **Step 3: If a test fails on the `spring.jackson.serialization.indent-output` or `spring.jackson.time-zone` properties (main `application.properties:16-17`) being silently ignored (visible as a `spring-boot-properties-migrator`-style warning, or a JSON formatting assertion failing)**

Add the `spring-boot-properties-migrator` dependency temporarily to see the exact deprecation mapping:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```
Run `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=0 &` briefly (or run one `@SpringBootTest`) to capture the startup log's migration report, apply the reported renames to `src/main/resources/application.properties`, then remove the migrator dependency again — it is a diagnostic tool only, not a permanent dependency.

- [ ] **Step 4: If `spring-security-test` no longer resolves or `@WithMockUser` / `SecurityMockMvcRequestPostProcessors.csrf()` fail at runtime**

Check whether `org.springframework.security:spring-security-test` (currently declared directly in `pom.xml`, not via a starter) still resolves under the Spring Boot 4 BOM. If it fails, replace it in `pom.xml` with:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 5: Re-run until green**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test`
Expected: `Tests run: 132, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
Fix Spring Boot 4 compile/runtime breakage

Resolves whatever Spring Boot 4.0.4 migration issues surfaced when
running the existing test suite (see individual file diffs). No
intentional behavior change — all 132 pre-existing tests still pass.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

(If Steps 2-4 needed no changes at all because the suite was already green in Step 1, skip this commit — there is nothing to commit.)

---

### Task 3: Fix the missing `@Valid` validation bug

**Context:** Only `AuthController` uses `@Valid @RequestBody`. `CustomerController.createCustomer`/`updateCustomer`, `OrderController.createOrder`/`updateOrderStatus`, and `PizzaController.createPizza`/`updatePizza` accept `@RequestBody` **without** `@Valid`, so the Bean Validation annotations on `CreatePizzaRequest`, `CreateCustomerRequest`, `CreateOrderRequest`, `UpdatePizzaRequest` etc. never run. Two existing tests document this as expected behavior and must be corrected to expect the fix.

**Files:**
- Modify: `src/main/java/be/vives/pizzastore/controller/CustomerController.java:155,184-186`
- Modify: `src/main/java/be/vives/pizzastore/controller/OrderController.java:124,154-156`
- Modify: `src/main/java/be/vives/pizzastore/controller/PizzaController.java:130,161-163`
- Modify: `src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java:161-179`
- Modify: `src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java:140-160`

**Interfaces:**
- Produces: all six create/update controller methods now validate their `@RequestBody` before the method executes, throwing `MethodArgumentNotValidException` on failure — the same exception type `GlobalExceptionHandler.handleValidationException` already handles.

- [ ] **Step 1: Confirm the bug with the existing (soon-to-be-wrong) tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q test -Dtest=PizzaControllerTest#createPizza_InvalidRequest_StillCreates`
Expected: `BUILD SUCCESS` — the test currently passes *because* it asserts the buggy behavior (`status().isCreated()` for a blank name and `-5.00` price).

- [ ] **Step 2: Rewrite the PizzaControllerTest to expect the correct behavior**

In `src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java`, replace the test at lines 161-179:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void createPizza_InvalidRequest_StillCreates() throws Exception {
        // Given - invalid request (blank name, negative price)
        // Note: Validation is not enforced at controller level without @Valid annotation
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Short",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "", new BigDecimal("-5.00"), "Short", null, true, null);
        when(pizzaService.create(any(CreatePizzaRequest.class))).thenReturn(response);

        // When / Then - without @Valid, invalid requests are still processed
        mockMvc.perform(post("/api/pizzas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }
```

with:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void createPizza_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given - invalid request (blank name, negative price)
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Short",
                true,
                null
        );

        // When / Then - @Valid rejects the request before it reaches the service
        mockMvc.perform(post("/api/pizzas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(pizzaService, never()).create(any(CreatePizzaRequest.class));
    }
```

This test uses `verify(..., never())`, which needs a new static import. `PizzaControllerTest.java` currently has separate `import static org.mockito.Mockito.verify;` and `import static org.mockito.Mockito.when;` lines (not a wildcard import) — add a third line right after them:
```java
import static org.mockito.Mockito.never;
```

- [ ] **Step 3: Run it to confirm it now fails (the fix isn't in yet)**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q test -Dtest=PizzaControllerTest#createPizza_InvalidRequest_ReturnsBadRequest`
Expected: `BUILD FAILURE` — the controller still returns 201 because `@Valid` hasn't been added yet.

- [ ] **Step 4: Add `@Valid` to the six controller methods**

In `src/main/java/be/vives/pizzastore/controller/PizzaController.java`, add the import:
```java
import jakarta.validation.Valid;
```
and change line 130 from:
```java
    public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
```
to:
```java
    public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
```
and change (around line 163) from:
```java
    public ResponseEntity<PizzaResponse> updatePizza(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id,
            @RequestBody UpdatePizzaRequest request) {
```
to:
```java
    public ResponseEntity<PizzaResponse> updatePizza(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdatePizzaRequest request) {
```

In `src/main/java/be/vives/pizzastore/controller/CustomerController.java`, add the import:
```java
import jakarta.validation.Valid;
```
and change line 155 from:
```java
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CreateCustomerRequest request) {
```
to:
```java
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
```
and change (around line 184-186) from:
```java
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @RequestBody UpdateCustomerRequest request) {
```
to:
```java
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
```

In `src/main/java/be/vives/pizzastore/controller/OrderController.java`, add the import:
```java
import jakarta.validation.Valid;
```
and change line 124 from:
```java
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
```
to:
```java
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
```
and change (around line 154-156) from:
```java
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request) {
```
to:
```java
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
```

- [ ] **Step 5: Run the PizzaControllerTest fix to confirm it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q test -Dtest=PizzaControllerTest#createPizza_InvalidRequest_ReturnsBadRequest`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Fix the integration test the same way**

In `src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java`, replace the test at lines 140-160:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void createPizza_WithInvalidData_StillCreates() throws Exception {
        // Given - Invalid pizza (multiple validation errors)
        // Note: Validation is not enforced at controller level without @Valid annotation
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",  // Blank - would be invalid with @Valid
                new BigDecimal("-5.00"),  // Negative - would be invalid with @Valid
                "Test description",
                true,
                null
        );

        // When / Then - without @Valid, invalid requests are still processed
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("")))
                .andExpect(jsonPath("$.price", is(-5.00)));
    }
```

with:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void createPizza_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Given - Invalid pizza (blank name, negative price)
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Test description",
                true,
                null
        );

        // When / Then - @Valid rejects the request; nothing is persisted
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
```

- [ ] **Step 7: Run the full suite**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test`
Expected: `Tests run: 132, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` (same count as Task 2 — two tests were corrected, not added).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/be/vives/pizzastore/controller/CustomerController.java \
        src/main/java/be/vives/pizzastore/controller/OrderController.java \
        src/main/java/be/vives/pizzastore/controller/PizzaController.java \
        src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java \
        src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java
git commit -m "$(cat <<'EOF'
Fix missing @Valid on create/update endpoints

createCustomer, updateCustomer, createOrder, updateOrderStatus,
createPizza and updatePizza accepted @RequestBody without @Valid, so
the Bean Validation constraints on the request DTOs (@NotBlank,
@DecimalMin, etc.) never ran. Two tests documented this as expected
behavior; they now assert the correct 400 response instead.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Migrate error handling to RFC 7807 ProblemDetail

**Context:** `GlobalExceptionHandler` currently builds a custom `ErrorResponse` record (`timestamp`, `status`, `error`, `message`, `path`, `validationErrors`). None of the existing controller tests assert on this body's fields (only on HTTP status codes), so this migration is safe for existing tests — but there is currently **no test at all** exercising `GlobalExceptionHandler`'s behavior end-to-end (the "not found" tests use `Optional`/`null` returns, not thrown exceptions). This task adds that missing coverage using the new `ProblemDetail` shape, TDD-style: write the failing tests first, then implement.

**Files:**
- Modify: `src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java` (add 3 tests)
- Modify: `src/test/java/be/vives/pizzastore/controller/OrderControllerTest.java` (add 2 tests)
- Modify: `src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java` (full rewrite)
- Delete: `src/main/java/be/vives/pizzastore/dto/response/ErrorResponse.java`

**Interfaces:**
- Produces: `GlobalExceptionHandler`'s seven `@ExceptionHandler` methods now return `org.springframework.http.ProblemDetail` instead of `ResponseEntity<ErrorResponse>`. Response bodies use RFC 7807 fields: `type` (URI, `https://api.pizzastore.example.com/errors/{slug}`), `title` (auto-derived from the HTTP status reason phrase), `status`, `detail`, `instance` (the request path), and — for validation errors only — an `errors` extension property (`List<Map<String,String>>` of `{field, message}`).

- [ ] **Step 1: Add the failing ResourceNotFoundException test to CustomerControllerTest**

In `src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java`, add the import:
```java
import be.vives.pizzastore.exception.ResourceNotFoundException;
```
Then add this test after `getCustomer_whenExists_shouldReturnCustomer` (after line 105):

```java
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenNotExists_shouldReturnProblemDetail() throws Exception {
        // Arrange
        when(customerService.findById(999L)).thenThrow(new ResourceNotFoundException("Customer", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Not Found")))
                .andExpect(jsonPath("$.detail", is("Customer with id 999 not found")))
                .andExpect(jsonPath("$.type", is("https://api.pizzastore.example.com/errors/not-found")))
                .andExpect(jsonPath("$.instance", is("/api/customers/999")));

        verify(customerService).findById(999L);
    }
```

- [ ] **Step 2: Add the failing validation test to CustomerControllerTest**

Add this test after the one from Step 1:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_withInvalidEmail_shouldReturnValidationProblemDetail() throws Exception {
        // Arrange - every field is valid except email, so exactly one violation is produced
        // (name="Jane Doe" satisfies @NotBlank+@Size, password="password123" satisfies
        // @NotBlank+@Size(min=8), phone/address satisfy their @Size(max=...) constraints)
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane Doe", "not-an-email", "password123", "1234567890", "123 Main St");

        // Act & Assert
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Validation failed")))
                .andExpect(jsonPath("$.type", is("https://api.pizzastore.example.com/errors/validation")))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("email")));

        verify(customerService, never()).create(any());
    }
```

- [ ] **Step 3: Add the failing generic-error test to CustomerControllerTest**

Add this test after the one from Step 2:

```java
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenUnexpectedErrorOccurs_shouldReturnProblemDetail() throws Exception {
        // Arrange
        when(customerService.findById(1L)).thenThrow(new IllegalStateException("boom"));

        // Act & Assert
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.type", is("https://api.pizzastore.example.com/errors/internal-error")));

        verify(customerService).findById(1L);
    }
```

- [ ] **Step 4: Run the three new tests to confirm they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q test -Dtest=CustomerControllerTest`
Expected: `BUILD FAILURE` — the handler still returns the old `ErrorResponse` shape (no `type`/`title`/`instance` fields, and no `application/problem+json` content type).

- [ ] **Step 5: Add the failing BusinessException test to OrderControllerTest**

In `src/test/java/be/vives/pizzastore/controller/OrderControllerTest.java`, add the import:
```java
import be.vives.pizzastore.exception.BusinessException;
```
Then add this test after `createOrder_shouldReturnCreatedOrder` (after line 203):

```java
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_whenCustomerNotFound_shouldReturnProblemDetail() throws Exception {
        // Arrange
        CreateOrderRequest.OrderLineRequest lineRequest = new CreateOrderRequest.OrderLineRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest(999L, List.of(lineRequest));

        when(orderService.create(any(CreateOrderRequest.class)))
                .thenThrow(new BusinessException("Customer with id 999 not found"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.title", is("Unprocessable Entity")))
                .andExpect(jsonPath("$.detail", is("Customer with id 999 not found")))
                .andExpect(jsonPath("$.type", is("https://api.pizzastore.example.com/errors/business-rule")));

        verify(orderService).create(any(CreateOrderRequest.class));
    }
```

- [ ] **Step 6: Add the failing malformed-JSON test to OrderControllerTest**

Add this test after `updateOrderStatus_whenNotExists_shouldReturnNotFound` (after line 242):

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_withInvalidStatusValue_shouldReturnProblemDetail() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/orders/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"NOT_A_REAL_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is(
                        "Invalid OrderStatus value. Allowed values: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED")))
                .andExpect(jsonPath("$.type", is("https://api.pizzastore.example.com/errors/malformed-request")));

        verify(orderService, never()).updateStatus(any(), any());
    }
```

- [ ] **Step 7: Run the two new OrderControllerTest tests to confirm they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q test -Dtest=OrderControllerTest`
Expected: `BUILD FAILURE` for the same reason as Step 4.

- [ ] **Step 8: Rewrite GlobalExceptionHandler to return ProblemDetail**

Replace the entire contents of `src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java` with:

```java
package be.vives.pizzastore.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_BASE = "https://api.pizzastore.example.com/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed for request: {}", request.getDescription(false));

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("field", error.getField());
                    entry.put("message", error.getDefaultMessage());
                    return entry;
                })
                .toList();

        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "validation", request);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "conflict", request);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business logic error: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "business-rule", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        String detail = "Malformed JSON request";
        if (ex.getCause() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage != null && causeMessage.contains("OrderStatus")) {
                detail = "Invalid OrderStatus value. Allowed values: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED";
            }
        }

        return buildProblemDetail(HttpStatus.BAD_REQUEST, detail, "malformed-request", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN,
                "Access denied. You do not have permission to access this resource.", "access-denied", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.", "internal-error", request);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String typeSlug, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + typeSlug));
        problemDetail.setInstance(URI.create(extractPath(request)));
        return problemDetail;
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
```

- [ ] **Step 9: Delete the now-unused ErrorResponse DTO**

```bash
git rm src/main/java/be/vives/pizzastore/dto/response/ErrorResponse.java
```

- [ ] **Step 10: Run the full test suite**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test`
Expected: `Tests run: 137, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` (132 baseline + 5 new tests from Steps 1-3 and 5-6).

- [ ] **Step 11: Commit**

```bash
git add src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java \
        src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java \
        src/test/java/be/vives/pizzastore/controller/OrderControllerTest.java
git commit -m "$(cat <<'EOF'
Migrate GlobalExceptionHandler to RFC 7807 ProblemDetail

Replaces the custom ErrorResponse DTO with Spring's built-in
ProblemDetail, the error-handling standard Spring Boot 4 and the
Pro Spring Boot 4 book both promote. Adds the first tests that
actually exercise GlobalExceptionHandler end-to-end (previously only
HTTP status codes were asserted, never the error body).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Final verification and README update

**Files:**
- Modify: `README.md:8` (version reference)

**Interfaces:**
- Produces: a fully verified, documented Spring Boot 4.0.4 project — the deliverable this whole plan builds toward.

- [ ] **Step 1: Update the version reference in README.md**

Change (`README.md:8`):
```
- **Spring Boot 3.5.7**
```
to:
```
- **Spring Boot 4.0.4**
```

- [ ] **Step 2: Run the full verification build**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn clean verify`
Expected: `BUILD SUCCESS`, `Tests run: 137, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 3: Start the application once and manually confirm it boots**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn spring-boot:run` (in the background, or in a terminal you can stop with Ctrl+C after confirming)
Expected: log line `Started PizzaStoreApplication` with no stack traces, and `http://localhost:8080/swagger-ui.html` reachable (confirms springdoc 3.0.3 wired up correctly under Spring Boot 4).

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "$(cat <<'EOF'
Update README version reference to Spring Boot 4.0.4

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Write the gap-analysis and recommendations report

**Files:**
- Create: `docs/tekortenrapport-en-aanbevelingen.md`

**Interfaces:**
- Produces: the separate deliverable described in the design spec's "Apart deliverable" section — a Dutch-language report for the course author, not code.

- [ ] **Step 1: Write the report**

Create `docs/tekortenrapport-en-aanbevelingen.md` with these sections (write full prose/tables for each, not placeholders):

1. **Dekkingstabel per les (2–13)** — reuse and expand the table from the design spec (`docs/superpowers/specs/2026-09-09-pizzastore-spring-boot-4-upgrade-design.md`), adding a column with concrete file references in `pizzastore` for each lesson's concepts.
2. **Bevindingen uit deze upgrade** — summarize what Tasks 1-5 fixed: the Spring Boot 4 upgrade itself, the missing `@Valid` bug (name the 6 methods and the 2 tests that documented it), and the ProblemDetail migration. Note the still-unused `DuplicateResourceException` and the currently-unreachable `AccessDeniedException` handler branch (no `@PreAuthorize`/method security in the codebase, so all 403s come from `SecurityConfig.authorizeHttpRequests` directly, never through `GlobalExceptionHandler`) as minor follow-up observations.
3. **Quick wins (niet geïmplementeerd, aanbevolen)** — virtual threads (`spring.threads.virtual.enabled=true`), the `PageImpl` pagination warning fix (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`, referencing the exact warning text captured during the Task 2 baseline run), and replacing the hardcoded `application-prod.properties` database password and the `application.properties` JWT secret with environment-variable placeholders (e.g. `${DB_PASSWORD}`, `${JWT_SECRET}`), each with a one-line motivation and its book chapter/page reference.
4. **Voorstellen voor cursusuitbreiding (niet geïmplementeerd)** — Actuator/health-checks (book ch. 13), URL-based API versioning (book ch. 3), Docker Compose for the `prod` PostgreSQL profile (book ch. 18), and RestTestClient as a modern testing option alongside MockMvc (book ch. 3 & 10), each framed as candidate content for a future lesson 14 or as an enrichment of an existing lesson.

- [ ] **Step 2: Commit**

```bash
git add docs/tekortenrapport-en-aanbevelingen.md
git commit -m "$(cat <<'EOF'
Add gap-analysis and recommendations report

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review Notes

- **Spec coverage:** Task 1 covers the pom.xml upgrade section of the spec; Task 2 covers "get compile-fixes done"; Task 3 covers the newly-agreed `@Valid` fix; Task 4 covers the agreed ProblemDetail migration; Task 5 covers the spec's verification requirement; Task 6 covers the "apart deliverable" gap report. All spec sections are covered.
- **Type consistency:** `ProblemDetail`, `buildProblemDetail(HttpStatus, String, String, WebRequest)`, and the `errors` property key (`List<Map<String,String>>` with `field`/`message` keys) are used identically across Task 4's test assertions and the `GlobalExceptionHandler` implementation.
- **Placeholder scan:** no TBD/TODO markers; every step carries literal file paths, line numbers, and full code blocks; build-fix steps (Task 1 Steps 5-7, Task 2 Steps 2-4) are conditional/diagnostic by necessity (live dependency resolution cannot be predicted offline) but specify exact commands, exact known candidate fixes, and exact expected output for each branch.
