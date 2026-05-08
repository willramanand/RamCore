package dev.willram.ramcore.template;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TemplateRegistryTest {
    private static final ContentKey<ItemTemplate> BASE =
            ContentKey.of("ramcore", "base", ItemTemplate.class);
    private static final ContentKey<ItemTemplate> CHILD =
            ContentKey.of("ramcore", "child", ItemTemplate.class);

    @Test
    public void resolveComposesParentAndChild() {
        TemplateRegistry<ItemTemplate> registry = TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);
        registry.register("RamCore", Template.of(BASE, new ItemTemplate("Base", 5)));
        registry.register("RamCore", Template.extending(CHILD, BASE.id(), new ItemTemplate("Child", null)));

        ItemTemplate resolved = registry.resolve(CHILD.id());

        assertEquals("Child", resolved.name());
        assertEquals(Integer.valueOf(5), resolved.damage());
    }

    @Test
    public void missingParentReportsValidationError() {
        TemplateRegistry<ItemTemplate> registry = TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);
        registry.register("RamCore", Template.extending(CHILD, BASE.id(), new ItemTemplate("Child", null)));

        try {
            registry.validate();
        } catch (TemplateValidationException e) {
            assertTrue(e.errors().contains("ramcore:child: missing parent template ramcore:base"));
            return;
        }

        throw new AssertionError("expected TemplateValidationException");
    }

    @Test
    public void cycleReportsValidationError() {
        TemplateRegistry<ItemTemplate> registry = TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);
        registry.register("RamCore", Template.extending(BASE, CHILD.id(), new ItemTemplate("Base", 5)));
        registry.register("RamCore", Template.extending(CHILD, BASE.id(), new ItemTemplate("Child", null)));

        try {
            registry.validate();
        } catch (TemplateValidationException e) {
            assertTrue(e.errors().stream().anyMatch(error -> error.contains("cyclic template inheritance")));
            return;
        }

        throw new AssertionError("expected TemplateValidationException");
    }

    @Test
    public void duplicateTemplateFailsFast() {
        TemplateRegistry<ItemTemplate> registry = TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);
        registry.register("RamCore", Template.of(BASE, new ItemTemplate("Base", 5)));

        try {
            registry.register("Other", Template.of(BASE, new ItemTemplate("Other", 8)));
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("already registered"));
            return;
        }

        throw new AssertionError("expected ApiMisuseException");
    }

    @Test
    public void unregisterOwnerRemovesTemplates() {
        TemplateRegistry<ItemTemplate> registry = TemplateRegistry.create(ItemTemplate.class, ItemTemplate::merge);
        registry.register("A", Template.of(BASE, new ItemTemplate("Base", 5)));
        registry.register("B", Template.of(CHILD, new ItemTemplate("Child", 8)));

        assertEquals(1, registry.unregisterOwner("A"));

        assertFalse(registry.get(ContentId.parse("ramcore:base")).isPresent());
        assertTrue(registry.get(ContentId.parse("ramcore:child")).isPresent());
    }

    private record ItemTemplate(String name, Integer damage) {
        private static ItemTemplate merge(ItemTemplate parent, ItemTemplate child) {
            return new ItemTemplate(
                    child.name() == null ? parent.name() : child.name(),
                    child.damage() == null ? parent.damage() : child.damage()
            );
        }
    }
}
