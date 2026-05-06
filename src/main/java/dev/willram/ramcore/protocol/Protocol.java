/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.willram.ramcore.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import dev.willram.ramcore.event.functional.protocol.ProtocolSubscriptionBuilder;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

/**
 * Utilities for working with ProtocolLib.
 */
public final class Protocol {

    /**
     * Makes a HandlerBuilder for the given packets
     *
     * @param packets the packets to handle
     * @return a {@link ProtocolSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    public static ProtocolSubscriptionBuilder subscribe(@NotNull PacketType... packets) {
        return ProtocolSubscriptionBuilder.newBuilder(packets);
    }

    /**
     * Makes a HandlerBuilder for the given packets
     *
     * @param priority   the priority to listen at
     * @param packets the packets to handle
     * @return a {@link ProtocolSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    public static ProtocolSubscriptionBuilder subscribe(@NotNull ListenerPriority priority, @NotNull PacketType... packets) {
        return ProtocolSubscriptionBuilder.newBuilder(priority, packets);
    }

    /**
     * Gets the protocol manager.
     *
     * @return the protocol manager.
     */
    @NotNull
    public static ProtocolManager manager() {
        return ProtocolLibrary.getProtocolManager();
    }

    /**
     * Sends a packet to the given player.
     *
     * @param player the player
     * @param packet the packet
     */
    public static void sendPacket(@NotNull Player player, @NotNull PacketContainer packet) {
        manager().sendServerPacket(player, packet);
    }

    /**
     * Sends a packet on the scheduler context owned by the given player.
     *
     * @param player the player
     * @param packet the packet
     */
    public static void sendPacketScheduled(@NotNull Player player, @NotNull PacketContainer packet) {
        Schedulers.run(player, () -> sendPacket(player, packet));
    }

    /**
     * Sends a packet to all players connected to the server.
     *
     * @param packet the packet
     */
    public static void broadcastPacket(@NotNull PacketContainer packet) {
        manager().broadcastServerPacket(packet);
    }

    /**
     * Sends a packet to each of the given players
     *
     * @param players the players
     * @param packet the packet
     */
    public static void broadcastPacket(@NotNull Iterable<Player> players, @NotNull PacketContainer packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    /**
     * Sends a packet to each player on that player's scheduler context.
     *
     * @param players the players
     * @param packet the packet
     */
    public static void broadcastPacketScheduled(@NotNull Iterable<Player> players, @NotNull PacketContainer packet) {
        for (Player player : players) {
            sendPacketScheduled(player, packet);
        }
    }

}
