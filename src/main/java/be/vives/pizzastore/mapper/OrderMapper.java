package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderLine;
import be.vives.pizzastore.dto.response.OrderLineResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // customerId, customerName, pizzaId and pizzaName are fields of the documents themselves (see Order and
    // OrderLine), so MapStruct maps everything by name: no "customer.name"/"pizza.name" paths like in the JPA version
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
