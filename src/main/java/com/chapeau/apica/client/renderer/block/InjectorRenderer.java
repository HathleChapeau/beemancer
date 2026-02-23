/**
 * ============================================================
 * [InjectorRenderer.java]
 * Description: Renderer pour afficher l'abeille et l'essence sur l'injecteur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | InjectorBlockEntity         | Donnees a rendre     | getItemHandler()      |
 * | AnimationTimer              | Temps smooth         | Bobbing + rotation    |
 * | LevelRenderer               | Lumiere correcte     | getLightColor()       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer pour l'Injecteur.
 * Affiche l'abeille flottant entre les projecteurs et l'essence a plat dans le creux.
 */
public class InjectorRenderer implements BlockEntityRenderer<InjectorBlockEntity> {

    private final ItemRenderer itemRenderer;

    public InjectorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(InjectorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack beeStack = blockEntity.getItemHandler().getStackInSlot(InjectorBlockEntity.BEE_SLOT);
        ItemStack essenceStack = blockEntity.getItemHandler().getStackInSlot(InjectorBlockEntity.ESSENCE_SLOT);

        // Lumiere echantillonnee au-dessus du bloc pour eviter l'assombrissement
        int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());

        if (!essenceStack.isEmpty()) {
            renderEssence(essenceStack, blockEntity, poseStack, buffer, light, packedOverlay);
        }

        if (!beeStack.isEmpty()) {
            renderBee(beeStack, blockEntity, partialTick, poseStack, buffer, light, packedOverlay);
        }
    }

    /**
     * Rend l'essence a plat dans le creux central de l'injecteur, 0.5 pixel au-dessus de la plaque.
     * La plaque est a y=6/16, donc l'essence est a y=6.5/16.
     */
    private void renderEssence(ItemStack essence, InjectorBlockEntity blockEntity,
                                PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 6.5 / 16.0, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.scale(0.35f, 0.35f, 0.35f);

        itemRenderer.renderStatic(essence, ItemDisplayContext.FIXED, light, overlay,
                poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    /**
     * Rend l'abeille flottant entre les deux projecteurs avec bobbing et rotation.
     * Compense les transforms du BEWLR de MagicBeeItem (meme pattern que ResonatorRenderer).
     */
    private void renderBee(ItemStack bee, InjectorBlockEntity blockEntity, float partialTick,
                           PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        float time = AnimationTimer.getRenderTime(partialTick);
        float bob = (float) Math.sin(time * 0.1) * 0.03f;

        poseStack.pushPose();
        poseStack.translate(0.8f, 1.6f + bob, 0.8f);
        poseStack.scale(0.35f, 0.35f, 0.35f);
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));

        itemRenderer.renderStatic(bee, ItemDisplayContext.FIXED, light, overlay,
                poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(InjectorBlockEntity blockEntity) {
        return true;
    }
}
