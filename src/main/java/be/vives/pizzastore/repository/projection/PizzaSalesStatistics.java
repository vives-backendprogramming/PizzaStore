package be.vives.pizzastore.repository.projection;

import java.math.BigDecimal;

/**
 * Aggregated sales figures for one pizza, computed directly by MongoDB's aggregation framework
 * (see {@code OrderRepository.findPizzaSalesStatistics()}) rather than assembled in Java from a
 * list of {@code Order} documents. Not a document itself.
 */
public record PizzaSalesStatistics(
        String pizzaName,
        Long timesOrdered,
        Long totalQuantitySold,
        BigDecimal totalRevenue
) {
}
