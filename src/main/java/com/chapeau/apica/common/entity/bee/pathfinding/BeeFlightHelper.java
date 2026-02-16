/**
 * ============================================================
 * [BeeFlightHelper.java]
 * Description: Utilitaires de vol — separation boids entre abeilles
 * ============================================================
 *
 * Implémente la separation boids (Reynolds 1987) pour eviter que
 * les abeilles ne se superposent en vol.
 * Complexite: O(nearby_bees) par appel via AABB query.
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entite abeille       | Detection des voisines         |
 * | Level               | Monde Minecraft      | AABB entity query              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ForagingBehaviorGoal.java: Applique la separation au mouvement
 * - WildBeePatrolGoal.java: Applique la separation au mouvement
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.pathfinding;

import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Helper de vol pour abeilles.
 * Fournit un vecteur de separation boids pour eviter les collisions entre abeilles.
 */
public class BeeFlightHelper {

    private static final double SEPARATION_RADIUS = 1.5;
    private static final double SEPARATION_WEIGHT = 0.12;

    /**
     * Calcule un vecteur de separation boids pour l'abeille donnee.
     * Repousse l'abeille de ses voisines proportionnellement a l'inverse de la distance.
     *
     * @return vecteur de separation a ajouter au mouvement, ou Vec3.ZERO si aucune voisine
     */
    public static Vec3 computeSeparation(MagicBeeEntity bee) {
        Vec3 beePos = bee.position();
        AABB searchBox = bee.getBoundingBox().inflate(SEPARATION_RADIUS);

        List<MagicBeeEntity> neighbors = bee.level().getEntitiesOfClass(
                MagicBeeEntity.class, searchBox, other -> other != bee);

        if (neighbors.isEmpty()) {
            return Vec3.ZERO;
        }

        double repelX = 0;
        double repelY = 0;
        double repelZ = 0;

        for (MagicBeeEntity neighbor : neighbors) {
            Vec3 neighborPos = neighbor.position();
            double dx = beePos.x - neighborPos.x;
            double dy = beePos.y - neighborPos.y;
            double dz = beePos.z - neighborPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < 0.001) {
                // Presque superposees: repulsion aleatoire
                repelX += (bee.getRandom().nextFloat() - 0.5) * 0.1;
                repelY += (bee.getRandom().nextFloat() - 0.5) * 0.1;
                repelZ += (bee.getRandom().nextFloat() - 0.5) * 0.1;
            } else {
                double dist = Math.sqrt(distSq);
                double strength = 1.0 / dist;
                repelX += (dx / dist) * strength;
                repelY += (dy / dist) * strength;
                repelZ += (dz / dist) * strength;
            }
        }

        return new Vec3(
                repelX * SEPARATION_WEIGHT,
                repelY * SEPARATION_WEIGHT,
                repelZ * SEPARATION_WEIGHT
        );
    }
}
