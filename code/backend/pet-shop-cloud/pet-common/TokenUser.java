package com.petshop.common.security;

public class TokenUser {
    private final String username;
    private final String role;

    public TokenUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
