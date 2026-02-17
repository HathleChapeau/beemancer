/**
 * ============================================================
 * [InjectorMenu.java]
 * Description: Menu de l'injecteur d'essence avec slots abeille et essence
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaSlot               | Slots filtrables     | Slot abeille + slot essence    |
 * | InjectorBlockEntity     | Donnees serveur      | Inventaire + ContainerData     |
 * | ApicaMenu               | Base menu            | Player inventory + quickMove   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InjectorBlockEntity.java (creation menu)
 * - InjectorScreen.java (client GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaItems;
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

public class InjectorMenu extends ApicaMenu {

    private final InjectorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    private static final int BEE_SLOT = 0;
    private static final int ESSENCE_SLOT = 1;
    private static final int CONTAINER_SLOTS = 2;
    private static final int PLAYER_INV_START = CONTAINER_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    // Client constructor
    public InjectorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(10));
    }

    // Server constructor
    public InjectorMenu(int containerId, Inventory playerInventory, BlockEntity be, ContainerData data) {
        super(ApicaMenus.INJECTOR.get(), containerId);
        this.blockEntity = (InjectorBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Essence slot (gauche)
        addSlot(new ApicaSlot(blockEntity.getItemHandler(), BEE_SLOT, 35, 38)
                .withFilter(stack -> stack.getItem() instanceof EssenceItem
                        && !(stack.getItem() instanceof SpeciesEssenceItem)));

        // Bee slot (droite)
        addSlot(new ApicaSlot(blockEntity.getItemHandler(), ESSENCE_SLOT, 137, 38)
                .withFilter(stack -> stack.is(ApicaItems.MAGIC_BEE.get())));

        // Player inventory
        addPlayerInventory(playerInventory, 15, 137);
        addPlayerHotbar(playerInventory, 15, 195);
    }

    public InjectorBlockEntity getBlockEntity() { return blockEntity; }

    // ========== DATA GETTERS ==========

    public float getProgressRatio() {
        int time = data.get(1);
        if (time <= 0) return 0;
        return (float) data.get(0) / time;
    }

    public float getHungerRatio() {
        int max = data.get(3);
        if (max <= 0) return 0;
        return (float) data.get(2) / max;
    }

    public int getHunger() { return data.get(2); }
    public int getMaxHunger() { return data.get(3); }
    public int getDropPoints() { return data.get(4); }
    public int getSpeedPoints() { return data.get(5); }
    public int getForagingPoints() { return data.get(6); }
    public int getTolerancePoints() { return data.get(7); }
    public int getActivityPoints() { return data.get(8); }
    public boolean isSatiated() { return data.get(9) == 1; }
    public boolean hasBee() { return !blockEntity.getItemHandler().getStackInSlot(BEE_SLOT).isEmpty(); }

    // ========== QUICK MOVE ==========

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index < CONTAINER_SLOTS) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stackInSlot, 0, CONTAINER_SLOTS, false)) {
                    if (index < PLAYER_INV_END) {
                        if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.INJECTOR.get());
    }
}
