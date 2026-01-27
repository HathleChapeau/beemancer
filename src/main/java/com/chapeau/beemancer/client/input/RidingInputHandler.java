/**
 * ============================================================
 * [RidingInputHandler.java]
 * Description: Capture les inputs de déplacement et les envoie au serveur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RideableBeeEntity       | Entité montée        | Vérification du véhicule       |
 * | RidingInputPacket       | Packet réseau        | Envoi des inputs               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event handler
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.input;

import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.chapeau.beemancer.core.network.packets.RidingInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handler côté client pour capturer les inputs de déplacement.
 * Envoie un packet au serveur chaque tick quand l'input change.
 */
@OnlyIn(Dist.CLIENT)
public class RidingInputHandler {

    // Cache pour détecter les changements d'input
    private static float lastForward = 0f;
    private static float lastStrafe = 0f;
    private static boolean lastJump = false;
    private static boolean lastSprint = false;
    private static float lastCameraYaw = 0f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Vérifier que le joueur est connecté et monte une RideableBeeEntity
        if (player == null) return;
        if (mc.screen != null) return; // Pas d'input si un screen est ouvert
        if (!(player.getVehicle() instanceof RideableBeeEntity bee)) return;

        // Capturer les inputs actuels
        float forward = 0f;
        float strafe = 0f;

        if (mc.options.keyUp.isDown()) forward += 1f;
        if (mc.options.keyDown.isDown()) forward -= 1f;
        if (mc.options.keyLeft.isDown()) strafe += 1f;
        if (mc.options.keyRight.isDown()) strafe -= 1f;

        boolean jump = mc.options.keyJump.isDown();
        boolean sprint = mc.options.keySprint.isDown();
        float cameraYaw = player.getYRot();

        // Envoyer seulement si l'input a changé (optimisation)
        if (inputChanged(forward, strafe, jump, sprint, cameraYaw)) {
            RidingInputPacket packet = new RidingInputPacket(
                bee.getId(),
                forward,
                strafe,
                jump,
                sprint,
                cameraYaw
            );
            PacketDistributor.sendToServer(packet);

            // Mettre à jour le cache
            lastForward = forward;
            lastStrafe = strafe;
            lastJump = jump;
            lastSprint = sprint;
            lastCameraYaw = cameraYaw;
        }
    }

    /**
     * Vérifie si l'input a changé depuis le dernier tick.
     */
    private static boolean inputChanged(float forward, float strafe, boolean jump, boolean sprint, float cameraYaw) {
        return forward != lastForward ||
               strafe != lastStrafe ||
               jump != lastJump ||
               sprint != lastSprint ||
               Math.abs(cameraYaw - lastCameraYaw) > 0.1f;
    }

    /**
     * Réinitialise le cache d'input.
     * Appelé quand le joueur descend de l'abeille.
     */
    public static void resetCache() {
        lastForward = 0f;
        lastStrafe = 0f;
        lastJump = false;
        lastSprint = false;
        lastCameraYaw = 0f;
    }
}
