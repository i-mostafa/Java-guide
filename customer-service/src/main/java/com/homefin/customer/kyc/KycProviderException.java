package com.homefin.customer.kyc;

/** Transient provider failure (5xx) - worth retrying. */
public class KycProviderException extends RuntimeException {

    public KycProviderException(String message) {
        super(message);
    }
}
