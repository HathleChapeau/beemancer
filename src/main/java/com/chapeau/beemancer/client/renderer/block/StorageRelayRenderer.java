/**
 * ============================================================
 * [StorageRelayRenderer.java]
 * Description: Renderer pour le Storage Relay (mode edition)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                  | Utilisation           |
 * |-------------------------------|------------------------|-----------------------|
 * | StorageRelayBlockEntity      | BlockEntity            | Donnees de rendu      |
 * | DebugRenderHelper            | Rendu lignes/outlines  | drawLine/CubeOutline  |
 * | INetworkNode                 | Interface reseau       | Donnees communes      |
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

import java.util.Set;

/**
 * Renderer pour le Storage Relay.
 *
 * Mode edition:
 * - Outline jaune autour du relay
 * - Sphere de rayon d'action (jaune/orange)
 * - Lignes vertes vers chaque coffre enregistre
 * - Outlines bleus autour des coffres enregistres
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

        // Lignes et outlines vers les coffres
        Set<BlockPos> chests = blockEntity.getRegisteredChests();
        for (BlockPos chestPos : chests) {
            float dx = chestPos.getX() - relayPos.getX();
            float dy = chestPos.getY() - relayPos.getY();
            float dz = chestPos.getZ() - relayPos.getZ();

            DebugRenderHelper.drawLine(lineBuffer, matrix,
                0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f,
                0.2f, 1.0f, 0.2f, 1.0f);

            DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
                dx - 0.02f, dy - 0.02f, dz - 0.02f,
                dx + 1.02f, dy + 1.02f, dz + 1.02f,
                0.2f, 0.6f, 1.0f, 1.0f);
        }

        // Lignes magenta vers les noeuds connectes
        Set<BlockPos> nodes = blockEntity.getConnectedNodes();
        for (BlockPos nodePos : nodes) {
            float dx = nodePos.getX() - relayPos.getX();
            float dy = nodePos.getY() - relayPos.getY();
            float dz = nodePos.getZ() - relayPos.getZ();

            DebugRenderHelper.drawLine(lineBuffer, matrix,
                0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f,
                0.8f, 0.2f, 1.0f, 1.0f);

            DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
                dx - 0.01f, dy - 0.01f, dz - 0.01f,
                dx + 1.01f, dy + 1.01f, dz + 1.01f,
                0.8f, 0.2f, 1.0f, 0.8f);
        }

        poseStack.popPose();
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
