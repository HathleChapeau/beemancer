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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est forme, rend un modele 3D unique
 * representant toute la structure a la place des blocs individuels.
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

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
        // Context fourni par NeoForge
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

        // === FORMES DE TEST TRES VISIBLES ===

        // --- Coeur: GROS CUBE ROUGE au centre ---
        poseStack.pushPose();
        renderColoredCube(poseStack, buffer, packedLight,
            0.2f, 0.2f, 0.2f, 0.6f, 0.6f, 0.6f,  // cube de 0.2 a 0.8
            1.0f, 0.0f, 0.0f, 1.0f);  // ROUGE vif
        poseStack.popPose();

        // --- 8 Conduits: CUBES VERTS ---
        for (int[] offset : CONDUIT_OFFSETS) {
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            renderColoredCube(poseStack, buffer, packedLight,
                0.25f, 0.0f, 0.25f, 0.75f, 1.0f, 0.75f,  // pilier vertical
                0.0f, 1.0f, 0.0f, 1.0f);  // VERT vif
            poseStack.popPose();
        }

        // --- 4 Reservoirs: CUBES BLEUS ---
        for (int[] offset : RESERVOIR_OFFSETS) {
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);

            // Verifier le niveau de fluide
            int fluidLevel = 0;
            BlockEntity be = level.getBlockEntity(controllerPos.offset(offset[0], offset[1], offset[2]));
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                fluidLevel = reservoir.getFluidLevel();
            }

            // Reservoir: cube bleu
            renderColoredCube(poseStack, buffer, packedLight,
                0.1f, 0.25f, 0.25f, 0.9f, 0.75f, 0.75f,  // forme horizontale
                0.0f, 0.0f, 1.0f, 1.0f);  // BLEU vif

            // Si fluide, ajouter cube jaune/orange a l'interieur
            if (fluidLevel > 0) {
                float fluidHeight = 0.3f + (fluidLevel * 0.1f);
                renderColoredCube(poseStack, buffer, packedLight,
                    0.2f, 0.3f, 0.3f, 0.8f, 0.3f + fluidHeight, 0.7f,
                    1.0f, 0.7f, 0.0f, 0.9f);  // ORANGE (miel)
            }
            poseStack.popPose();
        }

        // --- BONUS: Grand anneau/cercle magique autour de la structure ---
        // Pour montrer clairement que le renderer fonctionne
        poseStack.pushPose();
        poseStack.translate(0, 1.5f, 0);  // Centre de la structure
        renderMagicRing(poseStack, buffer, packedLight, level.getGameTime() + partialTick);
        poseStack.popPose();
    }

    /**
     * Rend un cube colore simple (toutes les 6 faces).
     */
    private void renderColoredCube(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ,
                                    float r, float g, float b, float a) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        var pose = poseStack.last();

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
    }

    /**
     * Rend un anneau magique rotatif autour de la structure.
     */
    private void renderMagicRing(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float time) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        var pose = poseStack.last();

        float radius = 2.0f;
        int segments = 16;
        float thickness = 0.1f;
        float rotation = (time * 2f) % 360f;

        // Rotation de l'anneau
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));

        // Dessiner des petits cubes en cercle
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);

            // Couleur arc-en-ciel
            float hue = (float) i / segments;
            float r = (float) Math.abs(Math.sin(hue * Math.PI * 2));
            float g = (float) Math.abs(Math.sin((hue + 0.33f) * Math.PI * 2));
            float b = (float) Math.abs(Math.sin((hue + 0.66f) * Math.PI * 2));

            poseStack.pushPose();
            poseStack.translate(x, 0, z);

            // Petit cube
            var innerPose = poseStack.last();
            float s = thickness;

            // Face dessus
            consumer.addVertex(innerPose, -s, s, -s).setColor(r, g, b, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, 1, 0);
            consumer.addVertex(innerPose, -s, s, s).setColor(r, g, b, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, 1, 0);
            consumer.addVertex(innerPose, s, s, s).setColor(r, g, b, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, 1, 0);
            consumer.addVertex(innerPose, s, s, -s).setColor(r, g, b, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, 1, 0);

            // Face dessous
            consumer.addVertex(innerPose, -s, -s, s).setColor(r, g, b, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, -1, 0);
            consumer.addVertex(innerPose, -s, -s, -s).setColor(r, g, b, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, -1, 0);
            consumer.addVertex(innerPose, s, -s, -s).setColor(r, g, b, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, -1, 0);
            consumer.addVertex(innerPose, s, -s, s).setColor(r, g, b, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(innerPose, 0, -1, 0);

            poseStack.popPose();
        }

        poseStack.popPose();
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
