/**
 * ============================================================
 * [BeeCreatorMenu.java]
 * Description: Menu du Bee Creator — sync ContainerData pour 7 couleurs de parties
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaMenu           | Base menu            | Infrastructure                 |
 * | ApicaMenus          | Type registre        | BEE_CREATOR menu type          |
 * | BeePart             | Enum parties         | COUNT pour ContainerData       |
 * | ContainerData       | Sync data            | 7 couleurs (une par partie)    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorBlockEntity (createMenu)
 * - BeeCreatorScreen (GUI)
 * - ApicaMenus (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class BeeCreatorMenu extends ApicaMenu {

    private final ContainerData data;
    private final BlockPos blockPos;

    /** Client constructor (from network). */
    public BeeCreatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(BeePart.COUNT), buf.readBlockPos());
    }

    /** Server constructor (from BlockEntity via openMenu buf callback). */
    public BeeCreatorMenu(int containerId, Inventory playerInventory, ContainerData data, BlockPos pos) {
        super(ApicaMenus.BEE_CREATOR.get(), containerId);
        this.data = data;
        this.blockPos = pos;
        addDataSlots(data);
    }

    public BlockPos getBlockPos() { return blockPos; }

    /** Couleur de la partie a l'index donne (0xRRGGBB). */
    public int getPartColor(BeePart part) {
        return data.get(part.getIndex());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.blockPosition().distSqr(blockPos) <= 64 * 64;
    }
}
