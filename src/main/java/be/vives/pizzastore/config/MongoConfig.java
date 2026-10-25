package be.vives.pizzastore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// The MongoDB counterpart of JpaConfig (@EnableJpaAuditing) on the main branch: fills @CreatedDate,
// @LastModifiedDate, @CreatedBy and @LastModifiedBy (the user comes from AuditorAwareImpl).
// Documents need no @EntityListeners(AuditingEntityListener.class): this annotation alone is enough.
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
