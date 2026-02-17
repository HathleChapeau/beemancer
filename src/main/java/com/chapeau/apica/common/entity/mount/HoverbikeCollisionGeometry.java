/**
 * ============================================================
 * [HoverbikeCollisionGeometry.java]
 * Description: Calcul des probes de collision directionnelles du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Mth                 | Trigonometrie         | sin/cos pour rotation yaw      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEntity.java: Appel dans travel() pour detection entites
 * - HoverbikeCollisionHandler.java: Recoit les probes pour resolution
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Geometrie de collision stateless pour le Hoverbike.
 * 3 probes mini-AABB (nez, corps, queue) transformees par le yaw de l'entite.
 * Complement aux collisions vanilla Entity.move() pour la detection d'entites.
 */
public final class HoverbikeCollisionGeometry {

    private HoverbikeCollisionGeometry() {}

    /** Offsets locaux des 3 probes (Z = avant, X = cote). */
    private static final Vec3 PROBE_NOSE = new Vec3(0, 0, 0.8);
    private static final Vec3 PROBE_TAIL = new Vec3(0, 0, -0.7);
    private static final Vec3 PROBE_BODY = Vec3.ZERO;

    /** Demi-taille de chaque probe (0.3 x 0.5 x 0.3 blocs). */
    private static final double HALF_W = 0.15;
    private static final double HALF_H = 0.25;
    private static final double HALF_D = 0.15;

    /** Micro-inflation pour eviter les faux negatifs aux bords. */
    private static final double INFLATE = 0.1;

    /**
     * Calcule les 3 AABB probes en coordonnees monde.
     *
     * @param center Position centrale de l'entite (monde)
     * @param yaw    Rotation yaw de l'entite en degres
     * @return Tableau de 3 AABB : [nez, corps, queue]
     */
    public static AABB[] calculateWorldBoxes(Vec3 center, float yaw) {
        return new AABB[]{
                createProbe(center, PROBE_NOSE, yaw),
                createProbe(center, PROBE_BODY, yaw),
                createProbe(center, PROBE_TAIL, yaw)
        };
    }

    /**
     * Cree une AABB probe en transformant un offset local par le yaw.
     */
    private static AABB createProbe(Vec3 center, Vec3 localOffset, float yaw) {
        Vec3 worldOffset = localToWorld(localOffset, yaw);
        double cx = center.x + worldOffset.x;
        double cy = center.y + worldOffset.y;
        double cz = center.z + worldOffset.z;
        return new AABB(
                cx - HALF_W - INFLATE, cy - HALF_H, cz - HALF_D - INFLATE,
                cx + HALF_W + INFLATE, cy + HALF_H, cz + HALF_D + INFLATE
        );
    }

    /**
     * Convertit un offset local en offset monde via rotation yaw.
     * Meme convention que HoverbikePhysics.localToWorld.
     */
    private static Vec3 localToWorld(Vec3 local, float yaw) {
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);
        return new Vec3(
                local.x * cos - local.z * sin,
                local.y,
                local.z * cos + local.x * sin
        );
    }
}
