/**
 * ============================================================
 * [HoneyedStoneStairBlock.java]
 * Description: Escalier en pierre miellée pour le Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | StairBlock          | Base escalier        | Comportement standard |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyAltarMultiblock.java (validation pattern avec orientation)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Escalier en pierre miellée.
 * L'orientation est importante pour la validation du Honey Altar.
 */
public class HoneyedStoneStairBlock extends StairBlock {

    public HoneyedStoneStairBlock(Supplier<BlockState> baseState, Properties properties) {
        super(baseState, properties);
    }
}
