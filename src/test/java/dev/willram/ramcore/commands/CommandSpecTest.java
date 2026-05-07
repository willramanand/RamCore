package dev.willram.ramcore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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

        assertNotNull("help literal should be attached after configuration", help);
        assertNotNull("help literal should be executable", help.getCommand());
        assertNotNull("child literal should be attached after configuration", child);
        assertNotNull("incomplete child literal should have a missing argument fallback", child.getCommand());
        assertNotNull("raw Brigadier literal should be attached", raw);
        assertNotNull("raw Brigadier literal should be executable", raw.getCommand());

        CommandNode<CommandSourceStack> amountArgument = child.getChild("amount");
        assertNotNull("nested argument should be attached after configuration", amountArgument);
        assertNotNull("nested argument should be executable", amountArgument.getCommand());
    }

    @Test(expected = IllegalStateException.class)
    public void builtNodesRejectLateMutation() {
        CommandSpec spec = RamCommands.command("root");
        CommandSpec.Node child = spec.literal("child");

        spec.build();

        child.literal("late");
    }
}
