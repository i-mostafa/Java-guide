package com.homefin.application.valuation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValuationResponse(String valuationId, String propertyReference, BigDecimal estimatedValue,
                                String currency, String confidence) {
}
