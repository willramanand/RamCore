package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.random.VariableAmount;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class LootPoolBuilderTest {

    @Test
    public void lootPoolAppliesConditionsFunctionsAndBonusRolls() {
        LootPool pool = InstancedLoot.pool("rare")
                .rolls(1)
                .bonusRolls(context -> (int) context.luck())
                .when(LootConditions.metadataEquals("tier", "boss"))
                .entry(InstancedLoot.entry("gem", LootReward.of("gem"))
                        .weight(1)
                        .apply(LootFunctions.amount(VariableAmount.fixed(3)))
                        .apply(LootFunctions.metadata("source", "pool"))
                        .build())
                .build();
        LootTable table = InstancedLoot.table("example:boss")
                .rolls(0)
                .pool(pool)
                .build();

        LootGenerationResult result = InstancedLoot.generator().generate(table, LootContext.builder("boss")
                .luck(2)
                .metadata("tier", "boss")
                .build(), new Random(1));

        assertTrue(result.successful());
        assertEquals(3, result.rewards().size());
        assertEquals(3, result.rewards().getFirst().amount());
        assertEquals("pool", result.rewards().getFirst().metadata().get("source"));
    }

    @Test
    public void lootPoolSkipsWhenPoolConditionFails() {
        LootPool pool = LootPool.builder("nether")
                .when(LootConditions.world("world_nether"))
                .entry(LootPoolEntry.builder("scrap", LootReward.of("scrap")).build())
                .build();
        LootTable table = LootTable.builder(ContentId.parse("example:nether")).rolls(0).pool(pool).build();

        LootGenerationResult result = new LootGenerator().generate(table, LootContext.builder("test")
                .world("world")
                .build(), new Random(1));

        assertTrue(result.rewards().isEmpty());
    }

    @Test
    public void lootCapabilityReportsPartialSupport() {
        NmsAccessRegistry registry = InstancedLoot.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.LOOT_TABLES).status());
    }
}
