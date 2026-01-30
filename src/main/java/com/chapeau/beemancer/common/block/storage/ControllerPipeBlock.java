/**
 * ============================================================
 * [ControllerPipeBlock.java]
 * Description: Bloc conduit structurel pour le multibloc Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                 | Raison                | Utilisation                    |
 * |----------------------------|----------------------|--------------------------------|
 * | BaseEntityBlock            | Bloc avec BlockEntity | Support BER pour formed        |
 * | ControllerPipeBlockEntity  | Données formed        | Rotation + spread              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern storage_controller)
 * - StorageControllerBlockEntity.java (setFormedOnStructureBlocks)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.ControllerPipeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Conduit structurel du multibloc Storage Controller.
 * Cube simple quand non formé, coude orienté quand formé (rendu par BER).
 * Aucune logique de transport de fluide — bloc purement structurel.
 */
public class ControllerPipeBlock extends BaseEntityBlock {
    public static final MapCodec<ControllerPipeBlock> CODEC = simpleCodec(ControllerPipeBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 7);

    // Collision box: pipe core (4,4,4)→(12,12,12) matches the visual model
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public ControllerPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FORMED, false)
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FORMED_ROTATION);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControllerPipeBlockEntity(pos, state);
    }
}
