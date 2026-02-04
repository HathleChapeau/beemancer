/**
 * ============================================================
 * [ControlledHiveBlock.java]
 * Description: Bloc ruche contrôlée pour le multibloc Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Base Minecraft       | Bloc simple                    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern storage_controller)
 * - StorageControllerBlockEntity.java (spawn/retour abeilles)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bloc ruche passive pour le multibloc Storage Controller.
 * Placé en haut de la structure (0, +1, 0), sert de point
 * d'entrée/sortie pour les delivery bees.
 * Aucun comportement propre — le controller gère tout.
 */
public class ControlledHiveBlock extends Block {

    public ControlledHiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
