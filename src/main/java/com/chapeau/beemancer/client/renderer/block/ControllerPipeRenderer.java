/**
 * ============================================================
 * [ControllerPipeRenderer.java]
 * Description: Renderer pour le conduit formé du multibloc Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                  | Utilisation                    |
 * |--------------------------------|------------------------|---------------------------------|
 * | ControllerPipeBlockEntity     | Données formed          | rotation, spread               |
 * | BlockEntityRenderer           | Interface renderer      | Rendu custom                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.ControllerPipeBlock;
import com.chapeau.beemancer.common.blockentity.storage.ControllerPipeBlockEntity;
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
 * Rend le modèle de coude (elbow) quand le controller pipe est formé.
 *
 * formedRotation (0-7):
 * - 0-3: coude vertical vers le bas, Y rotation 0/90/180/270
 * - 4-7: coude vertical vers le haut (x=180 flip), Y rotation 0/90/180/270
 */
public class ControllerPipeRenderer implements BlockEntityRenderer<ControllerPipeBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation FORMED_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/storage/controller_pipe_formed"));

    public ControllerPipeRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(ControllerPipeBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(ControllerPipeBlock.FORMED)) {
            return;
        }

        int rotation = state.getValue(ControllerPipeBlock.FORMED_ROTATION);
        int yRotation = rotation & 3;
        boolean flipped = rotation >= 4;

        BakedModel formedModel = Minecraft.getInstance().getModelManager()
            .getModel(FORMED_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();

        // Appliquer le spread (décalage en coordonnées monde)
        float spreadX = blockEntity.getFormedSpreadX();
        float spreadZ = blockEntity.getFormedSpreadZ();
        if (spreadX != 0.0f || spreadZ != 0.0f) {
            poseStack.translate(spreadX, 0, spreadZ);
        }

        // Appliquer la rotation au centre du bloc
        poseStack.translate(0.5, 0.5, 0.5);
        if (flipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        if (yRotation != 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation * 90.0f));
        }
        poseStack.translate(-0.5, -0.5, -0.5);

        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            formedModel,
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
}
