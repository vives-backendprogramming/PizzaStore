package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    // favoritePizzaIds is an array: MongoDB matches a document when the array contains the value
    boolean existsByFavoritePizzaIds(String pizzaId);

    // No findByIdWithOrders()/findByIdWithFavorites(): MongoDB has no JOIN FETCH. The favorite pizza ids come
    // with every Customer document; the pizzas themselves and the customer's orders are separate queries
    // (PizzaRepository.findAllById, OrderRepository.findByCustomerId).
}
