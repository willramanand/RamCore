package dev.willram.ramcore.selector;

import dev.willram.ramcore.exception.RamPreconditions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Reusable entity selection pipeline independent of command parsing.
 *
 * @param <T> selected entity type
 */
public final class EntitySelector<T extends Entity> {
    private final Class<T> type;
    private final List<Predicate<? super T>> filters = new ArrayList<>();
    private Location origin;
    private double maxDistanceSquared = -1.0d;
    private SelectorSort sort = SelectorSort.NONE;
    private int limit = -1;

    EntitySelector(@NotNull Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    public EntitySelector<T> filter(@NotNull Predicate<? super T> filter) {
        this.filters.add(Objects.requireNonNull(filter, "filter"));
        return this;
    }

    @NotNull
    public EntitySelector<T> uuid(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return filter(entity -> uuid.equals(entity.getUniqueId()));
    }

    @NotNull
    public EntitySelector<T> name(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        return filter(entity -> name.equalsIgnoreCase(entity.getName()));
    }

    @NotNull
    public EntitySelector<T> world(@NotNull World world) {
        Objects.requireNonNull(world, "world");
        return filter(entity -> world.equals(entity.getWorld()));
    }

    @NotNull
    public EntitySelector<T> within(@NotNull Location origin, double radius) {
        RamPreconditions.checkArgument(radius >= 0.0d, "selector radius must be >= 0", "Pass a non-negative radius.");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.maxDistanceSquared = radius * radius;
        return this;
    }

    @NotNull
    public EntitySelector<T> nearest(@NotNull Location origin) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sort = SelectorSort.NEAREST;
        return this;
    }

    @NotNull
    public EntitySelector<T> farthest(@NotNull Location origin) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sort = SelectorSort.FARTHEST;
        return this;
    }

    @NotNull
    public EntitySelector<T> random() {
        this.sort = SelectorSort.RANDOM;
        return this;
    }

    @NotNull
    public EntitySelector<T> sort(@NotNull SelectorSort sort) {
        this.sort = Objects.requireNonNull(sort, "sort");
        return this;
    }

    @NotNull
    public SelectorSort sort() {
        return this.sort;
    }

    @NotNull
    public EntitySelector<T> limit(int limit) {
        RamPreconditions.checkArgument(limit >= 0, "selector limit must be >= 0", "Pass 0 for no results or a positive maximum.");
        this.limit = limit;
        return this;
    }

    public int limit() {
        return this.limit;
    }

    @NotNull
    public List<T> select(@NotNull Collection<? extends Entity> entities) {
        Objects.requireNonNull(entities, "entities");
        List<T> selected = new ArrayList<>();
        for (Entity entity : entities) {
            if (!this.type.isInstance(entity)) {
                continue;
            }

            T typed = this.type.cast(entity);
            if (this.maxDistanceSquared >= 0.0d && distanceSquared(typed) > this.maxDistanceSquared) {
                continue;
            }

            if (matches(typed)) {
                selected.add(typed);
            }
        }

        applySort(selected);
        if (this.limit >= 0 && selected.size() > this.limit) {
            return List.copyOf(selected.subList(0, this.limit));
        }
        return List.copyOf(selected);
    }

    @NotNull
    public Optional<T> first(@NotNull Collection<? extends Entity> entities) {
        List<T> selected = select(entities);
        return selected.isEmpty() ? Optional.empty() : Optional.of(selected.get(0));
    }

    @NotNull
    public T single(@NotNull Collection<? extends Entity> entities) {
        List<T> selected = select(entities);
        if (selected.size() != 1) {
            throw RamPreconditions.misuse(
                    "selector expected exactly one entity but matched " + selected.size(),
                    "Use first(...), limit(1), or adjust selector filters before calling single(...)."
            );
        }
        return selected.get(0);
    }

    private boolean matches(@NotNull T entity) {
        for (Predicate<? super T> filter : this.filters) {
            if (!filter.test(entity)) {
                return false;
            }
        }
        return true;
    }

    private void applySort(@NotNull List<T> selected) {
        if (this.sort == SelectorSort.RANDOM) {
            Collections.shuffle(selected);
            return;
        }
        if (this.sort == SelectorSort.NEAREST || this.sort == SelectorSort.FARTHEST) {
            RamPreconditions.checkState(
                    this.origin != null,
                    "distance selector sort requires an origin",
                    "Call nearest(origin), farthest(origin), or within(origin, radius) before sorting by distance."
            );
            Comparator<T> comparator = Comparator.comparingDouble(this::distanceSquared);
            if (this.sort == SelectorSort.FARTHEST) {
                comparator = comparator.reversed();
            }
            selected.sort(comparator);
        }
    }

    private double distanceSquared(@NotNull Entity entity) {
        if (this.origin == null) {
            return 0.0d;
        }

        Location location = entity.getLocation();
        if (!Objects.equals(this.origin.getWorld(), location.getWorld())) {
            return Double.MAX_VALUE;
        }

        double dx = this.origin.getX() - location.getX();
        double dy = this.origin.getY() - location.getY();
        double dz = this.origin.getZ() - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
