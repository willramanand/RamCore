package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.party.PartyId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ObjectiveTrackerTest {

    @Test
    public void unchainedObjectiveAdvancesMatchingTasksAndCompletes() {
        ContentId id = ContentId.parse("quest:daily");
        ObjectiveDefinition definition = Objectives.objective(id)
                .task(Objectives.task("kill_zombies", ObjectiveAction.KILL, "minecraft:zombie", 2))
                .task(Objectives.task("collect_bones", ObjectiveAction.COLLECT, "minecraft:bone", 3))
                .build();
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());
        List<ObjectiveUpdate> updates = new ArrayList<>();
        ObjectiveTracker tracker = Objectives.tracker().register(definition).listener(updates::add);

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.KILL, "minecraft:zombie"));
        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.KILL, "minecraft:zombie"));
        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.COLLECT, "minecraft:bone").amount(3));

        ObjectiveProgress progress = tracker.progress(subject, id);
        assertTrue(progress.completed(definition));
        assertEquals(2L, progress.current("kill_zombies"));
        assertEquals(3L, progress.current("collect_bones"));
        assertEquals(3, updates.size());
        assertTrue(updates.get(2).objectiveCompleted());
    }

    @Test
    public void chainedObjectiveOnlyAdvancesFirstIncompleteTask() {
        ContentId id = ContentId.parse("quest:chain");
        ObjectiveDefinition definition = Objectives.objective(id)
                .chained(true)
                .task(Objectives.task("enter_spawn", ObjectiveAction.ENTER_REGION, "example:spawn", 1))
                .task(Objectives.task("talk_guide", ObjectiveAction.INTERACT_ENTITY, "example:guide", 1))
                .build();
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());
        ObjectiveTracker tracker = Objectives.tracker().register(definition);

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.INTERACT_ENTITY, "example:guide"));

        ObjectiveProgress progress = tracker.progress(subject, id);
        assertEquals(0L, progress.current("talk_guide"));

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.ENTER_REGION, "example:spawn"));
        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.INTERACT_ENTITY, "example:guide"));

        assertTrue(progress.completed(definition));
    }

    @Test
    public void wildcardTaskMatchesAnyTargetAndCapsAtRequiredAmount() {
        ContentId id = ContentId.parse("battlepass:any_kill");
        ObjectiveDefinition definition = Objectives.objective(id)
                .task(Objectives.task("kills", ObjectiveAction.KILL, "*", 5))
                .build();
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());
        ObjectiveTracker tracker = Objectives.tracker().register(definition);

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.KILL, "minecraft:skeleton").amount(2));
        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.KILL, "minecraft:zombie").amount(10));

        ObjectiveProgress progress = tracker.progress(subject, id);
        assertEquals(5L, progress.current("kills"));
        assertTrue(progress.completed(definition));
    }

    @Test
    public void resetClearsOneObjectiveProgress() {
        ContentId id = ContentId.parse("achievement:mining");
        ObjectiveDefinition definition = Objectives.objective(id)
                .task(Objectives.task("stone", ObjectiveAction.COLLECT, "minecraft:stone", 10))
                .build();
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());
        ObjectiveTracker tracker = Objectives.tracker().register(definition);

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.COLLECT, "minecraft:stone").amount(4));
        tracker.reset(subject, id);

        assertEquals(0L, tracker.progress(subject, id).current("stone"));
    }

    @Test
    public void hiddenDefinitionMarksTaskProgressHidden() {
        ContentId id = ContentId.parse("tutorial:hidden");
        ObjectiveDefinition definition = Objectives.objective(id)
                .hidden(true)
                .task(Objectives.task("secret", ObjectiveAction.CUSTOM, "secret", 1))
                .build();
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());
        ObjectiveTracker tracker = Objectives.tracker().register(definition);

        ObjectiveTaskProgress view = tracker.progress(subject, id).tasks(definition).get(0);

        assertTrue(view.hidden());
        assertFalse(view.completed());
    }

    @Test
    public void partySubjectTracksSharedProgress() {
        ContentId id = ContentId.parse("dungeon:clear");
        ObjectiveDefinition definition = Objectives.objective(id)
                .task(Objectives.task("boss", ObjectiveAction.KILL, "example:boss", 1))
                .build();
        ObjectiveSubject party = ObjectiveSubject.party(PartyId.of("example:party"));
        ObjectiveTracker tracker = Objectives.tracker().register(definition);

        tracker.apply(ObjectiveEvent.of(party, ObjectiveAction.KILL, "example:boss"));

        assertTrue(tracker.progress(party, id).completed(definition));
    }
}
