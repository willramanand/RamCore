package dev.willram.ramcore.ability;

import dev.willram.ramcore.commands.CommandArgument;
import dev.willram.ramcore.commands.CommandModule;
import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.commands.RamArguments;
import dev.willram.ramcore.content.ContentId;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * The command trigger: {@code /<label> <ability>} casts the named ability for the sender with
 * {@link AbilityTrigger#COMMAND}. Register it from {@link dev.willram.ramcore.RamPlugin#registerCommands}.
 */
public final class AbilityCommandModule implements CommandModule {
    private static final CommandArgument<String> ABILITY = RamArguments.string("ability");

    private final AbilityService service;
    private final String label;

    public AbilityCommandModule(@NotNull AbilityService service) {
        this(service, "cast");
    }

    public AbilityCommandModule(@NotNull AbilityService service, @NotNull String label) {
        this.service = requireNonNull(service, "service");
        this.label = requireNonNull(label, "label");
    }

    @Override
    @NotNull
    public Collection<CommandSpec> commands() {
        CommandSpec spec = CommandSpec.command(this.label)
                .description("Cast an ability by id.")
                .playerOnly()
                .argument(ABILITY, ability -> ability.executes(context -> {
                    Player player = context.requirePlayer();
                    String raw = context.get(ABILITY);
                    ContentId id;
                    try {
                        id = ContentId.parse(raw);
                    } catch (RuntimeException invalid) {
                        context.reply("<red>Invalid ability id: <white>" + raw);
                        return;
                    }
                    if (!this.service.registry().contains(id)) {
                        context.reply("<red>Unknown ability: <white>" + id);
                        return;
                    }
                    CastResult result = this.service.cast(player, id, AbilityTrigger.COMMAND);
                    context.reply(message(result));
                }));
        return List.of(spec);
    }

    private static String message(@NotNull CastResult result) {
        return switch (result.status()) {
            case CAST -> "<green>Cast <white>" + result.abilityId() + "</white>.";
            case CASTING -> "<yellow>Casting <white>" + result.abilityId() + "</white>...";
            case ON_COOLDOWN -> "<red>On cooldown for <white>"
                    + (result.remainingMillis() / 1000L + 1L) + "s</white>.";
            case INSUFFICIENT_COST -> "<red>Not enough resources to cast <white>" + result.abilityId() + "</white>.";
            case INVALID -> "<red>Cannot cast: <white>" + String.join(", ", result.errors());
            case BUSY -> "<red>You are already casting.";
        };
    }
}
