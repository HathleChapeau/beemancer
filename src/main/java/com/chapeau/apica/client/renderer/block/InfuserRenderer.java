/**
 * ============================================================
 * [InfuserRenderer.java]
 * Description: Renderer pour l'item flottant au centre de l'Infuser
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InfuserBlockEntity      | Donnees item         | getInputSlot(), getOutputSlot()|
 * | FloatingItemHelper      | Rendu item flottant  | renderFloatingItem()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.renderer.util.FloatingItemHelper;
import com.chapeau.apica.common.blockentity.alchemy.InfuserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer pour l'Infuser.
 * Affiche l'item au centre du bloc avec bobbing et rotation.
 * Priorite: input slot, sinon output slot (craft termine).
 */
public class InfuserRenderer implements BlockEntityRenderer<InfuserBlockEntity> {

    private final ItemRenderer itemRenderer;

    public InfuserRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(InfuserBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) return;

        ItemStack displayItem = blockEntity.getInputSlot().getStackInSlot(0);
        if (displayItem.isEmpty()) {
            displayItem = blockEntity.getOutputSlot().getStackInSlot(0);
        }
        if (displayItem.isEmpty()) return;

        FloatingItemHelper.renderFloatingItem(itemRenderer, displayItem, blockEntity.getLevel(),
            partialTick, poseStack, buffer, packedLight, packedOverlay,
            0.5, 0.5, 0.5, 0.4f, 0.03f, 1.5f);
    }
}
