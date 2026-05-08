package dev.willram.ramcore.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class NpcSpecTest {

    @Test
    public void specAppliesCommonEntityOptions() {
        RecordingEntity entity = RecordingEntity.recordingEntity();

        NpcSpec<Entity> spec = NpcSpec.of(Entity.class)
                .name(Component.text("Guide"))
                .nameVisible(true)
                .visibleByDefault(false)
                .invulnerable(true)
                .gravity(false)
                .silent(true)
                .glowing(true)
                .persistent(false);

        spec.apply(entity.entity());

        assertTrue(entity.calls.contains("customName:Guide"));
        assertTrue(entity.calls.contains("setCustomNameVisible:true"));
        assertTrue(entity.calls.contains("setVisibleByDefault:false"));
        assertTrue(entity.calls.contains("setInvulnerable:true"));
        assertTrue(entity.calls.contains("setGravity:false"));
        assertTrue(entity.calls.contains("setSilent:true"));
        assertTrue(entity.calls.contains("setGlowing:true"));
        assertTrue(entity.calls.contains("setPersistent:false"));
    }

    @Test
    public void specAppliesLivingEntityOptions() {
        RecordingEntity entity = RecordingEntity.recordingLiving();

        NpcSpec<LivingEntity> spec = NpcSpec.of(LivingEntity.class)
                .ai(false)
                .collidable(false)
                .removeWhenFarAway(false)
                .canPickupItems(false);

        spec.apply(entity.livingEntity());

        assertTrue(entity.calls.contains("setAI:false"));
        assertTrue(entity.calls.contains("setCollidable:false"));
        assertTrue(entity.calls.contains("setRemoveWhenFarAway:false"));
        assertTrue(entity.calls.contains("setCanPickupItems:false"));
    }

    @Test
    public void handleDispatchesClicksWithContext() {
        RecordingEntity entity = RecordingEntity.recordingEntity();
        RecordingEntity player = RecordingEntity.recordingPlayer();
        AtomicReference<NpcClickContext> clicked = new AtomicReference<>();

        NpcSpec<Entity> spec = NpcSpec.of(Entity.class).onClick(clicked::set);
        NpcHandle<Entity> handle = new NpcHandle<>(entity.entity(), spec);

        handle.click(player.player(), NpcClickType.ATTACK);

        assertSame(handle, clicked.get().npc());
        assertSame(player.player(), clicked.get().player());
        assertSame(entity.entity(), clicked.get().entity());
        assertEquals(NpcClickType.ATTACK, clicked.get().type());
    }

    @Test
    public void handleDelegatesPlayerVisibility() {
        RecordingEntity entity = RecordingEntity.recordingEntity();
        RecordingEntity player = RecordingEntity.recordingPlayer();
        Plugin plugin = (Plugin) Proxy.newProxyInstance(
                NpcSpecTest.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (ignored, method, args) -> defaultValue(method.getReturnType())
        );
        NpcHandle<Entity> handle = new NpcHandle<>(entity.entity(), NpcSpec.of(Entity.class));

        handle.hideFrom(plugin, player.player());
        handle.showTo(plugin, player.player());

        assertTrue(handle.visibleTo(player.player()));
        assertTrue(player.calls.contains("hideEntity"));
        assertTrue(player.calls.contains("showEntity"));
        assertTrue(player.calls.contains("canSee"));
    }

    @Test
    public void registryDispatchesRegisteredNpcAndRespectsConsumeFlag() {
        RecordingEntity entity = RecordingEntity.recordingEntity();
        RecordingEntity player = RecordingEntity.recordingPlayer();
        List<NpcClickType> clicks = new ArrayList<>();

        NpcRegistry registry = NpcRegistry.create();
        NpcSpec<Entity> spec = NpcSpec.of(Entity.class)
                .consumeClicks(false)
                .onClick(context -> clicks.add(context.type()));
        NpcHandle<Entity> handle = new NpcHandle<>(entity.entity(), spec);
        registry.register("test", handle);

        assertFalse(registry.dispatch(player.player(), entity.entity(), NpcClickType.INTERACT));
        assertEquals(List.of(NpcClickType.INTERACT), clicks);
        assertTrue(registry.npc(entity.entity()).isPresent());
        assertTrue(registry.unregister(entity.entity()));
        assertFalse(registry.npc(entity.entity()).isPresent());
    }

    private static final class RecordingEntity {
        private final List<String> calls = new ArrayList<>();
        private final Object proxy;
        private final UUID uuid = UUID.randomUUID();

        private RecordingEntity(Class<?> type) {
            this.proxy = Proxy.newProxyInstance(
                    NpcSpecTest.class.getClassLoader(),
                    new Class<?>[]{type},
                    (ignored, method, args) -> {
                        String name = method.getName();
                        if (name.equals("customName") && args != null && args.length == 1 && args[0] instanceof Component component) {
                            this.calls.add("customName:" + PlainTextComponentSerializer.plainText().serialize(component));
                            return null;
                        }
                        if (name.equals("getUniqueId")) {
                            return this.uuid;
                        }
                        if (name.equals("isValid") || name.equals("canSee")) {
                            this.calls.add(name);
                            return true;
                        }
                        if (name.equals("hideEntity") || name.equals("showEntity")) {
                            this.calls.add(name);
                            return null;
                        }
                        if (name.startsWith("set")) {
                            this.calls.add(name + ":" + args[0]);
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private static RecordingEntity recordingEntity() {
            return new RecordingEntity(Entity.class);
        }

        private static RecordingEntity recordingLiving() {
            return new RecordingEntity(LivingEntity.class);
        }

        private static RecordingEntity recordingPlayer() {
            return new RecordingEntity(Player.class);
        }

        private Entity entity() {
            return (Entity) this.proxy;
        }

        private LivingEntity livingEntity() {
            return (LivingEntity) this.proxy;
        }

        private Player player() {
            return (Player) this.proxy;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
