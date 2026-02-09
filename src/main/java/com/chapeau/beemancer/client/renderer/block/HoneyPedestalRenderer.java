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
 * | FloatingItemHelper          | Rendu item flottant  | renderFloatingItem()  |
 * | MagicBeeItem                | Detection abeille    | Rendu specifique      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FloatingItemHelper;
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
        if (storedItem.isEmpty()) return;

        if (storedItem.getItem() instanceof MagicBeeItem) {
            renderBeeItem(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, storedItem);
        } else {
            FloatingItemHelper.renderFloatingItem(itemRenderer, storedItem, blockEntity.getLevel(),
                partialTick, poseStack, buffer, packedLight, packedOverlay,
                0.5, 1.25, 0.5, 0.5f, 0.05f, 2.0f);
        }
    }

    private void renderBeeItem(HoneyPedestalBlockEntity blockEntity, float partialTick,
                                PoseStack poseStack, MultiBufferSource buffer,
                                int packedLight, int packedOverlay, ItemStack storedItem) {
        float time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTick;
        float bob = (float) Math.sin(time * 0.1) * 0.05f;

        poseStack.pushPose();
        poseStack.translate(0.8f, 2.2f + bob, 0.8f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));

        itemRenderer.renderStatic(storedItem, ItemDisplayContext.FIXED, packedLight, packedOverlay,
            poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HoneyPedestalBlockEntity blockEntity) {
        return true;
    }
}
