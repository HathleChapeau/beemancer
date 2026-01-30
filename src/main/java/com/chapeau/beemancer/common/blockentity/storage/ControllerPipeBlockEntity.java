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
 * - ControllerPipeRenderer.java (lecture rotation/spread)
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
 * - formedRotation (0-7): direction du coude + flip vertical
 * - formedSpreadX/Z: décalage en coordonnées monde pour l'écartement
 *
 * Pas de tick — bloc purement visuel.
 */
public class ControllerPipeBlockEntity extends BlockEntity {

    private int formedRotation = 0;
    private float formedSpreadX = 0.0f;
    private float formedSpreadZ = 0.0f;

    public ControllerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CONTROLLER_PIPE.get(), pos, state);
    }

    // --- Getters ---

    public int getFormedRotation() {
        return formedRotation;
    }

    public float getFormedSpreadX() {
        return formedSpreadX;
    }

    public float getFormedSpreadZ() {
        return formedSpreadZ;
    }

    // --- Setter ---

    /**
     * Configure l'état formé: rotation du coude et décalage de spread.
     * Appelé par StorageControllerBlockEntity lors de la formation du multibloc.
     */
    public void setFormed(int rotation, float spreadX, float spreadZ) {
        this.formedRotation = rotation;
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
        this.formedRotation = 0;
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
        tag.putInt("FormedRotation", formedRotation);
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
        formedRotation = tag.getInt("FormedRotation");
        formedSpreadX = tag.getFloat("FormedSpreadX");
        formedSpreadZ = tag.getFloat("FormedSpreadZ");
    }

    // --- Sync Client ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("FormedRotation", formedRotation);
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
