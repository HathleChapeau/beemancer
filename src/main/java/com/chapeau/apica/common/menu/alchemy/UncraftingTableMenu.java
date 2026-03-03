/**
 * ============================================================
 * [UncraftingTableMenu.java]
 * Description: Menu pour l'Uncrafting Table — layout vanilla 176x166
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|----------------------|--------------------------------|
 * | UncraftingTableBlockEntity      | BlockEntity source   | Slots et fluides               |
 * | ApicaMenu                       | Base menu            | Inventaire joueur              |
 * | ApicaSlot                       | Slots custom         | Input/output                   |
 * | ApicaMenus                      | Registre menu        | Type menu                      |
 * ------------------------------------------------------------
 *
 * SLOTS:
 * - 0: input (item a decomposer)
 * - 1-9: output (grille 3x3 des ingredients)
 * - 10-36: player inventory
 * - 37-45: hotbar
 *
 * UTILISE PAR:
 * - UncraftingTableBlockEntity (createMenu)
 * - UncraftingTableScreen (client GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.artifacts.UncraftingTableBlockEntity;
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

public class UncraftingTableMenu extends ApicaMenu {
    private final BlockEntity blockEntity;
    private final FluidTank nectarTank;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    private static final int CONTAINER_SLOT_COUNT = 10;

    /** Client constructor. */
    public UncraftingTableMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    /** Server constructor. */
    public UncraftingTableMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.UNCRAFTING_TABLE.get(), containerId);
        this.blockEntity = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        UncraftingTableBlockEntity table = (UncraftingTableBlockEntity) be;
        ItemStackHandler inputHandler = table.getInputSlot();
        ItemStackHandler outputHandler = table.getOutputSlots();
        this.nectarTank = table.getNectarTank();

        addDataSlots(data);

        // Slot 0: input (gauche)
        addSlot(new ApicaSlot(inputHandler, 0, 48, 35));

        // Slots 1-9: output grille 3x3 (droite)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(ApicaSlot.output(outputHandler, row * 3 + col,
                        106 + col * 18, 17 + row * 18));
            }
        }

        // Slots 10-45: player inventory + hotbar (positions vanilla standard)
        addPlayerInventory(playerInv, 8, 84);
        addPlayerHotbar(playerInv, 8, 142);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getFluidAmount() { return data.get(2); }
    public int getFluidCapacity() { int c = data.get(3); return c > 0 ? c : 4000; }

    public float getProgressRatio() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }

    public BlockEntity getBlockEntity() { return blockEntity; }
    public FluidTank getNectarTank() { return nectarTank; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, CONTAINER_SLOT_COUNT, 0, 1, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.UNCRAFTING_TABLE.get());
    }
}
