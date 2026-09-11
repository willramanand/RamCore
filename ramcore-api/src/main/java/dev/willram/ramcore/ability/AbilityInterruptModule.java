package dev.willram.ramcore.ability;

import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.terminable.TerminableConsumer;
import dev.willram.ramcore.terminable.module.TerminableModule;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Cancels an in-progress channel when the caster takes damage or moves to a different block. Opt-in:
 * bind it alongside {@link AbilityService} only if you want channels to break. An interrupted channel
 * does not run the ability's action or start its cooldown.
 */
public final class AbilityInterruptModule implements TerminableModule {
    private final AbilityService service;
    private final boolean onDamage;
    private final boolean onMove;

    /** Interrupts on both damage and movement. */
    public AbilityInterruptModule(@NotNull AbilityService service) {
        this(service, true, true);
    }

    public AbilityInterruptModule(@NotNull AbilityService service, boolean onDamage, boolean onMove) {
        this.service = requireNonNull(service, "service");
        this.onDamage = onDamage;
        this.onMove = onMove;
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        if (this.onDamage) {
            Events.subscribe(EntityDamageEvent.class)
                    .filter(event -> event.getEntity() instanceof Player)
                    .handler(event -> this.service.interruptIfCasting((Player) event.getEntity()), consumer);
        }
        if (this.onMove) {
            Events.subscribe(PlayerMoveEvent.class)
                    .filter(AbilityInterruptModule::changedBlock)
                    .handler(event -> this.service.interruptIfCasting(event.getPlayer()), consumer);
        }
    }

    private static boolean changedBlock(@NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return false;
        }
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
