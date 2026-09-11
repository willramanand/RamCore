package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Startup self-test plan for NMS capabilities.
 */
public final class NmsSelfTestPlan {
    private final List<NmsSelfTestCase> tests;

    private NmsSelfTestPlan(List<NmsSelfTestCase> tests) {
        this.tests = List.copyOf(tests);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public static NmsSelfTestPlan optionalAllCapabilities() {
        Builder builder = builder();
        for (NmsCapability capability : NmsCapability.values()) {
            builder.optional(capability.key(), capability, "Capability should report an explicit status.");
        }
        return builder.build();
    }

    @NotNull
    public List<NmsSelfTestCase> tests() {
        return this.tests;
    }

    @NotNull
    public NmsSelfTestReport run(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        List<NmsSelfTestResult> results = new ArrayList<>();
        for (NmsSelfTestCase test : this.tests) {
            results.add(new NmsSelfTestResult(test, registry.check(test.capability())));
        }
        return new NmsSelfTestReport(results);
    }

    public static final class Builder {
        private final List<NmsSelfTestCase> tests = new ArrayList<>();

        private Builder() {
        }

        @NotNull
        public Builder optional(@NotNull String id, @NotNull NmsCapability capability, @NotNull String expectation) {
            this.tests.add(NmsSelfTestCase.optional(id, capability, expectation));
            return this;
        }

        @NotNull
        public Builder required(@NotNull String id, @NotNull NmsCapability capability, @NotNull String expectation) {
            this.tests.add(NmsSelfTestCase.required(id, capability, expectation));
            return this;
        }

        @NotNull
        public NmsSelfTestPlan build() {
            return new NmsSelfTestPlan(this.tests);
        }
    }
}
