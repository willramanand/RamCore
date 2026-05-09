package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One startup self-test for a capability.
 */
public record NmsSelfTestCase(
        @NotNull String id,
        @NotNull NmsCapability capability,
        boolean required,
        @NotNull String expectation
) {

    public NmsSelfTestCase {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(expectation, "expectation");
        if (id.isBlank()) {
            throw new IllegalArgumentException("self-test id must not be blank");
        }
    }

    @NotNull
    public static NmsSelfTestCase optional(@NotNull String id, @NotNull NmsCapability capability, @NotNull String expectation) {
        return new NmsSelfTestCase(id, capability, false, expectation);
    }

    @NotNull
    public static NmsSelfTestCase required(@NotNull String id, @NotNull NmsCapability capability, @NotNull String expectation) {
        return new NmsSelfTestCase(id, capability, true, expectation);
    }
}
