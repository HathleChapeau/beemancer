/**
 * ============================================================
 * [IncubatorRenderer.java]
 * Description: Renderer pour l'item flottant au centre de l'Incubator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | IncubatorBlockEntity    | Donnees item         | getItem(0)                     |
 * | MagicBeeItem            | Detection abeille    | Rendu specifique BEWLR         |
 * | FloatingItemHelper      | Rendu item flottant  | renderFloatingItem()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FloatingItemHelper;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
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
 * Renderer pour l'Incubator.
 * Affiche l'item au centre du bloc avec bobbing et rotation.
 * Rendu specifique pour les abeilles (MagicBeeItem) qui utilisent un BEWLR
 * avec des transforms internes a compenser.
 */
public class IncubatorRenderer implements BlockEntityRenderer<IncubatorBlockEntity> {

    private final ItemRenderer itemRenderer;

    public IncubatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(IncubatorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) return;

        ItemStack displayItem = blockEntity.getItemHandler().getStackInSlot(0);
        if (displayItem.isEmpty()) return;

        if (displayItem.getItem() instanceof MagicBeeItem) {
            renderBeeItem(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, displayItem);
        } else {
            FloatingItemHelper.renderFloatingItem(itemRenderer, displayItem, blockEntity.getLevel(),
                partialTick, poseStack, buffer, packedLight, packedOverlay,
                0.5, 0.5, 0.5, 0.4f, 0.03f, 1.5f);
        }
    }

    private void renderBeeItem(IncubatorBlockEntity blockEntity, float partialTick,
                                PoseStack poseStack, MultiBufferSource buffer,
                                int packedLight, int packedOverlay, ItemStack displayItem) {
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.1) * 0.03f;

        poseStack.pushPose();
        poseStack.translate(1.05f, 2.37f + bob, 1.05f);
        poseStack.scale(1.1f, 1.1f, 1.1f);
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));

        itemRenderer.renderStatic(displayItem, ItemDisplayContext.FIXED, packedLight, packedOverlay,
            poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }
}
