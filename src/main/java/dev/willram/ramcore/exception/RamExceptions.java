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

package dev.willram.ramcore.exception;

import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.exception.events.RamExceptionEvent;
import dev.willram.ramcore.exception.types.EventHandlerException;
import dev.willram.ramcore.exception.types.PromiseChainException;
import dev.willram.ramcore.exception.types.SchedulerTaskException;
import dev.willram.ramcore.interfaces.Delegate;
import dev.willram.ramcore.utils.LoaderUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central handler for exceptions that occur within user-written
 * Runnables and handlers running in helper.
 */
public final class RamExceptions {
    private RamExceptions() {}

    private static final ThreadLocal<AtomicBoolean> NOT_TODAY_STACK_OVERFLOW_EXCEPTION =
            ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    private static void log(InternalException exception) {
        // print to logger
        LoaderUtils.getPlugin().log(exception.getMessage());

        // call event
        AtomicBoolean firing = NOT_TODAY_STACK_OVERFLOW_EXCEPTION.get();
        if (firing.compareAndSet(false, true)) {
            try {
                Events.call(new RamExceptionEvent(exception));
            } finally {
                firing.set(false);
            }
        }
    }

    public static void reportScheduler(Throwable throwable) {
        log(new SchedulerTaskException(throwable));
    }

    public static void reportPromise(Throwable throwable) {
        log(new PromiseChainException(throwable));
    }

    public static void reportEvent(Object event, Throwable throwable) {
        log(new EventHandlerException(throwable, event));
        throwable.fillInStackTrace();
        throwable.printStackTrace();
    }

    public static Runnable wrapSchedulerTask(Runnable runnable) {
        return new SchedulerWrappedRunnable(runnable);
    }

    private static final class SchedulerWrappedRunnable implements Runnable, Delegate<Runnable> {
        private final Runnable delegate;

        private SchedulerWrappedRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                this.delegate.run();
            } catch (Throwable t) {
                reportScheduler(t);
            }
        }

        @Override
        public Runnable getDelegate() {
            return this.delegate;
        }
    }

}
