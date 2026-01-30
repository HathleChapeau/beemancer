/**
 * ============================================================
 * [StorageTerminalRenderer.java]
 * Description: Renderer pour le Storage Terminal (portal plane dynamique)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageTerminalBlockEntity      | BlockEntity            | Données de rendu      |
 * | StorageTerminalBlock            | FORMED_ROTATION        | Orientation           |
 * | StorageControllerBlockEntity    | Controller lié         | État honey depleted   |
 * | RenderType                      | Type de rendu          | Entity translucent    |
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
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Renderer pour le Storage Terminal.
 *
 * Rend le portal plane dynamiquement selon l'état du controller:
 * - Controller actif (miel disponible): texture nether_portal
 * - Controller inactif (honey depleted): texture plate grise
 * - Pas de controller lié: texture nether_portal (par défaut)
 *
 * Le portal plane est un quad 8x8 pixels centré dans le cadre du terminal,
 * positionné à z=14.5/16 (flush avec le cadre arrière).
 * La rotation est gérée via FORMED_ROTATION du blockstate (0-3 → Y 0/90/180/270).
 */
public class StorageTerminalRenderer implements BlockEntityRenderer<StorageTerminalBlockEntity> {

    private static final ResourceLocation PORTAL_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/block/nether_portal.png");
    private static final ResourceLocation OFF_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/block/logistic/storage_terminal_off.png");

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
        // Context non utilisé
    }

    @Override
    public void render(StorageTerminalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        int rotation = state.getValue(StorageTerminalBlock.FORMED_ROTATION);

        // Déterminer la texture selon l'état du controller
        boolean depleted = false;
        StorageControllerBlockEntity controller = blockEntity.getController();
        if (controller != null) {
            depleted = controller.isHoneyDepleted();
        }

        ResourceLocation texture = depleted ? OFF_TEXTURE : PORTAL_TEXTURE;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        poseStack.pushPose();

        // Appliquer la rotation Y selon formed_rotation
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 90.0f));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Rendre le quad portal à [4, 4, 14.5] → [12, 12, 14.5] (en pixels)
        renderPortalQuad(consumer, poseStack.last().pose(), packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * Rend un quad double-face pour le portal plane.
     * Position: [4/16, 4/16, 14.5/16] → [12/16, 12/16, 14.5/16]
     * UVs: [4/16, 4/16] → [12/16, 12/16] (zone centrale 8x8 de la texture 16x16)
     */
    private void renderPortalQuad(VertexConsumer consumer, Matrix4f matrix,
                                   int light, int overlay) {
        float x1 = 4.0f / 16.0f;
        float y1 = 4.0f / 16.0f;
        float x2 = 12.0f / 16.0f;
        float y2 = 12.0f / 16.0f;
        float z = 14.5f / 16.0f;

        float u1 = 4.0f / 16.0f;
        float v1 = 4.0f / 16.0f;
        float u2 = 12.0f / 16.0f;
        float v2 = 12.0f / 16.0f;

        // Face sud (normal +Z) — visible depuis l'extérieur
        consumer.addVertex(matrix, x1, y1, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y1, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1, y2, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // Face nord (normal -Z) — visible depuis le controller
        consumer.addVertex(matrix, x2, y1, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x1, y1, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x1, y2, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x2, y2, z).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
    }
}
