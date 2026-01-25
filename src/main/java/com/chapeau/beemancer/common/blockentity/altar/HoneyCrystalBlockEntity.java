/**
 * ============================================================
 * [HoneyCrystalBlockEntity.java]
 * Description: BlockEntity du cristal de miel - Contrôleur Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockController| Interface contrôleur | Implémentation        |
 * | MultiblockPatterns  | Définition pattern   | HONEY_ALTAR           |
 * | MultiblockValidator | Validation           | tryFormAltar()        |
 * | MultiblockEvents    | Enregistrement       | Détection destruction |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyCrystalBlock.java (création BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.common.block.altar.HoneyCrystalBlock;
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
 * BlockEntity du cristal de miel.
 * Implémente MultiblockController pour gérer le Honey Altar.
 */
public class HoneyCrystalBlockEntity extends BlockEntity implements MultiblockController {

    private boolean altarFormed = false;

    public HoneyCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HONEY_CRYSTAL.get(), pos, state);
    }

    // ==================== MultiblockController ====================

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.HONEY_ALTAR;
    }

    @Override
    public boolean isFormed() {
        return altarFormed;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        altarFormed = true;
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(HoneyCrystalBlock.FORMED, true), 3);
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        altarFormed = false;
        if (level != null && !level.isClientSide()) {
            // Vérifier que le bloc existe encore avant de modifier son état
            if (level.getBlockState(worldPosition).hasProperty(HoneyCrystalBlock.FORMED)) {
                level.setBlock(worldPosition, getBlockState().setValue(HoneyCrystalBlock.FORMED, false), 3);
            }
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    // ==================== Public API ====================

    /**
     * Tente de former le Honey Altar.
     * @return true si la formation a réussi
     */
    public boolean tryFormAltar() {
        if (level == null || level.isClientSide()) return false;

        // Vérifier le pattern via le système modulaire
        boolean valid = MultiblockValidator.validate(getPattern(), level, worldPosition);

        if (valid) {
            onMultiblockFormed();
            return true;
        }

        return false;
    }

    /**
     * Vérifie si l'altar est actuellement formé.
     * @deprecated Utiliser isFormed() à la place
     */
    @Deprecated
    public boolean isAltarFormed() {
        return isFormed();
    }

    /**
     * Appelé quand l'altar est cassé.
     * @deprecated Utiliser onMultiblockBroken() à la place
     */
    @Deprecated
    public void onAltarBroken() {
        onMultiblockBroken();
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Se désinscrire quand le bloc est enlevé
        MultiblockEvents.unregisterController(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Si déjà formé au chargement, se réinscrire
        if (altarFormed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("AltarFormed", altarFormed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        altarFormed = tag.getBoolean("AltarFormed");
    }

    // ==================== Sync ====================

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
