package dev.willram.ramcore.integration;

import dev.willram.ramcore.economy.Economies;
import dev.willram.ramcore.placeholder.PlaceholderRegistry;
import dev.willram.ramcore.placeholder.PlaceholderApiBridge;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the Vault and PlaceholderAPI bridges degrade cleanly (no {@code net.milkbowl} or
 * {@code me.clip} class loading, no server calls) when the plugins are absent.
 */
public final class BridgeAbsenceTest {

    private static IntegrationRegistry noPlugins() {
        return IntegrationRegistry.standard(new PluginDetector() {
            @Override
            public boolean present(String pluginName) {
                return false;
            }

            @Override
            public boolean enabled(String pluginName) {
                return false;
            }

            @Override
            public Optional<String> version(String pluginName) {
                return Optional.empty();
            }
        });
    }

    @Test
    public void economyDetectIsEmptyWithoutVault() {
        assertTrue(Economies.detect(noPlugins()).isEmpty());
    }

    @Test
    public void placeholderBridgeRegistersNothingWithoutPapi() {
        PlaceholderRegistry placeholders = PlaceholderRegistry.create();
        assertEquals(0, PlaceholderApiBridge.register(noPlugins(), placeholders, "RamCore", "1.0"));
    }
}
