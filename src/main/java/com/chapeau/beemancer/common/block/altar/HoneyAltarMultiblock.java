/**
 * ============================================================
 * [HoneyAltarMultiblock.java]
 * Description: DEPRECATED - Utiliser core/multiblock/ à la place
 * ============================================================
 *
 * @deprecated Cette classe est conservée pour compatibilité.
 * Utiliser {@link com.chapeau.beemancer.core.multiblock.MultiblockPatterns#HONEY_ALTAR}
 * et {@link com.chapeau.beemancer.core.multiblock.MultiblockValidator} à la place.
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * @deprecated Utiliser le système modulaire dans core/multiblock/
 */
@Deprecated
public class HoneyAltarMultiblock {

    /**
     * @deprecated Utiliser MultiblockValidator.validate(MultiblockPatterns.HONEY_ALTAR, level, crystalPos)
     */
    @Deprecated
    public static boolean validatePattern(Level level, BlockPos crystalPos) {
        return MultiblockValidator.validate(MultiblockPatterns.HONEY_ALTAR, level, crystalPos);
    }
}
