package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Startup self-test report for reflective handles and adapters.
 */
public record NmsSelfTestReport(@NotNull List<NmsSelfTestResult> results) {

    public NmsSelfTestReport {
        results = List.copyOf(Objects.requireNonNull(results, "results"));
    }

    public boolean passed() {
        return this.results.stream().allMatch(NmsSelfTestResult::passed);
    }

    @NotNull
    public List<NmsSelfTestResult> failures() {
        return this.results.stream().filter(result -> !result.passed()).toList();
    }

    @NotNull
    public List<String> lines() {
        return this.results.stream().map(NmsSelfTestResult::line).toList();
    }
}
