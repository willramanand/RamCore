package dev.willram.ramcore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("UnstableApiUsage")
public final class CommandSpecTest {

    @Test
    public void buildAttachesConfiguredChildren() {
        CommandArgument<Integer> amount = RamArguments.integer("amount", 1, 10);
        CommandSpec spec = RamCommands.command("root").withHelp();

        spec.literal("child", child -> child
                .argument(amount, argument -> argument
                        .executes(context -> {
                        })));

        spec.root().thenBrigadier(Commands.literal("raw")
                .executes(context -> Command.SINGLE_SUCCESS));

        LiteralCommandNode<CommandSourceStack> root = spec.build();
        CommandNode<CommandSourceStack> help = root.getChild("help");
        CommandNode<CommandSourceStack> child = root.getChild("child");
        CommandNode<CommandSourceStack> raw = root.getChild("raw");

        assertNotNull(help, "help literal should be attached after configuration");
        assertNotNull(help.getCommand(), "help literal should be executable");
        assertNotNull(child, "child literal should be attached after configuration");
        assertNotNull(child.getCommand(), "incomplete child literal should have a missing argument fallback");
        assertNotNull(raw, "raw Brigadier literal should be attached");
        assertNotNull(raw.getCommand(), "raw Brigadier literal should be executable");

        CommandNode<CommandSourceStack> amountArgument = child.getChild("amount");
        assertNotNull(amountArgument, "nested argument should be attached after configuration");
        assertNotNull(amountArgument.getCommand(), "nested argument should be executable");
    }

    @Test
    public void builtNodesRejectLateMutation() {
        CommandSpec spec = RamCommands.command("root");
        CommandSpec.Node child = spec.literal("child");

        spec.build();

        assertThrows(IllegalStateException.class, () -> child.literal("late"));
    }
}
