package be.vives.pizzastore.domain;

import java.math.BigDecimal;

// Embedded value object: no @Document and no id, it lives inside its Order document. In the JPA version it
// was its own table with @ManyToOne references to Order and Pizza. Here it references the pizza by id and
// copies its name and price: an order line records what was ordered at that moment, even if the pizza is
// renamed or gets a new price later (the price was already copied in the JPA version, for the same reason).
public class OrderLine {

    private String pizzaId;

    private String pizzaName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    // Constructors
    public OrderLine() {
    }

    public OrderLine(Pizza pizza, Integer quantity) {
        this.pizzaId = pizza.getId();
        this.pizzaName = pizza.getName();
        this.quantity = quantity;
        this.unitPrice = pizza.getPrice();
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public String getPizzaId() {
        return pizzaId;
    }

    public void setPizzaId(String pizzaId) {
        this.pizzaId = pizzaId;
    }

    public String getPizzaName() {
        return pizzaName;
    }

    public void setPizzaName(String pizzaName) {
        this.pizzaName = pizzaName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    private void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    public String toString() {
        return "OrderLine{" +
                "pizzaId='" + pizzaId + '\'' +
                ", pizzaName='" + pizzaName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}
