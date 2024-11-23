package dev.willram.ramcore.menu;

import dev.willram.ramcore.RamCore;
import dev.willram.ramcore.event.Events;
import dev.willram.ramcore.metadata.Metadata;
import dev.willram.ramcore.metadata.MetadataKey;
import dev.willram.ramcore.metadata.MetadataMap;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.MinecraftVersions;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.TerminableConsumer;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import dev.willram.ramcore.utils.LoaderUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Gui implements TerminableConsumer {
    public static final MetadataKey<Gui> OPEN_GUI_KEY = MetadataKey.create("open-gui-custom", Gui.class);
    public static final String GUI_ITEM_KEY = "ignore-gui-item";

    // The player holding the GUI
    private final Player player;
    // The backing inventory instance
    private final Inventory inventory;
    // The initial title set when the inventory was made.
    private final String initialTitle;
    // The slots in the gui, lazily loaded
    private final Map<Integer, Item> slots;
    // This remains true until after #redraw is called for the first time
    private boolean firstDraw = true;
    private boolean itemsPlaceable = false;

    private final CompositeTerminable compositeTerminable = CompositeTerminable.create();

    private boolean valid = false;
    private boolean invalidated = false;

    public Gui(Player player, int lines, String title) {
        this.player = Objects.requireNonNull(player, "player");
        this.initialTitle = Objects.requireNonNull(title, "title");
        this.inventory = Bukkit.createInventory(player, lines * 9, MiniMessage.miniMessage().deserialize(this.initialTitle));
        this.slots = new HashMap<>();
        this.itemsPlaceable = false;
    }

    public Gui(Player player, int lines, String title, boolean itemsPlaceable) {
        this.player = Objects.requireNonNull(player, "player");
        this.initialTitle = Objects.requireNonNull(title, "title");
        this.inventory = Bukkit.createInventory(player, lines * 9, MiniMessage.miniMessage().deserialize(this.initialTitle));
        this.slots = new HashMap<>();
        this.itemsPlaceable = itemsPlaceable;
    }

    /**
     * Places items on the GUI. Called when the GUI is opened.
     * Use {@link #isFirstDraw()} to determine if this is the first time redraw has been called.
     */
    public abstract void redraw();

    public abstract boolean clickHandler(InventoryClickEvent event);
    public abstract void closeHandler(InventoryCloseEvent event);
    public abstract void invalidateHandler();
    /**
     * Gets the player viewing this Gui
     *
     * @return the player viewing this gui
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Gets the delegate Bukkit inventory
     *
     * @return the bukkit inventory being wrapped by this instance
     */
    public Inventory getHandle() {
        return this.inventory;
    }

    /**
     * Gets the initial title which was set when this GUI was made
     *
     * @return the initial title used when this GUI was made
     */
    public String getInitialTitle() {
        return this.initialTitle;
    }

    @Nonnull
    @Override
    public <T extends AutoCloseable> T bind(@Nonnull T terminable) {
        return this.compositeTerminable.bind(terminable);
    }

    public boolean isFirstDraw() {
        return this.firstDraw;
    }

    public void setItem(int slot, Item item) {
        this.inventory.setItem(slot, item.getItemStack());
        this.slots.put(slot, item);
    }

    public Item getItem(int slot) {
        return this.slots.get(slot);
    }

    public void clearItems() {
        this.inventory.clear();
        this.slots.clear();
    }

    public void open() {
        if (MinecraftVersion.getRuntimeVersion().isAfterOrEq(MinecraftVersions.v1_16)) {
            // delay by a tick in 1.16+ to prevent an unwanted PlayerInteractEvent interfering with inventory clicks
            Schedulers.sync().runLater(() -> {
                if (!this.player.isOnline()) {
                    return;
                }
                handleOpen();
            }, 1);
        } else {
            handleOpen();
        }
    }

    private void handleOpen() {
        if (this.valid) {
            throw new IllegalStateException("Gui is already opened.");
        }
        this.firstDraw = true;
        this.invalidated = false;
        try {
            redraw();
        } catch (Exception e) {
            e.printStackTrace();
            invalidate();
            return;
        }

        this.firstDraw = false;
        startListening();
        this.player.openInventory(this.inventory);
        Metadata.provideForPlayer(this.player).put(OPEN_GUI_KEY, this);
        this.valid = true;
    }

    public void close() {
        this.player.closeInventory();
    }

    private void invalidate() {
        this.valid = false;
        this.invalidated = true;

        MetadataMap metadataMap = Metadata.provideForPlayer(this.player);
        Gui existing = metadataMap.getOrNull(OPEN_GUI_KEY);
        if (existing == this) {
            metadataMap.remove(OPEN_GUI_KEY);
        }

        this.invalidateHandler();

        // stop listening
        this.compositeTerminable.closeAndReportException();

        // clear all items from the GUI, just in case the menu didn't close properly.
        clearItems();
    }

    /**
     * Returns true unless this GUI has been invalidated, through being closed, or the player leaving.
     * @return true unless this GUI has been invalidated.
     */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * Registers the event handlers for this GUI
     */
    private void startListening() {
        Events.merge(Player.class)
                .bindEvent(PlayerDeathEvent.class, PlayerDeathEvent::getEntity)
                .bindEvent(PlayerQuitEvent.class, PlayerEvent::getPlayer)
                .bindEvent(PlayerChangedWorldEvent.class, PlayerEvent::getPlayer)
                .bindEvent(PlayerTeleportEvent.class, PlayerEvent::getPlayer)
                .filter(p -> p.equals(this.player))
                .filter(p -> isValid())
                .handler(p -> invalidate())
                .bindWith(this);

        Events.subscribe(InventoryDragEvent.class)
                .filter(e -> e.getInventory().getHolder() != null)
                .filter(e -> e.getInventory().getHolder().equals(this.player))
                .handler(e -> {
                    e.setCancelled(true);
                    if (!isValid()) {
                        close();
                    }
                }).bindWith(this);

        Events.subscribe(InventoryClickEvent.class)
                .handler(e -> {
                    if (!isValid()) {
                        close();
                        return;
                    }

                    if (itemsPlaceable) {
                        if (e.getClickedInventory() == null) {
                            return;
                        }

                        if (!e.getClickedInventory().equals(this.inventory)) {
                            return;
                        }
                    } else {
                        if (e.getInventory().getHolder() == null) {
                            return;
                        }

                        if (!e.getInventory().getHolder().equals(this.player)) {
                            return;
                        }

                        if (!e.getInventory().equals(this.inventory)) {
                            return;
                        }
                    }
                    boolean returnEarly = this.clickHandler(e);
                    if (returnEarly) {
                        return;
                    }

                    e.setCancelled(true);

                    int slotId = e.getRawSlot();

                    // check if the click was in the top inventory
                    if (slotId != e.getSlot()) {
                        return;
                    }

                    Item clickedItem = this.getItem(slotId);
                    if (clickedItem != null) {
                        var handlers = clickedItem.getHandlers().entrySet();
                        if (handlers == null) {
                            return;
                        }
                        for (Map.Entry<ClickType, Consumer<InventoryClickEvent>> handler : handlers) {
                            try {
                                handler.getValue().accept(e);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                })
                .bindWith(this);

        Events.subscribe(InventoryOpenEvent.class)
                .filter(e -> e.getPlayer().equals(this.player))
                .filter(e -> !e.getInventory().equals(this.inventory))
                .filter(e -> isValid())
                .handler(e -> invalidate())
                .bindWith(this);

        Events.subscribe(InventoryCloseEvent.class)
                .filter(e -> e.getPlayer().equals(this.player))
                .filter(e -> isValid())
                .handler(e -> {
                    if (!e.getInventory().equals(this.inventory)) {
                        return;
                    }

                    this.closeHandler(e);
                    invalidate();
                })
                .bindWith(this);
    }
}
