/**
 * ============================================================
 * [HiveFlowerPool.java]
 * Description: Gestion du pool partagé de fleurs pour une ruche
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FlowerSearchHelper  | Recherche fleurs     | Scan des fleurs environnantes  |
 * | FlowerGene          | Gène fleur           | Tag des fleurs valides         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity.java: Gestion des fleurs pour les abeilles
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.util.FlowerSearchHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gère le pool partagé de fleurs disponibles pour une ruche.
 * Empêche les conflits en assignant les fleurs individuellement aux abeilles.
 */
public class HiveFlowerPool {

    private static final int SCAN_INTERVAL = 100; // 5 secondes

    private final List<BlockPos> availableFlowers = new ArrayList<>();
    private int scanCooldown = 0;

    // --- Scanning ---

    /**
     * Décrémente le cooldown de scan.
     * @return true si un scan doit être effectué
     */
    public boolean tickScanCooldown() {
        scanCooldown--;
        if (scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            return true;
        }
        return false;
    }

    /**
     * Scanne les fleurs autour de la ruche.
     *
     * @param level Le monde
     * @param hivePos Position de la ruche
     * @param flowerTags Tags de fleurs à rechercher
     * @param radius Rayon de recherche
     * @param assignedFlowers Fleurs déjà assignées (à exclure)
     */
    public void scanFlowers(Level level, BlockPos hivePos, Set<TagKey<Block>> flowerTags,
                            int radius, Collection<BlockPos> assignedFlowers) {
        if (level == null) return;

        Set<BlockPos> found = new HashSet<>();

        for (TagKey<Block> flowerTag : flowerTags) {
            List<BlockPos> flowers = FlowerSearchHelper.findAllFlowers(level, hivePos, radius, flowerTag);
            found.addAll(flowers);
        }

        // Exclure les fleurs déjà assignées
        found.removeAll(assignedFlowers);

        // Mettre à jour et mélanger
        availableFlowers.clear();
        availableFlowers.addAll(found);
        Collections.shuffle(availableFlowers);
    }

    // --- Assignment ---

    /**
     * Assigne une fleur aléatoire depuis le pool.
     *
     * @param level Le monde (pour validation)
     * @param flowerTag Tag de fleur valide pour cette abeille
     * @param random Source de random
     * @return La fleur assignée ou null si aucune disponible
     */
    @Nullable
    public BlockPos assignRandomFlower(Level level, @Nullable TagKey<Block> flowerTag, RandomSource random) {
        if (availableFlowers.isEmpty()) {
            return null;
        }

        // Essayer jusqu'à 10 fois de trouver une fleur valide
        for (int attempt = 0; attempt < 10 && !availableFlowers.isEmpty(); attempt++) {
            int index = random.nextInt(availableFlowers.size());
            BlockPos flower = availableFlowers.get(index);

            if (level != null && flowerTag != null) {
                if (FlowerSearchHelper.isValidFlower(level, flower, flowerTag)) {
                    availableFlowers.remove(index);
                    return flower;
                } else {
                    // Fleur invalide, la retirer du pool
                    availableFlowers.remove(index);
                }
            } else {
                // Pas de validation possible, utiliser directement
                availableFlowers.remove(index);
                return flower;
            }
        }

        return null;
    }

    /**
     * Retourne une fleur au pool (quand une abeille échoue ou meurt).
     */
    public void returnFlower(BlockPos flower) {
        if (flower != null && !availableFlowers.contains(flower)) {
            availableFlowers.add(flower);
        }
    }

    // --- Queries ---

    public boolean hasFlowers() {
        return !availableFlowers.isEmpty();
    }

    public int getFlowerCount() {
        return availableFlowers.size();
    }

    public List<BlockPos> getAvailableFlowers() {
        return Collections.unmodifiableList(availableFlowers);
    }

    public int getScanCooldown() {
        return scanCooldown;
    }

    // --- Utility ---

    public void clear() {
        availableFlowers.clear();
        scanCooldown = 0;
    }
}
