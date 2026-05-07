@file:JvmName("RamCoreKtx")

package dev.willram.ramcore.kotlin

import com.google.common.reflect.TypeToken
import dev.willram.ramcore.commands.CommandArgument
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.CommandModule
import dev.willram.ramcore.commands.CommandSpec
import dev.willram.ramcore.commands.RamCommands
import dev.willram.ramcore.commands.ResolvedCommandArgument
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.event.functional.merged.MergedSubscriptionBuilder
import dev.willram.ramcore.event.functional.single.SingleSubscriptionBuilder
import dev.willram.ramcore.metadata.MetadataKey
import dev.willram.ramcore.metadata.MetadataMap
import dev.willram.ramcore.scheduler.TaskContext
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority

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

fun command(label: String, configure: CommandSpec.() -> Unit): CommandSpec =
    RamCommands.command(label).apply(configure)

fun commandModule(vararg specs: CommandSpec): CommandModule =
    RamCommands.module(*specs)

fun Commands.register(vararg specs: CommandSpec): Set<String> =
    RamCommands.register(this, *specs)

fun Commands.register(vararg modules: CommandModule): Set<String> =
    RamCommands.register(this, *modules)

fun CommandSpec.literal(name: String, configure: CommandSpec.Node.() -> Unit): CommandSpec {
    literal(name).configure()
    return this
}

fun <T : Any> CommandSpec.argument(argument: CommandArgument<T>, configure: CommandSpec.Node.() -> Unit): CommandSpec {
    argument(argument).configure()
    return this
}

fun <T : Any, R : ArgumentResolver<T>> CommandSpec.argument(
    argument: ResolvedCommandArgument<T, R>,
    configure: CommandSpec.Node.() -> Unit
): CommandSpec {
    argument(argument).configure()
    return this
}

fun CommandSpec.Node.literal(name: String, configure: CommandSpec.Node.() -> Unit): CommandSpec.Node {
    literal(name).configure()
    return this
}

fun <T : Any> CommandSpec.Node.argument(argument: CommandArgument<T>, configure: CommandSpec.Node.() -> Unit): CommandSpec.Node {
    argument(argument).configure()
    return this
}

fun <T : Any, R : ArgumentResolver<T>> CommandSpec.Node.argument(
    argument: ResolvedCommandArgument<T, R>,
    configure: CommandSpec.Node.() -> Unit
): CommandSpec.Node {
    argument(argument).configure()
    return this
}

operator fun <T : Any> CommandContext.get(argument: CommandArgument<T>): T = this.get(argument)

fun <T : Any, R : ArgumentResolver<T>> CommandContext.resolve(argument: ResolvedCommandArgument<T, R>): T =
    this.resolve(argument)

fun Entity.taskContext(): TaskContext = TaskContext.of(this)

fun Location.taskContext(): TaskContext = TaskContext.of(this)

fun Block.taskContext(): TaskContext = TaskContext.of(this)

fun BlockState.taskContext(): TaskContext = TaskContext.of(this)

fun Chunk.taskContext(): TaskContext = TaskContext.of(this)

fun World.chunkTaskContext(chunkX: Int, chunkZ: Int): TaskContext = TaskContext.of(this, chunkX, chunkZ)
