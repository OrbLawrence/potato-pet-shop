package com.petshop.common.security;

public class BearerTokenResolver {
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public BearerTokenResolver(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenUser resolve(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIX)) {
            return null;
        }
        String token = authorization.substring(PREFIX.length()).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            return null;
        }
        return new TokenUser(
                jwtTokenProvider.extractUsername(token),
                jwtTokenProvider.extractRole(token)
        );
    }
}
