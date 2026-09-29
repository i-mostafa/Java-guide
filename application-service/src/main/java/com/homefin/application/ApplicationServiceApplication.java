// "package" = the namespace this file lives in. It MUST match the folder path
// (src/main/java/com/homefin/application). Think of it as the module path in an ES import.
package com.homefin.application;

// "import" brings a class from another package into scope by its short name, like
// `import { SpringApplication } from '...'`. Nothing is executed on import (no side effects in Java).
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point of the application-service microservice (the "main.ts" / "server.ts" of this app).
 *
 * <p>Role: {@code main} boots Spring. Spring then scans this package and all sub-packages for
 * classes marked {@code @Component}, {@code @Service}, {@code @RestController}, {@code @Configuration}...
 * creates one instance of each (a "bean"), injects their constructor dependencies, starts the
 * embedded Tomcat HTTP server and runs Flyway migrations. It is like a NestJS {@code AppModule} +
 * {@code NestFactory.create(AppModule).listen()}, except modules are discovered automatically.
 *
 * <p>Annotations ({@code @Something}) are metadata attached to code, similar to TS decorators, but
 * they do nothing on their own: some library must READ them. Here Spring reads them at runtime
 * (startup) via reflection.
 */
// @SpringBootApplication (runtime, read by Spring at startup) = three annotations in one:
//   @Configuration (this class may declare beans), @EnableAutoConfiguration (configure libraries found
//   on the classpath: web server, JPA, Kafka, security...) and @ComponentScan (find beans in this package tree).
@SpringBootApplication
// @EnableFeignClients (runtime): scan for interfaces annotated @FeignClient (see CustomerClient) and
// generate an HTTP client implementation for each at startup.
@EnableFeignClients
// @ConfigurationPropertiesScan (runtime): find records/classes annotated @ConfigurationProperties
// (FinanceProperties, ValuationProperties) and bind application.yml values into them.
@ConfigurationPropertiesScan
// "public" = visible from any package. A top-level public class must live in a file with the same name.
public class ApplicationServiceApplication {

    // The JVM starts the program by calling this exact signature.
    // static = belongs to the class itself, callable without "new" (like a static method in a TS class).
    // void = returns nothing. String[] args = command-line arguments (like process.argv.slice(2)).
    public static void main(String[] args) {
        // ApplicationServiceApplication.class = a runtime handle to this class (reflection), used as the
        // root of the component scan and the source of the annotations above.
        SpringApplication.run(ApplicationServiceApplication.class, args);
    }
}
