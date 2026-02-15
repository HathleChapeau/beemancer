/**
 * ============================================================
 * [StorageTerminalRenderer.java]
 * Description: Renderer pour le Storage Terminal (vide, reserve pour futur usage)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageTerminalBlockEntity      | BlockEntity            | Donnees de rendu      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renderer pour le Storage Terminal.
 * Actuellement vide: le modele blockstate gere tout le rendu.
 * Reserve pour futur ajout de rendu dynamique.
 */
public class StorageTerminalRenderer implements BlockEntityRenderer<StorageTerminalBlockEntity> {

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StorageTerminalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // Pas de rendu additionnel: le modele blockstate suffit
    }
}
