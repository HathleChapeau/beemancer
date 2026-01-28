/**
 * ============================================================
 * [FlowerSearchHelper.java]
 * Description: Utilitaire pour rechercher des fleurs dans un rayon
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FlowerGene          | Gène fleur           | Tag des blocs cibles           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity.java: Detection fleurs pour abeilles
 * - ForagingBehaviorGoal.java: Recherche de fleurs
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire pour rechercher des blocs correspondant à un tag dans un rayon.
 */
public final class FlowerSearchHelper {
    
    private FlowerSearchHelper() {}
    
    /**
     * Recherche toutes les fleurs dans un rayon autour d'une position.
     * Effectue un raycast pour ignorer les fleurs bloquées par des blocs.
     *
     * @param level Le monde
     * @param center Position centrale de recherche
     * @param radius Rayon de recherche
     * @param flowerTag Tag des blocs à rechercher
     * @return Liste des positions de fleurs trouvées (avec ligne de vue dégagée)
     */
    public static List<BlockPos> findAllFlowers(Level level, BlockPos center, int radius, TagKey<Block> flowerTag) {
        List<BlockPos> flowers = new ArrayList<>();

        if (flowerTag == null || level == null) {
            return flowers;
        }

        Vec3 centerVec = Vec3.atCenterOf(center);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);

                    if (blockState.is(flowerTag)) {
                        // Raycast pour vérifier la ligne de vue
                        if (hasLineOfSight(level, centerVec, checkPos)) {
                            flowers.add(checkPos.immutable());
                        }
                    }
                }
            }
        }

        return flowers;
    }

    /**
     * Vérifie si la ligne de vue entre le centre et la position cible est dégagée.
     *
     * @param level Le monde
     * @param from Position de départ (centre de la ruche)
     * @param to Position cible (fleur)
     * @return true si aucun bloc ne bloque la vue
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, BlockPos to) {
        Vec3 toVec = Vec3.atCenterOf(to);

        BlockHitResult result = level.clip(new ClipContext(
            from,
            toVec,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));

        // Si on n'a rien touché ou si on a touché la fleur elle-même, la vue est dégagée
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Si on a touché un bloc, vérifier si c'est la position de la fleur
        return result.getBlockPos().equals(to);
    }
    
    /**
     * Recherche la fleur la plus proche dans un rayon.
     *
     * @param level Le monde
     * @param center Position centrale de recherche
     * @param radius Rayon de recherche
     * @param flowerTag Tag des blocs à rechercher
     * @return Position de la fleur la plus proche ou null si aucune trouvée
     */
    public static BlockPos findClosestFlower(Level level, BlockPos center, int radius, TagKey<Block> flowerTag) {
        if (flowerTag == null || level == null) {
            return null;
        }

        Vec3 centerVec = Vec3.atCenterOf(center);
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);

                    if (blockState.is(flowerTag)) {
                        double dist = checkPos.distSqr(center);
                        if (dist < closestDist && hasLineOfSight(level, centerVec, checkPos)) {
                            closestDist = dist;
                            closest = checkPos.immutable();
                        }
                    }
                }
            }
        }

        return closest;
    }
    
    /**
     * Vérifie si une position contient une fleur valide.
     *
     * @param level Le monde
     * @param pos Position à vérifier
     * @param flowerTag Tag des blocs valides
     * @return true si la position contient une fleur valide
     */
    public static boolean isValidFlower(Level level, BlockPos pos, TagKey<Block> flowerTag) {
        if (flowerTag == null || level == null || pos == null) {
            return false;
        }
        return level.getBlockState(pos).is(flowerTag);
    }
}
