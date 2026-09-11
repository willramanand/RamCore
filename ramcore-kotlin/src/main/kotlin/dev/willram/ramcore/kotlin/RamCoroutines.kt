@file:JvmName("RamCoroutines")

package dev.willram.ramcore.kotlin

import dev.willram.ramcore.RamPlugin
import dev.willram.ramcore.promise.Promise
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.TaskContext
import dev.willram.ramcore.terminable.Terminable
import dev.willram.ramcore.terminable.TerminableConsumer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.CompletionException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits this promise from a coroutine. Cancelling the coroutine cancels the promise.
 */
suspend fun <T> Promise<T>.await(): T = suspendCancellableCoroutine { continuation ->
    val future = this.toCompletableFuture()
    future.whenComplete { value, error ->
        when {
            error == null -> continuation.resume(value)
            error is CompletionException && error.cause != null -> continuation.resumeWithException(error.cause!!)
            else -> continuation.resumeWithException(error)
        }
    }
    continuation.invokeOnCancellation { this.cancel() }
}

/**
 * A [CoroutineDispatcher] that runs continuations on a RamCore [TaskContext]. Already-anchored code
 * is not re-dispatched: global checks [Schedulers.isSyncThread], region/entity check
 * [Bukkit.isOwnedByCurrentRegion].
 */
class RamContextDispatcher(val context: TaskContext) : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Schedulers.forContext(this.context).execute(block)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        val target = this.context
        val entity = target.entity()
        val location = target.location()
        return when {
            target.globalContext() -> !Schedulers.isSyncThread()
            target.asyncContext() -> true
            entity != null -> !Bukkit.isOwnedByCurrentRegion(entity)
            location != null -> !Bukkit.isOwnedByCurrentRegion(location)
            else -> true
        }
    }
}

/** Dispatchers that route coroutine work through RamCore's schedulers. */
object RamDispatchers {
    val global: CoroutineDispatcher = RamContextDispatcher(TaskContext.global())
    val async: CoroutineDispatcher = RamContextDispatcher(TaskContext.async())

    fun region(location: Location): CoroutineDispatcher = RamContextDispatcher(TaskContext.of(location))
    fun entity(entity: Entity): CoroutineDispatcher = RamContextDispatcher(TaskContext.of(entity))
    fun player(player: Player): CoroutineDispatcher = RamContextDispatcher(TaskContext.of(player))
}

/**
 * A [CoroutineScope] bound to this consumer: its job is a [SupervisorJob] on the given dispatcher,
 * and it is cancelled when the consumer closes.
 */
fun TerminableConsumer.coroutineScope(dispatcher: CoroutineDispatcher = RamDispatchers.global): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    this.bind(Terminable { scope.cancel() })
    return scope
}

/** Launches a coroutine in a plugin-bound scope; it is cancelled on plugin disable. */
fun RamPlugin.launch(
    dispatcher: CoroutineDispatcher = RamDispatchers.global,
    block: suspend CoroutineScope.() -> Unit
): Job = coroutineScope(dispatcher).launch(block = block)
