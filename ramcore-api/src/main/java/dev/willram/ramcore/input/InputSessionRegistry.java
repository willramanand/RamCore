package dev.willram.ramcore.input;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.utils.RamLog;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Tracks one active {@link InputSession} per player. A new request supersedes the previous one for
 * that player. The event-handling methods ({@link #onChat}, {@link #onQuit}, and the anvil hooks)
 * are called by {@link InputListener} in production and directly by tests, so the registry never
 * registers Bukkit listeners itself.
 *
 * <p>Stability: experimental. Folia note: chat arrives async and completions hop to the player
 * scheduler; a session is stored under the player id in a concurrent map.</p>
 */
public final class InputSessionRegistry {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Map<UUID, InputSession<?>> sessions = new ConcurrentHashMap<>();

    /**
     * Starts a request for the player, cancelling any request already running for them.
     *
     * @param player  the player
     * @param request the request
     * @param parser  turns the raw text into the result
     * @param <T>     the result type
     * @return a promise completed on the player's scheduler with the parsed value, or failed with an
     * {@link InputCancelledException}
     */
    @NotNull
    public <T> Promise<T> request(@NotNull Player player, @NotNull InputRequest request, @NotNull InputParser<T> parser) {
        requireNonNull(player, "player");
        requireNonNull(request, "request");
        requireNonNull(parser, "parser");

        UUID id = player.getUniqueId();
        InputSession<?> previous = this.sessions.remove(id);
        if (previous != null) {
            previous.finishExceptionally(InputCancelledException.Reason.SUPERSEDED);
        }

        InputSession<T> session = new InputSession<>(player, request, parser, () -> this.sessions.remove(id));
        this.sessions.put(id, session);
        session.start();
        open(session);
        return session.promise();
    }

    /**
     * Starts a request that yields the raw text.
     *
     * @param player  the player
     * @param request the request
     * @return the promise
     */
    @NotNull
    public Promise<String> request(@NotNull Player player, @NotNull InputRequest request) {
        return request(player, request, InputParser.identity());
    }

    /**
     * Whether the player has an active request.
     *
     * @param playerId the player id
     * @return true if a request is in flight
     */
    public boolean has(@NotNull UUID playerId) {
        return this.sessions.containsKey(requireNonNull(playerId, "playerId"));
    }

    /**
     * Cancels the player's active request, if any.
     *
     * @param playerId the player id
     * @param reason   why
     * @return true if a request was cancelled
     */
    public boolean cancel(@NotNull UUID playerId, @NotNull InputCancelledException.Reason reason) {
        InputSession<?> session = this.sessions.remove(requireNonNull(playerId, "playerId"));
        if (session == null) {
            return false;
        }
        session.finishExceptionally(reason);
        return true;
    }

    // ---- backend openers ----

    private void open(@NotNull InputSession<?> session) {
        switch (session.request().backend()) {
            case CHAT -> {
                // nothing to open; the prompt was sent by session.start()
            }
            case ANVIL -> Schedulers.run(session.player(), () -> openAnvil(session));
            case SIGN -> {
                // A virtual sign edit needs packet support that is not wired yet; fall back to chat.
                RamLog.warn("input SIGN backend is not implemented natively; falling back to CHAT for " + session.player().getName());
            }
        }
    }

    private void openAnvil(@NotNull InputSession<?> session) {
        Player player = session.player();
        if (!player.isOnline()) {
            session.finishExceptionally(InputCancelledException.Reason.OFFLINE);
            return;
        }
        InventoryView view = player.openAnvil(player.getLocation(), true);
        session.view(view);
    }

    // ---- event hooks (called by InputListener or tests) ----

    /**
     * Feeds a chat message to the player's CHAT request. Cancels the event so the message is not
     * broadcast. No-op when the player has no CHAT request in flight.
     *
     * @param event the chat event
     */
    public void onChat(@NotNull AsyncChatEvent event) {
        requireNonNull(event, "event");
        InputSession<?> session = this.sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.request().backend() != InputBackend.CHAT) {
            return;
        }
        event.setCancelled(true);
        session.offer(PLAIN.serialize(event.message()));
    }

    /**
     * Handles a result-slot click in an anvil opened for a request.
     *
     * @param player    the clicking player
     * @param view      the inventory view clicked
     * @param rawSlot   the raw slot
     * @return true when the click belonged to an active anvil request (and should be cancelled)
     */
    public boolean onAnvilClick(@NotNull Player player, @NotNull InventoryView view, int rawSlot) {
        InputSession<?> session = this.sessions.get(requireNonNull(player, "player").getUniqueId());
        if (session == null || session.request().backend() != InputBackend.ANVIL || session.view() != view) {
            return false;
        }
        if (rawSlot != 2 || !(view instanceof AnvilView anvil)) {
            return true; // consume other slots but do not complete
        }
        String text = anvil.getRenameText();
        Schedulers.run(player, player::closeInventory);
        session.offer(text == null ? "" : text);
        return true;
    }

    /**
     * Cancels a request whose backing inventory the player just closed.
     *
     * @param player the player
     * @param view   the closed view
     */
    public void onInventoryClose(@NotNull Player player, @NotNull InventoryView view) {
        InputSession<?> session = this.sessions.get(requireNonNull(player, "player").getUniqueId());
        if (session != null && session.view() == view && session.active()) {
            session.finishExceptionally(InputCancelledException.Reason.CANCELLED);
        }
    }

    /**
     * Cancels the player's request because they left.
     *
     * @param playerId the player id
     */
    public void onQuit(@NotNull UUID playerId) {
        cancel(playerId, InputCancelledException.Reason.QUIT);
    }
}
