package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.presentation.PresentationContext;
import dev.willram.ramcore.presentation.PresentationEffect;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.stat.StatService;
import dev.willram.ramcore.stat.StatSnapshot;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Per-player ability state machine. Gates a cast on cooldown then {@link StatCost} then the action's
 * {@code validate}; instant abilities execute immediately, channelled abilities schedule a cast timer
 * on the player's scheduler and execute when it elapses. One channel at a time; {@link #interrupt()}
 * cancels an in-progress channel. The cooldown starts when the ability executes.
 *
 * <p>Not thread-safe: an instance belongs to one player and is used on that player's thread. Cooldown
 * timing uses an injectable {@link Clock} for deterministic tests.</p>
 *
 * <p>Cooldowns are tracked here per ability (each ability carries its own {@link Ability#cooldown()}
 * duration) rather than through a shared {@code CooldownTracker}, whose single base timeout and
 * system clock do not fit per-ability durations or deterministic tests.</p>
 */
public final class AbilityCaster implements Terminable {
    private final Player player;
    private final StatService statService;
    private final Clock clock;
    private final Map<ContentId, Long> readyAtMillis = new HashMap<>();

    private Ability casting;
    private Promise<Void> castPromise;
    private Task channelTask;
    private boolean closed;

    private AbilityCaster(@NotNull Player player, @Nullable StatService statService, @NotNull Clock clock) {
        this.player = requireNonNull(player, "player");
        this.statService = statService;
        this.clock = requireNonNull(clock, "clock");
    }

    /**
     * A caster with no stat service: abilities with a {@link StatCost} are always denied
     * ({@link AbilityCastStatus#INSUFFICIENT_COST}) since no stats are available.
     *
     * @param player the player
     * @return the caster
     */
    @NotNull
    public static AbilityCaster create(@NotNull Player player) {
        return new AbilityCaster(player, null, Clock.systemUTC());
    }

    /**
     * A caster that reads costs from the given stat service.
     *
     * @param player      the player
     * @param statService the stat service
     * @return the caster
     */
    @NotNull
    public static AbilityCaster create(@NotNull Player player, @NotNull StatService statService) {
        return new AbilityCaster(player, requireNonNull(statService, "statService"), Clock.systemUTC());
    }

    /**
     * A caster with an injectable clock for cooldown timing (tests).
     *
     * @param player      the player
     * @param statService the stat service, or {@code null}
     * @param clock       the cooldown clock
     * @return the caster
     */
    @NotNull
    public static AbilityCaster create(@NotNull Player player, @Nullable StatService statService, @NotNull Clock clock) {
        return new AbilityCaster(player, statService, clock);
    }

    /**
     * Attempts to cast an ability.
     *
     * @param ability the ability
     * @param trigger what triggered the cast
     * @return the result
     */
    @NotNull
    public CastResult cast(@NotNull Ability ability, @NotNull AbilityTrigger trigger) {
        requireNonNull(ability, "ability");
        requireNonNull(trigger, "trigger");
        ContentId id = ability.id();

        if (this.closed || this.casting != null) {
            return CastResult.of(AbilityCastStatus.BUSY, id);
        }

        long now = this.clock.millis();
        Long ready = this.readyAtMillis.get(id);
        if (ready != null && now < ready) {
            return CastResult.onCooldown(id, ready - now);
        }

        StatSnapshot snapshot = this.statService != null ? this.statService.snapshot(this.player) : StatSnapshot.empty();
        if (ability.cost().isPresent() && !ability.cost().get().affordable(snapshot)) {
            return CastResult.of(AbilityCastStatus.INSUFFICIENT_COST, id);
        }

        List<LivingEntity> targets = ability.targeting().resolve(this.player);
        AbilityContext context = AbilityContext.of(this.player, targets, snapshot, trigger);

        List<String> errors = ability.action().validate(context);
        if (!errors.isEmpty()) {
            return CastResult.invalid(id, errors);
        }

        if (ability.channel().isPresent()) {
            return beginChannel(ability, context);
        }

        if (ability.castTicks() <= 0L) {
            execute(ability, context);
            return CastResult.of(AbilityCastStatus.CAST, id);
        }

        this.casting = ability;
        this.castPromise = Schedulers.runLater(this.player, () -> {
            this.casting = null;
            this.castPromise = null;
            execute(ability, context);
        }, ability.castTicks());
        return CastResult.of(AbilityCastStatus.CASTING, id);
    }

    private CastResult beginChannel(@NotNull Ability ability, @NotNull AbilityContext context) {
        AbilityChannel channel = ability.channel().orElseThrow();
        int total = channel.tickCount();
        int[] index = {0};
        this.casting = ability;
        this.channelTask = Schedulers.runTimerTask(this.player, channel.intervalTicks(), channel.intervalTicks(),
                task -> {
                    index[0]++;
                    channel.onTick().tick(context, index[0]);
                    if (index[0] >= total) {
                        task.stop();
                        this.casting = null;
                        this.channelTask = null;
                        execute(ability, context);
                    }
                });
        return CastResult.of(AbilityCastStatus.CASTING, ability.id());
    }

    private void execute(@NotNull Ability ability, @NotNull AbilityContext context) {
        ability.action().run(context);
        playEffects(ability);
        long cooldownMillis = ability.cooldown().toMillis();
        if (cooldownMillis > 0L) {
            this.readyAtMillis.put(ability.id(), this.clock.millis() + cooldownMillis);
        }
    }

    private void playEffects(@NotNull Ability ability) {
        if (ability.effects().isEmpty()) {
            return;
        }
        PresentationContext presentation = PresentationContext.of(this.player);
        for (PresentationEffect effect : ability.effects()) {
            effect.play(presentation);
        }
    }

    /**
     * Cancels an in-progress channel, if any. No cooldown is applied to an interrupted cast.
     *
     * @return {@code true} if a channel was cancelled
     */
    public boolean interrupt() {
        if (this.casting == null) {
            return false;
        }
        if (this.castPromise != null) {
            this.castPromise.cancel();
        }
        if (this.channelTask != null) {
            this.channelTask.stop();
        }
        this.casting = null;
        this.castPromise = null;
        this.channelTask = null;
        return true;
    }

    /** Whether a channel is in progress. */
    public boolean casting() {
        return this.casting != null;
    }

    /** The ability currently being channelled, if any. */
    @NotNull
    public Optional<ContentId> castingAbility() {
        return Optional.ofNullable(this.casting).map(Ability::id);
    }

    /**
     * Cooldown remaining for an ability in milliseconds, or {@code 0} if ready.
     *
     * @param abilityId the ability id
     * @return the remaining milliseconds
     */
    public long remainingCooldownMillis(@NotNull ContentId abilityId) {
        Long ready = this.readyAtMillis.get(requireNonNull(abilityId, "abilityId"));
        long now = this.clock.millis();
        return ready != null && now < ready ? ready - now : 0L;
    }

    @Override
    public void close() {
        interrupt();
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
