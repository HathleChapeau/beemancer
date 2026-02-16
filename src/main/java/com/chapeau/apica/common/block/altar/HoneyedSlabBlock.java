/**
 * ============================================================
 * [HoneyedSlabBlock.java]
 * Description: Slab en honeyed stone décoratif
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
 * Slab en honeyed stone. Bloc décoratif.
 */
public class HoneyedSlabBlock extends SlabBlock {

    public HoneyedSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
