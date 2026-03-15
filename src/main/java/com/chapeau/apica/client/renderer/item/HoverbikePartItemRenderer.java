/**
 * ============================================================
 * [HoverbikePartItemRenderer.java]
 * Description: BEWLR pour rendre les modeles 3D des parties HoverBee dans l'inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance             | Raison                | Utilisation                    |
 * |------------------------|----------------------|--------------------------------|
 * | HoverbikePartItem      | Type de l'item       | Recuperer categorie/variant    |
 * | HoverbikePartVariants  | Registre modeles     | Obtenir factory du modele      |
 * | HoverbikePartModel     | Modele de base       | Rendu du modele                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer custom pour afficher les pieces du HoverBee en 3D dans l'inventaire,
 * en utilisant le modele de la variante correspondante.
 */
public class HoverbikePartItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Cache des modeles par cle (categorie_variantIndex). */
    private static final Map<String, HoverbikePartModel> MODEL_CACHE = new HashMap<>();

    public HoverbikePartItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        if (!(stack.getItem() instanceof HoverbikePartItem partItem)) return;

        HoverbikePart category = partItem.getCategory();
        int variantIndex = partItem.getVariantIndex();

        HoverbikePartModel model = getOrBuildModel(category, variantIndex);
        if (model == null) return;

        poseStack.pushPose();

        // Transformations selon le contexte d'affichage
        applyTransform(poseStack, displayContext, category);

        // Rendu du modele
        boolean foil = stack.hasFoil();
        VertexConsumer vc = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(model.getTextureLocation()), false, foil);

        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    private void applyTransform(PoseStack poseStack, ItemDisplayContext context, HoverbikePart category) {
        // Scale de base selon la categorie (les modeles ont des tailles differentes)
        float baseScale = getBaseScale(category);

        switch (context) {
            case GUI -> {
                poseStack.translate(0.57, 0.45, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(30));
                poseStack.mulPose(Axis.YP.rotationDegrees(225));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                poseStack.scale(baseScale * 4, baseScale * 4, baseScale * 4);
            }
            case FIXED -> {
                poseStack.translate(0.5, 0.45, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 4, baseScale * 4, baseScale * 4);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.65, 0.50, 0.4);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 4, baseScale * 4, baseScale * 4);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.35, 0.50, 0.4);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 4, baseScale * 4, baseScale * 4);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.5, 0.6, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 2, baseScale * 2, baseScale * 2);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5, 0.6, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 2, baseScale * 2, baseScale * 2);
            }
            case GROUND -> {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                poseStack.scale(baseScale * 2, baseScale * 2, baseScale * 2);
            }
            default -> {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(baseScale, baseScale, baseScale);
            }
        }
    }

    private float getBaseScale(HoverbikePart category) {
        return switch (category) {
            case SADDLE -> 0.35f;
            case WING_PROTECTOR -> 0.25f;
            case CONTROL_LEFT, CONTROL_RIGHT -> 0.4f;
        };
    }

    private static HoverbikePartModel getOrBuildModel(HoverbikePart category, int variantIndex) {
        String key = category.name() + "_" + variantIndex;
        return MODEL_CACHE.computeIfAbsent(key, k -> {
            try {
                HoverbikePartVariants.VariantEntry variant = HoverbikePartVariants.getVariant(category, variantIndex);
                if (variant == null || variant.factory() == null) return null;

                var entityModels = Minecraft.getInstance().getEntityModels();
                ModelPart root = entityModels.bakeLayer(variant.factory().layerLocation());
                return variant.factory().constructor().apply(root);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** Vide le cache des modeles (pour rechargement des ressources). */
    public static void clearCache() {
        MODEL_CACHE.clear();
    }
}
