package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PizzaRepository extends MongoRepository<Pizza, String> {

    // Derived query methods: the same names as the JPA version, Spring Data MongoDB turns them into
    // MongoDB query documents instead of SQL
    Optional<Pizza> findByName(String name);

    List<Pizza> findByPriceLessThan(BigDecimal maxPrice);

    List<Pizza> findByPriceGreaterThanEqual(BigDecimal minPrice);

    List<Pizza> findByNameContainingIgnoreCase(String keyword);

    List<Pizza> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Custom query: @Query takes a MongoDB query document (JSON) instead of JPQL. ?0 is the first parameter.
    // The equivalent of "name LIKE %:keyword% OR description LIKE %:keyword%" (case-sensitive, like LIKE).
    // Unlike a derived ...Containing query, ?0 is used as a regular expression as is: '.' or '*' in the keyword are regex syntax.
    @Query("{ '$or': [ { 'name': { '$regex': ?0 } }, { 'description': { '$regex': ?0 } } ] }")
    List<Pizza> searchByKeyword(String keyword);

    // No findByIdWithNutritionalInfo(): nutritionalInfo is embedded in the Pizza document,
    // so a plain findById() already returns it.
}
