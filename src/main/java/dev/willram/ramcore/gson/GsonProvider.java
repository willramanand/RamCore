/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.willram.ramcore.gson;

import com.google.gson.*;
import dev.willram.ramcore.datatree.DataTree;
import dev.willram.ramcore.gson.typeadapters.BukkitSerializableAdapterFactory;
import dev.willram.ramcore.gson.typeadapters.GsonSerializableAdapterFactory;
import dev.willram.ramcore.gson.typeadapters.JsonElementTreeSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.jetbrains.annotations.NotNull;
import java.io.Reader;
import java.util.Objects;

/**
 * Provides static instances of Gson
 */
public final class GsonProvider {

    private static final Gson STANDARD_GSON = GsonComponentSerializer
            .builder()
            .build()
            .populator()
            .apply(
                    new GsonBuilder()
                    .registerTypeHierarchyAdapter(DataTree.class, JsonElementTreeSerializer.INSTANCE)
                    .registerTypeAdapterFactory(GsonSerializableAdapterFactory.INSTANCE)
                    .registerTypeAdapterFactory(BukkitSerializableAdapterFactory.INSTANCE)
                    .serializeNulls()
                    .disableHtmlEscaping()
            ).create();

    private static final Gson PRETTY_PRINT_GSON = GsonComponentSerializer
            .builder()
            .build()
            .populator()
            .apply(
                    new GsonBuilder()
                            .registerTypeHierarchyAdapter(DataTree.class, JsonElementTreeSerializer.INSTANCE)
                            .registerTypeAdapterFactory(GsonSerializableAdapterFactory.INSTANCE)
                            .registerTypeAdapterFactory(BukkitSerializableAdapterFactory.INSTANCE)
                            .serializeNulls()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
            ).create();


    @NotNull
    public static Gson standard() {
        return STANDARD_GSON;
    }

    @NotNull
    public static Gson prettyPrinting() {
        return PRETTY_PRINT_GSON;
    }

    @NotNull
    public static JsonObject readObject(@NotNull Reader reader) {
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    @NotNull
    public static JsonObject readObject(@NotNull String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    public static void writeObject(@NotNull Appendable writer, @NotNull JsonObject object) {
        standard().toJson(object, writer);
    }

    public static void writeObjectPretty(@NotNull Appendable writer, @NotNull JsonObject object) {
        prettyPrinting().toJson(object, writer);
    }

    public static void writeElement(@NotNull Appendable writer, @NotNull JsonElement element) {
        standard().toJson(element, writer);
    }

    public static void writeElementPretty(@NotNull Appendable writer, @NotNull JsonElement element) {
        prettyPrinting().toJson(element, writer);
    }

    @NotNull
    public static String toString(@NotNull JsonElement element) {
        return Objects.requireNonNull(standard().toJson(element));
    }

    @NotNull
    public static String toStringPretty(@NotNull JsonElement element) {
        return Objects.requireNonNull(prettyPrinting().toJson(element));
    }

    private GsonProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    @NotNull
    @Deprecated
    public static Gson get() {
        return standard();
    }

    @NotNull
    @Deprecated
    public static Gson getPrettyPrinting() {
        return prettyPrinting();
    }

}
