/**
 * ============================================================
 * [StorageTerminalRenderer.java]
 * Description: Renderer pour l'animation des plaques du Storage Terminal formé
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageTerminalBlockEntity      | BlockEntity            | Données de rendu      |
 * | StorageTerminalBlock            | FORMED property        | État formé            |
 * | BlockEntityRenderer             | Interface renderer     | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.StorageTerminalBlock;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Storage Terminal en mode formé.
 * Anime les 2 plaques (middle et front) en rotation opposée sur l'axe Z.
 * La back plate reste statique (rendue par le blockstate).
 */
public class StorageTerminalRenderer implements BlockEntityRenderer<StorageTerminalBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation PLATE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/storage/storage_terminal_plate"));

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(StorageTerminalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(StorageTerminalBlock.FORMED) ||
            !state.getValue(StorageTerminalBlock.FORMED)) {
            return;
        }

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = (gameTime + partialTick);

        BakedModel plateModel = Minecraft.getInstance().getModelManager()
            .getModel(PLATE_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        int formedRotation = state.getValue(StorageTerminalBlock.FORMED_ROTATION);

        // Middle plate — tourne en sens horaire sur Z
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // Appliquer la rotation du multibloc d'abord
        applyFormedRotation(poseStack, formedRotation);
        // Animation: rotation sur Z
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.0f));
        poseStack.translate(-0.5, -0.5, -0.5);
        // Positionner en Z=13/16 (centre de la middle plate originale z=12-14)
        poseStack.translate(0, 0, 13.0 / 16.0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            plateModel,
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

        // Front plate — tourne en sens anti-horaire sur Z
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyFormedRotation(poseStack, formedRotation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 2.0f));
        poseStack.translate(-0.5, -0.5, -0.5);
        // Positionner en Z=11/16 (centre de la front plate originale z=10-12)
        poseStack.translate(0, 0, 11.0 / 16.0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            plateModel,
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
    }

    /**
     * Applique la rotation Y correspondant au formed_rotation du multibloc.
     * Doit être appelée APRÈS translate(0.5, 0.5, 0.5) pour pivoter autour du centre.
     * Le +180 compense l'inversion du terminal (même fix que le blockstate).
     */
    private void applyFormedRotation(PoseStack poseStack, int formedRotation) {
        float yRotation = (formedRotation * 90.0f) + 180.0f;
        if (yRotation != 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(StorageTerminalBlockEntity blockEntity) {
        return false;
    }
}
