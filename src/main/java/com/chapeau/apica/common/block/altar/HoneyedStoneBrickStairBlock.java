/**
 * ============================================================
 * [HoneyedStoneBrickStairBlock.java]
 * Description: Escalier en brique de pierre miellee, bloc decoratif
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | StairBlock          | Base escalier        | Comportement standard |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.altar;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Escalier en brique de pierre miellee.
 * Bloc decoratif standard sans propriete multibloc.
 */
public class HoneyedStoneBrickStairBlock extends StairBlock {

    public HoneyedStoneBrickStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
