package dev.willram.ramcore.example.kotlin

import dev.willram.ramcore.RamPlugin
import dev.willram.ramcore.ability.Ability
import dev.willram.ramcore.ability.AbilityRegistry
import dev.willram.ramcore.ability.AbilityService
import dev.willram.ramcore.ability.AbilityTargets
import dev.willram.ramcore.ability.AbilityTrigger
import dev.willram.ramcore.content.ContentId
import dev.willram.ramcore.dialogue.Dialogue
import dev.willram.ramcore.dialogue.DialogueActions
import dev.willram.ramcore.dialogue.DialogueChoice
import dev.willram.ramcore.dialogue.DialogueNode
import dev.willram.ramcore.dialogue.DialogueRegistry
import dev.willram.ramcore.dialogue.DialogueSession
import dev.willram.ramcore.kotlin.command
import dev.willram.ramcore.kotlin.literal
import dev.willram.ramcore.kotlin.register
import dev.willram.ramcore.resourcepack.AssetSource
import dev.willram.ramcore.resourcepack.ResourcePackAssetId
import dev.willram.ramcore.resourcepack.ResourcePackBuilder
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.TaskContext
import dev.willram.ramcore.stat.ItemStatSource
import dev.willram.ramcore.stat.Stat
import dev.willram.ramcore.stat.StatModifier
import dev.willram.ramcore.stat.StatRegistry
import dev.willram.ramcore.stat.StatService
import dev.willram.ramcore.stat.StatSource
import dev.willram.ramcore.worldinstance.PaperWorldBackend
import dev.willram.ramcore.worldinstance.WorldInstanceOptions
import dev.willram.ramcore.worldinstance.WorldInstanceService
import io.papermc.paper.command.brigadier.Commands

/**
 * Kotlin twin of the Java [dev.willram.ramcore.example.SamplePlugin]: the same custom stat, ability,
 * dialogue, instanced dungeon, and generated resource pack, written with the RamCore Kotlin command
 * DSL and SAM-friendly APIs. Wire-up only — see `RELEASE_READINESS.md`.
 */
class SamplePlugin : RamPlugin() {

    private val stats = StatRegistry()
    private val abilities = AbilityRegistry()
    private val dialogues = DialogueRegistry()

    private lateinit var statService: StatService
    private lateinit var abilityService: AbilityService
    private lateinit var worldInstances: WorldInstanceService

    override fun load() {
        // 3.4 — a custom stat + service (installed from load()).
        stats.register(name, Stat.of(POWER, 10.0))
        statService = StatService.install(this, stats)

        // 3.3 — an ability that spends the stat, targeting what the caster looks at.
        abilities.register(
            name,
            Ability.builder(STRIKE)
                .cooldown(java.time.Duration.ofSeconds(3))
                .cost(POWER, 5.0)
                .targeting(AbilityTargets.lookingAt(20.0))
                .action { context -> context.targets().forEach { it.fireTicks = 60 } }
                .build(),
        )
        abilityService = AbilityService.install(this, abilities, statService)

        // 3.5 — a two-node dialogue.
        dialogues.register(
            name,
            Dialogue.builder(GUIDE)
                .node(
                    DialogueNode.builder("root", "<yellow>Welcome, traveller. Care for a quest?")
                        .choice(DialogueChoice.of("<green>Accept", "accept"))
                        .choice(DialogueChoice.of("<red>Not now", null))
                        .build(),
                )
                .node(
                    DialogueNode.builder("accept", "<green>Splendid! Slay the dungeon boss.")
                        .action(DialogueActions.message("<gray>(quest objective started)"))
                        .build(),
                )
                .build(),
        )

        // 3.2 — instanced worlds from plugins/<this>/world-templates/<name>/.
        worldInstances = WorldInstanceService(PaperWorldBackend(), dataFolder.toPath().resolve("world-templates"))
    }

    override fun enable() {
        statService.addSource(ItemStatSource())
        statService.addSource(StatSource { _ -> listOf(StatModifier.add(POWER, 25.0, "example:base")) })
        abilityService.bindHotbar(0, STRIKE)
        Schedulers.runAsync { worldInstances.sweepStartup() }
    }

    override fun disable() {
    }

    override fun registerCommands(commands: Commands) {
        val spec = command("sample") {
            description("RamCore example commands.")
            playerOnly()
            literal("cast") {
                description("Cast the example ability.")
                executes { context ->
                    context.reply("<gray>" + abilityService.cast(context.requirePlayer(), STRIKE, AbilityTrigger.COMMAND).status())
                }
            }
            literal("talk") {
                description("Open the example dialogue.")
                executes { context ->
                    DialogueSession.chat(context.requirePlayer(), dialogues.require(GUIDE)).start()
                }
            }
            literal("dungeon") {
                description("Create an instanced dungeon world.")
                executes { context ->
                    context.reply("<gray>Creating dungeon...")
                    worldInstances.create("dungeon", WorldInstanceOptions.defaults())
                        .thenApply(TaskContext.global()) { instance ->
                            context.reply("<green>Dungeon ready: <white>" + instance.name())
                            instance
                        }
                }
            }
            literal("pack") {
                description("Build the example resource pack.")
                executesAsync { context ->
                    try {
                        val report = ResourcePackBuilder.create()
                            .name("Sample Pack")
                            .minecraftVersion("1.21.4")
                            .item(RUBY_ITEM, AssetSource.ofString("<png-bytes>"))
                            .buildTo(dataFolder.toPath().resolve("pack.zip"))
                        context.reply("<green>Built pack, sha1=<white>" + report.sha1Hex())
                    } catch (failure: Exception) {
                        context.reply("<red>Pack build failed: <white>" + failure.message)
                    }
                }
            }
        }
        commands.register(spec)
    }

    private companion object {
        val POWER: ContentId = ContentId.of("sample", "power")
        val STRIKE: ContentId = ContentId.of("sample", "strike")
        val GUIDE: ContentId = ContentId.of("sample", "guide")
        val RUBY_ITEM: ResourcePackAssetId = ResourcePackAssetId.of("sample", "ruby_sword")
    }
}
