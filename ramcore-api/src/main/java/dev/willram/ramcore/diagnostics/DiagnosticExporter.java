package dev.willram.ramcore.diagnostics;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Builds paste-safe text exports with conservative redaction and line limits.
 */
public final class DiagnosticExporter {
    private static final int MAX_LINE_LENGTH = 240;
    private static final Set<String> SENSITIVE_TOKENS = Set.of(
            "token",
            "secret",
            "password",
            "passwd",
            "apikey",
            "api_key",
            "authorization",
            "bearer"
    );

    @NotNull
    public static List<String> safeLines(@NotNull List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        return lines.stream()
                .map(DiagnosticExporter::redact)
                .map(DiagnosticExporter::truncate)
                .toList();
    }

    @NotNull
    public static String safeText(@NotNull List<String> lines) {
        return String.join(System.lineSeparator(), safeLines(lines));
    }

    @NotNull
    public static String redact(@NotNull String line) {
        Objects.requireNonNull(line, "line");
        String lower = line.toLowerCase(Locale.ROOT);
        for (String token : SENSITIVE_TOKENS) {
            if (lower.contains(token)) {
                int separator = firstSeparator(line);
                if (separator >= 0) {
                    return line.substring(0, separator + 1) + " <redacted>";
                }
                return "<redacted>";
            }
        }
        return line;
    }

    private static int firstSeparator(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }

    private static String truncate(String line) {
        if (line.length() <= MAX_LINE_LENGTH) {
            return line;
        }
        return line.substring(0, MAX_LINE_LENGTH - 14) + "... <truncated>";
    }

    private DiagnosticExporter() {
    }
}
