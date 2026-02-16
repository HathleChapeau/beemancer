/**
 * ============================================================
 * [ResonatorBlockEntity.java]
 * Description: Stocke les 4 parametres d'onde (freq, amp, phase, harmonics)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Type enregistre      | Construction                   |
 * | ResonatorMenu           | Menu associe         | createMenu()                   |
 * | ContainerData           | Sync serveur→client  | 4 valeurs int                  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorBlock (newBlockEntity, openMenu)
 * - ResonatorMenu (server constructor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.resonator;

import com.chapeau.apica.common.menu.ResonatorMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class ResonatorBlockEntity extends BlockEntity implements MenuProvider {

    /** Frequency in Hz (1-80), stored as raw int. */
    private int frequency = 20;
    /** Amplitude 0-100 (displayed as 0.0-1.0). */
    private int amplitude = 70;
    /** Phase 0-360 (degrees). */
    private int phase = 0;
    /** Harmonics 0-100 (displayed as 0.0-1.0). */
    private int harmonics = 0;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> frequency;
                case 1 -> amplitude;
                case 2 -> phase;
                case 3 -> harmonics;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> frequency = value;
                case 1 -> amplitude = value;
                case 2 -> phase = value;
                case 3 -> harmonics = value;
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.RESONATOR.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.resonator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(containerId, playerInventory, containerData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Frequency", frequency);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Phase", phase);
        tag.putInt("Harmonics", harmonics);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = tag.getInt("Frequency");
        amplitude = tag.getInt("Amplitude");
        phase = tag.getInt("Phase");
        harmonics = tag.getInt("Harmonics");
    }

    public void setFrequency(int freq) { this.frequency = freq; setChanged(); }
    public void setAmplitude(int amp) { this.amplitude = amp; setChanged(); }
    public void setPhase(int ph) { this.phase = ph; setChanged(); }
    public void setHarmonics(int harm) { this.harmonics = harm; setChanged(); }
}
