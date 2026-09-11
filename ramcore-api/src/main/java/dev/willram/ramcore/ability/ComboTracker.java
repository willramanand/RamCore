package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * Records each player's recent ability casts (id + timestamp) and matches them against registered
 * {@link AbilityCombo}s. When the most recent casts equal a combo's steps in order and within its
 * window, {@link #record(UUID, ContentId)} returns that combo.
 *
 * <p>Not a service; owned by {@link AbilityService}. Uses an injectable {@link Clock} for tests.</p>
 */
public final class ComboTracker {
    private record Cast(ContentId id, long atMillis) {
    }

    private final Clock clock;
    private final List<AbilityCombo> combos = new CopyOnWriteArrayList<>();
    private final Map<UUID, Deque<Cast>> history = new ConcurrentHashMap<>();

    public ComboTracker() {
        this(Clock.systemUTC());
    }

    public ComboTracker(@NotNull Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    /** Registers a combo to match against. */
    public void register(@NotNull AbilityCombo combo) {
        this.combos.add(requireNonNull(combo, "combo"));
    }

    /** Forgets a player's cast history (e.g. after a combo fires, or on quit). */
    public void clear(@NotNull UUID playerId) {
        this.history.remove(requireNonNull(playerId, "playerId"));
    }

    /**
     * Records a cast and returns the combo it completes, if any.
     *
     * @param playerId  the caster
     * @param abilityId the ability just cast
     * @return the matched combo, or empty
     */
    @NotNull
    public Optional<AbilityCombo> record(@NotNull UUID playerId, @NotNull ContentId abilityId) {
        requireNonNull(playerId, "playerId");
        requireNonNull(abilityId, "abilityId");
        long now = this.clock.millis();
        Deque<Cast> casts = this.history.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        casts.addLast(new Cast(abilityId, now));
        pruneOldest(casts, now);

        for (AbilityCombo combo : this.combos) {
            if (matches(casts, combo, now)) {
                return Optional.of(combo);
            }
        }
        return Optional.empty();
    }

    private void pruneOldest(@NotNull Deque<Cast> casts, long now) {
        long maxWindowMillis = 0L;
        for (AbilityCombo combo : this.combos) {
            maxWindowMillis = Math.max(maxWindowMillis, combo.window().toMillis());
        }
        while (!casts.isEmpty() && now - casts.peekFirst().atMillis() > maxWindowMillis) {
            casts.removeFirst();
        }
        // also cap absolute size to avoid unbounded growth when nothing is registered
        while (casts.size() > 32) {
            casts.removeFirst();
        }
    }

    private static boolean matches(@NotNull Deque<Cast> casts, @NotNull AbilityCombo combo, long now) {
        List<ContentId> steps = combo.steps();
        if (casts.size() < steps.size()) {
            return false;
        }
        Cast[] recent = casts.toArray(new Cast[0]);
        int start = recent.length - steps.size();
        if (now - recent[start].atMillis() > combo.window().toMillis()) {
            return false;
        }
        for (int i = 0; i < steps.size(); i++) {
            if (!recent[start + i].id().equals(steps.get(i))) {
                return false;
            }
        }
        return true;
    }
}
