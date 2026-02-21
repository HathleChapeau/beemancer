/**
 * ============================================================
 * [HoneyedStoneBrickSlabBlock.java]
 * Description: Slab en brique de pierre miellee decoratif
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SlabBlock           | Base Minecraft       | Slab standard                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.altar;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Slab en brique de pierre miellee. Bloc decoratif.
 */
public class HoneyedStoneBrickSlabBlock extends SlabBlock {

    public HoneyedStoneBrickSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
