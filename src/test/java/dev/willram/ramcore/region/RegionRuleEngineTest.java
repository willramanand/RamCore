package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.serialize.Region;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RegionRuleEngineTest {

    @Test
    public void cuboidRuleDeniesInsidePosition() {
        RegionRuleEngine engine = new RegionRuleEngine();
        ContentId spawn = ContentId.parse("ramcore:spawn");
        RuleRegion region = RuleRegion.builder(spawn, RegionShapes.cuboid(Region.of(
                        Position.of(0, 0, 0, "world"),
                        Position.of(10, 10, 10, "world")
                )))
                .rule(RegionRule.of("deny-block", RegionAction.BLOCK, 0, RegionRuleResult.DENY))
                .build();
        engine.register("RamCore", region);

        RegionDecision decision = engine.evaluate(RegionQuery.of(Position.of(5, 5, 5, "world"), RegionAction.BLOCK));

        assertTrue(decision.denied());
        assertEquals(spawn, decision.region());
        assertEquals("deny-block", decision.rule());
    }

    @Test
    public void outsidePositionPasses() {
        RegionRuleEngine engine = new RegionRuleEngine();
        engine.register("RamCore", RuleRegion.builder(ContentId.parse("ramcore:spawn"), RegionShapes.sphere(
                        Position.of(0, 0, 0, "world"),
                        3
                ))
                .rule(RegionRule.of("deny-command", RegionAction.COMMAND, 0, RegionRuleResult.DENY))
                .build());

        RegionDecision decision = engine.evaluate(RegionQuery.of(Position.of(10, 0, 0, "world"), RegionAction.COMMAND));

        assertEquals(RegionRuleResult.PASS, decision.result());
    }

    @Test
    public void regionPriorityWinsAcrossOverlaps() {
        RegionRuleEngine engine = new RegionRuleEngine();
        RegionShape shape = RegionShapes.sphere(Position.of(0, 0, 0, "world"), 10);
        engine.register("RamCore", RuleRegion.builder(ContentId.parse("ramcore:low"), shape)
                .priority(1)
                .rule(RegionRule.of("deny", RegionAction.INTERACT, 0, RegionRuleResult.DENY))
                .build());
        engine.register("RamCore", RuleRegion.builder(ContentId.parse("ramcore:high"), shape)
                .priority(10)
                .rule(RegionRule.of("allow", RegionAction.INTERACT, 0, RegionRuleResult.ALLOW))
                .build());

        RegionDecision decision = engine.evaluate(RegionQuery.of(Position.of(0, 0, 0, "world"), RegionAction.INTERACT));

        assertTrue(decision.allowed());
        assertEquals(ContentId.parse("ramcore:high"), decision.region());
    }

    @Test
    public void conditionCanPassRule() {
        RegionRuleEngine engine = new RegionRuleEngine();
        engine.register("RamCore", RuleRegion.builder(ContentId.parse("ramcore:arena"), RegionShapes.sphere(
                        Position.of(0, 0, 0, "world"),
                        10
                ))
                .rule(RegionRule.of("deny-locked", RegionAction.ENTER, 0, RegionRuleResult.DENY)
                        .when(query -> Boolean.TRUE.equals(query.metadata().get("locked"))))
                .build());

        RegionDecision pass = engine.evaluate(RegionQuery.of(Position.of(0, 0, 0, "world"), RegionAction.ENTER));
        RegionDecision deny = engine.evaluate(RegionQuery.of(Position.of(0, 0, 0, "world"), RegionAction.ENTER)
                .withMetadata(Map.of("locked", true)));

        assertEquals(RegionRuleResult.PASS, pass.result());
        assertTrue(deny.denied());
    }

    @Test
    public void unregisterOwnerRemovesRegions() {
        RegionRuleEngine engine = new RegionRuleEngine();
        ContentId id = ContentId.parse("ramcore:temp");
        engine.register("Temp", RuleRegion.builder(id, RegionShapes.sphere(Position.of(0, 0, 0, "world"), 1)).build());

        assertEquals(1, engine.unregisterOwner("Temp"));

        assertFalse(engine.contains(id));
    }
}
