package dev.willram.ramcore.nms.reflect;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Reflection lookup helper that returns absence instead of leaking ClassNotFoundException.
 */
public final class GuardedNmsLookup {
    private final ClassLoader classLoader;

    private GuardedNmsLookup(@NotNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @NotNull
    public static GuardedNmsLookup using(@NotNull ClassLoader classLoader) {
        return new GuardedNmsLookup(classLoader);
    }

    @NotNull
    public static GuardedNmsLookup current() {
        return using(Thread.currentThread().getContextClassLoader());
    }

    @NotNull
    public Optional<Class<?>> findClass(@NotNull String className) {
        try {
            return Optional.of(Class.forName(className, false, this.classLoader));
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }

    public boolean hasClass(@NotNull String className) {
        return findClass(className).isPresent();
    }
}
