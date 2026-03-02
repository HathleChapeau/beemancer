/**
 * ============================================================
 * [UncraftingTableRenderer.java]
 * Description: Renderer pour l'Uncrafting Table — item flottant au-dessus du bloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|----------------------|--------------------------------|
 * | UncraftingTableBlockEntity      | Donnees item         | getInputSlot()                 |
 * | FloatingItemHelper              | Rendu item flottant  | renderFloatingItem()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.renderer.util.FloatingItemHelper;
import com.chapeau.apica.common.blockentity.alchemy.UncraftingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer pour l'Uncrafting Table.
 * Affiche l'item d'input flottant au-dessus du bloc avec rotation lente.
 */
public class UncraftingTableRenderer implements BlockEntityRenderer<UncraftingTableBlockEntity> {

    private final ItemRenderer itemRenderer;

    public UncraftingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(UncraftingTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) return;

        ItemStack displayItem = blockEntity.getInputSlot().getStackInSlot(0);
        if (displayItem.isEmpty()) return;

        FloatingItemHelper.renderFloatingItem(itemRenderer, displayItem, blockEntity.getLevel(),
                partialTick, poseStack, buffer, packedLight, packedOverlay,
                0.5, 0.97, 0.5, 0.1f, 0.01f, 1.0f);
    }
}
