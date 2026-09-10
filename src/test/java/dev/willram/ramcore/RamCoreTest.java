package dev.willram.ramcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RamCoreTest {

    @Test
    public void diagnosticsCommandIsEnabledByDefault() {
        assertTrue(RamCore.diagnosticsEnabled(null));
    }

    @Test
    public void diagnosticsCommandCanBeDisabledBySystemProperty() {
        assertFalse(RamCore.diagnosticsEnabled("false"));
        assertTrue(RamCore.diagnosticsEnabled("true"));
        assertTrue(RamCore.diagnosticsEnabled("TRUE"));
    }
}
