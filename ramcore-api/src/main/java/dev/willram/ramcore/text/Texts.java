package dev.willram.ramcore.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

/**
 * Facade for common text formatting operations.
 */
public final class Texts {
    private static final TextFormatter STANDARD = TextFormatter.create(MiniMessage.miniMessage());
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");

    @NotNull
    public static TextFormatter formatter() {
        return STANDARD;
    }

    @NotNull
    public static TextFormatter formatter(@NotNull MiniMessage miniMessage) {
        return TextFormatter.create(miniMessage);
    }

    @NotNull
    public static TextContext.Builder context() {
        return TextContext.builder();
    }

    @NotNull
    public static Component render(@NotNull String template) {
        return STANDARD.component(template);
    }

    @NotNull
    public static Component render(@NotNull String template, @NotNull TextContext context) {
        return STANDARD.component(template, context);
    }

    @NotNull
    public static Component item(@NotNull String template, @NotNull TextContext context) {
        return STANDARD.item(template, context);
    }

    @NotNull
    public static String plain(@NotNull String template) {
        return STANDARD.plain(template);
    }

    @NotNull
    public static String plain(@NotNull String template, @NotNull TextContext context) {
        return STANDARD.plain(template, context);
    }

    @NotNull
    public static List<String> plainLines(@NotNull Collection<String> templates, @NotNull TextContext context) {
        return STANDARD.plainLines(templates, context);
    }

    @NotNull
    static String plainNumber(double value) {
        return COORDINATE_FORMAT.format(value);
    }

    private Texts() {
    }
}
