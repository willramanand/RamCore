package dev.willram.ramcore.ability;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.item.nbt.CustomItemIdentity;
import dev.willram.ramcore.item.nbt.CustomItemIdentityStore;
import dev.willram.ramcore.service.Service;
import dev.willram.ramcore.service.ServiceContext;
import dev.willram.ramcore.service.ServiceKey;
import dev.willram.ramcore.stat.StatService;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Owns the {@link AbilityRegistry}, one {@link AbilityCaster} per online player, and the trigger
 * bindings (custom item → ability, hotbar slot → ability, swap-hands → ability). On enable it binds
 * the {@link AbilityTriggerModule}; consumers register {@link AbilityCommandModule} for the command
 * trigger and call {@link #cast(Player, ContentId, AbilityTrigger)} for custom triggers.
 *
 * <p>Stability: experimental.</p>
 */
public final class AbilityService implements Service, Terminable {

    public static final ServiceKey<AbilityService> KEY = ServiceKey.of("abilities", AbilityService.class);

    private final AbilityRegistry registry;
    private final StatService statService;
    private final RamPlugin plugin;
    private final Clock clock;
    private final Map<UUID, AbilityCaster> casters = new ConcurrentHashMap<>();
    private final Map<NamespacedKey, ContentId> itemBindings = new ConcurrentHashMap<>();
    private final Map<Integer, ContentId> hotbarBindings = new ConcurrentHashMap<>();
    private final ComboTracker comboTracker;
    private volatile ContentId swapBinding;
    private volatile boolean closed;

    private AbilityService(@NotNull AbilityRegistry registry, @Nullable StatService statService,
                           @Nullable RamPlugin plugin, @NotNull Clock clock) {
        this.registry = requireNonNull(registry, "registry");
        this.statService = statService;
        this.plugin = plugin;
        this.clock = requireNonNull(clock, "clock");
        this.comboTracker = new ComboTracker(clock);
    }

    /** A service with no plugin attached (tests); nothing listens to Bukkit events. */
    @NotNull
    public static AbilityService create(@NotNull AbilityRegistry registry) {
        return new AbilityService(registry, null, null, Clock.systemUTC());
    }

    /** A service with no plugin attached, reading costs from a stat service. */
    @NotNull
    public static AbilityService create(@NotNull AbilityRegistry registry, @Nullable StatService statService) {
        return new AbilityService(registry, statService, null, Clock.systemUTC());
    }

    /** A test service with an injectable clock for cooldown timing. */
    @NotNull
    public static AbilityService create(@NotNull AbilityRegistry registry, @Nullable StatService statService,
                                        @NotNull Clock clock) {
        return new AbilityService(registry, statService, null, clock);
    }

    /**
     * Registers the service under {@link #KEY} and binds the trigger listeners on enable. Call from
     * {@link RamPlugin#load()}.
     */
    @NotNull
    public static AbilityService install(@NotNull RamPlugin plugin, @NotNull AbilityRegistry registry) {
        return install(plugin, registry, null);
    }

    @NotNull
    public static AbilityService install(@NotNull RamPlugin plugin, @NotNull AbilityRegistry registry,
                                         @Nullable StatService statService) {
        AbilityService service = new AbilityService(registry, statService, plugin, Clock.systemUTC());
        plugin.services().register(KEY, service);
        return service;
    }

    @NotNull
    public AbilityRegistry registry() {
        return this.registry;
    }

    /** The caster for a player, creating it on first use. */
    @NotNull
    public AbilityCaster caster(@NotNull Player player) {
        requireNonNull(player, "player");
        return this.casters.computeIfAbsent(player.getUniqueId(),
                k -> AbilityCaster.create(player, this.statService, this.clock));
    }

    /** Drops (and interrupts) a player's caster, e.g. on quit. */
    public void removeCaster(@NotNull UUID playerId) {
        AbilityCaster caster = this.casters.remove(requireNonNull(playerId, "playerId"));
        if (caster != null) {
            caster.close();
        }
        this.comboTracker.clear(playerId);
    }

    /**
     * Resolves an ability by id and casts it for the player.
     *
     * @param player    the caster
     * @param abilityId the ability id (must be registered)
     * @param trigger   the trigger
     * @return the cast result
     */
    @NotNull
    public CastResult cast(@NotNull Player player, @NotNull ContentId abilityId, @NotNull AbilityTrigger trigger) {
        Ability ability = this.registry.get(abilityId).orElseThrow(() -> RamPreconditions.misuse(
                "no ability registered with id '" + abilityId + "'",
                "register the ability before binding or casting it"));
        CastResult result = caster(player).cast(ability, trigger);
        if (result.started() && trigger != AbilityTrigger.CUSTOM) {
            this.comboTracker.record(player.getUniqueId(), abilityId).ifPresent(combo -> {
                this.comboTracker.clear(player.getUniqueId());
                this.registry.get(combo.finisher())
                        .ifPresent(finisher -> caster(player).cast(finisher, AbilityTrigger.CUSTOM));
            });
        }
        return result;
    }

    /**
     * Registers a combo: casting its steps in order within its window casts the finisher.
     *
     * @param combo the combo
     */
    public void registerCombo(@NotNull AbilityCombo combo) {
        this.comboTracker.register(combo);
    }

    /** The player's caster if one exists (does not create one). */
    @NotNull
    public Optional<AbilityCaster> existingCaster(@NotNull UUID playerId) {
        return Optional.ofNullable(this.casters.get(requireNonNull(playerId, "playerId")));
    }

    /**
     * Interrupts a player's in-progress channel if they have one, without creating a caster.
     *
     * @param player the player
     * @return {@code true} if a channel was interrupted
     */
    public boolean interruptIfCasting(@NotNull Player player) {
        AbilityCaster caster = this.casters.get(player.getUniqueId());
        return caster != null && caster.casting() && caster.interrupt();
    }

    // ---- trigger bindings ----

    public void bindItem(@NotNull NamespacedKey identityKey, @NotNull ContentId abilityId) {
        this.itemBindings.put(requireNonNull(identityKey, "identityKey"), requireNonNull(abilityId, "abilityId"));
    }

    public void bindHotbar(int slot, @NotNull ContentId abilityId) {
        RamPreconditions.checkArgument(slot >= 0 && slot <= 8, "hotbar slot must be 0-8 (was " + slot + ")",
                "pass a slot in 0..8");
        this.hotbarBindings.put(slot, requireNonNull(abilityId, "abilityId"));
    }

    public void bindSwapHands(@NotNull ContentId abilityId) {
        this.swapBinding = requireNonNull(abilityId, "abilityId");
    }

    @NotNull
    public Optional<ContentId> hotbarBinding(int slot) {
        return Optional.ofNullable(this.hotbarBindings.get(slot));
    }

    @NotNull
    public Optional<ContentId> itemBinding(@NotNull NamespacedKey identityKey) {
        return Optional.ofNullable(this.itemBindings.get(identityKey));
    }

    @NotNull
    public Optional<ContentId> swapBinding() {
        return Optional.ofNullable(this.swapBinding);
    }

    // ---- trigger handlers (called by AbilityTriggerModule; return empty when nothing bound) ----

    @NotNull
    public Optional<CastResult> handleItemUse(@NotNull Player player, @Nullable ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        Optional<CustomItemIdentity> identity = CustomItemIdentityStore.read(item);
        if (identity.isEmpty()) {
            return Optional.empty();
        }
        return itemBinding(identity.get().key()).map(id -> cast(player, id, AbilityTrigger.ITEM_USE));
    }

    @NotNull
    public Optional<CastResult> handleHotbar(@NotNull Player player, int slot) {
        return hotbarBinding(slot).map(id -> cast(player, id, AbilityTrigger.HOTBAR));
    }

    @NotNull
    public Optional<CastResult> handleSwapHands(@NotNull Player player) {
        return swapBinding().map(id -> cast(player, id, AbilityTrigger.SWAP_HANDS));
    }

    @Override
    public void enable(@NotNull ServiceContext context) {
        RamPreconditions.checkState(!this.closed, "ability service is closed",
                "Create a new service; a closed one cannot be enabled again.");
        if (this.plugin != null) {
            this.plugin.bindModule(new AbilityTriggerModule(this));
        }
        context.bind(this);
    }

    @Override
    public void disable(@NotNull ServiceContext context) {
        close();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.casters.values().forEach(AbilityCaster::close);
        this.casters.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
