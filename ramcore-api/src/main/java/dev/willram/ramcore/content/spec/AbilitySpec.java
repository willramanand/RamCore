package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.ability.Ability;
import dev.willram.ramcore.ability.AbilityAction;
import dev.willram.ramcore.ability.AbilityTargeting;
import dev.willram.ramcore.ability.AbilityTargets;
import dev.willram.ramcore.ability.StatCost;
import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.presentation.PresentationEffect;
import dev.willram.ramcore.time.DurationParser;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * A pure description of an ability's declarative fields (cooldown, cast time, cost, targeting).
 * Config type {@code abilities}. The behaviour ({@link AbilityAction}) and visual
 * {@link PresentationEffect}s are supplied in code, since they are not expressible as data — combine
 * with {@link #toAbility(ContentId, AbilityAction, List)}.
 *
 * @param cooldown  the cooldown
 * @param castTicks the cast time in ticks
 * @param cost      the stat cost gate, or {@code null}
 * @param targeting the resolved targeting
 */
public record AbilitySpec(@NotNull Duration cooldown, long castTicks, @Nullable StatCost cost,
                          @NotNull AbilityTargeting targeting) {

    @NotNull
    public static AbilitySpec deserialize(@NotNull ConfigurationNode node) {
        Duration cooldown;
        try {
            cooldown = DurationParser.parse(node.node("cooldown").getString("0s"));
        } catch (IllegalArgumentException bad) {
            throw new ContentDeserializeException("invalid ability cooldown: " + bad.getMessage());
        }

        long castTicks = node.node("castTicks").getLong(0L);
        if (castTicks < 0L) {
            throw new ContentDeserializeException("ability castTicks must not be negative");
        }

        StatCost cost = null;
        ConfigurationNode costNode = node.node("cost");
        if (!costNode.virtual()) {
            String stat = costNode.node("stat").getString();
            if (stat == null || stat.isBlank()) {
                throw new ContentDeserializeException("ability cost is missing 'stat'");
            }
            double amount = costNode.node("amount").getDouble(0.0D);
            if (!Double.isFinite(amount) || amount < 0.0D) {
                throw new ContentDeserializeException("ability cost amount must be finite and non-negative");
            }
            cost = new StatCost(ContentId.parse(stat), amount);
        }

        AbilityTargeting targeting = targeting(node.node("targeting"));
        return new AbilitySpec(cooldown, castTicks, cost, targeting);
    }

    private static AbilityTargeting targeting(@NotNull ConfigurationNode node) {
        String type = node.node("type").getString("self").trim().toLowerCase();
        return switch (type) {
            case "self" -> AbilityTargets.self();
            case "none" -> AbilityTargets.none();
            case "radius" -> AbilityTargets.radius(requiredRadius(node, "radius"));
            case "nearest" -> AbilityTargets.nearest(requiredRadius(node, "radius"));
            case "looking" -> AbilityTargets.lookingAt(requiredRadius(node, "range"));
            default -> throw new ContentDeserializeException("unknown ability targeting type '" + type + "'");
        };
    }

    private static double requiredRadius(@NotNull ConfigurationNode node, @NotNull String field) {
        double value = node.node(field).getDouble(0.0D);
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new ContentDeserializeException("ability targeting needs a positive '" + field + "'");
        }
        return value;
    }

    /**
     * Builds an {@link Ability} from this spec's data plus a code-supplied action and effects.
     *
     * @param id      the ability id
     * @param action  the behaviour
     * @param effects the visual effects
     * @return the ability
     */
    @NotNull
    public Ability toAbility(@NotNull ContentId id, @NotNull AbilityAction action,
                            @NotNull List<PresentationEffect> effects) {
        return Ability.builder(id)
                .cooldown(this.cooldown)
                .castTicks(this.castTicks)
                .cost(this.cost)
                .targeting(this.targeting)
                .action(action)
                .effects(effects)
                .build();
    }

    /**
     * Builds an {@link Ability} from this spec with no action or effects (attach behaviour later).
     *
     * @param id the ability id
     * @return the ability
     */
    @NotNull
    public Ability toAbility(@NotNull ContentId id) {
        return toAbility(id, AbilityAction.NONE, List.of());
    }
}
