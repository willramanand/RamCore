package dev.willram.ramcore.content;

import dev.willram.ramcore.content.spec.ItemSpec;
import dev.willram.ramcore.content.spec.RegionSpec;
import dev.willram.ramcore.content.spec.RewardPlanSpec;
import dev.willram.ramcore.economy.Economy;
import dev.willram.ramcore.economy.InMemoryEconomy;
import dev.willram.ramcore.region.RuleRegion;
import dev.willram.ramcore.reward.RewardActionFactories;
import dev.willram.ramcore.reward.RewardContext;
import dev.willram.ramcore.reward.RewardEngine;
import dev.willram.ramcore.reward.RewardPlan;
import dev.willram.ramcore.reward.RewardReport;
import dev.willram.ramcore.serialize.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SpecLoaderTest {
    private final Economy economy = new InMemoryEconomy();
    private final RewardActionFactories factories = RewardActionFactories.standard(this.economy);

    private SpecLoader loader() {
        return SpecLoader.create()
                .deserializer("items", ItemSpec::deserialize)
                .deserializer("regions", RegionSpec::deserialize)
                .deserializer("rewards", node -> RewardPlanSpec.deserialize(node, this.factories));
    }

    private static void write(Path root, String type, String name, String... lines) throws Exception {
        Path dir = root.resolve(type);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void itemSpecRoundTripAndInheritedFields(@TempDir Path root) throws Exception {
        write(root, "items", "base.yml",
                "id: test:base",
                "material: DIAMOND_SWORD",
                "lore:",
                "  - Base line");
        write(root, "items", "epic.yml",
                "id: test:epic",
                "extends: test:base",
                "name: '<gold>Epic'",
                "amount: 1",
                "enchantments:",
                "  sharpness: 5");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        ItemSpec epic = result.get(ContentId.parse("test:epic"), ItemSpec.class).orElseThrow();
        assertEquals("DIAMOND_SWORD", epic.material(), "inherited material");
        assertEquals(List.of("Base line"), epic.lore(), "inherited lore");
        assertEquals(5, epic.enchantments().get("sharpness"));
        assertEquals(2, result.ofType("items", ItemSpec.class).size());
    }

    @Test
    public void badMaterialAndUnknownRewardTypeCollected(@TempDir Path root) throws Exception {
        write(root, "items", "bad.yml", "id: test:bad", "material: NOT_A_MATERIAL");
        write(root, "rewards", "quest.yml",
                "id: test:quest",
                "entries:",
                "  - type: teleport",
                "    x: 1");

        SpecLoadResult result = loader().load(root);
        assertFalse(result.successful());
        assertEquals(2, result.errors().size(), () -> result.errors().toString());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("unknown material 'NOT_A_MATERIAL'")));
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("unknown reward type 'teleport'")));
    }

    @Test
    public void rewardPlanSpecBuildsAWorkingPlan(@TempDir Path root) throws Exception {
        write(root, "rewards", "coins.yml",
                "id: test:coins",
                "entries:",
                "  - id: payout",
                "    type: money",
                "    guaranteed: true",
                "    amount: 250");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        RewardPlanSpec spec = result.get(ContentId.parse("test:coins"), RewardPlanSpec.class).orElseThrow();
        assertEquals(1, spec.guaranteed().size());

        RewardPlan plan = ContentRegistrar.toRewardPlan(spec, this.factories);
        UUID player = UUID.randomUUID();
        RewardReport report = new RewardEngine().execute(plan, RewardContext.of("quest").withSubject(player), new Random(1));
        assertTrue(report.successful());
        assertEquals(250, this.economy.balance(player));
    }

    @Test
    public void regionSpecBuildsRuleRegion(@TempDir Path root) throws Exception {
        write(root, "regions", "spawn.yml",
                "id: test:spawn",
                "priority: 5",
                "shape:",
                "  type: cuboid",
                "  min: { x: 0, y: 0, z: 0, world: world }",
                "  max: { x: 10, y: 10, z: 10, world: world }",
                "rules:",
                "  - id: no-build",
                "    action: BLOCK",
                "    result: DENY");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        RegionSpec spec = result.get(ContentId.parse("test:spawn"), RegionSpec.class).orElseThrow();
        assertEquals(5, spec.priority());
        assertEquals(1, spec.rules().size());

        RuleRegion region = ContentRegistrar.toRuleRegion(ContentId.parse("test:spawn"), spec);
        assertTrue(region.shape().contains(Position.of(5, 5, 5, "world")));
        assertFalse(region.shape().contains(Position.of(50, 5, 5, "world")));
    }
}
