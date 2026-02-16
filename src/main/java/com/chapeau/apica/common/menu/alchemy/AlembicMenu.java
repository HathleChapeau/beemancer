/**
 * ============================================================
 * [AlembicMenu.java]
 * Description: Menu pour l'alambic multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | AlembicHeartBlockEntity     | Stockage etat        | Accesseurs tanks/data |
 * | ApicaBlocks             | Validation block     | stillValid            |
 * | ApicaMenus              | Type menu            | Registration          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AlembicHeartBlockEntity.java (createMenu)
 * - AlembicScreen.java (client GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.AlembicHeartBlockEntity;
import com.chapeau.apica.common.menu.ApicaMenu;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AlembicMenu extends ApicaMenu {
    private final AlembicHeartBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AlembicMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(5));
    }

    public AlembicMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.ALEMBIC.get(), containerId);
        this.blockEntity = (AlembicHeartBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Player inventory (centered in 190px container)
        addPlayerInventory(playerInv, 15, 107);
        addPlayerHotbar(playerInv, 15, 165);
    }

    public AlembicHeartBlockEntity getBlockEntity() { return blockEntity; }
    public int getProgress() { return data.get(0); }
    public int getProcessTime() { return data.get(1); }
    public int getHoneyAmount() { return data.get(2); }
    public int getRoyalJellyAmount() { return data.get(3); }
    public int getNectarAmount() { return data.get(4); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (!moveItemStackTo(stack, 0, 36, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.ALEMBIC_HEART.get());
    }
}
