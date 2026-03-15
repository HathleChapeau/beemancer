/**
 * ============================================================
 * [HoneyedGlassBlock.java]
 * Description: Bloc de verre mielle avec propriete multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | TransparentBlock    | Base verre           | Rendu transparent              |
 * | MultiblockProperty  | Etat multibloc       | Blockstate multiblock          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern alembic)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.building;

import com.chapeau.apica.core.multiblock.MultiblockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc de verre mielle.
 * Participe au multibloc Alembic.
 */
public class HoneyedGlassBlock extends TransparentBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("alembic");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Alembic: tube central + tubes horizontaux (orientés E/W pour FACING=WEST)
    private static final VoxelShape SHAPE_ALEMBIC_EW = Shapes.or(
        Block.box(3, 11, 5, 13, 17, 11),
        Block.box(0, 13, 6, 16, 16, 10)   // Tubes sur axe X
    );
    // Alembic: tubes orientés N/S pour FACING=NORTH
    private static final VoxelShape SHAPE_ALEMBIC_NS = Shapes.or(
        Block.box(5, 11, 3, 11, 17, 13),
        Block.box(6, 13, 0, 10, 16, 16)   // Tubes sur axe Z
    );

    public HoneyedGlassBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            Direction facing = state.getValue(FACING);
            return (facing == Direction.NORTH || facing == Direction.SOUTH)
                ? SHAPE_ALEMBIC_NS
                : SHAPE_ALEMBIC_EW;
        }
        return super.getShape(state, level, pos, context);
    }
}
