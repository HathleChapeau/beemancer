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
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern alembic)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.building;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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

    // Alembic: tube central + tubes horizontaux
    private static final VoxelShape SHAPE_ALEMBIC = Shapes.or(
        Block.box(3, 11, 5, 13, 17, 11),
        Block.box(0, 13, 6, 16, 16, 10)
    );

    public HoneyedGlassBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return SHAPE_ALEMBIC;
        }
        return super.getShape(state, level, pos, context);
    }
}
