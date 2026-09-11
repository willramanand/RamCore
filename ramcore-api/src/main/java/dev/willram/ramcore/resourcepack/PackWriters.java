package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Writers for the small JSON files a content-item pack needs: {@code pack.mcmeta}, the modern
 * item-model definition ({@code assets/<ns>/items/<name>.json}), and a basic generated item model
 * ({@code assets/<ns>/models/item/<name>.json}). Each returns JSON as a string;
 * {@link ResourcePackBuilder} wraps them in an {@link AssetSource}.
 *
 * <p>These files are small and fixed-shape, so the JSON is written directly (no Gson dependency),
 * keeping the builder free of server-provided libraries and unit-testable off-server.</p>
 */
public final class PackWriters {

    private PackWriters() {
    }

    /**
     * {@code pack.mcmeta} for the given format and optional description.
     *
     * @param packFormat  the pack format
     * @param description the pack description, or {@code null}
     * @return the JSON
     */
    @NotNull
    public static String packMeta(int packFormat, @Nullable String description) {
        return "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": " + packFormat + ",\n"
                + "    \"description\": " + quote(description == null ? "" : description) + "\n"
                + "  }\n"
                + "}\n";
    }

    /**
     * The modern item-model definition that points a custom item id at a model.
     *
     * @param model the model id (e.g. {@code myplugin:item/ruby_sword})
     * @return the JSON
     */
    @NotNull
    public static String itemModelDefinition(@NotNull ResourcePackAssetId model) {
        requireNonNull(model, "model");
        return "{\n"
                + "  \"model\": {\n"
                + "    \"type\": \"minecraft:model\",\n"
                + "    \"model\": " + quote(reference(model)) + "\n"
                + "  }\n"
                + "}\n";
    }

    /**
     * A basic {@code item/generated} model with a single {@code layer0} texture.
     *
     * @param texture the texture id (e.g. {@code myplugin:item/ruby_sword})
     * @return the JSON
     */
    @NotNull
    public static String basicItemModel(@NotNull ResourcePackAssetId texture) {
        requireNonNull(texture, "texture");
        return "{\n"
                + "  \"parent\": \"minecraft:item/generated\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": " + quote(reference(texture)) + "\n"
                + "  }\n"
                + "}\n";
    }

    @NotNull
    static String reference(@NotNull ResourcePackAssetId id) {
        return id.namespace() + ":" + id.path();
    }

    @NotNull
    static String quote(@NotNull String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
