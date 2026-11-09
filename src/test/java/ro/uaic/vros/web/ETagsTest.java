package ro.uaic.vros.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The validator must be deterministic and change when visible data changes. */
class ETagsTest {
    @Test
    void sameDtoProducesTheSameWeakEtag() {
        String a = ETags.weak("scenario 2, vehicles=[1, 3]");
        String b = ETags.weak("scenario 2, vehicles=[1, 3]");
        assertEquals(a, b);
        assertTrue(a.startsWith("W/\""));
        assertTrue(a.endsWith("\""));
    }

    @Test
    void changedDtoProducesDifferentValidator() {
        assertNotEquals(ETags.weak("name=Morning"), ETags.weak("name=Evening"));
    }
}
