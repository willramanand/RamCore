package dev.willram.ramcore.testkit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builds {@link Proxy}-backed fakes of Bukkit interfaces without a server.
 *
 * <p>Every fake answers {@code equals}, {@code hashCode}, and {@code toString} sensibly and returns a
 * neutral default (false, 0, null, empty collection) for anything not stubbed. Use
 * {@link #recording(Class)} to capture call names for assertions.</p>
 */
public final class ProxyFakes {
    private ProxyFakes() {
    }

    /** A fake where every method returns its neutral default. */
    @NotNull
    public static <T> T stub(@NotNull Class<T> type) {
        return proxy(type, Map.of());
    }

    /**
     * A fake answering the given method names with fixed values.
     *
     * @param type    interface to fake
     * @param answers method name to return value (or {@link Function} of the argument array)
     */
    @NotNull
    public static <T> T proxy(@NotNull Class<T> type, @NotNull Map<String, ?> answers) {
        return proxy(type, new Handler(answers, null));
    }

    /** A fake that records every call name into {@link Recording#calls()}. */
    @NotNull
    public static <T> Recording<T> recording(@NotNull Class<T> type) {
        return recording(type, Map.of());
    }

    @NotNull
    public static <T> Recording<T> recording(@NotNull Class<T> type, @NotNull Map<String, ?> answers) {
        List<String> calls = Collections.synchronizedList(new ArrayList<>());
        T fake = proxy(type, new Handler(answers, calls));
        return new Recording<>(fake, calls);
    }

    /** Raw escape hatch: a proxy over a custom handler. */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> T proxy(@NotNull Class<T> type, @NotNull InvocationHandler handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    /** The neutral default for a return type: false, zero, empty collection, or null. */
    @Nullable
    public static Object defaultValue(@NotNull Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == List.class || type == java.util.Collection.class) {
            return List.of();
        }
        if (type == java.util.Set.class) {
            return java.util.Set.of();
        }
        if (type == Map.class) {
            return Map.of();
        }
        if (type == java.util.Optional.class) {
            return java.util.Optional.empty();
        }
        return null;
    }

    /** A fake plus the names of the methods invoked on it. */
    public record Recording<T>(@NotNull T fake, @NotNull List<String> calls) {
        public boolean called(@NotNull String method) {
            return this.calls.contains(method);
        }
    }

    private static final class Handler implements InvocationHandler {
        private final Map<String, Object> answers;
        private final @Nullable List<String> calls;

        private Handler(Map<String, ?> answers, @Nullable List<String> calls) {
            this.answers = new HashMap<>(answers);
            this.calls = calls;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return switch (name) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Fake<" + proxy.getClass().getInterfaces()[0].getSimpleName() + ">";
                    default -> null;
                };
            }
            if (this.calls != null) {
                this.calls.add(name);
            }
            if (this.answers.containsKey(name)) {
                Object answer = this.answers.get(name);
                if (answer instanceof Function<?, ?> function) {
                    @SuppressWarnings("unchecked")
                    Function<Object[], Object> typed = (Function<Object[], Object>) function;
                    return typed.apply(args == null ? new Object[0] : args);
                }
                return answer;
            }
            return defaultValue(method.getReturnType());
        }
    }
}
