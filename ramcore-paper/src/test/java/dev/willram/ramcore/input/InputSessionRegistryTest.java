package dev.willram.ramcore.input;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InputSessionRegistryTest {
    private FakeScheduler scheduler;
    private InputSessionRegistry registry;
    private UUID id;
    private List<String> sent;
    private Player player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.registry = new InputSessionRegistry();
        this.id = UUID.randomUUID();
        this.sent = new ArrayList<>();
        this.player = ProxyFakes.proxy(Player.class, Map.of(
                "getUniqueId", this.id,
                "getName", "will",
                "isOnline", true,
                "sendMessage", (Function<Object[], Object>) args -> {
                    if (args[0] instanceof Component component) {
                        this.sent.add(plain(component));
                    }
                    return null;
                }));
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    private static String plain(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private AsyncChatEvent chat(String message) {
        return new AsyncChatEvent(true, this.player, Set.<Audience>of(), (ChatRenderer) null,
                Component.text(message), Component.text(message), (SignedMessage) null);
    }

    private InputRequest chatRequest() {
        return InputRequest.builder().prompt(Component.text("Type it:")).build();
    }

    private static InputCancelledException.Reason reasonOf(CompletionException e) {
        return assertInstanceOf(InputCancelledException.class, e.getCause()).reason();
    }

    // ---- happy path ----

    @Test
    public void chatCaptureCompletesPromiseAndCancelsEvent() {
        Promise<String> promise = this.registry.request(this.player, chatRequest());
        this.scheduler.tick(3);
        assertEquals(List.of("Type it:"), this.sent, "prompt sent on the player scheduler");
        assertTrue(this.registry.has(this.id));

        AsyncChatEvent event = chat("Steve");
        this.registry.onChat(event);
        assertTrue(event.isCancelled(), "the captured message is not broadcast");

        this.scheduler.tick(3);
        assertTrue(promise.isDone());
        assertEquals("Steve", promise.join());
        assertFalse(this.registry.has(this.id), "session removed once complete");
    }

    @Test
    public void ignoresChatWhenNoRequestActive() {
        AsyncChatEvent event = chat("hi");
        this.registry.onChat(event);
        assertFalse(event.isCancelled());
    }

    // ---- cancel word ----

    @Test
    public void cancelWordFailsWithCancelled() {
        Promise<String> promise = this.registry.request(this.player, chatRequest());
        this.registry.onChat(chat("CANCEL"));
        this.scheduler.tick(3);
        assertEquals(InputCancelledException.Reason.CANCELLED, reasonOf(assertThrows(CompletionException.class, promise::join)));
        assertFalse(this.registry.has(this.id));
    }

    @Test
    public void customCancelWordAndDisabledCancel() {
        Promise<String> promise = this.registry.request(this.player,
                InputRequest.builder().cancelWord("").build());
        this.registry.onChat(chat("cancel")); // cancelling disabled: treated as the value
        this.scheduler.tick(3);
        assertEquals("cancel", promise.join());
    }

    // ---- timeout ----

    @Test
    public void timeoutFailsWithTimeout() {
        Promise<String> promise = this.registry.request(this.player,
                InputRequest.builder().timeout(20).build());
        this.scheduler.tick(19);
        assertFalse(promise.isDone());
        this.scheduler.tick(2); // timeout fires at 20, completion hops one more tick
        assertEquals(InputCancelledException.Reason.TIMEOUT, reasonOf(assertThrows(CompletionException.class, promise::join)));
        assertFalse(this.registry.has(this.id));
    }

    @Test
    public void chatAfterTimeoutIsIgnored() {
        Promise<String> promise = this.registry.request(this.player,
                InputRequest.builder().timeout(20).build());
        this.scheduler.tick(21);
        assertThrows(CompletionException.class, promise::join);
        // a late message must not complete or throw
        this.registry.onChat(chat("late"));
        this.scheduler.tick(3);
    }

    // ---- validation and retries ----

    @Test
    public void validationFailureConsumesRetriesThenExhausts() {
        Promise<String> promise = this.registry.request(this.player, InputRequest.builder()
                .retries(1)
                .validator(s -> s.equals("yes"), Component.text("say yes"))
                .build());

        this.registry.onChat(chat("no"));   // attempt 1 fails, one retry left
        this.scheduler.tick(3);
        assertFalse(promise.isDone(), "still waiting after the first failure");
        assertTrue(this.sent.contains("say yes"));

        this.registry.onChat(chat("nope")); // attempt 2 fails, exhausted
        this.scheduler.tick(3);
        assertEquals(InputCancelledException.Reason.EXHAUSTED, reasonOf(assertThrows(CompletionException.class, promise::join)));
    }

    @Test
    public void validationPassesWithinRetries() {
        Promise<String> promise = this.registry.request(this.player, InputRequest.builder()
                .retries(2)
                .validator(s -> s.equals("yes"), Component.text("say yes"))
                .build());
        this.registry.onChat(chat("no"));
        this.registry.onChat(chat("yes"));
        this.scheduler.tick(3);
        assertEquals("yes", promise.join());
    }

    // ---- parser ----

    @Test
    public void parserProducesTypedValue() {
        Promise<Integer> promise = this.registry.request(this.player,
                InputRequest.builder().build(), Integer::parseInt);
        this.registry.onChat(chat("42"));
        this.scheduler.tick(3);
        assertEquals(42, promise.join());
    }

    @Test
    public void parseFailureConsumesARetry() {
        Promise<Integer> promise = this.registry.request(this.player,
                InputRequest.builder().retries(1).build(), Integer::parseInt);
        this.registry.onChat(chat("x"));   // parse fails, one retry left
        this.scheduler.tick(3);
        assertFalse(promise.isDone());
        this.registry.onChat(chat("7"));
        this.scheduler.tick(3);
        assertEquals(7, promise.join());
    }

    // ---- registry semantics ----

    @Test
    public void newRequestSupersedesThePrevious() {
        Promise<String> first = this.registry.request(this.player, chatRequest());
        Promise<String> second = this.registry.request(this.player, chatRequest());
        this.scheduler.tick(3);

        assertEquals(InputCancelledException.Reason.SUPERSEDED, reasonOf(assertThrows(CompletionException.class, first::join)));
        assertTrue(this.registry.has(this.id));

        this.registry.onChat(chat("done"));
        this.scheduler.tick(3);
        assertEquals("done", second.join());
    }

    @Test
    public void quitCancelsTheRequest() {
        Promise<String> promise = this.registry.request(this.player, chatRequest());
        this.registry.onQuit(this.id);
        this.scheduler.tick(3);
        assertEquals(InputCancelledException.Reason.QUIT, reasonOf(assertThrows(CompletionException.class, promise::join)));
        assertFalse(this.registry.has(this.id));
    }

    @Test
    public void explicitCancelReportsFalseWhenNothingActive() {
        assertFalse(this.registry.cancel(this.id, InputCancelledException.Reason.CANCELLED));
    }

    // ---- request builder validation ----

    @Test
    public void builderRejectsNegativeTimeoutAndRetries() {
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> InputRequest.builder().timeout(-1));
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> InputRequest.builder().retries(-1));
    }
}
