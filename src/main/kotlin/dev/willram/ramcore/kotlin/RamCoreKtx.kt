@file:JvmName("RamCoreKtx")

package dev.willram.ramcore.kotlin

import com.google.common.reflect.TypeToken
import dev.willram.ramcore.commands.CommandArgument
import dev.willram.ramcore.commands.CommandCooldown
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.CommandModule
import dev.willram.ramcore.commands.CommandSpec
import dev.willram.ramcore.commands.RamCommands
import dev.willram.ramcore.commands.ResolvedCommandArgument
import dev.willram.ramcore.config.BukkitConfig
import dev.willram.ramcore.config.ConfigKey
import dev.willram.ramcore.content.ContentId
import dev.willram.ramcore.cooldown.Cooldown
import dev.willram.ramcore.encounter.EncounterAbility
import dev.willram.ramcore.encounter.EncounterDefinition
import dev.willram.ramcore.encounter.EncounterListener
import dev.willram.ramcore.encounter.EncounterPhase
import dev.willram.ramcore.encounter.EncounterRegistry
import dev.willram.ramcore.encounter.Encounters
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.event.functional.merged.MergedSubscriptionBuilder
import dev.willram.ramcore.event.functional.single.SingleSubscriptionBuilder
import dev.willram.ramcore.integration.IntegrationRegistry
import dev.willram.ramcore.integration.Integrations
import dev.willram.ramcore.integration.PluginDetector
import dev.willram.ramcore.message.MessageCatalog
import dev.willram.ramcore.message.MessageKey
import dev.willram.ramcore.message.MessagePlaceholders
import dev.willram.ramcore.metadata.MetadataKey
import dev.willram.ramcore.metadata.MetadataMap
import dev.willram.ramcore.npc.NpcHandle
import dev.willram.ramcore.npc.NpcRegistry
import dev.willram.ramcore.npc.NpcSpec
import dev.willram.ramcore.npc.Npcs
import dev.willram.ramcore.objective.ObjectiveAction
import dev.willram.ramcore.objective.ObjectiveDefinition
import dev.willram.ramcore.objective.ObjectiveTask
import dev.willram.ramcore.objective.ObjectiveTracker
import dev.willram.ramcore.objective.Objectives
import dev.willram.ramcore.party.Parties
import dev.willram.ramcore.party.PartyManager
import dev.willram.ramcore.party.PartyOptions
import dev.willram.ramcore.permission.PermissionNode
import dev.willram.ramcore.permission.PermissionRequirement
import dev.willram.ramcore.permission.Permissions
import dev.willram.ramcore.promise.Promise
import dev.willram.ramcore.scheduler.TaskContext
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.Plugin
import java.nio.file.Path
import java.util.concurrent.TimeUnit

inline fun <reified T : Any> ramTypeToken(): TypeToken<T> = object : TypeToken<T>() {}

inline fun <reified T : Event> subscribe(
    priority: EventPriority = EventPriority.NORMAL
): SingleSubscriptionBuilder<T> = Events.subscribe(T::class.java, priority)

inline fun <reified T : Any> merge(): MergedSubscriptionBuilder<T> = Events.merge(ramTypeToken<T>())

inline fun <reified T : Any> metadataKey(id: String): MetadataKey<T> = MetadataKey.create(id, ramTypeToken<T>())

inline fun <reified T : Any> configKey(path: String, defaultValue: T): ConfigKey<T> =
    ConfigKey.of(path, T::class.java, defaultValue)

inline fun <reified T : Any> requiredConfigKey(path: String): ConfigKey<T> =
    ConfigKey.required(path, T::class.java)

fun bukkitConfig(path: Path, vararg keys: ConfigKey<*>): BukkitConfig =
    BukkitConfig.load(path, *keys)

fun messageKey(id: String, defaultTemplate: String): MessageKey =
    MessageKey.of(id, defaultTemplate)

fun messageCatalog(configure: MessageCatalog.Builder.() -> Unit): MessageCatalog =
    MessageCatalog.builder().apply(configure).build()

fun parsedPlaceholder(name: String, value: Any): TagResolver =
    MessagePlaceholders.parsed(name, value)

fun unparsedPlaceholder(name: String, value: Any): TagResolver =
    MessagePlaceholders.unparsed(name, value)

fun componentPlaceholder(name: String, value: ComponentLike): TagResolver =
    MessagePlaceholders.component(name, value)

fun permission(value: String): PermissionNode =
    Permissions.node(value)

fun permission(value: String, denialMessage: String): PermissionNode =
    Permissions.node(value, denialMessage)

fun permissionsAll(vararg nodes: PermissionNode): PermissionRequirement =
    Permissions.all(*nodes)

fun permissionsAny(vararg nodes: PermissionNode): PermissionRequirement =
    Permissions.any(*nodes)

operator fun CommandSender.contains(node: PermissionNode): Boolean =
    Permissions.has(this, node)

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

fun CommandSpec.Node.cooldown(amount: Long, unit: TimeUnit): CommandSpec.Node =
    cooldown(Cooldown.of(amount, unit))

fun CommandSpec.Node.cooldownTicks(ticks: Long): CommandSpec.Node =
    cooldown(Cooldown.ofTicks(ticks))

fun CommandSpec.Node.cooldown(cooldown: Cooldown, key: (CommandContext) -> Any): CommandSpec.Node =
    cooldown(CommandCooldown.keyed(cooldown, key))

operator fun <T : Any> CommandContext.get(argument: CommandArgument<T>): T = this.get(argument)

fun <T : Any, R : ArgumentResolver<T>> CommandContext.resolve(argument: ResolvedCommandArgument<T, R>): T =
    this.resolve(argument)

fun Entity.taskContext(): TaskContext = TaskContext.of(this)

fun Location.taskContext(): TaskContext = TaskContext.of(this)

fun Block.taskContext(): TaskContext = TaskContext.of(this)

fun BlockState.taskContext(): TaskContext = TaskContext.of(this)

fun Chunk.taskContext(): TaskContext = TaskContext.of(this)

fun World.chunkTaskContext(chunkX: Int, chunkZ: Int): TaskContext = TaskContext.of(this, chunkX, chunkZ)

inline fun <reified T : Entity> npcSpec(configure: NpcSpec<T>.() -> Unit = {}): NpcSpec<T> =
    NpcSpec.of(T::class.java).apply(configure)

fun npcRegistry(): NpcRegistry = NpcRegistry.create()

fun npcRegistry(plugin: Plugin): NpcRegistry = Npcs.registry(plugin)

fun <T : Entity> Location.spawnNpc(spec: NpcSpec<T>): Promise<NpcHandle<T>> =
    Npcs.spawn(this, spec)

fun partyOptions(): PartyOptions = Parties.options()

fun partyManager(): PartyManager = Parties.manager()

fun partyManager(options: PartyOptions): PartyManager = Parties.manager(options)

fun objectiveTracker(): ObjectiveTracker = Objectives.tracker()

fun objective(id: ContentId, configure: ObjectiveDefinition.Builder.() -> Unit): ObjectiveDefinition =
    Objectives.objective(id).apply(configure).build()

fun objectiveTask(id: String, action: ObjectiveAction, target: String, required: Long): ObjectiveTask =
    Objectives.task(id, action, target, required)

fun encounterRegistry(): EncounterRegistry = Encounters.registry()

fun encounterRegistry(listener: EncounterListener): EncounterRegistry = Encounters.registry(listener)

fun encounter(id: ContentId, maxHealth: Double, configure: EncounterDefinition.Builder.() -> Unit): EncounterDefinition =
    Encounters.encounter(id, maxHealth).apply(configure).build()

fun encounterPhase(id: String, atOrBelowHealthPercent: Double, configure: EncounterPhase.() -> Unit = {}): EncounterPhase =
    Encounters.phase(id, atOrBelowHealthPercent).apply(configure)

fun encounterAbility(id: String, intervalTicks: Long, configure: EncounterAbility.() -> Unit = {}): EncounterAbility =
    Encounters.ability(id, intervalTicks).apply(configure)

fun integrationRegistry(): IntegrationRegistry = Integrations.registry()

fun standardIntegrations(): IntegrationRegistry = Integrations.standard()

fun standardIntegrations(detector: PluginDetector): IntegrationRegistry = Integrations.standard(detector)
