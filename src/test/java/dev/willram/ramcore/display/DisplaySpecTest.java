package dev.willram.ramcore.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class DisplaySpecTest {

    @Test
    public void textSpecAppliesTextAndOptions() {
        RecordingDisplay display = RecordingDisplay.text();

        TextDisplaySpec.text(Component.text("Hello"))
                .lineWidth(120)
                .background(Color.BLACK)
                .opacity((byte) 100)
                .shadowed(true)
                .seeThrough(true)
                .defaultBackground(false)
                .alignment(TextDisplay.TextAlignment.CENTER)
                .options()
                .billboard(Display.Billboard.CENTER)
                .viewRange(32f);

        TextDisplaySpec spec = TextDisplaySpec.text(Component.text("Hello"))
                .lineWidth(120)
                .background(Color.BLACK)
                .opacity((byte) 100)
                .shadowed(true)
                .seeThrough(true)
                .defaultBackground(false)
                .alignment(TextDisplay.TextAlignment.CENTER);
        spec.options().billboard(Display.Billboard.CENTER).viewRange(32f);

        spec.apply(display.textDisplay());

        assertTrue(display.calls.contains("text:Hello"));
        assertTrue(display.calls.contains("setLineWidth:120"));
        assertTrue(display.calls.contains("setBackgroundColor"));
        assertTrue(display.calls.contains("setTextOpacity:100"));
        assertTrue(display.calls.contains("setShadowed:true"));
        assertTrue(display.calls.contains("setSeeThrough:true"));
        assertTrue(display.calls.contains("setDefaultBackground:false"));
        assertTrue(display.calls.contains("setAlignment:CENTER"));
        assertTrue(display.calls.contains("setBillboard:CENTER"));
        assertTrue(display.calls.contains("setViewRange:32.0"));
    }

    @Test
    public void itemSpecAppliesTransformWithoutServerRegistryObjects() {
        RecordingDisplay display = RecordingDisplay.item();

        new ItemDisplaySpec().transform(ItemDisplay.ItemDisplayTransform.GUI).apply(display.itemDisplay());

        assertTrue(display.calls.contains("setItemDisplayTransform:GUI"));
    }

    @Test
    public void hologramSpecPreservesLineOrderAndSpacing() {
        HologramSpec spec = HologramSpec.create()
                .lineSpacing(0.5d)
                .text(Component.text("Top"))
                .text(Component.text("Bottom"));

        assertEquals(2, spec.lines().size());
        assertEquals(0.5d, spec.lineSpacing(), 0.0d);
    }

    private static final class RecordingDisplay {
        private final List<String> calls = new ArrayList<>();
        private final Object proxy;

        private RecordingDisplay(Class<?> type) {
            this.proxy = Proxy.newProxyInstance(
                    DisplaySpecTest.class.getClassLoader(),
                    new Class<?>[]{type},
                    (ignored, method, args) -> {
                        String name = method.getName();
                        if (name.equals("text") && args != null && args.length == 1 && args[0] instanceof Component component) {
                            this.calls.add("text:" + PlainTextComponentSerializer.plainText().serialize(component));
                            return null;
                        }
                        if (name.startsWith("set")) {
                            if (args == null || args.length == 0) {
                                this.calls.add(name);
                            } else if (args[0] instanceof ItemStack itemStack) {
                                this.calls.add(name + ":" + itemStack.getAmount());
                            } else if (args[0] instanceof Color) {
                                this.calls.add(name);
                            } else {
                                this.calls.add(name + ":" + args[0]);
                            }
                            return null;
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) {
                            return false;
                        }
                        if (returnType == int.class) {
                            return 0;
                        }
                        if (returnType == float.class) {
                            return 0f;
                        }
                        if (returnType == double.class) {
                            return 0d;
                        }
                        return null;
                    }
            );
        }

        private static RecordingDisplay text() {
            return new RecordingDisplay(TextDisplay.class);
        }

        private static RecordingDisplay item() {
            return new RecordingDisplay(ItemDisplay.class);
        }

        private TextDisplay textDisplay() {
            return (TextDisplay) this.proxy;
        }

        private ItemDisplay itemDisplay() {
            return (ItemDisplay) this.proxy;
        }
    }
}
