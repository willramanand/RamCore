package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;

/**
 * Entry points for RamCore party helpers.
 */
public final class Parties {

    @NotNull
    public static PartyManager manager() {
        return PartyManager.create();
    }

    @NotNull
    public static PartyManager manager(@NotNull PartyOptions options) {
        return PartyManager.create(options);
    }

    @NotNull
    public static PartyManager manager(@NotNull PartyOptions options, @NotNull Clock clock) {
        return PartyManager.create(options, clock);
    }

    @NotNull
    public static PartyOptions options() {
        return PartyOptions.defaults();
    }

    private Parties() {
    }
}
