package dev.willram.ramcore.party;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Tracks numeric contribution, such as boss damage, by member id.
 */
public final class PartyContributionTracker {
    private final Map<UUID, Double> contributions = new LinkedHashMap<>();

    public void add(@NotNull UUID playerId, double amount) {
        requireNonNull(playerId, "playerId");
        RamPreconditions.checkArgument(amount >= 0.0d, "contribution amount must be non-negative", "Use zero or a positive amount.");
        this.contributions.merge(playerId, amount, Double::sum);
    }

    public void set(@NotNull UUID playerId, double amount) {
        requireNonNull(playerId, "playerId");
        RamPreconditions.checkArgument(amount >= 0.0d, "contribution amount must be non-negative", "Use zero or a positive amount.");
        this.contributions.put(playerId, amount);
    }

    public double get(@NotNull UUID playerId) {
        return this.contributions.getOrDefault(requireNonNull(playerId, "playerId"), 0.0d);
    }

    public double total() {
        return this.contributions.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @NotNull
    public Set<UUID> eligible(double minimum) {
        RamPreconditions.checkArgument(minimum >= 0.0d, "minimum contribution must be non-negative", "Use zero or a positive amount.");
        return this.contributions.entrySet().stream()
                .filter(entry -> entry.getValue() >= minimum)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    @NotNull
    public Map<UUID, Double> top(int limit) {
        RamPreconditions.checkArgument(limit >= 0, "limit must be non-negative", "Use zero or a positive amount.");
        return this.contributions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, ignored) -> left, LinkedHashMap::new));
    }

    @NotNull
    public Map<UUID, Double> snapshot() {
        return Map.copyOf(this.contributions);
    }

    public void clear() {
        this.contributions.clear();
    }
}
