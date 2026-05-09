package dev.willram.ramcore.nms;

import dev.willram.ramcore.ai.MobAi;
import dev.willram.ramcore.brain.MobBrains;
import dev.willram.ramcore.combat.CombatControls;
import dev.willram.ramcore.entity.EntityControls;
import dev.willram.ramcore.item.component.ItemComponents;
import dev.willram.ramcore.item.nbt.ItemNbt;
import dev.willram.ramcore.loot.InstancedLoot;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsAdapter;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCategory;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsCompatibilityMatrix;
import dev.willram.ramcore.nms.api.NmsExampleModules;
import dev.willram.ramcore.nms.api.NmsQuarantineAction;
import dev.willram.ramcore.nms.api.NmsQuarantinePolicy;
import dev.willram.ramcore.nms.api.NmsSelfTestPlan;
import dev.willram.ramcore.nms.api.NmsSelfTestReport;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.nms.api.NmsUnsupportedException;
import dev.willram.ramcore.nms.reflect.GuardedNmsLookup;
import dev.willram.ramcore.nms.reflect.ReflectiveNmsAdapter;
import dev.willram.ramcore.packet.Packets;
import dev.willram.ramcore.path.Pathfinders;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import dev.willram.ramcore.world.WorldBlocks;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class NmsAccessStrategyTest {

    @Test
    public void registryReturnsFirstUsableAdapterByTier() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
                .register(adapter("reflect", NmsAccessTier.GUARDED_REFLECTION, NmsSupportStatus.SUPPORTED))
                .register(adapter("paper", NmsAccessTier.PAPER_API, NmsSupportStatus.SUPPORTED));

        NmsCapabilityCheck check = registry.require(NmsCapability.MOB_GOALS);

        assertEquals("paper", check.adapterId());
        assertEquals(NmsAccessTier.PAPER_API, check.tier());
    }

    @Test
    public void unsupportedCapabilityThrowsClearException() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE);

        try {
            registry.require(NmsCapability.BRAIN_MEMORY);
            fail("Expected unsupported capability");
        } catch (NmsUnsupportedException e) {
            assertEquals(NmsCapability.BRAIN_MEMORY, e.capability());
            assertEquals(NmsSupportStatus.UNKNOWN, e.check().status());
            assertTrue(e.getMessage().contains("BRAIN_MEMORY"));
        }
    }

    @Test
    public void diagnosticsExposeVersionsAdaptersAndCapabilities() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
                .register(adapter("paper", NmsAccessTier.PAPER_API, NmsSupportStatus.PARTIAL));

        assertTrue(registry.diagnostics().lines().stream().anyMatch(line -> line.contains("Minecraft version: 1.21.0")));
        assertTrue(registry.diagnostics().lines().stream().anyMatch(line -> line.contains("Adapters: paper")));
        assertTrue(registry.diagnostics().lines().stream().anyMatch(line -> line.contains("MOB_GOALS=PARTIAL")));
    }

    @Test
    public void reflectiveAdapterUsesGuardedClassProbes() {
        GuardedNmsLookup lookup = GuardedNmsLookup.using(getClass().getClassLoader());
        ReflectiveNmsAdapter adapter = ReflectiveNmsAdapter.create("reflect-test", lookup)
                .capability(NmsCapability.ITEM_NBT, "java.lang.String")
                .capability(NmsCapability.BLOCK_ENTITY_NBT, "missing.DoesNotExist");

        assertTrue(lookup.hasClass("java.lang.String"));
        assertFalse(lookup.hasClass("missing.DoesNotExist"));
        assertEquals(NmsSupportStatus.SUPPORTED, adapter.check(NmsCapability.ITEM_NBT).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, adapter.check(NmsCapability.BLOCK_ENTITY_NBT).status());
    }

    @Test
    public void releaseMinecraftVersionsCompareWithoutSnapshots() {
        MinecraftVersion v120 = MinecraftVersion.of(1, 20, 0);
        MinecraftVersion v121 = MinecraftVersion.of(1, 21, 0);

        assertTrue(v121.isAfterOrEq(v120));
        assertTrue(v120.isBeforeOrEq(v121));
    }

    @Test
    public void compatibilityMatrixRendersCapabilityRowsForSupportedVersions() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
                .override(new NmsCapabilityCheck(
                        NmsCapability.PACKETS,
                        NmsSupportStatus.PARTIAL,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "packets",
                        MinecraftVersion.of(1, 20, 0),
                        MinecraftVersion.of(1, 21, 0),
                        "packet visuals"
                ));

        NmsCompatibilityMatrix matrix = registry.compatibilityMatrix(List.of(
                MinecraftVersion.of(1, 20, 0),
                MinecraftVersion.of(1, 22, 0)
        ));

        assertEquals(2, matrix.cells(NmsCapability.PACKETS).size());
        assertTrue(matrix.markdownLines().stream().anyMatch(line -> line.contains("1.20.0") && line.contains("PARTIAL")));
        assertTrue(matrix.markdownLines().stream().anyMatch(line -> line.contains("1.22.0") && line.contains("Outside adapter version range")));
    }

    @Test
    public void startupSelfTestsFailOnlyRequiredUnavailableCapabilities() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
                .register(adapter("paper", NmsAccessTier.PAPER_API, NmsSupportStatus.SUPPORTED));
        NmsSelfTestPlan plan = NmsSelfTestPlan.builder()
                .required("mob-goals-required", NmsCapability.MOB_GOALS, "Mob goals must be available for this module.")
                .required("brain-required", NmsCapability.BRAIN_MEMORY, "Brain access must be available for this module.")
                .optional("packets-optional", NmsCapability.PACKETS, "Packet visuals can be disabled.")
                .build();

        NmsSelfTestReport report = registry.selfTest(plan);

        assertFalse(report.passed());
        assertEquals(1, report.failures().size());
        assertTrue(report.lines().stream().anyMatch(line -> line.startsWith("FAIL brain-required")));
        assertTrue(report.lines().stream().anyMatch(line -> line.startsWith("PASS packets-optional")));
    }

    @Test
    public void quarantinePolicyDisablesUnsupportedAndRequiresReviewForUnknown() {
        NmsQuarantinePolicy policy = NmsQuarantinePolicy.strict();

        assertEquals(NmsQuarantineAction.WARN, policy.evaluate(new NmsCapabilityCheck(
                NmsCapability.ITEM_NBT,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper",
                null,
                null,
                "partial"
        )).action());
        assertEquals(NmsQuarantineAction.DISABLE, policy.evaluate(NmsCapabilityCheck.unsupported(
                NmsCapability.PACKETS,
                "none",
                "ProtocolLib missing"
        )).action());
        assertEquals(NmsQuarantineAction.REQUIRE_REVIEW, policy.evaluate(NmsCapabilityCheck.unknown(
                NmsCapability.BLOCK_ENTITY_NBT
        )).action());
    }

    @Test
    public void exampleModulesCoverAdvancedManualTestingSurfaces() {
        Set<NmsCapability> capabilities = NmsExampleModules.defaults().stream()
                .map(module -> module.capability())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(capabilities.contains(NmsCapability.MOB_GOALS));
        assertTrue(capabilities.contains(NmsCapability.ITEM_NBT));
        assertTrue(capabilities.contains(NmsCapability.BLOCK_ENTITY_NBT));
        assertTrue(capabilities.contains(NmsCapability.PACKETS));
        assertTrue(NmsExampleModules.defaults().stream().allMatch(module -> !module.manualCheck().isBlank()));
    }

    @Test
    public void capabilitiesHaveStableKeysCategoriesAndDescriptions() {
        Set<String> keys = java.util.Arrays.stream(NmsCapability.values())
                .map(NmsCapability::key)
                .collect(Collectors.toSet());

        assertEquals(NmsCapability.values().length, keys.size());
        assertEquals("item-snbt", NmsCapability.ITEM_SNBT.key());
        assertEquals(NmsCapabilityCategory.ITEM, NmsCapability.ITEM_SNBT.category());
        assertTrue(java.util.Arrays.stream(NmsCapability.values()).allMatch(capability -> !capability.description().isBlank()));
    }

    @Test
    public void facadeRegistrationReportsGranularCapabilitySurface() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(26, 1, 2), NmsVersion.NONE);
        MobAi.registerPaperCapability(registry);
        MobBrains.registerPaperCapabilities(registry);
        Pathfinders.registerPaperCapability(registry);
        EntityControls.registerPaperCapability(registry);
        CombatControls.registerPaperCapability(registry);
        ItemComponents.registerPaperCapability(registry);
        ItemNbt.registerPaperCapability(registry);
        WorldBlocks.registerPaperCapability(registry);
        InstancedLoot.registerPaperCapability(registry);
        Packets.registerProtocolCapability(registry, true);

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.MOB_GOAL_SNAPSHOTS).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.BRAIN_ACTIVITY_WRITE).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.PATHFINDING_ROUTES).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.ENTITY_SPAWNING).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.COMBAT_ATTACK_COOLDOWN).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.ITEM_BINARY_SERIALIZATION).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.ITEM_SNBT).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.RAW_BLOCK_ENTITY_SNBT).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.RAMCORE_LOOT_GENERATION).status());
        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.PACKET_PROTOCOLLIB_TRANSPORT).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.PACKET_VISUAL_STATE).status());
    }

    @Test
    public void packetTransportCapabilityReflectsProtocolLibAvailability() {
        NmsAccessRegistry registry = NmsAccessRegistry.create(MinecraftVersion.of(26, 1, 2), NmsVersion.NONE);

        Packets.registerProtocolCapability(registry, false);

        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.PACKETS).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.PACKET_PROTOCOLLIB_TRANSPORT).status());
        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.PACKET_VISUAL_STATE).status());
    }

    private static NmsAdapter adapter(String id, NmsAccessTier tier, NmsSupportStatus status) {
        return new NmsAdapter() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public NmsAccessTier tier() {
                return tier;
            }

            @Override
            public Set<NmsCapability> capabilities() {
                return Set.of(NmsCapability.MOB_GOALS);
            }

            @Override
            public NmsCapabilityCheck check(NmsCapability capability) {
                return new NmsCapabilityCheck(capability, status, tier, id, null, null, "test adapter");
            }
        };
    }
}
