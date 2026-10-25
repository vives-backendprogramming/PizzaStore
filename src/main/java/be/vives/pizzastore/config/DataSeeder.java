package be.vives.pizzastore.config;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderLine;
import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.domain.Role;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The dummy data of data.sql on the main branch. data.sql is SQL, run by Spring Boot's SQL initialization against a
 * relational database; MongoDB has no such mechanism. The usual replacement is a {@link CommandLineRunner} that saves
 * the data through the repositories once the application has started.
 * <p>
 * Only in the dev profile (like data.sql, which only ran for the embedded H2 database), and only when the database is
 * still empty: unlike H2 in memory, a MongoDB database survives a restart.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // "password123" encoded with BCrypt; the {bcrypt} prefix tells Spring Security's DelegatingPasswordEncoder
    // which algorithm made the hash (see SecurityConfig.passwordEncoder)
    // For testing: email: emma.johnson@example.com, password: password123
    // For admin: email: admin@pizzastore.be, password: password123
    private static final String PASSWORD_HASH = "{bcrypt}$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri";

    private final PizzaRepository pizzaRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public DataSeeder(PizzaRepository pizzaRepository, CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.pizzaRepository = pizzaRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (pizzaRepository.count() > 0) {
            log.info("Database already contains data, skipping the seed data");
            return;
        }

        // Pizzas, each with its nutritional info embedded
        Pizza margherita = pizza("Margherita", "8.99", "Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil",
                "https://images.unsplash.com/photo-1574071318508-1cdbab80d002", 266, "11.0", "33.0", "10.0");
        Pizza pepperoni = pizza("Pepperoni", "10.99", "Tomato sauce, mozzarella, and spicy pepperoni slices",
                "https://images.unsplash.com/photo-1628840042765-356cda07504e", 298, "13.5", "36.0", "12.5");
        Pizza quattroFormaggi = pizza("Quattro Formaggi", "11.99", "Four cheese blend: mozzarella, gorgonzola, parmesan, and fontina",
                "https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f", 320, "15.0", "35.0", "14.0");
        Pizza vegetariana = pizza("Vegetariana", "9.99", "Fresh vegetables: bell peppers, mushrooms, onions, tomatoes, and olives",
                "https://images.unsplash.com/photo-1627626775846-122c3f8e3e3b", 245, "9.0", "38.0", "8.5");
        Pizza diavola = pizza("Diavola", "12.99", "Spicy salami, hot peppers, tomato sauce, and mozzarella",
                "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e", 310, "14.0", "36.0", "13.0");
        Pizza hawaii = pizza("Hawaii", "11.49", "Ham, pineapple, tomato sauce, and mozzarella",
                "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38", 275, "12.0", "39.0", "9.5");
        Pizza funghi = pizza("Funghi", "10.49", "Mushrooms, tomato sauce, mozzarella, and fresh herbs",
                "https://images.unsplash.com/photo-1513104890138-7c749659a591", 255, "10.5", "35.0", "9.0");
        Pizza prosciutto = pizza("Prosciutto", "13.99", "Italian prosciutto, arugula, parmesan shavings, and mozzarella",
                "https://images.unsplash.com/photo-1571407970349-bc81e7e96a47", 295, "16.0", "33.0", "11.5");
        Pizza marinara = pizza("Marinara", "7.99", "Tomato sauce, garlic, oregano, and extra virgin olive oil (no cheese)",
                null, 200, "6.0", "35.0", "5.0");
        Pizza capricciosa = pizza("Capricciosa", "12.49", "Ham, mushrooms, artichokes, olives, tomato sauce, and mozzarella",
                "https://images.unsplash.com/photo-1595854341625-f33ee10dbf94", 285, "13.0", "37.0", "10.5");
        Pizza bbqChicken = pizza("BBQ Chicken", "13.49", "Grilled chicken, BBQ sauce, red onions, and mozzarella",
                null, 310, "18.0", "34.0", "12.0");
        Pizza tonno = pizza("Tonno", "11.99", "Tuna, red onions, capers, tomato sauce, and mozzarella",
                null, 270, "14.0", "34.0", "9.5");

        // saveAll() fills in the generated ids, which the customers and orders below refer to
        pizzaRepository.saveAll(List.of(margherita, pepperoni, quattroFormaggi, vegetariana, diavola, hawaii,
                funghi, prosciutto, marinara, capricciosa, bbqChicken, tonno));

        // Customers, each with the ids of its favorite pizzas
        Customer emma = customer("Emma Johnson", "emma.johnson@example.com", "+32 470 12 34 56", "Rue de la Loi 123, 1000 Brussels",
                Role.CUSTOMER, margherita, quattroFormaggi, diavola);
        Customer liam = customer("Liam Smith", "liam.smith@example.com", "+32 471 23 45 67", "Meir 45, 2000 Antwerp",
                Role.CUSTOMER, pepperoni, diavola, bbqChicken);
        Customer olivia = customer("Olivia Brown", "olivia.brown@example.com", "+32 472 34 56 78", "Korenmarkt 12, 9000 Ghent",
                Role.CUSTOMER, margherita, vegetariana, marinara);
        Customer noah = customer("Noah Davis", "noah.davis@example.com", "+32 473 45 67 89", "Grand Place 1, 7000 Mons",
                Role.CUSTOMER, pepperoni, hawaii, prosciutto);
        Customer ava = customer("Ava Wilson", "ava.wilson@example.com", "+32 474 56 78 90", "Boulevard Tirou 89, 6000 Charleroi",
                Role.CUSTOMER, quattroFormaggi, funghi, capricciosa);
        Customer admin = customer("Admin User", "admin@pizzastore.be", "+32 475 67 89 01", "Headquarters, 1000 Brussels",
                Role.ADMIN);

        customerRepository.saveAll(List.of(emma, liam, olivia, noah, ava, admin));

        // Orders, each with its order lines embedded; addOrderLine() computes the total amount
        orderRepository.saveAll(List.of(
                order("ORD-20240115-00001", "2024-01-15T12:30:00", OrderStatus.DELIVERED, emma,
                        new OrderLine(margherita, 2), new OrderLine(quattroFormaggi, 1)),
                order("ORD-20240115-00002", "2024-01-15T14:00:00", OrderStatus.DELIVERED, liam,
                        new OrderLine(pepperoni, 1), new OrderLine(diavola, 1)),
                order("ORD-20240116-00003", "2024-01-16T10:15:00", OrderStatus.DELIVERED, olivia,
                        new OrderLine(vegetariana, 2), new OrderLine(marinara, 2)),
                order("ORD-20240116-00004", "2024-01-16T18:45:00", OrderStatus.DELIVERED, emma,
                        new OrderLine(pepperoni, 1), new OrderLine(diavola, 1)),
                order("ORD-20240117-00005", "2024-01-17T11:00:00", OrderStatus.DELIVERED, noah,
                        new OrderLine(prosciutto, 1)),
                order("ORD-20240117-00006", "2024-01-17T19:30:00", OrderStatus.DELIVERED, ava,
                        new OrderLine(quattroFormaggi, 2), new OrderLine(capricciosa, 1)),
                order("ORD-20240118-00007", "2024-01-18T12:00:00", OrderStatus.READY, liam,
                        new OrderLine(pepperoni, 2)),
                order("ORD-20240118-00008", "2024-01-18T13:15:00", OrderStatus.PREPARING, olivia,
                        new OrderLine(margherita, 1), new OrderLine(funghi, 1), new OrderLine(hawaii, 1)),
                order("ORD-20240118-00009", "2024-01-18T17:00:00", OrderStatus.CONFIRMED, emma,
                        new OrderLine(bbqChicken, 1), new OrderLine(tonno, 1)),
                order("ORD-20240118-00010", "2024-01-18T19:00:00", OrderStatus.PENDING, noah,
                        new OrderLine(margherita, 1), new OrderLine(vegetariana, 1))));

        log.info("Seed data saved: {} pizzas, {} customers, {} orders",
                pizzaRepository.count(), customerRepository.count(), orderRepository.count());
    }

    private Pizza pizza(String name, String price, String description, String imageUrl,
                        int calories, String protein, String carbohydrates, String fat) {
        Pizza pizza = new Pizza(name, new BigDecimal(price), description);
        pizza.setImageUrl(imageUrl);
        pizza.setNutritionalInfo(new NutritionalInfo(calories, new BigDecimal(protein), new BigDecimal(carbohydrates), new BigDecimal(fat)));
        return pizza;
    }

    private Customer customer(String name, String email, String phone, String address, Role role, Pizza... favorites) {
        Customer customer = new Customer(name, email);
        customer.setPassword(PASSWORD_HASH);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setRole(role);
        for (Pizza favorite : favorites) {
            customer.addFavoritePizza(favorite);
        }
        return customer;
    }

    private Order order(String orderNumber, String orderDate, OrderStatus status, Customer customer, OrderLine... orderLines) {
        Order order = new Order(orderNumber, customer, status);
        order.setOrderDate(LocalDateTime.parse(orderDate));
        for (OrderLine orderLine : orderLines) {
            order.addOrderLine(orderLine);
        }
        return order;
    }
}
