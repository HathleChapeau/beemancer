/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer du Honey Altar multibloc complet
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed()            |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est forme, rend un modele 3D unique
 * representant toute la structure a la place des blocs individuels.
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    // Offsets des conduits (Y+1)
    private static final int[][] CONDUIT_OFFSETS = {
        {0, 1, -1}, {0, 1, 1}, {1, 1, 0}, {-1, 1, 0},
        {-1, 1, -1}, {1, 1, -1}, {-1, 1, 1}, {1, 1, 1}
    };

    // Offsets des reservoirs (Y+2)
    private static final int[][] RESERVOIR_OFFSETS = {
        {0, 2, -1}, {0, 2, 1}, {1, 2, 0}, {-1, 2, 0}
    };

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Seulement rendre si le multibloc est forme
        if (!blockEntity.isFormed()) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos controllerPos = blockEntity.getBlockPos();

        // --- Rendre le coeur (le controleur lui-meme) ---
        poseStack.pushPose();
        renderFormattedHeart(poseStack, buffer, packedLight);
        poseStack.popPose();

        // --- Rendre les 8 conduits ---
        for (int[] offset : CONDUIT_OFFSETS) {
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            renderConduit(poseStack, buffer, packedLight, level, controllerPos.offset(offset[0], offset[1], offset[2]));
            poseStack.popPose();
        }

        // --- Rendre les 4 reservoirs ---
        for (int[] offset : RESERVOIR_OFFSETS) {
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            renderReservoir(poseStack, buffer, packedLight, level, controllerPos.offset(offset[0], offset[1], offset[2]));
            poseStack.popPose();
        }
    }

    /**
     * Rend le coeur de l'altar (forme activee).
     */
    private void renderFormattedHeart(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Coeur actif - cube dore brillant
        BlockState state = BeemancerBlocks.ALTAR_HEART.get().defaultBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            consumer,
            state,
            model,
            1.0f, 0.9f, 0.6f, // Teinte doree
            packedLight,
            OverlayTexture.NO_OVERLAY
        );
    }

    /**
     * Rend un conduit active.
     */
    private void renderConduit(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                Level level, BlockPos pos) {
        // Utiliser le modele "formed" du conduit
        BlockState state = BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get().defaultBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            consumer,
            state,
            model,
            1.0f, 0.95f, 0.7f, // Teinte cristal active
            packedLight,
            OverlayTexture.NO_OVERLAY
        );
    }

    /**
     * Rend un reservoir avec son niveau de fluide.
     */
    private void renderReservoir(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                  Level level, BlockPos pos) {
        // Recuperer le niveau de fluide du reservoir
        int fluidLevel = 0;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HoneyReservoirBlockEntity reservoir) {
            fluidLevel = reservoir.getFluidLevel();
        }

        // Utiliser le modele du reservoir
        BlockState state = BeemancerBlocks.HONEY_RESERVOIR.get().defaultBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            consumer,
            state,
            model,
            0.9f, 0.85f, 0.7f, // Teinte reservoir
            packedLight,
            OverlayTexture.NO_OVERLAY
        );

        // Si fluide present, rendre le fluide a l'interieur
        if (fluidLevel > 0) {
            renderFluidInReservoir(poseStack, buffer, packedLight, fluidLevel);
        }
    }

    /**
     * Rend le fluide a l'interieur d'un reservoir.
     */
    private void renderFluidInReservoir(PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight, int fluidLevel) {
        // Le fluide est un cube interne au reservoir
        // Dimensions: de [4,5,5] a [12, 5+level*1.5, 11]
        float minX = 4f / 16f;
        float minY = 5f / 16f;
        float minZ = 5f / 16f;
        float maxX = 12f / 16f;
        float maxY = minY + (fluidLevel * 1.5f / 16f);
        float maxZ = 11f / 16f;

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        // Couleur miel
        float r = 0.92f;
        float g = 0.61f;
        float b = 0.23f;
        float a = 0.9f;

        // Rendre un cube simple pour le fluide
        renderFluidCube(poseStack, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, packedLight);
    }

    /**
     * Rend un cube de fluide translucide.
     */
    private void renderFluidCube(PoseStack poseStack, VertexConsumer consumer,
                                  float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ,
                                  float r, float g, float b, float a, int packedLight) {
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 1, 0);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 0, 0, 1);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal, 1, 0, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        // La structure s'etend au-dela du bloc controleur
        return true;
    }

    @Override
    public int getViewDistance() {
        // Distance de rendu etendue pour les grandes structures
        return 96;
    }
}
