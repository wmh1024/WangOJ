package cn.wmhwiki.wangojbackend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordSaltValidatorTest {

    @Test
    void acceptsRandomLongSalt() {
        assertDoesNotThrow(() -> new PasswordSaltValidator("0123456789abcdef0123456789abcdef").validate());
    }

    @Test
    void rejectsBlankSalt() {
        assertThrows(IllegalStateException.class, () -> new PasswordSaltValidator("").validate());
        assertThrows(IllegalStateException.class, () -> new PasswordSaltValidator("   ").validate());
    }

    @Test
    void rejectsKnownPlaceholdersAndShortValues() {
        assertThrows(IllegalStateException.class,
                () -> new PasswordSaltValidator("change-me-please").validate());
        assertThrows(IllegalStateException.class,
                () -> new PasswordSaltValidator("please-generate-a-random-32-chars-string").validate());
        assertThrows(IllegalStateException.class,
                () -> new PasswordSaltValidator("__REQUIRED__").validate());
        assertThrows(IllegalStateException.class,
                () -> new PasswordSaltValidator("short").validate());
    }
}
