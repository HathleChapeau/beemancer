/**
 * ============================================================
 * [AltarHeartBlockEntity.java]
 * Description: BlockEntity du Coeur de l'Autel - Contrôleur Honey Altar
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
 * - AltarHeartBlock.java (création BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.altar.AltarHeartBlock;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.block.altar.HoneyPedestalBlock;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.block.altar.HoneyedStoneBlock;
import com.chapeau.beemancer.common.block.altar.HoneyedStoneStairBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity du Coeur de l'Autel.
 * Implémente MultiblockController pour gérer le Honey Altar.
 */
public class AltarHeartBlockEntity extends BlockEntity implements MultiblockController {

    private boolean altarFormed = false;

    public AltarHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALTAR_HEART.get(), pos, state);
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
            // Mettre à jour le contrôleur
            level.setBlock(worldPosition, getBlockState().setValue(AltarHeartBlock.FORMED, true), 3);

            // Mettre à jour tous les blocs du multibloc
            updateMultiblockBlocksState(true);

            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        altarFormed = false;
        if (level != null && !level.isClientSide()) {
            // Mettre à jour le contrôleur
            if (level.getBlockState(worldPosition).hasProperty(AltarHeartBlock.FORMED)) {
                level.setBlock(worldPosition, getBlockState().setValue(AltarHeartBlock.FORMED, false), 3);
            }

            // Mettre à jour tous les blocs du multibloc
            updateMultiblockBlocksState(false);

            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    /**
     * Met à jour l'état FORMED de tous les blocs du multibloc.
     */
    private void updateMultiblockBlocksState(boolean formed) {
        if (level == null) return;

        // === Pedestal à Y-2 ===
        updateBlockFormed(worldPosition.offset(0, -2, 0), HoneyPedestalBlock.FORMED, formed);

        // === 4 Stairs à Y-2 ===
        BlockPos[] stairOffsets = {
            new BlockPos(0, -2, -1),  // N
            new BlockPos(0, -2, 1),   // S
            new BlockPos(1, -2, 0),   // E
            new BlockPos(-1, -2, 0)   // W
        };
        for (BlockPos offset : stairOffsets) {
            updateBlockFormed(worldPosition.offset(offset), HoneyedStoneStairBlock.FORMED, formed);
        }

        // === 8 Conduits à Y+1 ===
        BlockPos[] conduitOffsets = {
            new BlockPos(0, 1, -1),   // N
            new BlockPos(0, 1, 1),    // S
            new BlockPos(1, 1, 0),    // E
            new BlockPos(-1, 1, 0),   // W
            new BlockPos(-1, 1, -1),  // NW
            new BlockPos(1, 1, -1),   // NE
            new BlockPos(-1, 1, 1),   // SW
            new BlockPos(1, 1, 1)     // SE
        };
        for (BlockPos offset : conduitOffsets) {
            updateBlockFormed(worldPosition.offset(offset), HoneyCrystalConduitBlock.FORMED, formed);
        }

        // === Honeyed Stone centre à Y+1 (layer 1: base avec colonne) ===
        updateHoneyedStone(worldPosition.offset(0, 1, 0), formed, 1);

        // === Honeyed Stone centre à Y+2 (layer 2: gros cube) ===
        updateHoneyedStone(worldPosition.offset(0, 2, 0), formed, 2);

        // === 4 Réservoirs à Y+2 (orientés vers le centre) ===
        updateReservoir(worldPosition.offset(0, 2, -1), formed, Direction.SOUTH);  // N pointe vers S
        updateReservoir(worldPosition.offset(0, 2, 1), formed, Direction.NORTH);   // S pointe vers N
        updateReservoir(worldPosition.offset(1, 2, 0), formed, Direction.WEST);    // E pointe vers W
        updateReservoir(worldPosition.offset(-1, 2, 0), formed, Direction.EAST);   // W pointe vers E
    }

    /**
     * Met à jour la propriété FORMED d'un bloc si elle existe.
     */
    private void updateBlockFormed(BlockPos pos, net.minecraft.world.level.block.state.properties.BooleanProperty property, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(property)) {
            level.setBlock(pos, state.setValue(property, formed), 3);
        }
    }

    /**
     * Met à jour un HoneyedStoneBlock avec FORMED et LAYER.
     */
    private void updateHoneyedStone(BlockPos pos, boolean formed, int layer) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HoneyedStoneBlock.FORMED) && state.hasProperty(HoneyedStoneBlock.LAYER)) {
            level.setBlock(pos, state
                .setValue(HoneyedStoneBlock.FORMED, formed)
                .setValue(HoneyedStoneBlock.LAYER, formed ? layer : 0), 3);
        }
    }

    /**
     * Met à jour un HoneyReservoirBlock avec FORMED et FACING.
     */
    private void updateReservoir(BlockPos pos, boolean formed, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HoneyReservoirBlock.FORMED) && state.hasProperty(HoneyReservoirBlock.FACING)) {
            level.setBlock(pos, state
                .setValue(HoneyReservoirBlock.FORMED, formed)
                .setValue(HoneyReservoirBlock.FACING, facing), 3);
        }
    }

    // ==================== Public API ====================

    /**
     * Tente de former le Honey Altar.
     * @return true si la formation a réussi
     */
    public boolean tryFormAltar() {
        if (level == null || level.isClientSide()) return false;

        // Utiliser validateDetailed pour avoir les infos d'échec
        var result = MultiblockValidator.validateDetailed(getPattern(), level, worldPosition);

        if (result.valid()) {
            onMultiblockFormed();
            return true;
        }

        // Log l'échec pour debug
        Beemancer.LOGGER.debug("Altar validation failed at {} - {}",
            result.failedAt(), result.reason());
        return false;
    }

    /**
     * Récupère les 4 réservoirs du multiblock formé.
     * @return Liste des HoneyReservoirBlockEntity (peut être vide si non formé)
     */
    public List<HoneyReservoirBlockEntity> getReservoirs() {
        List<HoneyReservoirBlockEntity> reservoirs = new ArrayList<>();
        if (!altarFormed || level == null) return reservoirs;

        // Positions des réservoirs à Y+2 relatif au contrôleur
        BlockPos[] offsets = {
            new BlockPos(0, 2, -1),  // Nord
            new BlockPos(0, 2, 1),   // Sud
            new BlockPos(1, 2, 0),   // Est
            new BlockPos(-1, 2, 0)   // Ouest
        };

        for (BlockPos offset : offsets) {
            BlockEntity be = level.getBlockEntity(worldPosition.offset(offset));
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                reservoirs.add(reservoir);
            }
        }

        return reservoirs;
    }

    /**
     * Calcule le total de fluide d'un type spécifique dans tous les réservoirs.
     * @param fluidType Le type de fluide à compter
     * @return Le montant total en mB
     */
    public int getTotalFluidAmount(Fluid fluidType) {
        int total = 0;
        for (HoneyReservoirBlockEntity reservoir : getReservoirs()) {
            if (!reservoir.getFluid().isEmpty() && reservoir.getFluid().getFluid() == fluidType) {
                total += reservoir.getFluidAmount();
            }
        }
        return total;
    }

    /**
     * Vérifie si l'altar est actuellement formé.
     */
    public boolean isAltarFormed() {
        return isFormed();
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
