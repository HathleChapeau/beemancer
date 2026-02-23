/**
 * ============================================================
 * [ResonatorRenderer.java]
 * Description: Renderer pour afficher l'abeille posee sur le resonateur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | ResonatorBlockEntity        | Donnees a rendre     | getStoredBee()        |
 * | AnimationTimer              | Temps smooth         | Bobbing + rotation    |
 * | MagicBeeItem                | Detection abeille    | Rendu 3D via BEWLR    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.block.resonator.ResonatorBlockEntity;
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
 * Renderer pour le Resonateur.
 * Affiche l'abeille stockee flottant et tournant au-dessus du bloc.
 * Meme pattern que HoneyPedestalRenderer.
 */
public class ResonatorRenderer implements BlockEntityRenderer<ResonatorBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ResonatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(ResonatorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack storedBee = blockEntity.getStoredBee();
        if (storedBee.isEmpty()) return;

        float time = AnimationTimer.getRenderTime(partialTick);
        float bob = (float) Math.sin(time * 0.1) * 0.04f;

        // Lumiere echantillonnee au-dessus du bloc pour eviter l'assombrissement
        int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());

        poseStack.pushPose();
        poseStack.translate(0.8f, 1.4f + bob, 0.8f);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));

        itemRenderer.renderStatic(storedBee, ItemDisplayContext.FIXED, light, packedOverlay,
                poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ResonatorBlockEntity blockEntity) {
        return true;
    }
}
