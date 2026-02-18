/**
 * ============================================================
 * [ResonatorBlockEntity.java]
 * Description: Stocke les 4 parametres d'onde et une abeille optionnelle
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Type enregistre      | Construction                   |
 * | ResonatorMenu           | Menu associe         | createMenu()                   |
 * | ContainerData           | Sync serveur→client  | 5 valeurs (4 onde + hasBee)    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorBlock (newBlockEntity, openMenu, bee placement)
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
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

    /** Abeille stockee sur le resonateur (visuel + target waveform). */
    private ItemStack storedBee = ItemStack.EMPTY;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> frequency;
                case 1 -> amplitude;
                case 2 -> phase;
                case 3 -> harmonics;
                case 4 -> storedBee.isEmpty() ? 0 : 1;
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
            return 5;
        }
    };

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.RESONATOR.get(), pos, state);
    }

    // ========== BEE STORAGE ==========

    public boolean hasBee() {
        return !storedBee.isEmpty();
    }

    public ItemStack getStoredBee() {
        return storedBee;
    }

    /**
     * Place une abeille sur le resonateur.
     * @param stack l'item abeille (sera copie avec count=1)
     * @return true si l'abeille a ete placee
     */
    public boolean placeBee(ItemStack stack) {
        if (stack.isEmpty()) return false;
        storedBee = stack.copyWithCount(1);
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Retire l'abeille du resonateur.
     * @return l'abeille retiree ou ItemStack.EMPTY
     */
    public ItemStack removeBee() {
        if (storedBee.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = storedBee.copy();
        storedBee = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return removed;
    }

    // ========== MENU ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.resonator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(containerId, playerInventory, containerData, worldPosition, storedBee);
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Frequency", frequency);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Phase", phase);
        tag.putInt("Harmonics", harmonics);
        if (!storedBee.isEmpty()) {
            tag.put("StoredBee", storedBee.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = tag.getInt("Frequency");
        amplitude = tag.getInt("Amplitude");
        phase = tag.getInt("Phase");
        harmonics = tag.getInt("Harmonics");
        if (tag.contains("StoredBee")) {
            storedBee = ItemStack.parse(registries, tag.getCompound("StoredBee"))
                    .orElse(ItemStack.EMPTY);
        } else {
            storedBee = ItemStack.EMPTY;
        }
    }

    // ========== CLIENT SYNC ==========

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Frequency", frequency);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Phase", phase);
        tag.putInt("Harmonics", harmonics);
        if (!storedBee.isEmpty()) {
            tag.put("StoredBee", storedBee.save(registries, new CompoundTag()));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    // ========== SETTERS ==========

    public void setFrequency(int freq) { this.frequency = freq; setChanged(); }
    public void setAmplitude(int amp) { this.amplitude = amp; setChanged(); }
    public void setPhase(int ph) { this.phase = ph; setChanged(); }
    public void setHarmonics(int harm) { this.harmonics = harm; setChanged(); }
}
