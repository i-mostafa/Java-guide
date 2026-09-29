package com.homefin.application.valuation;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring "HTTP Interface" client (Spring Framework 6+): like Feign but built into Spring and
 * backed by RestClient. Great for 3rd-party APIs that are NOT in service discovery.
 */
@HttpExchange(url = "/valuation/v1", accept = "application/json", contentType = "application/json")
public interface ValuationClient {

    @PostExchange("/estimates")
    ValuationResponse estimate(@RequestBody ValuationRequest request);
}
