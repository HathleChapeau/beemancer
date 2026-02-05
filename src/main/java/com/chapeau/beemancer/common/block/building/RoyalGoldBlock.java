/**
 * ============================================================
 * [RoyalGoldBlock.java]
 * Description: Bloc d'or royal avec propriete multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Base Minecraft       | Bloc simple                    |
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc d'or royal.
 * Participe au multibloc Alembic.
 */
public class RoyalGoldBlock extends Block {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("alembic");

    public RoyalGoldBlock(Properties properties) {
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
            return Shapes.empty();
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }
}
