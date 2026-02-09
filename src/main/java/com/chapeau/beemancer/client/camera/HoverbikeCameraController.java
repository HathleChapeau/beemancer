/**
 * ============================================================
 * [HoverbikeCameraController.java]
 * Description: Controleur de camera pour le Hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite montee        | Mode et vitesse                |
 * | HoverbikeMode       | Mode actuel          | Comportement camera            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement event handler
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.camera;

import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.chapeau.beemancer.common.entity.mount.HoverbikeMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Controle la camera quand le joueur monte un Hoverbike.
 * - Monte : force 3eme personne
 * - Descend : restaure la camera precedente
 * - Mode RUN : FOV boost proportionnel a la vitesse
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeCameraController {

    private static CameraType savedCameraType = null;
    private static boolean wasRiding = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean isRiding = player.getVehicle() instanceof HoverbikeEntity;

        // Detection montage
        if (isRiding && !wasRiding) {
            savedCameraType = mc.options.getCameraType();
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }

        // Detection demontage
        if (!isRiding && wasRiding) {
            if (savedCameraType != null) {
                mc.options.setCameraType(savedCameraType);
                savedCameraType = null;
            }
        }

        wasRiding = isRiding;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof HoverbikeEntity hoverbike)) return;

        // En mode RUN, FOV boost proportionnel a la vitesse
        if (hoverbike.getMode() == HoverbikeMode.RUN) {
            double speed = hoverbike.getForwardSpeed();
            double maxSpeed = hoverbike.getSettings().maxRunSpeed();
            double speedRatio = Math.min(speed / maxSpeed, 1.0);

            // Augmenter le FOV jusqu'a +15 degres a vitesse max
            double baseFov = event.getFOV();
            double fovBoost = speedRatio * 15.0;
            event.setFOV(baseFov + fovBoost);
        }
    }
}
