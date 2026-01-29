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
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
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

        float time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTick;
        float bob = (float) Math.sin(time * 0.1) * 0.05f;

        boolean isBee = storedItem.getItem() instanceof MagicBeeItem;

        if (isBee) {
            // Abeilles: le BEWLR ajoute ses propres transforms (flip, translate 0.5)
            // On compense pour centrer le modèle 3D au-dessus du pedestal
            poseStack.translate(0.5, 1.15 + bob, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));
            poseStack.scale(0.6f, 0.6f, 0.6f);
            // Compenser le translate(0.5, 0.5, 0.5) du BEWLR FIXED
            poseStack.translate(-0.5, -0.5, -0.5);
        } else {
            // Items normaux
            poseStack.translate(0.5, 1.25 + bob, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));
            poseStack.scale(0.5f, 0.5f, 0.5f);
        }

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
