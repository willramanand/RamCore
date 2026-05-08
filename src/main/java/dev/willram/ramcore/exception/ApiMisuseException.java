package dev.willram.ramcore.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when plugin code calls RamCore APIs in an invalid way.
 */
public final class ApiMisuseException extends RuntimeException {
    private final String problem;
    private final String fix;

    public ApiMisuseException(@NotNull String problem, @NotNull String fix) {
        super("RamCore API misuse: " + problem + " Fix: " + fix);
        this.problem = problem;
        this.fix = fix;
    }

    @NotNull
    public String problem() {
        return this.problem;
    }

    @NotNull
    public String fix() {
        return this.fix;
    }
}
