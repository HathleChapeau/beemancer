/**
 * ============================================================
 * [MultiblockEvents.java]
 * Description: Gestionnaire d'événements pour les multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockController| Interface contrôleur | Détection             |
 * | MultiblockValidator | Vérification         | Positions             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement events)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Écoute les événements de destruction de blocs pour invalider les multiblocs.
 */
public class MultiblockEvents {

    // Cache des contrôleurs actifs pour une recherche rapide
    private static final Set<BlockPos> activeControllers = new HashSet<>();

    /**
     * Enregistre un contrôleur comme actif (multibloc formé).
     */
    public static void registerActiveController(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            activeControllers.add(pos);
        }
    }

    /**
     * Désenregistre un contrôleur (multibloc cassé ou désactivé).
     */
    public static void unregisterController(BlockPos pos) {
        activeControllers.remove(pos);
    }

    /**
     * Nettoie tous les contrôleurs (appelé au déchargement du monde).
     */
    public static void clearAll() {
        activeControllers.clear();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos brokenPos = event.getPos();

        // Copie pour éviter ConcurrentModification
        Set<BlockPos> controllersToCheck = new HashSet<>(activeControllers);

        for (BlockPos controllerPos : controllersToCheck) {
            // Vérifier si le contrôleur existe encore
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (!(be instanceof MultiblockController controller)) {
                activeControllers.remove(controllerPos);
                continue;
            }

            if (!controller.isFormed()) {
                continue;
            }

            // Vérifier si le bloc cassé fait partie de ce multibloc (avec rotation)
            if (MultiblockValidator.isPartOfMultiblock(
                    controller.getPattern(), controllerPos, brokenPos, controller.getRotation())) {
                // Invalider le multibloc
                controller.onMultiblockBroken();
                activeControllers.remove(controllerPos);
            }
        }
    }
}
