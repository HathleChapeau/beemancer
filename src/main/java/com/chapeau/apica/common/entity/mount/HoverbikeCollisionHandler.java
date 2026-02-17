/**
 * ============================================================
 * [HoverbikeCollisionHandler.java]
 * Description: Resolution des collisions entite-entite pour le Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeCollisionGeometry | Probes AABB   | Detection des entites          |
 * | HoverbikeSettings   | Push force config    | Force de poussee               |
 * | HoverbikePhysics    | Epsilon              | Seuil de collision             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEntity.java: Instance dans l'entite, appels tick() et resolve()
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere les collisions entite-entite pour un Hoverbike.
 * Push uniquement, pas de degats. Cooldown par entite pour eviter le spam.
 * Detection sandwich : ne pousse pas si l'entite ciblee est coincee.
 */
public class HoverbikeCollisionHandler {

    /** Cooldown en ticks avant de repousser la meme entite. */
    private static final int HIT_COOLDOWN = 5;

    /** Nombre de ticks apres demontage pendant lesquels l'ancien rider n'est pas pousse. */
    private static final int DISMOUNT_GRACE_TICKS = 3;

    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();
    private UUID lastRiderUUID = null;
    private int dismountGraceCounter = 0;

    /**
     * Decremente tous les cooldowns et le compteur anti-push demontage.
     * Appeler chaque tick serveur.
     */
    public void tick() {
        Iterator<Map.Entry<UUID, Integer>> it = hitCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        if (dismountGraceCounter > 0) {
            dismountGraceCounter--;
            if (dismountGraceCounter <= 0) {
                lastRiderUUID = null;
            }
        }
    }

    /**
     * Enregistre le rider qui vient de descendre pour eviter de le pousser.
     */
    public void onPassengerRemoved(UUID riderUUID) {
        this.lastRiderUUID = riderUUID;
        this.dismountGraceCounter = DISMOUNT_GRACE_TICKS;
    }

    /**
     * Resout les collisions entite pour les probes donnees.
     * Pousse les entites touchees, ne fait PAS de degats.
     *
     * @param bike    L'entite hoverbike source
     * @param probes  Les AABB probes (retournees par HoverbikeCollisionGeometry)
     * @param level   Le monde
     * @param settings Constantes physiques (pour la force de push)
     */
    public void resolveEntityCollisions(HoverbikeEntity bike, AABB[] probes, Level level, HoverbikeSettings settings) {
        double pushForce = settings.collisionPushForce();

        for (AABB probe : probes) {
            List<Entity> entities = level.getEntities(bike, probe);
            for (Entity target : entities) {
                if (!canHit(bike, target)) {
                    continue;
                }

                // Direction de poussee : du centre du bike vers l'entite ciblee
                Vec3 pushDir = target.position().subtract(bike.position());
                double dist = pushDir.horizontalDistance();
                if (dist < HoverbikePhysics.COLLISION_EPSILON) {
                    continue;
                }
                pushDir = pushDir.normalize().scale(pushForce);

                // Detection sandwich : ne pas pousser si l'entite est coincee
                AABB targetBB = target.getBoundingBox();
                AABB movedBB = targetBB.move(pushDir.x, 0, pushDir.z);
                if (!level.noCollision(target, movedBB)) {
                    continue;
                }

                // Appliquer le push
                target.push(pushDir.x, pushDir.y * 0.5, pushDir.z);
                target.hurtMarked = true;

                // Mettre en cooldown
                hitCooldowns.put(target.getUUID(), HIT_COOLDOWN);
            }
        }
    }

    /**
     * Filtre d'exclusion : tout est inclus par defaut sauf les cas explicites.
     */
    private boolean canHit(HoverbikeEntity bike, Entity target) {
        // Exclure les spectateurs
        if (target.isSpectator()) {
            return false;
        }

        // Exclure le passager du bike
        if (bike.getPassengers().contains(target)) {
            return false;
        }

        // Exclure les projectiles
        if (target instanceof Projectile) {
            return false;
        }

        // Exclure les items
        if (target instanceof ItemEntity) {
            return false;
        }

        // Exclure les entites sans physique
        if (target.noPhysics) {
            return false;
        }

        // Exclure le dernier rider pendant la grace period
        if (lastRiderUUID != null && dismountGraceCounter > 0
                && target.getUUID().equals(lastRiderUUID)) {
            return false;
        }

        // Exclure les entites en cooldown
        if (hitCooldowns.containsKey(target.getUUID())) {
            return false;
        }

        return true;
    }
}
