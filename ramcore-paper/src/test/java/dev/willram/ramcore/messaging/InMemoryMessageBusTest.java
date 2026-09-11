package dev.willram.ramcore.messaging;

import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InMemoryMessageBusTest {
    private FakeScheduler scheduler;
    private InMemoryMessageBus bus;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.bus = new InMemoryMessageBus();
    }

    @AfterEach
    void tearDown() {
        this.bus.close();
        this.scheduler.close();
    }

    @Test
    public void deliversPayloadOnTheAsyncScheduler() {
        AtomicReference<String> received = new AtomicReference<>();
        this.bus.subscribe("chat", (channel, payload) -> received.set(new String(payload, StandardCharsets.UTF_8)));

        this.bus.publish("chat", "hello".getBytes(StandardCharsets.UTF_8));
        assertNull(received.get(), "delivery is async, not inline");

        this.scheduler.runAsync();
        assertEquals("hello", received.get());
    }

    @Test
    public void otherChannelsAreNotDelivered() {
        AtomicReference<String> received = new AtomicReference<>();
        this.bus.subscribe("a", (channel, payload) -> received.set("got"));
        this.bus.publish("b", new byte[]{1});
        this.scheduler.runAsync();
        assertNull(received.get());
    }

    @Test
    public void unsubscribeStopsDelivery() {
        AtomicReference<String> received = new AtomicReference<>();
        var handle = this.bus.subscribe("chat", (channel, payload) -> received.set("got"));
        handle.closeSilently();
        this.bus.publish("chat", new byte[]{1});
        this.scheduler.runAsync();
        assertNull(received.get());
    }

    @Test
    public void typedPublishAndSubscribeRoundTrip() {
        AtomicReference<Ping> received = new AtomicReference<>();
        MessageCodec<Ping> codec = MessageCodec.gson(Ping.class);
        this.bus.subscribe("ping", codec, (channel, message) -> received.set(message));

        this.bus.publish("ping", new Ping("node-1", 7), codec);
        this.scheduler.runAsync();

        assertEquals("node-1", received.get().node());
        assertEquals(7, received.get().count());
    }

    @Test
    public void publishWithNoSubscribersCompletesQuietly() {
        assertTrue(this.bus.publish("empty", new byte[]{1}).isDone());
    }

    @Test
    public void closedBusDoesNotDeliver() {
        AtomicReference<String> received = new AtomicReference<>();
        this.bus.subscribe("chat", (channel, payload) -> received.set("got"));
        this.bus.close();
        assertTrue(this.bus.isClosed());
        this.bus.publish("chat", new byte[]{1});
        this.scheduler.runAsync();
        assertNull(received.get());
        assertFalse(this.bus.publish("chat", new byte[]{1}).isDone() && received.get() != null);
    }

    record Ping(String node, int count) {
    }
}
