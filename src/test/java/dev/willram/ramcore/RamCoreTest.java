package dev.willram.ramcore;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
