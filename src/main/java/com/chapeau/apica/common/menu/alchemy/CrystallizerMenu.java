/**
 * ============================================================
 * [CrystallizerMenu.java]
 * Description: Menu pour le cristalliseur
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.alchemy.CrystallizerBlockEntity;
import com.chapeau.apica.common.menu.ApicaMenu;
import com.chapeau.apica.common.quest.QuestEvents;
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

public class CrystallizerMenu extends ApicaMenu {
    private final CrystallizerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Slot indices
    private static final int OUTPUT_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public CrystallizerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(3));
    }

    public CrystallizerMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.CRYSTALLIZER.get(), containerId);
        this.blockEntity = (CrystallizerBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;
        
        addDataSlots(data);

        // Output slot (extraction seulement) avec callback pour les quêtes
        // Positions pour reduced_bg (110px panel centre dans 176px container, panel offset=33)
        addSlot(ApicaSlot.output(blockEntity.getOutputSlot(), 0, 33 + 63, 39)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "crystallizer", s)));

        // Player inventory (centered in 176px container)
        addPlayerInventory(playerInv, 7, 107);
        addPlayerHotbar(playerInv, 7, 165);
    }

    public CrystallizerBlockEntity getBlockEntity() { return blockEntity; }
    public int getProgress() { return data.get(0); }
    public int getProcessTime() { return data.get(1); }
    public int getFluidAmount() { return data.get(2); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, PLAYER_INV_START, -1, -1, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.CRYSTALLIZER.get());
    }
}
