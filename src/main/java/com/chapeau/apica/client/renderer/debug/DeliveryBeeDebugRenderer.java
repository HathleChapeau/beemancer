/**
 * ============================================================
 * [DeliveryBeeDebugRenderer.java]
 * Description: Renderer debug pour visualiser les paths des delivery bees
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | DeliveryBeeEntity             | Entite bee           | Waypoints, position            |
 * | DebugWandItem                 | Flag displayDebug    | Toggle affichage               |
 * | DebugRenderHelper             | Primitives de rendu  | drawLine, drawCubeOutline      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement de l'event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.debug;

import com.chapeau.apica.client.renderer.util.DebugRenderHelper;
import com.chapeau.apica.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderer debug qui affiche les waypoints des delivery bees quand displayDebug=true.
 *
 * Couleurs:
 * - Cyan: outbound waypoints (controller -> source)
 * - Magenta: transit waypoints (source -> dest via LCA)
 * - Jaune: home waypoints (dest -> controller)
 * - Vert: position bee actuelle
 */
@OnlyIn(Dist.CLIENT)
public class DeliveryBeeDebugRenderer {

    private static final int SCAN_RANGE = 64;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Scanner les delivery bees dans un rayon autour du joueur
        AABB scanArea = new AABB(mc.player.blockPosition()).inflate(SCAN_RANGE);
        List<DeliveryBeeEntity> bees = mc.level.getEntitiesOfClass(DeliveryBeeEntity.class, scanArea);

        if (bees.isEmpty()) return;

        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        for (DeliveryBeeEntity bee : bees) {
            renderBeeWaypoints(bee, lineBuffer, matrix);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    /**
     * Rend les waypoints d'une bee avec des lignes colorees.
     * Utilise les donnees synchronisees pour fonctionner cote client.
     */
    private static void renderBeeWaypoints(DeliveryBeeEntity bee, VertexConsumer buffer, Matrix4f matrix) {
        Vec3 beePos = bee.position();

        // Recuperer les donnees synchronisees
        net.minecraft.nbt.CompoundTag tag = bee.getDebugWaypointsTag();
        if (tag == null || tag.isEmpty()) return;

        BlockPos controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
        BlockPos sourcePos = tag.contains("Source") ? BlockPos.of(tag.getLong("Source")) : null;
        BlockPos destPos = tag.contains("Dest") ? BlockPos.of(tag.getLong("Dest")) : null;
        BlockPos returnPos = tag.contains("Return") ? BlockPos.of(tag.getLong("Return")) : null;

        List<BlockPos> outbound = longArrayToBlockPosList(tag.getLongArray("Outbound"));
        List<BlockPos> transit = longArrayToBlockPosList(tag.getLongArray("Transit"));
        List<BlockPos> home = longArrayToBlockPosList(tag.getLongArray("Home"));

        // Marqueur vert: position actuelle de la bee
        float bx = (float) beePos.x;
        float by = (float) beePos.y;
        float bz = (float) beePos.z;
        DebugRenderHelper.drawCubeOutline(buffer, matrix,
            bx - 0.2f, by - 0.2f, bz - 0.2f,
            bx + 0.2f, by + 0.2f, bz + 0.2f,
            0.2f, 1.0f, 0.2f, 1.0f);

        // Outbound: Cyan (controller -> relays -> source)
        if (!outbound.isEmpty() && controllerPos != null) {
            renderPath(buffer, matrix, controllerPos, outbound, sourcePos,
                0.3f, 0.9f, 1.0f, 0.9f);
        }

        // Transit: Magenta (source -> relays -> dest)
        if (!transit.isEmpty() && sourcePos != null && destPos != null) {
            renderPath(buffer, matrix, sourcePos, transit, destPos,
                1.0f, 0.3f, 0.9f, 0.9f);
        }

        // Home: Jaune (dest -> relays -> controller)
        if (!home.isEmpty() && destPos != null && returnPos != null) {
            renderPath(buffer, matrix, destPos, home, returnPos,
                1.0f, 0.9f, 0.2f, 0.9f);
        }

        // Outline source (vert)
        if (sourcePos != null) {
            DebugRenderHelper.drawCubeOutline(buffer, matrix,
                sourcePos.getX() - 0.02f, sourcePos.getY() - 0.02f, sourcePos.getZ() - 0.02f,
                sourcePos.getX() + 1.02f, sourcePos.getY() + 1.02f, sourcePos.getZ() + 1.02f,
                0.2f, 1.0f, 0.2f, 0.8f);
        }

        // Outline dest (rouge)
        if (destPos != null) {
            DebugRenderHelper.drawCubeOutline(buffer, matrix,
                destPos.getX() - 0.02f, destPos.getY() - 0.02f, destPos.getZ() - 0.02f,
                destPos.getX() + 1.02f, destPos.getY() + 1.02f, destPos.getZ() + 1.02f,
                1.0f, 0.3f, 0.2f, 0.8f);
        }
    }

    /**
     * Rend un chemin: start -> waypoints -> end
     */
    private static void renderPath(VertexConsumer buffer, Matrix4f matrix,
                                    BlockPos start, List<BlockPos> waypoints, BlockPos end,
                                    float r, float g, float b, float a) {
        BlockPos prev = start;

        // Lignes vers chaque waypoint
        for (BlockPos wp : waypoints) {
            drawLineToCenter(buffer, matrix, prev, wp, r, g, b, a);
            // Petit marqueur au waypoint
            DebugRenderHelper.drawCubeOutline(buffer, matrix,
                wp.getX() + 0.3f, wp.getY() + 0.3f, wp.getZ() + 0.3f,
                wp.getX() + 0.7f, wp.getY() + 0.7f, wp.getZ() + 0.7f,
                r, g, b, a * 0.7f);
            prev = wp;
        }

        // Ligne finale vers la destination
        if (end != null) {
            drawLineToCenter(buffer, matrix, prev, end, r, g, b, a);
        }
    }

    /**
     * Dessine une ligne entre les centres de deux blocs.
     */
    private static void drawLineToCenter(VertexConsumer buffer, Matrix4f matrix,
                                          BlockPos from, BlockPos to,
                                          float r, float g, float b, float a) {
        DebugRenderHelper.drawLine(buffer, matrix,
            from.getX() + 0.5f, from.getY() + 0.5f, from.getZ() + 0.5f,
            to.getX() + 0.5f, to.getY() + 0.5f, to.getZ() + 0.5f,
            r, g, b, a);
    }

    /**
     * Convertit un long array en liste de BlockPos.
     */
    private static List<BlockPos> longArrayToBlockPosList(long[] longs) {
        List<BlockPos> result = new java.util.ArrayList<>(longs.length);
        for (long l : longs) {
            result.add(BlockPos.of(l));
        }
        return result;
    }
}
