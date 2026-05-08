package dev.willram.ramcore.presentation;

import dev.willram.ramcore.terminable.Terminable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class PresentationEffectsTest {

    @Test
    public void messageSendsToEveryAudience() {
        RecordingAudience first = new RecordingAudience();
        RecordingAudience second = new RecordingAudience();
        PresentationContext context = PresentationContext.of(first, second);

        PresentationEffects.message(Component.text("Hello")).play(context);

        assertEquals(List.of("message:Hello"), first.events);
        assertEquals(List.of("message:Hello"), second.events);
    }

    @Test
    public void actionBarAndTitleUseAdventureAudienceMethods() {
        RecordingAudience audience = new RecordingAudience();
        PresentationContext context = PresentationContext.of(audience);

        PresentationEffects.actionBar(Component.text("Action")).play(context);
        PresentationEffects.title(Title.title(Component.text("Title"), Component.text("Sub"))).play(context);

        assertEquals(List.of("action:Action", "title"), audience.events);
    }

    @Test
    public void bossBarTerminableHidesBarOnClose() throws Exception {
        RecordingAudience audience = new RecordingAudience();
        BossBar bossBar = BossBar.bossBar(Component.text("Boss"), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        Terminable terminable = PresentationEffects.bossBar(bossBar).play(PresentationContext.of(audience));
        terminable.close();

        assertEquals(List.of("boss:show", "boss:hide"), audience.events);
    }

    @Test
    public void immediateSequenceRunsInOrder() {
        RecordingAudience audience = new RecordingAudience();
        PresentationEffects.sequence()
                .then(PresentationEffects.message(Component.text("One")))
                .then(PresentationEffects.actionBar(Component.text("Two")))
                .play(PresentationContext.of(audience));

        assertEquals(List.of("message:One", "action:Two"), audience.events);
    }

    private static final class RecordingAudience implements Audience {
        private final List<String> events = new ArrayList<>();

        @Override
        public void sendMessage(ComponentLike message) {
            this.events.add("message:" + plain(message));
        }

        @Override
        public void sendActionBar(ComponentLike message) {
            this.events.add("action:" + plain(message));
        }

        @Override
        public void showTitle(Title title) {
            this.events.add("title");
        }

        @Override
        public void showBossBar(BossBar bar) {
            this.events.add("boss:show");
        }

        @Override
        public void hideBossBar(BossBar bar) {
            this.events.add("boss:hide");
        }

        @Override
        public void playSound(Sound sound) {
            this.events.add("sound");
        }

        private static String plain(ComponentLike component) {
            return PlainTextComponentSerializer.plainText().serialize(component.asComponent());
        }
    }
}
