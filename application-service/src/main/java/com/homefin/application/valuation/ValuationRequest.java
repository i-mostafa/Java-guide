package com.homefin.application.valuation;

/**
 * JSON request body sent to the valuation provider's {@code POST /valuation/v1/estimates}.
 *
 * <p>Our own model of the 3rd-party contract; propertyType is a plain String here (not our enum) so the
 * provider's vocabulary doesn't leak into the domain. TS analogy:
 * {@code type ValuationRequest = { propertyReference: string; city: string; propertyType: string }}.
 */
// record: immutable data class; Jackson serializes each component as a JSON property of the same name.
public record ValuationRequest(String propertyReference, String city, String propertyType) {
}
