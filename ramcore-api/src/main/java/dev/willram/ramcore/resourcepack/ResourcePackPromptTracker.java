package dev.willram.ramcore.resourcepack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Tracks resource-pack prompts, status events, and timeout transitions.
 */
public final class ResourcePackPromptTracker implements Listener {
    private final Map<RequestKey, ResourcePackRequest> requests = new LinkedHashMap<>();

    @NotNull
    public static ResourcePackPromptTracker create() {
        return new ResourcePackPromptTracker();
    }

    @NotNull
    public ResourcePackRequest send(@NotNull Player player, @NotNull ResourcePackPrompt prompt) {
        ResourcePackRequest request = track(player.getUniqueId(), prompt, System.currentTimeMillis());
        prompt.send(player);
        return request;
    }

    @NotNull
    public ResourcePackRequest track(@NotNull UUID playerId, @NotNull ResourcePackPrompt prompt) {
        return track(playerId, prompt, System.currentTimeMillis());
    }

    @NotNull
    public ResourcePackRequest track(@NotNull UUID playerId, @NotNull ResourcePackPrompt prompt, long nowMillis) {
        requireNonNull(playerId, "playerId");
        requireNonNull(prompt, "prompt");
        long timeoutAt = prompt.timeoutTicks() <= 0L ? 0L : nowMillis + (prompt.timeoutTicks() * 50L);
        ResourcePackRequest request = new ResourcePackRequest(
                playerId,
                prompt.id(),
                prompt,
                ResourcePackPromptStatus.REQUESTED,
                nowMillis,
                nowMillis,
                timeoutAt
        );
        this.requests.put(new RequestKey(playerId, prompt.id()), request);
        return request;
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        handle(event.getPlayer().getUniqueId(), event.getID(), event.getStatus(), System.currentTimeMillis());
    }

    @NotNull
    public Optional<ResourcePackRequest> handle(
            @NotNull UUID playerId,
            @NotNull UUID packId,
            @NotNull PlayerResourcePackStatusEvent.Status status,
            long nowMillis
    ) {
        return handle(playerId, packId, map(status), nowMillis);
    }

    @NotNull
    public Optional<ResourcePackRequest> handle(
            @NotNull UUID playerId,
            @NotNull UUID packId,
            @NotNull ResourcePackPromptStatus status,
            long nowMillis
    ) {
        RequestKey key = new RequestKey(playerId, packId);
        ResourcePackRequest request = this.requests.get(key);
        if (request == null || request.terminal()) {
            return Optional.empty();
        }

        ResourcePackRequest updated = request.status(status, nowMillis);
        this.requests.put(key, updated);
        return Optional.of(updated);
    }

    @NotNull
    public List<ResourcePackRequest> sweepTimeouts(long nowMillis) {
        List<ResourcePackRequest> timedOut = this.requests.values().stream()
                .filter(request -> request.timedOut(nowMillis))
                .map(request -> request.status(ResourcePackPromptStatus.TIMED_OUT, nowMillis))
                .toList();
        timedOut.forEach(request -> this.requests.put(new RequestKey(request.playerId(), request.packId()), request));
        return timedOut;
    }

    @NotNull
    public Optional<ResourcePackRequest> get(@NotNull UUID playerId, @NotNull UUID packId) {
        return Optional.ofNullable(this.requests.get(new RequestKey(playerId, packId)));
    }

    @NotNull
    public Optional<ResourcePackRequest> latest(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        ResourcePackRequest latest = null;
        for (ResourcePackRequest request : this.requests.values()) {
            if (request.playerId().equals(playerId) && (latest == null || request.updatedAtMillis() >= latest.updatedAtMillis())) {
                latest = request;
            }
        }
        return Optional.ofNullable(latest);
    }

    @NotNull
    public List<ResourcePackRequest> pending() {
        return this.requests.values().stream()
                .filter(request -> !request.terminal())
                .toList();
    }

    @NotNull
    public Collection<ResourcePackRequest> requests() {
        return List.copyOf(this.requests.values());
    }

    public void removePlayer(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        this.requests.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    public void clear() {
        this.requests.clear();
    }

    @NotNull
    public static ResourcePackPromptStatus map(@NotNull PlayerResourcePackStatusEvent.Status status) {
        return switch (requireNonNull(status, "status")) {
            case ACCEPTED -> ResourcePackPromptStatus.ACCEPTED;
            case DOWNLOADED -> ResourcePackPromptStatus.DOWNLOADED;
            case SUCCESSFULLY_LOADED -> ResourcePackPromptStatus.LOADED;
            case DECLINED -> ResourcePackPromptStatus.DECLINED;
            case FAILED_DOWNLOAD -> ResourcePackPromptStatus.FAILED_DOWNLOAD;
            case INVALID_URL -> ResourcePackPromptStatus.INVALID_URL;
            case FAILED_RELOAD -> ResourcePackPromptStatus.FAILED_RELOAD;
            case DISCARDED -> ResourcePackPromptStatus.DISCARDED;
        };
    }

    private record RequestKey(@NotNull UUID playerId, @NotNull UUID packId) {
        private RequestKey {
            requireNonNull(playerId, "playerId");
            requireNonNull(packId, "packId");
        }
    }
}
