package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.repository.projection.PizzaSalesStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderNumber(String orderNumber);

    // for OrderSecurity: does this order belong to this customer? The JPA version could follow o.customer.email
    // with a join; a Mongo query only sees the order document itself, so the caller first looks up the customer id
    boolean existsByIdAndCustomerId(String id, String customerId);

    // "orderLines.pizzaId" looks inside the embedded order lines: is this pizza in any order?
    boolean existsByOrderLinesPizzaId(String pizzaId);

    void deleteByCustomerId(String customerId);

    // No findByIdWithOrderLines()/findByCustomerIdWithCustomer(): the order lines and the customer's name are
    // part of the Order document, so a plain findById()/findByCustomerId() already returns them.

    // DTO projection with the aggregation framework: the equivalent of the JPA version's
    // SELECT new PizzaSalesStatistics(...) ... GROUP BY ... ORDER BY. Each string is one pipeline stage:
    // one document per order line, grouped by pizza name, renamed to the record's components, sorted.
    @Aggregation(pipeline = {
            "{ '$unwind': '$orderLines' }",
            "{ '$group': { '_id': '$orderLines.pizzaName', 'timesOrdered': { '$sum': 1 }, 'totalQuantitySold': { '$sum': '$orderLines.quantity' }, 'totalRevenue': { '$sum': '$orderLines.subtotal' } } }",
            "{ '$project': { '_id': 0, 'pizzaName': '$_id', 'timesOrdered': 1, 'totalQuantitySold': 1, 'totalRevenue': 1 } }",
            "{ '$sort': { 'totalQuantitySold': -1 } }"
    })
    List<PizzaSalesStatistics> findPizzaSalesStatistics();
}
