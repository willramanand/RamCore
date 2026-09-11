package dev.willram.ramcore.diagnostics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JVM memory snapshot for lightweight debug exports.
 */
public record DiagnosticMemorySnapshot(long maxBytes, long totalBytes, long freeBytes) {

    @NotNull
    public static DiagnosticMemorySnapshot capture() {
        Runtime runtime = Runtime.getRuntime();
        return new DiagnosticMemorySnapshot(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory());
    }

    public long usedBytes() {
        return this.totalBytes - this.freeBytes;
    }

    @NotNull
    public List<String> lines() {
        return List.of(
                "memory.used=" + mb(usedBytes()) + " MiB",
                "memory.free=" + mb(this.freeBytes) + " MiB",
                "memory.total=" + mb(this.totalBytes) + " MiB",
                "memory.max=" + mb(this.maxBytes) + " MiB",
                "cache.registry=none"
        );
    }

    private static long mb(long bytes) {
        return bytes / (1024L * 1024L);
    }
}
