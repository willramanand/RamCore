package dev.willram.ramcore.example;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.ability.Ability;
import dev.willram.ramcore.ability.AbilityRegistry;
import dev.willram.ramcore.ability.AbilityService;
import dev.willram.ramcore.ability.AbilityTargets;
import dev.willram.ramcore.ability.AbilityTrigger;
import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.dialogue.Dialogue;
import dev.willram.ramcore.dialogue.DialogueChoice;
import dev.willram.ramcore.dialogue.DialogueNode;
import dev.willram.ramcore.dialogue.DialogueRegistry;
import dev.willram.ramcore.dialogue.DialogueSession;
import dev.willram.ramcore.dialogue.DialogueActions;
import dev.willram.ramcore.resourcepack.AssetSource;
import dev.willram.ramcore.resourcepack.PackBuildReport;
import dev.willram.ramcore.resourcepack.ResourcePackAssetId;
import dev.willram.ramcore.resourcepack.ResourcePackBuilder;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatModifier;
import dev.willram.ramcore.stat.StatRegistry;
import dev.willram.ramcore.stat.StatService;
import dev.willram.ramcore.worldinstance.PaperWorldBackend;
import dev.willram.ramcore.worldinstance.WorldInstanceOptions;
import dev.willram.ramcore.worldinstance.WorldInstanceService;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

/**
 * Demonstrates the RamCore gameplay platform: a custom stat, an ability that spends it, an NPC-style
 * dialogue, a throwaway instanced dungeon, and a generated resource pack. Wire-up only — the live
 * DoD smoke tests are recorded in {@code RELEASE_READINESS.md}.
 */
public final class SamplePlugin extends RamPlugin {
    private static final ContentId POWER = ContentId.of("sample", "power");
    private static final ContentId STRIKE = ContentId.of("sample", "strike");
    private static final ContentId GUIDE = ContentId.of("sample", "guide");
    private static final ResourcePackAssetId RUBY_ITEM = ResourcePackAssetId.of("sample", "ruby_sword");

    private final StatRegistry stats = new StatRegistry();
    private final AbilityRegistry abilities = new AbilityRegistry();
    private final DialogueRegistry dialogues = new DialogueRegistry();

    private StatService statService;
    private AbilityService abilityService;
    private WorldInstanceService worldInstances;

    @Override
    public void load() {
        // 3.4 — a custom stat + service (services must be installed from load()).
        this.stats.register(getName(), Stat.of(POWER, 10.0D));
        this.statService = StatService.install(this, this.stats);

        // 3.3 — an ability that spends the stat, targeting what the caster looks at.
        this.abilities.register(getName(), Ability.builder(STRIKE)
                .cooldown(Duration.ofSeconds(3))
                .cost(POWER, 5.0D)
                .targeting(AbilityTargets.lookingAt(20))
                .action(context -> context.targets().forEach(target -> target.setFireTicks(60)))
                .build());
        this.abilityService = AbilityService.install(this, this.abilities, this.statService);

        // 3.5 — a two-node dialogue.
        this.dialogues.register(getName(), Dialogue.builder(GUIDE)
                .node(DialogueNode.builder("root", "<yellow>Welcome, traveller. Care for a quest?")
                        .choice(DialogueChoice.of("<green>Accept", "accept"))
                        .choice(DialogueChoice.of("<red>Not now", null))
                        .build())
                .node(DialogueNode.builder("accept", "<green>Splendid! Slay the dungeon boss.")
                        .action(DialogueActions.message("<gray>(quest objective started)"))
                        .build())
                .build());

        // 3.2 — instanced worlds from plugins/<this>/world-templates/<name>/.
        this.worldInstances = new WorldInstanceService(new PaperWorldBackend(),
                getDataFolder().toPath().resolve("world-templates"));
    }

    @Override
    public void enable() {
        // Grant the stat from equipped items, and bind the ability to hotbar slot 0.
        this.statService.addSource(new dev.willram.ramcore.stat.ItemStatSource());
        this.statService.addSource(player -> List.of(StatModifier.add(POWER, 25.0D, "example:base")));
        this.abilityService.bindHotbar(0, STRIKE);

        // Clean up any dungeons left over from a crash, off-thread.
        Schedulers.runAsync(this.worldInstances::sweepStartup);
    }

    @Override
    public void disable() {
    }

    @Override
    public void registerCommands(@NotNull Commands commands) {
        CommandSpec spec = CommandSpec.command("sample")
                .description("RamCore example commands.")
                .playerOnly()
                .literal("cast", cast -> cast
                        .description("Cast the example ability.")
                        .executes(context -> context.reply(
                                "<gray>" + this.abilityService.cast(context.requirePlayer(), STRIKE, AbilityTrigger.COMMAND).status())))
                .literal("talk", talk -> talk
                        .description("Open the example dialogue.")
                        .executes(context -> DialogueSession.chat(context.requirePlayer(),
                                this.dialogues.require(GUIDE)).start()))
                .literal("dungeon", dungeon -> dungeon
                        .description("Create an instanced dungeon world.")
                        .executes(context -> {
                            context.reply("<gray>Creating dungeon...");
                            this.worldInstances.create("dungeon", WorldInstanceOptions.defaults())
                                    .thenApply(TaskContext.global(), instance -> {
                                        context.reply("<green>Dungeon ready: <white>" + instance.name());
                                        return instance;
                                    });
                        }))
                .literal("pack", pack -> pack
                        .description("Build the example resource pack.")
                        .executesAsync(context -> {
                            try {
                                PackBuildReport report = ResourcePackBuilder.create()
                                        .name("Sample Pack")
                                        .minecraftVersion("1.21.4")
                                        .item(RUBY_ITEM, AssetSource.ofString("<png-bytes>"))
                                        .buildTo(getDataFolder().toPath().resolve("pack.zip"));
                                context.reply("<green>Built pack, sha1=<white>" + report.sha1Hex());
                            } catch (Exception failure) {
                                context.reply("<red>Pack build failed: <white>" + failure.getMessage());
                            }
                        }));
        RamCommands.register(commands, spec);
    }
}
