/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer pour l'animation des conduits du Honey Altar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed()            |
 * | AltarConduitAnimator    | Animation conduits   | Calcul orbite         |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.animation.AltarConduitAnimator;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
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
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est formé, rend les conduits qui orbitent autour de l'altar.
 * Utilise le modèle "formed" des conduits (barre horizontale).
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    // Chemin vers le modèle formed
    private static final ModelResourceLocation CONDUIT_FORMED_MODEL =
        new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "honey_crystal_conduit"), "facing=north,formed=true");

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Seulement rendre les conduits animés si le multibloc est formé
        if (!blockEntity.isFormed()) {
            return;
        }

        // Rendre les conduits qui orbitent autour de l'altar
        renderOrbitingConduits(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les 4 conduits qui orbitent autour de l'altar.
     * Chaque conduit utilise le modèle "formed" et pointe vers le centre.
     */
    private void renderOrbitingConduits(AltarHeartBlockEntity blockEntity, float partialTick,
                                        PoseStack poseStack, MultiBufferSource buffer,
                                        int packedLight, int packedOverlay) {

        float rotationAngle = AltarConduitAnimator.getRotationAngle(blockEntity, partialTick);

        // Récupérer le BakedModel du conduit formed directement
        BakedModel formedModel = Minecraft.getInstance().getModelManager()
            .getModel(CONDUIT_FORMED_MODEL);

        // BlockState pour les propriétés (texture, etc.) - mais on utilise le modèle formed
        BlockState conduitState = BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get().defaultBlockState()
            .setValue(HoneyCrystalConduitBlock.FORMED, true)
            .setValue(HoneyCrystalConduitBlock.FACING, Direction.NORTH);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

        // Rendre les 4 conduits en orbite
        for (int i = 0; i < AltarConduitAnimator.CONDUIT_COUNT; i++) {
            poseStack.pushPose();

            // Calculer la position orbitale du conduit
            Vec3 offset = AltarConduitAnimator.getConduitOffset(i, rotationAngle);
            poseStack.translate(offset.x, offset.y, offset.z);

            // Centrer pour la rotation de facing
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotation pour que le conduit pointe vers le centre
            float facingAngle = AltarConduitAnimator.getConduitFacingAngle(i, rotationAngle);
            poseStack.mulPose(Axis.YP.rotationDegrees(facingAngle));

            // Revenir au coin du bloc
            poseStack.translate(-0.5, -0.5, -0.5);

            // Rendre le modèle directement
            blockRenderer.getModelRenderer().tesselateBlock(
                blockEntity.getLevel(),
                formedModel,
                conduitState,
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

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        // Les conduits orbitent autour, donc dépassent du bloc contrôleur
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
