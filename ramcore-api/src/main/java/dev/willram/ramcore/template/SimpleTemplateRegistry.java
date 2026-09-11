package dev.willram.ramcore.template;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class SimpleTemplateRegistry<T> implements TemplateRegistry<T> {
    private final Class<T> type;
    private final TemplateComposer<T> composer;
    private final Map<ContentId, TemplateEntry<T>> entries = new LinkedHashMap<>();

    SimpleTemplateRegistry(@NotNull Class<T> type, @NotNull TemplateComposer<T> composer) {
        this.type = requireNonNull(type, "type");
        this.composer = requireNonNull(composer, "composer");
    }

    @NotNull
    @Override
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    @Override
    public TemplateEntry<T> register(@NotNull String owner, @NotNull Template<T> template) {
        requireNonNull(owner, "owner");
        requireNonNull(template, "template");

        ContentId id = template.key().id();
        RamPreconditions.checkArgument(!owner.isBlank(), "template owner must not be blank", "Use plugin name or module id.");
        RamPreconditions.checkArgument(
                template.key().type().equals(this.type),
                "template key type " + template.key().type().getName() + " does not match registry type " + this.type.getName(),
                "Register templates with keys created for this registry type."
        );
        RamPreconditions.checkArgument(
                this.type.isInstance(template.value()),
                "template value does not match registry type " + this.type.getName(),
                "Register an instance of " + this.type.getName() + "."
        );
        RamPreconditions.checkArgument(
                !this.entries.containsKey(id),
                "template id already registered: " + id,
                "Choose a different namespaced id or unregister the previous owner first."
        );

        TemplateEntry<T> entry = new TemplateEntry<>(owner, template);
        this.entries.put(id, entry);
        return entry;
    }

    @NotNull
    @Override
    public Optional<Template<T>> get(@NotNull ContentId id) {
        TemplateEntry<T> entry = this.entries.get(requireNonNull(id, "id"));
        return entry == null ? Optional.empty() : Optional.of(entry.template());
    }

    @NotNull
    @Override
    public Template<T> require(@NotNull ContentId id) {
        return get(id).orElseThrow(() -> RamPreconditions.misuse(
                "template id not registered: " + id,
                "Register the template before lookup or check get(id) first."
        ));
    }

    @NotNull
    @Override
    public T resolve(@NotNull ContentId id) {
        validate();
        return resolve(id, new LinkedHashSet<>());
    }

    @NotNull
    @Override
    public Set<ContentId> ids() {
        return Set.copyOf(this.entries.keySet());
    }

    @NotNull
    @Override
    public Collection<TemplateEntry<T>> entries() {
        return List.copyOf(this.entries.values());
    }

    @Override
    public void validate() {
        List<String> errors = new ArrayList<>();
        for (ContentId id : this.entries.keySet()) {
            validate(id, new LinkedHashSet<>(), new ArrayDeque<>(), errors);
        }

        if (!errors.isEmpty()) {
            throw new TemplateValidationException(errors);
        }
    }

    @Override
    public int unregisterOwner(@NotNull String owner) {
        requireNonNull(owner, "owner");
        int before = this.entries.size();
        this.entries.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
        return before - this.entries.size();
    }

    @Override
    public void clear() {
        this.entries.clear();
    }

    private T resolve(ContentId id, Set<ContentId> seen) {
        if (!seen.add(id)) {
            throw RamPreconditions.misuse(
                    "cyclic template inheritance while resolving " + id,
                    "Remove parent cycle before resolving templates."
            );
        }

        Template<T> template = require(id);
        if (!template.hasParent()) {
            return template.value();
        }

        T parent = resolve(template.parent(), seen);
        return this.composer.compose(parent, template.value());
    }

    private void validate(ContentId id, Set<ContentId> visiting, Deque<ContentId> path, List<String> errors) {
        if (!visiting.add(id)) {
            path.addLast(id);
            errors.add("cyclic template inheritance: " + path);
            path.removeLast();
            return;
        }

        Template<T> template = require(id);
        path.addLast(id);
        if (template.hasParent()) {
            ContentId parent = template.parent();
            if (!this.entries.containsKey(parent)) {
                errors.add(id + ": missing parent template " + parent);
            } else {
                validate(parent, visiting, path, errors);
            }
        }
        path.removeLast();
        visiting.remove(id);
    }
}
