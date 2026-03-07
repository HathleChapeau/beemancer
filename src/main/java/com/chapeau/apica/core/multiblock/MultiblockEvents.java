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
 * - Apica.java (enregistrement events)
 * - Tous les multiblocs (register/unregister)
 *
 * ============================================================
 */
package com.chapeau.apica.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Écoute les événements de destruction de blocs pour invalider les multiblocs.
 * Les contrôleurs actifs sont indexés par dimension pour éviter les faux positifs cross-dimension.
 */
public class MultiblockEvents {

    // Cache des contrôleurs actifs indexé par dimension
    private static final Map<ResourceKey<Level>, Set<BlockPos>> activeControllers = new HashMap<>();

    /**
     * Enregistre un contrôleur comme actif (multibloc formé).
     */
    public static void registerActiveController(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            activeControllers.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos);
        }
    }

    /**
     * Désenregistre un contrôleur (multibloc cassé ou désactivé).
     * Version dimension-aware (préférée).
     */
    public static void unregisterController(Level level, BlockPos pos) {
        Set<BlockPos> controllers = activeControllers.get(level.dimension());
        if (controllers != null) {
            controllers.remove(pos);
            if (controllers.isEmpty()) {
                activeControllers.remove(level.dimension());
            }
        }
    }

    /**
     * Désenregistre un contrôleur sans dimension (rétrocompatibilité).
     * Cherche dans toutes les dimensions.
     */
    public static void unregisterController(BlockPos pos) {
        for (Set<BlockPos> controllers : activeControllers.values()) {
            controllers.remove(pos);
        }
    }

    /**
     * Retourne les positions de tous les controllers actifs dans une dimension donnée.
     */
    public static Set<BlockPos> getActiveControllers(Level level) {
        Set<BlockPos> controllers = activeControllers.get(level.dimension());
        return controllers != null ? Collections.unmodifiableSet(controllers) : Collections.emptySet();
    }

    /**
     * Retourne les positions de tous les controllers actifs (toutes dimensions confondues).
     * Utilisé par StorageEvents pour la déduplication inter-réseau [BM].
     */
    public static Set<BlockPos> getActiveControllers() {
        Set<BlockPos> all = new HashSet<>();
        for (Set<BlockPos> controllers : activeControllers.values()) {
            all.addAll(controllers);
        }
        return Collections.unmodifiableSet(all);
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

        // Ne chercher que dans la dimension du bloc cassé
        Set<BlockPos> dimensionControllers = activeControllers.get(level.dimension());
        if (dimensionControllers == null || dimensionControllers.isEmpty()) return;

        // Copie pour éviter ConcurrentModification
        Set<BlockPos> controllersToCheck = new HashSet<>(dimensionControllers);

        for (BlockPos controllerPos : controllersToCheck) {
            if (!level.isLoaded(controllerPos)) continue;
            // Vérifier si le contrôleur existe encore
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (!(be instanceof MultiblockController controller)) {
                dimensionControllers.remove(controllerPos);
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
                dimensionControllers.remove(controllerPos);
            }
        }
    }
}
