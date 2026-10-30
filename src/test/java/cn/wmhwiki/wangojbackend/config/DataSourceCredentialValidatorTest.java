package cn.wmhwiki.wangojbackend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceCredentialValidatorTest {

    @Test
    void acceptsRandomLongPassword() {
        assertDoesNotThrow(() -> new DataSourceCredentialValidator(true, "0123456789abcdef0123456789abcdef")
                .validate());
    }

    @Test
    void rejectsBlankPassword() {
        assertThrows(IllegalStateException.class, () -> new DataSourceCredentialValidator(true, "").validate());
        assertThrows(IllegalStateException.class, () -> new DataSourceCredentialValidator(true, "   ").validate());
        assertThrows(IllegalStateException.class, () -> new DataSourceCredentialValidator(true, null).validate());
    }

    @Test
    void rejectsKnownPlaceholders() {
        assertThrows(IllegalStateException.class,
                () -> new DataSourceCredentialValidator(true, "change-me").validate());
        assertThrows(IllegalStateException.class,
                () -> new DataSourceCredentialValidator(true, "change-me-please").validate());
        assertThrows(IllegalStateException.class,
                () -> new DataSourceCredentialValidator(true, "__REQUIRED__").validate());
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalStateException.class,
                () -> new DataSourceCredentialValidator(true, "short").validate());
        assertThrows(IllegalStateException.class,
                () -> new DataSourceCredentialValidator(true, "elevenchars").validate());
    }

    @Test
    void skipsValidationWhenDisabled() {
        assertDoesNotThrow(() -> new DataSourceCredentialValidator(false, "change-me").validate());
        assertDoesNotThrow(() -> new DataSourceCredentialValidator(false, "").validate());
    }
}
