package dev.willram.ramcore.path;

import org.jetbrains.annotations.Nullable;

/**
 * Paper-exposed navigation toggles for a path request.
 *
 * <p>Node penalties, lava behavior, flying/swimming navigation, and custom move
 * controls require version-specific adapters and are intentionally not exposed
 * here until a guarded NMS implementation is available.</p>
 */
public record PathNavigationProfile(
        @Nullable Boolean canOpenDoors,
        @Nullable Boolean canPassDoors,
        @Nullable Boolean canFloat
) {

    public static PathNavigationProfile unchanged() {
        return new PathNavigationProfile(null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasChanges() {
        return this.canOpenDoors != null || this.canPassDoors != null || this.canFloat != null;
    }

    public static final class Builder {
        private Boolean canOpenDoors;
        private Boolean canPassDoors;
        private Boolean canFloat;

        public Builder canOpenDoors(boolean canOpenDoors) {
            this.canOpenDoors = canOpenDoors;
            return this;
        }

        public Builder canPassDoors(boolean canPassDoors) {
            this.canPassDoors = canPassDoors;
            return this;
        }

        public Builder canFloat(boolean canFloat) {
            this.canFloat = canFloat;
            return this;
        }

        public PathNavigationProfile build() {
            return new PathNavigationProfile(this.canOpenDoors, this.canPassDoors, this.canFloat);
        }
    }
}
