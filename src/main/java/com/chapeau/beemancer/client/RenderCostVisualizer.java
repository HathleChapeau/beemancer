package com.chapeau.beemancer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.List;


@OnlyIn(Dist.CLIENT)
public class RenderCostVisualizer {
    public static boolean ENABLED = false;
    private static final int RADIUS = 10; // Rayon réduit car le calcul est lourd
    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!ENABLED || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        BlockPos playerPos = mc.player.blockPosition();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;

                    // --- CALCUL PRÉCIS (Comptage des faces/Quads) ---
                    int quadCount = countQuads(mc, state, pos);

                    if (quadCount > 0) {
                        // 0-6 quads (Cube simple) = Vert
                        // 7-24 quads (Escalier, Barrière) = Jaune
                        // 25+ quads (Modèle complexe/Fleurs détaillées) = Rouge
                        float progress = Math.min(quadCount / 30f, 1.0f);

                        float r = progress;
                        float g = 1.0f - progress;
                        float b = 0.0f;

                        LevelRenderer.renderLineBox(poseStack, buffer, new AABB(pos), r, g, b, 0.8f);
                    }
                }
            }
        }
        poseStack.popPose();
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private static int countQuads(Minecraft mc, BlockState state, BlockPos pos) {
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        int count = 0;

        // On compte les quads pour chaque face (Culling)
        for (Direction d : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, d, RANDOM, ModelData.EMPTY, null);
            count += quads.size();
        }
        // Et les quads sans face spécifique (souvent les plus complexes : plantes, détails)
        List<BakedQuad> quads = model.getQuads(state, null, RANDOM, ModelData.EMPTY, null);
        count += quads.size();

        return count;
    }
}