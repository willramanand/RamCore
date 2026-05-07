# RamCore Public API

This document describes the public interfaces intended for plugin authors. It is organized by subsystem instead of raw package order, because most RamCore APIs are meant to be used as workflows.

Unless stated otherwise, examples assume:

```java
import dev.willram.ramcore.*;
import dev.willram.ramcore.commands.*;
import dev.willram.ramcore.scheduler.*;
import dev.willram.ramcore.terminable.*;
```

## Plugin Base

### `RamPlugin`

`RamPlugin` is the preferred base class for plugins using RamCore.

Implement:

- `load()` for `onLoad` work.
- `enable()` for `onEnable` work.
- `disable()` for plugin-specific shutdown.
- `registerCommands(Commands commands)` for Paper lifecycle command registration.

Useful methods:

- `log(String message)` sends a MiniMessage-formatted console line with the plugin name prefix.
- `registerListener(Listener listener)` registers a Bukkit listener against this plugin.
- `bind(AutoCloseable terminable)` registers resources to be closed automatically on disable.
- `bindModule(TerminableModule module)` sets up a reusable module and binds its resources.

Example:

```java
public final class ExamplePlugin extends RamPlugin {
    @Override
    public void load() {
    }

    @Override
    public void enable() {
        registerListener(new ExampleListener());
    }

    @Override
    public void disable() {
    }

    @Override
    public void registerCommands(Commands commands) {
        RamCommands.register(commands, ExampleCommands.COMMAND);
    }
}
```

`RamPlugin` owns a `CompositeTerminable`. Bound listeners, tasks, subscriptions, and other closeables are closed in LIFO order during disable.

## Commands

Package: `dev.willram.ramcore.commands`

RamCore exposes a Brigadier-first command DSL over Paper's command lifecycle API. It does not use Bukkit-style `String[] args` parsing.

Primary types:

- `RamCommands` creates and registers command specs.
- `CommandSpec` builds Brigadier literal and argument trees.
- `CommandSpec.Node` configures nested literals and arguments.
- `RamArguments` exposes common Brigadier and Paper argument factories.
- `CommandArgument<T>` declares direct Brigadier values such as `String`, `Integer`, `World`, or `Component`.
- `ResolvedCommandArgument<T, R>` declares Paper resolver arguments such as player/entity selectors.
- `CommandContext` wraps Brigadier's context and exposes sender, player, location, typed arguments, resolved selectors, messaging, and validation helpers.
- `CommandExecutor` is the throwing functional interface for command execution.
- `CommandSuggestionProvider` and `CommandSuggestions` provide reusable tab completion helpers.
- `CommandInterruptException` cleanly stops execution and sends a sender-facing message.

Public command API surface:

- Stable entry points: `RamCommands`, `CommandSpec`, `CommandSpec.Node`, `RamArguments`, `CommandContext`, `CommandModule`, `CommandArgument`, `ResolvedCommandArgument`, `CommandSuggestion`, `CommandSuggestionProvider`, `StringCommandSuggestionProvider`, `CommandSuggestions`, `CommandExecutor`, and `CommandInterruptException`.
- Raw Brigadier interop is intentionally available through `CommandSpec.Node#thenBrigadier(...)` and `CommandSpec.Node#brigadier()`.
- Command trees should be created during Paper's command lifecycle, normally through `RamPlugin#registerCommands`.
- Configure the full command tree before registration. Building or registering a spec finalizes its nodes.
- The generated help output filters branches the sender cannot run. This includes `permission(...)`, `playerOnly()`, and custom `requires(...)` predicates.
- Incomplete non-executable nodes get a RamCore fallback message for missing subcommands or arguments instead of Paper's generic incomplete-command response.

### Java Command Example

```java
public final class ExampleCommands {
    private static final ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> TARGET =
            RamArguments.player("target");

    private static final CommandArgument<Integer> AMOUNT =
            RamArguments.integer("amount", 1, 64);

    public static final CommandSpec COMMAND = RamCommands.command("coins")
            .description("Manage player coins")
            .permission("example.coins")
            .withHelp()
            .literal("give", give -> give
                    .argument(TARGET, target -> target
                            .argument(AMOUNT, amount -> amount.executes(ctx -> {
                                Player targetPlayer = ctx.player(TARGET);
                                int coins = ctx.get(AMOUNT);

                                ctx.assertTrue(coins > 0, "<red>Amount must be positive.");
                                ctx.msg("<green>Gave " + coins + " coins to " + targetPlayer.getName() + ".");
                            }))));
}
```

### Generated Help

Use `.withHelp()` to add a generated `help` literal:

```java
CommandSpec command = RamCommands.command("coins")
        .description("Manage player coins.")
        .alias("coin")
        .withHelp();
```

The generated help includes:

- command title and description
- aliases
- visible usage lines only
- examples declared with `example(...)`

Add descriptions and examples on executable nodes:

```java
command.literal("give", give -> give
        .permission("example.coins.give")
        .argument(TARGET, target -> target
                .argument(AMOUNT, amount -> amount
                        .description("Give coins to one player.")
                        .example("coins give Steve 50")
                        .executes(ctx -> {
                            // ...
                        }))));
```

If a player runs `/coins give Steve` without the amount, RamCore reports the missing `<amount>` argument and shows the example when one is available.

### Kotlin Command Example

```kotlin
private val target = RamArguments.player("target")
private val amount = RamArguments.integer("amount", 1, 64)

val coinsCommand = command("coins") {
    description("Manage player coins")
    permission("example.coins")
    withHelp()

    literal("give") {
        argument(target) {
            argument(amount) {
                description("Give coins to one player.")
                example("coins give Steve 50")

                executes { ctx ->
                    val player = ctx.player(target)
                    val coins = ctx[amount]
                    ctx.msg("<green>Gave $coins coins to ${player.name}.")
                }
            }
        }
    }
}
```

### Argument Access

Use argument constants instead of string names:

```java
CommandArgument<String> REASON = RamArguments.greedyString("reason");

String reason = ctx.get(REASON);
Optional<String> optionalReason = ctx.optional(REASON);
```

For Paper selector arguments:

```java
ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> TARGETS =
        RamArguments.players("targets");

List<Player> players = ctx.players(TARGETS);
```

### Requirements

Use `permission(...)`, `playerOnly()`, or custom predicates:

```java
RamCommands.command("staff")
        .permission("example.staff")
        .playerOnly()
        .requires(source -> source.getSender().isOp());
```

These requirements also affect generated help visibility. A console sender will not see `playerOnly()` branches, and a player without a node permission will not see that branch in generated help.

### Suggestions

Static and dynamic suggestion helpers:

```java
CommandArgument<String> WORLD_NAME = RamArguments.word("world");

RamCommands.command("warp")
        .argument(WORLD_NAME, world -> world
                .suggests(CommandSuggestions.worlds())
                .executes(ctx -> {
                    String worldName = ctx.get(WORLD_NAME);
                }));
```

### Raw Brigadier Escape Hatch

Use `CommandSpec.Node#thenBrigadier(...)` to attach a raw Brigadier child when Paper or Brigadier exposes behavior RamCore does not wrap yet. `CommandSpec.Node#brigadier()` returns the underlying `ArgumentBuilder<CommandSourceStack, ?>` for advanced mutation of the current node.

Prefer the RamCore DSL unless you need Brigadier behavior that is not represented by `CommandSpec.Node`.

## Scheduling And Threads

Package: `dev.willram.ramcore.scheduler`

RamCore scheduling is Paper/Folia-aware. Prefer anchored scheduling over direct Bukkit scheduler calls when touching world state.

Primary types:

- `Schedulers` provides global, async, entity, region, block, chunk, and player scheduling helpers.
- `Scheduler` is an executor-like facade that can run, call, delay, and repeat work.
- `TaskContext` declares scheduling intent: global, async, entity-bound, region-bound, or chunk-bound.
- `Task` represents a repeating task and is also a `Terminable`.
- `Ticks` converts between ticks and time units.
- `RamExecutors` exposes executor instances for sync/async use.
- `TaskBuilder`, `ContextualTaskBuilder`, and `ContextualPromiseBuilder` provide a fluent scheduling builder.

Common patterns:

```java
Schedulers.runGlobal(() -> {
    // server/global thread work
});

Schedulers.runAsync(() -> {
    // background work
});

Schedulers.run(player, () -> {
    // entity-region safe work for this player
});

Schedulers.runLater(TaskContext.of(location), () -> {
    // region-safe delayed work
}, 20L);

Task task = Schedulers.runTimer(TaskContext.async(), () -> {
    // repeating async work
}, 20L, 20L);
plugin.bind(task);
```

Use `TaskContext` where the target is not obvious:

```java
Schedulers.run(TaskContext.of(chunk), () -> updateChunkData(chunk));
Schedulers.run(TaskContext.async(), this::saveCache);
```

## Promises

Package: `dev.willram.ramcore.promise`

`Promise<V>` is a server-thread-aware future abstraction. It resembles `CompletableFuture`, but every continuation declares sync or async intent.

Primary types:

- `Promise<V>` represents an eventually supplied value.
- `ThreadContext` is `SYNC` or `ASYNC`.

Creation:

```java
Promise<String> p = Promise.supplyingAsync(() -> loadNameFromDisk(uuid));
Promise<Void> started = Promise.start();
Promise<Integer> complete = Promise.completed(5);
Promise<Integer> failed = Promise.exceptionally(new IllegalStateException("failed"));
```

Continuation:

```java
Promise.supplyingAsync(() -> loadProfile(uuid))
        .thenAcceptSync(profile -> player.sendMessage(profile.displayName()))
        .exceptionallySync(error -> {
            player.sendRichMessage("<red>Failed to load profile.");
            return null;
        });
```

Use `toCompletableFuture()` when interoperating with APIs that expect JDK futures.

## Terminables And Resource Ownership

Package: `dev.willram.ramcore.terminable`

RamCore uses `Terminable` to make task, subscription, and resource cleanup explicit.

Primary types:

- `Terminable` extends `AutoCloseable` and adds safe closing helpers.
- `TerminableConsumer` accepts closeables and modules.
- `CompositeTerminable` owns many closeables and closes them in reverse bind order.
- `TerminableModule` is a reusable setup unit.

Examples:

```java
CompositeTerminable composite = CompositeTerminable.create();
composite.bind(Schedulers.runTimer(TaskContext.async(), this::tick, 20L, 20L));
composite.closeAndReportException();
```

With `RamPlugin`:

```java
bind(Events.subscribe(PlayerJoinEvent.class)
        .handler(event -> event.getPlayer().sendRichMessage("<green>Welcome."))
);
```

## Events

Package: `dev.willram.ramcore.event`

RamCore event APIs create terminable functional subscriptions.

Primary types:

- `Events` creates single or merged event subscriptions and calls events.
- `SingleSubscription<T>` represents a subscription to one Bukkit event type.
- `MergedSubscription<T>` maps multiple event classes into one handler type.
- `ProtocolSubscription` represents ProtocolLib packet subscriptions.
- `SingleSubscriptionBuilder`, `MergedSubscriptionBuilder`, and `ProtocolSubscriptionBuilder` configure subscriptions.
- `EventFilters` and `EventHandlers` provide reusable predicates and handlers.

Single event:

```java
bind(Events.subscribe(PlayerJoinEvent.class)
        .filter(event -> event.getPlayer().hasPermission("example.join"))
        .handler(event -> event.getPlayer().sendRichMessage("<green>Hello."))
);
```

Merged events:

```java
bind(Events.merge(Player.class)
        .bindEvent(PlayerQuitEvent.class, PlayerQuitEvent::getPlayer)
        .bindEvent(PlayerDeathEvent.class, PlayerDeathEvent::getEntity)
        .handler(player -> cleanup(player))
);
```

Call events:

```java
Events.call(new ExampleEvent(...));
Events.callSync(new ExampleEvent(...));
Events.callAsync(new ExampleEvent(...));
```

## Commands And Events Lifecycle Rule

When using `RamPlugin`, commands are registered during Paper's `LifecycleEvents.COMMANDS`. Event subscriptions and scheduled tasks should usually be bound to the plugin with `bind(...)` so they clean up on disable.

## ProtocolLib Integration

Package: `dev.willram.ramcore.protocol`

`Protocol` exposes helpers around ProtocolLib. Use it together with the protocol event builders for packet listeners.

Primary types:

- `Protocol` centralizes ProtocolLib access.
- `ProtocolSubscriptionBuilder` configures packet subscriptions.
- `ProtocolHandlerList` stores protocol subscriptions.

ProtocolLib is optional in `paper-plugin.yml`; guard features if your plugin can run without it.

## Metadata

Package: `dev.willram.ramcore.metadata`

RamCore metadata is an in-memory, typed key/value store associated with players, entities, blocks, and worlds.

Primary types:

- `Metadata` provides standard registries and object-specific lookup helpers.
- `MetadataKey<T>` is a typed key.
- `MetadataMap` stores values by typed key.
- `MetadataRegistry<T>` owns maps for a domain key type.
- `TransientValue<T>` wrappers control retention.
- `ExpiringValue`, `ExpireAfterAccessValue`, `SoftValue`, and `WeakValue` are value wrappers.
- `PlayerMetadataRegistry`, `EntityMetadataRegistry`, `BlockMetadataRegistry`, and `WorldMetadataRegistry` are typed registry facades.

Example:

```java
MetadataKey<Integer> KILLS = MetadataKey.create("kills", Integer.class);

MetadataMap metadata = Metadata.provideForPlayer(player);
metadata.put(KILLS, metadata.getOrDefault(KILLS, 0) + 1);

int kills = metadata.getOrDefault(KILLS, 0);
```

Lookups:

```java
Map<Player, Integer> playersWithKills = Metadata.lookupPlayersWithKey(KILLS);
```

Player metadata is removed on quit. Registry cleanup also runs periodically.

## Persistent Data Containers

Package: `dev.willram.ramcore.pdc`

Primary types:

- `PDCs` simplifies `PersistentDataHolder` get/set/has calls using RamCore namespaced keys.
- `DataType` exposes a large set of `PersistentDataType` constants and factories for Bukkit types, primitive arrays, collections, maps, enums, and serializable values.

Example:

```java
PDCs.set(itemMeta, "coins", DataType.INTEGER, 10);
Integer coins = PDCs.get(itemMeta, "coins", DataType.INTEGER);
boolean hasCoins = PDCs.has(itemMeta, "coins");
```

Collection and enum data types:

```java
PersistentDataType<?, List<String>> stringList = DataType.asList(DataType.STRING);
PersistentDataType<String, GameMode> gameMode = DataType.asEnum(GameMode.class);
```

## Items And GUIs

Packages: `dev.willram.ramcore.item`, `dev.willram.ramcore.menu`

Primary types:

- `ItemStackBuilder` fluently builds item stacks and clickable GUI items.
- `Gui` is an abstract inventory GUI base bound to a player.
- `Item` is an immutable GUI item with click handlers.

Item example:

```java
ItemStack stack = ItemStackBuilder.of(Material.DIAMOND)
        .name("<aqua>Reward")
        .lore("<gray>Click to claim.")
        .amount(3)
        .build();
```

GUI item:

```java
Item reward = ItemStackBuilder.of(Material.EMERALD)
        .name("<green>Claim")
        .build(() -> player.sendRichMessage("<green>Claimed."));
```

GUI implementation:

```java
public final class ExampleGui extends Gui {
    public ExampleGui(Player player) {
        super(player, 3, "<green>Example");
    }

    @Override
    public void redraw() {
        setItem(13, ItemStackBuilder.of(Material.EMERALD)
                .name("<green>Claim")
                .build(() -> getPlayer().sendRichMessage("<green>Claimed.")));
    }

    @Override
    public boolean clickHandler(InventoryClickEvent event) {
        return false;
    }

    @Override
    public void closeHandler(InventoryCloseEvent event) {
    }

    @Override
    public void invalidateHandler() {
    }
}
```

Call `open()` to show a GUI. The GUI invalidates itself when the player dies, quits, changes worlds, teleports away, opens another inventory, or closes it.

## Scoreboards

Package: `dev.willram.ramcore.scoreboard`

RamCore scoreboards are packet-backed scoreboard abstractions.

Primary types:

- `ScoreboardProvider` exposes a scoreboard instance.
- `PacketScoreboardProvider` is the packet scoreboard provider implementation.
- `Scoreboard` creates, retrieves, and removes teams/objectives, including per-player variants.
- `ScoreboardObjective` manages display name, display slot, scores, lines, and subscriptions.
- `ScoreboardTeam` manages display name, prefix, suffix, color, collision, visibility, members, and subscriptions.

Example:

```java
Scoreboard scoreboard = new PacketScoreboardProvider().getScoreboard();
ScoreboardObjective sidebar = scoreboard.createObjective("stats", "<green>Stats", DisplaySlot.SIDEBAR);
sidebar.applyLines("<gray>Coins: <white>10", "<gray>Kills: <white>2");
sidebar.subscribe(player);
```

## Cooldowns

Package: `dev.willram.ramcore.cooldown`

Primary types:

- `Cooldown` represents a single timeout.
- `CooldownMap<T>` stores self-populating cooldowns per key.
- `ComposedCooldownMap<I, O>` maps input objects to stable internal keys.

Examples:

```java
Cooldown cooldown = Cooldown.of(5, TimeUnit.SECONDS);
if (!cooldown.test()) {
    player.sendRichMessage("<red>Wait " + cooldown.remainingTime(TimeUnit.SECONDS) + "s.");
}
```

Per-player:

```java
ComposedCooldownMap<Player, UUID> cooldowns =
        ComposedCooldownMap.create(Cooldown.ofTicks(100), Player::getUniqueId);

if (cooldowns.test(player)) {
    castSpell(player);
}
```

## Data Trees, JSON, And Configs

Packages: `dev.willram.ramcore.datatree`, `dev.willram.ramcore.gson`, `dev.willram.ramcore.config`

Primary types:

- `DataTree` reads nested JSON or Configurate data with path resolution.
- `GsonDataTree` and `ConfigurateDataTree` are concrete tree implementations.
- `GsonProvider` exposes configured standard and pretty-print Gson instances.
- `GsonSerializable` is a reflection-based JSON serialization contract.
- `JsonBuilder` builds JSON objects and arrays.
- `GsonConverter`, `MutableGsonConverter`, `ImmutableGsonConverter`, and `GsonConverters` support converter-based serialization.
- `Configs` exposes shared Configurate serializers, currently including `Location`.

Data tree example:

```java
JsonObject object = GsonProvider.readObject(jsonString);
DataTree tree = DataTree.from(object);

String name = tree.resolve("profile", "name").asString();
int level = tree.resolve("profile", "level").asInt();
```

GsonSerializable contract:

```java
public final class ExampleValue implements GsonSerializable {
    public static ExampleValue deserialize(JsonElement element) {
        return new ExampleValue(element.getAsJsonObject().get("name").getAsString());
    }

    @Override
    public JsonElement serialize() {
        JsonObject object = new JsonObject();
        object.addProperty("name", this.name);
        return object;
    }
}
```

Use `GsonProvider.standard()` for normal serialization and `GsonProvider.prettyPrinting()` for human-readable output.

## Repositories And Data Items

Package: `dev.willram.ramcore.data`

Primary types:

- `DataRepository<K, V extends DataItem>` is a simple keyed in-memory registry with abstract `setup()` and `saveAll()`.
- `DataItem` tracks transient saving state and whether the item should be saved.
- `NamespacedKeys` creates RamCore namespaced keys.

This package is intentionally minimal. Plugins supply persistence behavior.

## Serialization Models

Package: `dev.willram.ramcore.serialize`

Primary types:

- `Point`, `VectorPoint`, `Position`, `BlockPosition`, and `ChunkPosition` represent coordinates.
- `Region`, `BlockRegion`, `ChunkRegion`, and `CircularRegion` represent spatial bounds.
- `Direction` represents orientation.
- `Base64Util`, `InventorySerialization`, `Serializers`, and `VectorSerializers` provide serialization helpers.

These types generally implement `GsonSerializable` and are meant for config or data storage.

Example:

```java
BlockPosition pos = BlockPosition.of(block);
JsonElement json = pos.serialize();
BlockPosition copy = BlockPosition.deserialize(json);
```

## NBT And Reflection

Packages: `dev.willram.ramcore.nbt`, `dev.willram.ramcore.reflect`, `dev.willram.ramcore.shadows`

Primary types:

- `NBT` and `NBTTagType` expose NBT helpers and tag classification.
- `MinecraftVersion`, `MinecraftVersions`, `NmsVersion`, `SnapshotVersion`, and `ServerReflection` handle version parsing and versioned class names.
- `Proxies` helps create reflection/shadow proxies.
- `dev.willram.ramcore.shadows.nbt.*` contains shadow interfaces for NMS NBT types.

These APIs are advanced and version-sensitive. Prefer Bukkit/Paper APIs where available.

Example:

```java
MinecraftVersion runtime = MinecraftVersion.getRuntimeVersion();
if (runtime.isAfterOrEq(MinecraftVersions.v1_20)) {
    // version-specific behavior
}
```

## Buckets And Cycles

Package: `dev.willram.ramcore.bucket`

Primary types:

- `Bucket<E>` is a `Set<E>` split into deterministic partitions.
- `BucketPartition<E>` represents one partition.
- `Cycle<E>` cycles through a fixed list indefinitely.
- `BucketFactory` creates hash, synchronized, concurrent, or supplied-set buckets.
- `PartitioningStrategy<T>`, `GenericPartitioningStrategy`, and `PartitioningStrategies` decide partition placement.

Example:

```java
Bucket<Player> bucket = BucketFactory.newConcurrentBucket(20, PartitioningStrategies.hashCode());
bucket.addAll(Bukkit.getOnlinePlayers());

BucketPartition<Player> nextPartition = bucket.asCycle().next();
nextPartition.forEach(this::tickPlayer);
```

Use buckets to spread expensive repeated work across ticks.

## Random Values

Package: `dev.willram.ramcore.random`

Primary types:

- `RandomSelector<E>` picks values uniformly or by weight.
- `Weighted` and `Weigher<E>` define weighted values.
- `WeightedObject<T>` wraps arbitrary objects with weights.
- `VariableAmount` produces fixed, ranged, variance, addition, and optional random amounts.

Examples:

```java
RandomSelector<WeightedObject<String>> selector = RandomSelector.weighted(List.of(
        WeightedObject.of("common", 10),
        WeightedObject.of("rare", 1)
));

String value = selector.pick().get();
int amount = VariableAmount.range(2, 8).getFlooredAmount();
```

## Cache Helpers

Package: `dev.willram.ramcore.cache`

Primary types:

- `Lazy<T>` memoizes a supplier until invalidated.
- `Cache<T>` stores a manually updated or lazily supplied value.
- `Expiring<T>` stores a value that refreshes after a duration.

Use these for local plugin state, not durable persistence.

## Time And Formatting

Package: `dev.willram.ramcore.time`

Primary types:

- `Time` exposes time utilities.
- `DurationParser` parses human-readable durations into `Duration`.
- `DurationFormatter` formats durations.

Example:

```java
Duration duration = DurationParser.parseSafely("5m").orElse(Duration.ofSeconds(30));
```

## Utility Packages

Packages: `dev.willram.ramcore.utils`, `dev.willram.ramcore.functions`, `dev.willram.ramcore.interfaces`

Notable types:

- `Numbers` parses numeric strings into optional values.
- `Delegates` adapts between common functional interfaces.
- `Delegate<T>` exposes `getDelegate()`.
- `TypeAware<T>` exposes a Guava `TypeToken<T>`.
- `Indexing`, `Formatter`, `Maths`, `TxtUtils`, `ItemUtils`, `LoaderUtils`, and `VBlockFace` provide miscellaneous utility behavior.

Use these directly when they fit; avoid building new behavior on `LoaderUtils` unless you need access to the loaded RamCore plugin instance.

## Exceptions

Package: `dev.willram.ramcore.exception`

Primary types:

- `RamExceptions` reports scheduler, event handler, and promise chain failures.
- `InternalException` is the base runtime exception for RamCore internal wrappers.
- `EventHandlerException`, `PromiseChainException`, and `SchedulerTaskException` represent subsystem failures.
- `RamExceptionEvent` is fired when RamCore reports an exception.

Most plugin code should not need to construct these directly; subscribe to `RamExceptionEvent` if you want centralized reporting.

## Kotlin Extensions

Package: `dev.willram.ramcore.kotlin`

Top-level helpers include:

- `ramTypeToken<T>()`
- `subscribe<T>()`
- `merge<T>()`
- `metadataKey<T>(id)`
- `MetadataMap.value(...)`
- `MetadataMap[key] = value`
- `command("name") { ... }`
- `Commands.register(...)`
- command DSL `literal {}` and `argument {}` helpers
- `CommandContext[arg]`
- `Entity.taskContext()`, `Location.taskContext()`, `Block.taskContext()`, `BlockState.taskContext()`, `Chunk.taskContext()`, and `World.chunkTaskContext(...)`

Example:

```kotlin
private val kills = metadataKey<Int>("kills")

Metadata.provideForPlayer(player)[kills] = 5

val spec = command("hello") {
    executes { ctx -> ctx.msg("<green>Hello.") }
}
```

## Package Map

| Package | Purpose |
| --- | --- |
| `dev.willram.ramcore` | Plugin base and RamCore plugin entry point. |
| `commands` | Brigadier-first command API. |
| `scheduler` | Paper/Folia-aware scheduling and task contexts. |
| `promise` | Thread-aware promise/future abstraction. |
| `event` | Functional Bukkit and ProtocolLib event subscriptions. |
| `terminable` | Resource lifecycle and cleanup ownership. |
| `metadata` | In-memory typed metadata on players/entities/blocks/worlds. |
| `pdc` | PersistentDataContainer helpers and data types. |
| `item`, `menu` | ItemStack builder and inventory GUI primitives. |
| `scoreboard` | Packet scoreboard teams and objectives. |
| `cooldown` | Single and mapped cooldowns. |
| `datatree`, `gson`, `config` | JSON, Configurate, and structured data utilities. |
| `serialize` | Serializable coordinate, position, region, and inventory utilities. |
| `data` | Simple repository/data item base types. |
| `bucket` | Partitioned sets and cycles for spreading work. |
| `random` | Weighted selection and variable amounts. |
| `cache` | Lazy and expiring local value containers. |
| `time` | Time, duration parsing, and formatting. |
| `reflect`, `nbt`, `shadows` | Advanced versioned reflection and NBT helpers. |
| `utils`, `functions`, `interfaces` | General helper types. |
| `exception` | Exception reporting and wrapper types. |

## API Stability Notes

- Prefer facade classes (`RamCommands`, `Schedulers`, `Events`, `Metadata`, `GsonProvider`, `PDCs`) over implementation classes.
- Types under `shadows`, `reflect`, and `nbt` are public but advanced and sensitive to Minecraft server changes.
- Commands should be registered only through Paper's lifecycle registrar, normally via `RamPlugin#registerCommands`.
- Scheduler calls that touch world, entity, block, or chunk state should use entity/region/chunk contexts rather than raw async work.
- Bind `Task`, subscriptions, and other `AutoCloseable` resources to a `RamPlugin` or `CompositeTerminable`.
