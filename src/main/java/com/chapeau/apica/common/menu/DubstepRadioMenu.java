/**
 * ============================================================
 * [DubstepRadioMenu.java]
 * Description: Menu du Dubstep Radio — sync ContainerData (BPM, transport, swing, volume)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaMenu           | Base menu            | Infrastructure slots           |
 * | ApicaMenus          | Type registre        | DUBSTEP_RADIO menu type        |
 * | ContainerData       | Sync data            | 5 valeurs (BPM, steps, etc.)   |
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
 * Synchronise BPM, steps, transport, swing et volume via ContainerData.
 */
public class DubstepRadioMenu extends ApicaMenu {

    private static final int DATA_COUNT = 5;

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

    /** Nombre de steps actifs. */
    public int getStepCount() {
        return data.get(1);
    }

    /** True si la sequence joue. */
    public boolean isPlaying() {
        return data.get(2) == 1;
    }

    /** Swing 0-100 (affiche comme 0-100%). */
    public int getSwing() {
        return data.get(3);
    }

    /** Master volume 0-100. */
    public int getMasterVolume() {
        return data.get(4);
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
