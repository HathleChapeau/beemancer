/**
 * ============================================================
 * [TranslucentOutlineRenderer.java]
 * Description: Override de l'outline de sélection pour les blocs translucent
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerBlocks          | Identification blocs | Vérifier si bloc concerné      |
 * | RenderHighlightEvent     | Event NeoForge       | Intercepter rendu outline      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement sur EVENT_BUS)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Redessine l'outline de sélection pour les blocs translucent de Beemancer.
 * Le rendu vanilla a un bug de depth buffer sur les blocs translucent
 * qui rend l'outline transparent au lieu du trait noir.
 */
public class TranslucentOutlineRenderer {

    private static final Set<Block> TRANSLUCENT_BLOCKS = Set.of(
        BeemancerBlocks.CRYSTALLIZER.get(),
        BeemancerBlocks.INFUSER.get(),
        BeemancerBlocks.HONEY_TANK.get(),
        BeemancerBlocks.POLLEN_POT.get()
    );

    private static final float LINE_RED = 0.16f;
    private static final float LINE_GREEN = 0.16f;
    private static final float LINE_BLUE = 0.16f;
    private static final float LINE_ALPHA = 1f;

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        BlockPos blockPos = event.getTarget().getBlockPos();
        Level level = mc.level;
        BlockState blockState = level.getBlockState(blockPos);

        if (!TRANSLUCENT_BLOCKS.contains(blockState.getBlock())) {
            return;
        }

        event.setCanceled(true);

        VoxelShape shape = blockState.getShape(level, blockPos);
        if (shape.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(
            blockPos.getX() - camera.getPosition().x,
            blockPos.getY() - camera.getPosition().y,
            blockPos.getZ() - camera.getPosition().z
        );

        Matrix4f matrix = poseStack.last().pose();

        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float dx = (float) (maxX - minX);
            float dy = (float) (maxY - minY);
            float dz = (float) (maxZ - minZ);
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            float nx = length > 1e-6f ? dx / length : 1f;
            float ny = length > 1e-6f ? dy / length : 0f;
            float nz = length > 1e-6f ? dz / length : 0f;

            vertexConsumer.addVertex(matrix, (float) minX, (float) minY, (float) minZ)
                .setColor(LINE_RED, LINE_GREEN, LINE_BLUE, LINE_ALPHA)
                .setNormal(nx, ny, nz);

            vertexConsumer.addVertex(matrix, (float) maxX, (float) maxY, (float) maxZ)
                .setColor(LINE_RED, LINE_GREEN, LINE_BLUE, LINE_ALPHA)
                .setNormal(nx, ny, nz);
        });

        poseStack.popPose();
    }
}
