package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Reads and writes repository items.
 */
public interface DataSerializer<V extends DataItem> {

    @NotNull
    V read(@NotNull Reader reader) throws IOException;

    void write(@NotNull Writer writer, @NotNull V item) throws IOException;
}
