package dev.willram.ramcore.entity;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EntityControlUtilitiesTest {
    private static final Map<LivingEntity, EntityState> STATES = new ConcurrentHashMap<>();

    @Test
    public void controlMutatesAndTemporaryModifierRestoresEntityState() {
        EntityState state = new EntityState();
        TestMob mob = mob("controlled", state);
        LivingEntity target = living("target");

        EntityTemporaryModifier<TestMob> temporary = EntityControls.temporary(mob);
        temporary.control()
                .invulnerable(true)
                .gravity(false)
                .silent(true)
                .glowing(true)
                .persistent(true)
                .invisible(true)
                .ai(false)
                .collidable(false)
                .removeWhenFarAway(true)
                .canPickupItems(true)
                .aware(false)
                .target(target)
                .jumping(true)
                .velocity(new Vector(1, 2, 3))
                .scoreboardTag("temporary");

        assertTrue(state.invulnerable);
        assertFalse(state.gravity);
        assertEquals(target, state.target);
        assertTrue(state.tags.contains("temporary"));

        temporary.close();

        assertFalse(state.invulnerable);
        assertTrue(state.gravity);
        assertEquals(null, state.target);
        assertFalse(state.tags.contains("temporary"));
        assertEquals(new Vector(0, 0, 0), state.velocity);
        assertTrue(temporary.isClosed());
    }

    @Test
    public void templateAppliesEquipmentAttributesFlagsAndTags() {
        EntityState state = new EntityState();
        TestMob mob = mob("templated", state);
        Attribute maxHealth = attributeType("max_health");
        EntityTemplate<TestMob> template = EntityControls.template(TestMob.class)
                .name(Component.text("Guard"))
                .invulnerable(true)
                .ai(false)
                .aware(false)
                .canPickupItems(true)
                .scoreboardTag("guard")
                .equipment(EntityControls.equipment()
                        .dropChance(EquipmentSlot.HAND, 0.25f)
                        .build())
                .attributes(EntityControls.attributes()
                        .base(maxHealth, 40.0d)
                        .build());

        template.apply(mob);

        assertEquals(Component.text("Guard"), state.customName);
        assertTrue(state.invulnerable);
        assertFalse(state.ai);
        assertFalse(state.aware);
        assertTrue(state.canPickupItems);
        assertTrue(state.tags.contains("guard"));
        assertEquals(0.25f, state.dropChance, 0.0001f);
        assertEquals(40.0d, state.maxHealthBase, 0.0001d);
    }

    @Test
    public void configuredSpawnerUsesSpawnReasonAndPassengerTemplates() {
        AtomicReference<CreatureSpawnEvent.SpawnReason> reason = new AtomicReference<>();
        World world = world(reason);
        Location location = new Location(world, 1, 64, 2);
        EntityTemplate<TestMob> passenger = EntityControls.template(TestMob.class)
                .scoreboardTag("passenger");
        EntityTemplate<TestMob> template = EntityControls.template(TestMob.class)
                .spawnReason(CreatureSpawnEvent.SpawnReason.CUSTOM)
                .randomizeData(true)
                .scoreboardTag("root")
                .passenger(passenger);

        EntitySpawnHandle<TestMob> handle = ConfiguredEntitySpawner.spawnNow(world, location, template);

        assertEquals(CreatureSpawnEvent.SpawnReason.CUSTOM, reason.get());
        assertTrue(handle.entity().getScoreboardTags().contains("root"));
        assertEquals(1, handle.passengers().size());
        assertTrue(handle.passengers().get(0).getScoreboardTags().contains("passenger"));
        assertEquals(1, ((EntityState) state(handle.entity())).passengers);
    }

    @Test
    public void angerUsesSafeReflectionWhenEntitySupportsIt() {
        EntityState state = new EntityState();
        TestMob mob = mob("angry", state);

        assertTrue(EntityControls.control(mob).angry(true));
        assertTrue(EntityControls.control(mob).anger(80));

        assertTrue(state.angry);
        assertEquals(80, state.anger);
    }

    @Test
    public void entityControlCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = EntityControls.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.ENTITY_CONTROL).status());
    }

    private static TestMob mob(String name, EntityState state) {
        state.name = name;
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> state.uuid;
            case "getName" -> state.name;
            case "customName" -> {
                if (args == null) {
                    yield state.customName;
                }
                state.customName = (Component) args[0];
                yield null;
            }
            case "isCustomNameVisible" -> state.customNameVisible;
            case "setCustomNameVisible" -> {
                state.customNameVisible = (boolean) args[0];
                yield null;
            }
            case "isVisibleByDefault" -> state.visibleByDefault;
            case "setVisibleByDefault" -> {
                state.visibleByDefault = (boolean) args[0];
                yield null;
            }
            case "isInvulnerable" -> state.invulnerable;
            case "setInvulnerable" -> {
                state.invulnerable = (boolean) args[0];
                yield null;
            }
            case "hasGravity" -> state.gravity;
            case "setGravity" -> {
                state.gravity = (boolean) args[0];
                yield null;
            }
            case "isSilent" -> state.silent;
            case "setSilent" -> {
                state.silent = (boolean) args[0];
                yield null;
            }
            case "isGlowing" -> state.glowing;
            case "setGlowing" -> {
                state.glowing = (boolean) args[0];
                yield null;
            }
            case "isPersistent" -> state.persistent;
            case "setPersistent" -> {
                state.persistent = (boolean) args[0];
                yield null;
            }
            case "isInvisible" -> state.invisible;
            case "setInvisible" -> {
                state.invisible = (boolean) args[0];
                yield null;
            }
            case "getVelocity" -> state.velocity.clone();
            case "setVelocity" -> {
                state.velocity = ((Vector) args[0]).clone();
                yield null;
            }
            case "getScoreboardTags" -> state.tags;
            case "addScoreboardTag" -> state.tags.add((String) args[0]);
            case "removeScoreboardTag" -> state.tags.remove((String) args[0]);
            case "hasAI" -> state.ai;
            case "setAI" -> {
                state.ai = (boolean) args[0];
                yield null;
            }
            case "isCollidable" -> state.collidable;
            case "setCollidable" -> {
                state.collidable = (boolean) args[0];
                yield null;
            }
            case "getRemoveWhenFarAway" -> state.removeWhenFarAway;
            case "setRemoveWhenFarAway" -> {
                state.removeWhenFarAway = (boolean) args[0];
                yield null;
            }
            case "getCanPickupItems" -> state.canPickupItems;
            case "setCanPickupItems" -> {
                state.canPickupItems = (boolean) args[0];
                yield null;
            }
            case "setJumping" -> {
                state.jumping = (boolean) args[0];
                yield null;
            }
            case "isAware" -> state.aware;
            case "setAware" -> {
                state.aware = (boolean) args[0];
                yield null;
            }
            case "getTarget" -> state.target;
            case "setTarget" -> {
                state.target = (LivingEntity) args[0];
                yield null;
            }
            case "setAngry" -> {
                state.angry = (boolean) args[0];
                yield null;
            }
            case "setAnger" -> {
                state.anger = (int) args[0];
                yield null;
            }
            case "getEquipment" -> equipment(state);
            case "getAttribute" -> attribute(state);
            case "addPassenger" -> {
                state.passengers++;
                yield true;
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Mob{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        TestMob mob = proxy(TestMob.class, handler);
        STATES.put(mob, state);
        return mob;
    }

    private static LivingEntity living(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> UUID.nameUUIDFromBytes(("living:" + name).getBytes());
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Living{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(LivingEntity.class, handler);
    }

    private static EntityEquipment equipment(EntityState state) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "setItem" -> {
                state.equipmentItem = args[1];
                yield null;
            }
            case "setDropChance" -> {
                state.dropChance = (float) args[1];
                yield null;
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> defaultValue(method.getReturnType());
        };
        return proxy(EntityEquipment.class, handler);
    }

    private static AttributeInstance attribute(EntityState state) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "setBaseValue" -> {
                state.maxHealthBase = (double) args[0];
                yield null;
            }
            case "getBaseValue", "getValue" -> state.maxHealthBase;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> defaultValue(method.getReturnType());
        };
        return proxy(AttributeInstance.class, handler);
    }

    private static Attribute attributeType(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "translationKey" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Attribute{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Attribute.class, handler);
    }

    private static World world(AtomicReference<CreatureSpawnEvent.SpawnReason> reason) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "spawn" -> {
                reason.set((CreatureSpawnEvent.SpawnReason) args[2]);
                EntityState state = new EntityState();
                TestMob mob = mob("spawned", state);
                @SuppressWarnings("unchecked")
                Consumer<TestMob> consumer = (Consumer<TestMob>) args[4];
                consumer.accept(mob);
                yield mob;
            }
            case "getName" -> "world";
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "World{world}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(World.class, handler);
    }

    private static Object state(LivingEntity entity) {
        return STATES.get(entity);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    public interface TestMob extends Mob {
        void setAngry(boolean angry);

        void setAnger(int anger);
    }

    private static final class EntityState {
        private final UUID uuid = UUID.randomUUID();
        private String name;
        private Component customName;
        private boolean customNameVisible;
        private boolean visibleByDefault = true;
        private boolean invulnerable;
        private boolean gravity = true;
        private boolean silent;
        private boolean glowing;
        private boolean persistent;
        private boolean invisible;
        private boolean ai = true;
        private boolean collidable = true;
        private boolean removeWhenFarAway;
        private boolean canPickupItems;
        private boolean jumping;
        private boolean aware = true;
        private boolean angry;
        private int anger;
        private int passengers;
        private LivingEntity target;
        private Vector velocity = new Vector(0, 0, 0);
        private Set<String> tags = new LinkedHashSet<>();
        private Object equipmentItem;
        private float dropChance;
        private double maxHealthBase = 20.0d;
    }
}
