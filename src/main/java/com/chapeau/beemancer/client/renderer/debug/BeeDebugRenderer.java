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
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.debug;

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

    private static final float LINE_WIDTH = 2.0f;
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

        // Vérifier si le debug est actif
        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        // Chercher toutes les MagicBee dans un rayon de 64 blocs
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
                renderLine(poseStack, bufferSource, beePos, destPos,
                        LINE_R, LINE_G, LINE_B, LINE_A);
                renderSquare(poseStack, bufferSource, destPos);
            }

            // Dessiner le chemin Theta* (blanc)
            List<BlockPos> path = bee.getDebugPath();
            if (!path.isEmpty()) {
                Vec3 prevPos = beePos;
                for (BlockPos waypoint : path) {
                    Vec3 wpPos = Vec3.atCenterOf(waypoint);
                    renderLine(poseStack, bufferSource, prevPos, wpPos,
                            PATH_R, PATH_G, PATH_B, PATH_A);
                    renderWaypointMarker(poseStack, bufferSource, wpPos);
                    prevPos = wpPos;
                }
            }
        }

        poseStack.popPose();

        // Flush le buffer
        bufferSource.endBatch();
    }

    /**
     * Dessine une ligne entre deux points avec couleur personnalisée.
     */
    private static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 from, Vec3 to,
                                    float r, float g, float b, float a) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.001) return;
        dir = dir.scale(1.0 / len);
        float nx = (float) dir.x;
        float ny = (float) dir.y;
        float nz = (float) dir.z;

        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);

        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
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
        addColoredLineVertex(consumer, matrix, x - s, y, z, x + s, y, z, WP_R, WP_G, WP_B, WP_A);
        addColoredLineVertex(consumer, matrix, x, y - s, z, x, y + s, z, WP_R, WP_G, WP_B, WP_A);
        addColoredLineVertex(consumer, matrix, x, y, z - s, x, y, z + s, WP_R, WP_G, WP_B, WP_A);
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
        // Ligne 1: -x,-z -> +x,-z
        addLineVertex(consumer, matrix, x - s, y, z - s, x + s, y, z - s);
        // Ligne 2: +x,-z -> +x,+z
        addLineVertex(consumer, matrix, x + s, y, z - s, x + s, y, z + s);
        // Ligne 3: +x,+z -> -x,+z
        addLineVertex(consumer, matrix, x + s, y, z + s, x - s, y, z + s);
        // Ligne 4: -x,+z -> -x,-z
        addLineVertex(consumer, matrix, x - s, y, z + s, x - s, y, z - s);

        // Croix au centre
        addLineVertex(consumer, matrix, x - s * 0.5f, y, z, x + s * 0.5f, y, z);
        addLineVertex(consumer, matrix, x, y, z - s * 0.5f, x, y, z + s * 0.5f);
    }

    private static void addLineVertex(VertexConsumer consumer, Matrix4f matrix,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2) {
        addColoredLineVertex(consumer, matrix, x1, y1, z1, x2, y2, z2,
                SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
    }

    private static void addColoredLineVertex(VertexConsumer consumer, Matrix4f matrix,
                                              float x1, float y1, float z1,
                                              float x2, float y2, float z2,
                                              float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) len = 1;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        consumer.addVertex(matrix, x1, y1, z1)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);

        consumer.addVertex(matrix, x2, y2, z2)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
    }
}
