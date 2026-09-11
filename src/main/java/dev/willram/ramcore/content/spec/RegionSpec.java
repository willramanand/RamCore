package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.region.RegionAction;
import dev.willram.ramcore.region.RegionRule;
import dev.willram.ramcore.region.RegionRuleResult;
import dev.willram.ramcore.region.RegionShape;
import dev.willram.ramcore.region.RegionShapes;
import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.serialize.Region;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A pure description of a region: a {@link RegionShape} (cuboid or sphere), a priority, and its
 * rules. Off-server; {@code ContentRegistrar.toRuleRegion} turns it into a {@code RuleRegion}.
 *
 * @param shape    the containment shape
 * @param priority the region priority
 * @param rules    the rules
 */
public record RegionSpec(@NotNull RegionShape shape, int priority, @NotNull List<RegionRule> rules) {

    public RegionSpec {
        requireNonNull(shape, "shape");
        rules = List.copyOf(rules);
    }

    @NotNull
    public static RegionSpec deserialize(@NotNull ConfigurationNode node) {
        requireNonNull(node, "node");
        int priority = node.node("priority").getInt(0);

        ConfigurationNode shapeNode = node.node("shape");
        String shapeType = shapeNode.node("type").getString("cuboid").trim().toLowerCase();
        RegionShape shape = switch (shapeType) {
            case "cuboid" -> RegionShapes.cuboid(Region.of(position(shapeNode.node("min")), position(shapeNode.node("max"))));
            case "sphere" -> {
                double radius = shapeNode.node("radius").getDouble(0);
                if (radius <= 0) {
                    throw new ContentDeserializeException("sphere shape needs a positive 'radius'");
                }
                yield RegionShapes.sphere(position(shapeNode.node("center")), radius);
            }
            default -> throw new ContentDeserializeException("unknown region shape type '" + shapeType + "'");
        };

        List<RegionRule> rules = new ArrayList<>();
        for (ConfigurationNode ruleNode : node.node("rules").childrenList()) {
            rules.add(rule(ruleNode));
        }

        return new RegionSpec(shape, priority, rules);
    }

    private static RegionRule rule(@NotNull ConfigurationNode node) {
        String id = node.node("id").getString();
        if (id == null || id.isBlank()) {
            throw new ContentDeserializeException("region rule is missing 'id'");
        }
        RegionAction action = enumValue(RegionAction.class, node.node("action").getString(), "action");
        RegionRuleResult result = enumValue(RegionRuleResult.class, node.node("result").getString(), "result");
        int priority = node.node("priority").getInt(0);
        return RegionRule.of(id, action, priority, result);
    }

    private static <E extends Enum<E>> E enumValue(@NotNull Class<E> type, String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new ContentDeserializeException("region rule is missing '" + field + "'");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException unknown) {
            throw new ContentDeserializeException("unknown " + field + " '" + value + "'");
        }
    }

    private static Position position(@NotNull ConfigurationNode node) {
        if (node.virtual()) {
            throw new ContentDeserializeException("missing position");
        }
        String world = node.node("world").getString("world");
        return Position.of(node.node("x").getDouble(), node.node("y").getDouble(), node.node("z").getDouble(), world);
    }
}
