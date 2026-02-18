/**
 * ============================================================
 * [ResonatorMenu.java]
 * Description: Menu du resonateur avec sync ContainerData et slot visuel abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaMenu               | Base menu            | Player inventory helpers       |
 * | ApicaMenus              | Type registre        | RESONATOR menu type            |
 * | ContainerData           | Sync data            | 5 valeurs (4 onde + hasBee)    |
 * | ApicaSlot               | Slot visuel          | Slot abeille non-interactif    |
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

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ResonatorMenu extends ApicaMenu {

    private final ContainerData data;
    private final BlockPos blockPos;
    private final ItemStackHandler beeDisplay = new ItemStackHandler(1);

    /** Client constructor (from network). */
    public ResonatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(5), buf.readBlockPos(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
    }

    /** Server constructor (from BlockEntity via openMenu buf callback). */
    public ResonatorMenu(int containerId, Inventory playerInventory, ContainerData data,
                         BlockPos pos, ItemStack storedBee) {
        super(ApicaMenus.RESONATOR.get(), containerId);
        this.data = data;
        this.blockPos = pos;
        addDataSlots(data);

        // Slot visuel abeille (non-interactif: pas d'insert, pas d'extract)
        beeDisplay.setStackInSlot(0, storedBee.copy());
        // Slot visuel seulement: pas d'insert (outputOnly) et pas d'extract (inputOnly)
        addSlot(new ApicaSlot(beeDisplay, 0, 178, 6).outputOnly().inputOnly());
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
    /** True si une abeille est posee sur le resonateur. */
    public boolean hasBee() { return data.get(4) == 1; }

    /** Retourne l'ItemStack de l'abeille affichee. */
    public ItemStack getStoredBee() { return beeDisplay.getStackInSlot(0); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
