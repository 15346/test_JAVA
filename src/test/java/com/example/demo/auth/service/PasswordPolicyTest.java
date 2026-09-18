package com.example.demo.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsEightCharactersWithLetterAndDigit() {
        assertTrue(PasswordPolicy.isValid("abc12345"));
    }

    @Test
    void acceptsLetterAndSymbol() {
        assertTrue(PasswordPolicy.isValid("abcdefgh!"));
    }

    @Test
    void acceptsDigitAndSymbol() {
        assertTrue(PasswordPolicy.isValid("1234567!"));
    }

    @Test
    void rejectsOnlyLettersOnlyDigitsAndOnlySymbols() {
        assertAll(
                () -> assertFalse(PasswordPolicy.isValid("abcdefgh")),
                () -> assertFalse(PasswordPolicy.isValid("12345678")),
                () -> assertFalse(PasswordPolicy.isValid("!@#$%^&*"))
        );
    }

    @Test
    void rejectsShortPasswordsWhitespaceAndNull() {
        assertAll(
                () -> assertFalse(PasswordPolicy.isValid("a1!")),
                () -> assertFalse(PasswordPolicy.isValid("abc 1234")),
                () -> assertFalse(PasswordPolicy.isValid(null))
        );
    }

    @Test
    void uppercaseAndLowercaseAreOneLetterCategory() {
        assertFalse(PasswordPolicy.isValid("Abcdefgh"));
    }
}
