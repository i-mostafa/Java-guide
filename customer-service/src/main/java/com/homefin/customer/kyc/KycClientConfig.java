package com.homefin.customer.kyc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// The HTTP client built into the JDK (Java 11+), used here as the low-level transport under RestClient.
import java.net.http.HttpClient;

/**
 * RestClient = Spring's modern synchronous HTTP client (think axios/fetch with a fluent API).
 * ALWAYS set timeouts on outbound calls; the default "wait forever" is how outages cascade.
 * The injected RestClient.Builder is pre-configured by Spring Boot with observability, so
 * every call gets a span and the trace id is propagated to the provider.
 *
 * <p>This is like creating a shared {@code axios.create({ baseURL, timeout, headers })} instance once and exporting
 * it. {@code @Configuration} (runtime) marks the class as a source of bean definitions; Spring reads it at startup.
 */
@Configuration
public class KycClientConfig {

    // @Bean (runtime): Spring calls this factory once at startup and registers the result as a singleton bean named
    // "kycRestClient". Both parameters are injected by Spring: the Boot-provided builder and our KycProperties.
    // KycClient then receives this RestClient through its constructor.
    @Bean
    RestClient kycRestClient(RestClient.Builder builder, KycProperties props) {
        // RestClient.Builder is a nested type (a class declared inside RestClient), referenced as Outer.Inner.
        // Builder pattern: chain setters, then build() creates the immutable object (Java has no object literals).
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())   // max time to open the TCP/TLS connection
                .build();
        // "new" creates an instance, same as TS. The type on the left could also have been written as "var".
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());   // max time to wait for the response

        return builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Api-Key", props.apiKey())   // sent on every request, like axios default headers
                .build();
    }
}
