package com.posadskiy.restsecurity.jwt;

/**
 * Configuration for JWT-based security.
 *
 * @param secret        HMAC secret for HS256/HS384/HS512 (required unless trustGateway is true)
 * @param rolesClaim    JWT claim name for roles (e.g. "roles", "authorities"); default "roles"
 * @param userIdClaim   JWT claim name for user ID; default "sub"
 * @param trustGateway  if true, skip signature verification (gateway already verified the token)
 */
public record JwtConfig(
        String secret,
        String rolesClaim,
        String userIdClaim,
        boolean trustGateway
) {

    public JwtConfig {
        rolesClaim = rolesClaim != null && !rolesClaim.isBlank() ? rolesClaim : "roles";
        userIdClaim = userIdClaim != null && !userIdClaim.isBlank() ? userIdClaim : "sub";
        if (!trustGateway && (secret == null || secret.isBlank())) {
            throw new IllegalArgumentException("JwtConfig: secret must be non-blank when trustGateway is false");
        }
    }

    /**
     * HMAC with default claim names.
     */
    public static JwtConfig withSecret(String secret) {
        return new JwtConfig(secret, "roles", "sub", false);
    }

    /**
     * HMAC, trust gateway (no signature verification).
     */
    public static JwtConfig withSecretTrustGateway(String secret) {
        return new JwtConfig(secret, "roles", "sub", true);
    }
}
