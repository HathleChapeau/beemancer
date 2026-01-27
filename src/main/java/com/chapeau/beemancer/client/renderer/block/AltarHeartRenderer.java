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
 * | AltarConduitAnimator    | Animation conduits   | Calcul rotation       |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.animation.AltarConduitAnimator;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est formé, rend les conduits qui tournent sur eux-mêmes.
 * Utilise le modèle "formed" des conduits.
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltarHeartRenderer.class);
    private final BlockRenderDispatcher blockRenderer;
    private static int logCounter = 0;

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        LOGGER.info("[AltarHeartRenderer] Renderer créé!");
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Log toutes les 100 frames
        if (logCounter++ % 100 == 0) {
            LOGGER.info("[AltarHeartRenderer] render() - isFormed={}, pos={}",
                blockEntity.isFormed(), blockEntity.getBlockPos());
        }

        // Seulement rendre les conduits animés si le multibloc est formé
        if (!blockEntity.isFormed()) {
            return;
        }

        if (logCounter % 100 == 1) {
            LOGGER.info("[AltarHeartRenderer] Multibloc formé, rendu des conduits...");
        }

        // Rendre les conduits qui tournent sur eux-mêmes
        renderRotatingConduits(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les 4 conduits qui tournent sur eux-mêmes à leurs positions fixes.
     */
    private void renderRotatingConduits(AltarHeartBlockEntity blockEntity, float partialTick,
                                        PoseStack poseStack, MultiBufferSource buffer,
                                        int packedLight, int packedOverlay) {

        float rotationAngle = AltarConduitAnimator.getRotationAngle(blockEntity, partialTick);

        // Utiliser le BlockState NON-FORMED car FORMED a RenderShape.INVISIBLE
        // qui empêche renderSingleBlock de rendre quoi que ce soit
        BlockState conduitState = BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get().defaultBlockState()
            .setValue(HoneyCrystalConduitBlock.FORMED, false)
            .setValue(HoneyCrystalConduitBlock.FACING, Direction.NORTH);

        if (logCounter % 100 == 1) {
            LOGGER.info("[AltarHeartRenderer] conduitState={}, rotationAngle={}", conduitState, rotationAngle);
        }

        // Positions fixes des 4 conduits cardinaux relatifs au contrôleur (Y+1)
        int[][] conduitOffsets = {
            {0, 1, -1},  // Nord
            {0, 1, 1},   // Sud
            {1, 1, 0},   // Est
            {-1, 1, 0}   // Ouest
        };

        for (int[] offset : conduitOffsets) {
            poseStack.pushPose();

            if (logCounter % 100 == 1) {
                LOGGER.info("[AltarHeartRenderer] Rendu conduit offset=[{},{},{}]", offset[0], offset[1], offset[2]);
            }

            // Translater vers la position fixe du conduit
            poseStack.translate(offset[0], offset[1], offset[2]);

            // Centrer pour la rotation
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotation sur l'axe Y (tourner sur lui-même)
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));

            // Revenir au coin du bloc
            poseStack.translate(-0.5, -0.5, -0.5);

            // Rendre le modèle
            blockRenderer.renderSingleBlock(
                conduitState,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                null
            );

            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        // Les conduits sont à Y+1, donc légèrement hors du bloc contrôleur
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
