package dev.willram.ramcore.stat;

import dev.willram.ramcore.party.PartyGroup;
import dev.willram.ramcore.party.PartyManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StatSource} that contributes modifiers derived from the player's party. The mapping from
 * a {@link PartyGroup} to modifiers is supplied by the caller (e.g. read from the party's metadata,
 * or scaled by {@link PartyGroup#size()}), keeping this source independent of any party-stat schema.
 */
public final class PartyStatSource implements StatSource {

    private final PartyManager manager;
    private final Function<PartyGroup, Collection<StatModifier>> extractor;

    public PartyStatSource(@NotNull PartyManager manager,
                           @NotNull Function<PartyGroup, Collection<StatModifier>> extractor) {
        this.manager = requireNonNull(manager, "manager");
        this.extractor = requireNonNull(extractor, "extractor");
    }

    @Override
    @NotNull
    public Collection<StatModifier> modifiers(@NotNull Player player) {
        return this.manager.partyOf(player.getUniqueId())
                .map(this.extractor)
                .orElse(List.of());
    }
}
