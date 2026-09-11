package dev.willram.ramcore.ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Resolves the living entities an ability affects when a player casts it. Built-ins are in
 * {@link AbilityTargets}.
 */
@FunctionalInterface
public interface AbilityTargeting {

    /**
     * The targets for a cast, or an empty list. Never returns {@code null}.
     *
     * @param caster the casting player
     * @return the resolved targets
     */
    @NotNull
    List<LivingEntity> resolve(@NotNull Player caster);
}
