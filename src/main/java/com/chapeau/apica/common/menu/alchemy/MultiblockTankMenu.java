/**
 * ============================================================
 * [MultiblockTankMenu.java]
 * Description: Menu pour le tank multibloc avec slot de bucket
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.MultiblockTankBlockEntity;
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
import net.neoforged.neoforge.items.SlotItemHandler;

public class MultiblockTankMenu extends ApicaMenu {
    protected final MultiblockTankBlockEntity blockEntity;
    protected final ContainerLevelAccess access;
    protected final ContainerData data;

    // Slot indices
    private static final int BUCKET_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public MultiblockTankMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(4));
    }

    private static MultiblockTankBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        BlockEntity be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof MultiblockTankBlockEntity tank) {
            MultiblockTankBlockEntity master = tank.getMaster();
            return master != null ? master : tank;
        }
        return null;
    }

    public MultiblockTankMenu(int containerId, Inventory playerInv, MultiblockTankBlockEntity be, ContainerData data) {
        super(ApicaMenus.MULTIBLOCK_TANK.get(), containerId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Bucket slot (reduced_bg layout: 110px panel centre dans 176px, panel offset=33)
        addSlot(new SlotItemHandler(blockEntity.getBucketSlot(), 0, 33 + 63, 39));

        // Player inventory (centered in 176px container)
        addPlayerInventory(playerInv, 7, 107);
        addPlayerHotbar(playerInv, 7, 165);
    }

    public MultiblockTankBlockEntity getBlockEntity() { return blockEntity; }
    public int getFluidAmount() { return data.get(0); }
    public int getCapacity() { return data.get(1); }
    public int getBlockCount() { return data.get(2); }
    public boolean isValidCuboid() { return data.get(3) == 1; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, PLAYER_INV_START, BUCKET_SLOT, BUCKET_SLOT + 1,
                           stack -> blockEntity.getBucketSlot().isItemValid(0, stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.MULTIBLOCK_TANK.get());
    }
}
