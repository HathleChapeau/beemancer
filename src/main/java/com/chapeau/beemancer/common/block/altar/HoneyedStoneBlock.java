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

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Bloc décoratif de pierre miellée.
 * Utilisé dans la structure du Honey Altar.
 * LAYER distingue la position dans le multibloc:
 * - 0: non formé (défaut)
 * - 1: Y+1 (base avec colonne)
 * - 2: Y+2 (gros cube central)
 */
public class HoneyedStoneBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, 2);

    public HoneyedStoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FORMED, false)
            .setValue(LAYER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, LAYER);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
