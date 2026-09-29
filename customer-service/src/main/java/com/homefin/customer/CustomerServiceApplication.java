// "package" = the namespace/folder this class lives in. It must match the directory path
// (src/main/java/com/homefin/customer). Reverse-domain naming (com.homefin...) avoids name clashes, like npm scopes.
package com.homefin.customer;

// "import" brings a class from another package into scope by its short name, like an ES import.
// Unlike JS, imports are only a naming convenience: everything on the classpath is always loadable.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of customer-service: the Java equivalent of {@code main.ts} with {@code NestFactory.create(AppModule)}
 * followed by {@code app.listen()}.
 *
 * <p>Responsibilities of this service: customer profiles (created when a user registers, via Kafka), profile
 * updates over REST, and identity verification (KYC) against a 3rd-party provider.
 *
 * <p>{@code @SpringBootApplication} (runtime, read by Spring at startup) combines three things:
 * <ul>
 *   <li>component scanning: find every class in this package and sub-packages annotated with {@code @Component},
 *       {@code @Service}, {@code @RestController}, {@code @Configuration} etc., instantiate it and register it
 *       in the dependency-injection container ("application context"). Like NestJS modules, but automatic.</li>
 *   <li>auto-configuration: based on the libraries on the classpath (see pom.xml), Spring Boot creates
 *       sensible default beans, e.g. a DataSource, a Kafka consumer factory, a JSON mapper.</li>
 *   <li>marks this class as a configuration source itself.</li>
 * </ul>
 *
 * <p>{@code @ConfigurationPropertiesScan} (runtime) finds classes annotated with {@code @ConfigurationProperties}
 * (such as {@code KycProperties}) and binds application.yml values into them.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
// "public" = visible from any package. Each .java file has at most one public top-level type, named like the file.
public class CustomerServiceApplication {

    // The JVM starts the program by calling this exact signature: "public static void main(String[] args)".
    // "static" = belongs to the class itself, not to an instance (no "new" needed), like a static method in TS.
    // "void" = returns nothing. String[] = array of strings (the CLI arguments, like process.argv.slice(2)).
    public static void main(String[] args) {
        // Boots Spring: loads config, creates all beans, starts embedded Tomcat on server.port and Kafka listeners.
        // "CustomerServiceApplication.class" is a reference to the class object itself (used as the scan root).
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
