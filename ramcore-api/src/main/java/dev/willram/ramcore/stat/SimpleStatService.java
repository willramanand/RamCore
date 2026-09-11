package dev.willram.ramcore.stat;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.service.ServiceContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

final class SimpleStatService implements StatService {
    private final StatRegistry registry;
    private final RamPlugin plugin;
    private final StatInvalidationListener listener;
    private final List<StatSource> sources = new CopyOnWriteArrayList<>();
    private final Map<UUID, StatSnapshot> cache = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile boolean closed;

    SimpleStatService(@NotNull StatRegistry registry, @Nullable RamPlugin plugin) {
        this.registry = requireNonNull(registry, "registry");
        this.plugin = plugin;
        this.listener = new StatInvalidationListener(this);
    }

    @Override
    @NotNull
    public StatRegistry registry() {
        return this.registry;
    }

    @Override
    public void addSource(@NotNull StatSource source) {
        RamPreconditions.checkState(!this.closed, "stat service is closed",
                "Add sources from load()/enable(), not after disable.");
        this.sources.add(requireNonNull(source, "source"));
    }

    @Override
    @NotNull
    public StatSnapshot snapshot(@NotNull Player player) {
        requireNonNull(player, "player");
        if (this.closed) {
            return StatSnapshot.empty();
        }
        return this.cache.computeIfAbsent(player.getUniqueId(), k -> recompute(player));
    }

    private StatSnapshot recompute(@NotNull Player player) {
        List<StatModifier> all = new ArrayList<>();
        for (StatSource source : this.sources) {
            all.addAll(source.modifiers(player));
        }
        return StatSnapshot.compute(this.registry, all);
    }

    @Override
    public void invalidate(@NotNull UUID playerId) {
        this.cache.remove(requireNonNull(playerId, "playerId"));
    }

    @Override
    public void enable(@NotNull ServiceContext context) {
        RamPreconditions.checkState(!this.closed, "stat service is closed",
                "Create a new service; a closed one cannot be enabled again.");
        if (this.enabled) {
            return;
        }
        this.enabled = true;
        if (this.plugin != null) {
            this.plugin.registerListener(this.listener);
        }
        context.bind(this);
    }

    @Override
    public void disable(@NotNull ServiceContext context) {
        close();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.cache.clear();
        this.sources.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
