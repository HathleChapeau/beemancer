/**
 * ============================================================
 * [RepairToolStationRenderer.java]
 * Description: Renderer pour afficher l'item pose sur la Repair Tool Station
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation           |
 * |---------------------------------|----------------------|-----------------------|
 * | RepairToolStationBlockEntity    | Donnees a rendre     | getStoredItem()       |
 * | FloatingItemHelper              | Rendu item flottant  | renderFloatingItem()  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.renderer.util.FloatingItemHelper;
import com.chapeau.apica.common.blockentity.artifacts.RepairToolStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

public class RepairToolStationRenderer implements BlockEntityRenderer<RepairToolStationBlockEntity> {

    private final ItemRenderer itemRenderer;

    public RepairToolStationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(RepairToolStationBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack storedItem = be.getStoredItem();
        if (storedItem.isEmpty()) return;

        FloatingItemHelper.renderFloatingItem(itemRenderer, storedItem, be.getLevel(),
                partialTick, poseStack, buffer, packedLight, packedOverlay,
                0.5, 0.45, 0.5, 0.5f, 0.02f, 1.0f);
    }
}
