package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Identifies one subject's progress on one objective.
 *
 * @param subject     the subject
 * @param objectiveId the objective
 */
public record ObjectiveProgressKey(@NotNull ObjectiveSubject subject, @NotNull ContentId objectiveId) {
    private static final String SEPARATOR = "/";

    public ObjectiveProgressKey {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(objectiveId, "objectiveId");
    }

    /**
     * Key codec for file and SQL backends: {@code <subjectType>/<subjectId>/<namespace:value>}.
     * The slash never appears in any of the parts, which are restricted to {@code [a-z0-9_.:-]}.
     */
    @NotNull
    public static DataKeyCodec<ObjectiveProgressKey> keyCodec() {
        return new DataKeyCodec<>() {
            @Override
            public @NotNull String encode(@NotNull ObjectiveProgressKey key) {
                return key.subject().type() + SEPARATOR + key.subject().id() + SEPARATOR + key.objectiveId();
            }

            @Override
            public @NotNull ObjectiveProgressKey decode(@NotNull String value) {
                String[] parts = value.split(SEPARATOR, 3);
                RamPreconditions.checkArgument(parts.length == 3,
                        "malformed objective progress key: " + value,
                        "Keys are written as <subjectType>/<subjectId>/<namespace:value>; do not hand-edit store files.");
                return new ObjectiveProgressKey(ObjectiveSubject.of(parts[0], parts[1]), ContentId.parse(parts[2]));
            }
        };
    }
}
