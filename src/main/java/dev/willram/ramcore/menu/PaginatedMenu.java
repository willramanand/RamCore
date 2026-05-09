package dev.willram.ramcore.menu;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Declarative paginated menu builder.
 */
public final class PaginatedMenu<T> {
    public static final String PAGE_STATE_KEY = "page";

    private final MenuView view;
    private final List<T> entries;
    private final List<Integer> itemSlots;
    private final PaginatedButtonFactory<T> buttonFactory;
    private final int previousSlot;
    private final MenuButton previousButton;
    private final int nextSlot;
    private final MenuButton nextButton;

    private PaginatedMenu(
            @NotNull MenuView.Builder viewBuilder,
            @NotNull List<T> entries,
            @NotNull List<Integer> itemSlots,
            @NotNull PaginatedButtonFactory<T> buttonFactory,
            int previousSlot,
            @Nullable MenuButton previousButton,
            int nextSlot,
            @Nullable MenuButton nextButton
    ) {
        this.entries = List.copyOf(entries);
        this.itemSlots = List.copyOf(itemSlots);
        this.buttonFactory = requireNonNull(buttonFactory, "buttonFactory");
        this.previousSlot = previousSlot;
        this.previousButton = previousButton;
        this.nextSlot = nextSlot;
        this.nextButton = nextButton;
        this.view = viewBuilder.render(this::render).build();
    }

    @NotNull
    public static <T> Builder<T> builder(@NotNull ComponentLike title, int rows, @NotNull PaginatedButtonFactory<T> buttonFactory) {
        return new Builder<>(MenuView.builder(title, rows), buttonFactory);
    }

    @NotNull
    public MenuView view() {
        return this.view;
    }

    @NotNull
    public MenuSession open(@NotNull Player player) {
        return MenuSession.open(player, this.view);
    }

    @NotNull
    public List<T> entries() {
        return this.entries;
    }

    @NotNull
    public List<Integer> itemSlots() {
        return this.itemSlots;
    }

    public int pageSize() {
        return this.itemSlots.size();
    }

    public int pageCount() {
        if (this.entries.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil(this.entries.size() / (double) pageSize());
    }

    public int clampPage(int page) {
        return Math.max(0, Math.min(page, pageCount() - 1));
    }

    @NotNull
    public List<T> entriesForPage(int page) {
        int currentPage = clampPage(page);
        int start = currentPage * pageSize();
        int end = Math.min(this.entries.size(), start + pageSize());
        return List.copyOf(this.entries.subList(start, end));
    }

    @NotNull
    public Map<Integer, MenuButton> buttonsForPage(int page) {
        int currentPage = clampPage(page);
        List<T> pageEntries = entriesForPage(currentPage);
        Map<Integer, MenuButton> buttons = new LinkedHashMap<>();
        for (int i = 0; i < pageEntries.size(); i++) {
            int index = currentPage * pageSize() + i;
            buttons.put(this.itemSlots.get(i), this.buttonFactory.button(pageEntries.get(i), index));
        }

        if (this.previousButton != null && currentPage > 0) {
            buttons.put(this.previousSlot, previousPageButton());
        }
        if (this.nextButton != null && currentPage < pageCount() - 1) {
            buttons.put(this.nextSlot, nextPageButton());
        }
        return buttons;
    }

    private void render(MenuSession session) {
        int page = clampPage(session.state().intValue(PAGE_STATE_KEY, 0));
        session.state().put(PAGE_STATE_KEY, page);
        buttonsForPage(page).forEach(session::setButton);
    }

    @NotNull
    private MenuButton previousPageButton() {
        return wrapNavigationButton(this.previousButton, -1);
    }

    @NotNull
    private MenuButton nextPageButton() {
        return wrapNavigationButton(this.nextButton, 1);
    }

    @NotNull
    private MenuButton wrapNavigationButton(MenuButton source, int delta) {
        return MenuButton.builder(source.itemStack())
                .onAny(context -> {
                    int current = context.session().state().intValue(PAGE_STATE_KEY, 0);
                    context.session().state().put(PAGE_STATE_KEY, clampPage(current + delta));
                    context.session().renderNow();
                })
                .build();
    }

    public static final class Builder<T> {
        private final MenuView.Builder viewBuilder;
        private final PaginatedButtonFactory<T> buttonFactory;
        private final List<T> entries = new ArrayList<>();
        private final List<Integer> itemSlots = new ArrayList<>();
        private int previousSlot = -1;
        private MenuButton previousButton;
        private int nextSlot = -1;
        private MenuButton nextButton;

        private Builder(MenuView.Builder viewBuilder, PaginatedButtonFactory<T> buttonFactory) {
            this.viewBuilder = requireNonNull(viewBuilder, "viewBuilder");
            this.buttonFactory = requireNonNull(buttonFactory, "buttonFactory");
        }

        @NotNull
        public Builder<T> entries(@NotNull Iterable<? extends T> entries) {
            requireNonNull(entries, "entries").forEach(entry -> this.entries.add(requireNonNull(entry, "entry")));
            return this;
        }

        @NotNull
        public Builder<T> entry(@NotNull T entry) {
            this.entries.add(requireNonNull(entry, "entry"));
            return this;
        }

        @NotNull
        public Builder<T> slots(int... slots) {
            for (int slot : slots) {
                MenuView.Builder.validateSlot(slot, this.viewBuilder.build().size());
                this.itemSlots.add(slot);
            }
            return this;
        }

        @NotNull
        public Builder<T> slotRange(int startInclusive, int endInclusive) {
            for (int slot = startInclusive; slot <= endInclusive; slot++) {
                slots(slot);
            }
            return this;
        }

        @NotNull
        public Builder<T> previous(int slot, @NotNull MenuButton button) {
            this.previousSlot = slot;
            this.previousButton = requireNonNull(button, "button");
            return this;
        }

        @NotNull
        public Builder<T> next(int slot, @NotNull MenuButton button) {
            this.nextSlot = slot;
            this.nextButton = requireNonNull(button, "button");
            return this;
        }

        @NotNull
        public Builder<T> decorate(@NotNull java.util.function.Consumer<MenuView.Builder> decorator) {
            requireNonNull(decorator, "decorator").accept(this.viewBuilder);
            return this;
        }

        @NotNull
        public PaginatedMenu<T> build() {
            RamPreconditions.checkArgument(!this.itemSlots.isEmpty(), "paginated menu must have at least one item slot", "Call slots(...) or slotRange(...).");
            int size = this.viewBuilder.build().size();
            if (this.previousButton != null) {
                MenuView.Builder.validateSlot(this.previousSlot, size);
            }
            if (this.nextButton != null) {
                MenuView.Builder.validateSlot(this.nextSlot, size);
            }
            return new PaginatedMenu<>(
                    this.viewBuilder,
                    this.entries,
                    this.itemSlots,
                    this.buttonFactory,
                    this.previousSlot,
                    this.previousButton,
                    this.nextSlot,
                    this.nextButton
            );
        }
    }
}
