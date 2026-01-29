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
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
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
 * - 2 cubes à x=2 et x=14, le tout tourne sur X/Y/Z à des rythmes différents
 * - Chaque cube oscille individuellement avec sin/cos
 */
public class StorageControllerRenderer implements BlockEntityRenderer<StorageControllerBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CUBE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/storage/storage_controller_cube"));

    public StorageControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(StorageControllerBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        boolean formed = state.hasProperty(StorageControllerBlock.FORMED) &&
                         state.getValue(StorageControllerBlock.FORMED);

        if (formed) {
            renderFormedAnimation(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (blockEntity.isEditMode()) {
            renderEditMode(blockEntity, poseStack, bufferSource);
        }
    }

    /**
     * Rend l'animation du controller formé.
     * Tout le renderer tourne sur X, Y, Z à des rythmes différents.
     * Chaque cube oscille individuellement avec sin/cos.
     */
    private void renderFormedAnimation(StorageControllerBlockEntity blockEntity, float partialTick,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = (gameTime + partialTick);

        BakedModel cubeModel = Minecraft.getInstance().getModelManager()
            .getModel(CUBE_MODEL_LOC);

        BlockState state = blockEntity.getBlockState();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        // === Rotation globale (tout le renderer tourne) ===
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 1.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.7f));
        poseStack.translate(-0.5, -0.5, -0.5);

        // === Cube 1 (position x=2/16) avec oscillation individuelle ===
        poseStack.pushPose();
        float osc1X = (float) Math.sin(time * 0.05) * 0.05f;
        float osc1Y = (float) Math.cos(time * 0.07) * 0.05f;
        // Décaler vers x=2/16 depuis le centre du modèle (7/16)
        poseStack.translate((2.0 / 16.0) - (7.0 / 16.0) + osc1X, osc1Y, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            cubeModel,
            state,
            blockEntity.getBlockPos(),
            poseStack,
            vertexConsumer,
            false,
            random,
            packedLight,
            packedOverlay,
            ModelData.EMPTY,
            RenderType.solid()
        );
        poseStack.popPose();

        // === Cube 2 (position x=14/16) avec oscillation individuelle ===
        poseStack.pushPose();
        float osc2X = (float) Math.sin(time * 0.06) * 0.05f;
        float osc2Y = (float) Math.cos(time * 0.04) * 0.05f;
        // Décaler vers x=14/16 depuis le centre du modèle (7/16)
        poseStack.translate((14.0 / 16.0) - (7.0 / 16.0) + osc2X, osc2Y, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            cubeModel,
            state,
            blockEntity.getBlockPos(),
            poseStack,
            vertexConsumer,
            false,
            random,
            packedLight,
            packedOverlay,
            ModelData.EMPTY,
            RenderType.solid()
        );
        poseStack.popPose();

        poseStack.popPose(); // Fin rotation globale
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

        BlockPos controllerPos = blockEntity.getBlockPos();
        Set<BlockPos> chests = blockEntity.getRegisteredChests();

        for (BlockPos chestPos : chests) {
            renderLineToChest(lineBuffer, matrix, controllerPos, chestPos);
        }

        poseStack.popPose();
    }

    /**
     * Dessine un outline rouge autour du controller.
     */
    private void renderControllerOutline(VertexConsumer buffer, Matrix4f matrix) {
        float r = 1.0f, g = 0.2f, b = 0.2f, a = 1.0f;
        float min = -0.01f;
        float max = 1.01f;

        drawLine(buffer, matrix, min, min, min, max, min, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, min, max, min, max, r, g, b, a);
        drawLine(buffer, matrix, max, min, max, min, min, max, r, g, b, a);
        drawLine(buffer, matrix, min, min, max, min, min, min, r, g, b, a);

        drawLine(buffer, matrix, min, max, min, max, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, max, min, max, max, max, r, g, b, a);
        drawLine(buffer, matrix, max, max, max, min, max, max, r, g, b, a);
        drawLine(buffer, matrix, min, max, max, min, max, min, r, g, b, a);

        drawLine(buffer, matrix, min, min, min, min, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, min, max, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, max, max, max, max, r, g, b, a);
        drawLine(buffer, matrix, min, min, max, min, max, max, r, g, b, a);
    }

    /**
     * Dessine une ligne verte du controller vers un coffre et un outline bleu autour du coffre.
     */
    private void renderLineToChest(VertexConsumer buffer, Matrix4f matrix,
                                    BlockPos controllerPos, BlockPos chestPos) {
        float dx = chestPos.getX() - controllerPos.getX();
        float dy = chestPos.getY() - controllerPos.getY();
        float dz = chestPos.getZ() - controllerPos.getZ();

        drawLine(buffer, matrix, 0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f, 0.2f, 1.0f, 0.2f, 1.0f);

        float r = 0.2f, g = 0.6f, b = 1.0f, a = 1.0f;
        float min = -0.02f;
        float max = 1.02f;

        drawLine(buffer, matrix, dx + min, dy + min, dz + min, dx + max, dy + min, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + min, dx + max, dy + min, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + max, dx + min, dy + min, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + min, dz + max, dx + min, dy + min, dz + min, r, g, b, a);

        drawLine(buffer, matrix, dx + min, dy + max, dz + min, dx + max, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + max, dz + min, dx + max, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + max, dz + max, dx + min, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + max, dz + max, dx + min, dy + max, dz + min, r, g, b, a);

        drawLine(buffer, matrix, dx + min, dy + min, dz + min, dx + min, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + min, dx + max, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + max, dx + max, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + min, dz + max, dx + min, dy + max, dz + max, r, g, b, a);
    }

    /**
     * Utilitaire pour dessiner une ligne.
     */
    private void drawLine(VertexConsumer buffer, Matrix4f matrix,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        buffer.addVertex(matrix, x1, y1, z1)
              .setColor(r, g, b, a)
              .setNormal(dx, dy, dz);
        buffer.addVertex(matrix, x2, y2, z2)
              .setColor(r, g, b, a)
              .setNormal(dx, dy, dz);
    }

    @Override
    public boolean shouldRenderOffScreen(StorageControllerBlockEntity blockEntity) {
        return blockEntity.isEditMode() || blockEntity.isFormed();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
