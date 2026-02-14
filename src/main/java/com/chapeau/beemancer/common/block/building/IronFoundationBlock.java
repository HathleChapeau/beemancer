/**
 * ============================================================
 * [IronFoundationBlock.java]
 * Description: Bloc de fondation en fer pour les multiblocs
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockProperty  | Etat multibloc       | Blockstate            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (validation pattern)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.building;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc de fondation en fer.
 * Remplace le Honeyed Stone dans tous les multiblocs.
 * Participe aux multiblocs: Altar, Extractor, Infuser, Centrifuge, Storage.
 */
public class IronFoundationBlock extends Block {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK =
        MultiblockProperty.create("altar", "extractor", "infuser", "centrifuge", "storage", "storage_top");
    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 3);

    private static final VoxelShape SHAPE_REDUCED = Block.box(2, 2, 2, 14, 14, 14);

    public IronFoundationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, FORMED_ROTATION);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockProperty mb = state.getValue(MULTIBLOCK);
        if (mb == MultiblockProperty.CENTRIFUGE || mb == MultiblockProperty.INFUSER) {
            return SHAPE_REDUCED;
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockProperty mb = state.getValue(MULTIBLOCK);
        if (mb == MultiblockProperty.CENTRIFUGE || mb == MultiblockProperty.INFUSER) {
            return SHAPE_REDUCED;
        }
        return Shapes.block();
    }
}
