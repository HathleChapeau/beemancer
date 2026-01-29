/**
 * ============================================================
 * [MultiblockController.java]
 * Description: Interface pour les blocs contrôleurs de multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockPattern   | Pattern à valider    | Définition            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyCrystalBlockEntity
 * - Futurs contrôleurs de multiblocs
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;

/**
 * Interface implémentée par les BlockEntities qui contrôlent un multibloc.
 */
public interface MultiblockController {

    /**
     * @return Le pattern de ce multibloc
     */
    MultiblockPattern getPattern();

    /**
     * @return true si le multibloc est actuellement formé
     */
    boolean isFormed();

    /**
     * @return La position du contrôleur dans le monde
     */
    BlockPos getControllerPos();

    /**
     * Appelé quand le multibloc est formé avec succès.
     */
    void onMultiblockFormed();

    /**
     * Appelé quand le multibloc est détruit (bloc cassé).
     */
    void onMultiblockBroken();

    /**
     * @return La rotation horizontale du multibloc (0=0°, 1=90°, 2=180°, 3=270°).
     *         Par défaut 0 (pas de rotation).
     */
    default int getRotation() {
        return 0;
    }
}
