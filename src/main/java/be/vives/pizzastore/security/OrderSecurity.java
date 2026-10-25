package be.vives.pizzastore.security;

import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Instance-level security: is the logged-in user the customer who placed this order?
 * <p>
 * The URL rules in {@link SecurityConfig} only know the role. Whether customer Emma may read order 12 depends on
 * the data, so {@code @PreAuthorize} expressions call this bean by its name: {@code @orderSecurity.isOwner(#id, authentication)}.
 */
@Component("orderSecurity")
public class OrderSecurity {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderSecurity(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    public boolean isOwner(String orderId, Authentication authentication) {
        if (orderId == null || authentication == null) {
            return false;
        }
        // the username of a PizzaStore user is the e-mail address (see CustomUserDetailsService).
        // The JPA version asks this in one query with a join (existsByIdAndCustomerEmail); MongoDB cannot join, and an
        // order only stores its customerId (an e-mail address can change), so: first the customer, then the order.
        // An unknown order, or a user without a customer document, is "not yours" too.
        return customerRepository.findByEmail(authentication.getName())
                .map(customer -> orderRepository.existsByIdAndCustomerId(orderId, customer.getId()))
                .orElse(false);
    }
}
