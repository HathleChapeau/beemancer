package com.chapeau.beemancer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LogicCostVisualizer {
    public static boolean ENABLED = false;
    private static final int RADIUS = 16;

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

                    // On ne récupère le BlockEntity que si nécessaire (optimisation)
                    if (!state.hasBlockEntity()) continue;
                    BlockEntity be = mc.level.getBlockEntity(pos);

                    float cost = getPreciseLogicCost(state, be);

                    if (cost > 0) {
                        // Dégradé Vert (Faible) -> Rouge/Violet (Fort)
                        float r = cost;
                        float g = 1.0f - cost;
                        float b = (cost > 0.8f) ? 1.0f : 0.0f; // Ajoute du bleu pour faire violet si très élevé

                        LevelRenderer.renderLineBox(poseStack, buffer, new AABB(pos), r, g, b, 1.0f);
                    }
                }
            }
        }
        poseStack.popPose();
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private static float getPreciseLogicCost(BlockState state, BlockEntity be) {
        // 1. HOPPERS (Le plus gros coupable habituel)
        if (state.getBlock() instanceof HopperBlock) {
            boolean isEnabled = state.getValue(HopperBlock.ENABLED); // Vrai si PAS de signal redstone
            // Un hopper actif scanne les entités et les inventaires à chaque tick
            return isEnabled ? 0.9f : 0.1f; // 0.1 si désactivé par redstone (très léger)
        }

        // 2. FOURS & MACHINES DE CUISSON
        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            boolean isLit = state.getValue(AbstractFurnaceBlock.LIT);
            // Un four allumé fait des calculs de recette et de cuisson
            return isLit ? 0.7f : 0.05f;
        }

        // 3. SPAWNERS (Toujours actifs si joueur proche)
        if (be instanceof SpawnerBlockEntity) return 1.0f;

        // 4. PISTONS (Seulement si en mouvement ou étendu)
        if (state.getBlock() instanceof PistonBaseBlock) {
            return state.getValue(PistonBaseBlock.EXTENDED) ? 0.4f : 0.0f;
        }

        // 5. BEACONS (Scanne la zone pour donner des effets)
        if (be instanceof BeaconBlockEntity) return 0.6f;

        // 6. COFFRES (Passifs sauf si beaucoup d'inventaire, on met une valeur basse)
        if (be instanceof ChestBlockEntity) return 0.05f;

        return 0.0f;
    }
}