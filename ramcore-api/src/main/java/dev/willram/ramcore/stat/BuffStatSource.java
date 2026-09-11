package dev.willram.ramcore.stat;

import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StatSource} of timed, per-player buffs. Each buff is a {@link StatModifier} with an
 * optional expiry; expired buffs are dropped lazily when the player's modifiers are next collected.
 *
 * <p>Pass an {@code onChange} callback (typically {@code service::invalidate}) so that adding or
 * clearing a buff invalidates the affected player's cached snapshot.</p>
 */
public final class BuffStatSource implements StatSource, Terminable {

    private record Buff(StatModifier modifier, @Nullable Instant expiresAt) {
        boolean expired(Instant now) {
            return this.expiresAt != null && !now.isBefore(this.expiresAt);
        }
    }

    private final Clock clock;
    private final Consumer<UUID> onChange;
    private final Map<UUID, List<Buff>> buffs = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public BuffStatSource() {
        this(Clock.systemUTC(), id -> {
        });
    }

    public BuffStatSource(@NotNull Clock clock, @NotNull Consumer<UUID> onChange) {
        this.clock = requireNonNull(clock, "clock");
        this.onChange = requireNonNull(onChange, "onChange");
    }

    /**
     * Adds a permanent buff (until cleared).
     *
     * @param playerId the player
     * @param modifier the modifier
     */
    public void add(@NotNull UUID playerId, @NotNull StatModifier modifier) {
        add(playerId, modifier, null);
    }

    /**
     * Adds a timed buff.
     *
     * @param playerId the player
     * @param modifier the modifier
     * @param duration how long the buff lasts; {@code null} for permanent
     */
    public void add(@NotNull UUID playerId, @NotNull StatModifier modifier, @Nullable Duration duration) {
        requireNonNull(playerId, "playerId");
        requireNonNull(modifier, "modifier");
        Instant expiresAt = duration == null ? null : this.clock.instant().plus(duration);
        this.buffs.computeIfAbsent(playerId, k -> new ArrayList<>()).add(new Buff(modifier, expiresAt));
        this.onChange.accept(playerId);
    }

    /**
     * Removes all buffs for a player.
     *
     * @param playerId the player
     */
    public void clear(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        if (this.buffs.remove(playerId) != null) {
            this.onChange.accept(playerId);
        }
    }

    @Override
    @NotNull
    public List<StatModifier> modifiers(@NotNull Player player) {
        List<Buff> active = this.buffs.get(player.getUniqueId());
        if (active == null || active.isEmpty()) {
            return List.of();
        }
        Instant now = this.clock.instant();
        List<StatModifier> out = new ArrayList<>(active.size());
        boolean removed = active.removeIf(buff -> buff.expired(now));
        for (Buff buff : active) {
            out.add(buff.modifier());
        }
        if (removed && active.isEmpty()) {
            this.buffs.remove(player.getUniqueId());
        }
        return out;
    }

    @Override
    public void close() {
        this.closed = true;
        this.buffs.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
