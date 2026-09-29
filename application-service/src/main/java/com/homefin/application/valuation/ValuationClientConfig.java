package com.homefin.application.valuation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

/**
 * Builds the ValuationClient bean: an HTTP client with base URL, timeouts, API key header and error mapping.
 *
 * <p>Role: runs once at startup. The result is injected into ValuationGateway. TS analogy:
 * {@code axios.create({ baseURL, timeout, headers: { 'X-Api-Key': key } })} plus a response interceptor that
 * throws on 5xx, then wrapping it in a typed client for the ValuationClient interface.
 *
 * <p>Layers, bottom to top: JDK HttpClient (the actual network I/O, with connect timeout) wrapped by a Spring
 * request factory (adds read timeout), used by RestClient (Spring's fluent HTTP client, like axios), adapted
 * into HttpServiceProxyFactory, which generates the ValuationClient implementation.
 */
// @Configuration (runtime): Spring calls the @Bean method below at startup.
@Configuration
public class ValuationClientConfig {

    // @Bean (runtime): both parameters are injected by Spring. RestClient.Builder is a prototype bean pre-configured
    // by Spring Boot (JSON converters, tracing/metrics instrumentation);
    // ValuationProperties comes from app.valuation.*.
    @Bean
    ValuationClient valuationClient(RestClient.Builder builder, ValuationProperties props) {
        // props.connectTimeout() returns a java.time.Duration (e.g. "2s" in YAML -> 2 seconds).
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());

        RestClient restClient = builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Api-Key", props.apiKey())
                // defaultStatusHandler(predicate, handler): for responses where the predicate is true (any 5xx),
                // run the handler. HttpStatusCode::is5xxServerError is a method reference used as the predicate
                // (= code -> code.is5xxServerError()). Throwing ValuationProviderException lets Resilience4j retry it
                // (it is listed in retry-exceptions in application.yml). 4xx keep Spring's default exceptions.
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new ValuationProviderException("Valuation provider error " + res.getStatusCode().value());
                })
                .build();

        // Generate the ValuationClient implementation. ValuationClient.class = the interface's runtime Class
        // object, which tells the factory which interface to implement.
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(ValuationClient.class);
    }
}
