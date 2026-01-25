/**
 * ============================================================
 * [MagicBeeItemRenderer.java]
 * Description: BEWLR pour rendre le modèle 3D de l'abeille dans l'inventaire
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeItem        | Données de l'item    | Récupérer l'espèce             |
 * | BeeModel            | Modèle vanilla       | Rendu de l'abeille             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.item;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MagicBeeItemRenderer extends BlockEntityWithoutLevelRenderer {

    // Cache des textures validées
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    // Texture vanilla fallback
    private static final ResourceLocation VANILLA_BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    private BeeModel<?> beeModel;

    public MagicBeeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    private BeeModel<?> getOrCreateModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BEE));
        }
        return beeModel;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        // Récupérer l'espèce depuis l'item
        String speciesId = getSpeciesFromStack(stack);
        ResourceLocation texture = getTextureForSpecies(speciesId);

        poseStack.pushPose();

        // Ajuster la position et l'échelle selon le contexte d'affichage
        switch (displayContext) {
            case GUI -> {
                // Dans l'inventaire/GUI
                poseStack.translate(0.5, 0.1, 0.5);
                poseStack.scale(0.9f, 0.9f, 0.9f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30));
                poseStack.mulPose(Axis.YP.rotationDegrees(45));
            }
            case FIXED -> {
                // Item frame
                poseStack.translate(0.5, 0.3, 0.5);
                poseStack.scale(0.8f, 0.8f, 0.8f);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // En main première personne
                poseStack.translate(0.5, 0.4, 0.5);
                poseStack.scale(0.6f, 0.6f, 0.6f);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                // En main troisième personne
                poseStack.translate(0.5, 0.3, 0.5);
                poseStack.scale(0.5f, 0.5f, 0.5f);
            }
            case GROUND -> {
                // Au sol
                poseStack.translate(0.5, 0.2, 0.5);
                poseStack.scale(0.5f, 0.5f, 0.5f);
            }
            default -> {
                poseStack.translate(0.5, 0.3, 0.5);
                poseStack.scale(0.6f, 0.6f, 0.6f);
            }
        }

        // Flip pour correspondre à l'orientation du modèle
        poseStack.scale(-1.0f, -1.0f, 1.0f);

        // Rendre le modèle
        BeeModel<?> model = getOrCreateModel();
        VertexConsumer vertexConsumer = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(texture), false, stack.hasFoil());

        model.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }

    private String getSpeciesFromStack(ItemStack stack) {
        if (stack.getItem() instanceof MagicBeeItem) {
            BeeGeneData geneData = MagicBeeItem.getGeneData(stack);
            Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
            if (speciesGene != null) {
                return speciesGene.getId();
            }
        }
        return "meadow"; // Default
    }

    private ResourceLocation getTextureForSpecies(String speciesId) {
        return TEXTURE_CACHE.computeIfAbsent(speciesId, id -> {
            ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath(
                    Beemancer.MOD_ID,
                    "textures/entity/bee/" + id + "_bee.png"
            );

            // Vérifier si la texture existe
            if (Minecraft.getInstance().getResourceManager().getResource(customTexture).isPresent()) {
                return customTexture;
            }

            return VANILLA_BEE_TEXTURE;
        });
    }

    /**
     * Vide le cache des textures (pour rechargement des ressources)
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }
}
