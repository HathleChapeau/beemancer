/**
 * ============================================================
 * [ChopperCubePreviewRenderer.java]
 * Description: Renderer pour le glow des buches detectees par le Chopper Cube
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ChopperCubeItem         | Scan buches          | findConnectedLogs()            |
 * | ChopperCubeLockHelper   | Etat lock            | Positions verrouillees         |
 * | RenderLevelStageEvent   | Hook rendu           | Dessin outlines                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.common.item.tool.ChopperCubeItem;
import com.chapeau.apica.common.item.tool.ChopperCubeLockHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Dessine des contours verts autour des buches detectees par le Chopper Cube.
 * Mode live : suit le curseur. Mode locked : positions figees par clic droit.
 */
@OnlyIn(Dist.CLIENT)
public class ChopperCubePreviewRenderer {

    private static final float R = 0.2f;
    private static final float G = 0.9f;
    private static final float B = 0.2f;
    private static final float A = 0.8f;

    // Cache live (evite recalcul chaque frame)
    private static BlockPos cachedTargetPos = null;
    private static List<BlockPos> cachedPositions = List.of();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasItemMainHand = player.getMainHandItem().getItem() instanceof ChopperCubeItem;
        boolean hasItemOffHand = player.getOffhandItem().getItem() instanceof ChopperCubeItem;

        if (!hasItemMainHand && !hasItemOffHand) {
            ChopperCubeLockHelper.reset();
            cachedTargetPos = null;
            cachedPositions = List.of();
            return;
        }

        List<BlockPos> positions;

        if (ChopperCubeLockHelper.isLocked()) {
            positions = ChopperCubeLockHelper.getLockedPositions();
        } else {
            positions = computeLivePositions(mc, player);
        }

        if (positions.isEmpty()) return;

        renderOutlines(event, mc, positions);
    }

    /**
     * Calcule les positions en mode live (curseur sur une buche).
     */
    private static List<BlockPos> computeLivePositions(Minecraft mc, Player player) {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        Level level = player.level();

        if (!level.getBlockState(targetPos).is(BlockTags.LOGS)) {
            return List.of();
        }

        // Cache valide ?
        if (targetPos.equals(cachedTargetPos)) {
            return cachedPositions;
        }

        List<BlockPos> result = ChopperCubeItem.findConnectedLogs(level, targetPos);
        cachedTargetPos = targetPos.immutable();
        cachedPositions = result;
        return result;
    }

    /**
     * Dessine les outlines vertes autour des buches.
     */
    private static void renderOutlines(RenderLevelStageEvent event, Minecraft mc,
                                        List<BlockPos> positions) {
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (BlockPos pos : positions) {
            renderBlockOutline(poseStack, buffer, pos);
        }

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * Dessine un contour autour d'un bloc.
     */
    private static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer, BlockPos pos) {
        double offset = 0.002;
        double offsetD = offset * 2;

        double x = pos.getX() + offset;
        double y = pos.getY() + offset;
        double z = pos.getZ() + offset;

        LevelRenderer.renderLineBox(
            poseStack, buffer,
            x, y, z,
            x + 1 - offsetD, y + 1 - offsetD, z + 1 - offsetD,
            R, G, B, A
        );
    }
}
