# RamCore Public API

This document describes the public interfaces intended for plugin authors. It is organized by subsystem instead of raw package order, because most RamCore APIs are meant to be used as workflows.

Unless stated otherwise, examples assume:

```java
import dev.willram.ramcore.*;
import dev.willram.ramcore.ai.*;
import dev.willram.ramcore.brain.*;
import dev.willram.ramcore.commands.*;
import dev.willram.ramcore.combat.*;
import dev.willram.ramcore.content.*;
import dev.willram.ramcore.display.*;
import dev.willram.ramcore.encounter.*;
import dev.willram.ramcore.entity.*;
import dev.willram.ramcore.integration.*;
import dev.willram.ramcore.loot.*;
import dev.willram.ramcore.message.*;
import dev.willram.ramcore.item.component.*;
import dev.willram.ramcore.item.nbt.*;
import dev.willram.ramcore.nms.api.*;
import dev.willram.ramcore.nms.reflect.*;
import dev.willram.ramcore.npc.*;
import dev.willram.ramcore.objective.*;
import dev.willram.ramcore.party.*;
import dev.willram.ramcore.packet.*;
import dev.willram.ramcore.path.*;
import dev.willram.ramcore.permission.*;
import dev.willram.ramcore.presentation.*;
import dev.willram.ramcore.region.*;
import dev.willram.ramcore.resourcepack.*;
import dev.willram.ramcore.reward.*;
import dev.willram.ramcore.scheduler.*;
import dev.willram.ramcore.selector.*;
import dev.willram.ramcore.service.*;
import dev.willram.ramcore.template.*;
import dev.willram.ramcore.terminable.*;
import dev.willram.ramcore.text.*;
import dev.willram.ramcore.trade.*;
import dev.willram.ramcore.world.*;
```

For release policy, module ownership, API stability levels, and Folia-safety rules, see [Module Boundaries](MODULE_BOUNDARIES.md).

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

`MessageCatalog` also accepts `TextContext` from the text formatting layer:

```java
messages.send(player, WELCOME, Texts.context()
        .unparsed("player", player.getName())
        .build());
```

## Text Formatting

Package: `dev.willram.ramcore.text`

RamCore text formatting centralizes MiniMessage rendering and placeholder resolution for messages, commands, scoreboards, menus, item text, and config-backed templates. Prefer typed `TextPlaceholder<T>` declarations and `TextContext` values for reusable systems; one-off parsed, unparsed, and component placeholders are still available through the context builder.

Primary types:

- `Texts` is the facade for rendering components, item text, plain text, plain scoreboard lines, and context builders.
- `TextFormatter` wraps a `MiniMessage` instance for custom renderers.
- `TextPlaceholder<T>` declares a typed placeholder name, value type, insertion mode, and string conversion.
- `TextContext` stores immutable placeholder values and merges contexts.
- `TextContexts` and `TextPlaceholders` provide common player, entity, world, and location placeholders.
- `TextPlaceholderMode` distinguishes parsed, unparsed, and component insertion.

Examples:

```java
TextPlaceholder<Integer> LEVEL =
        TextPlaceholder.parsed("level", Integer.class, String::valueOf);

TextContext context = Texts.context()
        .put(LEVEL, 12)
        .unparsed("player", player.getName())
        .build();

Component line = Texts.render("<green><player> reached level <level>.", context);
String scoreboardLine = Texts.plain("<gold>Level: <level>", context);
```

Common context helpers:

```java
TextContext playerContext = TextContexts.player(player);
TextContext locationContext = TextContexts.location(player.getLocation());

TextContext combined = playerContext.merge(locationContext);
```

Convenience integrations:

```java
context.reply("<green>Hello <player>.", TextContexts.player(player));

MenuView menu = Menus.menu("<green><player>'s Rewards", TextContexts.player(player), 3)
        .build();

ItemStack icon = ItemStackBuilder.of(Material.EMERALD)
        .name("<green>Reward for <player>", TextContexts.player(player))
        .build();

objective.applyLines(TextContexts.player(player),
        "<green><player>",
        "<gray>Coins: <coins>");
```

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

## Instanced Loot

Package: `dev.willram.ramcore.loot`

RamCore instanced loot separates generation from claiming. Loot tables produce generic `LootReward` values without side effects; consuming plugins decide whether those values become virtual inventory entries, physical drops, reward actions, claim tokens, or custom payloads. `LootInstance` stores generated rewards with expiry and duplicate-claim rules.

Primary types:

- `LootTable` defines guaranteed entries, weighted entries, base rolls, bonus rolls, and optional named pools.
- `LootPool` defines an independent weighted roll group with its own conditions, rolls, bonus rolls, and reward functions.
- `LootPoolEntry` creates weighted pool rewards and can attach entry-specific conditions/functions.
- `LootConditions` and `LootFunctions` provide common predicates and transformations such as chance, luck, metadata checks, amount changes, payload transforms, and reward metadata.
- `LootEntry` creates one generated `LootReward` and can be conditionally eligible.
- `LootContext` carries player, group, entity, region, world, luck, and custom metadata.
- `LootGenerator` rolls a table into a `LootGenerationResult`.
- `LootInstance` stores claimable generated rewards.
- `LootClaimPolicy` controls duplicate protection: single claim, once per player, or unlimited.
- `LootInstanceStore` stores instances, handles claims, sweeps expiry, rerolls rewards, and emits listener hooks.
- `InstancedLoot` is the facade for tables, rewards, generators, and in-memory stores.

Example:

```java
LootTable chest = InstancedLoot.table("example:boss_chest")
        .guaranteed(LootEntry.guaranteed("coins", LootReward.of("coins", "gold", 50)))
        .weighted(LootEntry.weighted("rare_gem", 1, LootReward.of("rare_gem")))
        .weighted(LootEntry.weighted("potion", 5, LootReward.of("potion")))
        .rolls(2)
        .bonusRolls(context -> context.luck() > 1.0 ? 1 : 0)
        .build();

LootContext context = LootContext.builder("boss")
        .player(player.getUniqueId())
        .sourceEntity(boss.getUniqueId())
        .luck(playerLuck(player))
        .build();

LootGenerationResult generated = InstancedLoot.generator().generate(chest, context, random);
```

Claimable instance:

```java
LootInstanceStore loot = InstancedLoot.inMemoryStore();

LootInstance instance = loot.register(InstancedLoot.instance(chest.id(), generated.rewards())
        .scope(LootInstanceScope.GROUP)
        .claimPolicy(LootClaimPolicy.PER_PLAYER_ONCE)
        .expiresAt(InstancedLoot.expiresAfter(Instant.now(), Duration.ofMinutes(10)))
        .build());

LootClaimResult claim = loot.claim(instance.id(), player.getUniqueId(), Instant.now());
if (claim.successful()) {
    renderClaim(player, claim.rewards());
}
```

Use `sweepExpired(now)` from a scheduled task if instances have expiry times. Use `reroll(instanceId, rewards, clearClaims)` for reroll tokens or admin/debug tools.

Pool builders are useful when one table needs multiple independent roll groups:

```java
LootPool rarePool = InstancedLoot.pool("rare")
        .rolls(1)
        .bonusRolls(context -> (int) context.luck())
        .when(LootConditions.metadataEquals("tier", "boss"))
        .entry(InstancedLoot.entry("gem", LootReward.of("gem"))
                .weight(2)
                .apply(LootFunctions.metadata("source", "rare_pool"))
                .build())
        .entry(InstancedLoot.entry("relic", LootReward.of("relic"))
                .weight(1)
                .apply(LootFunctions.amount(VariableAmount.range(1, 3)))
                .build())
        .build();

LootTable table = InstancedLoot.table("example:boss")
        .rolls(0)
        .pool(rarePool)
        .build();
```

`InstancedLoot.registerPaperCapability(...)` reports `LOOT_TABLES` as partial support: RamCore covers side-effect-free generated loot and Paper merchant recipes, while direct vanilla datapack loot table mutation remains an NMS/datapack boundary.

## Trades

Package: `dev.willram.ramcore.trade`

Trade helpers build and apply Paper `MerchantRecipe` values without depending on NMS internals.

Primary types:

- `TradeOffer` builds one recipe with result item, up to two ingredients, uses, max uses, demand, special price, price multiplier, villager experience, experience reward, and discount behavior.
- `TradeProfile` is an ordered recipe set that can replace or append to an existing merchant.
- `TradeRestockBehavior` makes villager reset, restock, and demand update actions explicit where Paper exposes them.
- `Trades` is the facade for offer/profile creation and scheduler-aware application to `AbstractVillager`.

Example:

```java
TradeOffer relicTrade = Trades.offer(relicItem)
        .ingredient(new ItemStack(Material.EMERALD, 16))
        .maxUses(8)
        .demand(2)
        .priceMultiplier(0.05f)
        .villagerExperience(5)
        .build();

TradeProfile profile = Trades.profile()
        .mode(TradeSetMode.REPLACE)
        .restockBehavior(TradeRestockBehavior.UPDATE_DEMAND_AFTER_APPLY)
        .offer(relicTrade)
        .build();

Trades.apply(villager, profile);
```

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

## Resource Packs

Package: `dev.willram.ramcore.resourcepack`

RamCore resource-pack helpers track pack asset ids, runtime item/sound/font keys, pack metadata, and player prompt outcomes.

Primary types:

- `ResourcePackAssetId` is a `namespace:path` asset key with Bukkit `NamespacedKey` conversion.
- `ResourcePackAsset` stores custom model data, item model keys, sound keys, font glyph keys, and metadata.
- `ResourcePackAssetRegistry` registers assets with owner cleanup and duplicate runtime-key detection.
- `ResourcePackPrompt` describes one pack prompt, hash, forced flag, and timeout.
- `ResourcePackPromptTracker` sends prompts, listens for `PlayerResourcePackStatusEvent`, and marks timed-out requests.
- `ResourcePackItems` and `ResourcePackSounds` apply tracked item model data or create custom Adventure sounds.

Example:

```java
ResourcePackAssetRegistry assets = ResourcePacks.registry();

ResourcePackAsset fireSword = ResourcePackAsset.builder(
        ResourcePacks.id("example:item/fire_sword"),
        ResourcePackAssetType.ITEM_MODEL
).customModelData(101)
        .itemModelKey(ResourcePacks.id("example:item/fire_sword"))
        .metadata("source", "items/fire_sword.json")
        .build();

assets.register(getName(), fireSword);

ItemStack icon = ResourcePackItems.apply(
        ItemStackBuilder.of(Material.DIAMOND_SWORD).name("<red>Fire Sword").build(),
        fireSword
);
```

Pack prompt tracking:

```java
ResourcePackPromptTracker tracker = ResourcePacks.tracker();
registerListener(tracker);

ResourcePackPrompt prompt = ResourcePacks.prompt(
        URI.create("https://cdn.example.com/example-pack.zip"),
        "0123456789abcdef0123456789abcdef01234567"
).forced(false)
        .prompt(Component.text("Download the example resource pack?"))
        .timeoutTicks(20L * 30L)
        .build();

tracker.send(player, prompt);
```

Call `tracker.sweepTimeouts(nowMillis)` from a scheduled task if you use prompt timeouts. Status events update tracked requests automatically while the tracker is registered as a Bukkit listener.

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

Use grouped keys when several commands share one cooldown store but need separate action scopes:

```java
CommandCooldown dailyKit = CommandCooldown
        .grouped(Cooldown.of(24, TimeUnit.HOURS), "kit.daily", ctx -> ctx.requirePlayer().getUniqueId());
```

Use `CommandCooldown.perPlayer(cooldown)` when console should be rejected and the cooldown should follow player UUID rather than sender name.

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

## Diagnostics

Package: `dev.willram.ramcore.diagnostics`

RamCore diagnostics provide paste-safe runtime reports and developer/admin command surfaces for inspecting the current plugin state.

The bundled `/ramcore` diagnostics command remains enabled by default for release builds and is gated by `ramcore.diagnostics`, which defaults to op in `paper-plugin.yml`. Server owners can disable registration entirely with the JVM property `-Dramcore.diagnostics=false`.

Primary types:

- `FoliaDiagnosticsCommandModule` registers `/ramcore` diagnostics, scheduler checks, selectors, safe exports, integration/NMS dumps, and item/entity/block/context inspectors.
- `PluginDiagnostics` captures a `PluginDiagnosticReport` from a plugin, command specs, integrations, NMS registry, and diagnostic providers.
- `DiagnosticRegistry` and `DiagnosticProvider` let modules expose validation, preview, content, config, loot, reward, effect, or region diagnostics without hard-coding every subsystem into the command module.
- `DiagnosticExporter` redacts sensitive lines and truncates very long values before paste/export.
- `SchedulerDiagnostics`, `DiagnosticMemorySnapshot`, and `CommandDiagnostics` expose scheduler mode, Paper/Folia detection, JVM memory/cache notes, and command tree dumps.
- `ServiceRegistry#diagnostics()` exposes registered services, dependency ids, service type, and lifecycle state.

Register the built-in command module:

```java
@Override
public void registerCommands(Commands commands) {
    RamCommands.register(commands, new FoliaDiagnosticsCommandModule(this));
}
```

Useful command surfaces:

```text
/ramcore diagnostics summary
/ramcore diagnostics export
/ramcore diagnostics services
/ramcore diagnostics commands
/ramcore diagnostics integrations
/ramcore diagnostics nms
/ramcore inspect item
/ramcore inspect block
/ramcore inspect entity @e[type=zombie,limit=1]
/ramcore inspect context
```

Custom providers:

```java
DiagnosticRegistry diagnostics = DiagnosticRegistry.create()
        .register(new DiagnosticProvider() {
            @Override
            public String id() {
                return "loot";
            }

            @Override
            public String category() {
                return "gameplay";
            }

            @Override
            public List<String> lines() {
                return List.of("lootTables=12", "lastValidation=ok");
            }
        });
```

Use providers for project-specific content lists, template/config validation, loot-roll tests, reward/effect previews, region dumps, or other debug commands that should appear in safe exports.

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
- `EventSubscriptionGroup` owns several subscriptions and closes them together.
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

Priority shortcuts, one-shot handlers, and grouped cleanup:

```java
EventSubscriptionGroup group = Events.group();

Events.high(PlayerInteractEvent.class)
        .filter(EventFilters.all(
                EventFilters.ignoreCancelled(),
                event -> event.getPlayer().hasPermission("example.use")
        ))
        .handler(event -> handleInteract(event), group);

Events.once(PlayerJoinEvent.class)
        .handler(event -> event.getPlayer().sendRichMessage("<green>First join callback."), group);

group.close();
```

Lifecycle-bound helpers:

```java
Events.listen(PlayerQuitEvent.class, event -> cleanup(event.getPlayer()), this);

Events.listen(PlayerMoveEvent.class,
        EventPriority.MONITOR,
        EventFilters.ignoreSameBlock(),
        event -> updateRegion(event.getPlayer()),
        this);
```

Merged events:

```java
bind(Events.merge(Player.class)
        .bindEvent(PlayerQuitEvent.class, PlayerQuitEvent::getPlayer)
        .bindEvent(PlayerDeathEvent.class, PlayerDeathEvent::getEntity)
        .handler(player -> cleanup(player))
);
```

Reusable filters:

```java
Predicate<PlayerEvent> admins = EventFilters.playerHasPermission("example.admin");
Predicate<PlayerEvent> notAdmins = EventFilters.not(admins);
Predicate<Event> onlyJoins = EventFilters.type(PlayerJoinEvent.class);
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

## Packet Visuals

Package: `dev.willram.ramcore.packet`

Packet visual helpers model per-viewer state that is intentionally separate from server state. Use this layer for ProtocolLib-backed visuals when Bukkit/Paper events or normal entity APIs are not enough.

Primary types:

- `Packets` creates viewer sessions, in-memory transports, diagnostics, and `PACKETS` capability checks.
- `PacketVisualSession` applies viewer-scoped operations such as metadata previews, glowing previews, fake equipment, fake entities, scoreboard visual hints, and reset.
- `PacketVisualState` stores the current visual state for one viewer without mutating Bukkit entities.
- `PacketVisualTransport` is the dispatch boundary; `InMemoryPacketVisualTransport` records operations for tests.
- `ProtocolLibPacketVisualTransport` resolves online players and sends ProtocolLib packets produced by a `ProtocolVisualPacketFactory`.
- `PacketDiagnostics` reports ProtocolLib availability and client-version assumptions.

Example:

```java
PacketViewer viewer = PacketViewer.of(player);
PacketVisualSession session = Packets.session(viewer, packetTransport);

session.metadataPreview(entity.getEntityId(), "pose", "sleeping");
session.glowing(entity.getEntityId(), true);
session.equipment(entity.getEntityId(), EquipmentSlot.HAND, previewItem);

PacketFakeEntity hologramMarker = PacketFakeEntity.builder(9001, UUID.randomUUID(), "minecraft:armor_stand", player.getWorld().getName())
        .position(player.getX(), player.getY() + 2.0, player.getZ())
        .metadata("marker", true)
        .build();

session.spawnFake(hologramMarker);
```

`Packets.registerProtocolCapability(...)` reports `PACKETS` as partial when ProtocolLib is available. Logical operations are stable RamCore state; concrete packet factories must remain version guarded because metadata indices, fake-entity spawn packets, equipment packets, and client assumptions change across Minecraft releases.

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
- `PdcKey<P, C>` is a typed namespaced key with an optional default value.
- `PdcView` reads typed values from a holder or read-only `PersistentDataContainerView`.
- `PdcEditor` mutates typed values on a holder or mutable `PersistentDataContainer`.
- `PdcObjectDataType<T>` stores custom objects through a string codec.
- `PdcJsonDataType<T>` stores custom objects as JSON using RamCore's Gson provider.
- `DataType` exposes a large set of `PersistentDataType` constants and factories for Bukkit types, primitive arrays, collections, maps, enums, and serializable values.

Example:

```java
PDCs.set(itemMeta, "coins", DataType.INTEGER, 10);
Integer coins = PDCs.get(itemMeta, "coins", DataType.INTEGER);
boolean hasCoins = PDCs.has(itemMeta, "coins");
```

Typed keys:

```java
PdcKey<Integer, Integer> coins =
        PDCs.typedKey("example", "coins", DataType.INTEGER).defaultValue(0);

PDCs.set(itemMeta, coins, 25);
int current = PDCs.getOrDefault(itemMeta, coins);
Optional<Integer> optionalCoins = PDCs.get(itemMeta, coins);
```

Typed editor and view:

```java
PdcEditor editor = PDCs.edit(itemMeta);
editor.set(coins, 50);

PdcView view = PDCs.view(itemMeta);
int storedCoins = view.require(coins);
editor.remove(coins);
```

JSON-backed objects:

```java
record Profile(String name, int level) {}

PdcKey<String, Profile> profile =
        PDCs.typedKey("example", "profile", PDCs.jsonType(Profile.class));

PDCs.set(itemMeta, profile, new Profile("guide", 3));
Profile loaded = PDCs.get(itemMeta, profile).orElseThrow();
```

Collection and enum data types:

```java
PersistentDataType<?, List<String>> stringList = DataType.asList(DataType.STRING);
PersistentDataType<String, GameMode> gameMode = DataType.asEnum(GameMode.class);
```

## Items And GUIs

Packages: `dev.willram.ramcore.item`, `dev.willram.ramcore.item.component`, `dev.willram.ramcore.item.nbt`, `dev.willram.ramcore.menu`

Primary types:

- `ItemStackBuilder` fluently builds item stacks and clickable GUI items.
- `ItemComponents` wraps Paper's experimental item data components behind RamCore patch, snapshot, diff, copy, and profile helpers.
- `ItemComponentPatch`, `ItemComponentSnapshot`, and `ItemComponentProfile` provide safe component reads, mutations, comparisons, and common builders.
- `ItemNbt` exposes safe item binary serialization, explicit SNBT codec boundaries, item snapshots, structured item diffs, custom item identity helpers, and template presets.
- `ItemSnapshot`, `ItemDiff`, and `ItemDiffSection` compare type, amount, meta, PDC keys, data components, enchantments, attributes, and raw NBT when a codec supplies it.
- `CustomItemIdentity` and `CustomItemIdentityStore` store namespaced custom item ids in item PDC so identity survives cloning, stacking, serialization, and item-meta edits.
- `MenuView` is a declarative inventory menu definition with static buttons, dynamic rendering, lifecycle hooks, update ticks, and click policy.
- `MenuSession` is one opened menu for one viewer with mutable viewer state and Folia-safe player-bound rendering.
- `MenuButton` is a declarative clickable button with per-click-type or fallback actions.
- `PaginatedMenu<T>` renders entries into item slots and handles previous/next page state.
- `Menus` is the facade for menu, button, session, and pagination builders.
- `Gui` and `Item` remain as legacy compatibility primitives.

Item example:

```java
ItemStack stack = ItemStackBuilder.of(Material.DIAMOND)
        .name("<aqua>Reward")
        .lore("<gray>Click to claim.")
        .amount(3)
        .components(components -> components
                .maxStackSize(16)
                .enchantmentGlintOverride(true))
        .build();
```

Data components:

```java
ItemComponentBackend components = ItemComponents.edit(stack);
ItemComponentSnapshot before = components.snapshot();

ItemComponents.profile()
        .maxDamage(500)
        .damage(0)
        .itemModel(Key.key("example", "ruby_sword"))
        .glider(false)
        .weapon(2, 0.5f)
        .build()
        .apply(components);

ItemComponentPatch changed = before.diff(components.snapshot());
ItemComponentSerializedPatch stored = ItemComponents.serialize(changed);
```

Patch helpers:

```java
ItemComponents.patch()
        .unset(DataComponentTypes.CUSTOM_MODEL_DATA)
        .reset(DataComponentTypes.MAX_STACK_SIZE)
        .build()
        .apply(components);

boolean sameForGameplay = ItemComponents.matchesIgnoring(
        ItemComponents.edit(first),
        ItemComponents.edit(second),
        Set.of(DataComponentTypes.DAMAGE)
);
```

NBT and serialization:

```java
BukkitItemBinaryCodec binary = ItemNbt.binaryCodec();
String encoded = binary.exportBase64(stack);
ItemStack restored = binary.importBase64(encoded);

ItemSnapshot beforeSnapshot = ItemNbt.snapshot(before);
ItemSnapshot afterSnapshot = ItemNbt.snapshot(after, ItemNbt.unsupportedSnbtCodec());
ItemDiff diff = beforeSnapshot.diff(afterSnapshot);

if (diff.changed(ItemDiffSection.PDC) || diff.changed(ItemDiffSection.DATA_COMPONENTS)) {
    logger.info("custom item state changed");
}
```

Custom identity and presets:

```java
CustomItemIdentity identity = new CustomItemIdentity(NamespacedKey.fromString("example:ruby_sword"), 1);
ItemStack identified = ItemNbt.identify(stack, identity);
boolean sameIdentity = CustomItemIdentityStore.matches(identified, identity);

ItemTemplatePreset preset = ItemTemplatePresets.weapon(Material.DIAMOND_SWORD)
        .withComponents(ItemComponents.profile()
                .itemModel(Key.key("example", "ruby_sword"))
                .maxDamage(900)
                .build());
```

GUI item:

```java
Item reward = ItemStackBuilder.of(Material.EMERALD)
        .name("<green>Claim")
        .build(() -> player.sendRichMessage("<green>Claimed."));
```

Declarative menu:

```java
MenuView menu = Menus.menu("<green>Rewards", 3)
        .button(13, Menus.button(ItemStackBuilder.of(Material.EMERALD)
                        .name("<green>Claim")
                        .build())
                .onAny(ctx -> claimReward(ctx.session().player()))
                .closeAfterClick(true)
                .build())
        .onClose(session -> saveMenuState(session.player(), session.state().snapshot()))
        .build();

MenuSession session = Menus.open(player, menu);
session.state().put("source", "daily_reward");
```

Dynamic rendering and update ticks:

```java
MenuView liveStats = Menus.menu("<aqua>Stats", 3)
        .render(session -> session.setButton(11, statButton(session.player())))
        .onTick(session -> session.state().put("ticks", session.state().intValue("ticks", 0) + 1))
        .updateEveryTicks(20L)
        .build();
```

Pagination:

```java
PaginatedMenu<RewardTemplate> rewards = Menus.paginated(Component.text("Rewards"), 6, (reward, index) ->
        Menus.button(renderRewardIcon(reward))
                .onAny(ctx -> previewReward(ctx.session().player(), reward))
                .build())
        .entries(rewardTemplates)
        .slotRange(0, 44)
        .previous(45, Menus.button(previousIcon()).build())
        .next(53, Menus.button(nextIcon()).build())
        .build();

rewards.open(player);
```

`MenuSession` owns event subscriptions and scheduled update tasks. Rendering and opening are routed through the player scheduler so menu mutations stay anchored to the viewer on Folia.

`ITEM_DATA_COMPONENTS` is reported as partial Paper API support through `ItemComponents.registerPaperCapability(...)`. `ITEM_NBT` is reported as partial Paper API support through `ItemNbt.registerPaperCapability(...)`: Bukkit/Paper exposes safe binary item serialization and structured meta/PDC/component inspection, but raw SNBT import/export requires a guarded NMS adapter. The component API is experimental in Paper, so consuming plugins should prefer RamCore patches/profiles over direct `ItemStack#setData(...)` calls when they need a stable boundary.

## World And Blocks

Package: `dev.willram.ramcore.world`

World/block helpers keep mutation APIs anchored to Paper/Folia schedulers and expose typed block-entity surfaces before falling back to guarded NMS boundaries.

Primary types:

- `WorldBlocks` snapshots block states, edits block states on their owning region scheduler, configures spawners, restores structure snapshots, and reports `BLOCK_ENTITY_NBT` capability support.
- `BlockEntitySnapshot` captures material, block data, PDC keys, typed properties, and optional raw SNBT for signs, containers, spawners, skulls, banners, lecterns, command blocks, and generic tile states.
- `BlockEntityNbtCodec` makes raw block-entity SNBT support explicit; the default codec is unsupported until a guarded NMS adapter is registered.
- `SpawnerConfig`, `SpawnerEntityTemplate`, `SpawnerWeightedEntry`, and `SpawnerSpawnRule` wrap Paper spawner APIs with validation for delays, ranges, spawn counts, entity templates, and weighted potential spawns.
- `StructureSnapshot` stores relative block snapshots and defaults restore operations to a single target chunk so Folia region ownership is not crossed accidentally.

Example:

```java
SpawnerConfig config = SpawnerConfig.builder()
        .spawnedTemplate(SpawnerEntityTemplate.of(EntityType.ZOMBIE))
        .minSpawnDelay(20)
        .maxSpawnDelay(80)
        .spawnCount(3)
        .requiredPlayerRange(12)
        .spawnRange(4)
        .build();

WorldBlocks.configureSpawner(spawnerState, config, true, false);

BlockEntitySnapshot snapshot = WorldBlocks.snapshot(signState);

StructureSnapshot structure = StructureSnapshot.capture(region);
WorldBlocks.restore(targetLocation, structure, StructureApplyOptions.DEFAULT);
```

`BLOCK_ENTITY_NBT` is reported as partial Paper API support through `WorldBlocks.registerPaperCapability(...)`: Paper exposes typed block states and scheduler-safe mutation, while raw block-entity SNBT import/export remains behind the NMS adapter boundary.

## Selectors

Package: `dev.willram.ramcore.selector`

RamCore selectors provide reusable player and entity filtering outside the command API. They do not parse Brigadier selector strings; use `RamArguments.players(...)` or `RamArguments.entities(...)` for command input, then use these selectors to refine command-resolved collections or any other entity collection.

Primary types:

- `Selectors` creates player and entity selectors and includes name/UUID helpers.
- `EntitySelector<T>` filters entities by type, UUID, name, world, distance, custom predicates, sort, and limit.
- `PlayerSelector` adds player predicates such as permission, game mode, and online state.
- `SelectorSort` declares supported ordering modes.

Example:

```java
List<Player> candidates = Selectors.players()
        .permission("example.reward")
        .within(origin, 32)
        .nearest(origin)
        .limit(5)
        .select(Bukkit.getOnlinePlayers());
```

Entity queries:

```java
List<LivingEntity> nearbyMobs = Selectors.entity(LivingEntity.class)
        .world(origin.getWorld())
        .within(origin, 24)
        .filter(entity -> entity.isValid() && !entity.isDead())
        .select(origin.getWorld().getEntities());
```

Use `first(...)` when no result is acceptable and `single(...)` when exactly one match is required. `single(...)` throws `ApiMisuseException` if the selector matches zero or multiple entries.

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
- `CooldownTracker<T>` adds structured test results, manual cleanup, and expiry callbacks.
- `CooldownKey` stores grouped cooldown keys for shared stores.
- `ActionThrottle<T>` applies per-action throttling using grouped cooldown keys.
- `Cooldowns` creates common cooldown stores such as grouped and player cooldowns.

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

Structured results and expiry callbacks:

```java
CooldownTracker<UUID> casts = Cooldowns.tracker(Cooldown.of(10, TimeUnit.SECONDS))
        .onExpire((playerId, cooldown) -> cleanupCastState(playerId));

CooldownResult<UUID> result = casts.test(player.getUniqueId());
if (result.denied()) {
    player.sendRichMessage("<red>Wait " + result.remainingSecondsCeil() + "s.");
}

casts.sweepExpired();
```

Action throttling:

```java
ActionThrottle<UUID> throttle = ActionThrottle.create(Cooldown.ofTicks(20));
if (throttle.allowed(player.getUniqueId(), "menu_click")) {
    handleClick();
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

- `DataRepository<K, V extends DataItem>` is a keyed in-memory registry with setup/save lifecycle hooks, optional lookup, required lookup, values, and size helpers.
- `DataItem` tracks persistent `dataVersion` plus transient saving, dirty, and save-enabled state.
- `FileDataRepository<K, V>` stores one JSON file per item, loads directories, saves all or dirty items, queues async saves, deletes files, and applies versioned migrations.
- `DataKeyCodec<K>` maps keys to file-safe names; string and UUID codecs are provided.
- `DataSerializer<V>` abstracts repository item serialization; `GsonDataSerializer` provides standard and pretty Gson serializers.
- `DataRepositoryMigration<V>` and `DataMigration<V>` define ordered migration hooks.
- `Repositories` provides common JSON repository factories.
- `NamespacedKeys` creates RamCore namespaced keys.

Example:

```java
FileDataRepository<UUID, PlayerProfile> profiles = Repositories.jsonByUuid(
        getDataFolder().toPath().resolve("profiles"),
        PlayerProfile.class,
        Schedulers.async()
);

profiles.migrateTo(2, (profile, fromVersion) -> {
    profile.recalculateIndexes();
    return profile;
});

profiles.setup();

PlayerProfile profile = profiles.require(player.getUniqueId());
profile.markDirty();
profiles.queueSave(player.getUniqueId());
```

Use `saveDirty()` for tick or shutdown batches. `close()` flushes queued saves and writes remaining dirty items.

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

## NMS Access Strategy

Packages: `dev.willram.ramcore.nms.api`, `dev.willram.ramcore.nms.reflect`

RamCore internal access follows this order: Paper API first, RamCore adapter second, guarded reflection third, and versioned implementation last. Consuming plugins should depend on RamCore interfaces and capability checks, not raw `net.minecraft` classes.

Primary types:

- `NmsAccess` creates runtime or explicit-version registries.
- `NmsAccessRegistry` registers adapters, checks capabilities, requires support, and produces diagnostics.
- `NmsCapability` names both broad feature buckets and granular surfaces such as goal snapshots, brain memory reads, navigation profiles, item SNBT, spawner configuration, trade recipes, and packet transports.
- `NmsCapabilityCategory` groups capability checks into AI, brain, pathfinding, entity, combat, item, world, loot, and packet areas.
- `NmsCapabilityCheck` records support status, access tier, adapter id, version bounds, and reason.
- `NmsUnsupportedException` is thrown by `require(...)` when a capability is unavailable.
- `NmsDiagnostics` provides startup/admin lines describing runtime version, NMS version, adapters, and capability decisions.
- `NmsCompatibilityMatrix` and `NmsCompatibilityCell` render capability support across supported Minecraft versions.
- `NmsSelfTestPlan` and `NmsSelfTestReport` run startup checks for required and optional internals.
- `NmsQuarantinePolicy` and `NmsQuarantineDecision` decide whether unsupported or unknown internals should be enabled, warned, disabled, or held for review.
- `NmsExampleModules` lists manual smoke-test surfaces for advanced feature checks on Paper and Folia.
- `GuardedNmsLookup` and `ReflectiveNmsAdapter` support reflection probes without leaking `ClassNotFoundException`.

Example:

```java
NmsAccessRegistry nms = NmsAccess.runtimeRegistry();
NmsCapabilityCheck goals = nms.check(NmsCapability.MOB_GOALS);
NmsCapabilityCheck rawItemSnbt = nms.check(NmsCapability.ITEM_SNBT);

if (goals.usable()) {
    // Use a RamCore mob-goals facade, not raw net.minecraft classes.
}

logger.info(rawItemSnbt.summary());
```

Fail-fast requirement:

```java
try {
    nms.require(NmsCapability.ITEM_DATA_COMPONENTS);
} catch (NmsUnsupportedException e) {
    logger.warning(e.getMessage());
}
```

Diagnostics:

```java
for (String line : nms.diagnostics().lines()) {
    logger.info(line);
}
```

Startup self-test:

```java
NmsSelfTestPlan plan = NmsSelfTestPlan.builder()
        .required("mob-goals", NmsCapability.MOB_GOALS, "Custom mobs require Paper mob goals.")
        .optional("packets", NmsCapability.PACKETS, "Packet previews can be disabled.")
        .build();

NmsSelfTestReport report = nms.selfTest(plan);
for (String line : report.lines()) {
    logger.info(line);
}
```

Compatibility matrix:

```java
NmsCompatibilityMatrix matrix = nms.compatibilityMatrix(List.of(
        MinecraftVersion.of(1, 21, 0)
));

for (String line : matrix.markdownLines()) {
    logger.info(line);
}
```

Quarantine decisions:

```java
NmsQuarantinePolicy policy = NmsQuarantinePolicy.strict();
NmsQuarantineDecision decision = policy.evaluate(nms.check(NmsCapability.ITEM_NBT));

if (!decision.usable()) {
    logger.warning(decision.reason());
}
```

NMS package policy:

- Public contracts live under `dev.willram.ramcore.nms.api`.
- Guarded reflection helpers live under `dev.willram.ramcore.nms.reflect`.
- Future version-specific implementations should live under packages such as `dev.willram.ramcore.nms.v1_21_x`.
- Public APIs should expose Bukkit/Paper/RamCore types, not raw NMS handles.
- `SUPPORTED` internals may stay enabled after startup self-tests pass; `PARTIAL` internals should warn and get a manual example-module check.
- `UNSUPPORTED` internals should be disabled, and `UNKNOWN` internals require review before being exposed on a new Minecraft release.

## Mob AI

Package: `dev.willram.ramcore.ai`

RamCore mob AI helpers wrap Paper's `MobGoals` API behind a small controller layer. Paper is the primary backend; future NMS adapters can register additional capability support through the NMS access strategy when Paper does not expose a behavior.

Primary types:

- `MobAi` creates controllers, goal keys, custom goal builders, Paper/in-memory backends, and the NMS capability marker for mob goals.
- `MobAiController<T>` adds, removes, replaces, pauses, resumes, snapshots, restores, and diagnoses goals for one mob.
- `MobGoalBackend` abstracts Paper goal operations; `PaperMobGoalBackend` uses `Bukkit.getMobGoals()`, while `InMemoryMobGoalBackend` is useful for tests and offline planning.
- `RamMobGoalBuilder<T>` and `RamMobGoal<T>` create callback-backed Paper goals with activation, stay-active, start, stop, and tick hooks.
- `CommonMobGoals` provides reusable goals such as follow entity, guard area, patrol waypoints, flee from entity, attack target, look-at target, return home, leash to region, and idle animation.
- `MobGoalDecorators` adds conditions, cooldowns, timeouts, random chance gates, distance gates, line-of-sight gates, and health gates.
- `MobGoalSnapshot`, `MobAiDiagnostics`, and `MobGoalConflict` expose restore/debug data for active goals, tracked priorities, shared-type conflicts, and current targets.

Example:

```java
GoalKey<Mob> guardKey = MobAi.key(Mob.class, NamespacedKey.fromString("example:guard"));

MobAiController<Mob> ai = MobAi.controller(guard);
RamMobGoal<Mob> guardGoal = CommonMobGoals.guardArea(
        guard,
        MobAi.goal(guardKey),
        home,
        12.0,
        1.1
);

MobGoalSnapshot<Mob> beforeOverride = ai.snapshot();
ai.replace(2, guardGoal);
```

Pause and restore:

```java
ai.pause(guardKey);
ai.resume(guardKey);
ai.restore(beforeOverride);
```

Decorators:

```java
Goal<Mob> guarded = MobGoalDecorators.cooldown(
        guardGoal,
        Duration.ofSeconds(5),
        Clock.systemUTC()
);
```

Diagnostics:

```java
MobAiDiagnostics<Mob> diagnostics = ai.diagnostics();
List<MobGoalConflict<Mob>> conflicts = diagnostics.conflicts();
```

Paper does not expose vanilla goal priorities, so snapshots restore RamCore-tracked goals. Use `MobAi.registerPaperCapability(nmsRegistry)` to report Paper-backed mob goal support through the NMS diagnostics layer.

## Brain Memories

Package: `dev.willram.ramcore.brain`

RamCore brain helpers expose the stable part of modern mob brains first: Bukkit/Paper `MemoryKey<T>` reads and writes through `LivingEntity#getMemory` and `setMemory`. Sensors and activities are reported through the NMS capability system as unsupported until a versioned adapter is added, because Paper does not expose stable public types for them.

Primary types:

- `MobBrains` creates controllers/backends and registers brain capability decisions with `NmsAccessRegistry`.
- `MobBrainController` reads, writes, clears, snapshots, and diagnoses selected memories for one living entity.
- `MobBrainBackend` abstracts memory access; `PaperMobBrainBackend` uses Bukkit/Paper memory methods and `InMemoryMobBrainBackend` supports tests/offline planning.
- `BrainMemorySnapshot` and `BrainMemoryValue<T>` capture present and absent selected memory keys.
- `MobBrainDiagnostics` reports selected memories plus brain-memory, sensor, and activity capability checks.

Example:

```java
MobBrainController brain = MobBrains.controller(mob);

brain.home(homeLocation)
        .meetingPoint(meetingPoint)
        .angryAt(target.getUniqueId())
        .attackCooldown(40);

Optional<Location> home = brain.get(MemoryKey.HOME);
brain.clear(MemoryKey.ANGRY_AT);
```

Diagnostics:

```java
MobBrainDiagnostics diagnostics = brain.diagnostics(MobBrains.COMMON_DEBUG_KEYS);
for (String line : diagnostics.lines()) {
    logger.info(line);
}
```

NMS capability reporting:

```java
NmsAccessRegistry nms = MobBrains.registerPaperCapabilities(NmsAccess.runtimeRegistry());
NmsCapabilityCheck memory = nms.check(NmsCapability.BRAIN_MEMORY);
NmsCapabilityCheck sensors = nms.check(NmsCapability.BRAIN_SENSORS);
```

`BRAIN_MEMORY` is reported as partial Paper API support. `BRAIN_SENSORS` and `BRAIN_ACTIVITY` remain unsupported until a guarded version-specific adapter exists.

## Pathfinding

Package: `dev.willram.ramcore.path`

RamCore pathfinding wraps Paper's `Pathfinder` API with request objects, progress snapshots, failure/cancellation reasons, and entity-bound scheduling. The public API stays Paper-first and reports deeper navigation features as partial until a guarded NMS adapter is available.

Primary types:

- `Pathfinders` creates controllers, requests, routes, patrols, backends, and NMS capability decisions.
- `PathController` creates manually ticked path tasks or schedules Folia-safe tasks on the mob's entity scheduler.
- `PathRequest` describes a destination, speed, maximum distance, stuck timeout, repath interval, completion distance, timeout, navigation profile, and callbacks.
- `PathTask`, `PathProgress`, and `PathTaskResult` expose runtime state, path points, remaining distance, stuck detection, terminal status, failure reason, and cancellation reason.
- `PathNavigationProfile` maps the Paper-exposed navigation toggles for opening doors, passing doors, and floating.
- `WaypointRoute` and `PatrolController` run ordered waypoint routes. `Pathfinders.patrolGoal(...)` adapts a route into the existing RamCore mob-goal facade.
- `PathBackend`, `PaperPathBackend`, and `InMemoryPathBackend` keep Paper implementation details behind a testable contract.

Example:

```java
PathTask task = Pathfinders.controller(guard).schedule(Pathfinders.to(home)
        .speed(1.2)
        .maxDistance(48.0)
        .stuckTimeoutTicks(60)
        .repathIntervalTicks(20)
        .navigationProfile(PathNavigationProfile.builder()
                .canOpenDoors(true)
                .canFloat(true)
                .build())
        .onFailure(result -> logger.warning(result.message()))
        .build());
```

Waypoint patrol:

```java
WaypointRoute route = Pathfinders.route()
        .add(pointA)
        .add(pointB)
        .add(pointC)
        .loop(true)
        .build();

RamMobGoal<Mob> patrol = Pathfinders.patrolGoal(
        guard,
        MobAi.goal(MobAi.key(Mob.class, NamespacedKey.fromString("example:patrol"))),
        route,
        PathOptions.builder().speed(1.1).completionDistance(1.0).build()
);
```

NMS capability reporting:

```java
NmsAccessRegistry nms = Pathfinders.registerPaperCapability(NmsAccess.runtimeRegistry());
NmsCapabilityCheck pathfinding = nms.check(NmsCapability.PATHFINDING);
```

`PATHFINDING` is reported as partial Paper API support. Paper exposes path movement, path points, door toggles, and float toggles. Node penalties, water/lava internals, flying/swimming navigation selection, and custom movement controllers should stay behind future version-specific adapters.

## Entity Control

Package: `dev.willram.ramcore.entity`

RamCore entity control helpers wrap the stable Paper/Bukkit entity surface for common mob setup and temporary mutations. Direct NMS look, move, jump, and anger controllers are intentionally kept behind future adapters; the current API reports Paper support as partial.

Primary types:

- `EntityControls` creates controls, templates, equipment specs, attribute specs, snapshots, temporary modifiers, and NMS capability decisions.
- `EntityControl<T>` offers fluent operations for look-at, velocity, teleport, jumping, target selection, awareness, persistence, despawn behavior, pickup rules, invulnerability, visibility, tags, and path controller access.
- `EntityControlSnapshot` and `EntityTemporaryModifier<T>` capture common entity state and restore it through the `Terminable` lifecycle.
- `EntityTemplate<T>` configures living entity spawns with flags, equipment/drop chances, attributes, scoreboard tags, PDC customization, metadata, AI customization, path navigation profile, passengers, spawn reason, and final customizers.
- `ConfiguredEntitySpawner` spawns templates region-safely through RamCore schedulers, with `spawnNow(...)` available when the caller already owns the region context.
- `EntityEquipmentSpec`, `EntityAttributeSpec`, and `EntitySpawnHandle<T>` provide focused value objects for template application and spawn results.

Example:

```java
EntityTemporaryModifier<Mob> override = EntityControls.temporary(guard);
override.control()
        .invulnerable(true)
        .aware(false)
        .clearTarget()
        .scoreboardTag("cinematic");

override.close(); // restores captured flags, target, velocity, and tags
```

Configured spawn:

```java
EntityTemplate<Zombie> guardTemplate = EntityControls.template(Zombie.class)
        .name(Component.text("Gate Guard"))
        .persistent(true)
        .removeWhenFarAway(false)
        .canPickupItems(false)
        .scoreboardTag("guard")
        .spawnReason(CreatureSpawnEvent.SpawnReason.CUSTOM)
        .equipment(EntityControls.equipment()
                .dropChance(EquipmentSlot.HAND, 0.0f)
                .build())
        .attributes(EntityControls.attributes()
                .base(Attribute.MAX_HEALTH, 40.0)
                .base(Attribute.MOVEMENT_SPEED, 0.28)
                .build());

Promise<EntitySpawnHandle<Zombie>> spawned = ConfiguredEntitySpawner.spawn(spawnPoint, guardTemplate);
```

NMS capability reporting:

```java
NmsAccessRegistry nms = EntityControls.registerPaperCapability(NmsAccess.runtimeRegistry());
NmsCapabilityCheck control = nms.check(NmsCapability.ENTITY_CONTROL);
```

`ENTITY_CONTROL` is reported as partial Paper API support. Paper covers common entity flags, target selection, pickup rules, equipment drops, attributes, and spawning. Raw movement controllers and version-specific anger internals should stay behind future adapters.

## Attribute And Combat Helpers

Package: `dev.willram.ramcore.combat`

RamCore combat helpers wrap Paper's vanilla attribute and damage APIs with typed value objects and restorable temporary buffs. They cover stable Bukkit/Paper behavior first; attack cooldown internals, attack animations, hurt timer internals, and custom damage routing remain NMS-adapter territory.

Primary types:

- `CombatControls` creates attribute specs, modifier specs, combat profiles, damage profiles, temporary buffs, and NMS capability decisions.
- `EntityAttributeSpec` now supports both base attribute values and keyed attribute modifiers.
- `AttributeModifierSpec` builds keyed Bukkit `AttributeModifier` values with `ADD_NUMBER`, `ADD_SCALAR`, and `MULTIPLY_SCALAR_1` operations.
- `AttributeBuff` applies an attribute spec temporarily and restores previous base values and same-key modifiers when closed.
- `CombatProfile` groups common mob combat attributes such as movement speed, follow range, armor, armor toughness, scale, safe fall distance, gravity, step height, knockback resistance, and interaction reach.
- `DamageProfile` applies damage with optional damager or `DamageSource`, no-damage-tick clearing, post-damage invulnerability ticks, and hurt direction.

Example:

```java
CombatProfile brute = CombatControls.profile()
        .movementSpeed(0.32)
        .followRange(32.0)
        .armor(10.0)
        .armorToughness(4.0)
        .scale(1.15)
        .stepHeight(1.0)
        .build();

brute.apply(mob);
```

Temporary buff:

```java
AttributeBuff buff = CombatControls.buff(mob, CombatControls.attributes()
        .modifier(Attribute.MOVEMENT_SPEED, CombatControls.modifier(NamespacedKey.fromString("example:sprint"))
                .addScalar(0.25)
                .transientModifier(true)
                .build())
        .build());

buff.close(); // removes the sprint modifier and restores any replaced modifier with the same key
```

Damage profile:

```java
CombatControls.damage(8.0)
        .damager(attacker)
        .clearNoDamageTicks(true)
        .noDamageTicksAfter(10)
        .hurtDirection(180.0f)
        .build()
        .apply(target);
```

NMS capability reporting:

```java
NmsAccessRegistry nms = CombatControls.registerPaperCapability(NmsAccess.runtimeRegistry());
NmsCapabilityCheck attributes = nms.check(NmsCapability.ENTITY_ATTRIBUTES);
```

`ENTITY_ATTRIBUTES` is reported as partial Paper API support. Paper covers vanilla attributes, modifiers, invulnerability ticks, and damage application; deeper combat internals require version-specific adapters.

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
| `ai` | Paper-backed mob goal facade, custom goals, decorators, snapshots, and diagnostics. |
| `brain` | Paper memory-key access and diagnostics for modern mob brains. |
| `combat` | Attribute builders, modifier specs, temporary buffs, combat profiles, and controlled damage application. |
| `commands` | Brigadier-first command API. |
| `content` | Typed namespaced content registries with owner tracking. |
| `diagnostics` | Paste-safe debug exports, scheduler/service/command/runtime reports, diagnostic providers, and `/ramcore` inspect commands. |
| `display` | Display entity specs, region-safe spawning, and hologram stacks. |
| `encounter` | Boss encounter definitions, phases, timed abilities, enrage/wipe/reset state, and contribution tracking. |
| `entity` | Entity control wrappers, temporary modifiers, configured mob templates, equipment, attributes, and spawn helpers. |
| `integration` | Optional plugin capability detection for LuckPerms, Vault, placeholders, WorldGuard, ProtocolLib, Citizens, ItemsAdder, and Oraxen. |
| `loot` | Side-effect-free loot tables, weighted pools, conditions, functions, and claimable instanced loot. |
| `npc` | Server-backed NPC specs, click dispatch, visibility, and cleanup. |
| `objective` | Storage-neutral objective definitions, subject progress, chained tasks, and progress events. |
| `party` | In-memory party/group membership, invites, rules, contribution tracking, and teleport helpers. |
| `packet` | Viewer-scoped packet visual state, fake entity/equipment previews, ProtocolLib transport boundaries, and diagnostics. |
| `path` | Paper-backed managed path requests, progress tracking, Folia-safe path tasks, routes, and patrol goals. |
| `service` | Typed service registry and dependency-aware lifecycle. |
| `template` | Typed reusable templates with parent inheritance and validation. |
| `text` | MiniMessage formatting, typed placeholders, and reusable render contexts. |
| `trade` | MerchantRecipe offer builders, merchant profiles, and scheduler-aware villager trade application. |
| `message` | MiniMessage catalog, reusable keys, prefixes, and placeholders. |
| `nms.api`, `nms.reflect` | Guarded NMS capability strategy, diagnostics, compatibility matrices, startup self-tests, quarantine decisions, and reflection probes. |
| `permission` | Typed permission nodes, grouped checks, and command requirements. |
| `presentation` | Adventure message, title, sound, boss-bar, and sequence effects. |
| `region` | Lightweight regions and priority rule evaluation. |
| `resourcepack` | Resource-pack asset ids, item/sound/font keys, metadata, prompts, and status tracking. |
| `reward` | Generic reward validation, preview, and execution pipeline. |
| `scheduler` | Paper/Folia-aware scheduling and task contexts. |
| `selector` | Reusable collection-based player and entity selectors. |
| `promise` | Thread-aware promise/future abstraction. |
| `event` | Functional Bukkit and ProtocolLib event subscriptions. |
| `terminable` | Resource lifecycle and cleanup ownership. |
| `metadata` | In-memory typed metadata on players/entities/blocks/worlds. |
| `pdc` | Typed PersistentDataContainer keys, views, editors, object serialization, and data types. |
| `item`, `item.component`, `item.nbt`, `menu` | ItemStack builder, experimental Paper data-component wrappers, item serialization/diff/identity helpers, declarative inventory menus, viewer sessions, and pagination. |
| `world` | Folia-aware block-state edits, typed block-entity snapshots, spawner configuration, and guarded structure snapshot/restore helpers. |
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
