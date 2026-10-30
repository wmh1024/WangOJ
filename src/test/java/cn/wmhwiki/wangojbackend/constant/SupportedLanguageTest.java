package cn.wmhwiki.wangojbackend.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportedLanguageTest {

    @Test
    void normalizesAliasesAndCase() {
        assertEquals("c++", SupportedLanguage.normalize("CPP"));
        assertEquals("javascript", SupportedLanguage.normalize(" js "));
        assertEquals("golang", SupportedLanguage.normalize("go"));
        assertEquals("java", SupportedLanguage.normalize("Java"));
        assertEquals("", SupportedLanguage.normalize(null));
    }

    @Test
    void acceptsOnlyWhitelist() {
        assertTrue(SupportedLanguage.isSupported("c"));
        assertTrue(SupportedLanguage.isSupported("c++"));
        assertTrue(SupportedLanguage.isSupported("cpp"));
        assertTrue(SupportedLanguage.isSupported("javascript"));
        assertFalse(SupportedLanguage.isSupported("rust"));
        assertFalse(SupportedLanguage.isSupported(null));
    }
}
