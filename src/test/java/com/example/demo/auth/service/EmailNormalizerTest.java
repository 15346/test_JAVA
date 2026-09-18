package com.example.demo.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void normalizeTrimsAndLowercasesUsingRootLocale() {
        assertEquals("hello@126.com", EmailNormalizer.normalize("  Hello@126.COM "));
    }

    @Test
    void normalizeReturnsNullForNull() {
        assertNull(EmailNormalizer.normalize(null));
    }
}
