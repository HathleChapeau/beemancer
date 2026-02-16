/**
 * ============================================================
 * [ResonatorMenu.java]
 * Description: Menu du resonateur (pas de slots, sync ContainerData uniquement)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaMenu               | Base menu            | Player inventory helpers       |
 * | ApicaMenus              | Type registre        | RESONATOR menu type            |
 * | ContainerData           | Sync data            | 4 valeurs onde                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorBlockEntity (createMenu)
 * - ResonatorScreen (GUI)
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

public class ResonatorMenu extends ApicaMenu {

    private final ContainerData data;
    private final BlockPos blockPos;

    /** Client constructor (from network). */
    public ResonatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(4), buf.readBlockPos());
    }

    /** Server constructor (from BlockEntity). */
    public ResonatorMenu(int containerId, Inventory playerInventory, ContainerData data) {
        this(containerId, playerInventory, data, BlockPos.ZERO);
    }

    private ResonatorMenu(int containerId, Inventory playerInventory, ContainerData data, BlockPos pos) {
        super(ApicaMenus.RESONATOR.get(), containerId);
        this.data = data;
        this.blockPos = pos;
        addDataSlots(data);
    }

    public BlockPos getBlockPos() { return blockPos; }

    /** Frequency in Hz (1-80). */
    public int getFrequency() { return data.get(0); }
    /** Amplitude 0-100. */
    public int getAmplitude() { return data.get(1); }
    /** Phase 0-360 degrees. */
    public int getPhase() { return data.get(2); }
    /** Harmonics 0-100. */
    public int getHarmonics() { return data.get(3); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
