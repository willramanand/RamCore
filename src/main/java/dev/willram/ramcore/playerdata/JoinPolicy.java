package dev.willram.ramcore.playerdata;

/**
 * What {@link PlayerDataService} does when a player joins before their data has finished loading.
 *
 * <p>There is deliberately no {@code BLOCK} option: the join runs on the player's region thread on
 * Folia and blocking it stalls every player in that region.</p>
 */
public enum JoinPolicy {

    /**
     * Wait up to {@link PlayerDataOptions#loadTimeout()} after the join; if the data is still not
     * loaded, kick the player with {@link PlayerDataOptions#kickMessage()}.
     */
    KICK,

    /**
     * Let the player in. Reads return {@link java.util.Optional#empty()} until the load completes;
     * use {@link PlayerDataService#whenReady(java.util.UUID)} to run code once it has.
     */
    DEFER
}
