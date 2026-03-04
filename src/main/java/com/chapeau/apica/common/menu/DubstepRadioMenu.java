/**
 * ============================================================
 * [DubstepRadioMenu.java]
 * Description: Menu du Dubstep Radio — sync ContainerData (BPM, pageCount, transport, volume)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaMenu           | Base menu            | Infrastructure slots           |
 * | ApicaMenus          | Type registre        | DUBSTEP_RADIO menu type        |
 * | ContainerData       | Sync data            | 3 valeurs (BPM, pages, playing)|
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioBlockEntity (createMenu)
 * - DubstepRadioScreen (GUI)
 * - ApicaMenus (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu sans inventaire joueur pour le DAW.
 * Synchronise BPM, pageCount et transport via ContainerData.
 */
public class DubstepRadioMenu extends ApicaMenu {

    private static final int DATA_COUNT = 3;

    private final ContainerData data;
    private final BlockPos blockPos;

    /** Client constructor (from network). */
    public DubstepRadioMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT), buf.readBlockPos());
    }

    /** Server constructor (from BlockEntity via openMenu buf callback). */
    public DubstepRadioMenu(int containerId, Inventory playerInventory, ContainerData data, BlockPos pos) {
        super(ApicaMenus.DUBSTEP_RADIO.get(), containerId);
        this.data = data;
        this.blockPos = pos;
        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    /** BPM (40-300). */
    public int getBpm() {
        return data.get(0);
    }

    /** Nombre de pages (1-8). */
    public int getPageCount() {
        return data.get(1);
    }

    /** True si la sequence joue. */
    public boolean isPlaying() {
        return data.get(2) == 1;
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
