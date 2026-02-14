/**
 * ============================================================
 * [StorageControllerRenderer.java]
 * Description: Renderer pour le Storage Controller (coeur anime + mode edition)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                  | Utilisation                    |
 * |---------------------------------|------------------------|--------------------------------|
 * | StorageControllerBlockEntity    | BlockEntity            | Donnees de rendu               |
 * | StorageControllerAnimator       | Animation coeur        | Tick, apply rotations          |
 * | AnimationController             | Apply animations       | tick(), applyAnimation()       |
 * | DebugRenderHelper               | Rendu lignes/outlines  | drawLine/CubeOutline           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement du renderer + modele additionnel)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.animation.AnimationController;
import com.chapeau.beemancer.client.animation.StorageControllerAnimator;
import com.chapeau.beemancer.client.renderer.util.DebugRenderHelper;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.util.StorageHelper;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import com.chapeau.beemancer.common.blockentity.storage.StorageNetworkRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renderer pour le Storage Controller.
 *
 * Quand forme: rend le coeur (core) avec rotation animee par quarts de tour.
 * Le coeur ne tourne que si le niveau de miel est au dessus de 0.
 *
 * Mode edition:
 * - Outline rouge autour du controller
 * - Lignes vertes vers chaque coffre enregistre
 * - Outlines bleus autour des coffres enregistres
 */
public class StorageControllerRenderer implements BlockEntityRenderer<StorageControllerBlockEntity> {

    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/multibloc/storage/storage_controller_core"));

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public StorageControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(StorageControllerBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (blockEntity.isFormed() && blockEntity.getLevel() != null) {
            renderAnimatedCore(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            StorageControllerAnimator.remove(blockEntity.getBlockPos());
        }

        if (blockEntity.isEditMode()) {
            renderEditMode(blockEntity, poseStack, bufferSource);
        }
    }

    /**
     * Rend le coeur anime du controller forme.
     * Rotation par quarts de tour avec ease-in, axe cyclique X->Y->Z.
     * Ne tourne que si honey > 0.
     */
    private void renderAnimatedCore(StorageControllerBlockEntity blockEntity, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        BlockPos pos = blockEntity.getBlockPos();
        float currentTime = blockEntity.getLevel().getGameTime() + partialTick;
        boolean shouldAnimate = blockEntity.getHoneyStored() > 0;

        StorageControllerAnimator.tick(pos, currentTime, shouldAnimate);
        AnimationController ctrl = StorageControllerAnimator.getController(pos);

        BlockState controllerState = BeemancerBlocks.STORAGE_CONTROLLER.get().defaultBlockState();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        ctrl.applyAnimation("heart_x", poseStack);
        ctrl.applyAnimation("heart_y", poseStack);
        ctrl.applyAnimation("heart_z", poseStack);

        BakedModel coreModel = Minecraft.getInstance().getModelManager().getModel(CORE_MODEL_LOC);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, controllerState, pos,
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout());
        poseStack.popPose();
    }

    /**
     * Rend le mode edition (outlines et lignes debug).
     */
    private void renderEditMode(StorageControllerBlockEntity blockEntity,
                                 PoseStack poseStack, MultiBufferSource bufferSource) {

        var player = Minecraft.getInstance().player;
        if (player == null || !player.getUUID().equals(blockEntity.getEditingPlayer())) {
            return;
        }

        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();

        renderControllerOutline(lineBuffer, matrix);

        DebugRenderHelper.drawSphereOutline(lineBuffer, matrix,
            0.5f, 0.5f, 0.5f, StorageControllerBlockEntity.MAX_RANGE, 48,
            1.0f, 0.8f, 0.2f, 0.4f);

        BlockPos controllerPos = blockEntity.getBlockPos();

        for (BlockPos nodePos : blockEntity.getConnectedNodes()) {
            renderBlockLink(lineBuffer, matrix, controllerPos, nodePos,
                    0.8f, 0.2f, 1.0f, 1.0f, 0.8f);
        }

        Level level = blockEntity.getLevel();
        StorageNetworkRegistry registry = blockEntity.getNetworkRegistry();
        Set<BlockPos> renderedChestHalves = new HashSet<>();
        for (Map.Entry<BlockPos, StorageNetworkRegistry.NetworkEntry> entry : registry.getAll().entrySet()) {
            if (!entry.getValue().ownerNode().equals(controllerPos)) continue;

            BlockPos blockPos = entry.getKey();
            switch (entry.getValue().type()) {
                case CHEST -> {
                    if (renderedChestHalves.contains(blockPos)) continue;
                    BlockPos otherHalf = (level != null)
                        ? StorageHelper.getDoubleChestOtherHalf(level, blockPos) : null;
                    if (otherHalf != null) renderedChestHalves.add(otherHalf);
                    renderLineToChest(lineBuffer, matrix, controllerPos, blockPos, level);
                }
                case INTERFACE -> renderBlockLink(lineBuffer, matrix, controllerPos, blockPos,
                        1.0f, 0.6f, 0.1f, 1.0f, 0.8f);
                case TERMINAL -> renderBlockLink(lineBuffer, matrix, controllerPos, blockPos,
                        0.1f, 0.8f, 0.9f, 1.0f, 0.8f);
                case HIVE -> renderBlockLink(lineBuffer, matrix, controllerPos, blockPos,
                        1.0f, 0.9f, 0.2f, 1.0f, 0.8f);
            }
        }

        for (BlockPos takenPos : blockEntity.getTakenChestPositions()) {
            float dx = takenPos.getX() - controllerPos.getX();
            float dy = takenPos.getY() - controllerPos.getY();
            float dz = takenPos.getZ() - controllerPos.getZ();
            DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
                dx - 0.04f, dy - 0.04f, dz - 0.04f,
                dx + 1.04f, dy + 1.04f, dz + 1.04f,
                0.0f, 0.9f, 0.0f, 1.0f);
            DebugRenderHelper.drawCubeOutline(lineBuffer, matrix,
                dx - 0.06f, dy - 0.06f, dz - 0.06f,
                dx + 1.06f, dy + 1.06f, dz + 1.06f,
                0.0f, 0.7f, 0.0f, 0.8f);
        }

        poseStack.popPose();
    }

    /**
     * Dessine un outline rouge autour du controller.
     */
    private void renderControllerOutline(VertexConsumer buffer, Matrix4f matrix) {
        DebugRenderHelper.drawCubeOutline(buffer, matrix, -0.01f, -0.01f, -0.01f, 1.01f, 1.01f, 1.01f,
                1.0f, 0.2f, 0.2f, 1.0f);
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
     * Dessine une ligne verte du controller vers un coffre et un outline bleu autour du coffre.
     * Pour les doubles chests, dessine un seul grand outline couvrant les 2 blocs.
     */
    private void renderLineToChest(VertexConsumer buffer, Matrix4f matrix,
                                    BlockPos controllerPos, BlockPos chestPos, Level level) {
        float dx = chestPos.getX() - controllerPos.getX();
        float dy = chestPos.getY() - controllerPos.getY();
        float dz = chestPos.getZ() - controllerPos.getZ();

        float margin = 0.02f;
        float minX = dx - margin;
        float minY = dy - margin;
        float minZ = dz - margin;
        float maxX = dx + 1 + margin;
        float maxY = dy + 1 + margin;
        float maxZ = dz + 1 + margin;

        BlockPos otherHalf = StorageHelper.getDoubleChestOtherHalf(level, chestPos);
        if (otherHalf != null) {
            float ox = otherHalf.getX() - controllerPos.getX();
            float oy = otherHalf.getY() - controllerPos.getY();
            float oz = otherHalf.getZ() - controllerPos.getZ();
            minX = Math.min(minX, ox - margin);
            minY = Math.min(minY, oy - margin);
            minZ = Math.min(minZ, oz - margin);
            maxX = Math.max(maxX, ox + 1 + margin);
            maxY = Math.max(maxY, oy + 1 + margin);
            maxZ = Math.max(maxZ, oz + 1 + margin);
        }

        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;
        float centerZ = (minZ + maxZ) / 2f;
        DebugRenderHelper.drawLine(buffer, matrix, 0.5f, 0.5f, 0.5f, centerX, centerY, centerZ,
                0.2f, 1.0f, 0.2f, 1.0f);

        DebugRenderHelper.drawCubeOutline(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ,
                0.2f, 0.6f, 1.0f, 1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(StorageControllerBlockEntity blockEntity) {
        return blockEntity.isEditMode();
    }

    @Override
    public AABB getRenderBoundingBox(StorageControllerBlockEntity blockEntity) {
        if (blockEntity.isEditMode()) {
            return new AABB(blockEntity.getBlockPos()).inflate(StorageControllerBlockEntity.MAX_RANGE);
        }
        return new AABB(blockEntity.getBlockPos()).inflate(1.0);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
