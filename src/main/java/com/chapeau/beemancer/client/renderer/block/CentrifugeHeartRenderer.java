/**
 * ============================================================
 * [CentrifugeHeartRenderer.java]
 * Description: Renderer pour les cubes centraux animés de la Centrifuge multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | CentrifugeHeartBlockEntity    | Données animation    | getRotation(), getVelocity()   |
 * | CentrifugeHeartBlock          | WORKING property     | État actif                     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.CentrifugeHeartBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
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
 * Renderer pour le Centrifuge Heart multibloc.
 *
 * Rend 2 cubes centraux qui tournent:
 * - Cube 1: rotation normale (axe Y)
 * - Cube 2: tourné de 45° par rapport au cube 1
 *
 * La vitesse de rotation (velocity) accélère quand WORKING=true et décélère quand WORKING=false.
 * L'angle de rotation est interpolé pour un rendu smooth.
 */
public class CentrifugeHeartRenderer implements BlockEntityRenderer<CentrifugeHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/alchemy/centrifuge_heart_core"));

    public CentrifugeHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(CentrifugeHeartBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();

        boolean formed = state.hasProperty(CentrifugeHeartBlock.MULTIBLOCK)
            && state.getValue(CentrifugeHeartBlock.MULTIBLOCK) != MultiblockProperty.NONE;

        if (!formed) {
            return;
        }

        BakedModel coreModel = Minecraft.getInstance().getModelManager()
            .getModel(CORE_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());

        float rotation = blockEntity.getClientRotation(partialTick);

        // Cube 1: rotation normale
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()
        );
        poseStack.popPose();

        // Cube 2: tourné de 45° par rapport au cube 1
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation + 45.0f));
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.translucent()
        );
        poseStack.popPose();
    }
}
