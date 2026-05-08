package dev.willram.ramcore.exception;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Fail-fast validation helpers for public RamCore APIs.
 */
public final class RamPreconditions {

    public static void checkArgument(boolean condition, @NotNull String problem, @NotNull String fix) {
        if (!condition) {
            throw misuse(problem, fix);
        }
    }

    public static void checkState(boolean condition, @NotNull String problem, @NotNull String fix) {
        if (!condition) {
            throw misuse(problem, fix);
        }
    }

    @NotNull
    public static String notBlank(@NotNull String value, @NotNull String subject) {
        requireNonNull(value, subject);
        String trimmed = value.trim();
        checkArgument(
                !trimmed.isEmpty() && trimmed.chars().noneMatch(Character::isWhitespace),
                subject + " must not be blank or contain whitespace",
                "Use a stable identifier such as 'example.feature' or 'feature_name'."
        );
        return trimmed;
    }

    @NotNull
    public static ApiMisuseException misuse(@NotNull String problem, @NotNull String fix) {
        return new ApiMisuseException(requireNonNull(problem, "problem"), requireNonNull(fix, "fix"));
    }

    private RamPreconditions() {
    }
}
