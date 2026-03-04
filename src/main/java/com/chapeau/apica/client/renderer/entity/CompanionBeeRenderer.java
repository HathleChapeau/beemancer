/**
 * ============================================================
 * [CompanionBeeRenderer.java]
 * Description: Renderer pour CompanionBeeEntity avec scale reduit, item porte et coffre backpack
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeRenderer         | Renderer vanilla     | Base du rendu                  |
 * | CompanionBeeEntity  | Entite compagnon     | Donnees de rendu               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renderer pour les abeilles compagnon.
 * Scale 0.4f (mini-abeille).
 * Type MAGNET: rend l'item porte sous le ventre.
 * Type BACKPACK: rend un coffre en permanence sous le ventre.
 */
public class CompanionBeeRenderer extends BeeRenderer {

    private static final float COMPANION_BEE_SCALE = 0.4f;
    private static final ItemStack CHEST_ICON = new ItemStack(Items.CHEST);

    private final ItemRenderer itemRenderer;

    public CompanionBeeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    protected void scale(Bee bee, PoseStack poseStack, float partialTick) {
        poseStack.scale(COMPANION_BEE_SCALE, COMPANION_BEE_SCALE, COMPANION_BEE_SCALE);
    }

    @Override
    public void render(Bee bee, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(bee, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (bee instanceof CompanionBeeEntity companion) {
            if (companion.getCompanionType() == CompanionBeeEntity.CompanionType.BACKPACK) {
                renderChest(poseStack, bufferSource, packedLight, entityYaw);
            } else {
                ItemStack carried = companion.getCarriedItem();
                if (!carried.isEmpty()) {
                    renderCarriedItem(carried, poseStack, bufferSource, packedLight, entityYaw);
                }
            }
        }
    }

    /** Rend un item porte sous le ventre (abeille magnet). */
    private void renderCarriedItem(ItemStack stack, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, float entityYaw) {
        poseStack.pushPose();
        poseStack.translate(0.0, -0.15, 0.0);
        poseStack.scale(0.3f, 0.3f, 0.3f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }

    /** Rend un coffre sous le ventre (abeille backpack). */
    private void renderChest(PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, float entityYaw) {
        poseStack.pushPose();
        poseStack.translate(0.0, -0.25, 0.0);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        itemRenderer.renderStatic(CHEST_ICON, ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }
}
