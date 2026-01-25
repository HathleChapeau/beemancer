/**
 * ============================================================
 * [StorageControllerRenderer.java]
 * Description: Renderer pour visualiser le mode édition du Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | BlockEntity            | Données de rendu      |
 * | RenderType                      | Type de rendu          | Lignes debug          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Renderer pour le Storage Controller en mode édition.
 *
 * Affiche:
 * - Outline rouge autour du controller
 * - Lignes vertes vers chaque coffre enregistré
 * - Outlines bleus autour des coffres enregistrés
 */
public class StorageControllerRenderer implements BlockEntityRenderer<StorageControllerBlockEntity> {

    public StorageControllerRenderer(BlockEntityRendererProvider.Context context) {
        // Context non utilisé
    }

    @Override
    public void render(StorageControllerBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // Seulement afficher en mode édition
        if (!blockEntity.isEditMode()) {
            return;
        }

        // Vérifier que c'est le joueur qui édite
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getUUID().equals(blockEntity.getEditingPlayer())) {
            return;
        }

        // Obtenir le buffer une seule fois pour toutes les lignes
        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();

        // Outline rouge autour du controller
        renderControllerOutline(lineBuffer, matrix);

        // Lignes et outlines pour les coffres
        BlockPos controllerPos = blockEntity.getBlockPos();
        Set<BlockPos> chests = blockEntity.getRegisteredChests();

        for (BlockPos chestPos : chests) {
            renderLineToChest(lineBuffer, matrix, controllerPos, chestPos);
        }

        poseStack.popPose();
    }

    /**
     * Dessine un outline rouge autour du controller.
     */
    private void renderControllerOutline(VertexConsumer buffer, Matrix4f matrix) {
        float r = 1.0f, g = 0.2f, b = 0.2f, a = 1.0f;
        float min = -0.01f;
        float max = 1.01f;

        // Dessiner les 12 arêtes du cube
        // Bottom face
        drawLine(buffer, matrix, min, min, min, max, min, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, min, max, min, max, r, g, b, a);
        drawLine(buffer, matrix, max, min, max, min, min, max, r, g, b, a);
        drawLine(buffer, matrix, min, min, max, min, min, min, r, g, b, a);

        // Top face
        drawLine(buffer, matrix, min, max, min, max, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, max, min, max, max, max, r, g, b, a);
        drawLine(buffer, matrix, max, max, max, min, max, max, r, g, b, a);
        drawLine(buffer, matrix, min, max, max, min, max, min, r, g, b, a);

        // Vertical edges
        drawLine(buffer, matrix, min, min, min, min, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, min, max, max, min, r, g, b, a);
        drawLine(buffer, matrix, max, min, max, max, max, max, r, g, b, a);
        drawLine(buffer, matrix, min, min, max, min, max, max, r, g, b, a);
    }

    /**
     * Dessine une ligne verte du controller vers un coffre et un outline bleu autour du coffre.
     */
    private void renderLineToChest(VertexConsumer buffer, Matrix4f matrix,
                                    BlockPos controllerPos, BlockPos chestPos) {
        // Position relative du coffre
        float dx = chestPos.getX() - controllerPos.getX();
        float dy = chestPos.getY() - controllerPos.getY();
        float dz = chestPos.getZ() - controllerPos.getZ();

        // Ligne verte du centre du controller au centre du coffre
        drawLine(buffer, matrix, 0.5f, 0.5f, 0.5f, dx + 0.5f, dy + 0.5f, dz + 0.5f, 0.2f, 1.0f, 0.2f, 1.0f);

        // Outline bleu autour du coffre
        float r = 0.2f, g = 0.6f, b = 1.0f, a = 1.0f;
        float min = -0.02f;
        float max = 1.02f;

        // Bottom face
        drawLine(buffer, matrix, dx + min, dy + min, dz + min, dx + max, dy + min, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + min, dx + max, dy + min, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + max, dx + min, dy + min, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + min, dz + max, dx + min, dy + min, dz + min, r, g, b, a);

        // Top face
        drawLine(buffer, matrix, dx + min, dy + max, dz + min, dx + max, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + max, dz + min, dx + max, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + max, dz + max, dx + min, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + max, dz + max, dx + min, dy + max, dz + min, r, g, b, a);

        // Vertical edges
        drawLine(buffer, matrix, dx + min, dy + min, dz + min, dx + min, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + min, dx + max, dy + max, dz + min, r, g, b, a);
        drawLine(buffer, matrix, dx + max, dy + min, dz + max, dx + max, dy + max, dz + max, r, g, b, a);
        drawLine(buffer, matrix, dx + min, dy + min, dz + max, dx + min, dy + max, dz + max, r, g, b, a);
    }

    /**
     * Utilitaire pour dessiner une ligne.
     */
    private void drawLine(VertexConsumer buffer, Matrix4f matrix,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float r, float g, float b, float a) {
        // Calculer la normale (direction de la ligne normalisée)
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        buffer.addVertex(matrix, x1, y1, z1)
              .setColor(r, g, b, a)
              .setNormal(dx, dy, dz);
        buffer.addVertex(matrix, x2, y2, z2)
              .setColor(r, g, b, a)
              .setNormal(dx, dy, dz);
    }

    @Override
    public boolean shouldRenderOffScreen(StorageControllerBlockEntity blockEntity) {
        // Rendre même si le bloc est hors écran (pour les lignes longues)
        return blockEntity.isEditMode();
    }

    @Override
    public int getViewDistance() {
        // Distance de vue étendue pour le mode édition
        return 48;
    }
}
