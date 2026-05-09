package dev.willram.ramcore.event;

import dev.willram.ramcore.event.filter.EventFilters;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class EventUtilitiesTest {

    @Test
    public void subscriptionGroupClosesBoundSubscriptionsInReverseOrder() {
        List<String> closed = new ArrayList<>();
        EventSubscriptionGroup group = EventSubscriptionGroup.create();

        group.bind(new RecordingCloseable("first", closed));
        group.bind(new RecordingCloseable("second", closed));

        assertEquals(2, group.size());
        group.close();

        assertTrue(group.isClosed());
        assertEquals(List.of("second", "first"), closed);
        assertEquals(0, group.size());
    }

    @Test
    public void closedSubscriptionGroupClosesNewBindingsImmediately() {
        List<String> closed = new ArrayList<>();
        EventSubscriptionGroup group = EventSubscriptionGroup.create();
        group.close();

        group.bind(new RecordingCloseable("late", closed));

        assertEquals(List.of("late"), closed);
    }

    @Test
    public void filtersComposeBooleanPredicates() {
        Predicate<Integer> positive = value -> value > 0;
        Predicate<Integer> even = value -> value % 2 == 0;

        assertTrue(EventFilters.all(positive, even).test(4));
        assertFalse(EventFilters.all(positive, even).test(3));
        assertTrue(EventFilters.any(positive, even).test(-2));
        assertFalse(EventFilters.not(positive).test(1));
        assertTrue(EventFilters.always().test("anything"));
        assertFalse(EventFilters.never().test("anything"));
    }

    @Test
    public void typeFilterMatchesSubclassEvents() {
        Predicate<BaseTestEvent> childOnly = EventFilters.type(ChildTestEvent.class);

        assertTrue(childOnly.test(new ChildTestEvent()));
        assertFalse(childOnly.test(new BaseTestEvent()));
    }

    @Test
    public void priorityAndOneShotHelpersCreateBuilders() {
        assertNotNull(Events.lowest(BaseTestEvent.class));
        assertNotNull(Events.low(BaseTestEvent.class));
        assertNotNull(Events.normal(BaseTestEvent.class));
        assertNotNull(Events.high(BaseTestEvent.class));
        assertNotNull(Events.highest(BaseTestEvent.class));
        assertNotNull(Events.monitor(BaseTestEvent.class));
        assertNotNull(Events.once(BaseTestEvent.class));
        assertNotNull(Events.filtered(BaseTestEvent.class, event -> true));
        assertNotNull(Events.group());
    }

    private record RecordingCloseable(String id, List<String> closed) implements AutoCloseable {
        @Override
        public void close() {
            this.closed.add(this.id);
        }
    }

    public static class BaseTestEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static final class ChildTestEvent extends BaseTestEvent {
    }
}
