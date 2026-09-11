package dev.willram.ramcore.input;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * An immutable text-input request: what to ask, how long to wait, how to cancel, and how many
 * retries a failed validation or parse may consume.
 *
 * <p>Build with {@link #builder()}. Stability: experimental.</p>
 */
public final class InputRequest {
    private final InputBackend backend;
    private final @Nullable Component prompt;
    private final long timeoutTicks;
    private final String cancelWord;
    private final int retries;
    private final @Nullable Predicate<String> validator;
    private final @Nullable Component validationError;

    private InputRequest(Builder builder) {
        this.backend = builder.backend;
        this.prompt = builder.prompt;
        this.timeoutTicks = builder.timeoutTicks;
        this.cancelWord = builder.cancelWord;
        this.retries = builder.retries;
        this.validator = builder.validator;
        this.validationError = builder.validationError;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public InputBackend backend() {
        return this.backend;
    }

    @NotNull
    public Optional<Component> prompt() {
        return Optional.ofNullable(this.prompt);
    }

    public long timeoutTicks() {
        return this.timeoutTicks;
    }

    @NotNull
    public String cancelWord() {
        return this.cancelWord;
    }

    public int retries() {
        return this.retries;
    }

    /**
     * Validates one attempt.
     *
     * @param input the raw text
     * @return the error component when invalid, empty when valid
     */
    @NotNull
    Optional<Component> validationError(@NotNull String input) {
        if (this.validator == null || this.validator.test(input)) {
            return Optional.empty();
        }
        return Optional.of(this.validationError != null ? this.validationError : Component.text("Invalid input."));
    }

    public static final class Builder {
        private InputBackend backend = InputBackend.CHAT;
        private @Nullable Component prompt;
        private long timeoutTicks;
        private String cancelWord = "cancel";
        private int retries;
        private @Nullable Predicate<String> validator;
        private @Nullable Component validationError;

        @NotNull
        public Builder backend(@NotNull InputBackend backend) {
            this.backend = requireNonNull(backend, "backend");
            return this;
        }

        @NotNull
        public Builder prompt(@NotNull Component prompt) {
            this.prompt = requireNonNull(prompt, "prompt");
            return this;
        }

        /**
         * Sets the timeout in ticks; {@code 0} (the default) waits forever.
         *
         * @param timeoutTicks ticks before the request times out
         * @return this builder
         */
        @NotNull
        public Builder timeout(long timeoutTicks) {
            RamPreconditions.checkArgument(timeoutTicks >= 0, "timeout must not be negative", "Use 0 for no timeout, or a positive tick count.");
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        /**
         * The word a player types to cancel (case-insensitive, trimmed). Default {@code cancel}.
         * Pass an empty string to disable cancelling by word.
         *
         * @param cancelWord the cancel word
         * @return this builder
         */
        @NotNull
        public Builder cancelWord(@NotNull String cancelWord) {
            this.cancelWord = requireNonNull(cancelWord, "cancelWord");
            return this;
        }

        /**
         * How many extra attempts a failed validation or parse may consume. Default {@code 0}
         * (one attempt total).
         *
         * @param retries the retry count
         * @return this builder
         */
        @NotNull
        public Builder retries(int retries) {
            RamPreconditions.checkArgument(retries >= 0, "retries must not be negative", "Use 0 for a single attempt.");
            this.retries = retries;
            return this;
        }

        @NotNull
        public Builder validator(@NotNull Predicate<String> validator, @NotNull Component error) {
            this.validator = requireNonNull(validator, "validator");
            this.validationError = requireNonNull(error, "error");
            return this;
        }

        @NotNull
        public InputRequest build() {
            return new InputRequest(this);
        }
    }
}
