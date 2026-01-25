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

/**
 * Bloc décoratif de pierre miellée.
 * Utilisé dans la structure du Honey Altar.
 */
public class HoneyedStoneBlock extends Block {

    public HoneyedStoneBlock(Properties properties) {
        super(properties);
    }
}
