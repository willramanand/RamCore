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

package dev.willram.ramcore.event;

import com.google.common.reflect.TypeToken;
import dev.willram.ramcore.event.functional.merged.MergedSubscriptionBuilder;
import dev.willram.ramcore.event.functional.single.SingleSubscriptionBuilder;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.TerminableConsumer;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A functional event listening utility.
 */
public final class Events {

    /**
     * Makes a SingleSubscriptionBuilder for a given event
     *
     * @param eventClass the class of the event
     * @param <T>        the event type
     * @return a {@link SingleSubscriptionBuilder} to construct the event handler
     * @throws NullPointerException if eventClass is null
     */
    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> subscribe(@NotNull Class<T> eventClass) {
        return SingleSubscriptionBuilder.newBuilder(eventClass);
    }

    /**
     * Makes a SingleSubscriptionBuilder for a given event
     *
     * @param eventClass the class of the event
     * @param priority   the priority to listen at
     * @param <T>        the event type
     * @return a {@link SingleSubscriptionBuilder} to construct the event handler
     * @throws NullPointerException if eventClass or priority is null
     */
    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> subscribe(@NotNull Class<T> eventClass, @NotNull EventPriority priority) {
        return SingleSubscriptionBuilder.newBuilder(eventClass, priority);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> lowest(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.LOWEST);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> low(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.LOW);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> normal(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.NORMAL);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> high(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.HIGH);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> highest(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.HIGHEST);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> monitor(@NotNull Class<T> eventClass) {
        return subscribe(eventClass, EventPriority.MONITOR);
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> once(@NotNull Class<T> eventClass) {
        return subscribe(eventClass).once();
    }

    @NotNull
    public static <T extends Event> SingleSubscriptionBuilder<T> filtered(@NotNull Class<T> eventClass, @NotNull Predicate<T> predicate) {
        return subscribe(eventClass).filter(predicate);
    }

    @NotNull
    public static <T extends Event> SingleSubscription<T> listen(@NotNull Class<T> eventClass,
                                                                 @NotNull Consumer<? super T> handler,
                                                                 @NotNull TerminableConsumer owner) {
        Objects.requireNonNull(owner, "owner");
        return owner.bind(subscribe(eventClass).handler(handler));
    }

    @NotNull
    public static <T extends Event> SingleSubscription<T> listen(@NotNull Class<T> eventClass,
                                                                 @NotNull EventPriority priority,
                                                                 @NotNull Predicate<T> filter,
                                                                 @NotNull Consumer<? super T> handler,
                                                                 @NotNull TerminableConsumer owner) {
        Objects.requireNonNull(owner, "owner");
        return owner.bind(subscribe(eventClass, priority).filter(filter).handler(handler));
    }

    @NotNull
    public static <T extends Event> SingleSubscription<T> listenOnce(@NotNull Class<T> eventClass,
                                                                     @NotNull Consumer<? super T> handler,
                                                                     @NotNull TerminableConsumer owner) {
        Objects.requireNonNull(owner, "owner");
        return owner.bind(once(eventClass).handler(handler));
    }

    /**
     * Makes a MergedSubscriptionBuilder for a given super type
     *
     * @param handledClass the super type of the event handler
     * @param <T>          the super type class
     * @return a {@link MergedSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    public static <T> MergedSubscriptionBuilder<T> merge(@NotNull Class<T> handledClass) {
        return MergedSubscriptionBuilder.newBuilder(handledClass);
    }

    @NotNull
    public static EventSubscriptionGroup group() {
        return EventSubscriptionGroup.create();
    }

    /**
     * Makes a MergedSubscriptionBuilder for a given super type
     *
     * @param type the super type of the event handler
     * @param <T>  the super type class
     * @return a {@link MergedSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    public static <T> MergedSubscriptionBuilder<T> merge(@NotNull TypeToken<T> type) {
        return MergedSubscriptionBuilder.newBuilder(type);
    }

    /**
     * Makes a MergedSubscriptionBuilder for a super event class
     *
     * @param superClass   the abstract super event class
     * @param eventClasses the event classes to be bound to
     * @param <S>          the super class type
     * @return a {@link MergedSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    @SafeVarargs
    public static <S extends Event> MergedSubscriptionBuilder<S> merge(@NotNull Class<S> superClass, @NotNull Class<? extends S>... eventClasses) {
        return MergedSubscriptionBuilder.newBuilder(superClass, eventClasses);
    }

    /**
     * Makes a MergedSubscriptionBuilder for a super event class
     *
     * @param superClass   the abstract super event class
     * @param priority     the priority to listen at
     * @param eventClasses the event classes to be bound to
     * @param <S>          the super class type
     * @return a {@link MergedSubscriptionBuilder} to construct the event handler
     */
    @NotNull
    @SafeVarargs
    public static <S extends Event> MergedSubscriptionBuilder<S> merge(@NotNull Class<S> superClass, @NotNull EventPriority priority, @NotNull Class<? extends S>... eventClasses) {
        return MergedSubscriptionBuilder.newBuilder(superClass, priority, eventClasses);
    }

    /**
     * Submit the event on the current thread
     *
     * @param event the event to call
     */
    public static void call(@NotNull Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Submit the event on a new async thread.
     *
     * @param event the event to call
     */
    public static void callAsync(@NotNull Event event) {
        Schedulers.runAsync(() -> call(event));
    }

    /**
     * Submit the event on the main server thread.
     *
     * @param event the event to call
     */
    public static void callSync(@NotNull Event event) {
        Schedulers.runGlobal(() -> call(event));
    }

    /**
     * Submit the event on the current thread
     *
     * @param event the event to call
     */
    @NotNull
    public static <T extends Event> T callAndReturn(@NotNull T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * Submit the event on a new async thread.
     *
     * @param event the event to call
     */
    @NotNull
    public static <T extends Event> T callAsyncAndJoin(@NotNull T event) {
        return Schedulers.callAsync(() -> callAndReturn(event)).join();
    }

    /**
     * Submit the event on the main server thread.
     *
     * @param event the event to call
     */
    @NotNull
    public static <T extends Event> T callSyncAndJoin(@NotNull T event) {
        return Schedulers.callGlobal(() -> callAndReturn(event)).join();
    }

    private Events() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
