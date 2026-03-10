/**
 * ============================================================
 * [MagicBeeRenderLayer.java]
 * Description: RenderLayer multi-pass tinte pour MagicBeeEntity via ApicaBeeModel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeRenderer    | Renderer parent      | Acces donnees espece courante  |
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass par partie    |
 * | BeeSpeciesManager   | Donnees especes      | Couleurs par partie            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Layer de rendu multi-pass pour les abeilles magiques.
 * Chaque partie du modele (corpus, rayures, yeux, pupilles, pattes,
 * antennes, ailes, dard) est rendue separement avec sa propre couleur.
 *
 * Ce layer est appele par LivingEntityRenderer APRES les transforms
 * (rotation, scale, flip) et setupAnim, dans le meme contexte PoseStack.
 */
public class MagicBeeRenderLayer extends RenderLayer<MagicBeeEntity, ApicaBeeModel<MagicBeeEntity>> {

    private final MagicBeeRenderer parentRenderer;

    public MagicBeeRenderLayer(MagicBeeRenderer renderer) {
        super(renderer);
        this.parentRenderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       MagicBeeEntity bee, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        ApicaBeeModel<MagicBeeEntity> model = getParentModel();
        BeeSpeciesManager.BeeSpeciesData data = parentRenderer.currentData;

        int bodyColor = data != null ? data.partColorBody : 0xCC8800;
        int stripeColor = data != null ? data.partColorStripe : 0x1A1A1A;
        int wingColor = data != null ? data.partColorWing : 0xAADDFF;
        int antennaColor = data != null ? data.partColorAntenna : 0x1A1A1A;
        int stingerColor = data != null ? data.partColorStinger : 0xDDAA00;
        int eyeColor = data != null ? data.partColorEye : 0x1A1A1A;
        int pupilColor = data != null ? data.partColorPupil : 0xFFFFFF;

        int overlay = LivingEntityRenderer.getOverlayCoords(bee, 0.0F);

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

        // Restaure la visibilite complete du modele
        model.showAll();
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
