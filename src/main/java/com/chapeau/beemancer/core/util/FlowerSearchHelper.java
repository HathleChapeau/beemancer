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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utilitaire pour rechercher des blocs correspondant à un tag dans un rayon.
 */
public final class FlowerSearchHelper {
    
    private FlowerSearchHelper() {}
    
    /**
     * Recherche toutes les fleurs dans un rayon autour d'une position.
     *
     * @param level Le monde
     * @param center Position centrale de recherche
     * @param radius Rayon de recherche
     * @param flowerTag Tag des blocs à rechercher
     * @return Liste des positions de fleurs trouvées (triée par distance)
     */
    public static List<BlockPos> findAllFlowers(Level level, BlockPos center, int radius, TagKey<Block> flowerTag) {
        List<BlockPos> flowers = new ArrayList<>();
        
        if (flowerTag == null || level == null) {
            return flowers;
        }
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    
                    if (blockState.is(flowerTag)) {
                        flowers.add(checkPos.immutable());
                    }
                }
            }
        }
        
        // Trier par distance au centre
        flowers.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        
        return flowers;
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
        
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    
                    if (blockState.is(flowerTag)) {
                        double dist = checkPos.distSqr(center);
                        if (dist < closestDist) {
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
