/**
 * ============================================================
 * [BeeDebugRenderer.java]
 * Description: Renderer de debug pour visualiser les destinations des abeilles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * | MagicBeeEntity      | Destinations         | Récupération des positions     |
 * | DebugRenderHelper   | Rendu lignes         | drawLine(), drawCubeOutline()  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.debug;

import com.chapeau.beemancer.client.renderer.util.DebugRenderHelper;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderer pour afficher les lignes de debug des abeilles.
 * Affiche une ligne de l'abeille vers sa destination avec un carré à la destination.
 */
@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer {

    private static final float SQUARE_SIZE = 0.3f;

    // Couleurs - ligne destination (orange)
    private static final float LINE_R = 1.0f;
    private static final float LINE_G = 0.5f;
    private static final float LINE_B = 0.0f;
    private static final float LINE_A = 0.8f;

    // Couleurs - carré destination (jaune)
    private static final float SQUARE_R = 1.0f;
    private static final float SQUARE_G = 1.0f;
    private static final float SQUARE_B = 0.0f;
    private static final float SQUARE_A = 0.9f;

    // Couleurs - chemin Theta* (blanc)
    private static final float PATH_R = 1.0f;
    private static final float PATH_G = 1.0f;
    private static final float PATH_B = 1.0f;
    private static final float PATH_A = 0.9f;

    // Couleurs - waypoints (cyan)
    private static final float WP_R = 0.0f;
    private static final float WP_G = 1.0f;
    private static final float WP_B = 1.0f;
    private static final float WP_A = 0.9f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        AABB searchBox = player.getBoundingBox().inflate(64);
        List<MagicBeeEntity> bees = mc.level.getEntitiesOfClass(MagicBeeEntity.class, searchBox);

        if (bees.isEmpty()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (MagicBeeEntity bee : bees) {
            Vec3 beePos = bee.position().add(0, bee.getBbHeight() / 2, 0);

            // Dessiner la ligne de destination (orange)
            BlockPos destination = bee.getDebugDestination();
            if (destination != null) {
                Vec3 destPos = Vec3.atCenterOf(destination);
                DebugRenderHelper.drawLine(poseStack, bufferSource, beePos, destPos,
                        LINE_R, LINE_G, LINE_B, LINE_A);
                renderSquare(poseStack, bufferSource, destPos);
            }

            // Dessiner le chemin Theta* (blanc)
            List<BlockPos> path = bee.getDebugPath();
            if (!path.isEmpty()) {
                Vec3 prevPos = beePos;
                for (BlockPos waypoint : path) {
                    Vec3 wpPos = Vec3.atCenterOf(waypoint);
                    DebugRenderHelper.drawLine(poseStack, bufferSource, prevPos, wpPos,
                            PATH_R, PATH_G, PATH_B, PATH_A);
                    renderWaypointMarker(poseStack, bufferSource, wpPos);
                    prevPos = wpPos;
                }
            }
        }

        poseStack.popPose();

        bufferSource.endBatch();
    }

    /**
     * Dessine un petit marqueur de waypoint (croix 3D cyan).
     */
    private static void renderWaypointMarker(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;
        float s = 0.15f;

        // Croix 3D (3 lignes)
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z, x + s, y, z, WP_R, WP_G, WP_B, WP_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y - s, z, x, y + s, z, WP_R, WP_G, WP_B, WP_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y, z - s, x, y, z + s, WP_R, WP_G, WP_B, WP_A);
    }

    /**
     * Dessine un petit carré à une position.
     */
    private static void renderSquare(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;
        float s = SQUARE_SIZE;

        // Carré horizontal (4 lignes)
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z - s, x + s, y, z - s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x + s, y, z - s, x + s, y, z + s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x + s, y, z + s, x - s, y, z + s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z + s, x - s, y, z - s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);

        // Croix au centre
        DebugRenderHelper.drawLine(consumer, matrix, x - s * 0.5f, y, z, x + s * 0.5f, y, z, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y, z - s * 0.5f, x, y, z + s * 0.5f, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
    }
}
