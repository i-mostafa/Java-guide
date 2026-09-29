package com.homefin.application.valuation;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring "HTTP Interface" client (Spring Framework 6+): like Feign but built into Spring and
 * backed by RestClient. Great for 3rd-party APIs that are NOT in service discovery.
 *
 * <p>Role: declares the external valuation provider's API as a Java interface. Unlike Feign there is no
 * classpath scanning: ValuationClientConfig explicitly creates the implementation with
 * {@code HttpServiceProxyFactory}, which generates a runtime proxy that turns each method call into an HTTP
 * request. Calling {@code estimate(req)} sends {@code POST {baseUrl}/valuation/v1/estimates} with {@code req}
 * as JSON and parses the JSON response. TS analogy: a typed API client generated from an interface.
 */
// @HttpExchange (runtime, read by HttpServiceProxyFactory): defaults shared by all methods: URL prefix and
// the Accept / Content-Type headers.
@HttpExchange(url = "/valuation/v1", accept = "application/json", contentType = "application/json")
public interface ValuationClient {

    // @PostExchange (runtime): this method is an HTTP POST to the given sub-path.
    // @RequestBody (runtime): serialize this argument as the JSON request body.
    @PostExchange("/estimates")
    ValuationResponse estimate(@RequestBody ValuationRequest request);
}
