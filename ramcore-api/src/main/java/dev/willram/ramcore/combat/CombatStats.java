package dev.willram.ramcore.combat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The standard {@code ramcore:} combat stat ids read by {@link DamageCalculator}. Consumers register
 * matching {@code Stat}s (see {@code dev.willram.ramcore.stat}) and grant them through any stat
 * source. All values are read from a {@code StatSnapshot}; absent stats read as {@code 0}.
 */
public final class CombatStats {

    /** Attacker chance to crit, {@code [0, 1]}. */
    public static final ContentId CRIT_CHANCE = ContentId.of("ramcore", "crit_chance");

    /** Extra damage fraction on a crit; {@code 0.5} means {@code x1.5}. */
    public static final ContentId CRIT_DAMAGE = ContentId.of("ramcore", "crit_damage");

    /** Fraction of dealt damage returned to the attacker as health. */
    public static final ContentId LIFESTEAL = ContentId.of("ramcore", "lifesteal");

    /** Flat damage removed by the defender after crit and resistance. */
    public static final ContentId DEFENSE = ContentId.of("ramcore", "defense");

    private CombatStats() {
    }

    /**
     * The defender resistance stat id for an element, e.g. {@code fire -> ramcore:resist_fire}. The
     * resistance value is a fraction in {@code [0, 1)} of damage of that element ignored.
     *
     * @param element the element name (lower-cased; letters, digits, {@code _ . -})
     * @return the resistance stat id
     */
    @NotNull
    public static ContentId resistance(@NotNull String element) {
        String normalized = RamPreconditions.notBlank(element, "element").toLowerCase(Locale.ROOT);
        return ContentId.of("ramcore", "resist_" + normalized);
    }
}
