package dev.willram.ramcore.ai;

import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Snapshot of RamCore-tracked goals. Paper does not expose vanilla goal priorities,
 * so this snapshot intentionally restores goals registered through this facade.
 */
public record MobGoalSnapshot<T extends Mob>(@NotNull List<MobGoalRegistration<T>> registrations) {
    public MobGoalSnapshot {
        registrations = List.copyOf(registrations);
    }
}
