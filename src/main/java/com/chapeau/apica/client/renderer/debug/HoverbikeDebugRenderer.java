/**
 * ============================================================
 * [HoverbikeDebugRenderer.java]
 * Description: Rendu debug 3D des systemes de collision du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Donnees raycasts     | Positions rays + hits          |
 * | HoverbikeCollisionGeometry | Probes AABB   | Affichage des probes           |
 * | DebugRenderHelper   | Primitives de rendu  | drawLine, drawCubeOutline      |
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.debug;

import com.chapeau.apica.client.renderer.util.DebugRenderHelper;
import com.chapeau.apica.common.entity.mount.HoverbikeCollisionGeometry;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Affiche en debug 3D tous les systemes de detection de collision du Hoverbike :
 * - 6 raycasts predictifs (vert = libre, rouge = hit, point jaune = impact)
 * - 3 probes AABB de collision entites (cyan)
 * - Bounding box de l'entite (blanc)
 * Visible uniquement quand displayDebug = true et le joueur monte un Hoverbike.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeDebugRenderer {

    // Couleurs raycasts libres (vert)
    private static final float RAY_FREE_R = 0.2f;
    private static final float RAY_FREE_G = 1.0f;
    private static final float RAY_FREE_B = 0.2f;
    private static final float RAY_FREE_A = 0.8f;

    // Couleurs raycasts touches (rouge)
    private static final float RAY_HIT_R = 1.0f;
    private static final float RAY_HIT_G = 0.2f;
    private static final float RAY_HIT_B = 0.2f;
    private static final float RAY_HIT_A = 0.9f;

    // Couleur point d'impact (jaune)
    private static final float HIT_POINT_R = 1.0f;
    private static final float HIT_POINT_G = 1.0f;
    private static final float HIT_POINT_B = 0.0f;
    private static final float HIT_POINT_A = 1.0f;

    // Couleur probes AABB (cyan)
    private static final float PROBE_R = 0.0f;
    private static final float PROBE_G = 0.8f;
    private static final float PROBE_B = 1.0f;
    private static final float PROBE_A = 0.6f;

    // Couleur bounding box entite (blanc)
    private static final float BB_R = 1.0f;
    private static final float BB_G = 1.0f;
    private static final float BB_B = 1.0f;
    private static final float BB_A = 0.4f;

    // Taille du marqueur de point d'impact
    private static final float HIT_MARKER_SIZE = 0.06f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.player.getVehicle() instanceof HoverbikeEntity hoverbike)) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        // 1. Raycasts predictifs
        renderPredictiveRays(hoverbike, lineBuffer, matrix);

        // 2. Probes AABB de collision entites
        renderCollisionProbes(hoverbike, lineBuffer, matrix);

        // 3. Bounding box de l'entite
        renderEntityBoundingBox(hoverbike, lineBuffer, matrix);

        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
    }

    /**
     * Dessine les 6 raycasts predictifs.
     * Vert si le ray ne touche rien, rouge jusqu'au point d'impact si touche.
     * Point jaune au point d'impact.
     */
    private static void renderPredictiveRays(HoverbikeEntity bike, VertexConsumer buffer, Matrix4f matrix) {
        if (!bike.isDebugRaysActive()) return;

        Vec3[] starts = bike.getDebugRayStarts();
        Vec3[] ends = bike.getDebugRayEnds();
        Vec3[] hits = bike.getDebugRayHits();

        for (int i = 0; i < 6; i++) {
            if (starts[i] == null || ends[i] == null) continue;

            if (hits[i] != null) {
                // Ray a touche : ligne rouge du start au hit
                drawLineVec3(buffer, matrix, starts[i], hits[i],
                        RAY_HIT_R, RAY_HIT_G, RAY_HIT_B, RAY_HIT_A);

                // Marqueur au point d'impact (petit cube jaune)
                float hx = (float) hits[i].x;
                float hy = (float) hits[i].y;
                float hz = (float) hits[i].z;
                DebugRenderHelper.drawCubeOutline(buffer, matrix,
                        hx - HIT_MARKER_SIZE, hy - HIT_MARKER_SIZE, hz - HIT_MARKER_SIZE,
                        hx + HIT_MARKER_SIZE, hy + HIT_MARKER_SIZE, hz + HIT_MARKER_SIZE,
                        HIT_POINT_R, HIT_POINT_G, HIT_POINT_B, HIT_POINT_A);
            } else {
                // Ray libre : ligne verte du start au end
                drawLineVec3(buffer, matrix, starts[i], ends[i],
                        RAY_FREE_R, RAY_FREE_G, RAY_FREE_B, RAY_FREE_A);
            }
        }
    }

    /**
     * Dessine les 3 probes AABB de collision entites (nez, corps, queue).
     */
    private static void renderCollisionProbes(HoverbikeEntity bike, VertexConsumer buffer, Matrix4f matrix) {
        AABB[] probes = HoverbikeCollisionGeometry.calculateWorldBoxes(bike.position(), bike.getYRot());
        for (AABB probe : probes) {
            DebugRenderHelper.drawCubeOutline(buffer, matrix,
                    (float) probe.minX, (float) probe.minY, (float) probe.minZ,
                    (float) probe.maxX, (float) probe.maxY, (float) probe.maxZ,
                    PROBE_R, PROBE_G, PROBE_B, PROBE_A);
        }
    }

    /**
     * Dessine la bounding box de l'entite.
     */
    private static void renderEntityBoundingBox(HoverbikeEntity bike, VertexConsumer buffer, Matrix4f matrix) {
        AABB bb = bike.getBoundingBox();
        DebugRenderHelper.drawCubeOutline(buffer, matrix,
                (float) bb.minX, (float) bb.minY, (float) bb.minZ,
                (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ,
                BB_R, BB_G, BB_B, BB_A);
    }

    /**
     * Dessine une ligne entre deux Vec3 via le VertexConsumer deja obtenu.
     */
    private static void drawLineVec3(VertexConsumer buffer, Matrix4f matrix,
                                     Vec3 from, Vec3 to,
                                     float r, float g, float b, float a) {
        DebugRenderHelper.drawLine(buffer, matrix,
                (float) from.x, (float) from.y, (float) from.z,
                (float) to.x, (float) to.y, (float) to.z,
                r, g, b, a);
    }
}
