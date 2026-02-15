/**
 * ============================================================
 * [AssemblyTableRenderer.java]
 * Description: Renderer pour afficher la piece de moto sur la table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | AssemblyTableBlockEntity    | Donnees a rendre     | getStoredItem()       |
 * | FloatingItemHelper          | Rendu item flottant  | renderFloatingItem()  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement renderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FloatingItemHelper;
import com.chapeau.beemancer.common.blockentity.mount.AssemblyTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renderer pour l'Assembly Table.
 * Affiche la piece de moto stockee flottant et tournant au-dessus du slab.
 */
public class AssemblyTableRenderer implements BlockEntityRenderer<AssemblyTableBlockEntity> {

    private final ItemRenderer itemRenderer;

    public AssemblyTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(AssemblyTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack storedItem = blockEntity.getStoredItem();
        if (storedItem.isEmpty()) return;

        FloatingItemHelper.renderFloatingItem(itemRenderer, storedItem, blockEntity.getLevel(),
                partialTick, poseStack, buffer, packedLight, packedOverlay,
                0.5, 0.85, 0.5, 0.5f, 0.03f, 2.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(AssemblyTableBlockEntity blockEntity) {
        return true;
    }
}
