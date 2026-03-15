/**
 * ============================================================
 * [AlembicHeartRenderer.java]
 * Description: Renderer pour les parties animees de l'Alembic Heart multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                   | Raison                | Utilisation               |
 * |-----------------------------|----------------------|---------------------------|
 * | AlembicHeartBlockEntity     | Donnees etat         | isFormed(), DISTILLING    |
 * | AlembicHeartBlock           | MULTIBLOCK property  | Etat forme                |
 * | RotatingModelHelper         | Rotation modeles     | renderWithYRotation, XYZ  |
 * | AnimationTimer              | Temps animation      | Rotation continue         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.block.alchemy.AlembicHeartBlock;
import com.chapeau.apica.common.blockentity.alchemy.AlembicHeartBlockEntity;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.client.renderer.util.RotatingModelHelper;
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

/**
 * Renderer pour l'Alembic Heart multibloc.
 *
 * Quand forme, rend:
 * - 1 ring qui tourne sur l'axe Y (rotation lente)
 * - 2 cubes qui tournent sur les axes XYZ (style Crystallizer)
 *
 * Quand DISTILLING=true, la rotation est plus rapide.
 */
public class AlembicHeartRenderer implements BlockEntityRenderer<AlembicHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation RING_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/multibloc/alembic/alembic_heart_ring"));

    public static final ModelResourceLocation CUBE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/multibloc/alembic/alembic_heart_cube"));

    public AlembicHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AlembicHeartBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();

        // Ne rien rendre si non forme
        boolean formed = state.hasProperty(AlembicHeartBlock.MULTIBLOCK)
            && state.getValue(AlembicHeartBlock.MULTIBLOCK) != MultiblockProperty.NONE;

        if (!formed) {
            return;
        }

        boolean distilling = state.hasProperty(AlembicHeartBlock.DISTILLING)
            && state.getValue(AlembicHeartBlock.DISTILLING);

        BakedModel ringModel = Minecraft.getInstance().getModelManager()
            .getModel(RING_MODEL_LOC);
        BakedModel cubeModel = Minecraft.getInstance().getModelManager()
            .getModel(CUBE_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        float time = AnimationTimer.getRenderTime(partialTick);

        // Vitesse de rotation: plus rapide quand distillation active
        float ringSpeed = distilling ? 2.0f : 0.5f;
        float cubeSpeed = distilling ? 1.5f : 0.3f;

        // Ring: rotation Y autour du centre
        float ringRotation = time * ringSpeed * 20.0f; // 20 deg/sec base
        RotatingModelHelper.renderWithYRotation(blockRenderer, ringModel,
            blockEntity.getLevel(), state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, random, packedLight, packedOverlay,
            RenderType.solid(), ringRotation);

        // Cubes: rotation XYZ style Crystallizer
        float xRot1 = time * cubeSpeed * 3.0f;
        float yRot1 = time * cubeSpeed * 4.5f;
        float zRot1 = time * cubeSpeed * 2.1f;

        // Cube 1
        RotatingModelHelper.renderWithXYZRotation(blockRenderer, cubeModel,
            blockEntity.getLevel(), state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, random, packedLight, packedOverlay,
            RenderType.solid(), xRot1, yRot1, zRot1, 1.0f);

        // Cube 2: phase decalee (comme Crystallizer)
        RotatingModelHelper.renderWithXYZRotation(blockRenderer, cubeModel,
            blockEntity.getLevel(), state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, random, packedLight, packedOverlay,
            RenderType.solid(),
            xRot1 + 90, yRot1 + 60, zRot1 + 45, 1.0f);
    }
}
