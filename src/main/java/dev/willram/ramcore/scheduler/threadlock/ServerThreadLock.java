package dev.willram.ramcore.scheduler.threadlock;

import dev.willram.ramcore.terminable.Terminable;

/**
 * Blocks the caller until the server thread is parked, runs the caller's code while the server
 * thread waits, and releases it on {@link #close()}.
 *
 * @deprecated Parking the global tick thread from another thread is unsafe on Folia, where there is
 * no single server thread that owns world state, and it stalls every region on Paper. Schedule the
 * work instead: {@code Schedulers.call(TaskContext.of(entity), callable).join()} from an async
 * thread, or {@code Promise.thenApply(TaskContext, fn)} to continue on the owning thread. Obtaining
 * a lock on a regionised (Folia) server throws {@code ApiMisuseException}.
 */
@Deprecated(since = "2.1")
public interface ServerThreadLock extends Terminable {

    /**
     * Obtains a lock on the server thread. Returns immediately when already on it.
     *
     * @return the lock; close it to release the server thread
     * @throws dev.willram.ramcore.exception.ApiMisuseException on a regionised (Folia) server
     */
    static ServerThreadLock obtain() {
        return new ServerThreadLockImpl();
    }

    @Override
    void close();

}
