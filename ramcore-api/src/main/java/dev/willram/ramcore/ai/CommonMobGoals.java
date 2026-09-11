package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.GoalType;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reusable custom goals built on Paper pathfinding and entity APIs.
 */
public final class CommonMobGoals {

    @NotNull
    public static <T extends Mob> RamMobGoal<T> followEntity(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                             @NotNull Supplier<? extends Entity> target,
                                                             double speed, double minDistance, double maxDistance) {
        double minSquared = minDistance * minDistance;
        double maxSquared = maxDistance * maxDistance;
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> inRange(mob, target.get(), maxSquared))
                .stayActiveWhen(() -> inRange(mob, target.get(), maxSquared))
                .onTick(() -> {
                    Entity entity = target.get();
                    if (entity != null && sameWorld(mob, entity) && mob.getLocation().distanceSquared(entity.getLocation()) > minSquared) {
                        mob.getPathfinder().moveTo(entity, speed);
                    }
                })
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> guardArea(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                          @NotNull Location center, double radius, double speed) {
        double radiusSquared = radius * radius;
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> sameWorld(mob.getLocation(), center) && mob.getLocation().distanceSquared(center) > radiusSquared)
                .stayActiveWhen(() -> sameWorld(mob.getLocation(), center) && mob.getLocation().distanceSquared(center) > radiusSquared)
                .onTick(() -> mob.getPathfinder().moveTo(center, speed))
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> patrol(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                       @NotNull List<Location> waypoints, double speed, double closeDistance) {
        List<Location> points = List.copyOf(Objects.requireNonNull(waypoints, "waypoints"));
        AtomicInteger index = new AtomicInteger();
        double closeSquared = closeDistance * closeDistance;
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> !points.isEmpty())
                .stayActiveWhen(() -> !points.isEmpty())
                .onTick(() -> {
                    Location waypoint = points.get(index.get());
                    if (!sameWorld(mob.getLocation(), waypoint)) {
                        return;
                    }
                    if (mob.getLocation().distanceSquared(waypoint) <= closeSquared) {
                        waypoint = points.get(index.updateAndGet(current -> (current + 1) % points.size()));
                    }
                    mob.getPathfinder().moveTo(waypoint, speed);
                })
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> fleeFrom(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                         @NotNull Supplier<? extends Entity> threat,
                                                         double speed, double triggerDistance, double fleeDistance) {
        double triggerSquared = triggerDistance * triggerDistance;
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> inRange(mob, threat.get(), triggerSquared))
                .stayActiveWhen(() -> inRange(mob, threat.get(), triggerSquared))
                .onTick(() -> {
                    Entity entity = threat.get();
                    if (entity == null || !sameWorld(mob, entity)) {
                        return;
                    }
                    Vector away = mob.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(fleeDistance);
                    mob.getPathfinder().moveTo(mob.getLocation().clone().add(away), speed);
                })
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> attackTarget(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                             @NotNull Supplier<? extends LivingEntity> target) {
        return builder.types(GoalType.TARGET)
                .activateWhen(() -> target.get() != null)
                .stayActiveWhen(() -> target.get() != null)
                .onStart(() -> mob.setTarget(target.get()))
                .onTick(() -> mob.setTarget(target.get()))
                .onStop(() -> mob.setTarget(null))
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> lookAt(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                       @NotNull Supplier<? extends Entity> target) {
        return builder.types(GoalType.LOOK)
                .activateWhen(() -> target.get() != null)
                .stayActiveWhen(() -> target.get() != null)
                .onTick(() -> {
                    Entity entity = target.get();
                    if (entity != null && sameWorld(mob, entity)) {
                        Location location = entity.getLocation();
                        mob.lookAt(location.getX(), location.getY(), location.getZ(), LookAnchor.EYES);
                    }
                })
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> returnHome(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                           @NotNull Location home, double speed, double distance) {
        return guardArea(mob, builder, home, distance, speed);
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> leashToRegion(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                              @NotNull Predicate<Location> allowedRegion,
                                                              @NotNull Location returnPoint, double speed) {
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> !allowedRegion.test(mob.getLocation()))
                .stayActiveWhen(() -> !allowedRegion.test(mob.getLocation()))
                .onTick(() -> mob.getPathfinder().moveTo(returnPoint, speed))
                .build();
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> idleAnimation(@NotNull RamMobGoalBuilder<T> builder, @NotNull Runnable tick) {
        return builder.types(GoalType.LOOK, GoalType.UNKNOWN_BEHAVIOR)
                .activateWhen(() -> true)
                .stayActiveWhen(() -> true)
                .onTick(tick)
                .build();
    }

    private static boolean inRange(Entity source, Entity target, double maxDistanceSquared) {
        return target != null && sameWorld(source, target)
                && source.getLocation().distanceSquared(target.getLocation()) <= maxDistanceSquared;
    }

    private static boolean sameWorld(Entity first, Entity second) {
        return first.getWorld().equals(second.getWorld());
    }

    private static boolean sameWorld(Location first, Location second) {
        return Objects.equals(first.getWorld(), second.getWorld());
    }

    private CommonMobGoals() {
    }
}
