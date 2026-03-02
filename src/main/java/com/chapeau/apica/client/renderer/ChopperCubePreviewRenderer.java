/**
 * ============================================================
 * [ChopperCubePreviewRenderer.java]
 * Description: Renderer pour le glow ambre et les abeilles orbitantes du Chopper Cube
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ChopperCubeItem         | Scan buches          | findConnectedLogs()            |
 * | ChopperCubeLockHelper   | Etat lock            | Positions verrouillees         |
 * | AnimationTimer          | Temps fluide         | Pulsation + orbite             |
 * | BeeModel                | Modele abeille       | Rendu mini-abeilles            |
 * | RenderLevelStageEvent   | Hook rendu           | Dessin outlines + abeilles     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.item.tool.ChopperCubeItem;
import com.chapeau.apica.common.item.tool.ChopperCubeLockHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Comparator;
import java.util.List;

/**
 * Dessine des contours ambre pulsants autour des buches detectees par le Chopper Cube.
 * En mode destruction, 2 mini-abeilles orbitent autour du bloc en cours.
 * Mode live : suit le curseur. Mode locked : positions figees par clic droit.
 */
@OnlyIn(Dist.CLIENT)
public class ChopperCubePreviewRenderer {

    // Couleur ambre/miel
    private static final float R = 1.0f;
    private static final float G = 0.78f;
    private static final float B = 0.15f;

    // Abeilles orbitantes
    private static final float BEE_SCALE = 0.3f;
    private static final double ORBIT_RADIUS = 0.8;
    private static final ResourceLocation BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    // Cache live (evite recalcul chaque frame)
    private static BlockPos cachedTargetPos = null;
    private static List<BlockPos> cachedPositions = List.of();

    // BeeModel lazy-init
    private static BeeModel<?> beeModel;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasItem = player.getMainHandItem().getItem() instanceof ChopperCubeItem
                       || player.getOffhandItem().getItem() instanceof ChopperCubeItem;

        if (!hasItem) {
            ChopperCubeLockHelper.reset();
            cachedTargetPos = null;
            cachedPositions = List.of();
            return;
        }

        boolean chopping = ChopperCubeLockHelper.isLocked();
        List<BlockPos> positions;

        if (chopping) {
            positions = ChopperCubeLockHelper.getLockedPositions();
        } else {
            positions = computeLivePositions(mc, player);
        }

        if (positions.isEmpty()) return;

        // Filtrer les blocs deja detruits
        Level level = player.level();
        List<BlockPos> remaining = positions.stream()
                .filter(p -> !level.getBlockState(p).isAir())
                .toList();

        if (remaining.isEmpty()) {
            if (chopping) ChopperCubeLockHelper.reset();
            return;
        }

        renderOutlines(event, mc, remaining);

        if (chopping) {
            // Bloc courant = le plus haut non-air (destruction top -> bottom)
            BlockPos target = remaining.stream()
                    .max(Comparator.comparingInt(BlockPos::getY))
                    .orElse(null);
            if (target != null) {
                renderOrbitingBees(event, mc, target);
            }
        }
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

        if (targetPos.equals(cachedTargetPos)) {
            return cachedPositions;
        }

        List<BlockPos> result = ChopperCubeItem.findConnectedLogs(level, targetPos);
        cachedTargetPos = targetPos.immutable();
        cachedPositions = result;
        return result;
    }

    /**
     * Dessine des contours ambre avec alpha pulsante autour des buches.
     */
    private static void renderOutlines(RenderLevelStageEvent event, Minecraft mc,
                                        List<BlockPos> positions) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        float time = AnimationTimer.getRenderTime(event.getPartialTick().getGameTimeDeltaPartialTick(true));

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (BlockPos pos : positions) {
            float alpha = 0.4f + 0.4f * (float) Math.sin(time * 0.12 + pos.getY() * 0.8);
            renderBlockOutline(poseStack, buffer, pos, alpha);
        }

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * Dessine un contour ambre autour d'un bloc.
     */
    private static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer,
                                            BlockPos pos, float alpha) {
        double offset = 0.002;
        double offsetD = offset * 2;

        double x = pos.getX() + offset;
        double y = pos.getY() + offset;
        double z = pos.getZ() + offset;

        LevelRenderer.renderLineBox(
            poseStack, buffer,
            x, y, z,
            x + 1 - offsetD, y + 1 - offsetD, z + 1 - offsetD,
            R, G, B, alpha
        );
    }

    /**
     * Rend 2 petites abeilles orbitant autour du bloc en cours de destruction.
     * Utilise le BeeModel vanilla, meme pattern que BeeStatueRenderer.
     */
    private static void renderOrbitingBees(RenderLevelStageEvent event, Minecraft mc,
                                            BlockPos targetPos) {
        Level level = mc.player.level();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        Vec3 center = Vec3.atCenterOf(targetPos);
        float time = AnimationTimer.getRenderTime(event.getPartialTick().getGameTimeDeltaPartialTick(true));
        int light = LevelRenderer.getLightColor(level, targetPos);

        BeeModel<?> model = getOrCreateBeeModel();
        var bufferSource = mc.renderBuffers().bufferSource();
        RenderType beeRenderType = RenderType.entityCutout(BEE_TEXTURE);
        VertexConsumer vc = bufferSource.getBuffer(beeRenderType);

        for (int i = 0; i < 2; i++) {
            double angle = time * 0.15 + i * Math.PI;
            double bx = center.x + Math.cos(angle) * ORBIT_RADIUS;
            double bz = center.z + Math.sin(angle) * ORBIT_RADIUS;

            poseStack.pushPose();
            poseStack.translate(bx - camPos.x, center.y - camPos.y + 0.1, bz - camPos.z);

            // Rotation pour suivre la direction d'orbite
            float yRot = (float) Math.toDegrees(angle) + 90;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

            // Retourner le modele (convention Minecraft entity models)
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            poseStack.scale(BEE_SCALE, BEE_SCALE, BEE_SCALE);

            model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

        bufferSource.endBatch(beeRenderType);
    }

    /**
     * Cree ou retourne le BeeModel en cache.
     */
    private static BeeModel<?> getOrCreateBeeModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BEE)
            );
        }
        return beeModel;
    }
}
