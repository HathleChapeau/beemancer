/**
 * ============================================================
 * [MagicHiveMenu.java]
 * Description: Menu de la ruche magique avec slots bee et output (small et multibloc)
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.common.block.hive.HiveMultiblockBlockEntity;
import com.chapeau.apica.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.apica.common.menu.slot.BeeAssignmentSlot;
import com.chapeau.apica.common.menu.slot.OutputOnlySlot;
import com.chapeau.apica.common.quest.QuestEvents;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MagicHiveMenu extends ApicaMenu {
    private final Container container;
    private final ContainerData data;
    private final boolean isMultiblock;
    private final int beeSlotCount;

    // Honeycomb layout offsets (center + 6 surrounding)
    private static final int[][] HONEYCOMB_OFFSETS = {
        {0, 0},      // Center
        {-10, -17},  // Top-left
        {10, -17},   // Top-right
        {-10, 17},   // Bottom-left
        {10, 17},    // Bottom-right
        {-20, 0},    // Left
        {20, 0}      // Right
    };

    // Layout constants for small hive (bg_beehive_small 142x110, imageWidth=176)
    private static final int SMALL_PANEL_W = 142;
    private static final int SMALL_PANEL_OFFSET = (176 - SMALL_PANEL_W) / 2; // 17
    private static final int SMALL_BEE_START_X = SMALL_PANEL_OFFSET + 52;
    private static final int SMALL_BEE_Y = 17;
    private static final int SMALL_COMB_CENTER_X = SMALL_PANEL_OFFSET + 71;
    private static final int SMALL_COMB_CENTER_Y = 70;
    private static final int SMALL_INV_X = 7;
    private static final int SMALL_INV_Y = 122;
    private static final int SMALL_HOTBAR_Y = 180;

    // Layout constants for multiblock hive (bg_beehive 216x110)
    private static final int LARGE_BEE_START_X = 56;
    private static final int LARGE_BEE_Y = 17;
    private static final int LARGE_COMB_CENTER_X = 108;
    private static final int LARGE_COMB_CENTER_Y = 70;
    private static final int LARGE_INV_X = 28;
    private static final int LARGE_INV_Y = 122;
    private static final int LARGE_HOTBAR_Y = 180;

    // Client constructor
    public MagicHiveMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
             buf.readBoolean()
                 ? new SimpleContainer(HiveMultiblockBlockEntity.TOTAL_SLOTS)
                 : new SimpleContainer(MagicHiveBlockEntity.TOTAL_SLOTS),
             new SimpleContainerData(11),
             false); // placeholder, will be overridden
        // Note: isMultiblock is determined from container size in the main constructor
    }

    // Server constructor
    public MagicHiveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data,
             container.getContainerSize() == HiveMultiblockBlockEntity.TOTAL_SLOTS);
        if (container.getContainerSize() == HiveMultiblockBlockEntity.TOTAL_SLOTS) {
            QuestEvents.onMenuOpen(playerInventory.player, "hive_multiblock");
        }
    }

    private MagicHiveMenu(int containerId, Inventory playerInventory, Container container,
                           ContainerData data, boolean isMultiblockHint) {
        super(ApicaMenus.MAGIC_HIVE.get(), containerId);
        this.container = container;
        this.data = data;
        this.isMultiblock = container.getContainerSize() == HiveMultiblockBlockEntity.TOTAL_SLOTS;
        this.beeSlotCount = isMultiblock ? HiveMultiblockBlockEntity.BEE_SLOTS : MagicHiveBlockEntity.BEE_SLOTS;

        addDataSlots(data);

        if (isMultiblock) {
            setupLargeLayout(playerInventory);
        } else {
            setupSmallLayout(playerInventory);
        }
    }

    private void setupSmallLayout(Inventory playerInventory) {
        // Bee assignment slots (2 slots)
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            addSlot(new BeeAssignmentSlot(container, i, SMALL_BEE_START_X + i * 22, SMALL_BEE_Y));
        }

        // Output slots (7 honeycomb layout)
        for (int i = 0; i < MagicHiveBlockEntity.OUTPUT_SLOTS; i++) {
            int slotIndex = MagicHiveBlockEntity.BEE_SLOTS + i;
            int x = SMALL_COMB_CENTER_X + HONEYCOMB_OFFSETS[i][0] - 8;
            int y = SMALL_COMB_CENTER_Y + HONEYCOMB_OFFSETS[i][1] - 8;
            addSlot(new OutputOnlySlot(container, slotIndex, x, y));
        }

        // Player inventory (centered in 176px)
        addPlayerInventory(playerInventory, SMALL_INV_X, SMALL_INV_Y);
        addPlayerHotbar(playerInventory, SMALL_INV_X, SMALL_HOTBAR_Y);
    }

    private void setupLargeLayout(Inventory playerInventory) {
        // Bee assignment slots (5 slots)
        for (int i = 0; i < HiveMultiblockBlockEntity.BEE_SLOTS; i++) {
            addSlot(new BeeAssignmentSlot(container, i, LARGE_BEE_START_X + i * 22, LARGE_BEE_Y));
        }

        // Output slots (7 honeycomb layout)
        for (int i = 0; i < HiveMultiblockBlockEntity.OUTPUT_SLOTS; i++) {
            int slotIndex = HiveMultiblockBlockEntity.BEE_SLOTS + i;
            int x = LARGE_COMB_CENTER_X + HONEYCOMB_OFFSETS[i][0] - 8;
            int y = LARGE_COMB_CENTER_Y + HONEYCOMB_OFFSETS[i][1] - 8;
            addSlot(new OutputOnlySlot(container, slotIndex, x, y));
        }

        // Player inventory (centered in 216px: (216-162)/2 = 27 → slot start at 28)
        addPlayerInventory(playerInventory, LARGE_INV_X, LARGE_INV_Y);
        addPlayerHotbar(playerInventory, LARGE_INV_X, LARGE_HOTBAR_Y);
    }

    public boolean isMultiblock() { return isMultiblock; }
    public int getBeeSlotCount() { return beeSlotCount; }

    /**
     * @return true si le crystal antibreeding est present (pas de reproduction)
     */
    public boolean isAntibreedingMode() {
        return data.get(0) != 0;
    }

    /** @return true si des fleurs sont disponibles */
    public boolean hasFlowers() {
        return data.get(1) != 0;
    }

    /** @return true si des champignons sont disponibles */
    public boolean hasMushrooms() {
        return data.get(2) != 0;
    }

    /** @return true si c'est le jour */
    public boolean isDaytime() {
        return data.get(3) != 0;
    }

    /** @return la temperature du biome (-2 a 2) */
    public int getTemperature() {
        return data.get(4) - 2;
    }

    /** @return true si une ruche voisine est trop proche (rayon 4) */
    public boolean isCrowded() {
        return data.get(10) != 0;
    }

    /** @return true si l'abeille dans le slot peut aller butiner */
    public boolean canBeeForage(int slot) {
        if (slot < 0 || slot >= beeSlotCount) return false;
        return data.get(5 + slot) != 0;
    }

    // Alias pour compatibilite
    public boolean isBreedingMode() {
        return isAntibreedingMode();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int totalSlots = beeSlotCount + 7; // bee + output
        return doQuickMove(index, totalSlots, 0, beeSlotCount, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public Container getContainer() {
        return container;
    }
}
