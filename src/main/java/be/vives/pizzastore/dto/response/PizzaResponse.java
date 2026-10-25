package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record PizzaResponse(
        String id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        Boolean available,
        NutritionalInfoResponse nutritionalInfo
) {
}
