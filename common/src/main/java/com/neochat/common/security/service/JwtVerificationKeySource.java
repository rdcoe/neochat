package com.neochat.common.security.service;

import java.security.PublicKey;

/**
 * Extension point for pluggable JWT verification key sources.
 */
public interface JwtVerificationKeySource {

    /**
     * Returns the public key used to verify JWT signatures.
     */
    PublicKey getVerificationKey();
}
