/**
 * ============================================================
 * [HoneyTankMenu.java]
 * Description: Menu pour le tank avec slot de bucket
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class HoneyTankMenu extends BeemancerMenu {
    protected final HoneyTankBlockEntity blockEntity;
    protected final ContainerLevelAccess access;
    protected final ContainerData data;

    // Slot indices
    private static final int BUCKET_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public HoneyTankMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(1));
    }

    public HoneyTankMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.HONEY_TANK.get(), containerId);
        this.blockEntity = (HoneyTankBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Bucket slot (reduced_bg layout: 110px panel centre dans 176px, panel offset=33)
        addSlot(new SlotItemHandler(blockEntity.getBucketSlot(), 0, 33 + 15, 38));

        // Player inventory (centered in 176px container)
        addPlayerInventory(playerInv, 7, 107);
        addPlayerHotbar(playerInv, 7, 165);
    }

    protected HoneyTankMenu(MenuType<?> type, int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(type, containerId);
        this.blockEntity = (HoneyTankBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Bucket slot (reduced_bg layout)
        addSlot(new SlotItemHandler(blockEntity.getBucketSlot(), 0, 33 + 15, 38));

        // Player inventory (centered in 176px container)
        addPlayerInventory(playerInv, 7, 107);
        addPlayerHotbar(playerInv, 7, 165);
    }

    public HoneyTankBlockEntity getBlockEntity() { return blockEntity; }
    public int getFluidAmount() { return data.get(0); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, PLAYER_INV_START, BUCKET_SLOT, BUCKET_SLOT + 1,
                           stack -> blockEntity.getBucketSlot().isItemValid(0, stack));
    }

    protected Block getValidBlock() {
        return BeemancerBlocks.HONEY_TANK.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, getValidBlock());
    }
}
