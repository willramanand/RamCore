# RamCore Public API

This document describes the public interfaces intended for plugin authors. It is organized by subsystem instead of raw package order, because most RamCore APIs are meant to be used as workflows.

Unless stated otherwise, examples assume:

```java
import dev.willram.ramcore.*;
import dev.willram.ramcore.commands.*;
import dev.willram.ramcore.content.*;
import dev.willram.ramcore.display.*;
import dev.willram.ramcore.encounter.*;
import dev.willram.ramcore.integration.*;
import dev.willram.ramcore.message.*;
import dev.willram.ramcore.npc.*;
import dev.willram.ramcore.objective.*;
import dev.willram.ramcore.party.*;
import dev.willram.ramcore.permission.*;
import dev.willram.ramcore.presentation.*;
import dev.willram.ramcore.region.*;
import dev.willram.ramcore.reward.*;
import dev.willram.ramcore.scheduler.*;
import dev.willram.ramcore.service.*;
import dev.willram.ramcore.template.*;
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
- `services()` exposes the plugin service registry.
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

## Services

Package: `dev.willram.ramcore.service`

RamCore services are typed, dependency-aware lifecycle units. Register services during `RamPlugin#load()`. RamCore loads registered services after `load()`, enables them before `enable()`, and disables them after plugin-specific `disable()` in reverse dependency order.

Primary types:

- `ServiceRegistry` registers services, resolves dependencies, and runs lifecycle phases.
- `ServiceKey<T>` is a typed service id.
- `ServiceRegistration<T>` configures dependencies.
- `Service` exposes optional `load`, `enable`, and `disable` callbacks.
- `ServiceContext` provides service lookup and `TerminableConsumer` binding during callbacks.

Example:

```java
public final class ExamplePlugin extends RamPlugin {
    private static final ServiceKey<ConfigService> CONFIG =
            ServiceKey.of("config", ConfigService.class);
    private static final ServiceKey<MessageService> MESSAGES =
            ServiceKey.of("messages", MessageService.class);

    @Override
    public void load() {
        services().register(CONFIG, new ConfigService(this));
        services().register(MESSAGES, new MessageService()).dependsOn(CONFIG);
    }

    @Override
    public void enable() {
        services().require(MESSAGES).announceStartup();
    }

    @Override
    public void disable() {
    }

    @Override
    public void registerCommands(Commands commands) {
    }
}
```

Dependency order:

```java
services().register(MESSAGES, new MessageService()).dependsOn(CONFIG);
```

`CONFIG` loads and enables before `MESSAGES`. `MESSAGES` disables before `CONFIG`. Missing or cyclic dependencies fail during service load.

## Messages

Package: `dev.willram.ramcore.message`

RamCore messages use Adventure `Component` output and MiniMessage templates. Use `MessageKey` constants for reusable message ids, then render them through a `MessageCatalog`.

Primary types:

- `MessageKey` declares a reusable id and fallback template.
- `MessageCatalog` renders templates with optional prefix and sends to Adventure audiences.
- `MessagePlaceholders` creates parsed, unparsed, component, and map-backed MiniMessage placeholders.

Example:

```java
private static final MessageKey WELCOME =
        MessageKey.of("welcome", "<green>Welcome, <player>!");

private final MessageCatalog messages = MessageCatalog.builder()
        .prefix("<gold>[Example]</gold> ")
        .message(WELCOME, "<green>Hello, <player>.")
        .build();

public void greet(Player player) {
    messages.send(player, WELCOME, MessagePlaceholders.parsed("player", player.getName()));
}
```

Use `renderRaw(...)` when no prefix should be applied, such as inventory titles or scoreboard lines.

## Content Registries

Package: `dev.willram.ramcore.content`

RamCore content registries provide typed, namespaced, owner-tracked lookup for code-defined content. Config-backed content can build on this later.

Primary types:

- `ContentId` is a `namespace:value` identifier.
- `ContentKey<T>` combines id and value type.
- `ContentEntry<T>` stores key, owner, and value.
- `ContentRegistry<T>` registers values, detects conflicts, tracks owners, and supports cleanup by owner.

Example:

```java
ContentRegistry<ItemTemplate> items = ContentRegistry.create(ItemTemplate.class);
ContentKey<ItemTemplate> fireSword =
        ContentKey.of("example", "fire_sword", ItemTemplate.class);

items.register(getName(), fireSword, template);

ItemTemplate template = items.require(ContentId.parse("example:fire_sword"));
```

Duplicate ids fail fast. Use `unregisterOwner(owner)` during reloads or module shutdown.

## Templates

Package: `dev.willram.ramcore.template`

RamCore templates provide typed reusable definitions with optional parent inheritance. Each content type supplies its own merge logic through `TemplateComposer<T>`.

Primary types:

- `Template<T>` stores key, optional parent id, and value.
- `TemplateComposer<T>` merges parent values with child overrides.
- `TemplateRegistry<T>` registers, validates, resolves, and owner-cleans templates.
- `TemplateValidationException` reports missing parents and inheritance cycles.

Example:

```java
TemplateRegistry<ItemTemplate> templates =
        TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);

ContentKey<ItemTemplate> base = ContentKey.of("example", "base_sword", ItemTemplate.class);
ContentKey<ItemTemplate> fire = ContentKey.of("example", "fire_sword", ItemTemplate.class);

templates.register(getName(), Template.of(base, baseTemplate));
templates.register(getName(), Template.extending(fire, base.id(), fireOverrides));

ItemTemplate resolved = templates.resolve(fire.id());
```

Call `validate()` during reload/startup to fail fast before gameplay code resolves templates.

## Regions And Rules

Package: `dev.willram.ramcore.region`

RamCore regions provide lightweight shape containment and priority-based rule evaluation independent of WorldGuard.

Primary types:

- `RegionShape` tests whether a `Position` is inside a region.
- `RegionShapes` creates cuboid, sphere, any, and all shapes.
- `RuleRegion` combines a shape, region priority, and rules.
- `RegionRule` evaluates one action category and returns `ALLOW`, `DENY`, or `PASS`.
- `RegionRuleEngine` registers regions and evaluates highest-priority matching rules.

Example:

```java
RegionRuleEngine engine = new RegionRuleEngine();
RuleRegion spawn = RuleRegion.builder(ContentId.parse("example:spawn"), RegionShapes.cuboid(region))
        .priority(10)
        .rule(RegionRule.of("deny-block", RegionAction.BLOCK, 0, RegionRuleResult.DENY))
        .build();

engine.register(getName(), spawn);

RegionDecision decision = engine.evaluate(RegionQuery.of(position, RegionAction.BLOCK));
if (decision.denied()) {
    // cancel block action
}
```

Rules can be conditional with `rule.when(query -> ...)`. Region priority is applied before rule priority when regions overlap.

## Rewards

Package: `dev.willram.ramcore.reward`

RamCore rewards provide a generic validation, preview, and execution pipeline separate from loot generation.

Primary types:

- `RewardContext` carries scope, subject, and metadata.
- `RewardAction` executes one reward and can validate prerequisites.
- `RewardEntry` defines guaranteed or weighted reward entries with optional conditions.
- `RewardPlan` groups guaranteed rewards and weighted rolls.
- `RewardEngine` validates, previews, and executes plans.
- `RewardReport` returns outcomes and validation errors.

Example:

```java
RewardPlan plan = RewardPlan.builder()
        .guaranteed(RewardEntry.guaranteed("message", ctx -> {
            player.sendRichMessage("<green>Quest complete.");
            return RewardOutcome.success("message");
        }))
        .weighted(RewardEntry.weighted("bonus_item", giveBonusItem, 1))
        .rolls(1)
        .build();

RewardReport preview = engine.preview(plan, RewardContext.of("quest"), random);
RewardReport result = engine.execute(plan, RewardContext.of("quest"), random);
```

Preview selects rewards without applying actions. Execution stops before side effects if validation returns errors.

## Presentation

Package: `dev.willram.ramcore.presentation`

RamCore presentation effects wrap Adventure output behind reusable, terminable effects.

Primary types:

- `PresentationContext` targets one or more audiences and carries scheduler anchor.
- `PresentationEffect` plays an effect and returns a `Terminable` for cleanup.
- `PresentationEffects` provides message, action bar, title, sound, boss bar, custom, and sequence effects.

Example:

```java
PresentationContext context = PresentationContext.of(player);

PresentationEffects.sequence()
        .then(PresentationEffects.message(Component.text("Wave started.")))
        .then(PresentationEffects.actionBar(Component.text("Defend the gate.")), 20L)
        .play(context)
        .bindWith(this);
```

Boss-bar effects hide the bar when their returned `Terminable` closes. Delayed sequence steps run through `Schedulers` using the context `TaskContext`.

## Displays And Holograms

Package: `dev.willram.ramcore.display`

RamCore display helpers configure Bukkit display entities and spawn them through region-aware scheduler calls.

Primary types:

- `TextDisplaySpec`, `ItemDisplaySpec`, and `BlockDisplaySpec` configure display entities.
- `DisplayOptions` applies shared settings such as billboard, brightness, view range, shadows, size, and interpolation.
- `DisplaySpawner` spawns one configured display and returns a `DisplayHandle`.
- `HologramSpec` defines stacked text display lines.
- `Holograms` spawns hologram stacks and returns a terminable `Hologram`.

Example:

```java
TextDisplaySpec label = TextDisplaySpec.text(Component.text("Objective"))
        .shadowed(true)
        .alignment(TextDisplay.TextAlignment.CENTER);
label.options().billboard(Display.Billboard.CENTER).viewRange(32f);

DisplaySpawner.spawn(location, label)
        .thenAcceptSync(handle -> bind(handle));

HologramSpec hologram = HologramSpec.create()
        .text(Component.text("Boss"))
        .text(Component.text("Phase 2"));

Holograms.spawn(location, hologram)
        .thenAcceptSync(instance -> bind(instance));
```

`DisplayHandle#close()` and `Hologram#close()` remove spawned display entities through their entity schedulers.

## NPCs

Package: `dev.willram.ramcore.npc`

RamCore NPC helpers manage server-backed Bukkit entities first. Packet-only fake NPCs can build on this later without making simple static NPCs depend on ProtocolLib or NMS.

Primary types:

- `NpcSpec` configures a spawned entity with nameplate, visibility, invulnerability, gravity, AI, persistence, and click behavior.
- `NpcSpawner` spawns one configured NPC through a region-aware scheduler call and returns an `NpcHandle`.
- `NpcHandle` owns the entity, supports teleport, look-at, per-player show/hide, click dispatch, and cleanup.
- `NpcRegistry` tracks owned handles and can register Bukkit interact/attack listeners through `NpcRegistry.create(plugin)`.
- `Npcs` is the small facade for specs, spawning, and registries.

Example:

```java
NpcRegistry npcs = Npcs.registry(this);
bind(npcs);

NpcSpec<Villager> guide = Npcs.spec(Villager.class)
        .name(Component.text("Guide"))
        .nameVisible(true)
        .ai(false)
        .invulnerable(true)
        .onClick(ctx -> messages.send(ctx.player(), GUIDE_GREETING));

Npcs.spawn(location, guide).thenAcceptSync(handle -> {
    npcs.register(getName(), handle);
    bind(handle);
});
```

Use `handle.hideFrom(plugin, player)`, `handle.showTo(plugin, player)`, and `handle.visibleTo(player)` for per-player visibility. `handle.lookAt(player)` rotates the NPC toward a player's eye location through the entity scheduler.

## Parties And Groups

Package: `dev.willram.ramcore.party`

RamCore parties are in-memory, UUID-based groups. Persistence, commands, chat formatting, and UI are intentionally left to consuming plugins.

Primary types:

- `PartyManager` creates parties, tracks one-party-per-player membership, manages invites, leave/kick/promote/disband actions, and applies membership rules.
- `PartyGroup` exposes leader, members, roles, pending invites, shared metadata, contribution tracking, and reward context.
- `PartyOptions` configures max size, invite TTL, and leader-leave behavior.
- `PartyMembershipRule` allows plugin-defined join eligibility.
- `PartyContributionTracker` records boss/event damage or other shared eligibility scores.
- `PartyTeleport` schedules member teleports through player schedulers.
- `PartyChatHandler` and `PartyChatContext` are small hooks for plugin-defined group chat.

Example:

```java
PartyManager parties = Parties.manager(
        Parties.options().maxMembers(4).disbandWhenLeaderLeaves(false)
);

PartyGroup party = parties.createParty(leaderId).value();
parties.invite(leaderId, friendId);
parties.accept(party.id(), friendId);

party.contributions().add(friendId, 42.0d);
Set<UUID> eligible = party.contributions().eligible(10.0d);

RewardContext context = party.rewardContext("dungeon_reward");
```

Custom join rule:

```java
parties.rule((party, playerId) -> bannedFromDungeon(playerId)
        ? PartyResult.failure("player is not eligible")
        : PartyResult.ok());
```

Teleport online members:

```java
PartyTeleport.teleport(party, Bukkit::getPlayer, destination);
```

## Objectives And Quest Progress

Package: `dev.willram.ramcore.objective`

RamCore objectives provide storage-neutral progress tracking for achievements, tutorials, quests, daily tasks, battle passes, dungeons, and party goals. Bukkit listeners, commands, regions, NPCs, and other systems feed generic `ObjectiveEvent` values into an `ObjectiveTracker`.

Primary types:

- `ObjectiveDefinition` describes one objective by `ContentId`, tasks, hidden state, and whether tasks must progress in order.
- `ObjectiveTask` defines one measurable step: action, target id, required amount, and hidden state.
- `ObjectiveSubject` scopes progress to a player, party, global id, or custom subject.
- `ObjectiveEvent` is the generic input used to advance progress.
- `ObjectiveProgress` exposes current per-task values and completion state.
- `ObjectiveUpdate` describes one task advancement and is emitted to `ObjectiveProgressListener` hooks.
- `Objectives` is the facade for trackers, definitions, and tasks.

Example:

```java
ObjectiveDefinition quest = Objectives.objective(ContentId.parse("quest:first_steps"))
        .chained(true)
        .task(Objectives.task("enter_spawn", ObjectiveAction.ENTER_REGION, "example:spawn", 1))
        .task(Objectives.task("talk_guide", ObjectiveAction.INTERACT_ENTITY, "example:guide", 1))
        .build();

ObjectiveTracker tracker = Objectives.tracker()
        .register(quest)
        .listener(update -> {
            if (update.objectiveCompleted()) {
                // reward or announce completion
            }
        });

ObjectiveSubject subject = ObjectiveSubject.player(player.getUniqueId());
tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.ENTER_REGION, "example:spawn"));
tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.INTERACT_ENTITY, "example:guide"));
```

Use `ObjectiveSubject.party(party.id())` for shared party progress. Use target `*` on a task to match any target for that action, such as "kill any mob". `tracker.reset(subject, objectiveId)` clears one objective, and `tracker.resetSubject(subject)` clears all progress for that subject.

## Bosses And Encounters

Package: `dev.willram.ramcore.encounter`

RamCore encounters are engine-level boss runs. They do not spawn mobs directly; plugin code wires Bukkit/NMS entities, regions, boss bars, rewards, and presentation effects to the encounter instance.

Primary types:

- `EncounterDefinition` describes a boss encounter, max health, phases, enrage timer, optional arena, and optional reward plan.
- `EncounterPhase` selects behavior by remaining health percentage and owns timed abilities.
- `EncounterAbility` schedules repeated ability callbacks by tick interval and initial delay.
- `EncounterInstance` tracks runtime state, health, current phase, elapsed ticks, contributors, wipe/reset/complete state, arena checks, and reward context.
- `EncounterRegistry` stores definitions and creates configured instances.
- `EncounterUpdate` and `EncounterListener` provide lifecycle hooks for boss bars, announcements, rewards, and diagnostics.
- `Encounters` is the facade for registries, definitions, phases, and abilities.

Example:

```java
EncounterDefinition boss = Encounters.encounter(ContentId.parse("dungeon:warden"), 500.0d)
        .enrageAfterTicks(20L * 60L * 5L)
        .arena(RegionShapes.cuboid(arenaRegion))
        .phase(Encounters.phase("main", 1.0d)
                .ability(Encounters.ability("slam", 100L).action((encounter, ability) -> castSlam())))
        .phase(Encounters.phase("burn", 0.35d)
                .ability(Encounters.ability("meteor", 60L).initialDelay(20L).action((encounter, ability) -> castMeteor())))
        .build();

EncounterInstance run = Encounters.registry(update -> {
            if (update.signal() == EncounterSignal.COMPLETE) {
                rewards.execute(rewardPlan, update.encounter().rewardContext("boss"), random);
            }
        })
        .register(boss)
        .create(boss.id());

run.start();
run.tick();
run.damage(player.getUniqueId(), damage);
```

Use `run.withinArena(position)` for arena-bound reset checks, `run.contributions()` for boss reward eligibility, and `run.wipe(reason)` or `run.reset(reason)` when consuming plugin logic detects a wipe/reset condition.

## Optional Integrations

Package: `dev.willram.ramcore.integration`

RamCore integrations expose capability checks without making core modules depend on optional plugins. Most built-in providers detect Bukkit plugin presence and report declared capabilities; deeper adapters can be layered behind those checks.

Primary types:

- `IntegrationRegistry` stores providers, lists snapshots, and answers capability availability.
- `IntegrationProvider` exposes one integration descriptor and current runtime snapshot.
- `IntegrationDescriptor` declares id, plugin name, capabilities, and description.
- `IntegrationCapability` describes supported surfaces such as permissions, economy, placeholders, regions, packets, NPCs, and custom items.
- `PluginDetector` abstracts plugin lookup so detection is testable without Bukkit.
- `StandardIntegrations` defines descriptors for LuckPerms, Vault, PlaceholderAPI, MiniPlaceholders, WorldGuard, ProtocolLib, Citizens, ItemsAdder, and Oraxen.
- `ProtocolLibIntegrationProvider` exposes a guarded `ProtocolManager` accessor when ProtocolLib is available.
- `Integrations` is the facade for custom or standard registries.

Example:

```java
IntegrationRegistry integrations = Integrations.standard();

if (integrations.supports(IntegrationCapability.ECONOMY)) {
    // enable economy-backed rewards
}

IntegrationSnapshot vault = integrations.require(StandardIntegrations.VAULT.id()).snapshot();
if (!vault.available()) {
    getLogger().info("Vault unavailable: " + vault.message());
}
```

Custom provider:

```java
IntegrationDescriptor descriptor = new IntegrationDescriptor(
        IntegrationId.of("example"),
        "ExamplePlugin",
        EnumSet.of(IntegrationCapability.CUSTOM_ITEMS),
        "Example custom item bridge"
);

integrations.register(new DetectedIntegrationProvider(descriptor, new BukkitPluginDetector()));
```

Use `Integrations.standard(detector)` in tests or custom bootstrap code when Bukkit's plugin manager should not be accessed directly.

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
- `CommandCooldown` adds per-sender or custom-key cooldown guards.
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

### Command Cooldowns

Attach a per-sender cooldown to an executable node:

```java
RamCommands.command("kit")
        .literal("claim", claim -> claim
                .cooldown(Cooldown.of(30, TimeUnit.SECONDS))
                .executes(ctx -> {
                    // give kit
                }));
```

Use custom keys and messages when cooldown scope should not be per sender:

```java
CommandCooldown cooldown = CommandCooldown
        .keyed(Cooldown.of(5, TimeUnit.MINUTES), ctx -> ctx.sender().getName() + ":daily")
        .message((ctx, remainingMillis) -> "<red>Daily reward is still cooling down.");
```

Cooldowns run before command execution. For `executesAsync(...)`, cooldown rejection happens before async work is scheduled.

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

Typed permission helpers:

```java
private static final PermissionNode ADMIN =
        Permissions.node("example.admin", "<red>Staff only.");

private static final PermissionNode RELOAD =
        ADMIN.child("reload");

private static final PermissionRequirement STAFF_OR_RELOAD =
        Permissions.any(ADMIN, RELOAD);

RamCommands.command("example")
        .permissions(STAFF_OR_RELOAD)
        .executes(ctx -> ctx.requirePermissions(STAFF_OR_RELOAD));
```

Use `PermissionRequirement.all(...)` when every node is required and `PermissionRequirement.any(...)` when any one node is enough.

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

Use explicit scheduler anchors when passing scheduling intent around:

```java
TaskContext context = TaskContext.player(player);
Schedulers.forContext(context).run(() -> updatePlayerState(player));

TaskContext asyncContext = TaskContext.async();
Schedulers.forContext(asyncContext).call(this::loadFromDisk);
```

`TaskContext#description()` gives concise diagnostics such as `global`, `async`, `entity:<uuid>`, `region:<world>@x,y,z`, or `chunk:<world>@x,z`.

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

- `BukkitConfig` loads typed Bukkit YAML configs with defaults, reloads, and validation.
- `TypedConfig` is the loaded typed config view.
- `ConfigKey<T>` declares a typed path, default or required value, and validators.
- `ConfigValidationException` reports all invalid config values together.
- `DataTree` reads nested JSON or Configurate data with path resolution.
- `GsonDataTree` and `ConfigurateDataTree` are concrete tree implementations.
- `GsonProvider` exposes configured standard and pretty-print Gson instances.
- `GsonSerializable` is a reflection-based JSON serialization contract.
- `JsonBuilder` builds JSON objects and arrays.
- `GsonConverter`, `MutableGsonConverter`, `ImmutableGsonConverter`, and `GsonConverters` support converter-based serialization.
- `Configs` exposes shared Configurate serializers, currently including `Location`.

Typed YAML example:

```java
private static final ConfigKey<String> PREFIX =
        ConfigKey.of("messages.prefix", String.class, "<gold>[Example]</gold>");

private static final ConfigKey<Integer> MAX_RETRIES =
        ConfigKey.of("startup.max-retries", Integer.class, 3)
                .validate(value -> value >= 0, "must be >= 0");

private TypedConfig config;

@Override
public void load() {
    this.config = BukkitConfig.load(getDataFolder().toPath().resolve("config.yml"), PREFIX, MAX_RETRIES);
}

@Override
public void enable() {
    String prefix = this.config.get(PREFIX);
    int retries = this.config.get(MAX_RETRIES);
}
```

Use `config.reload()` after editing the file. Reload reapplies defaults and throws `ConfigValidationException` if required values are missing or validators fail.

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

- `ApiMisuseException` reports invalid plugin usage of RamCore APIs with a problem and fix.
- `RamPreconditions` provides fail-fast validation helpers for public APIs.
- `RamExceptions` reports scheduler, event handler, and promise chain failures.
- `InternalException` is the base runtime exception for RamCore internal wrappers.
- `EventHandlerException`, `PromiseChainException`, and `SchedulerTaskException` represent subsystem failures.
- `RamExceptionEvent` is fired when RamCore reports an exception.

Most plugin code should not need to construct these directly; subscribe to `RamExceptionEvent` if you want centralized reporting. `ApiMisuseException` is intended to fail fast during development with a direct fix message.

## Kotlin Extensions

Package: `dev.willram.ramcore.kotlin`

Top-level helpers include:

- `ramTypeToken<T>()`
- `configKey<T>(path, defaultValue)`
- `requiredConfigKey<T>(path)`
- `bukkitConfig(path, vararg keys)`
- `subscribe<T>()`
- `merge<T>()`
- `metadataKey<T>(id)`
- `messageKey(id, defaultTemplate)`
- `messageCatalog { ... }`
- `parsedPlaceholder(...)`, `unparsedPlaceholder(...)`, and `componentPlaceholder(...)`
- `permission(...)`, `permissionsAll(...)`, and `permissionsAny(...)`
- `MetadataMap.value(...)`
- `MetadataMap[key] = value`
- `command("name") { ... }`
- `Commands.register(...)`
- command DSL `literal {}` and `argument {}` helpers
- command cooldown helpers `cooldown(amount, unit)`, `cooldownTicks(ticks)`, and `cooldown(cooldown) { ctx -> key }`
- `CommandContext[arg]`
- `Entity.taskContext()`, `Location.taskContext()`, `Block.taskContext()`, `BlockState.taskContext()`, `Chunk.taskContext()`, and `World.chunkTaskContext(...)`
- NPC helpers `npcSpec<T> { ... }`, `npcRegistry(plugin)`, and `Location.spawnNpc(spec)`
- party helpers `partyOptions()`, `partyManager()`, and `partyManager(options)`
- objective helpers `objectiveTracker()`, `objective(id) { ... }`, and `objectiveTask(...)`
- encounter helpers `encounterRegistry()`, `encounter(id, maxHealth) { ... }`, `encounterPhase(...)`, and `encounterAbility(...)`
- integration helpers `integrationRegistry()`, `standardIntegrations()`, and `standardIntegrations(detector)`

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
| `content` | Typed namespaced content registries with owner tracking. |
| `display` | Display entity specs, region-safe spawning, and hologram stacks. |
| `encounter` | Boss encounter definitions, phases, timed abilities, enrage/wipe/reset state, and contribution tracking. |
| `integration` | Optional plugin capability detection for LuckPerms, Vault, placeholders, WorldGuard, ProtocolLib, Citizens, ItemsAdder, and Oraxen. |
| `npc` | Server-backed NPC specs, click dispatch, visibility, and cleanup. |
| `objective` | Storage-neutral objective definitions, subject progress, chained tasks, and progress events. |
| `party` | In-memory party/group membership, invites, rules, contribution tracking, and teleport helpers. |
| `service` | Typed service registry and dependency-aware lifecycle. |
| `template` | Typed reusable templates with parent inheritance and validation. |
| `message` | MiniMessage catalog, reusable keys, prefixes, and placeholders. |
| `permission` | Typed permission nodes, grouped checks, and command requirements. |
| `presentation` | Adventure message, title, sound, boss-bar, and sequence effects. |
| `region` | Lightweight regions and priority rule evaluation. |
| `reward` | Generic reward validation, preview, and execution pipeline. |
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
