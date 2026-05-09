package dev.willram.ramcore.menu;

import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.metadata.Metadata;
import dev.willram.ramcore.metadata.MetadataKey;
import dev.willram.ramcore.metadata.MetadataMap;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.TerminableConsumer;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Runtime menu instance for one viewer.
 */
public final class MenuSession implements InventoryHolder, TerminableConsumer, AutoCloseable {
    public static final MetadataKey<MenuSession> OPEN_MENU_KEY = MetadataKey.create("open-menu-session", MenuSession.class);

    private final Player player;
    private final MenuView view;
    private final MenuState state = MenuState.create();
    private final CompositeTerminable terminables = CompositeTerminable.create();
    private final Map<Integer, MenuButton> buttons = new LinkedHashMap<>();
    private Inventory inventory;
    private boolean valid;
    private boolean opening;

    private MenuSession(@NotNull Player player, @NotNull MenuView view) {
        this.player = requireNonNull(player, "player");
        this.view = requireNonNull(view, "view");
    }

    @NotNull
    public static MenuSession create(@NotNull Player player, @NotNull MenuView view) {
        return new MenuSession(player, view);
    }

    @NotNull
    public static MenuSession open(@NotNull Player player, @NotNull MenuView view) {
        MenuSession session = create(player, view);
        session.open();
        return session;
    }

    public void open() {
        Schedulers.run(this.player, this::openNow);
    }

    private void openNow() {
        if (this.valid || this.opening) {
            throw new IllegalStateException("Menu session is already open.");
        }
        if (!this.player.isOnline()) {
            invalidate(false);
            return;
        }

        this.opening = true;
        this.inventory = Bukkit.createInventory(this, this.view.size(), this.view.title());
        renderNow();
        startListening();
        this.player.openInventory(this.inventory);
        Metadata.provideForPlayer(this.player).put(OPEN_MENU_KEY, this);
        this.valid = true;
        this.opening = false;
        this.view.openHandler().handle(this);
        startTicker();
    }

    public void refresh() {
        Schedulers.run(this.player, this::renderNow);
    }

    public void renderNow() {
        if (this.inventory == null) {
            return;
        }

        this.inventory.clear();
        this.buttons.clear();
        this.view.buttons().forEach(this::setButton);
        this.view.renderer().render(this);
    }

    @Override
    public void close() {
        Schedulers.run(this.player, this.player::closeInventory);
    }

    @NotNull
    public Player player() {
        return this.player;
    }

    @NotNull
    public MenuView view() {
        return this.view;
    }

    @NotNull
    public MenuState state() {
        return this.state;
    }

    public boolean valid() {
        return this.valid;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Nullable
    public MenuButton button(int slot) {
        return this.buttons.get(slot);
    }

    @NotNull
    public Map<Integer, MenuButton> buttons() {
        return Map.copyOf(this.buttons);
    }

    public void setButton(int slot, @NotNull MenuButton button) {
        MenuView.Builder.validateSlot(slot, this.view.size());
        requireNonNull(button, "button");
        this.buttons.put(slot, button);
        if (this.inventory != null) {
            this.inventory.setItem(slot, button.itemStack());
        }
    }

    public void clearButton(int slot) {
        this.buttons.remove(slot);
        if (this.inventory != null) {
            this.inventory.clear(slot);
        }
    }

    @NotNull
    @Override
    public <T extends AutoCloseable> T bind(@NotNull T terminable) {
        return this.terminables.bind(terminable);
    }

    private void startTicker() {
        long interval = this.view.updateIntervalTicks();
        if (interval <= 0L) {
            return;
        }

        bind(Schedulers.runTimerTask(this.player, interval, interval, task -> {
            if (!this.valid || !this.player.isOnline()) {
                task.stop();
                return;
            }
            this.view.tickHandler().handle(this);
            renderNow();
        }));
    }

    private void startListening() {
        Events.merge(Player.class)
                .bindEvent(PlayerDeathEvent.class, PlayerDeathEvent::getEntity)
                .bindEvent(PlayerQuitEvent.class, PlayerEvent::getPlayer)
                .bindEvent(PlayerChangedWorldEvent.class, PlayerEvent::getPlayer)
                .bindEvent(PlayerTeleportEvent.class, PlayerEvent::getPlayer)
                .filter(this.player::equals)
                .filter(player -> this.valid || this.opening)
                .handler(player -> invalidate(false))
                .bindWith(this);

        Events.subscribe(InventoryDragEvent.class)
                .filter(event -> owned(event.getInventory()))
                .handler(event -> {
                    if (!this.view.dragAllowed()) {
                        event.setCancelled(true);
                    }
                })
                .bindWith(this);

        Events.subscribe(InventoryClickEvent.class)
                .filter(event -> owned(event.getView().getTopInventory()))
                .handler(this::handleClick)
                .bindWith(this);

        Events.subscribe(InventoryOpenEvent.class)
                .filter(event -> event.getPlayer().equals(this.player))
                .filter(event -> this.valid)
                .filter(event -> !owned(event.getInventory()))
                .handler(event -> invalidate(false))
                .bindWith(this);

        Events.subscribe(InventoryCloseEvent.class)
                .filter(event -> event.getPlayer().equals(this.player))
                .filter(event -> owned(event.getInventory()))
                .handler(event -> invalidate(true))
                .bindWith(this);
    }

    private void handleClick(InventoryClickEvent event) {
        if (!this.valid) {
            event.setCancelled(true);
            return;
        }

        boolean topInventoryClick = event.getRawSlot() >= 0 && event.getRawSlot() < this.view.size();
        if (!topInventoryClick) {
            if (!this.view.playerInventoryClicksAllowed()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        MenuButton button = this.buttons.get(event.getRawSlot());
        if (button == null) {
            return;
        }

        MenuClickContext context = new MenuClickContext(this, event, event.getRawSlot(), button);
        button.handle(context);
        if (button.refreshAfterClick()) {
            renderNow();
        }
        if (button.closeAfterClick()) {
            close();
        }
    }

    private boolean owned(Inventory inventory) {
        return inventory != null && inventory.getHolder() == this;
    }

    private void invalidate(boolean callCloseHandler) {
        if (!this.valid && !this.opening) {
            return;
        }

        this.valid = false;
        this.opening = false;
        MetadataMap metadata = Metadata.provideForPlayer(this.player);
        MenuSession existing = metadata.getOrNull(OPEN_MENU_KEY);
        if (existing == this) {
            metadata.remove(OPEN_MENU_KEY);
        }

        if (callCloseHandler) {
            this.view.closeHandler().handle(this);
        }
        this.terminables.closeAndReportException();
        this.buttons.clear();
    }
}
