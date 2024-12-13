/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.testing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fake implementation of {@link ScheduledExecutorService} that lets tests control when tasks are
 * executed.
 */
@SuppressWarnings("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeScheduledExecutorService extends AbstractExecutorService
        implements ScheduledExecutorService, AutoCloseable {

    private static final TimeUnit CLOCK_UNIT = MILLISECONDS;

    private static long toClockUnit(Duration duration) {
        return duration.toMillis();
    }

    private static Duration durationFromClockUnit(long durationClockUnit) {
        return Duration.ofMillis(durationClockUnit);
    }

    private final Clock clock;
    private final Queue<Runnable> executeQueue = new ConcurrentLinkedQueue<>();
    private final PriorityBlockingQueue<DelayedFuture<?>> scheduledQueue =
            new PriorityBlockingQueue<>();

    private final AtomicLong nextSequenceId = new AtomicLong(0);
    private volatile boolean running = true;

    public FakeScheduledExecutorService() {
        this.clock = new Clock();
    }

    @Override
    public boolean isShutdown() {
        return !running;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown() && isEmpty();
    }

    @Override
    public void shutdown() {
        running = false;
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        running = false;
        List<Runnable> commands = Lists.newArrayList();
        commands.addAll(executeQueue);
        commands.addAll(scheduledQueue);
        executeQueue.clear();
        scheduledQueue.clear();
        return commands;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nullable TimeUnit unit) {
        checkState(!running);
        while (!executeQueue.isEmpty()) {
            runNext();
        }
        simulateSleepExecutingAllTasks(durationFromClockUnit(timeout));
        return isEmpty();
    }

    @Override
    public void execute(@Nullable Runnable command) {
        assertRunning();
        executeQueue.add(command);
    }

    private void assertRunning() {
        if (!running) {
            throw new RejectedExecutionException();
        }
    }

    @Override
    @NonNull
    public ScheduledFuture<?> schedule(
            @Nullable Runnable command, long delay, @Nullable TimeUnit unit) {
        assertRunning();
        DelayedFuture<?> future = new DelayedFuture<>(command, delay, unit);
        scheduledQueue.add(future);
        return future;
    }

    @Override
    @NonNull
    public <V> ScheduledFuture<V> schedule(
            @Nullable Callable<V> callable, long delay, @Nullable TimeUnit unit) {
        assertRunning();
        DelayedFuture<V> future = new DelayedCallable<V>(callable, delay, unit);
        scheduledQueue.add(future);
        return future;
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleAtFixedRate(
            @Nullable Runnable command, long initialDelay, long period, @Nullable TimeUnit unit) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleWithFixedDelay(
            @Nullable Runnable command, long initialDelay, long delay, @Nullable TimeUnit unit) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Returns true if the {@link #execute} queue contains at least one runnable. */
    public boolean hasNext() {
        return !executeQueue.isEmpty();
    }

    /** Runs the next runnable in the {@link #execute} queue. */
    public void runNext() {
        checkState(!executeQueue.isEmpty(), "execute queue must not be empty");
        Runnable runnable = executeQueue.remove();
        runTaskWithInterruptIsolation(runnable);
    }

    /** Runs all of the runnables that {@link #execute} enqueued. */
    public void runAll() {
        while (hasNext()) {
            runNext();
        }
    }

    /** Returns whether any runnable is in the {@link #execute} or {@link #schedule} queue. */
    @CheckReturnValue
    public boolean isEmpty() {
        return executeQueue.isEmpty() && scheduledQueue.isEmpty();
    }

    /**
     * Executes tasks from the {@link #schedule} queue until the given amount of simulated time has
     * passed.
     */
    public void simulateSleepExecutingAllTasks(@NonNull Duration duration) {
        long timeout = toClockUnit(duration);
        checkArgument(timeout >= 0, "timeout (%s) cannot be negative", timeout);

        long stopTime = clock.currentTimeMillis() + CLOCK_UNIT.toMillis(timeout);
        boolean done = false;

        while (!done) {
            long delay = (stopTime - clock.currentTimeMillis());
            if (delay >= 0 && simulateSleepExecutingAtMostOneTask(durationFromClockUnit(delay))) {
                continue;
            } else {
                done = true;
            }
        }
    }

    /**
     * Simulates sleeping up to the given timeout before executing the next scheduled task, if any.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public boolean simulateSleepExecutingAtMostOneTask(@NonNull Duration duration) {
        long timeout = toClockUnit(duration);
        checkArgument(timeout >= 0, "timeout (%s) cannot be negative", timeout);
        if (scheduledQueue.isEmpty()) {
            clock.advanceBy(duration);
            return false;
        }

        DelayedFuture<?> future = scheduledQueue.peek();
        long delay = future.getDelay(CLOCK_UNIT);
        if (delay > timeout) {
            // Next event is too far in the future; delay the entire time
            clock.advanceBy(duration);
            return false;
        }

        scheduledQueue.poll();
        runTaskWithInterruptIsolation(future);

        return true;
    }

    /**
     * Simulates sleeping as long as necessary before executing the next scheduled task. Does
     * nothing if the {@link #schedule} queue is empty.
     */
    public boolean simulateSleepExecutingAtMostOneTask() {
        if (scheduledQueue.isEmpty()) {
            return false;
        }

        DelayedFuture<?> future = scheduledQueue.poll();
        runTaskWithInterruptIsolation(future);
        return true;
    }

    private class DelayedFuture<T> implements ScheduledFuture<T>, Runnable {
        protected final long timeToRun;
        private final long sequenceId;
        private final Runnable command;
        private boolean cancelled;
        private boolean done;

        public DelayedFuture(Runnable command, long delay, TimeUnit unit) {
            checkArgument(delay >= 0, "delay (%s) cannot be negative", delay);

            this.command = command;
            timeToRun = clock.currentTimeMillis() + unit.toMillis(delay);
            sequenceId = nextSequenceId.getAndIncrement();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(timeToRun - clock.currentTimeMillis(), MILLISECONDS);
        }

        protected void maybeReschedule() {
            done = true;
        }

        @Override
        public void run() {
            if (clock.currentTimeMillis() < timeToRun) {
                clock.advanceBy(durationFromClockUnit(timeToRun - clock.currentTimeMillis()));
            }
            command.run();
            maybeReschedule();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            done = true;
            return scheduledQueue.remove(this);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public T get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            DelayedFuture<?> that = (DelayedFuture<?>) other;
            long diff = timeToRun - that.timeToRun;
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else if (sequenceId < that.sequenceId) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private class DelayedCallable<T> extends DelayedFuture<T> {
        private final FutureTask<T> task;

        private DelayedCallable(FutureTask<T> task, long delay, TimeUnit unit) {
            super(task, delay, unit);
            this.task = task;
        }

        public DelayedCallable(Callable<T> callable, long delay, TimeUnit unit) {
            this(new FutureTask<T>(callable), delay, unit);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            task.cancel(mayInterruptIfRunning);
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }
    }

    private static class Clock {
        private final AtomicReference<Instant> nowReference = new AtomicReference<>();

        public Clock() {
            setTo(Instant.EPOCH);
        }

        public long currentTimeMillis() {
            return nowReference.get().toEpochMilli();
        }

        public void advanceBy(Duration duration) {
            nowReference.getAndUpdate(now -> now.plus(duration));
        }

        public void setTo(Instant instant) {
            nowReference.set(instant);
        }
    }

    /** Clears this thread's interrupt bit, runs the task, and restores any previous interrupt. */
    private void runTaskWithInterruptIsolation(Runnable task) {
        boolean interruptBitWasSet = Thread.interrupted();
        try {
            task.run();
        } finally {
            if (interruptBitWasSet) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
