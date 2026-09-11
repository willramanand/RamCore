package dev.willram.ramcore.ability;

import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.terminable.TerminableConsumer;
import dev.willram.ramcore.terminable.module.TerminableModule;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Wires the Bukkit events that trigger abilities to an {@link AbilityService}: custom-item use,
 * hotbar-slot select, and swap-hands, plus caster cleanup on quit. Each subscription is bound to the
 * owning {@link TerminableConsumer} so it unregisters with the plugin. The command trigger is
 * separate ({@link AbilityCommandModule}).
 */
public final class AbilityTriggerModule implements TerminableModule {
    private final AbilityService service;

    public AbilityTriggerModule(@NotNull AbilityService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        Events.subscribe(PlayerInteractEvent.class)
                .filter(event -> event.getItem() != null)
                .handler(event -> this.service.handleItemUse(event.getPlayer(), event.getItem()), consumer);

        Events.subscribe(PlayerItemHeldEvent.class)
                .handler(event -> this.service.handleHotbar(event.getPlayer(), event.getNewSlot()), consumer);

        Events.subscribe(PlayerSwapHandItemsEvent.class, EventPriority.HIGH)
                .handler(event -> this.service.handleSwapHands(event.getPlayer())
                        .filter(CastResult::started)
                        .ifPresent(result -> event.setCancelled(true)), consumer);

        Events.subscribe(PlayerQuitEvent.class)
                .handler(event -> this.service.removeCaster(event.getPlayer().getUniqueId()), consumer);
    }
}
