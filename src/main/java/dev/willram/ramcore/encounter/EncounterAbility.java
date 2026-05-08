package dev.willram.ramcore.encounter;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Timed ability in an encounter phase.
 */
public final class EncounterAbility {
    private final String id;
    private final long intervalTicks;
    private long initialDelayTicks;
    private EncounterAbilityAction action = (encounter, ability) -> {
    };

    private EncounterAbility(@NotNull String id, long intervalTicks) {
        this.id = validate(id, "encounter ability id");
        RamPreconditions.checkArgument(intervalTicks > 0, "ability interval must be positive", "Use at least 1 tick.");
        this.intervalTicks = intervalTicks;
        this.initialDelayTicks = intervalTicks;
    }

    @NotNull
    public static EncounterAbility every(@NotNull String id, long intervalTicks) {
        return new EncounterAbility(id, intervalTicks);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    public long intervalTicks() {
        return this.intervalTicks;
    }

    public long initialDelayTicks() {
        return this.initialDelayTicks;
    }

    @NotNull
    public EncounterAbility initialDelay(long initialDelayTicks) {
        RamPreconditions.checkArgument(initialDelayTicks >= 0, "ability initial delay must be non-negative", "Use zero or a positive tick count.");
        this.initialDelayTicks = initialDelayTicks;
        return this;
    }

    @NotNull
    public EncounterAbility action(@NotNull EncounterAbilityAction action) {
        this.action = requireNonNull(action, "action");
        return this;
    }

    void execute(@NotNull EncounterInstance encounter) {
        this.action.execute(encounter, this);
    }

    @NotNull
    private static String validate(@NotNull String value, @NotNull String subject) {
        requireNonNull(value, subject);
        String trimmed = value.trim().toLowerCase();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.:-]+"),
                subject + " contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or colons."
        );
        return trimmed;
    }
}
