/**
 * ============================================================
 * [MultiblockValidator.java]
 * Description: Validation générique de patterns multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockPattern   | Définition pattern   | Validation            |
 * | BlockMatcher        | Predicates           | Vérification blocs    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Tout BlockEntity contrôleur de multibloc
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Valide un pattern de multibloc dans le monde.
 */
public class MultiblockValidator {

    /**
     * Valide un pattern à partir de la position du contrôleur.
     *
     * @param pattern Le pattern à valider
     * @param level Le monde
     * @param controllerPos Position du bloc contrôleur (origine du pattern)
     * @return true si le pattern est valide
     */
    public static boolean validate(MultiblockPattern pattern, Level level, BlockPos controllerPos) {
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            BlockPos checkPos = controllerPos.offset(element.offset());
            if (!element.matcher().matches(level, checkPos)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retourne les positions absolues de tous les blocs du multibloc.
     * Utile pour détecter si un bloc cassé fait partie du multibloc.
     *
     * @param pattern Le pattern
     * @param controllerPos Position du contrôleur
     * @return Liste des positions absolues (hors air)
     */
    public static List<BlockPos> getAbsolutePositions(MultiblockPattern pattern, BlockPos controllerPos) {
        List<BlockPos> positions = new ArrayList<>();
        for (Vec3i offset : pattern.getStructurePositions()) {
            positions.add(controllerPos.offset(offset));
        }
        return positions;
    }

    /**
     * Vérifie si une position fait partie d'un multibloc formé.
     *
     * @param pattern Le pattern
     * @param controllerPos Position du contrôleur
     * @param checkPos Position à vérifier
     * @return true si la position fait partie du multibloc
     */
    public static boolean isPartOfMultiblock(MultiblockPattern pattern, BlockPos controllerPos, BlockPos checkPos) {
        for (Vec3i offset : pattern.getStructurePositions()) {
            if (controllerPos.offset(offset).equals(checkPos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Résultat de validation avec détails.
     */
    public record ValidationResult(boolean valid, BlockPos failedAt, String reason) {
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(BlockPos pos, String reason) {
            return new ValidationResult(false, pos, reason);
        }
    }

    /**
     * Valide avec détails sur l'échec.
     */
    public static ValidationResult validateDetailed(MultiblockPattern pattern, Level level, BlockPos controllerPos) {
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            BlockPos checkPos = controllerPos.offset(element.offset());
            if (!element.matcher().matches(level, checkPos)) {
                return ValidationResult.failure(checkPos, "Block mismatch at " + element.offset());
            }
        }
        return ValidationResult.success();
    }
}
