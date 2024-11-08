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

package dev.willram.ramcore.scheduler;


import dev.willram.ramcore.terminable.Terminable;

import javax.annotation.Nonnull;

/**
 * Represents a scheduled repeating task
 */
@Nonnull
public interface Task extends Terminable {

    /**
     * Gets the number of times this task has ran. The counter is only incremented at the end of execution.
     *
     * @return the number of times this task has ran
     */
    int getTimesRan();

    /**
     * Stops the task
     *
     * @return true if the task wasn't already cancelled
     */
    boolean stop();

    /**
     * Gets the Bukkit ID for this task
     *
     * @return the bukkit id for this task
     */
    int getBukkitId();

    /**
     * {@link #stop() Stops} the task
     */
    @Override
    default void close() {
        stop();
    }
}