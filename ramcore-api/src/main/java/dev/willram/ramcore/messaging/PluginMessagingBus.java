package dev.willram.ramcore.messaging;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * A {@link MessageBus} over Bukkit plugin messaging channels. Messages ride an online player's
 * connection, so at least one player must be online to send or receive; a headless proxy setup
 * should use {@code RedisMessageBus} instead.
 *
 * <p>The Bukkit channel is fixed; the RamCore {@code channel} argument is encoded into the payload,
 * so many logical channels share one Bukkit channel. Registered channel name is
 * {@code ramcore:bus}.</p>
 */
public final class PluginMessagingBus implements MessageBus, PluginMessageListener {
    private static final String BUKKIT_CHANNEL = "ramcore:bus";

    private final Plugin plugin;
    private final Map<String, List<MessageHandler>> handlers = new ConcurrentHashMap<>();
    private volatile boolean closed;

    private PluginMessagingBus(@NotNull Plugin plugin) {
        this.plugin = requireNonNull(plugin, "plugin");
    }

    /**
     * Registers the Bukkit channels and returns the bus.
     *
     * @param plugin the plugin
     * @return the bus
     */
    @NotNull
    public static PluginMessagingBus register(@NotNull Plugin plugin) {
        PluginMessagingBus bus = new PluginMessagingBus(plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUKKIT_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUKKIT_CHANNEL, bus);
        return bus;
    }

    @NotNull
    @Override
    public Promise<Void> publish(@NotNull String channel, byte @NotNull [] payload) {
        requireNonNull(channel, "channel");
        requireNonNull(payload, "payload");
        return Schedulers.runAsync(() -> {
            Player carrier = this.plugin.getServer().getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier == null) {
                return;
            }
            carrier.sendPluginMessage(this.plugin, BUKKIT_CHANNEL, frame(channel, payload));
        });
    }

    @NotNull
    @Override
    public Terminable subscribe(@NotNull String channel, @NotNull MessageHandler handler) {
        requireNonNull(channel, "channel");
        requireNonNull(handler, "handler");
        List<MessageHandler> list = this.handlers.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>());
        list.add(handler);
        return () -> list.remove(handler);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!BUKKIT_CHANNEL.equals(channel)) {
            return;
        }
        Framed framed = unframe(message);
        List<MessageHandler> subscribers = this.handlers.get(framed.channel());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        List<MessageHandler> snapshot = List.copyOf(subscribers);
        Schedulers.runAsync(() -> snapshot.forEach(handler -> handler.handle(framed.channel(), framed.payload())));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.plugin.getServer().getMessenger().unregisterIncomingPluginChannel(this.plugin, BUKKIT_CHANNEL, this);
        this.plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(this.plugin, BUKKIT_CHANNEL);
        this.handlers.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    private static byte[] frame(String channel, byte[] payload) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream data = new java.io.DataOutputStream(out);
        try {
            data.writeUTF(channel);
            data.writeInt(payload.length);
            data.write(payload);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to frame plugin message", e);
        }
        return out.toByteArray();
    }

    private static Framed unframe(byte[] message) {
        try {
            java.io.DataInputStream data = new java.io.DataInputStream(new java.io.ByteArrayInputStream(message));
            String channel = data.readUTF();
            int length = data.readInt();
            byte[] payload = new byte[Math.max(0, length)];
            data.readFully(payload);
            return new Framed(channel, payload);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to read plugin message", e);
        }
    }

    private record Framed(String channel, byte[] payload) {
    }
}
