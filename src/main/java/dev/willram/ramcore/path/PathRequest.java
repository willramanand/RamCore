package dev.willram.ramcore.path;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable request describing a single managed path run.
 */
public final class PathRequest {
    private static final Consumer<PathProgress> NO_PROGRESS = ignored -> {};
    private static final Consumer<PathTaskResult> NO_RESULT = ignored -> {};

    private final PathDestination destination;
    private final PathOptions options;
    private final Consumer<PathProgress> progressConsumer;
    private final Consumer<PathTaskResult> completionConsumer;
    private final Consumer<PathTaskResult> failureConsumer;
    private final Consumer<PathTaskResult> cancellationConsumer;

    private PathRequest(Builder builder) {
        this.destination = Objects.requireNonNull(builder.destination, "destination");
        this.options = builder.optionsBuilder.build();
        this.progressConsumer = builder.progressConsumer;
        this.completionConsumer = builder.completionConsumer;
        this.failureConsumer = builder.failureConsumer;
        this.cancellationConsumer = builder.cancellationConsumer;
    }

    public static Builder to(@NotNull Location location) {
        return new Builder(PathDestination.location(location));
    }

    public static Builder to(@NotNull Entity entity) {
        return new Builder(PathDestination.entity(entity));
    }

    @NotNull
    public PathDestination destination() {
        return this.destination;
    }

    @NotNull
    public PathOptions options() {
        return this.options;
    }

    void progress(@NotNull PathProgress progress) {
        this.progressConsumer.accept(progress);
    }

    void complete(@NotNull PathTaskResult result) {
        this.completionConsumer.accept(result);
    }

    void fail(@NotNull PathTaskResult result) {
        this.failureConsumer.accept(result);
    }

    void cancel(@NotNull PathTaskResult result) {
        this.cancellationConsumer.accept(result);
    }

    public static final class Builder {
        private final PathDestination destination;
        private final PathOptions.Builder optionsBuilder = PathOptions.builder();
        private Consumer<PathProgress> progressConsumer = NO_PROGRESS;
        private Consumer<PathTaskResult> completionConsumer = NO_RESULT;
        private Consumer<PathTaskResult> failureConsumer = NO_RESULT;
        private Consumer<PathTaskResult> cancellationConsumer = NO_RESULT;

        private Builder(PathDestination destination) {
            this.destination = destination;
        }

        public Builder speed(double speed) {
            this.optionsBuilder.speed(speed);
            return this;
        }

        public Builder maxDistance(double maxDistance) {
            this.optionsBuilder.maxDistance(maxDistance);
            return this;
        }

        public Builder stuckTimeoutTicks(long stuckTimeoutTicks) {
            this.optionsBuilder.stuckTimeoutTicks(stuckTimeoutTicks);
            return this;
        }

        public Builder repathIntervalTicks(long repathIntervalTicks) {
            this.optionsBuilder.repathIntervalTicks(repathIntervalTicks);
            return this;
        }

        public Builder completionDistance(double completionDistance) {
            this.optionsBuilder.completionDistance(completionDistance);
            return this;
        }

        public Builder timeoutTicks(long timeoutTicks) {
            this.optionsBuilder.timeoutTicks(timeoutTicks);
            return this;
        }

        public Builder noTimeout() {
            this.optionsBuilder.noTimeout();
            return this;
        }

        public Builder stuckDistance(double stuckDistance) {
            this.optionsBuilder.stuckDistance(stuckDistance);
            return this;
        }

        public Builder navigationProfile(@NotNull PathNavigationProfile navigationProfile) {
            this.optionsBuilder.navigationProfile(navigationProfile);
            return this;
        }

        public Builder onProgress(@NotNull Consumer<PathProgress> consumer) {
            this.progressConsumer = Objects.requireNonNull(consumer, "consumer");
            return this;
        }

        public Builder onComplete(@NotNull Consumer<PathTaskResult> consumer) {
            this.completionConsumer = Objects.requireNonNull(consumer, "consumer");
            return this;
        }

        public Builder onFailure(@NotNull Consumer<PathTaskResult> consumer) {
            this.failureConsumer = Objects.requireNonNull(consumer, "consumer");
            return this;
        }

        public Builder onCancel(@NotNull Consumer<PathTaskResult> consumer) {
            this.cancellationConsumer = Objects.requireNonNull(consumer, "consumer");
            return this;
        }

        public PathRequest build() {
            return new PathRequest(this);
        }
    }
}
