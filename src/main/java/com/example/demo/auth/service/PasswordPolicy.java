package com.example.demo.auth.service;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8
                || password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        boolean letter = password.matches(".*[A-Za-z].*");
        boolean digit = password.matches(".*[0-9].*");
        boolean symbol = password.matches(".*[^\\sA-Za-z0-9].*");
        int categories = (letter ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
        return categories >= 2;
    }
}
