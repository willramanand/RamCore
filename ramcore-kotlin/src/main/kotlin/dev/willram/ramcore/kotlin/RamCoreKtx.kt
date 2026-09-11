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

// ---- stores (task 1.1) ----

fun <K : Any, V : Any> inMemoryStore(): dev.willram.ramcore.store.InMemoryStore<K, V> =
    dev.willram.ramcore.store.Stores.inMemory()

fun <K : Any, V : Any> dev.willram.ramcore.store.Store<K, V>.cached(): dev.willram.ramcore.store.CachedStore<K, V> =
    dev.willram.ramcore.store.Stores.cached(this)

inline fun <reified V : Any> jsonStoreByUuid(directory: java.nio.file.Path): dev.willram.ramcore.store.FileStore<java.util.UUID, V> =
    dev.willram.ramcore.store.Stores.jsonByUuid(directory, V::class.java)

inline fun <reified V : Any> jsonStoreByString(directory: java.nio.file.Path): dev.willram.ramcore.store.FileStore<String, V> =
    dev.willram.ramcore.store.Stores.jsonByString(directory, V::class.java)

fun <V : Any> migrations(configure: dev.willram.ramcore.store.StoreMigrations<V>.() -> dev.willram.ramcore.store.StoreMigrations<V>): dev.willram.ramcore.store.StoreMigrations<V> =
    dev.willram.ramcore.store.StoreMigrations.start<V>().configure()

fun partyManager(options: PartyOptions, store: dev.willram.ramcore.party.PartyStore): PartyManager =
    PartyManager.create(options, java.time.Clock.systemUTC(), store)

fun objectiveTracker(store: dev.willram.ramcore.objective.ObjectiveProgressStore): ObjectiveTracker =
    ObjectiveTracker.create(store)

// ---- player data (task 1.2) ----

/** A [dev.willram.ramcore.playerdata.PlayerDataKey] whose values are handed to the async writer as-is (immutable values). */
inline fun <reified T : Any> playerDataKey(id: String, noinline default: () -> T): dev.willram.ramcore.playerdata.PlayerDataKey<T> =
    dev.willram.ramcore.playerdata.PlayerDataKey.of(id, T::class.java, default)

/** A [dev.willram.ramcore.playerdata.PlayerDataKey] whose values are copied on the player's thread before every async save. */
inline fun <reified T : Any> playerDataKey(id: String, noinline default: () -> T, noinline snapshot: (T) -> T): dev.willram.ramcore.playerdata.PlayerDataKey<T> =
    dev.willram.ramcore.playerdata.PlayerDataKey.of(id, T::class.java, default, snapshot)

fun playerDataOptions(configure: dev.willram.ramcore.playerdata.PlayerDataOptions.() -> dev.willram.ramcore.playerdata.PlayerDataOptions = { this }): dev.willram.ramcore.playerdata.PlayerDataOptions =
    dev.willram.ramcore.playerdata.PlayerDataOptions.defaults().configure()

/** The loaded value for this player, or null before the load completes (see [dev.willram.ramcore.playerdata.JoinPolicy.DEFER]). */
fun <T : Any> org.bukkit.entity.Player.data(service: dev.willram.ramcore.playerdata.PlayerDataService, key: dev.willram.ramcore.playerdata.PlayerDataKey<T>): T? =
    service.get(this, key).orElse(null)

/** Replaces the value and marks it dirty. */
fun <T : Any> org.bukkit.entity.Player.setData(service: dev.willram.ramcore.playerdata.PlayerDataService, key: dev.willram.ramcore.playerdata.PlayerDataKey<T>, value: T) =
    service.set(this, key, value)

// ---- message locales (task 1.3) ----

/** Receiver for the [locale] DSL: `WELCOME to "Willkommen"` adds a template. */
class LocaleMessagesScope {
    val templates: MutableMap<MessageKey, String> = LinkedHashMap()

    infix fun MessageKey.to(template: String) {
        templates[this] = template
    }
}

/** Adds a locale's templates: `messageCatalog { locale(Locale.GERMANY) { WELCOME to "..." } }`. */
fun MessageCatalog.Builder.locale(locale: java.util.Locale, block: LocaleMessagesScope.() -> Unit): MessageCatalog.Builder =
    locale(locale, LocaleMessagesScope().apply(block).templates)

/** Loads a YAML message bundle from a directory (see MessageCatalogLoader). */
fun messagesYaml(directory: java.nio.file.Path, baseName: String = "messages", defaultLocale: java.util.Locale = java.util.Locale.US): dev.willram.ramcore.message.MessageCatalogLoader.Bundle =
    dev.willram.ramcore.message.MessageCatalogLoader.yaml(directory, baseName, defaultLocale)

// ---- player input (task 1.4) ----

fun inputRequest(configure: dev.willram.ramcore.input.InputRequest.Builder.() -> Unit): dev.willram.ramcore.input.InputRequest =
    dev.willram.ramcore.input.InputRequest.builder().apply(configure).build()

/** Asks this player for text: `player.askText { prompt(msg); timeout(200) }`. */
fun org.bukkit.entity.Player.askText(configure: dev.willram.ramcore.input.InputRequest.Builder.() -> Unit = {}): dev.willram.ramcore.promise.Promise<String> =
    dev.willram.ramcore.input.PlayerInput.request(this, inputRequest(configure))

/** Asks this player for text and parses it; a parse failure consumes a retry. */
fun <T : Any> org.bukkit.entity.Player.askText(parser: dev.willram.ramcore.input.InputParser<T>, configure: dev.willram.ramcore.input.InputRequest.Builder.() -> Unit = {}): dev.willram.ramcore.promise.Promise<T> =
    dev.willram.ramcore.input.PlayerInput.request(this, inputRequest(configure), parser)

// ---- economy, rewards, placeholders (task 1.5) ----

fun inMemoryEconomy(): dev.willram.ramcore.economy.Economy =
    dev.willram.ramcore.economy.Economies.inMemory()

fun detectEconomy(registry: dev.willram.ramcore.integration.IntegrationRegistry): dev.willram.ramcore.economy.Economy? =
    dev.willram.ramcore.economy.Economies.detect(registry).orElse(null)

fun rewardPlan(configure: dev.willram.ramcore.reward.RewardPlan.Builder.() -> Unit): dev.willram.ramcore.reward.RewardPlan =
    dev.willram.ramcore.reward.RewardPlan.builder().apply(configure).build()

fun placeholderRegistry(configure: dev.willram.ramcore.placeholder.PlaceholderRegistry.() -> Unit = {}): dev.willram.ramcore.placeholder.PlaceholderRegistry =
    dev.willram.ramcore.placeholder.PlaceholderRegistry.create().apply(configure)

fun ramCorePlaceholders(configure: dev.willram.ramcore.placeholder.RamCorePlaceholders.Builder.() -> Unit): dev.willram.ramcore.placeholder.PlaceholderProvider =
    dev.willram.ramcore.placeholder.RamCorePlaceholders.builder().apply(configure).build()

fun regionTracker(engine: dev.willram.ramcore.region.RegionRuleEngine): dev.willram.ramcore.region.RegionTracker =
    dev.willram.ramcore.region.RegionTracker.create(engine)

// ---- content definitions (task 1.6) ----

fun contentLoad(root: java.nio.file.Path): dev.willram.ramcore.content.ContentLoadResult =
    dev.willram.ramcore.content.ContentLoader.load(root)

/** Builds a SpecLoader and loads a directory: `contentLoader(dir) { deserializer("items", ItemSpec::deserialize) }`. */
fun contentLoader(root: java.nio.file.Path, configure: dev.willram.ramcore.content.SpecLoader.() -> Unit): dev.willram.ramcore.content.SpecLoadResult =
    dev.willram.ramcore.content.SpecLoader.create().apply(configure).load(root)
