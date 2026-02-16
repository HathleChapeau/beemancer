/**
 * ============================================================
 * [CrankBlockEntity.java]
 * Description: BlockEntity minimal pour le Crank (support BER)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities        | Registration         | Type du BlockEntity            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrankBlock (creation)
 * - CrankRenderer (rendu rotation)
 * - ClientSetup (enregistrement BER)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrankBlockEntity extends BlockEntity {

    public CrankBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.CRANK.get(), pos, state);
    }
}
