package dev.willram.ramcore.resourcepack;

/**
 * Normalized status for a tracked resource-pack prompt.
 */
public enum ResourcePackPromptStatus {
    REQUESTED(false),
    ACCEPTED(false),
    DOWNLOADED(false),
    LOADED(true),
    DECLINED(true),
    FAILED_DOWNLOAD(true),
    INVALID_URL(true),
    FAILED_RELOAD(true),
    DISCARDED(true),
    TIMED_OUT(true);

    private final boolean terminal;

    ResourcePackPromptStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return this.terminal;
    }
}
