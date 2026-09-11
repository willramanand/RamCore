package dev.willram.ramcore.display;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

/**
 * Shared display entity options.
 */
public final class DisplayOptions {
    private Display.Billboard billboard;
    private Display.Brightness brightness;
    private Color glowColor;
    private Float viewRange;
    private Float shadowRadius;
    private Float shadowStrength;
    private Float displayWidth;
    private Float displayHeight;
    private Integer interpolationDelay;
    private Integer interpolationDuration;
    private Integer teleportDuration;

    @NotNull
    public DisplayOptions billboard(@NotNull Display.Billboard billboard) {
        this.billboard = billboard;
        return this;
    }

    @NotNull
    public DisplayOptions brightness(@NotNull Display.Brightness brightness) {
        this.brightness = brightness;
        return this;
    }

    @NotNull
    public DisplayOptions glowColor(@NotNull Color glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    @NotNull
    public DisplayOptions viewRange(float viewRange) {
        this.viewRange = viewRange;
        return this;
    }

    @NotNull
    public DisplayOptions shadow(float radius, float strength) {
        this.shadowRadius = radius;
        this.shadowStrength = strength;
        return this;
    }

    @NotNull
    public DisplayOptions size(float width, float height) {
        this.displayWidth = width;
        this.displayHeight = height;
        return this;
    }

    @NotNull
    public DisplayOptions interpolation(int delayTicks, int durationTicks) {
        this.interpolationDelay = delayTicks;
        this.interpolationDuration = durationTicks;
        return this;
    }

    @NotNull
    public DisplayOptions teleportDuration(int ticks) {
        this.teleportDuration = ticks;
        return this;
    }

    public void apply(@NotNull Display display) {
        if (this.billboard != null) {
            display.setBillboard(this.billboard);
        }
        if (this.brightness != null) {
            display.setBrightness(this.brightness);
        }
        if (this.glowColor != null) {
            display.setGlowColorOverride(this.glowColor);
        }
        if (this.viewRange != null) {
            display.setViewRange(this.viewRange);
        }
        if (this.shadowRadius != null) {
            display.setShadowRadius(this.shadowRadius);
        }
        if (this.shadowStrength != null) {
            display.setShadowStrength(this.shadowStrength);
        }
        if (this.displayWidth != null) {
            display.setDisplayWidth(this.displayWidth);
        }
        if (this.displayHeight != null) {
            display.setDisplayHeight(this.displayHeight);
        }
        if (this.interpolationDelay != null) {
            display.setInterpolationDelay(this.interpolationDelay);
        }
        if (this.interpolationDuration != null) {
            display.setInterpolationDuration(this.interpolationDuration);
        }
        if (this.teleportDuration != null) {
            display.setTeleportDuration(this.teleportDuration);
        }
    }
}
