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

package dev.willram.ramcore.scheduler.builder;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Scheduler;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.scheduler.Task;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

class TaskBuilderImpl implements TaskBuilder {
    static final TaskBuilder INSTANCE = new TaskBuilderImpl();

    private final TaskBuilder.ThreadContextual sync;
    private final ThreadContextual async;

    private TaskBuilderImpl() {
        this.sync = new ThreadContextualBuilder(Schedulers.sync());
        this.async = new ThreadContextualBuilder(Schedulers.async());
    }

    @NotNull
    @Override
    public TaskBuilder.ThreadContextual on(@NotNull TaskContext context) {
        return new ThreadContextualBuilder(Schedulers.forContext(context));
    }

    @NotNull
    @Override
    public TaskBuilder.ThreadContextual sync() {
        return this.sync;
    }

    @NotNull
    @Override
    public TaskBuilder.ThreadContextual async() {
        return this.async;
    }

    private static final class ThreadContextualBuilder implements TaskBuilder.ThreadContextual {
        private final Scheduler scheduler;
        private final ContextualPromiseBuilder instant;

        ThreadContextualBuilder(Scheduler scheduler) {
            this.scheduler = scheduler;
            this.instant = new ContextualPromiseBuilderImpl(scheduler);
        }

        @NotNull
        @Override
        public ContextualPromiseBuilder now() {
            return this.instant;
        }

        @NotNull
        @Override
        public DelayedTick after(long ticks) {
            return new DelayedTickBuilder(this.scheduler, ticks);
        }

        @NotNull
        @Override
        public DelayedTime after(long duration, @NotNull TimeUnit unit) {
            return new DelayedTimeBuilder(this.scheduler, duration, unit);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder afterAndEvery(long ticks) {
            return new ContextualTaskBuilderTickImpl(this.scheduler, ticks, ticks);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder afterAndEvery(long duration, @NotNull TimeUnit unit) {
            return new ContextualTaskBuilderTimeImpl(this.scheduler, duration, unit, duration, unit);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder every(long ticks) {
            return new ContextualTaskBuilderTickImpl(this.scheduler, 0, ticks);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder every(long duration, @NotNull TimeUnit unit) {
            return new ContextualTaskBuilderTimeImpl(this.scheduler, 0, TimeUnit.NANOSECONDS, duration, unit);
        }
    }

    private static final class DelayedTickBuilder implements TaskBuilder.DelayedTick {
        private final Scheduler scheduler;
        private final long delay;

        DelayedTickBuilder(Scheduler scheduler, long delay) {
            this.scheduler = scheduler;
            this.delay = delay;
        }

        @NotNull
        @Override
        public <T> Promise<T> supply(@NotNull Supplier<T> supplier) {
            return this.scheduler.supplyLater(supplier, this.delay);
        }

        @NotNull
        @Override
        public <T> Promise<T> call(@NotNull Callable<T> callable) {
            return this.scheduler.callLater(callable, this.delay);
        }

        @NotNull
        @Override
        public Promise<Void> run(@NotNull Runnable runnable) {
            return this.scheduler.runLater(runnable, this.delay);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder every(long ticks) {
            return new ContextualTaskBuilderTickImpl(this.scheduler, this.delay, ticks);
        }
    }

    private static final class DelayedTimeBuilder implements TaskBuilder.DelayedTime {
        private final Scheduler scheduler;
        private final long delay;
        private final TimeUnit delayUnit;

        DelayedTimeBuilder(Scheduler scheduler, long delay, TimeUnit delayUnit) {
            this.scheduler = scheduler;
            this.delay = delay;
            this.delayUnit = delayUnit;
        }

        @NotNull
        @Override
        public <T> Promise<T> supply(@NotNull Supplier<T> supplier) {
            return this.scheduler.supplyLater(supplier, this.delay, this.delayUnit);
        }

        @NotNull
        @Override
        public <T> Promise<T> call(@NotNull Callable<T> callable) {
            return this.scheduler.callLater(callable, this.delay, this.delayUnit);
        }

        @NotNull
        @Override
        public Promise<Void> run(@NotNull Runnable runnable) {
            return this.scheduler.runLater(runnable, this.delay, this.delayUnit);
        }

        @NotNull
        @Override
        public ContextualTaskBuilder every(long duration, TimeUnit unit) {
            return new ContextualTaskBuilderTimeImpl(this.scheduler, this.delay, this.delayUnit, duration, unit);
        }
    }

    private static class ContextualPromiseBuilderImpl implements ContextualPromiseBuilder {
        private final Scheduler scheduler;

        ContextualPromiseBuilderImpl(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @NotNull
        @Override
        public <T> Promise<T> supply(@NotNull Supplier<T> supplier) {
            return this.scheduler.supply(supplier);
        }

        @NotNull
        @Override
        public <T> Promise<T> call(@NotNull Callable<T> callable) {
            return this.scheduler.call(callable);
        }

        @NotNull
        @Override
        public Promise<Void> run(@NotNull Runnable runnable) {
            return this.scheduler.run(runnable);
        }
    }

    private static class ContextualTaskBuilderTickImpl implements ContextualTaskBuilder {
        private final Scheduler scheduler;
        private final long delay;
        private final long interval;

        ContextualTaskBuilderTickImpl(Scheduler scheduler, long delay, long interval) {
            this.scheduler = scheduler;
            this.delay = delay;
            this.interval = interval;
        }

        @NotNull
        @Override
        public Task consume(@NotNull Consumer<Task> consumer) {
            return this.scheduler.runRepeating(consumer, this.delay, this.interval);
        }

        @NotNull
        @Override
        public Task run(@NotNull Runnable runnable) {
            return this.scheduler.runRepeating(runnable, this.delay, this.interval);
        }
    }

    private static class ContextualTaskBuilderTimeImpl implements ContextualTaskBuilder {
        private final Scheduler scheduler;
        private final long delay;
        private final TimeUnit delayUnit;
        private final long interval;
        private final TimeUnit intervalUnit;

        ContextualTaskBuilderTimeImpl(Scheduler scheduler, long delay, TimeUnit delayUnit, long interval, TimeUnit intervalUnit) {
            this.scheduler = scheduler;
            this.delay = delay;
            this.delayUnit = delayUnit;
            this.interval = interval;
            this.intervalUnit = intervalUnit;
        }

        @NotNull
        @Override
        public Task consume(@NotNull Consumer<Task> consumer) {
            return this.scheduler.runRepeating(consumer, this.delay, this.delayUnit, this.interval, this.intervalUnit);
        }

        @NotNull
        @Override
        public Task run(@NotNull Runnable runnable) {
            return this.scheduler.runRepeating(runnable, this.delay, this.delayUnit, this.interval, this.intervalUnit);
        }
    }
}
