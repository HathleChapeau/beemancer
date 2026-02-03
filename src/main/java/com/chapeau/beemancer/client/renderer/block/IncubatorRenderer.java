/**
 * ============================================================
 * [IncubatorRenderer.java]
 * Description: Renderer pour l'item flottant au centre de l'Incubator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | IncubatorBlockEntity      | Donnees item         | getItem(0)                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
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
 * Affiche l'item au centre du bloc avec bobbing et rotation,
 * meme style que l'InfuserRenderer.
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

        ItemStack displayItem = blockEntity.getItem(0);
        if (displayItem.isEmpty()) return;

        poseStack.pushPose();

        float time = blockEntity.getLevel().getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.1) * 0.03f;

        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(
                displayItem,
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
}
