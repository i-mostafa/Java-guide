package com.homefin.application.valuation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * JSON response of the valuation provider, deserialized by Jackson via the generated ValuationClient.
 *
 * <p>TS analogy: {@code type ValuationResponse = { valuationId: string; estimatedValue: number; ... }}, except
 * estimatedValue is parsed into an exact BigDecimal rather than a floating-point number.
 */
// @JsonIgnoreProperties(ignoreUnknown = true) (runtime, Jackson): silently skip JSON fields we don't model,
// so the 3rd party adding a field never breaks us (a "tolerant reader"). Explicit here for safety even though
// Spring Boot's default ObjectMapper already ignores unknown properties.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValuationResponse(String valuationId, String propertyReference, BigDecimal estimatedValue,
                                String currency, String confidence) {
}
