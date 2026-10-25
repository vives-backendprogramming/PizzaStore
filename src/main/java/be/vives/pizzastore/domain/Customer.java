package be.vives.pizzastore.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "customers")
public class Customer {

    @Id
    private String id;

    private String name;

    // the unique constraint of the JPA version; only created when spring.data.mongodb.auto-index-creation=true
    @Indexed(unique = true)
    private String email;

    private String password;

    private String phone;

    private String address;

    // an enum is stored as its name ("CUSTOMER"), like @Enumerated(EnumType.STRING) in JPA
    private Role role = Role.CUSTOMER;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // No List<Order> orders: a customer's orders can grow without limit, and every Order already stores
    // its customerId. Look them up with OrderRepository.findByCustomerId(customer.getId()).

    // @ManyToMany with a customer_favorite_pizzas join table in the JPA version. MongoDB has no join tables:
    // the customer document simply holds the ids of its favorite pizzas (a Set, so a pizza is in it only once).
    private Set<String> favoritePizzaIds = new HashSet<>();

    // Constructors
    public Customer() {
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Helper methods: only the id of the pizza is stored
    public void addFavoritePizza(Pizza pizza) {
        favoritePizzaIds.add(pizza.getId());
    }

    public void removeFavoritePizza(Pizza pizza) {
        favoritePizzaIds.remove(pizza.getId());
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Set<String> getFavoritePizzaIds() {
        return favoritePizzaIds;
    }

    public void setFavoritePizzaIds(Set<String> favoritePizzaIds) {
        this.favoritePizzaIds = favoritePizzaIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
