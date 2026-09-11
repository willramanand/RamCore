package dev.willram.ramcore.input;

import dev.willram.ramcore.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * One in-flight input request for one player. Backend-agnostic: it owns the promise, the retry
 * budget, validation and parsing; the {@link InputSessionRegistry} feeds it raw text and opens the
 * backing UI. All promise completions hop to the player's scheduler.
 *
 * @param <T> the parsed result type
 */
final class InputSession<T> {
    private final Player player;
    private final InputRequest request;
    private final InputParser<T> parser;
    private final dev.willram.ramcore.promise.Promise<T> promise = dev.willram.ramcore.promise.Promise.empty();
    private final Runnable onFinish;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private int remainingAttempts;
    private @Nullable InventoryView view;

    InputSession(@NotNull Player player, @NotNull InputRequest request, @NotNull InputParser<T> parser, @NotNull Runnable onFinish) {
        this.player = requireNonNull(player, "player");
        this.request = requireNonNull(request, "request");
        this.parser = requireNonNull(parser, "parser");
        this.onFinish = requireNonNull(onFinish, "onFinish");
        this.remainingAttempts = request.retries() + 1;
    }

    @NotNull
    dev.willram.ramcore.promise.Promise<T> promise() {
        return this.promise;
    }

    @NotNull
    Player player() {
        return this.player;
    }

    @NotNull
    InputRequest request() {
        return this.request;
    }

    void view(@Nullable InventoryView view) {
        this.view = view;
    }

    @Nullable
    InventoryView view() {
        return this.view;
    }

    boolean active() {
        return !this.done.get();
    }

    /** Sends the prompt (if any) and arms the timeout. */
    void start() {
        this.request.prompt().ifPresent(this::sendToPlayer);
        long timeout = this.request.timeoutTicks();
        if (timeout > 0) {
            Schedulers.runLater(this.player, timeout, () -> finishExceptionally(InputCancelledException.Reason.TIMEOUT));
        }
    }

    /** Processes one attempt of raw text. */
    void offer(@NotNull String raw) {
        if (this.done.get()) {
            return;
        }
        String input = requireNonNull(raw, "raw");
        String cancelWord = this.request.cancelWord();
        if (!cancelWord.isEmpty() && input.strip().equalsIgnoreCase(cancelWord)) {
            finishExceptionally(InputCancelledException.Reason.CANCELLED);
            return;
        }
        var error = this.request.validationError(input);
        if (error.isPresent()) {
            attemptFailed(error.get());
            return;
        }
        T value;
        try {
            value = this.parser.parse(input);
        } catch (Exception ex) {
            attemptFailed(Component.text("Could not read that, try again."));
            return;
        }
        finish(value);
    }

    private void attemptFailed(@NotNull Component message) {
        this.remainingAttempts--;
        if (this.remainingAttempts <= 0) {
            finishExceptionally(InputCancelledException.Reason.EXHAUSTED);
            return;
        }
        sendToPlayer(message);
    }

    void finish(@NotNull T value) {
        if (this.done.compareAndSet(false, true)) {
            this.onFinish.run();
            Schedulers.run(this.player, () -> this.promise.supply(value));
        }
    }

    void finishExceptionally(@NotNull InputCancelledException.Reason reason) {
        if (this.done.compareAndSet(false, true)) {
            this.onFinish.run();
            Schedulers.run(this.player, () -> this.promise.supplyException(new InputCancelledException(reason)));
        }
    }

    private void sendToPlayer(@NotNull Component message) {
        Schedulers.run(this.player, () -> this.player.sendMessage(message));
    }
}
