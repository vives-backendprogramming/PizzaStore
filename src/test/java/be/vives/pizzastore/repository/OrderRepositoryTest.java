package be.vives.pizzastore.repository;

import be.vives.pizzastore.MongoTestcontainersConfiguration;
import be.vives.pizzastore.config.AuditorAwareImpl;
import be.vives.pizzastore.config.MongoConfig;
import be.vives.pizzastore.domain.*;
import be.vives.pizzastore.repository.projection.PizzaSalesStatistics;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import({MongoTestcontainersConfiguration.class, MongoConfig.class, AuditorAwareImpl.class})
class OrderRepositoryTest {

    private static final String UNKNOWN_ID = new ObjectId().toHexString();

    // Order's constructor copies the customer's id and name: the customer must be saved first, so it has an id
    private Customer createTestCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        customer.setPassword("test123");
        return mongoTemplate.insert(customer);
    }

    private Pizza createTestPizza(String name, BigDecimal price, String description) {
        return mongoTemplate.insert(new Pizza(name, price, description));
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.remove(new Query(), Order.class);
        mongoTemplate.remove(new Query(), Customer.class);
        mongoTemplate.remove(new Query(), Pizza.class);
    }

    @Test
    void findByCustomerId_shouldReturnCustomerOrders() {
        // Arrange
        Customer customer1 = createTestCustomer("John Doe", "john@example.com");
        Customer customer2 = createTestCustomer("Jane Smith", "jane@example.com");

        mongoTemplate.insert(new Order("ORD-2024-000001", customer1, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000002", customer1, OrderStatus.DELIVERED));
        mongoTemplate.insert(new Order("ORD-2024-000003", customer2, OrderStatus.PENDING));

        // Act
        List<Order> orders = orderRepository.findByCustomerId(customer1.getId());

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getOrderNumber)
                .containsExactlyInAnyOrder("ORD-2024-000001", "ORD-2024-000002");
    }

    @Test
    void findByCustomerIdWithPageable_shouldReturnOnlyCustomerOrdersPaged() {
        // Arrange
        Customer customer1 = createTestCustomer("John Doe", "john@example.com");
        Customer customer2 = createTestCustomer("Jane Smith", "jane@example.com");

        mongoTemplate.insert(new Order("ORD-2024-000001", customer1, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000002", customer2, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000003", customer1, OrderStatus.DELIVERED));
        mongoTemplate.insert(new Order("ORD-2024-000004", customer1, OrderStatus.CONFIRMED));

        // Act
        Page<Order> page = orderRepository.findByCustomerId(customer1.getId(),
                PageRequest.of(0, 2, Sort.by("orderNumber")));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Order::getOrderNumber)
                .containsExactly("ORD-2024-000001", "ORD-2024-000003");
    }

    @Test
    void findByStatus_shouldReturnOrdersWithStatus() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");

        mongoTemplate.insert(new Order("ORD-2024-000001", customer, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000002", customer, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000003", customer, OrderStatus.DELIVERED));

        // Act
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        // Assert
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).extracting(Order::getStatus)
                .containsOnly(OrderStatus.PENDING);
    }

    @Test
    void findByOrderNumber_whenExists_shouldReturnOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        mongoTemplate.insert(new Order("ORD-2024-000001", customer, OrderStatus.PENDING));

        // Act
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-2024-000001");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-2024-000001");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByOrderNumber_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-NONEXISTENT");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findById_shouldReturnOrderWithItsEmbeddedLines() {
        // Arrange: the JPA version needs findByIdWithOrderLines (JOIN FETCH); here the lines are part of the document
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza pizza1 = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Pizza pizza2 = createTestPizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        order.addOrderLine(new OrderLine(pizza1, 2));
        order.addOrderLine(new OrderLine(pizza2, 1));
        mongoTemplate.insert(order);

        // Act
        Optional<Order> found = orderRepository.findById(order.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOrderLines()).hasSize(2);
        assertThat(found.get().getOrderLines()).extracting(OrderLine::getPizzaName)
                .containsExactlyInAnyOrder("Margherita", "Pepperoni");
        assertThat(found.get().getOrderLines()).extracting(OrderLine::getPizzaId)
                .containsExactlyInAnyOrder(pizza1.getId(), pizza2.getId());
        assertThat(found.get().getOrderLines()).extracting(OrderLine::getQuantity)
                .containsExactlyInAnyOrder(2, 1);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo("27.00");
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Order> found = orderRepository.findById(UNKNOWN_ID);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldStoreTheOrderLinesInsideTheOrderDocument() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza pizza = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        order.addOrderLine(new OrderLine(pizza, 2));

        // Act
        Order saved = orderRepository.save(order);

        // Assert: the raw document - the lines are an array inside the order, there is no order_lines collection
        Document document = mongoTemplate.getCollection("orders").find(new Document("_id", new ObjectId(saved.getId()))).first();
        assertThat(document).isNotNull();
        assertThat(document.getString("customerId")).isEqualTo(customer.getId());
        assertThat(document.getString("customerName")).isEqualTo("John Doe");
        assertThat(document.getString("status")).isEqualTo("PENDING");
        List<Document> lines = document.getList("orderLines", Document.class);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getString("pizzaName")).isEqualTo("Margherita");
        assertThat(mongoTemplate.collectionExists("orderLine")).isFalse();
    }

    @Test
    void customerName_isASnapshotTakenWhenTheOrderIsPlaced() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Order order = mongoTemplate.insert(new Order("ORD-2024-000001", customer, OrderStatus.PENDING));

        // Act: the customer changes their name afterwards
        customer.setName("John Renamed");
        mongoTemplate.save(customer);

        // Assert: the extended reference in the order keeps the name of the moment the order was placed
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void save_shouldPersistOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);

        // Act
        Order saved = orderRepository.save(order);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-2024-000001");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomerId()).isEqualTo(customer.getId());
        assertThat(saved.getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void existsByIdAndCustomerId_shouldOnlyBeTrueForTheCustomerWhoPlacedTheOrder() {
        // Arrange
        Customer owner = createTestCustomer("John Doe", "john@example.com");
        Customer other = createTestCustomer("Jane Smith", "jane@example.com");
        Order order = mongoTemplate.insert(new Order("ORD-2024-000001", owner, OrderStatus.PENDING));

        // Act + Assert
        assertThat(orderRepository.existsByIdAndCustomerId(order.getId(), owner.getId())).isTrue();
        assertThat(orderRepository.existsByIdAndCustomerId(order.getId(), other.getId())).isFalse();
        assertThat(orderRepository.existsByIdAndCustomerId(UNKNOWN_ID, owner.getId())).isFalse();
    }

    @Test
    void existsByOrderLinesPizzaId_shouldLookInsideTheEmbeddedOrderLines() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza ordered = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Pizza neverOrdered = createTestPizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        order.addOrderLine(new OrderLine(ordered, 1));
        mongoTemplate.insert(order);

        // Act + Assert
        assertThat(orderRepository.existsByOrderLinesPizzaId(ordered.getId())).isTrue();
        assertThat(orderRepository.existsByOrderLinesPizzaId(neverOrdered.getId())).isFalse();
    }

    @Test
    void deleteByCustomerId_shouldOnlyDeleteTheOrdersOfThatCustomer() {
        // Arrange
        Customer customer1 = createTestCustomer("John Doe", "john@example.com");
        Customer customer2 = createTestCustomer("Jane Smith", "jane@example.com");
        mongoTemplate.insert(new Order("ORD-2024-000001", customer1, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000002", customer1, OrderStatus.DELIVERED));
        mongoTemplate.insert(new Order("ORD-2024-000003", customer2, OrderStatus.PENDING));

        // Act
        orderRepository.deleteByCustomerId(customer1.getId());

        // Assert
        assertThat(orderRepository.findAll()).extracting(Order::getOrderNumber).containsExactly("ORD-2024-000003");
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Order order = mongoTemplate.insert(new Order("ORD-2024-000001", customer, OrderStatus.PENDING));
        String orderId = order.getId();

        // Act
        orderRepository.deleteById(orderId);

        // Assert
        Optional<Order> found = orderRepository.findById(orderId);
        assertThat(found).isEmpty();
    }

    @Test
    void findPizzaSalesStatistics_shouldAggregateQuantityAndRevenuePerPizza() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza margherita = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Pizza pepperoni = createTestPizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");

        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.DELIVERED);
        order1.addOrderLine(new OrderLine(margherita, 2));
        order1.addOrderLine(new OrderLine(pepperoni, 1));
        mongoTemplate.insert(order1);

        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED);
        order2.addOrderLine(new OrderLine(margherita, 1));
        mongoTemplate.insert(order2);

        // Act
        List<PizzaSalesStatistics> statistics = orderRepository.findPizzaSalesStatistics();

        // Assert: sorted by quantity sold, most sold first
        assertThat(statistics).extracting(PizzaSalesStatistics::pizzaName).containsExactly("Margherita", "Pepperoni");

        PizzaSalesStatistics margheritaStats = statistics.get(0);
        assertThat(margheritaStats.timesOrdered()).isEqualTo(2L);
        assertThat(margheritaStats.totalQuantitySold()).isEqualTo(3L);
        assertThat(margheritaStats.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(25.50));

        PizzaSalesStatistics pepperoniStats = statistics.get(1);
        assertThat(pepperoniStats.timesOrdered()).isEqualTo(1L);
        assertThat(pepperoniStats.totalQuantitySold()).isEqualTo(1L);
        assertThat(pepperoniStats.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
    }

    @Test
    void count_shouldReturnNumberOfOrders() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");

        long initialCount = orderRepository.count();

        mongoTemplate.insert(new Order("ORD-2024-000001", customer, OrderStatus.PENDING));
        mongoTemplate.insert(new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED));

        // Act
        long finalCount = orderRepository.count();

        // Assert
        assertThat(finalCount).isEqualTo(initialCount + 2);
    }
}
