package com.freelance.auth.dto;

import java.time.Instant;

public record Day0BootstrapResult(
        boolean bootstrapped,
        String publicKeyBase64,
        String privateKeyPem,
        String token,
        Instant expiresAt
) {
    public static Day0BootstrapResult skipped() {
        return new Day0BootstrapResult(false, null, null, null, null);
    }
}
