package dev.willram.ramcore.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Color;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Text display entity configuration.
 */
public final class TextDisplaySpec implements DisplaySpec<TextDisplay> {
    private final DisplayOptions options = new DisplayOptions();
    private Component text = Component.empty();
    private Integer lineWidth;
    private Color backgroundColor;
    private Byte textOpacity;
    private Boolean shadowed;
    private Boolean seeThrough;
    private Boolean defaultBackground;
    private TextDisplay.TextAlignment alignment;

    @NotNull
    public static TextDisplaySpec text(@NotNull ComponentLike text) {
        return new TextDisplaySpec().content(text);
    }

    @NotNull
    public TextDisplaySpec content(@NotNull ComponentLike text) {
        this.text = requireNonNull(text, "text").asComponent();
        return this;
    }

    @NotNull
    public TextDisplaySpec lineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    @NotNull
    public TextDisplaySpec background(@NotNull Color color) {
        this.backgroundColor = color;
        return this;
    }

    @NotNull
    public TextDisplaySpec opacity(byte opacity) {
        this.textOpacity = opacity;
        return this;
    }

    @NotNull
    public TextDisplaySpec shadowed(boolean shadowed) {
        this.shadowed = shadowed;
        return this;
    }

    @NotNull
    public TextDisplaySpec seeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        return this;
    }

    @NotNull
    public TextDisplaySpec defaultBackground(boolean defaultBackground) {
        this.defaultBackground = defaultBackground;
        return this;
    }

    @NotNull
    public TextDisplaySpec alignment(@NotNull TextDisplay.TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @NotNull
    public DisplayOptions options() {
        return this.options;
    }

    @NotNull
    @Override
    public Class<TextDisplay> type() {
        return TextDisplay.class;
    }

    @Override
    public void apply(@NotNull TextDisplay display) {
        this.options.apply(display);
        display.text(this.text);
        if (this.lineWidth != null) {
            display.setLineWidth(this.lineWidth);
        }
        if (this.backgroundColor != null) {
            display.setBackgroundColor(this.backgroundColor);
        }
        if (this.textOpacity != null) {
            display.setTextOpacity(this.textOpacity);
        }
        if (this.shadowed != null) {
            display.setShadowed(this.shadowed);
        }
        if (this.seeThrough != null) {
            display.setSeeThrough(this.seeThrough);
        }
        if (this.defaultBackground != null) {
            display.setDefaultBackground(this.defaultBackground);
        }
        if (this.alignment != null) {
            display.setAlignment(this.alignment);
        }
    }
}
