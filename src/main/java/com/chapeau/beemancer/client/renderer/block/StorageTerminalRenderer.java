/**
 * ============================================================
 * [StorageTerminalRenderer.java]
 * Description: Renderer pour le Storage Terminal (réservé pour animations futures)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageTerminalBlockEntity      | BlockEntity            | Données de rendu      |
 * | BlockEntityRenderer             | Interface renderer     | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
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
 * Le modèle formed est entièrement rendu par le blockstate (plaque 12x12x3 centrée).
 */
public class StorageTerminalRenderer implements BlockEntityRenderer<StorageTerminalBlockEntity> {

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
        // Context non utilisé
    }

    @Override
    public void render(StorageTerminalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // Le rendu formed est géré par le blockstate model
    }
}
