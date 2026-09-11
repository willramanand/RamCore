package dev.willram.ramcore.content;

import dev.willram.ramcore.ability.Ability;
import dev.willram.ramcore.content.spec.AbilitySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilitySpecTest {

    private SpecLoader loader() {
        return SpecLoader.create().deserializer("abilities", AbilitySpec::deserialize);
    }

    private static void write(Path root, String name, String... lines) throws Exception {
        Path dir = root.resolve("abilities");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void roundTripWithCostAndTargeting(@TempDir Path root) throws Exception {
        write(root, "fireball.yml",
                "id: test:fireball",
                "cooldown: 5s",
                "castTicks: 40",
                "cost:",
                "  stat: test:mana",
                "  amount: 20.0",
                "targeting:",
                "  type: radius",
                "  radius: 6.0");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        AbilitySpec spec = result.get(ContentId.parse("test:fireball"), AbilitySpec.class).orElseThrow();
        assertEquals(Duration.ofSeconds(5), spec.cooldown());
        assertEquals(40L, spec.castTicks());
        assertEquals(20.0D, spec.cost().amount());
        assertEquals(ContentId.parse("test:mana"), spec.cost().statId());

        Ability ability = spec.toAbility(ContentId.parse("test:fireball"));
        assertEquals(Duration.ofSeconds(5), ability.cooldown());
        assertEquals(40L, ability.castTicks());
        assertTrue(ability.cost().isPresent());
    }

    @Test
    public void defaultsWhenOmitted(@TempDir Path root) throws Exception {
        write(root, "blink.yml", "id: test:blink");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        AbilitySpec spec = result.get(ContentId.parse("test:blink"), AbilitySpec.class).orElseThrow();
        assertEquals(Duration.ZERO, spec.cooldown());
        assertEquals(0L, spec.castTicks());
        assertTrue(spec.cost() == null);
    }

    @Test
    public void badFieldsCollected(@TempDir Path root) throws Exception {
        write(root, "bad-target.yml", "id: test:bt", "targeting:", "  type: spiral");
        write(root, "bad-radius.yml", "id: test:br", "targeting:", "  type: radius");

        SpecLoadResult result = loader().load(root);
        assertFalse(result.successful());
        assertEquals(2, result.errors().size(), () -> result.errors().toString());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("unknown ability targeting type 'spiral'")));
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("needs a positive 'radius'")));
    }
}
