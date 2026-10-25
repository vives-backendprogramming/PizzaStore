package be.vives.pizzastore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * A real MongoDB server for the tests, in a Docker container started by Testcontainers. The JPA version needs none
 * of this: H2 runs in memory, inside the JVM. MongoDB has no in-memory mode, so a test needs a real server.
 * <p>
 * Import it in every test that needs the database: {@code @Import(MongoTestcontainersConfiguration.class)}.
 * {@code @ServiceConnection} makes Spring Boot connect to this container (random port) instead of spring.mongodb.uri.
 * The container lives as long as the application context: test classes with the same configuration share the cached
 * context, and so the container and its data. That is why every test cleans up or uses unique data.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer("mongo:8.0");
    }
}
