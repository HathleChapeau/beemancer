/**
 * ============================================================
 * [ItemFilterMenu.java]
 * Description: Menu du filtre d'item pipe — 9 ghost slots, mode, priority
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaMenu               | Base menu            | Player inventory + hotbar      |
 * | ItemPipeBlockEntity     | BE cible             | Acces aux donnees du filtre    |
 * | ItemFilterData          | Donnees filtre       | Ghost slots, mode, priority    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ItemPipeBlock.java (ouverture du menu)
 * - ItemFilterScreen.java (affichage GUI)
 * - ItemFilterActionPacket.java (handler server)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.data.ItemFilterData;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Menu du filtre d'item pipe.
 * Inventaire joueur present pour permettre le pick-up d'items en ghost slots.
 * Les ghost slots sont geres cote client via ItemFilterActionPacket.
 */
public class ItemFilterMenu extends ApicaMenu {

    private static final int PLAYER_INV_Y = 108;

    @Nullable
    private final ItemPipeBlockEntity blockEntity;
    private final ContainerData filterState;

    /** Client constructor. */
    public ItemFilterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    /** Server constructor. */
    public ItemFilterMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ApicaMenus.ITEM_FILTER.get(), containerId);
        this.blockEntity = be instanceof ItemPipeBlockEntity pipe ? pipe : null;

        if (blockEntity != null && blockEntity.getFilter() != null) {
            ItemFilterData filter = blockEntity.getFilter();
            this.filterState = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> filter.getMode().ordinal();
                        case 1 -> filter.getPriority();
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    switch (index) {
                        case 0 -> filter.setMode(value == 0
                            ? ItemFilterData.FilterMode.ACCEPT
                            : ItemFilterData.FilterMode.DENY);
                        case 1 -> filter.setPriority(value);
                    }
                }

                @Override
                public int getCount() {
                    return 2;
                }
            };
        } else {
            this.filterState = new SimpleContainerData(2);
        }

        addDataSlots(filterState);

        // Player inventory + hotbar
        addPlayerInventory(playerInventory, 8, PLAYER_INV_Y);
        addPlayerHotbar(playerInventory, 8, PLAYER_INV_Y + 58);
    }

    @Nullable
    public ItemPipeBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getMode() {
        return filterState.get(0);
    }

    public int getPriority() {
        return filterState.get(1);
    }

    /**
     * Retourne le ghost item du slot specifie (lecture directe depuis le filtre).
     */
    public ItemStack getGhostItem(int slot) {
        if (blockEntity == null || blockEntity.getFilter() == null) return ItemStack.EMPTY;
        return blockEntity.getFilter().getGhostItems().getStackInSlot(slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, 0, -1, -1, null);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        return player.distanceToSqr(
            blockEntity.getBlockPos().getX() + 0.5,
            blockEntity.getBlockPos().getY() + 0.5,
            blockEntity.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }
}
