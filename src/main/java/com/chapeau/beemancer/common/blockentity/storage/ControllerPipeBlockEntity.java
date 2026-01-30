/**
 * ============================================================
 * [ControllerPipeBlockEntity.java]
 * Description: BlockEntity pour le conduit structurel du Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerBlockEntities  | Type registration   | super()                        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ControllerPipeBlock.java (newBlockEntity)
 * - ControllerPipeRenderer.java (lecture spread)
 * - StorageControllerBlockEntity.java (set formed state)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Stocke les données de rendu du conduit formé:
 * - formedSpreadX/Z: décalage en coordonnées monde pour l'écartement
 *
 * La rotation du coude est stockée dans le blockstate (FORMED_ROTATION).
 * Pas de tick — bloc purement visuel.
 */
public class ControllerPipeBlockEntity extends BlockEntity {

    private float formedSpreadX = 0.0f;
    private float formedSpreadZ = 0.0f;

    public ControllerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CONTROLLER_PIPE.get(), pos, state);
    }

    // --- Getters ---

    public float getFormedSpreadX() {
        return formedSpreadX;
    }

    public float getFormedSpreadZ() {
        return formedSpreadZ;
    }

    // --- Setter ---

    /**
     * Configure le décalage de spread pour l'écartement visuel.
     * Appelé par StorageControllerBlockEntity lors de la formation du multibloc.
     */
    public void setFormed(float spreadX, float spreadZ) {
        this.formedSpreadX = spreadX;
        this.formedSpreadZ = spreadZ;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Réinitialise les données formed quand le multibloc est détruit.
     */
    public void clearFormed() {
        this.formedSpreadX = 0.0f;
        this.formedSpreadZ = 0.0f;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (formedSpreadX != 0.0f) {
            tag.putFloat("FormedSpreadX", formedSpreadX);
        }
        if (formedSpreadZ != 0.0f) {
            tag.putFloat("FormedSpreadZ", formedSpreadZ);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formedSpreadX = tag.getFloat("FormedSpreadX");
        formedSpreadZ = tag.getFloat("FormedSpreadZ");
    }

    // --- Sync Client ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (formedSpreadX != 0.0f) {
            tag.putFloat("FormedSpreadX", formedSpreadX);
        }
        if (formedSpreadZ != 0.0f) {
            tag.putFloat("FormedSpreadZ", formedSpreadZ);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
