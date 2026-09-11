package dev.willram.ramcore.input;

/**
 * How a {@link PlayerInput} request collects text from the player.
 */
public enum InputBackend {

    /** Reads the player's next chat message and cancels it so no one else sees it. Experimental. */
    CHAT,

    /** Opens an anvil and reads its rename field on result-slot click. Experimental. */
    ANVIL,

    /** Opens a sign edit screen and reads its lines. Paper-experimental; falls back to {@link #CHAT}. */
    SIGN
}
