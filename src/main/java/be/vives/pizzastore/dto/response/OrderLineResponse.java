package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

// no id: an order line is embedded in its order document and has no identity of its own
public record OrderLineResponse(
        String pizzaId,
        String pizzaName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
