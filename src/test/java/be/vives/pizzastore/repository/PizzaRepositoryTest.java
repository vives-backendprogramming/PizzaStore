package be.vives.pizzastore.repository;

import be.vives.pizzastore.MongoTestcontainersConfiguration;
import be.vives.pizzastore.config.AuditorAwareImpl;
import be.vives.pizzastore.config.MongoConfig;
import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.domain.Pizza;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataMongoTest: only the MongoDB part of the application (repositories, MongoTemplate), the counterpart of @DataJpaTest.
// Unlike @DataJpaTest it does NOT roll back after each test, and there is no embedded database: see @BeforeEach and
// MongoTestcontainersConfiguration. MongoConfig and AuditorAwareImpl switch on auditing (createdAt, createdBy, ...).
@DataMongoTest
@Import({MongoTestcontainersConfiguration.class, MongoConfig.class, AuditorAwareImpl.class})
class PizzaRepositoryTest {

    // a valid ObjectId that no document has
    private static final String UNKNOWN_ID = new ObjectId().toHexString();

    @Autowired
    private PizzaRepository pizzaRepository;

    // inserts the test data directly, like TestEntityManager in the JPA version: the "given" does not depend on the repository under test
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        // no rollback in MongoDB: start every test with an empty collection
        mongoTemplate.remove(new Query(), Pizza.class);
    }

    @Test
    void findById_ExistingPizza_ReturnsPizza() {
        // Given
        Pizza saved = mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato and mozzarella"));

        // When
        Optional<Pizza> result = pizzaRepository.findById(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Margherita");
        assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("8.50"));
    }

    @Test
    void findById_NonExistingPizza_ReturnsEmpty() {
        // When
        Optional<Pizza> result = pizzaRepository.findById(UNKNOWN_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByPriceLessThan_MultipleResults_ReturnsFilteredList() {
        // Given
        mongoTemplate.insert(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget option"));
        mongoTemplate.insert(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium price"));
        mongoTemplate.insert(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));

        // When
        List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("12.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Cheap Pizza", "Mid Pizza");
    }

    @Test
    void findByPriceLessThan_ComparesNumbersNotText() {
        // Given: as text "10.99" < "8.99", as numbers it is not (see spring.data.mongodb.representation.big-decimal)
        mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.99"), "Classic"));
        mongoTemplate.insert(new Pizza("Pepperoni", new BigDecimal("10.99"), "Spicy"));

        // When
        List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("9.00"));

        // Then
        assertThat(result).extracting(Pizza::getName).containsExactly("Margherita");
    }

    @Test
    void findByNameContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // Given
        mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        mongoTemplate.insert(new Pizza("Marinara", new BigDecimal("7.50"), "Simple"));
        mongoTemplate.insert(new Pizza("Quattro Formaggi", new BigDecimal("11.00"), "Cheese"));

        // When
        List<Pizza> result = pizzaRepository.findByNameContainingIgnoreCase("mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Marinara");
    }

    @Test
    void save_NewPizza_GeneratesId() {
        // Given
        Pizza pizza = new Pizza("Test Pizza", new BigDecimal("9.99"), "Test description");

        // When
        Pizza saved = pizzaRepository.save(pizza);

        // Then: MongoDB generated an ObjectId, Spring Data puts it in the String id as 24 hexadecimal characters
        assertThat(saved.getId()).isNotNull();
        assertThat(ObjectId.isValid(saved.getId())).isTrue();
    }

    @Test
    void save_NewPizza_FillsAuditFields() {
        // When
        Pizza saved = pizzaRepository.save(new Pizza("Test Pizza", new BigDecimal("9.99"), "Test description"));

        // Then: @EnableMongoAuditing (MongoConfig) and AuditorAwareImpl ("system" without a logged-in user)
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
    }

    @Test
    void save_PizzaWithNutritionalInfo_EmbedsItInThePizzaDocument() {
        // Given
        Pizza pizza = new Pizza("Margherita", new BigDecimal("8.99"), "Classic");
        pizza.setNutritionalInfo(new NutritionalInfo(266, new BigDecimal("11.0"), new BigDecimal("33.0"), new BigDecimal("10.0")));

        // When
        Pizza saved = pizzaRepository.save(pizza);

        // Then: the raw BSON document shows how the pizza is stored - one document, the nutritional info nested in it
        // (no nutritional_info table and no foreign key, as with JPA) and the price as a real decimal number
        Document document = mongoTemplate.getCollection("pizzas").find(new Document("_id", new ObjectId(saved.getId()))).first();
        assertThat(document).isNotNull();
        assertThat(document.get("nutritionalInfo", Document.class).getInteger("calories")).isEqualTo(266);
        assertThat(document.get("price")).isEqualTo(new Decimal128(new BigDecimal("8.99")));
        assertThat(mongoTemplate.collectionExists("nutritionalInfo")).isFalse();
    }

    @Test
    void delete_ExistingPizza_RemovesFromDatabase() {
        // Given
        Pizza pizza = mongoTemplate.insert(new Pizza("To Delete", new BigDecimal("10.00"), "Will be deleted"));
        String id = pizza.getId();

        // When
        pizzaRepository.deleteById(id);

        // Then
        Optional<Pizza> result = pizzaRepository.findById(id);
        assertThat(result).isEmpty();
    }

    @Test
    void findByName_ExistingName_ReturnsPizza() {
        // Given
        mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));

        // When
        Optional<Pizza> result = pizzaRepository.findByName("Margherita");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Margherita");
    }

    @Test
    void findByName_NonExistingName_ReturnsEmpty() {
        // When
        Optional<Pizza> result = pizzaRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByPriceGreaterThanEqual_MultipleResults_ReturnsFilteredList() {
        // Given
        mongoTemplate.insert(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget"));
        mongoTemplate.insert(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium"));
        mongoTemplate.insert(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));

        // When
        List<Pizza> result = pizzaRepository.findByPriceGreaterThanEqual(new BigDecimal("10.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Mid Pizza", "Expensive Pizza");
    }

    @Test
    void findByPriceBetween_MultipleResults_ReturnsFilteredList() {
        // Given
        mongoTemplate.insert(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget"));
        mongoTemplate.insert(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium"));
        mongoTemplate.insert(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));

        // When
        List<Pizza> result = pizzaRepository.findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("12.00"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mid Pizza");
    }

    @Test
    void searchByKeyword_MatchesNameOrDescription_ReturnsMatches() {
        // Given
        mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato"));
        mongoTemplate.insert(new Pizza("Marinara", new BigDecimal("7.50"), "Simple pizza"));
        mongoTemplate.insert(new Pizza("Quattro Formaggi", new BigDecimal("11.00"), "Four cheese special"));

        // When - search for "cheese"
        List<Pizza> result = pizzaRepository.searchByKeyword("cheese");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Quattro Formaggi");
    }

    @Test
    void searchByKeyword_MatchesName_ReturnsMatches() {
        // Given
        mongoTemplate.insert(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        mongoTemplate.insert(new Pizza("Marinara", new BigDecimal("7.50"), "Simple"));

        // When - search for part of name
        List<Pizza> result = pizzaRepository.searchByKeyword("Mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Marinara");
    }

    @Test
    void findById_PizzaWithNutritionalInfo_ReturnsItWithoutAJoin() {
        // Given: the JPA version needs findByIdWithNutritionalInfo (JOIN FETCH); here it is part of the document
        Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        pizza.setNutritionalInfo(new NutritionalInfo(266, new BigDecimal("11.0"), new BigDecimal("33.0"), new BigDecimal("10.0")));
        String id = mongoTemplate.insert(pizza).getId();

        // When
        Optional<Pizza> result = pizzaRepository.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNutritionalInfo()).isNotNull();
        assertThat(result.get().getNutritionalInfo().getCalories()).isEqualTo(266);
    }

    @Test
    void findAll_ReturnsAllPizzas() {
        // Given
        mongoTemplate.insert(new Pizza("Pizza 1", new BigDecimal("8.00"), "Desc 1"));
        mongoTemplate.insert(new Pizza("Pizza 2", new BigDecimal("9.00"), "Desc 2"));
        mongoTemplate.insert(new Pizza("Pizza 3", new BigDecimal("10.00"), "Desc 3"));

        // When
        List<Pizza> result = pizzaRepository.findAll();

        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    void existsById_ExistingPizza_ReturnsTrue() {
        // Given
        Pizza pizza = mongoTemplate.insert(new Pizza("Test", new BigDecimal("10.00"), "Test"));

        // When
        boolean exists = pizzaRepository.existsById(pizza.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_NonExistingPizza_ReturnsFalse() {
        // When
        boolean exists = pizzaRepository.existsById(UNKNOWN_ID);

        // Then
        assertThat(exists).isFalse();
    }
}
