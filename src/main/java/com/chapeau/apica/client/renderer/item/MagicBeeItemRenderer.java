/**
 * ============================================================
 * [MagicBeeItemRenderer.java]
 * Description: BEWLR pour rendre le modele ApicaBee 3D tinte dans l'inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeItem        | Donnees de l'item    | Recuperer l'espece             |
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass tinte         |
 * | BeeSpeciesManager   | Donnees especes      | Model types + couleurs parties |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MagicBeeItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Cache des modeles par cle de combinaison de types. */
    private static final Map<String, ApicaBeeModel<?>> MODEL_CACHE = new HashMap<>();

    public MagicBeeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        String speciesId = getSpeciesFromStack(stack);
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);

        String bodyId = data != null ? data.modelBody : "default";
        String wingId = data != null ? data.modelWing : "default";
        String stingerId = data != null ? data.modelStinger : "default";
        String antennaId = data != null ? data.modelAntenna : "default";

        BeeBodyType bodyType = resolveBodyType(bodyId);
        BeeWingType wingType = resolveWingType(wingId);
        BeeStingerType stingerType = resolveStingerType(stingerId);
        BeeAntennaType antennaType = resolveAntennaType(antennaId);

        ApicaBeeModel<?> model = getOrBuildModel(bodyType, wingType, stingerType, antennaType);
        if (model == null) return;

        int bodyColor = data != null ? data.partColorBody : 0xCC8800;
        int stripeColor = data != null ? data.partColorStripe : 0x1A1A1A;
        int wingColor = data != null ? data.partColorWing : 0xAADDFF;
        int antennaColor = data != null ? data.partColorAntenna : 0x1A1A1A;
        int stingerColor = data != null ? data.partColorStinger : 0xDDAA00;
        int eyeColor = data != null ? data.partColorEye : 0x1A1A1A;
        int pupilColor = data != null ? data.partColorPupil : 0xFFFFFF;

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(bodyType);
        ResourceLocation wingTex = ApicaBeeModel.getWingTexture(wingType);
        ResourceLocation stingerTex = ApicaBeeModel.getStingerTexture(stingerType);
        ResourceLocation antennaTex = ApicaBeeModel.getAntennaTexture(antennaType);

        poseStack.pushPose();

        // Ajuster position/echelle selon le contexte d'affichage
        switch (displayContext) {
            case GUI -> {
                poseStack.translate(0.5f, 2.2f, 0.5f);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30));
                poseStack.mulPose(Axis.YP.rotationDegrees(225));
                poseStack.scale(1.4f, 1.4f, 1.4f);
            }
            case FIXED -> {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.mulPose(Axis.YP.rotationDegrees(225));
                poseStack.scale(0.9f, 0.9f, 0.9f);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5, 2.0f, 0.5);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.mulPose(Axis.YP.rotationDegrees(-114.0f));
                poseStack.scale(1f, 1f, 1f);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5f, 0.2f, 1.2f);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-116.4f));
                poseStack.scale(0.6f, 0.6f, 0.6f);
            }
            case GROUND -> {
                poseStack.translate(0.5, 1.2f, 0.5);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.mulPose(Axis.YP.rotationDegrees(225));
                poseStack.scale(0.6f, 0.6f, 0.6f);
            }
            default -> {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(-1.0f, -1.0f, 1.0f);
                poseStack.scale(0.7f, 0.7f, 0.7f);
            }
        }

        boolean foil = stack.hasFoil();
        int overlay = OverlayTexture.NO_OVERLAY;

        // Multi-pass body render
        VertexConsumer bodyVC = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(bodyTex), false, foil);

        model.showCorpusOnly();
        model.renderToBuffer(poseStack, bodyVC, packedLight, overlay, toArgb(bodyColor));

        model.showStripeOnly();
        model.renderToBuffer(poseStack, bodyVC, packedLight, overlay, toArgb(stripeColor));

        model.showEyesOnly();
        model.renderToBuffer(poseStack, bodyVC, packedLight, overlay, toArgb(eyeColor));

        model.showPupilsOnly();
        model.renderToBuffer(poseStack, bodyVC, packedLight, overlay, toArgb(pupilColor));

        model.showUntintedOnly();
        model.renderToBuffer(poseStack, bodyVC, packedLight, overlay, toArgb(stripeColor));

        // Antenna
        VertexConsumer antennaVC = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(antennaTex), false, foil);
        model.renderAntenna(poseStack, antennaVC, packedLight, overlay, toArgb(antennaColor));

        // Wings
        VertexConsumer wingVC = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(wingTex), false, foil);
        model.renderWings(poseStack, wingVC, packedLight, overlay, toArgb(wingColor));

        // Stinger
        VertexConsumer stingerVC = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(stingerTex), false, foil);
        model.renderStinger(poseStack, stingerVC, packedLight, overlay, toArgb(stingerColor));

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
        return "meadow";
    }

    private static ApicaBeeModel<?> getOrBuildModel(BeeBodyType body, BeeWingType wing,
                                                     BeeStingerType stinger, BeeAntennaType antenna) {
        String key = body.getId() + "_" + wing.getId() + "_" + stinger.getId() + "_" + antenna.getId();
        return MODEL_CACHE.computeIfAbsent(key, k -> {
            try {
                var entityModels = Minecraft.getInstance().getEntityModels();
                ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(body));
                ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.getWingLayer(wing));
                ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.getStingerLayer(stinger));
                ModelPart antennaRoot = entityModels.bakeLayer(ApicaBeeModel.getAntennaLayer(antenna));
                return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, body);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static BeeBodyType resolveBodyType(String id) {
        for (BeeBodyType t : BeeBodyType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeBodyType.DEFAULT;
    }

    private static BeeWingType resolveWingType(String id) {
        for (BeeWingType t : BeeWingType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeWingType.DEFAULT;
    }

    private static BeeStingerType resolveStingerType(String id) {
        for (BeeStingerType t : BeeStingerType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeStingerType.DEFAULT;
    }

    private static BeeAntennaType resolveAntennaType(String id) {
        for (BeeAntennaType t : BeeAntennaType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeAntennaType.DEFAULT;
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Vide le cache des modeles (pour rechargement des ressources). */
    public static void clearCache() {
        MODEL_CACHE.clear();
    }
}
