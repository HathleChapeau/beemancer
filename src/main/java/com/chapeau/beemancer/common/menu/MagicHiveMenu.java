/**
 * ============================================================
 * [MagicHiveMenu.java]
 * Description: Menu de la ruche magique avec slots bee et output
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.menu.slot.BeeAssignmentSlot;
import com.chapeau.beemancer.common.menu.slot.OutputOnlySlot;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MagicHiveMenu extends BeemancerMenu {
    private final Container container;
    private final ContainerData data;
    
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

    // Client constructor
    public MagicHiveMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(MagicHiveBlockEntity.TOTAL_SLOTS), new SimpleContainerData(10));
    }

    // Server constructor
    public MagicHiveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(BeemancerMenus.MAGIC_HIVE.get(), containerId);
        this.container = container;
        this.data = data;

        // Track data for client sync
        addDataSlots(data);

        // Bee assignment slots (5 slots in a row at top, centered in 216px container)
        int beeSlotY = 17;
        int beeSlotStartX = 64;
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            addSlot(new BeeAssignmentSlot(container, i, beeSlotStartX + i * 18, beeSlotY));
        }

        // Output slots (7 honeycomb layout, centered in 216px container)
        int honeycombCenterX = 108;
        int honeycombCenterY = 70;
        for (int i = 0; i < MagicHiveBlockEntity.OUTPUT_SLOTS; i++) {
            int slotIndex = MagicHiveBlockEntity.BEE_SLOTS + i;
            int x = honeycombCenterX + HONEYCOMB_OFFSETS[i][0] - 8;
            int y = honeycombCenterY + HONEYCOMB_OFFSETS[i][1] - 8;
            addSlot(new OutputOnlySlot(container, slotIndex, x, y));
        }

        // Player inventory (centered in 216px container: (216-162)/2 = 27, slot at 28)
        addPlayerInventory(playerInventory, 28, 122);
        addPlayerHotbar(playerInventory, 28, 180);
    }

    /**
     * @return true si le crystal antibreeding est présent (pas de reproduction)
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

    /** @return la température du biome (-2 à 2) */
    public int getTemperature() {
        return data.get(4) - 2; // Retirer l'offset
    }

    /** @return true si l'abeille dans le slot peut aller butiner */
    public boolean canBeeForage(int slot) {
        if (slot < 0 || slot >= MagicHiveBlockEntity.BEE_SLOTS) return false;
        return data.get(5 + slot) != 0;
    }

    // Alias pour compatibilité
    public boolean isBreedingMode() {
        return isAntibreedingMode();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, MagicHiveBlockEntity.TOTAL_SLOTS,
                           0, MagicHiveBlockEntity.BEE_SLOTS, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public Container getContainer() {
        return container;
    }
}
