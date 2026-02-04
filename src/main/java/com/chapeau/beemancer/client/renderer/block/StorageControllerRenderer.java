/**
 * ============================================================
 * [StorageControllerRenderer.java]
 * Description: Renderer pour le Storage Controller (mode édition + animation formé)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | BlockEntity            | Données de rendu      |
 * | StorageControllerBlock          | FORMED property        | État formé            |
 * | RenderType                      | Type de rendu          | Lignes debug + solid  |
 * | DebugRenderHelper               | Rendu lignes/outlines  | drawLine/CubeOutline  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.DebugRenderHelper;
import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Renderer pour le Storage Controller.
 *
 * Mode édition:
 * - Outline rouge autour du controller
 * - Lignes vertes vers chaque coffre enregistré
 * - Outlines bleus autour des coffres enregistrés
 *
 * Mode formé:
 * - 2 petits cubes à x=2 et x=14, le tout tourne sur X/Y/Z à des rythmes différents
 * - 2 gros cubes 6x6x6 au centre, tournent rapidement sur eux-mêmes (même pattern XYZ)
 */
public class StorageControllerRenderer implements BlockEntityRenderer<StorageControllerBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CUBE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/storage/storage_controller_cube"));

    public static final ModelResourceLocation CUBE_BIG_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/storage/storage_controller_cube_big"));

    public StorageControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(StorageControllerBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        boolean formed = state.hasProperty(StorageControllerBlock.MULTIBLOCK) &&
                         !state.getValue(StorageControllerBlock.MULTIBLOCK).equals(MultiblockProperty.NONE);

        if (formed) {
            renderFormedAnimation(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (blockEntity.isEditMode()) {
            renderEditMode(blockEntity, poseStack, bufferSource);
        }
    }

    /**
     * Rend l'animation du controller formé.
     * Quand alimenté en miel:
     *   2 petits cubes à x=2 et x=14 tournent ensemble sur X/Y/Z.
     *   2 gros cubes 6x6x6 au centre tournent rapidement sur eux-mêmes.
     * Quand sans miel (honey depleted):
     *   Tous les cubes sont immobiles au centre, superposés.
     */
    private void renderFormedAnimation(StorageControllerBlockEntity blockEntity, float partialTick,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {

        BakedModel cubeModel = Minecraft.getInstance().getModelManager()
            .getModel(CUBE_MODEL_LOC);
        BakedModel cubeBigModel = Minecraft.getInstance().getModelManager()
            .getModel(CUBE_BIG_MODEL_LOC);

        BlockState state = blockEntity.getBlockState();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        boolean depleted = blockEntity.isHoneyDepleted();

        if (depleted) {
            // Mode éteint: tous les cubes au centre, immobiles
            // 2 gros cubes (superposés, pas de rotation)
            blockRenderer.getModelRenderer().tesselateBlock(
                blockEntity.getLevel(), cubeBigModel, state, blockEntity.getBlockPos(),
                poseStack, vertexConsumer, false, random,
                packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
            );

            // 2 petits cubes (au centre, pas de décalage)
            blockRenderer.getModelRenderer().tesselateBlock(
                blockEntity.getLevel(), cubeModel, state, blockEntity.getBlockPos(),
                poseStack, vertexConsumer, false, random,
                packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
            );
            return;
        }

        // Mode actif: animation complète
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = (gameTime + partialTick);

        // === 2 gros cubes au centre (rotation rapide sur eux-mêmes) ===
        // Cube A: même pattern XYZ, vitesse rapide
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.5f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.1f));
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), cubeBigModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );
        poseStack.popPose();

        // Cube B: même pattern XYZ, vitesse rapide, phase décalée
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.0f + 90));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.5f + 60));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.1f + 45));
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), cubeBigModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()
        );
        poseStack.popPose();

        // === Rotation globale des 2 petits cubes ===
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 1.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.7f));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Petit cube 1 (position x=2/16)
        poseStack.pushPose();
        poseStack.translate((2.0 / 16.0) - (7.0 / 16.0), 0, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), cubeModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()//.solid()
        );
        poseStack.popPose();

        // Petit cube 2 (position x=14/16)
        poseStack.pushPose();
        poseStack.translate((14.0 / 16.0) - (7.0 / 16.0), 0, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), cubeModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.endPortal()//.solid()
        );
        poseStack.popPose();

        poseStack.popPose(); // Fin rotation globale petits cubes
    }

    /**
     * Rend le mode édition (outlines et lignes debug).
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

        // Sphère de rayon d'action (centrée sur le bloc)
        DebugRenderHelper.drawSphereOutline(lineBuffer, matrix,
            0.5f, 0.5f, 0.5f, StorageControllerBlockEntity.MAX_RANGE, 48,
            1.0f, 0.8f, 0.2f, 0.4f);

        BlockPos controllerPos = blockEntity.getBlockPos();

        // Lignes vertes + outlines bleus vers TOUS les coffres du reseau
        Set<BlockPos> chests = blockEntity.getAllNetworkChests();
        for (BlockPos chestPos : chests) {
            renderLineToChest(lineBuffer, matrix, controllerPos, chestPos);
        }

        // Lignes magenta vers les noeuds connectes (relays)
        for (BlockPos nodePos : blockEntity.getConnectedNodes()) {
            renderBlockLink(lineBuffer, matrix, controllerPos, nodePos,
                    0.8f, 0.2f, 1.0f, 1.0f, 0.8f);
        }

        // Lignes orange vers les interfaces liees (import/export)
        for (BlockPos ifacePos : blockEntity.getLinkedInterfaces()) {
            renderBlockLink(lineBuffer, matrix, controllerPos, ifacePos,
                    1.0f, 0.6f, 0.1f, 1.0f, 0.8f);
        }

        // Lignes cyan vers les terminaux lies
        for (BlockPos termPos : blockEntity.getLinkedTerminals()) {
            renderBlockLink(lineBuffer, matrix, controllerPos, termPos,
                    0.1f, 0.8f, 0.9f, 1.0f, 0.8f);
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
     */
    private void renderLineToChest(VertexConsumer buffer, Matrix4f matrix,
                                    BlockPos controllerPos, BlockPos chestPos) {
        float dx = chestPos.getX() - controllerPos.getX();
        float dy = chestPos.getY() - controllerPos.getY();
        float dz = chestPos.getZ() - controllerPos.getZ();

        DebugRenderHelper.drawLine(buffer, matrix, 0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f,
                0.2f, 1.0f, 0.2f, 1.0f);

        float min = -0.02f;
        float max = 1.02f;
        DebugRenderHelper.drawCubeOutline(buffer, matrix, dx + min, dy + min, dz + min, dx + max, dy + max, dz + max,
                0.2f, 0.6f, 1.0f, 1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(StorageControllerBlockEntity blockEntity) {
        return blockEntity.isEditMode() || blockEntity.isFormed();
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
