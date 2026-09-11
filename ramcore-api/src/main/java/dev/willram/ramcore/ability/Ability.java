package dev.willram.ramcore.ability;

import dev.willram.ramcore.ability.telegraph.Telegraph;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.presentation.PresentationEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A castable ability definition: an identity, a cooldown, an optional {@link StatCost} gate, a cast
 * time in ticks, how it picks targets, the visual {@link PresentationEffect}s played on success, and
 * the {@link AbilityAction} run on success. Immutable; build with {@link #builder(ContentId)}.
 */
public final class Ability {
    private final ContentId id;
    private final Duration cooldown;
    private final StatCost cost;
    private final long castTicks;
    private final AbilityTargeting targeting;
    private final List<PresentationEffect> effects;
    private final AbilityAction action;
    private final AbilityChannel channel;
    private final Telegraph telegraph;

    private Ability(Builder builder) {
        this.id = requireNonNull(builder.id, "id");
        this.cooldown = builder.cooldown;
        this.cost = builder.cost;
        this.castTicks = builder.castTicks;
        this.targeting = builder.targeting;
        this.effects = List.copyOf(builder.effects);
        this.action = builder.action;
        this.channel = builder.channel;
        this.telegraph = builder.telegraph;
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id) {
        return new Builder(id);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    /** The cooldown between casts; {@link Duration#ZERO} for none. */
    @NotNull
    public Duration cooldown() {
        return this.cooldown;
    }

    /** The stat gate, if any. */
    @NotNull
    public Optional<StatCost> cost() {
        return Optional.ofNullable(this.cost);
    }

    /** The cast time in ticks; {@code 0} for instant. */
    public long castTicks() {
        return this.castTicks;
    }

    @NotNull
    public AbilityTargeting targeting() {
        return this.targeting;
    }

    @NotNull
    public List<PresentationEffect> effects() {
        return this.effects;
    }

    @NotNull
    public AbilityAction action() {
        return this.action;
    }

    /** The repeating channel, if this ability channels. */
    @NotNull
    public Optional<AbilityChannel> channel() {
        return Optional.ofNullable(this.channel);
    }

    /** The aim preview shown while the ability casts/channels, if any. */
    @NotNull
    public Optional<Telegraph> telegraph() {
        return Optional.ofNullable(this.telegraph);
    }

    public static final class Builder {
        private final ContentId id;
        private Duration cooldown = Duration.ZERO;
        private StatCost cost;
        private long castTicks;
        private AbilityTargeting targeting = AbilityTargets.self();
        private List<PresentationEffect> effects = List.of();
        private AbilityAction action = AbilityAction.NONE;
        private AbilityChannel channel;
        private Telegraph telegraph;

        private Builder(@NotNull ContentId id) {
            this.id = requireNonNull(id, "id");
        }

        @NotNull
        public Builder cooldown(@NotNull Duration cooldown) {
            requireNonNull(cooldown, "cooldown");
            RamPreconditions.checkArgument(!cooldown.isNegative(),
                    "ability cooldown must not be negative", "pass a non-negative cooldown");
            this.cooldown = cooldown;
            return this;
        }

        @NotNull
        public Builder cost(@Nullable StatCost cost) {
            this.cost = cost;
            return this;
        }

        @NotNull
        public Builder cost(@NotNull ContentId statId, double amount) {
            return cost(new StatCost(statId, amount));
        }

        @NotNull
        public Builder castTicks(long castTicks) {
            RamPreconditions.checkArgument(castTicks >= 0L,
                    "ability castTicks must not be negative", "pass castTicks >= 0");
            this.castTicks = castTicks;
            return this;
        }

        @NotNull
        public Builder targeting(@NotNull AbilityTargeting targeting) {
            this.targeting = requireNonNull(targeting, "targeting");
            return this;
        }

        @NotNull
        public Builder effects(@NotNull List<PresentationEffect> effects) {
            this.effects = List.copyOf(effects);
            return this;
        }

        @NotNull
        public Builder action(@NotNull AbilityAction action) {
            this.action = requireNonNull(action, "action");
            return this;
        }

        /**
         * Makes this ability channel: {@code onTick} fires on the caster's scheduler every interval
         * until the total elapses, then the action runs. Replaces the plain cast timer.
         *
         * @param channel the channel
         * @return this builder
         */
        @NotNull
        public Builder channel(@NotNull AbilityChannel channel) {
            this.channel = requireNonNull(channel, "channel");
            return this;
        }

        /**
         * Shows a server-side aim preview ({@link Telegraph}) while the ability casts or channels,
         * cleared automatically when the cast completes or is interrupted. Has no effect on an instant
         * ability (no cast window to preview).
         *
         * @param telegraph the aim preview
         * @return this builder
         */
        @NotNull
        public Builder telegraph(@NotNull Telegraph telegraph) {
            this.telegraph = requireNonNull(telegraph, "telegraph");
            return this;
        }

        @NotNull
        public Ability build() {
            return new Ability(this);
        }
    }
}
