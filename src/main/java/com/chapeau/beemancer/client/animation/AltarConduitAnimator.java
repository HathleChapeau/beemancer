/**
 * ============================================================
 * [AltarConduitAnimator.java]
 * Description: Gère l'animation de rotation des conduits autour de l'altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Source données       | getRotationSpeed()    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - AltarHeartRenderer.java (rendu des conduits animés)
 *
 * PATTERN:
 * - Inspiré de Create: rotation basée sur gameTime + partialTick
 * - Vitesse configurable dynamiquement via le BlockEntity
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Contrôleur d'animation pour les conduits de l'altar.
 * Calcule les positions orbitales des conduits autour du centre de l'altar.
 */
public class AltarConduitAnimator {

    // === Configuration par défaut ===
    /** Vitesse de rotation par défaut (degrés par tick) */
    public static final float DEFAULT_ROTATION_SPEED = 1.0f;

    /** Rayon de l'orbite des conduits (en blocs) */
    public static final float ORBIT_RADIUS = 1.0f;

    /** Offset Y de l'orbite par rapport au contrôleur (Y+1 = niveau des conduits) */
    public static final float ORBIT_Y_OFFSET = 1.0f;

    /** Nombre de conduits dans l'animation */
    public static final int CONDUIT_COUNT = 4;

    // === Calculs d'animation ===

    /**
     * Calcule l'angle de rotation interpolé pour l'animation.
     *
     * @param blockEntity Le BlockEntity de l'altar
     * @param partialTick L'interpolation entre ticks (0.0 - 1.0)
     * @return L'angle de rotation en degrés
     */
    public static float getRotationAngle(AltarHeartBlockEntity blockEntity, float partialTick) {
        if (blockEntity == null || !blockEntity.isFormed()) {
            return 0f;
        }
        return blockEntity.getInterpolatedRotationAngle(partialTick);
    }

    /**
     * Calcule l'angle de facing d'un conduit (pour qu'il pointe vers le centre).
     *
     * @param conduitIndex Index du conduit (0-3)
     * @param rotationAngle Angle de rotation actuel en degrés
     * @return Angle de facing en degrés (pour rotation Y du modèle)
     */
    public static float getConduitFacingAngle(int conduitIndex, float rotationAngle) {
        // Le conduit doit pointer vers le centre, donc angle opposé à sa position
        float baseAngle = conduitIndex * 90f;
        return baseAngle + rotationAngle + 180f;
    }

    /**
     * Calcule les offsets relatifs d'un conduit par rapport au centre.
     * Utile pour le rendu dans le BlockEntityRenderer.
     *
     * @param conduitIndex Index du conduit (0-3)
     * @param rotationAngle Angle de rotation actuel en degrés
     * @return Vec3 avec les offsets X, Y, Z
     */
    public static Vec3 getConduitOffset(int conduitIndex, float rotationAngle) {
        float baseAngle = conduitIndex * 90f;
        float totalAngle = baseAngle + rotationAngle;

        double angleRad = Math.toRadians(totalAngle);

        return new Vec3(
            Math.sin(angleRad) * ORBIT_RADIUS,
            ORBIT_Y_OFFSET,
            Math.cos(angleRad) * ORBIT_RADIUS
        );
    }

    /**
     * Convertit une vitesse en RPM vers degrés par tick.
     *
     * @param rpm Rotations par minute
     * @return Degrés par tick
     */
    public static float rpmToDegreesPerTick(float rpm) {
        // 1 RPM = 360° par minute = 360° / (60 sec * 20 ticks) = 0.3° par tick
        return rpm * 360f / (60f * 20f);
    }

    /**
     * Convertit des degrés par tick vers RPM.
     *
     * @param degreesPerTick Degrés par tick
     * @return Rotations par minute
     */
    public static float degreesPerTickToRpm(float degreesPerTick) {
        return degreesPerTick * 60f * 20f / 360f;
    }
}
