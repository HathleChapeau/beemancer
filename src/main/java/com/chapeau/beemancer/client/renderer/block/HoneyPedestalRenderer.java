/**
 * ============================================================
 * [HoneyPedestalRenderer.java]
 * Description: Renderer pour afficher l'item sur le pedestal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | HoneyPedestalBlockEntity    | Donnees a rendre     | getStoredItem()       |
 * | ItemRenderer                | Rendu item           | Affichage 3D          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer pour le Honey Pedestal.
 * Affiche l'item stocke flottant et tournant au-dessus du pedestal.
 */
public class HoneyPedestalRenderer implements BlockEntityRenderer<HoneyPedestalBlockEntity> {

    private final ItemRenderer itemRenderer;

    public HoneyPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(HoneyPedestalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack storedItem = blockEntity.getStoredItem();
        if (storedItem.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position au-dessus du pedestal (le pedestal fait 1 bloc de haut)
        poseStack.translate(0.5, 1.25, 0.5);

        // Rotation lente
        float time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));

        // Legere oscillation verticale (bob effect)
        float bob = (float) Math.sin(time * 0.1) * 0.05f;
        poseStack.translate(0, bob, 0);

        // Echelle de l'item
        float scale = 0.5f;
        poseStack.scale(scale, scale, scale);

        // Rendre l'item
        itemRenderer.renderStatic(
            storedItem,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            blockEntity.getLevel(),
            0
        );

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HoneyPedestalBlockEntity blockEntity) {
        return true; // L'item flotte au-dessus
    }
}
