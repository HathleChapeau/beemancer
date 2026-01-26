/**
 * ============================================================
 * [ExtractorHeartBlockEntity.java]
 * Description: BlockEntity du cœur de l'extracteur d'essence
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockController| Interface contrôleur | Implémentation        |
 * | MultiblockPatterns  | Définition pattern   | ESSENCE_EXTRACTOR     |
 * | MultiblockValidator | Validation           | tryFormExtractor()    |
 * | MultiblockEvents    | Enregistrement       | Détection destruction |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ExtractorHeartBlock.java (création BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.extractor;

import com.chapeau.beemancer.common.block.extractor.ExtractorHeartBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
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
 * BlockEntity du cœur de l'extracteur d'essence.
 * Implémente MultiblockController pour gérer l'Essence Extractor.
 */
public class ExtractorHeartBlockEntity extends BlockEntity implements MultiblockController {

    private boolean extractorFormed = false;

    public ExtractorHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.EXTRACTOR_HEART.get(), pos, state);
    }

    // ==================== MultiblockController ====================

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.ESSENCE_EXTRACTOR;
    }

    @Override
    public boolean isFormed() {
        return extractorFormed;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        extractorFormed = true;
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.FORMED, true), 3);
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
            syncToClient();
        }
    }

    @Override
    public void onMultiblockBroken() {
        extractorFormed = false;
        if (level != null && !level.isClientSide()) {
            if (level.getBlockState(worldPosition).hasProperty(ExtractorHeartBlock.FORMED)) {
                level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.FORMED, false), 3);
            }
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
            syncToClient();
        }
    }

    // ==================== Public API ====================

    /**
     * Tente de former l'Essence Extractor.
     * @return true si la formation a réussi
     */
    public boolean tryFormExtractor() {
        if (level == null || level.isClientSide()) return false;

        boolean valid = MultiblockValidator.validate(getPattern(), level, worldPosition);

        if (valid) {
            onMultiblockFormed();
            return true;
        }

        return false;
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Seulement désinscrire si formé (évite double appel avec onMultiblockBroken)
        if (extractorFormed) {
            MultiblockEvents.unregisterController(worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (extractorFormed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // ==================== Sync ====================

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ExtractorFormed", extractorFormed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        extractorFormed = tag.getBoolean("ExtractorFormed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
