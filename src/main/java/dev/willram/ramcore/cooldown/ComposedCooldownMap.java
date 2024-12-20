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

package dev.willram.ramcore.cooldown;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A self-populating, composed map of cooldown instances
 *
 * @param <I> input type
 * @param <O> internal type
 */
public interface ComposedCooldownMap<I, O> {

    /**
     * Creates a new collection with the cooldown properties defined by the base instance
     *
     * @param base the cooldown to base off
     * @return a new collection
     */
    @Nonnull
    static <I, O> ComposedCooldownMap<I, O> create(@Nonnull Cooldown base, @Nonnull Function<I, O> composeFunction) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(composeFunction, "composeFunction");
        return new ComposedCooldownMapImpl<>(base, composeFunction);
    }

    /**
     * Gets the base cooldown
     *
     * @return the base cooldown
     */
    @Nonnull
    Cooldown getBase();

    /**
     * Gets the internal cooldown instance associated with the given key.
     *
     * <p>The inline Cooldown methods in this class should be used to access the functionality of the cooldown as opposed
     * to calling the methods directly via the instance returned by this method.</p>
     *
     * @param key the key
     * @return a cooldown instance
     */
    @Nonnull
    Cooldown get(@Nonnull I key);

    void put(@Nonnull O key, @Nonnull Cooldown cooldown);

    /**
     * Gets the cooldowns contained within this collection.
     *
     * @return the backing map
     */
    @Nonnull
    Map<O, Cooldown> getAll();

    /* methods from Cooldown */

    default boolean test(@Nonnull I key) {
        return get(key).test();
    }

    default boolean testSilently(@Nonnull I key) {
        return get(key).testSilently();
    }

    default long elapsed(@Nonnull I key) {
        return get(key).elapsed();
    }

    default void reset(@Nonnull I key) {
        get(key).reset();
    }

    default long remainingMillis(@Nonnull I key) {
        return get(key).remainingMillis();
    }

    default long remainingTime(@Nonnull I key, @Nonnull TimeUnit unit) {
        return get(key).remainingTime(unit);
    }

    @Nonnull
    default OptionalLong getLastTested(@Nonnull I key) {
        return get(key).getLastTested();
    }

    default void setLastTested(@Nonnull I key, long time) {
        get(key).setLastTested(time);
    }

}
