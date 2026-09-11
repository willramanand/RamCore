package dev.willram.ramcore.input;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.promise.Promise;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Entry point for asking a player for text. Backed by a process-wide {@link InputSessionRegistry}.
 *
 * <p>Call {@link #install(RamPlugin)} once (typically from {@code RamPlugin.enable()}) so chat,
 * inventory and quit events reach the registry. Then request input:</p>
 *
 * <pre>{@code
 * PlayerInput.request(player, InputRequest.builder()
 *         .prompt(Component.text("Type a name, or 'cancel':"))
 *         .timeout(20 * 30)
 *         .build())
 *     .thenAcceptSync(name -> ...)
 *     .exceptionallySync(error -> ...);   // InputCancelledException on cancel/timeout/quit
 * }</pre>
 *
 * <p>Stability: experimental (CHAT, ANVIL); SIGN is Paper-experimental and currently falls back to
 * CHAT.</p>
 */
public final class PlayerInput {
    private static final InputSessionRegistry REGISTRY = new InputSessionRegistry();

    private PlayerInput() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Registers the event listener that feeds the shared registry. Idempotent per plugin is not
     * enforced; call once.
     *
     * @param plugin the owning plugin
     * @return the shared registry
     */
    @NotNull
    public static InputSessionRegistry install(@NotNull RamPlugin plugin) {
        requireNonNull(plugin, "plugin").registerListener(new InputListener(REGISTRY));
        return REGISTRY;
    }

    /**
     * The shared registry.
     *
     * @return the registry
     */
    @NotNull
    public static InputSessionRegistry registry() {
        return REGISTRY;
    }

    /**
     * Asks the player for text.
     *
     * @param player  the player
     * @param request the request
     * @return a promise of the entered text, failed with {@link InputCancelledException} otherwise
     */
    @NotNull
    public static Promise<String> request(@NotNull Player player, @NotNull InputRequest request) {
        return REGISTRY.request(player, request);
    }

    /**
     * Asks the player for text and parses it; a parse failure consumes a retry.
     *
     * @param player  the player
     * @param request the request
     * @param parser  the parser
     * @param <T>     the parsed type
     * @return a promise of the parsed value
     */
    @NotNull
    public static <T> Promise<T> request(@NotNull Player player, @NotNull InputRequest request, @NotNull InputParser<T> parser) {
        return REGISTRY.request(player, request, parser);
    }
}
