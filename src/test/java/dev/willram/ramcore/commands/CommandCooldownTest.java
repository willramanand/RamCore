package dev.willram.ramcore.commands;

import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.exception.ApiMisuseException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("UnstableApiUsage")
public final class CommandCooldownTest {

    @Test
    public void perSenderRejectsSecondUseWithinCooldown() throws Exception {
        CommandCooldown cooldown = CommandCooldown.perSender(Cooldown.of(1, TimeUnit.MINUTES));
        CommandContext context = context("tester");

        cooldown.check(context);

        try {
            cooldown.check(context);
        } catch (CommandInterruptException e) {
            assertTrue(e.getAction() != null);
            return;
        }

        throw new AssertionError("expected CommandInterruptException");
    }

    @Test
    public void customCooldownMessageReceivesRemainingTime() throws Exception {
        CommandCooldown cooldown = CommandCooldown.perSender(Cooldown.of(1, TimeUnit.MINUTES))
                .message((context, remainingMillis) -> "<red>Wait " + remainingMillis);
        CommandContext context = context("tester");

        cooldown.check(context);

        try {
            cooldown.check(context);
        } catch (CommandInterruptException e) {
            CapturingSender sender = new CapturingSender();
            e.getAction().accept(sender.proxy());
            assertTrue(sender.lastMessage.startsWith("<red>Wait "));
            return;
        }

        throw new AssertionError("expected CommandInterruptException");
    }

    @Test
    public void nullCooldownKeyFailsWithActionableMisuseException() {
        CommandCooldown cooldown = CommandCooldown.keyed(Cooldown.of(1, TimeUnit.MINUTES), context -> null);

        try {
            cooldown.check(context("tester"));
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("cooldown key"));
            assertTrue(e.fix().contains("non-null key"));
            return;
        } catch (CommandInterruptException e) {
            throw new AssertionError("expected ApiMisuseException", e);
        }

        throw new AssertionError("expected ApiMisuseException");
    }

    private static CommandContext context(String name) {
        CommandSender sender = sender(name);
        CommandSourceStack stack = (CommandSourceStack) Proxy.newProxyInstance(
                CommandCooldownTest.class.getClassLoader(),
                new Class<?>[]{CommandSourceStack.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSender" -> sender;
                    case "withLocation", "withExecutor" -> proxy;
                    default -> null;
                }
        );
        return new CommandContext(stack);
    }

    private static CommandSender sender(String name) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandCooldownTest.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return name;
                    }

                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == void.class) {
                        return null;
                    }
                    return null;
                }
        );
    }

    private static final class CapturingSender {
        private String lastMessage = "";

        private CommandSender proxy() {
            return (CommandSender) Proxy.newProxyInstance(
                    CommandCooldownTest.class.getClassLoader(),
                    new Class<?>[]{CommandSender.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("sendRichMessage") && args != null && args.length == 1) {
                            this.lastMessage = (String) args[0];
                            return null;
                        }

                        if (method.getName().equals("getName")) {
                            return "capture";
                        }

                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) {
                            return false;
                        }
                        if (returnType == int.class) {
                            return 0;
                        }
                        if (returnType == void.class) {
                            return null;
                        }
                        return null;
                    }
            );
        }
    }
}
