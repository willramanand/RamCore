package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory loot instance store suitable for simple plugins and tests.
 */
public final class InMemoryLootInstanceStore implements LootInstanceStore {
    private final Map<UUID, LootInstance> instances = new ConcurrentHashMap<>();
    private final List<LootInstanceListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    @NotNull
    public LootInstance register(@NotNull LootInstance instance) {
        Objects.requireNonNull(instance, "instance");
        this.instances.put(instance.id(), instance);
        for (LootInstanceListener listener : this.listeners) {
            listener.generated(instance);
        }
        return instance;
    }

    @Override
    @NotNull
    public Optional<LootInstance> get(@NotNull UUID instanceId) {
        return Optional.ofNullable(this.instances.get(Objects.requireNonNull(instanceId, "instanceId")));
    }

    @Override
    @NotNull
    public Collection<LootInstance> instances() {
        return List.copyOf(this.instances.values());
    }

    @Override
    @NotNull
    public LootClaimResult claim(@NotNull UUID instanceId, @NotNull UUID claimantId, @NotNull Instant now) {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(claimantId, "claimantId");
        Objects.requireNonNull(now, "now");

        LootInstance instance = this.instances.get(instanceId);
        if (instance == null) {
            return LootClaimResult.notFound(instanceId, claimantId);
        }

        LootClaimResult result = instance.claim(claimantId, now);
        if (result.successful()) {
            for (LootInstanceListener listener : this.listeners) {
                listener.claimed(instance, result);
            }
        }
        return result;
    }

    @Override
    public boolean remove(@NotNull UUID instanceId) {
        return this.instances.remove(Objects.requireNonNull(instanceId, "instanceId")) != null;
    }

    @Override
    @NotNull
    public List<LootInstance> sweepExpired(@NotNull Instant now) {
        Objects.requireNonNull(now, "now");
        List<LootInstance> expired = new ArrayList<>();
        for (LootInstance instance : this.instances.values()) {
            if (instance.expired(now) && this.instances.remove(instance.id(), instance)) {
                expired.add(instance);
                for (LootInstanceListener listener : this.listeners) {
                    listener.expired(instance);
                }
            }
        }
        return List.copyOf(expired);
    }

    @Override
    public void reroll(@NotNull UUID instanceId, @NotNull List<LootReward> rewards, boolean clearClaims) {
        LootInstance instance = this.instances.get(Objects.requireNonNull(instanceId, "instanceId"));
        if (instance == null) {
            return;
        }

        List<LootReward> previous = instance.rewards();
        instance.reroll(rewards, clearClaims);
        for (LootInstanceListener listener : this.listeners) {
            listener.rerolled(instance, previous);
        }
    }

    @Override
    public void addListener(@NotNull LootInstanceListener listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }
}
