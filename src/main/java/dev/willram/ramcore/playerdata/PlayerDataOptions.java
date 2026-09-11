package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Tuning for a {@link PlayerDataService}.
 *
 * @param joinPolicy       what to do when a player joins before their data is loaded
 * @param loadTimeout      how long a joined player may wait for data under {@link JoinPolicy#KICK};
 *                         also sizes the pending sweep (entries older than four timeouts are dropped)
 * @param autosaveInterval how often dirty values are written; {@link Duration#ZERO} disables autosave
 * @param kickMessage      shown when {@link JoinPolicy#KICK} fires
 * @param flushTimeout     how long shutdown waits for the final save before logging what did not flush
 */
public record PlayerDataOptions(@NotNull JoinPolicy joinPolicy, @NotNull Duration loadTimeout, @NotNull Duration autosaveInterval, @NotNull Component kickMessage, @NotNull Duration flushTimeout) {

    public PlayerDataOptions {
        requireNonNull(joinPolicy, "joinPolicy");
        requireNonNull(loadTimeout, "loadTimeout");
        requireNonNull(autosaveInterval, "autosaveInterval");
        requireNonNull(kickMessage, "kickMessage");
        requireNonNull(flushTimeout, "flushTimeout");
        RamPreconditions.checkArgument(!loadTimeout.isNegative() && !loadTimeout.isZero(), "loadTimeout must be positive", "Use something like Duration.ofSeconds(10).");
        RamPreconditions.checkArgument(!autosaveInterval.isNegative(), "autosaveInterval must not be negative", "Use Duration.ZERO to disable autosave.");
        RamPreconditions.checkArgument(!flushTimeout.isNegative() && !flushTimeout.isZero(), "flushTimeout must be positive", "Use something like Duration.ofSeconds(30).");
    }

    /**
     * {@link JoinPolicy#KICK}, 10 second load timeout, 5 minute autosave, 30 second flush.
     *
     * @return the defaults
     */
    @NotNull
    public static PlayerDataOptions defaults() {
        return new PlayerDataOptions(
                JoinPolicy.KICK,
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
                Component.text("Your data is still loading, please rejoin in a moment."),
                Duration.ofSeconds(30)
        );
    }

    @NotNull
    public PlayerDataOptions withJoinPolicy(@NotNull JoinPolicy joinPolicy) {
        return new PlayerDataOptions(joinPolicy, this.loadTimeout, this.autosaveInterval, this.kickMessage, this.flushTimeout);
    }

    @NotNull
    public PlayerDataOptions withLoadTimeout(@NotNull Duration loadTimeout) {
        return new PlayerDataOptions(this.joinPolicy, loadTimeout, this.autosaveInterval, this.kickMessage, this.flushTimeout);
    }

    @NotNull
    public PlayerDataOptions withAutosaveInterval(@NotNull Duration autosaveInterval) {
        return new PlayerDataOptions(this.joinPolicy, this.loadTimeout, autosaveInterval, this.kickMessage, this.flushTimeout);
    }

    @NotNull
    public PlayerDataOptions withKickMessage(@NotNull Component kickMessage) {
        return new PlayerDataOptions(this.joinPolicy, this.loadTimeout, this.autosaveInterval, kickMessage, this.flushTimeout);
    }

    @NotNull
    public PlayerDataOptions withFlushTimeout(@NotNull Duration flushTimeout) {
        return new PlayerDataOptions(this.joinPolicy, this.loadTimeout, this.autosaveInterval, this.kickMessage, flushTimeout);
    }
}
