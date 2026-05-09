package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Loot table with guaranteed entries and weighted rolls.
 */
public final class LootTable {
    private final ContentId id;
    private final List<LootEntry> guaranteed;
    private final List<LootEntry> weighted;
    private final List<LootPool> pools;
    private final int rolls;
    private final ToIntFunction<LootContext> bonusRolls;

    private LootTable(@NotNull ContentId id, @NotNull List<LootEntry> guaranteed, @NotNull List<LootEntry> weighted,
                      @NotNull List<LootPool> pools, int rolls, @NotNull ToIntFunction<LootContext> bonusRolls) {
        this.id = Objects.requireNonNull(id, "id");
        this.guaranteed = List.copyOf(guaranteed);
        this.weighted = List.copyOf(weighted);
        this.pools = List.copyOf(pools);
        this.rolls = rolls;
        this.bonusRolls = Objects.requireNonNull(bonusRolls, "bonusRolls");
        RamPreconditions.checkArgument(rolls >= 0, "loot table rolls must be >= 0", "Use 0 for guaranteed-only tables.");
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id) {
        return new Builder(id);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    @NotNull
    public List<LootEntry> guaranteed() {
        return this.guaranteed;
    }

    @NotNull
    public List<LootEntry> weighted() {
        return this.weighted;
    }

    @NotNull
    public List<LootPool> pools() {
        return this.pools;
    }

    public int rolls() {
        return this.rolls;
    }

    public int bonusRolls(@NotNull LootContext context) {
        return Math.max(0, this.bonusRolls.applyAsInt(context));
    }

    public static final class Builder {
        private final ContentId id;
        private final List<LootEntry> guaranteed = new ArrayList<>();
        private final List<LootEntry> weighted = new ArrayList<>();
        private final List<LootPool> pools = new ArrayList<>();
        private int rolls = 1;
        private ToIntFunction<LootContext> bonusRolls = context -> 0;

        private Builder(ContentId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        @NotNull
        public Builder guaranteed(@NotNull LootEntry entry) {
            this.guaranteed.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        @NotNull
        public Builder weighted(@NotNull LootEntry entry) {
            this.weighted.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        @NotNull
        public Builder pool(@NotNull LootPool pool) {
            this.pools.add(Objects.requireNonNull(pool, "pool"));
            return this;
        }

        @NotNull
        public Builder rolls(int rolls) {
            this.rolls = rolls;
            return this;
        }

        @NotNull
        public Builder bonusRolls(@NotNull ToIntFunction<LootContext> bonusRolls) {
            this.bonusRolls = Objects.requireNonNull(bonusRolls, "bonusRolls");
            return this;
        }

        @NotNull
        public LootTable build() {
            return new LootTable(this.id, this.guaranteed, this.weighted, this.pools, this.rolls, this.bonusRolls);
        }
    }
}
