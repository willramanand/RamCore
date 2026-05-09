package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Facade for loot tables, generation, and claimable instances.
 */
public final class InstancedLoot {

    @NotNull
    public static LootGenerator generator() {
        return new LootGenerator();
    }

    @NotNull
    public static LootInstanceStore inMemoryStore() {
        return new InMemoryLootInstanceStore();
    }

    @NotNull
    public static LootTable.Builder table(@NotNull String id) {
        return table(ContentId.parse(id));
    }

    @NotNull
    public static LootTable.Builder table(@NotNull ContentId id) {
        return LootTable.builder(id);
    }

    @NotNull
    public static LootPool.Builder pool(@NotNull String id) {
        return LootPool.builder(id);
    }

    @NotNull
    public static LootPoolEntry.Builder entry(@NotNull String id, @NotNull LootReward reward) {
        return LootPoolEntry.builder(id, reward);
    }

    @NotNull
    public static LootReward reward(@NotNull String id) {
        return LootReward.of(id);
    }

    @NotNull
    public static LootReward reward(@NotNull String id, @NotNull Object payload) {
        return LootReward.of(id, payload);
    }

    @NotNull
    public static LootInstance createInstance(@NotNull LootTable table, @NotNull LootContext context, @NotNull Random random,
                                              @NotNull LootInstanceScope scope, @NotNull LootClaimPolicy claimPolicy) {
        LootGenerationResult result = generator().generate(table, context, random);
        if (!result.successful()) {
            throw new IllegalStateException("loot generation failed: " + result.errors());
        }
        return instance(table.id(), result.rewards())
                .scope(scope)
                .claimPolicy(claimPolicy)
                .build();
    }

    @NotNull
    public static LootInstance.Builder instance(@NotNull ContentId tableId, @NotNull List<LootReward> rewards) {
        return LootInstance.builder(tableId, rewards);
    }

    @NotNull
    public static Instant expiresAfter(@NotNull Instant now, @NotNull Duration duration) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(duration, "duration");
        return now.plus(duration);
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.LOOT_TABLES,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "ramcore-loot-trade-builders",
                null,
                null,
                "RamCore provides side-effect-free loot pools and Paper MerchantRecipe trade builders; direct mutation of vanilla datapack loot tables and hidden trade restock internals require guarded NMS adapters."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.RAMCORE_LOOT_GENERATION,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-loot",
                        "RamCore loot tables, pools, conditions, functions, and generated rewards are side-effect-free and version independent."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.LOOT_INSTANCES,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-loot",
                        "RamCore claimable loot instances support scopes, expiry, duplicate-claim policies, and rerolls."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.VANILLA_LOOT_TABLE_MUTATION,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Direct vanilla datapack loot table mutation requires a guarded versioned adapter."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.TRADE_RECIPES,
                        NmsAccessTier.PAPER_API,
                        "paper-merchant-recipes",
                        "Paper exposes MerchantRecipe builders and application for villager and wandering trader offers."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.TRADE_RESTOCK_INTERNALS,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Hidden villager restock, demand, and gossip internals require a versioned adapter."
                ));
        return registry;
    }

    private InstancedLoot() {
    }
}
