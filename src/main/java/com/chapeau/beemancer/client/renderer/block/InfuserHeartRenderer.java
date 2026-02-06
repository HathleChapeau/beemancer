/**
 * ============================================================
 * [InfuserHeartRenderer.java]
 * Description: Renderer pour les cubes centraux statiques de l'Infuser multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | InfuserHeartBlockEntity     | Donnees formation    | isFormed()                     |
 * | InfuserHeartBlock           | WORKING property     | Etat actif                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.InfuserHeartBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.InfuserHeartBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour l'Infuser Heart multibloc.
 *
 * Rend 2 cubes centraux statiques (pas de rotation):
 * - Cube 1: orientation normale
 * - Cube 2: tourne de 45 degres (fixe)
 */
public class InfuserHeartRenderer implements BlockEntityRenderer<InfuserHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/alchemy/infuser_heart_core"));

    public InfuserHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(InfuserHeartBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();

        boolean formed = state.hasProperty(InfuserHeartBlock.MULTIBLOCK)
            && state.getValue(InfuserHeartBlock.MULTIBLOCK) != MultiblockProperty.NONE;

        if (!formed) {
            return;
        }

        BakedModel coreModel = Minecraft.getInstance().getModelManager()
            .getModel(CORE_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());

        // Cube 1: position normale (pas de rotation)
        poseStack.pushPose();
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()
        );
        poseStack.popPose();

        // Cube 2: tourne de 45 degres (fixe, pas anime)
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0f));
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()
        );
        poseStack.popPose();
    }
}
