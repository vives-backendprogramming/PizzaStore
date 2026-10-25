package be.vives.pizzastore.exception;

public class ResourceNotFoundException extends PizzaStoreException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String id) {
        super(String.format("%s with id %s not found", resourceName, id));
    }
}
