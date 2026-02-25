/**
 * ============================================================
 * [PipeExtractRenderer.java]
 * Description: BER pour l'indicateur d'extraction sur les pipes item et liquide
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ItemPipeBlockEntity     | Source extract dirs  | isExtracting(Direction)        |
 * | LiquidPipeBlockEntity   | Source extract dirs  | isExtracting(Direction)        |
 * | ModelResourceLocation   | Modele standalone    | item/liquid _side_extract      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement BER pour les 8 types de pipes)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.LiquidPipeBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour l'indicateur d'extraction des pipes.
 * Rend le modele _side_extract pour chaque direction où le pipe est en mode extraction.
 * Paramétré sur le type de modele (item vs liquide) via le constructeur.
 */
public class PipeExtractRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public static final ModelResourceLocation ITEM_EXTRACT_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/pipes/item_pipe_side_extract"));

    public static final ModelResourceLocation LIQUID_EXTRACT_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/pipes/liquid_pipe_side_extract"));

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();
    private final ModelResourceLocation modelLoc;

    public PipeExtractRenderer(BlockEntityRendererProvider.Context ctx, ModelResourceLocation modelLoc) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        this.modelLoc = modelLoc;
    }

    @Override
    public void render(T be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLoc);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        for (Direction dir : Direction.values()) {
            if (!isExtracting(be, dir)) continue;
            renderExtractArm(level, be, model, dir, poseStack, consumer, packedLight, packedOverlay);
        }
    }

    private void renderExtractArm(Level level, T be, BakedModel model, Direction dir,
                                   PoseStack poseStack, VertexConsumer consumer,
                                   int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotations inversées par rapport au blockstate JSON (convention PoseStack)
        switch (dir) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-270));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(-270));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            default    -> {} // NORTH: orientation par défaut
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        blockRenderer.getModelRenderer().tesselateBlock(
            level, model, be.getBlockState(), be.getBlockPos(),
            poseStack, consumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );

        poseStack.popPose();
    }

    private boolean isExtracting(T be, Direction dir) {
        if (be instanceof ItemPipeBlockEntity pipe) return pipe.isExtracting(dir);
        if (be instanceof LiquidPipeBlockEntity pipe) return pipe.isExtracting(dir);
        return false;
    }
}
