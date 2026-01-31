/**
 * ============================================================
 * [StorageRelayRenderer.java]
 * Description: Renderer pour le Storage Relay (mode edition)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageRelayBlockEntity       | BlockEntity          | Donnees de rendu               |
 * | DebugRenderHelper             | Rendu lignes/outlines| drawLine/CubeOutline           |
 * | INetworkNode                  | Interface reseau     | Donnees communes               |
 * | StorageNetworkRegistry        | Registre central     | Blocs possedes par ce relay    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.DebugRenderHelper;
import com.chapeau.beemancer.common.blockentity.storage.INetworkNode;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageNetworkRegistry;
import com.chapeau.beemancer.common.blockentity.storage.StorageRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Renderer pour le Storage Relay.
 *
 * Mode edition:
 * - Outline jaune autour du relay
 * - Sphere de rayon d'action (jaune/orange)
 * - Lignes vertes vers les coffres possedes par ce relay
 * - Lignes orange vers les interfaces possedees par ce relay
 * - Lignes cyan vers les terminaux possedes par ce relay
 * - Lignes magenta vers les noeuds connectes
 */
public class StorageRelayRenderer implements BlockEntityRenderer<StorageRelayBlockEntity> {

    public StorageRelayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StorageRelayBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (blockEntity.isEditMode()) {
            renderEditMode(blockEntity, poseStack, bufferSource);
        }
    }

    private void renderEditMode(StorageRelayBlockEntity blockEntity,
                                 PoseStack poseStack, MultiBufferSource bufferSource) {

        var player = Minecraft.getInstance().player;
        if (player == null || !player.getUUID().equals(blockEntity.getEditingPlayer())) {
            return;
        }

        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        // Outline jaune autour du relay
        DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
            -0.01f, -0.01f, -0.01f, 1.01f, 1.01f, 1.01f,
            1.0f, 0.8f, 0.2f, 1.0f);

        // Sphere de rayon
        DebugRenderHelper.drawSphereOutline(lineBuffer, matrix,
            0.5f, 0.5f, 0.5f, StorageRelayBlockEntity.MAX_RANGE, 48,
            1.0f, 0.8f, 0.2f, 0.4f);

        BlockPos relayPos = blockEntity.getBlockPos();

        // Lignes magenta vers les noeuds connectes
        for (BlockPos nodePos : blockEntity.getConnectedNodes()) {
            renderBlockLink(lineBuffer, matrix, relayPos, nodePos,
                    0.8f, 0.2f, 1.0f, 1.0f, 0.8f);
        }

        // Afficher les blocs possedes par ce relay depuis le registre central
        StorageControllerBlockEntity controller = findController(blockEntity);
        if (controller != null) {
            StorageNetworkRegistry registry = controller.getNetworkRegistry();
            Set<BlockPos> ownedBlocks = registry.getBlocksByOwner(relayPos);

            for (BlockPos blockPos : ownedBlocks) {
                StorageNetworkRegistry.NetworkBlockType type = registry.getType(blockPos);
                if (type == null) continue;

                switch (type) {
                    case CHEST -> {
                        // Vert + outline bleu
                        float dx = blockPos.getX() - relayPos.getX();
                        float dy = blockPos.getY() - relayPos.getY();
                        float dz = blockPos.getZ() - relayPos.getZ();
                        DebugRenderHelper.drawLine(lineBuffer, matrix,
                                0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f,
                                0.2f, 1.0f, 0.2f, 1.0f);
                        DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
                                dx - 0.02f, dy - 0.02f, dz - 0.02f,
                                dx + 1.02f, dy + 1.02f, dz + 1.02f,
                                0.2f, 0.6f, 1.0f, 1.0f);
                    }
                    case INTERFACE -> renderBlockLink(lineBuffer, matrix, relayPos, blockPos,
                            1.0f, 0.6f, 0.1f, 1.0f, 0.8f);
                    case TERMINAL -> renderBlockLink(lineBuffer, matrix, relayPos, blockPos,
                            0.1f, 0.8f, 0.9f, 1.0f, 0.8f);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * Dessine une ligne et un outline vers un bloc du reseau.
     */
    private void renderBlockLink(VertexConsumer buffer, Matrix4f matrix,
                                  BlockPos origin, BlockPos target,
                                  float r, float g, float b, float lineAlpha, float outlineAlpha) {
        float dx = target.getX() - origin.getX();
        float dy = target.getY() - origin.getY();
        float dz = target.getZ() - origin.getZ();

        DebugRenderHelper.drawLine(buffer, matrix,
                0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f,
                r, g, b, lineAlpha);

        DebugRenderHelper.drawCubeOutline(buffer, matrix,
                dx - 0.01f, dy - 0.01f, dz - 0.01f,
                dx + 1.01f, dy + 1.01f, dz + 1.01f,
                r, g, b, outlineAlpha);
    }

    /**
     * Trouve le controller du reseau via BFS depuis ce relay.
     */
    private StorageControllerBlockEntity findController(StorageRelayBlockEntity relay) {
        if (relay.getNodeLevel() == null) return null;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(relay.getNodePos());
        for (BlockPos connected : relay.getConnectedNodes()) {
            queue.add(connected);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;
            BlockEntity be = relay.getNodeLevel().getBlockEntity(pos);
            if (be instanceof StorageControllerBlockEntity controller) {
                return controller;
            }
            if (be instanceof INetworkNode node) {
                for (BlockPos neighbor : node.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean shouldRenderOffScreen(StorageRelayBlockEntity blockEntity) {
        return blockEntity.isEditMode();
    }

    @Override
    public AABB getRenderBoundingBox(StorageRelayBlockEntity blockEntity) {
        if (blockEntity.isEditMode()) {
            return new AABB(blockEntity.getBlockPos()).inflate(StorageRelayBlockEntity.MAX_RANGE);
        }
        return new AABB(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
