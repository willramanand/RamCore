package dev.willram.ramcore.resourcepack;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Creates Adventure sounds from resource-pack sound keys.
 */
public final class ResourcePackSounds {

    @NotNull
    public static Key key(@NotNull ResourcePackAssetId soundKey) {
        requireNonNull(soundKey, "soundKey");
        return Key.key(soundKey.namespace(), soundKey.path());
    }

    @NotNull
    public static Sound sound(@NotNull ResourcePackAssetId soundKey, @NotNull Sound.Source source, float volume, float pitch) {
        return Sound.sound(key(soundKey), requireNonNull(source, "source"), volume, pitch);
    }

    @NotNull
    public static Sound sound(@NotNull ResourcePackAsset asset, @NotNull Sound.Source source, float volume, float pitch) {
        ResourcePackAssetId soundKey = requireNonNull(asset, "asset").soundKey();
        if (soundKey == null) {
            soundKey = asset.id();
        }
        return sound(soundKey, source, volume, pitch);
    }

    private ResourcePackSounds() {
    }
}
