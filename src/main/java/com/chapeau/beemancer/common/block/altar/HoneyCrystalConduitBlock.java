/**
 * ============================================================
 * [HoneyCrystalConduitBlock.java]
 * Description: Conduit de cristal pour le Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | Block               | Base Minecraft       | Bloc simple           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyAltarMultiblock.java (validation pattern - étage 4)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Conduit de cristal de miel.
 * Transmet l'énergie magique dans le Honey Altar.
 * Change d'apparence quand le multibloc est formé.
 */
public class HoneyCrystalConduitBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Forme: pilier vertical fin
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public HoneyCrystalConduitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FORMED, false)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Quand formé, le rendu normal est désactivé - l'animation orbitale est gérée par AltarHeartRenderer
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
}
