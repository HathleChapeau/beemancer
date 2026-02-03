/**
 * ============================================================
 * [CodexItemRenderer.java]
 * Description: BEWLR pour afficher une texture differente du Codex en main vs en inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Beemancer           | MOD_ID               | ResourceLocation modeles       |
 * | ClientSetup         | Enregistrement       | registerAdditionalModels       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer via IClientItemExtensions
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.item;

import com.chapeau.beemancer.Beemancer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CodexItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final ModelResourceLocation HAND_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "item/codex_hand")
    );
    public static final ModelResourceLocation GUI_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "item/codex_gui")
    );

    public CodexItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel model;

        if (displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED) {
            model = mc.getModelManager().getModel(GUI_MODEL);
        } else {
            model = mc.getModelManager().getModel(HAND_MODEL);
        }

        mc.getItemRenderer().render(stack, displayContext, false, poseStack, buffer,
                packedLight, packedOverlay, model);
    }
}
