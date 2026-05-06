@file:JvmName("RamCoreKtx")

package dev.willram.ramcore.kotlin

import com.google.common.reflect.TypeToken
import dev.willram.ramcore.commands.CommandInterruptException
import dev.willram.ramcore.commands.arguments.Argument
import dev.willram.ramcore.commands.arguments.ArgumentParser
import dev.willram.ramcore.commands.arguments.ArgumentParserRegistry
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.event.functional.merged.MergedSubscriptionBuilder
import dev.willram.ramcore.event.functional.single.SingleSubscriptionBuilder
import dev.willram.ramcore.metadata.MetadataKey
import dev.willram.ramcore.metadata.MetadataMap
import dev.willram.ramcore.scheduler.TaskContext
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import java.util.Optional

inline fun <reified T : Any> ramTypeToken(): TypeToken<T> = object : TypeToken<T>() {}

inline fun <reified T : Event> subscribe(
    priority: EventPriority = EventPriority.NORMAL
): SingleSubscriptionBuilder<T> = Events.subscribe(T::class.java, priority)

inline fun <reified T : Any> merge(): MergedSubscriptionBuilder<T> = Events.merge(ramTypeToken<T>())

inline fun <reified T : Any> metadataKey(id: String): MetadataKey<T> = MetadataKey.create(id, ramTypeToken<T>())

inline fun <reified T : Any> MetadataMap.value(id: String): T? = getOrNull(metadataKey<T>(id))

fun <T : Any> MetadataMap.value(key: MetadataKey<T>): T? = getOrNull(key)

operator fun <T : Any> MetadataMap.set(key: MetadataKey<T>, value: T) {
    put(key, value)
}

inline fun <reified T : Any> Argument.parse(): Optional<T> = parse(ramTypeToken<T>())

inline fun <reified T : Any> Argument.parseOrNull(): T? {
    val parsed = parse<T>()
    return if (parsed.isPresent) parsed.get() else null
}

@Throws(CommandInterruptException::class)
inline fun <reified T : Any> Argument.parseOrFail(): T = parseOrFail(ramTypeToken<T>())

inline fun <reified T : Any> ArgumentParserRegistry.find(): Optional<ArgumentParser<T>> = find(ramTypeToken<T>())

inline fun <reified T : Any> ArgumentParserRegistry.findAll(): Collection<ArgumentParser<T>> = findAll(ramTypeToken<T>())

inline fun <reified T : Any> ArgumentParserRegistry.register(noinline parser: (String) -> T?) {
    register(ramTypeToken<T>(), ArgumentParser.of { input -> Optional.ofNullable(parser(input)) })
}

fun <T : Any> Optional<T>.orNull(): T? = if (isPresent) get() else null

fun Entity.taskContext(): TaskContext = TaskContext.of(this)

fun Location.taskContext(): TaskContext = TaskContext.of(this)

fun Block.taskContext(): TaskContext = TaskContext.of(this)

fun BlockState.taskContext(): TaskContext = TaskContext.of(this)

fun Chunk.taskContext(): TaskContext = TaskContext.of(this)

fun World.chunkTaskContext(chunkX: Int, chunkZ: Int): TaskContext = TaskContext.of(this, chunkX, chunkZ)
