package dev.willram.ramcore.diagnostics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Extension point for modules that can expose validation or preview diagnostics.
 */
public interface DiagnosticProvider {

    @NotNull
    String id();

    @NotNull
    String category();

    @NotNull
    default String description() {
        return "";
    }

    @NotNull
    List<String> lines();
}
