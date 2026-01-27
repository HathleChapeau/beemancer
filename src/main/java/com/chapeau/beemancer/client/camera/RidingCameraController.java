/**
 * ============================================================
 * [RidingCameraController.java]
 * Description: Contrôleur de caméra pour les modes WALK et RUN
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RideableBeeEntity       | Entité montée        | Mode et orientation            |
 * | RidingMode              | Mode actuel          | Comportement caméra            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event handler
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.camera;

import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.chapeau.beemancer.common.entity.mount.RidingMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Contrôle la caméra quand le joueur monte une RideableBeeEntity.
 *
 * Mode WALK: Caméra 3ème personne libre (joueur peut regarder librement)
 * Mode RUN: Caméra fixée derrière l'abeille, regarde vers l'avant
 */
@OnlyIn(Dist.CLIENT)
public class RidingCameraController {

    // État de la caméra sauvegardé avant montage
    private static CameraType savedCameraType = null;
    private static boolean wasRiding = false;

    // Paramètres caméra mode RUN
    private static final float SHOULDER_PITCH = 15f; // Légère plongée vers l'avant

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        boolean isRiding = player.getVehicle() instanceof RideableBeeEntity;

        // Détection montage
        if (isRiding && !wasRiding) {
            onMountBee(mc);
        }

        // Détection démontage
        if (!isRiding && wasRiding) {
            onDismountBee(mc);
        }

        wasRiding = isRiding;

        // En mode RUN, forcer la caméra en 3ème personne
        if (isRiding) {
            RideableBeeEntity bee = (RideableBeeEntity) player.getVehicle();
            if (bee.getRidingMode() == RidingMode.RUN) {
                if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
            }
        }
    }

    /**
     * Appelé quand le joueur monte l'abeille.
     */
    private static void onMountBee(Minecraft mc) {
        // Sauvegarder le type de caméra actuel
        savedCameraType = mc.options.getCameraType();

        // Forcer la caméra en 3ème personne
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /**
     * Appelé quand le joueur descend de l'abeille.
     */
    private static void onDismountBee(Minecraft mc) {
        // Restaurer le type de caméra sauvegardé
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;
        if (!(player.getVehicle() instanceof RideableBeeEntity bee)) return;

        // Mode RUN: verrouiller la caméra sur la direction de l'abeille
        if (bee.getRidingMode() == RidingMode.RUN) {
            // Forcer le yaw à suivre l'abeille
            float beeYaw = bee.getYRot();
            event.setYaw(beeYaw);

            // Légère plongée vers l'avant
            event.setPitch(SHOULDER_PITCH);
        }
        // Mode WALK: ne rien faire, laisser le joueur regarder librement
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;
        if (!(player.getVehicle() instanceof RideableBeeEntity bee)) return;

        // En mode RUN avec vitesse élevée, augmenter légèrement le FOV
        if (bee.getRidingMode() == RidingMode.RUN) {
            float speed = bee.getSyncedSpeed();
            float maxSpeed = bee.getSettings().maxRunSpeed();
            float speedRatio = Math.min(speed / maxSpeed, 1.0f);

            // Augmenter le FOV jusqu'à +15° à vitesse max
            double baseFov = event.getFOV();
            double fovBoost = speedRatio * 15.0;
            event.setFOV(baseFov + fovBoost);
        }
    }
}
