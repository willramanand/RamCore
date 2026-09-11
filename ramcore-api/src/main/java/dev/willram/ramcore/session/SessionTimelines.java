package dev.willram.ramcore.session;

import org.jetbrains.annotations.NotNull;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Formats a session timeline into plain, paste-safe lines. Run the result through
 * {@code DiagnosticExporter.safeLines} before display to redact anything sensitive in details.
 */
public final class SessionTimelines {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private SessionTimelines() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * One line per event: {@code HH:mm:ss tick=<t> <TYPE> <detail>}.
     *
     * @param events the events
     * @return formatted lines
     */
    @NotNull
    public static List<String> lines(@NotNull List<SessionEvent> events) {
        requireNonNull(events, "events");
        return events.stream()
                .map(event -> TIME.format(event.at()) + " tick=" + event.tick() + " " + event.type() + " " + event.detail())
                .toList();
    }
}
