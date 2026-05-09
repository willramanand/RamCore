package dev.willram.ramcore.combat;

import dev.willram.ramcore.entity.EntityAttributeSpec;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CombatControlsTest {

    @Test
    public void attributeSpecAppliesBaseAndModifiers() {
        Attribute movement = attribute("movement");
        AttributeState state = new AttributeState(0.1d);
        LivingEntity entity = living(state);
        NamespacedKey key = NamespacedKey.minecraft("speed_bonus");

        EntityAttributeSpec spec = CombatControls.attributes()
                .base(movement, 0.35d)
                .modifier(movement, CombatControls.modifier(key).addScalar(0.2d).build())
                .build();

        spec.apply(entity);

        assertEquals(0.35d, state.baseValue, 0.0001d);
        assertEquals(1, state.persistentAdds);
        assertEquals(0.2d, state.modifiers.get(key).getAmount(), 0.0001d);
    }

    @Test
    public void temporaryBuffRestoresBaseAndReplacedModifier() {
        Attribute armor = attribute("armor");
        AttributeState state = new AttributeState(2.0d);
        LivingEntity entity = living(state);
        NamespacedKey key = NamespacedKey.minecraft("armor_buff");
        AttributeModifier previous = new AttributeModifier(key, 1.0d, AttributeModifier.Operation.ADD_NUMBER);
        state.modifiers.put(key, previous);

        EntityAttributeSpec spec = CombatControls.attributes()
                .base(armor, 8.0d)
                .modifier(armor, CombatControls.modifier(key)
                        .addNumber(4.0d)
                        .transientModifier(true)
                        .build())
                .build();

        AttributeBuff buff = CombatControls.buff(entity, spec);

        assertEquals(8.0d, state.baseValue, 0.0001d);
        assertEquals(1, state.transientAdds);
        assertEquals(4.0d, state.modifiers.get(key).getAmount(), 0.0001d);

        buff.close();

        assertTrue(buff.isClosed());
        assertEquals(2.0d, state.baseValue, 0.0001d);
        assertEquals(previous, state.modifiers.get(key));
    }

    @Test
    public void combatProfileCanApplyAndCreateTemporaryBuff() {
        Attribute reach = attribute("reach");
        AttributeState state = new AttributeState(3.0d);
        LivingEntity entity = living(state);

        CombatProfile profile = CombatControls.profile()
                .base(reach, 5.0d)
                .build();

        AttributeBuff buff = profile.temporary(entity);
        assertEquals(5.0d, state.baseValue, 0.0001d);

        buff.close();
        assertEquals(3.0d, state.baseValue, 0.0001d);
    }

    @Test
    public void damageProfileAppliesDamageAndInvulnerabilityFrameControls() {
        AttributeState state = new AttributeState(20.0d);
        LivingEntity entity = living(state);
        Entity damager = entity("damager");

        CombatControls.damage(6.0d)
                .damager(damager)
                .clearNoDamageTicks(true)
                .noDamageTicksAfter(4)
                .maximumNoDamageTicksAfter(8)
                .hurtDirection(90.0f)
                .build()
                .apply(entity);

        assertEquals(6.0d, state.lastDamageAmount, 0.0001d);
        assertEquals(damager, state.lastDamager);
        assertEquals(4, state.noDamageTicks);
        assertEquals(8, state.maximumNoDamageTicks);
        assertEquals(90.0f, state.hurtDirection, 0.0001f);
        assertTrue(state.noDamageTicksClearedBeforeDamage);
    }

    @Test
    public void entityAttributesCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = CombatControls.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.ENTITY_ATTRIBUTES).status());
    }

    private static LivingEntity living(AttributeState state) {
        AttributeInstance instance = attributeInstance(state);
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> UUID.randomUUID();
            case "getAttribute" -> instance;
            case "setNoDamageTicks" -> {
                state.noDamageTicks = (int) args[0];
                if (state.damageCalls == 0 && state.noDamageTicks == 0) {
                    state.noDamageTicksClearedBeforeDamage = true;
                }
                yield null;
            }
            case "setMaximumNoDamageTicks" -> {
                state.maximumNoDamageTicks = (int) args[0];
                yield null;
            }
            case "setHurtDirection" -> {
                state.hurtDirection = (float) args[0];
                yield null;
            }
            case "damage" -> {
                state.damageCalls++;
                state.lastDamageAmount = (double) args[0];
                if (args.length > 1 && args[1] instanceof Entity entity) {
                    state.lastDamager = entity;
                }
                yield null;
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Living";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(LivingEntity.class, handler);
    }

    private static AttributeInstance attributeInstance(AttributeState state) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getBaseValue", "getValue" -> state.baseValue;
            case "setBaseValue" -> {
                state.baseValue = (double) args[0];
                yield null;
            }
            case "getModifier" -> state.modifiers.get(args[0]);
            case "removeModifier" -> {
                if (args[0] instanceof AttributeModifier modifier) {
                    state.modifiers.remove(modifier.getKey());
                } else {
                    state.modifiers.remove(args[0]);
                }
                yield null;
            }
            case "addModifier" -> {
                AttributeModifier modifier = (AttributeModifier) args[0];
                state.modifiers.put(modifier.getKey(), modifier);
                state.persistentAdds++;
                yield null;
            }
            case "addTransientModifier" -> {
                AttributeModifier modifier = (AttributeModifier) args[0];
                state.modifiers.put(modifier.getKey(), modifier);
                state.transientAdds++;
                yield null;
            }
            case "getModifiers" -> state.modifiers.values();
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> defaultValue(method.getReturnType());
        };
        return proxy(AttributeInstance.class, handler);
    }

    private static Attribute attribute(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "translationKey" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Attribute{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Attribute.class, handler);
    }

    private static Entity entity(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> UUID.nameUUIDFromBytes(("entity:" + name).getBytes());
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Entity{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Entity.class, handler);
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

    private static final class AttributeState {
        private double baseValue;
        private int persistentAdds;
        private int transientAdds;
        private int damageCalls;
        private int noDamageTicks = 20;
        private int maximumNoDamageTicks = 20;
        private double lastDamageAmount;
        private float hurtDirection;
        private boolean noDamageTicksClearedBeforeDamage;
        private Entity lastDamager;
        private final Map<Object, AttributeModifier> modifiers = new LinkedHashMap<>();

        private AttributeState(double baseValue) {
            this.baseValue = baseValue;
        }
    }
}
