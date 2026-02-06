/**
 * ============================================================
 * [HoneyedStoneBlock.java]
 * Description: Bloc de pierre miellée pour le Honey Altar
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
 * - HoneyAltarMultiblock.java (validation pattern)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;

/**
 * Bloc décoratif de pierre miellée.
 * Participe à plusieurs multiblocs: Altar, Extractor, Infuser, Centrifuge, Storage.
 */
public class HoneyedStoneBlock extends Block {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK =
        MultiblockProperty.create("altar", "extractor", "infuser", "centrifuge", "storage");

    // VoxelShape réduite pour le multibloc Centrifuge (12x12x12 centré)
    private static final VoxelShape SHAPE_CENTRIFUGE = Block.box(2, 2, 2, 14, 14, 14);

    public HoneyedStoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockProperty mb = state.getValue(MULTIBLOCK);
        if (mb == MultiblockProperty.CENTRIFUGE || mb == MultiblockProperty.INFUSER) {
            return SHAPE_CENTRIFUGE;
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockProperty mb = state.getValue(MULTIBLOCK);
        if (mb == MultiblockProperty.CENTRIFUGE || mb == MultiblockProperty.INFUSER) {
            return SHAPE_CENTRIFUGE;
        }
        return Shapes.block();
    }
}
