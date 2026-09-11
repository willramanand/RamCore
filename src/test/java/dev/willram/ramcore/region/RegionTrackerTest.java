package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.serialize.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RegionTrackerTest {
    private RegionRuleEngine engine;
    private RegionTracker tracker;
    private List<String> transitions;
    private UUID player;

    @BeforeEach
    void setUp() {
        this.engine = new RegionRuleEngine();
        RegionShape a = position -> position.getX() >= 0 && position.getX() <= 10;
        RegionShape b = position -> position.getX() >= 5 && position.getX() <= 15;
        this.engine.register("test", RuleRegion.builder(ContentId.of("test", "a"), a).priority(1).build());
        this.engine.register("test", RuleRegion.builder(ContentId.of("test", "b"), b).priority(2).build());

        this.transitions = new ArrayList<>();
        this.tracker = RegionTracker.create(this.engine, (id, plr, region, entered) ->
                this.transitions.add((entered ? "enter:" : "exit:") + region.id().value()));
        this.player = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        this.engine.close();
    }

    private Position at(double x) {
        return Position.of(x, 64, 0, "world");
    }

    @Test
    public void entersRegionsAtFirstPosition() {
        this.tracker.transition(this.player, null, at(1));
        assertEquals(List.of("enter:a"), this.transitions);
        assertEquals(Set.of(ContentId.of("test", "a")), this.tracker.current(this.player));
    }

    @Test
    public void firesEnterAndExitAcrossOverlap() {
        this.tracker.transition(this.player, null, at(1));   // a
        this.tracker.transition(this.player, null, at(7));   // a + b
        this.tracker.transition(this.player, null, at(12));  // b only
        this.tracker.transition(this.player, null, at(20));  // none

        assertEquals(List.of("enter:a", "enter:b", "exit:a", "exit:b"), this.transitions);
        assertTrue(this.tracker.current(this.player).isEmpty());
    }

    @Test
    public void stayingInSameRegionsFiresNothing() {
        this.tracker.transition(this.player, null, at(6));
        this.transitions.clear();
        this.tracker.transition(this.player, null, at(7)); // still a + b
        assertTrue(this.transitions.isEmpty());
        assertEquals(2, this.tracker.current(this.player).size());
    }

    @Test
    public void clearFiresExitForEveryCurrentRegion() {
        this.tracker.transition(this.player, null, at(7)); // a + b
        this.transitions.clear();
        this.tracker.clear(this.player, null);
        assertTrue(this.transitions.contains("exit:a"));
        assertTrue(this.transitions.contains("exit:b"));
        assertTrue(this.tracker.current(this.player).isEmpty());
    }

    @Test
    public void regionsAtAndRegionLookupWork() {
        assertEquals(2, this.engine.regionsAt(at(7)).size());
        assertEquals(ContentId.of("test", "b"), this.engine.regionsAt(at(7)).get(0).id(), "highest priority first");
        assertTrue(this.engine.region(ContentId.of("test", "a")).isPresent());
        assertTrue(this.engine.region(ContentId.of("test", "z")).isEmpty());
    }
}
