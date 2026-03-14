/**
 * ============================================================
 * [BeeDebugRenderer.java]
 * Description: Renderer de debug pour visualiser les destinations des abeilles
 *              et les infos des bee nests
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * | MagicBeeEntity      | Destinations         | Recuperation des positions     |
 * | BeeNestBlockEntity  | Infos nid            | MaxBees, respawn timers        |
 * | DebugRenderHelper   | Rendu lignes         | drawLine(), drawCubeOutline()  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.debug;

import com.chapeau.apica.client.renderer.util.DebugRenderHelper;
import com.chapeau.apica.common.block.hive.BeeNestBlockEntity;
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renderer pour afficher les lignes de debug des abeilles
 * et les infos des bee nests (maxBees, respawn timers).
 */
@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer {

    private static final float SQUARE_SIZE = 0.3f;

    // Couleurs - ligne destination (orange)
    private static final float LINE_R = 1.0f;
    private static final float LINE_G = 0.5f;
    private static final float LINE_B = 0.0f;
    private static final float LINE_A = 0.8f;

    // Couleurs - carre destination (jaune)
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

    // Couleurs - chemin vanilla FlyingPathNavigation (rouge)
    private static final float VANILLA_R = 1.0f;
    private static final float VANILLA_G = 0.0f;
    private static final float VANILLA_B = 0.0f;
    private static final float VANILLA_A = 0.9f;

    // Couleurs - texte nest (vert)
    private static final int NEST_TEXT_COLOR = 0xFF55FF55;
    private static final int NEST_TEXT_BG = 0x88000000;

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
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Collecter les nids a afficher via les abeilles (pour trouver leurs nids)
        AABB searchBox = player.getBoundingBox().inflate(64);
        List<MagicBeeEntity> bees = mc.level.getEntitiesOfClass(MagicBeeEntity.class, searchBox);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Dessiner les lignes des abeilles
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

            // Dessiner le chemin vanilla FlyingPathNavigation (rouge)
            List<BlockPos> vanillaPath = bee.getDebugVanillaPath();
            if (!vanillaPath.isEmpty()) {
                Vec3 prevPos = beePos;
                for (BlockPos waypoint : vanillaPath) {
                    Vec3 wpPos = Vec3.atCenterOf(waypoint);
                    DebugRenderHelper.drawLine(poseStack, bufferSource, prevPos, wpPos,
                            VANILLA_R, VANILLA_G, VANILLA_B, VANILLA_A);
                    renderVanillaWaypointMarker(poseStack, bufferSource, wpPos);
                    prevPos = wpPos;
                }
            }
        }

        poseStack.popPose();
        bufferSource.endBatch();

        // Dessiner les infos des bee nests (texte flottant)
        Set<BlockPos> renderedNests = new HashSet<>();
        for (MagicBeeEntity bee : bees) {
            BlockPos nestPos = bee.getHomeNestPos();
            if (nestPos != null && renderedNests.add(nestPos)) {
                renderNestInfo(mc, poseStack, cameraPos, nestPos);
            }
        }

        // Afficher aussi le nid que le joueur regarde (crosshair)
        if (mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos lookPos = blockHit.getBlockPos();
            if (renderedNests.add(lookPos)) {
                renderNestInfo(mc, poseStack, cameraPos, lookPos);
            }
        }
    }

    /**
     * Affiche les infos d'un bee nest en texte flottant au-dessus du bloc.
     */
    private static void renderNestInfo(Minecraft mc, PoseStack poseStack, Vec3 cameraPos,
                                        BlockPos nestPos) {
        if (mc.level == null) return;
        if (!(mc.level.getBlockEntity(nestPos) instanceof BeeNestBlockEntity nest)) return;

        int maxBees = nest.getMaxBees();
        int activeBees = nest.getActiveBeeCount();
        List<Integer> timers = nest.getRespawnTimers();

        Font font = mc.font;
        Vec3 textPos = Vec3.atCenterOf(nestPos).add(0, 1.5, 0);

        poseStack.pushPose();
        poseStack.translate(textPos.x - cameraPos.x, textPos.y - cameraPos.y, textPos.z - cameraPos.z);

        // Face la camera
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Ligne 1: Max bees
        String line1 = "Nest: " + activeBees + " active / " + maxBees + " max";
        int w1 = font.width(line1);
        font.drawInBatch(line1, -w1 / 2f, 0, NEST_TEXT_COLOR, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH,
                NEST_TEXT_BG, 0xF000F0);

        // Ligne 2+: Respawn timers
        if (!timers.isEmpty()) {
            for (int i = 0; i < timers.size(); i++) {
                float seconds = timers.get(i) / 20f;
                String line = "Respawn #" + (i + 1) + ": " + String.format("%.1fs", seconds);
                int w = font.width(line);
                font.drawInBatch(line, -w / 2f, (i + 1) * 10, NEST_TEXT_COLOR, false,
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH,
                        NEST_TEXT_BG, 0xF000F0);
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
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
     * Dessine un petit marqueur de waypoint vanilla (croix 3D rouge).
     */
    private static void renderVanillaWaypointMarker(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;
        float s = 0.1f;

        // Croix 3D (3 lignes) - plus petite que Theta*
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z, x + s, y, z, VANILLA_R, VANILLA_G, VANILLA_B, VANILLA_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y - s, z, x, y + s, z, VANILLA_R, VANILLA_G, VANILLA_B, VANILLA_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y, z - s, x, y, z + s, VANILLA_R, VANILLA_G, VANILLA_B, VANILLA_A);
    }

    /**
     * Dessine un petit carre a une position.
     */
    private static void renderSquare(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;
        float s = SQUARE_SIZE;

        // Carre horizontal (4 lignes)
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z - s, x + s, y, z - s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x + s, y, z - s, x + s, y, z + s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x + s, y, z + s, x - s, y, z + s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x - s, y, z + s, x - s, y, z - s, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);

        // Croix au centre
        DebugRenderHelper.drawLine(consumer, matrix, x - s * 0.5f, y, z, x + s * 0.5f, y, z, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
        DebugRenderHelper.drawLine(consumer, matrix, x, y, z - s * 0.5f, x, y, z + s * 0.5f, SQUARE_R, SQUARE_G, SQUARE_B, SQUARE_A);
    }
}
