package dev.willram.ramcore.path;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Tunable behavior for a managed path request.
 */
public record PathOptions(
        double speed,
        double maxDistance,
        long stuckTimeoutTicks,
        long repathIntervalTicks,
        double completionDistance,
        long timeoutTicks,
        double stuckDistance,
        @NotNull PathNavigationProfile navigationProfile
) {
    public static final double UNLIMITED_DISTANCE = Double.POSITIVE_INFINITY;
    public static final long NO_TIMEOUT = Long.MAX_VALUE;

    public PathOptions {
        navigationProfile = Objects.requireNonNull(navigationProfile, "navigationProfile");
        check(speed > 0.0d, "path speed must be greater than zero");
        check(maxDistance > 0.0d || Double.isInfinite(maxDistance), "max distance must be greater than zero or unlimited");
        check(stuckTimeoutTicks > 0L, "stuck timeout ticks must be greater than zero");
        check(repathIntervalTicks > 0L, "repath interval ticks must be greater than zero");
        check(completionDistance >= 0.0d, "completion distance must not be negative");
        check(timeoutTicks > 0L, "timeout ticks must be greater than zero");
        check(stuckDistance >= 0.0d, "stuck distance must not be negative");
    }

    public static PathOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static final class Builder {
        private double speed = 1.0d;
        private double maxDistance = UNLIMITED_DISTANCE;
        private long stuckTimeoutTicks = 60L;
        private long repathIntervalTicks = 20L;
        private double completionDistance = 1.5d;
        private long timeoutTicks = 20L * 30L;
        private double stuckDistance = 0.05d;
        private PathNavigationProfile navigationProfile = PathNavigationProfile.unchanged();

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder maxDistance(double maxDistance) {
            this.maxDistance = maxDistance;
            return this;
        }

        public Builder unlimitedDistance() {
            this.maxDistance = UNLIMITED_DISTANCE;
            return this;
        }

        public Builder stuckTimeoutTicks(long stuckTimeoutTicks) {
            this.stuckTimeoutTicks = stuckTimeoutTicks;
            return this;
        }

        public Builder repathIntervalTicks(long repathIntervalTicks) {
            this.repathIntervalTicks = repathIntervalTicks;
            return this;
        }

        public Builder completionDistance(double completionDistance) {
            this.completionDistance = completionDistance;
            return this;
        }

        public Builder timeoutTicks(long timeoutTicks) {
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        public Builder noTimeout() {
            this.timeoutTicks = NO_TIMEOUT;
            return this;
        }

        public Builder stuckDistance(double stuckDistance) {
            this.stuckDistance = stuckDistance;
            return this;
        }

        public Builder navigationProfile(@NotNull PathNavigationProfile navigationProfile) {
            this.navigationProfile = Objects.requireNonNull(navigationProfile, "navigationProfile");
            return this;
        }

        public PathOptions build() {
            return new PathOptions(
                    this.speed,
                    this.maxDistance,
                    this.stuckTimeoutTicks,
                    this.repathIntervalTicks,
                    this.completionDistance,
                    this.timeoutTicks,
                    this.stuckDistance,
                    this.navigationProfile
            );
        }
    }
}
