package dev.willram.ramcore.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class UpdateCheckerTest {

    @Test
    public void semVerParsesAndOrders() {
        assertEquals(new SemVer(2, 1, 0, ""), SemVer.parse("v2.1.0"));
        assertEquals(new SemVer(1, 2, 3, "rc1"), SemVer.parse("1.2.3-rc1+build9"));
        assertEquals(new SemVer(3, 0, 0, ""), SemVer.parse("3"));
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.9.9")) > 0);
        assertTrue(SemVer.parse("1.0.0").compareTo(SemVer.parse("1.0.0-rc1")) > 0, "release beats pre-release");
        assertEquals(0, SemVer.parse("1.2.3").compareTo(SemVer.parse("v1.2.3")));
    }

    @Test
    public void semVerRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("not-a-version"));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("1.2.3.4"));
    }

    @Test
    public void extractsTagFromGithubJson() {
        String json = "{\"url\":\"x\",\"tag_name\":\"v2.3.0\",\"name\":\"2.3.0\"}";
        assertEquals("v2.3.0", UpdateChecker.extractTag(json).orElseThrow());
        assertTrue(UpdateChecker.extractTag("{\"name\":\"x\"}").isEmpty());
        assertTrue(UpdateChecker.extractTag("not json").isEmpty());
    }

    @Test
    public void evaluateReportsUpdateAvailability() {
        assertTrue(UpdateChecker.evaluate("2.0.0", "v2.1.0").orElseThrow().updateAvailable());
        assertFalse(UpdateChecker.evaluate("2.1.0", "v2.1.0").orElseThrow().updateAvailable());
        assertFalse(UpdateChecker.evaluate("2.2.0", "v2.1.0").orElseThrow().updateAvailable());
        assertTrue(UpdateChecker.evaluate("2.0.0", "garbage").isEmpty());
    }

    @Test
    public void resultDescribes() {
        assertTrue(UpdateChecker.evaluate("2.0.0", "2.1.0").orElseThrow().describe().contains("update is available"));
        assertTrue(UpdateChecker.evaluate("2.1.0", "2.1.0").orElseThrow().describe().contains("up to date"));
    }
}
