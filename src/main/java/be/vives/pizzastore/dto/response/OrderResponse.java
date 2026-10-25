package be.vives.pizzastore.dto.response;

import be.vives.pizzastore.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String orderNumber,
        String customerId,
        String customerName,
        List<OrderLineResponse> orderLines,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime orderDate
) {
}
