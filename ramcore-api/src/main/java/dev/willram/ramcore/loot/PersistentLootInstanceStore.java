package dev.willram.ramcore.loot;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link LootInstanceStore} that keeps instances in memory and writes every change through to a
 * {@link Store}. Same interface and listener semantics as {@link InMemoryLootInstanceStore};
 * call {@link #load()} once at startup to restore unclaimed instances.
 *
 * <p>Stability: experimental (payload persistence depends on the consumer's
 * {@link LootPayloadCodec}). Folia-safe by design.</p>
 */
public final class PersistentLootInstanceStore implements LootInstanceStore {
    private final InMemoryLootInstanceStore memory = new InMemoryLootInstanceStore();
    private final Store<UUID, LootInstanceSnapshot> store;
    private final LootPayloadCodec payloads;

    public PersistentLootInstanceStore(@NotNull Store<UUID, LootInstanceSnapshot> store, @NotNull LootPayloadCodec payloads) {
        this.store = Objects.requireNonNull(store, "store");
        this.payloads = Objects.requireNonNull(payloads, "payloads");
    }

    @NotNull
    public Store<UUID, LootInstanceSnapshot> store() {
        return this.store;
    }

    /**
     * Restores persisted instances without firing {@code generated} listeners. Instances that have
     * already expired are deleted instead.
     *
     * @return number of instances restored
     */
    @NotNull
    public Promise<Integer> load() {
        return Promise.wrapFuture(this.store.loadAll().toCompletableFuture().thenApply(this::restore));
    }

    private int restore(Map<UUID, LootInstanceSnapshot> snapshots) {
        Instant now = Instant.now();
        int restored = 0;
        for (LootInstanceSnapshot snapshot : snapshots.values()) {
            LootInstance instance = snapshot.toInstance(this.payloads);
            if (instance.expired(now)) {
                forget(instance.id());
                continue;
            }
            this.memory.restore(instance);
            restored++;
        }
        return restored;
    }

    @NotNull
    @Override
    public LootInstance register(@NotNull LootInstance instance) {
        LootInstance registered = this.memory.register(instance);
        persist(registered);
        return registered;
    }

    @NotNull
    @Override
    public Optional<LootInstance> get(@NotNull UUID instanceId) {
        return this.memory.get(instanceId);
    }

    @NotNull
    @Override
    public Collection<LootInstance> instances() {
        return this.memory.instances();
    }

    @NotNull
    @Override
    public LootClaimResult claim(@NotNull UUID instanceId, @NotNull UUID claimantId, @NotNull Instant now) {
        LootClaimResult result = this.memory.claim(instanceId, claimantId, now);
        if (result.successful()) {
            this.memory.get(instanceId).ifPresent(this::persist);
        }
        return result;
    }

    @Override
    public boolean remove(@NotNull UUID instanceId) {
        boolean removed = this.memory.remove(instanceId);
        forget(instanceId);
        return removed;
    }

    @NotNull
    @Override
    public List<LootInstance> sweepExpired(@NotNull Instant now) {
        List<LootInstance> expired = this.memory.sweepExpired(now);
        expired.forEach(instance -> forget(instance.id()));
        return expired;
    }

    @Override
    public void reroll(@NotNull UUID instanceId, @NotNull List<LootReward> rewards, boolean clearClaims) {
        this.memory.reroll(instanceId, rewards, clearClaims);
        this.memory.get(instanceId).ifPresent(this::persist);
    }

    @Override
    public void addListener(@NotNull LootInstanceListener listener) {
        this.memory.addListener(listener);
    }

    private void persist(LootInstance instance) {
        this.store.save(instance.id(), LootInstanceSnapshot.of(instance, this.payloads)).exceptionallyAsync(error -> {
            RamLog.warn("failed to persist loot instance " + instance.id(), error);
            return null;
        });
    }

    private void forget(UUID instanceId) {
        this.store.delete(instanceId).exceptionallyAsync(error -> {
            RamLog.warn("failed to delete persisted loot instance " + instanceId, error);
            return null;
        });
    }
}
