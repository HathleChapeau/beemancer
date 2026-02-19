/**
 * ============================================================
 * [ApicaFurnaceMenu.java]
 * Description: Menu pour les fours Apica (honey, royal, nectar)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | ApicaFurnaceBlockEntity     | BlockEntity source   | Slots et fluides               |
 * | ApicaMenu                   | Base menu            | Inventaire joueur              |
 * | ApicaSlot                   | Slots custom         | Input/output                   |
 * | ApicaMenus                  | Registre menu        | Type menu                      |
 * ------------------------------------------------------------
 *
 * SLOTS (single):
 * - 0: input
 * - 1: output
 * - 2-28: player inventory
 * - 29-37: hotbar
 *
 * SLOTS (dual):
 * - 0-1: inputs
 * - 2-3: outputs
 * - 4-30: player inventory
 * - 31-39: hotbar
 *
 * UTILISE PAR:
 * - ApicaFurnaceBlockEntity (createMenu)
 * - ApicaFurnaceScreen (client GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.alchemy.ApicaFurnaceBlockEntity;
import com.chapeau.apica.common.menu.ApicaMenu;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ApicaFurnaceMenu extends ApicaMenu {
    private final BlockEntity blockEntity;
    private final FluidTank fuelTank;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final boolean dualSlot;

    private final int containerSlotCount;

    // Client constructor
    public ApicaFurnaceMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(6));
    }

    // Server constructor
    public ApicaFurnaceMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.APICA_FURNACE.get(), containerId);
        this.blockEntity = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        ApicaFurnaceBlockEntity furnace = (ApicaFurnaceBlockEntity) be;
        ItemStackHandler inputHandler = furnace.getInputSlots();
        ItemStackHandler outputHandler = furnace.getOutputSlots();
        this.fuelTank = furnace.getFuelTank();
        this.dualSlot = furnace.isDualSlot();

        addDataSlots(data);

        // Panel offset = 33 (reduced_bg centree dans 176px)
        // Slot positions = panelOffset + renderSlotX + 1 (slot bg = 18x18, item = 16x16 centre)
        if (dualSlot) {
            addSlot(new ApicaSlot(inputHandler, 0, 72, 31));
            addSlot(new ApicaSlot(inputHandler, 1, 72, 51));
            addSlot(ApicaSlot.output(outputHandler, 0, 114, 31));
            addSlot(ApicaSlot.output(outputHandler, 1, 114, 51));
            this.containerSlotCount = 4;
        } else {
            addSlot(new ApicaSlot(inputHandler, 0, 72, 41));
            addSlot(ApicaSlot.output(outputHandler, 0, 114, 41));
            this.containerSlotCount = 2;
        }

        addPlayerInventory(playerInv, 7, 107);
        addPlayerHotbar(playerInv, 7, 165);
    }

    public int getProgress0() { return data.get(0); }
    public int getMaxProgress0() { return data.get(1); }
    public int getProgress1() { return data.get(2); }
    public int getMaxProgress1() { return data.get(3); }
    public int getFuelAmount() { return data.get(4); }
    public int getFuelCapacity() { int c = data.get(5); return c > 0 ? c : 8000; }

    public float getProgressRatio0() {
        int max = getMaxProgress0();
        return max > 0 ? (float) getProgress0() / max : 0;
    }

    public float getProgressRatio1() {
        int max = getMaxProgress1();
        return max > 0 ? (float) getProgress1() / max : 0;
    }

    public boolean isDualSlot() { return dualSlot; }
    public BlockEntity getBlockEntity() { return blockEntity; }
    public FluidTank getFuelTank() { return fuelTank; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int inputEnd = dualSlot ? 2 : 1;
        return doQuickMove(index, containerSlotCount, 0, inputEnd, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.HONEY_FURNACE.get())
            || stillValid(access, player, ApicaBlocks.ROYAL_FURNACE.get())
            || stillValid(access, player, ApicaBlocks.NECTAR_FURNACE.get());
    }
}
