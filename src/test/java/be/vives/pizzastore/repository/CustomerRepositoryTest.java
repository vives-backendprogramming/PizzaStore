package be.vives.pizzastore.repository;

import be.vives.pizzastore.MongoTestcontainersConfiguration;
import be.vives.pizzastore.config.AuditorAwareImpl;
import be.vives.pizzastore.config.MongoConfig;
import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Pizza;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@Import({MongoTestcontainersConfiguration.class, MongoConfig.class, AuditorAwareImpl.class})
class CustomerRepositoryTest {

    private static final String UNKNOWN_ID = new ObjectId().toHexString();

    private Customer createTestCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        customer.setPassword("test123");
        return customer;
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.remove(new Query(), Customer.class);
        mongoTemplate.remove(new Query(), Pizza.class);
    }

    @Test
    void findByEmail_whenExists_shouldReturnCustomer() {
        // Arrange
        mongoTemplate.insert(createTestCustomer("John Doe", "john@example.com"));

        // Act
        Optional<Customer> found = customerRepository.findByEmail("john@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByEmail_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Customer> found = customerRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_whenExists_shouldReturnTrue() {
        // Arrange
        mongoTemplate.insert(createTestCustomer("John Doe", "john@example.com"));

        // Act
        boolean exists = customerRepository.existsByEmail("john@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_whenNotExists_shouldReturnFalse() {
        // Act
        boolean exists = customerRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void save_duplicateEmail_shouldBeRefusedByTheUniqueIndex() {
        // Arrange: @Indexed(unique = true) on email, created because spring.data.mongodb.auto-index-creation=true
        customerRepository.save(createTestCustomer("John Doe", "john@example.com"));

        // Act + Assert: the counterpart of the unique constraint violation in the JPA version
        assertThatThrownBy(() -> customerRepository.save(createTestCustomer("Other John", "john@example.com")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void findById_customerWithFavorites_shouldReturnTheFavoritePizzaIds() {
        // Arrange: the JPA version needs findByIdWithFavorites (JOIN FETCH); here the ids are part of the document
        Pizza pizza1 = mongoTemplate.insert(new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza with tomato and mozzarella"));
        Pizza pizza2 = mongoTemplate.insert(new Pizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pepperoni pizza"));

        Customer customer = createTestCustomer("John Doe", "john@example.com");
        customer.addFavoritePizza(pizza1);
        customer.addFavoritePizza(pizza2);
        customer.addFavoritePizza(pizza1);   // a Set: the same pizza only once, like the join table's primary key
        mongoTemplate.insert(customer);

        // Act
        Optional<Customer> found = customerRepository.findById(customer.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFavoritePizzaIds()).containsExactlyInAnyOrder(pizza1.getId(), pizza2.getId());
    }

    @Test
    void existsByFavoritePizzaIds_shouldTellWhetherAnyCustomerHasThePizzaAsFavorite() {
        // Arrange
        Pizza favorite = mongoTemplate.insert(new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic"));
        Pizza other = mongoTemplate.insert(new Pizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy"));
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        customer.addFavoritePizza(favorite);
        mongoTemplate.insert(customer);

        // Act + Assert
        assertThat(customerRepository.existsByFavoritePizzaIds(favorite.getId())).isTrue();
        assertThat(customerRepository.existsByFavoritePizzaIds(other.getId())).isFalse();
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Customer> found = customerRepository.findById(UNKNOWN_ID);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldPersistCustomer() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");

        // Act
        Customer saved = customerRepository.save(customer);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();   // auditing
        // Note: phone and address are null since we only set name and email in constructor
    }

    @Test
    void deleteById_shouldRemoveCustomer() {
        // Arrange
        Customer customer = mongoTemplate.insert(createTestCustomer("John Doe", "john@example.com"));
        String customerId = customer.getId();

        // Act
        customerRepository.deleteById(customerId);

        // Assert
        Optional<Customer> found = customerRepository.findById(customerId);
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllCustomers() {
        // Arrange
        mongoTemplate.insert(createTestCustomer("John Doe", "john@example.com"));
        mongoTemplate.insert(createTestCustomer("Jane Smith", "jane@example.com"));

        // Act
        var customers = customerRepository.findAll();

        // Assert
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(Customer::getName)
                .contains("John Doe", "Jane Smith");
    }
}
