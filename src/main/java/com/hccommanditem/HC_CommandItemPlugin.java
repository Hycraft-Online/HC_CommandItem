package com.hccommanditem;

import com.hccommanditem.interactions.ExecuteCommandInteraction;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * HC_CommandItem - Creates items that execute chat commands when used.
 *
 * Players are given a menu item in slot 9 that opens a menu when used.
 * The item is locked and cannot be moved or dropped.
 */
public class HC_CommandItemPlugin extends JavaPlugin {

    public static final String VERSION = "1.2.0";

    // The item ID for the command menu item
    public static final String MENU_ITEM_ID = "CommandMenu";

    // Slot 9 = index 8 (0-indexed)
    public static final byte SLOT_9_INDEX = 8;

    private static HC_CommandItemPlugin instance;

    public HC_CommandItemPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static HC_CommandItemPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        this.getLogger().at(Level.INFO).log("=================================");
        this.getLogger().at(Level.INFO).log("    HC_COMMAND ITEM " + VERSION);
        this.getLogger().at(Level.INFO).log("  Items that execute commands");
        this.getLogger().at(Level.INFO).log("=================================");

        // Register the ExecuteCommand interaction type using getCodecRegistry (like HearthStone does)
        this.getCodecRegistry(Interaction.CODEC).register(
            "ExecuteCommand",
            ExecuteCommandInteraction.class,
            ExecuteCommandInteraction.CODEC
        );

        this.getLogger().at(Level.INFO).log("[HC_CommandItem] Registered ExecuteCommand interaction");

        // Register player connect event to give the menu item
        this.getEventRegistry().register(PlayerConnectEvent.class, (event) -> {
            PlayerRef playerRef = event.getPlayerRef();

            // Delay execution to ensure player entity is fully initialized
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                try {
                    // Get player's CURRENT world (they may have been teleported since connect)
                    Ref<EntityStore> entityRef = playerRef.getReference();
                    if (entityRef == null || !entityRef.isValid()) {
                        this.getLogger().at(Level.WARNING).log(
                            "[HC_CommandItem] Invalid entity ref for " + playerRef.getUsername()
                        );
                        return;
                    }

                    // Get the current world from the entity reference
                    World currentWorld = entityRef.getStore().getExternalData().getWorld();
                    if (currentWorld == null) {
                        this.getLogger().at(Level.WARNING).log(
                            "[HC_CommandItem] No world for " + playerRef.getUsername()
                        );
                        return;
                    }

                    currentWorld.execute(() -> {
                        try {
                            // Re-validate entity ref on world thread
                            Ref<EntityStore> ref = playerRef.getReference();
                            if (ref == null || !ref.isValid()) {
                                return;
                            }

                            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                            if (player == null) {
                                this.getLogger().at(Level.WARNING).log(
                                    "[HC_CommandItem] Player component is null for " + playerRef.getUsername()
                                );
                                return;
                            }

                            giveAndLockMenuItem(player, ref);
                            this.getLogger().at(Level.INFO).log(
                                "[HC_CommandItem] Gave menu item to " + playerRef.getUsername()
                            );
                        } catch (Exception e) {
                            this.getLogger().at(Level.SEVERE).log(
                                "[HC_CommandItem] Failed to give menu item to " + playerRef.getUsername() + ": " + e.getMessage()
                            );
                        }
                    });
                } catch (Exception e) {
                    this.getLogger().at(Level.SEVERE).log(
                        "[HC_CommandItem] Failed to give menu item to " + playerRef.getUsername() + ": " + e.getMessage()
                    );
                }
            }, 1, TimeUnit.SECONDS);
        });

        this.getLogger().at(Level.INFO).log("[HC_CommandItem] Plugin enabled successfully!");
        this.getLogger().at(Level.INFO).log("[HC_CommandItem] Players will receive a menu item in slot 9");
    }

    /**
     * Give the menu item to a player in slot 9 and lock it.
     * If another item is in slot 9, move it to inventory or drop it.
     */
    private void giveAndLockMenuItem(Player player, Ref<EntityStore> entityRef) {
        Inventory inventory = player.getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        ItemContainer storage = inventory.getStorage();

        // Check if player already has the menu item in slot 9
        ItemStack currentItem = hotbar.getItemStack(SLOT_9_INDEX);
        if (currentItem != null && !currentItem.isEmpty()) {
            if (currentItem.getItemId().endsWith(MENU_ITEM_ID)) {
                // Already has the correct item, just ensure filters are set
                setCommandMenuFilters(hotbar);
                return;
            }

            // There's a different item in slot 9, need to move it
            // Try to move it to storage first
            MoveTransaction<ItemStackTransaction> moveResult = hotbar.moveItemStackFromSlot(
                SLOT_9_INDEX,
                currentItem.getQuantity(),
                storage
            );

            // Check if there's a remainder (couldn't fit in storage)
            ItemStack remainder = moveResult.getAddTransaction().getRemainder();
            if (remainder != null && !remainder.isEmpty()) {
                // Drop the remainder on the ground
                ItemUtils.dropItem(entityRef, remainder, entityRef.getStore());
                this.getLogger().at(Level.INFO).log(
                    "[HC_CommandItem] Dropped overflow item: " + remainder.getItemId()
                );
            }
        }

        // Create and place the menu item
        ItemStack menuItem = new ItemStack(MENU_ITEM_ID, 1);
        hotbar.setItemStackForSlot(SLOT_9_INDEX, menuItem);

        // Set filters to protect the command menu item
        setCommandMenuFilters(hotbar);
    }

    /**
     * Set filters to prevent removal/dropping of ONLY the command menu item from slot 9.
     * Other items can be freely moved.
     */
    private void setCommandMenuFilters(ItemContainer hotbar) {
        // Only deny removal if the item is our command menu item
        // Note: itemStack parameter is null for REMOVE/DROP, must get from container
        hotbar.setSlotFilter(FilterActionType.REMOVE, SLOT_9_INDEX,
            (actionType, container, slot, itemStack) -> {
                // Get the actual item in the slot
                ItemStack slotItem = container.getItemStack(slot);
                // Deny removal if it IS our command menu item
                if (slotItem != null && !slotItem.isEmpty() &&
                    slotItem.getItemId().endsWith(MENU_ITEM_ID)) {
                    return false; // Deny
                }
                return true; // Allow removal of other items
            }
        );

        // Only deny dropping if the item is our command menu item
        hotbar.setSlotFilter(FilterActionType.DROP, SLOT_9_INDEX,
            (actionType, container, slot, itemStack) -> {
                // Get the actual item in the slot
                ItemStack slotItem = container.getItemStack(slot);
                // Deny drop if it IS our command menu item
                if (slotItem != null && !slotItem.isEmpty() &&
                    slotItem.getItemId().endsWith(MENU_ITEM_ID)) {
                    return false; // Deny
                }
                return true; // Allow dropping of other items
            }
        );
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        this.getLogger().at(Level.INFO).log("[HC_CommandItem] Plugin disabled");
    }
}
