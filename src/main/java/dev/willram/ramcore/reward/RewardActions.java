package dev.willram.ramcore.reward;

import dev.willram.ramcore.economy.Economy;
import dev.willram.ramcore.economy.EconomyResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Concrete {@link RewardAction} factories.
 *
 * <p>Each resolves its player through {@link RewardSubjects}. Actions that touch world or player
 * state (command, message, item, permission check) must run on the appropriate scheduler context;
 * {@link #money} is safe wherever its {@link Economy} is. Stability: stable.</p>
 */
public final class RewardActions {

    private RewardActions() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Deposits money into the reward subject's account.
     *
     * @param economy the economy
     * @param amount  the amount (must be non-negative)
     * @return the action
     */
    @NotNull
    public static RewardAction money(@NotNull Economy economy, double amount) {
        requireNonNull(economy, "economy");
        return new RewardAction() {
            @NotNull
            @Override
            public RewardOutcome apply(@NotNull RewardContext context) {
                UUID id = RewardSubjects.playerId(context).orElse(null);
                if (id == null) {
                    return RewardOutcome.failed("money", "no player subject");
                }
                EconomyResult result = economy.deposit(id, amount);
                return result.success() ? RewardOutcome.success("money") : RewardOutcome.failed("money", result.message());
            }

            @NotNull
            @Override
            public List<String> validate(@NotNull RewardContext context) {
                if (amount < 0) {
                    return List.of("money amount must not be negative");
                }
                if (RewardSubjects.playerId(context).isEmpty()) {
                    return List.of("money reward needs a player subject");
                }
                return List.of();
            }
        };
    }

    /**
     * Runs a console command. {@code %player%} is replaced with the subject's name when resolvable.
     *
     * @param command the command line (without a leading slash)
     * @return the action
     */
    @NotNull
    public static RewardAction command(@NotNull String command) {
        requireNonNull(command, "command");
        return context -> {
            String resolved = command;
            Player player = RewardSubjects.onlinePlayer(context).orElse(null);
            if (player != null) {
                resolved = resolved.replace("%player%", player.getName());
            }
            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            return ok ? RewardOutcome.success("command") : RewardOutcome.failed("command", "command failed: " + resolved);
        };
    }

    /**
     * Sends a message to the subject if they are online.
     *
     * @param message the message
     * @return the action
     */
    @NotNull
    public static RewardAction message(@NotNull Component message) {
        requireNonNull(message, "message");
        return context -> RewardSubjects.onlinePlayer(context)
                .map(player -> {
                    player.sendMessage(message);
                    return RewardOutcome.success("message");
                })
                .orElseGet(() -> RewardOutcome.failed("message", "subject is not online"));
    }

    /**
     * Gives an item to the subject if they are online. Items that do not fit are dropped at the
     * player's location.
     *
     * @param item the item
     * @return the action
     */
    @NotNull
    public static RewardAction item(@NotNull ItemStack item) {
        requireNonNull(item, "item");
        return context -> RewardSubjects.onlinePlayer(context)
                .map(player -> {
                    player.getInventory().addItem(item.clone()).values()
                            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    return RewardOutcome.success("item");
                })
                .orElseGet(() -> RewardOutcome.failed("item", "subject is not online"));
    }

    /**
     * Succeeds only when the online subject holds the given permission node. Useful as a gate in a
     * guaranteed entry.
     *
     * @param node the permission node
     * @return the action
     */
    @NotNull
    public static RewardAction permissionCheck(@NotNull String node) {
        requireNonNull(node, "node");
        return context -> RewardSubjects.onlinePlayer(context)
                .map(player -> player.hasPermission(node)
                        ? RewardOutcome.success("permission-node-check")
                        : RewardOutcome.failed("permission-node-check", "missing permission: " + node))
                .orElseGet(() -> RewardOutcome.failed("permission-node-check", "subject is not online"));
    }
}
