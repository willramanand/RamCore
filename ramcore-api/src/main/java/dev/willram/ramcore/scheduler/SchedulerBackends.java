package dev.willram.ramcore.scheduler;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Holds the {@link SchedulerBackend} that {@link Schedulers} and {@code Promise} dispatch through.
 *
 * <p>The default backend is the Paper/Folia regionised scheduler. Tests install a deterministic
 * backend such as {@code FakeScheduler} from the RamCore test kit and reset it afterwards. This
 * class is internal: plugins must not install backends on a live server.</p>
 */
@ApiStatus.Internal
public final class SchedulerBackends {
    private static final SchedulerBackend DEFAULT = new PaperFoliaSchedulerBackend();
    private static volatile SchedulerBackend current = DEFAULT;

    private SchedulerBackends() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets the active backend.
     *
     * @return the backend every scheduler call routes through
     */
    @NotNull
    public static SchedulerBackend current() {
        return current;
    }

    /**
     * Replaces the active backend. Intended for tests only.
     *
     * @param backend the backend to install
     */
    public static void install(@NotNull SchedulerBackend backend) {
        current = Objects.requireNonNull(backend, "backend");
    }

    /**
     * Restores the default Paper/Folia backend.
     */
    public static void reset() {
        current = DEFAULT;
    }

    /**
     * Checks whether the default Paper/Folia backend is active.
     *
     * @return true when no test backend is installed
     */
    public static boolean isDefault() {
        return current == DEFAULT;
    }
}
