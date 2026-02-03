/**
 * ============================================================
 * [InfuserRenderer.java]
 * Description: Renderer pour l'item flottant au centre de l'Infuser
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | InfuserBlockEntity        | Données item         | getInputSlot(), getOutputSlot()|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.alchemy.InfuserBlockEntity;
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
 * Renderer pour l'Infuser.
 * Affiche l'item au centre du bloc avec bobbing et rotation.
 * Priorité: input slot, sinon output slot (craft terminé).
 * Les particules sont gérées côté serveur via ParticleHelper dans InfuserBlockEntity.serverTick().
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
