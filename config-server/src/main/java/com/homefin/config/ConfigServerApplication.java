package com.homefin.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralised configuration. Services call  GET /{application}/{profile}  on startup
 * (e.g. /customer-service/docker) and merge the result into their Environment.
 *
 * <p>This is the whole application: the entry point class. The YAML files it serves live in
 * src/main/resources/config-repo. TS analogy: a tiny Express app that serves
 * {@code config/<service>-<env>.yml} files, which each service fetches before it boots and
 * merges into its own settings (like layering dotenv files).
 */
// @SpringBootApplication (Spring Boot, runtime) combines three annotations:
//  - @Configuration: this class may define @Bean methods
//  - @EnableAutoConfiguration: auto-configure beans based on the JARs present (web server, etc.)
//  - @ComponentScan: find @Component/@Service/@RestController classes in this package and below
@SpringBootApplication
// @EnableConfigServer (Spring Cloud, runtime): turns this app into a config server by
// registering the HTTP endpoints that serve config files.
@EnableConfigServer
public class ConfigServerApplication {

    // The program entry point: the JVM looks for exactly "public static void main(String[] args)".
    // void = returns nothing. String[] = an array of strings (the CLI args, like process.argv).
    public static void main(String[] args) {
        // Boots Spring: creates the DI container, runs auto-configuration, starts embedded Tomcat.
        // Roughly NestFactory.create(AppModule).then(app => app.listen(port)).
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
