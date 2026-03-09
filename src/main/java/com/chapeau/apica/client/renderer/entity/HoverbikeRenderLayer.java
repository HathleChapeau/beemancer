/**
 * ============================================================
 * [HoverbikeRenderLayer.java]
 * Description: RenderLayer multi-pass tinte pour HoverBee via ApicaBeeModel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeRenderer   | Renderer parent      | Acces donnees espece courante  |
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass par partie    |
 * | BeeSpeciesManager   | Donnees especes      | Couleurs par partie            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Layer de rendu multi-pass pour le HoverBee.
 * Identique au MagicBeeRenderLayer mais pour HoverbikeEntity.
 * Chaque partie du modele est rendue avec sa propre couleur d'espece.
 */
public class HoverbikeRenderLayer extends RenderLayer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    private final HoverbikeRenderer parentRenderer;

    public HoverbikeRenderLayer(HoverbikeRenderer renderer) {
        super(renderer);
        this.parentRenderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        ApicaBeeModel<HoverbikeEntity> model = getParentModel();
        BeeSpeciesManager.BeeSpeciesData data = parentRenderer.currentData;

        int bodyColor = data != null ? data.partColorBody : 0xCC8800;
        int stripeColor = data != null ? data.partColorStripe : 0x1A1A1A;
        int wingColor = data != null ? data.partColorWing : 0xAADDFF;
        int antennaColor = data != null ? data.partColorAntenna : 0x1A1A1A;
        int stingerColor = data != null ? data.partColorStinger : 0xDDAA00;
        int eyeColor = data != null ? data.partColorEye : 0x1A1A1A;
        int pupilColor = data != null ? data.partColorPupil : 0xFFFFFF;

        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(parentRenderer.currentBodyType);
        ResourceLocation wingTex = ApicaBeeModel.getWingTexture(parentRenderer.currentWingType);
        ResourceLocation stingerTex = ApicaBeeModel.getStingerTexture(parentRenderer.currentStingerType);
        ResourceLocation antennaTex = ApicaBeeModel.getAntennaTexture(parentRenderer.currentAntennaType);

        // === Multi-pass body render ===
        VertexConsumer bodyVC = buffer.getBuffer(RenderType.entityCutout(bodyTex));

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

        // === Antenna ===
        VertexConsumer antennaVC = buffer.getBuffer(RenderType.entityCutout(antennaTex));
        model.renderAntenna(poseStack, antennaVC, packedLight, overlay, toArgb(antennaColor));

        // === Wings ===
        VertexConsumer wingVC = buffer.getBuffer(RenderType.entityCutout(wingTex));
        model.renderWings(poseStack, wingVC, packedLight, overlay, toArgb(wingColor));

        // === Stinger ===
        VertexConsumer stingerVC = buffer.getBuffer(RenderType.entityCutout(stingerTex));
        model.renderStinger(poseStack, stingerVC, packedLight, overlay, toArgb(stingerColor));
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
