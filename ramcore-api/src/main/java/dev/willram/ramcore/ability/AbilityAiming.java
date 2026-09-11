package dev.willram.ramcore.ability;

import dev.willram.ramcore.ability.telegraph.Telegraph;
import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;

import static java.util.Objects.requireNonNull;

/**
 * Interactive "aiming" mode: the player enters a state where a {@link Telegraph} preview and an
 * optional glow follow their look each refresh; they confirm with a click or swap-hands, and cancel
 * by moving, quitting, or letting it time out. On confirm the targets are resolved and handed back
 * through the returned {@link Aiming#result()} promise for the caller to cast; on cancel the promise
 * completes exceptionally.
 *
 * <p>Folia-safe: all rendering, glow, and timeout run on the player's scheduler. The render/timeout
 * state machine lives in {@link Aiming} and is driven purely by the scheduler; {@link #begin} adds
 * the Bukkit event wiring (confirm/cancel) on top.</p>
 */
public final class AbilityAiming {

    private AbilityAiming() {
    }

    /**
     * Enters aiming mode for {@code player} with the default {@link AimOptions}.
     *
     * @param player    the aiming player
     * @param targeting resolves the targets under the current aim
     * @param telegraph the preview shown while aiming
     * @return the live aiming session
     */
    @NotNull
    public static Aiming begin(@NotNull Player player, @NotNull AbilityTargeting targeting,
                               @NotNull Telegraph telegraph) {
        return begin(player, targeting, telegraph, AimOptions.defaults());
    }

    /**
     * Enters aiming mode for {@code player}.
     *
     * @param player    the aiming player
     * @param targeting resolves the targets under the current aim
     * @param telegraph the preview shown while aiming
     * @param options   the aim behaviour
     * @return the live aiming session
     */
    @NotNull
    public static Aiming begin(@NotNull Player player, @NotNull AbilityTargeting targeting,
                               @NotNull Telegraph telegraph, @NotNull AimOptions options) {
        Aiming aiming = new Aiming(player, targeting, telegraph, options);
        aiming.wireEvents();
        aiming.start();
        return aiming;
    }

    /**
     * Starts the aiming state machine (render, glow, timeout) without wiring Bukkit events. The
     * caller drives {@link Aiming#confirm()} / {@link Aiming#cancel()} directly. Used by tests and by
     * {@link #begin}, which layers the event wiring on top.
     */
    @NotNull
    static Aiming start(@NotNull Player player, @NotNull AbilityTargeting targeting,
                        @NotNull Telegraph telegraph, @NotNull AimOptions options) {
        Aiming aiming = new Aiming(player, targeting, telegraph, options);
        aiming.start();
        return aiming;
    }

    /**
     * Behaviour for an aiming session.
     *
     * @param refreshTicks how often the preview/glow refresh, in ticks (positive)
     * @param timeoutTicks how long before the aim cancels itself, in ticks (positive)
     * @param glowTargets  whether to glow the entities under the current aim
     * @param cancelOnMove whether walking to another block cancels the aim
     */
    public record AimOptions(long refreshTicks, long timeoutTicks, boolean glowTargets, boolean cancelOnMove) {

        public AimOptions {
            RamPreconditions.checkArgument(refreshTicks > 0L, "aim refreshTicks must be positive",
                    "pass refreshTicks > 0");
            RamPreconditions.checkArgument(timeoutTicks > 0L, "aim timeoutTicks must be positive",
                    "pass timeoutTicks > 0");
        }

        /** Refresh every 2 ticks, 10-second timeout, glow on, cancel on move. */
        @NotNull
        public static AimOptions defaults() {
            return new AimOptions(2L, 200L, true, true);
        }

        @NotNull
        public AimOptions withRefreshTicks(long refreshTicks) {
            return new AimOptions(refreshTicks, this.timeoutTicks, this.glowTargets, this.cancelOnMove);
        }

        @NotNull
        public AimOptions withTimeoutTicks(long timeoutTicks) {
            return new AimOptions(this.refreshTicks, timeoutTicks, this.glowTargets, this.cancelOnMove);
        }

        @NotNull
        public AimOptions withGlowTargets(boolean glowTargets) {
            return new AimOptions(this.refreshTicks, this.timeoutTicks, glowTargets, this.cancelOnMove);
        }

        @NotNull
        public AimOptions withCancelOnMove(boolean cancelOnMove) {
            return new AimOptions(this.refreshTicks, this.timeoutTicks, this.glowTargets, cancelOnMove);
        }
    }

    /**
     * A live aiming session. {@link #confirm()} resolves the current targets and completes
     * {@link #result()}; {@link #cancel()} and {@link #close()} abandon it. All are idempotent once
     * the session has settled.
     */
    public static final class Aiming implements Terminable {
        private final Player player;
        private final AbilityTargeting targeting;
        private final Telegraph telegraph;
        private final AimOptions options;
        private final Promise<List<LivingEntity>> result = Promise.empty();
        private final CompositeTerminable subscriptions = CompositeTerminable.create();
        private final Map<UUID, LivingEntity> glowing = new HashMap<>();

        private Terminable telegraphHandle;
        private Task renderTask;
        private Promise<Void> timeout;
        private boolean settled;
        private boolean closed;

        private Aiming(@NotNull Player player, @NotNull AbilityTargeting targeting,
                       @NotNull Telegraph telegraph, @NotNull AimOptions options) {
            this.player = requireNonNull(player, "player");
            this.targeting = requireNonNull(targeting, "targeting");
            this.telegraph = requireNonNull(telegraph, "telegraph");
            this.options = requireNonNull(options, "options");
        }

        private void start() {
            this.telegraphHandle = this.telegraph.show(this.player);
            this.renderTask = Schedulers.runTimer(this.player, 0L, this.options.refreshTicks(), this::refresh);
            this.timeout = Schedulers.runLater(this.player, this::cancel, this.options.timeoutTicks());
        }

        private void wireEvents() {
            Events.subscribe(PlayerInteractEvent.class)
                    .filter(this::isThisPlayer)
                    .handler(event -> confirm(), this.subscriptions);
            Events.subscribe(PlayerSwapHandItemsEvent.class, EventPriority.HIGH)
                    .filter(this::isThisPlayer)
                    .handler(event -> {
                        event.setCancelled(true);
                        confirm();
                    }, this.subscriptions);
            Events.subscribe(PlayerQuitEvent.class)
                    .filter(this::isThisPlayer)
                    .handler(event -> cancel(), this.subscriptions);
            if (this.options.cancelOnMove()) {
                Events.subscribe(PlayerMoveEvent.class)
                        .filter(this::isThisPlayer)
                        .filter(PlayerMoveEvent::hasChangedBlock)
                        .handler(event -> cancel(), this.subscriptions);
            }
        }

        private boolean isThisPlayer(@NotNull org.bukkit.event.player.PlayerEvent event) {
            return event.getPlayer().getUniqueId().equals(this.player.getUniqueId());
        }

        private void refresh() {
            if (this.settled) {
                return;
            }
            if (!this.player.isValid()) {
                cancel();
                return;
            }
            updateGlow();
        }

        private void updateGlow() {
            if (!this.options.glowTargets()) {
                return;
            }
            Set<UUID> current = new HashSet<>();
            for (LivingEntity target : this.targeting.resolve(this.player)) {
                UUID id = target.getUniqueId();
                current.add(id);
                if (!this.glowing.containsKey(id) && !target.isGlowing()) {
                    this.glowing.put(id, target);
                    Schedulers.run(target, () -> target.setGlowing(true));
                }
            }
            for (Iterator<Map.Entry<UUID, LivingEntity>> it = this.glowing.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, LivingEntity> entry = it.next();
                if (!current.contains(entry.getKey())) {
                    LivingEntity gone = entry.getValue();
                    Schedulers.run(gone, () -> gone.setGlowing(false));
                    it.remove();
                }
            }
        }

        /** Whether the session is still aiming (not yet confirmed, cancelled, or closed). */
        public boolean isActive() {
            return !this.settled;
        }

        /** The targets resolved on confirm; completes exceptionally on cancel/timeout/quit. */
        @NotNull
        public Promise<List<LivingEntity>> result() {
            return this.result;
        }

        /** Confirms the aim: resolves the current targets, completes {@link #result()}, and closes. */
        public void confirm() {
            if (this.settled) {
                return;
            }
            List<LivingEntity> targets = List.copyOf(this.targeting.resolve(this.player));
            this.settled = true;
            this.result.supply(targets);
            close();
        }

        /** Cancels the aim: completes {@link #result()} exceptionally and closes. */
        public void cancel() {
            if (this.settled) {
                return;
            }
            this.settled = true;
            this.result.supplyException(new CancellationException("aim cancelled"));
            close();
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.renderTask != null) {
                this.renderTask.stop();
            }
            if (this.timeout != null) {
                this.timeout.cancel();
            }
            if (this.telegraphHandle != null) {
                this.telegraphHandle.closeSilently();
            }
            for (LivingEntity target : new ArrayList<>(this.glowing.values())) {
                Schedulers.run(target, () -> target.setGlowing(false));
            }
            this.glowing.clear();
            this.subscriptions.closeSilently();
            if (!this.settled) {
                this.settled = true;
                this.result.supplyException(new CancellationException("aim closed"));
            }
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }
    }
}
