/**
 * ============================================================
 * [MiningLaserBeamRenderer.java]
 * Description: Rendu world-space du rayon laser (2 quads billboard croisés)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RenderLevelStageEvent   | Hook rendu world     | Rendu après translucents       |
 * | MiningLaserItem         | Détection item actif | chargeLevel + useTicks         |
 * | AnimationTimer          | Temps fluide         | Oscillation du beam            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement event bus)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.item.tool.MiningLaserItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Renderer world-space pour le rayon du Mining Laser.
 * Dessine 2 quads billboard croisés (formant un X vu de face) de l'origine
 * à la destination, avec un core lumineux fin et un glow large semi-transparent.
 * Gère la distinction first-person / third-person pour l'origine du rayon.
 */
@OnlyIn(Dist.CLIENT)
public class MiningLaserBeamRenderer {

    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/mining_laser_beam.png");

    /** Demi-largeur du core lumineux */
    private static final float CORE_HALF_WIDTH = 0.03f;

    /** Demi-largeur du glow */
    private static final float GLOW_HALF_WIDTH = 0.12f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!isLaserFiring(player)) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        Vec3 origin = computeBeamOrigin(player, camera, partialTick);
        Vec3 destination = computeBeamDestination(player, partialTick);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        float time = AnimationTimer.getRenderTime(partialTick);
        float pulse = 0.8f + 0.2f * (float) Math.sin(time * 0.5);

        // Core beam (fin, lumineux)
        renderBeamQuads(poseStack, bufferSource, origin, destination, camPos,
                CORE_HALF_WIDTH, 1.0f, 1.0f, 0.9f, pulse);

        // Glow beam (large, semi-transparent)
        renderBeamQuads(poseStack, bufferSource, origin, destination, camPos,
                GLOW_HALF_WIDTH, 1.0f, 0.95f, 0.6f, 0.3f * pulse);

        bufferSource.endBatch();
        poseStack.popPose();
    }

    /**
     * Vérifie si le joueur local tire activement le laser (jauge pleine).
     */
    private static boolean isLaserFiring(Player player) {
        if (!player.isUsingItem()) return false;
        if (!(player.getUseItem().getItem() instanceof MiningLaserItem)) return false;
        int chargeLevel = MiningLaserItem.getChargeLevel(player.getUseItem());
        if (chargeLevel <= 0) return false;
        return player.getTicksUsingItem() >= MiningLaserItem.CHARGE_TICKS;
    }

    /**
     * Calcule l'origine du rayon selon le mode caméra.
     */
    private static Vec3 computeBeamOrigin(Player player, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            Vec3 camBase = camera.getPosition();
            Vec3 look = player.getViewVector(partialTick);
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

            boolean isRightHand = mc.options.mainHand().get() == HumanoidArm.RIGHT;
            float sideOffset = isRightHand ? 0.3f : -0.3f;
            return camBase.add(right.scale(sideOffset)).add(0, -0.15, 0).add(look.scale(0.5));
        }

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        return eyePos.add(look.scale(0.5));
    }

    /**
     * Calcule la destination du rayon via raycast.
     */
    private static Vec3 computeBeamDestination(Player player, float partialTick) {
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 endPos = eyePos.add(look.scale(MiningLaserItem.MAX_RANGE));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getLocation();
        }
        return endPos;
    }

    /**
     * Dessine 2 quads croisés (X shape) entre origin et destination.
     * Les quads sont orientés perpendiculairement au rayon et à la caméra.
     */
    private static void renderBeamQuads(PoseStack poseStack, MultiBufferSource buffer,
                                         Vec3 origin, Vec3 destination, Vec3 camPos,
                                         float halfWidth, float r, float g, float b, float a) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;
        int light = 15728880; // Full bright
        PoseStack.Pose pose = poseStack.last();

        Vec3 beamDir = destination.subtract(origin);
        double length = beamDir.length();
        if (length < 0.01) return;
        Vec3 dir = beamDir.normalize();

        Vec3 beamCenter = origin.add(destination).scale(0.5);
        Vec3 toCam = camPos.subtract(beamCenter).normalize();

        // Perpendiculaire 1 : beam × toCam
        Vec3 perp1 = dir.cross(toCam).normalize().scale(halfWidth);
        // Perpendiculaire 2 : beam × perp1
        Vec3 perp2 = dir.cross(perp1.normalize()).normalize().scale(halfWidth);

        float vLen = (float) length;

        // Quad 1 (orientation perp1)
        emitBeamQuad(vc, pose, origin, destination, perp1, light, overlay, r, g, b, a, vLen);

        // Quad 2 (orientation perp2) — rotation 90° autour du rayon
        emitBeamQuad(vc, pose, origin, destination, perp2, light, overlay, r, g, b, a, vLen);
    }

    /**
     * Emet un quad entre origin et destination, élargi par perpOffset des deux côtés.
     */
    private static void emitBeamQuad(VertexConsumer vc, PoseStack.Pose pose,
                                      Vec3 origin, Vec3 destination, Vec3 perpOffset,
                                      int light, int overlay,
                                      float r, float g, float b, float a, float vLen) {
        Vec3 p0 = origin.add(perpOffset);
        Vec3 p1 = origin.subtract(perpOffset);
        Vec3 p2 = destination.subtract(perpOffset);
        Vec3 p3 = destination.add(perpOffset);

        // Normale : perpOffset normalisé
        Vec3 norm = perpOffset.normalize();

        vc.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z)
                .setColor(r, g, b, a)
                .setUv(0, 0).setOverlay(overlay).setLight(light)
                .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z)
                .setColor(r, g, b, a)
                .setUv(1, 0).setOverlay(overlay).setLight(light)
                .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z)
                .setColor(r, g, b, a)
                .setUv(1, vLen).setOverlay(overlay).setLight(light)
                .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z)
                .setColor(r, g, b, a)
                .setUv(0, vLen).setOverlay(overlay).setLight(light)
                .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
    }
}
