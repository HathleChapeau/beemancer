/**
 * ============================================================
 * [HoneyedStoneStairBlock.java]
 * Description: Escalier en pierre miellee pour le Honey Altar
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
 * - MultiblockPatterns.java (validation pattern avec orientation)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Escalier en pierre miellee.
 * L'orientation est importante pour la validation du Honey Altar.
 */
public class HoneyedStoneStairBlock extends StairBlock {

    public HoneyedStoneStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
