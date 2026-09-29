package com.homefin.customer.kyc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * RestClient = Spring's modern synchronous HTTP client (think axios/fetch with a fluent API).
 * ALWAYS set timeouts on outbound calls; the default "wait forever" is how outages cascade.
 * The injected RestClient.Builder is pre-configured by Spring Boot with observability, so
 * every call gets a span and the trace id is propagated to the provider.
 */
@Configuration
public class KycClientConfig {

    @Bean
    RestClient kycRestClient(RestClient.Builder builder, KycProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());

        return builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Api-Key", props.apiKey())
                .build();
    }
}
