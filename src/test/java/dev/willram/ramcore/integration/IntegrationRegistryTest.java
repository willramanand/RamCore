package dev.willram.ramcore.integration;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class IntegrationRegistryTest {

    @Test
    public void standardRegistryIncludesRequestedIntegrations() {
        IntegrationRegistry registry = Integrations.standard(new FakeDetector());

        assertTrue(registry.provider(StandardIntegrations.LUCK_PERMS.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.VAULT.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.PLACEHOLDER_API.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.MINI_PLACEHOLDERS.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.WORLD_GUARD.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.PROTOCOL_LIB.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.CITIZENS.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.ITEMS_ADDER.id()).isPresent());
        assertTrue(registry.provider(StandardIntegrations.ORAXEN.id()).isPresent());
    }

    @Test
    public void availableIntegrationSupportsDeclaredCapability() {
        FakeDetector detector = new FakeDetector().plugin("LuckPerms", true, "5.4");
        IntegrationRegistry registry = Integrations.standard(detector);
        IntegrationSnapshot snapshot = registry.require(StandardIntegrations.LUCK_PERMS.id()).snapshot();

        assertEquals(IntegrationStatus.AVAILABLE, snapshot.status());
        assertEquals("5.4", snapshot.version());
        assertTrue(registry.available(StandardIntegrations.LUCK_PERMS.id()));
        assertTrue(registry.supports(IntegrationCapability.PERMISSIONS));
    }

    @Test
    public void missingAndDisabledIntegrationsReportDistinctStates() {
        FakeDetector detector = new FakeDetector().plugin("Vault", false, "1.7");
        IntegrationRegistry registry = Integrations.standard(detector);

        assertEquals(IntegrationStatus.DISABLED, registry.require(StandardIntegrations.VAULT.id()).snapshot().status());
        assertEquals(IntegrationStatus.MISSING, registry.require(StandardIntegrations.WORLD_GUARD.id()).snapshot().status());
        assertFalse(registry.supports(IntegrationCapability.ECONOMY));
    }

    @Test
    public void duplicateRegistrationFailsFast() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                IntegrationId.of("example"),
                "Example",
                EnumSet.of(IntegrationCapability.CUSTOM_ITEMS),
                "Example integration"
        );
        IntegrationRegistry registry = Integrations.registry()
                .register(new StaticProvider(descriptor, IntegrationStatus.AVAILABLE));

        assertThrows(ApiMisuseException.class, () -> registry.register(new StaticProvider(descriptor, IntegrationStatus.AVAILABLE)));
    }

    @Test
    public void snapshotsAreSortedByIntegrationId() {
        IntegrationDescriptor beta = new IntegrationDescriptor(IntegrationId.of("beta"), "Beta", EnumSet.of(IntegrationCapability.NPCS), "");
        IntegrationDescriptor alpha = new IntegrationDescriptor(IntegrationId.of("alpha"), "Alpha", EnumSet.of(IntegrationCapability.PACKETS), "");

        IntegrationRegistry registry = Integrations.registry()
                .register(new StaticProvider(beta, IntegrationStatus.AVAILABLE))
                .register(new StaticProvider(alpha, IntegrationStatus.AVAILABLE));

        assertEquals("alpha", registry.snapshots().get(0).descriptor().id().toString());
        assertEquals("beta", registry.snapshots().get(1).descriptor().id().toString());
    }

    private record StaticProvider(IntegrationDescriptor descriptor, IntegrationStatus status) implements IntegrationProvider {
        @Override
        public IntegrationSnapshot snapshot() {
            return new IntegrationSnapshot(this.descriptor, this.status, null, this.status.name());
        }
    }

    private static final class FakeDetector implements PluginDetector {
        private final Map<String, Entry> plugins = new HashMap<>();

        private FakeDetector plugin(String name, boolean enabled, String version) {
            this.plugins.put(name, new Entry(enabled, version));
            return this;
        }

        @Override
        public boolean present(String pluginName) {
            return this.plugins.containsKey(pluginName);
        }

        @Override
        public boolean enabled(String pluginName) {
            Entry entry = this.plugins.get(pluginName);
            return entry != null && entry.enabled();
        }

        @Override
        public Optional<String> version(String pluginName) {
            Entry entry = this.plugins.get(pluginName);
            return entry == null ? Optional.empty() : Optional.of(entry.version());
        }

        private record Entry(boolean enabled, String version) {
        }
    }
}
