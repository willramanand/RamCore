package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Result of one startup self-test.
 */
public record NmsSelfTestResult(
        @NotNull NmsSelfTestCase test,
        @NotNull NmsCapabilityCheck check
) {

    public NmsSelfTestResult {
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(check, "check");
    }

    public boolean passed() {
        return !this.test.required() || this.check.usable();
    }

    @NotNull
    public String line() {
        return (passed() ? "PASS" : "FAIL") + " " + this.test.id() + " -> "
                + this.check.status() + " via " + this.check.adapterId() + ": " + this.check.reason();
    }
}
