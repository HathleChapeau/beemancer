/**
 * ============================================================
 * [BeeStatueRenderer.java]
 * Description: Renderer pour la statue d'abeille avec ApicaBeeModel multi-pass tinte
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | BeeStatueBlockEntity    | Donnees a rendre     | getSpeciesId()        |
 * | ApicaBeeModel           | Modele modulaire     | Multi-pass tinte      |
 * | BeeSpeciesManager       | Donnees especes      | Types + couleurs      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.block.statue.BeeStatueBlockEntity;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer pour la statue d'abeille.
 * Affiche le modele ApicaBee multi-pass tinte avec le nom de l'espece flottant au-dessus.
 */
public class BeeStatueRenderer implements BlockEntityRenderer<BeeStatueBlockEntity> {

    private final Font font;

    /** Cache des modeles par cle de combinaison de types. */
    private static final Map<String, ApicaBeeModel<?>> MODEL_CACHE = new HashMap<>();

    public BeeStatueRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BeeStatueBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        String speciesId = blockEntity.getSpeciesId();
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

        // ===== Render Bee Model =====
        poseStack.pushPose();

        poseStack.translate(0.5, 1.85, 0.5);

        float time = AnimationTimer.getRenderTime(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        float scale = 0.7f;
        poseStack.scale(scale, scale, scale);

        int overlay = OverlayTexture.NO_OVERLAY;

        // Multi-pass body render
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

        // Antenna
        VertexConsumer antennaVC = buffer.getBuffer(RenderType.entityCutout(antennaTex));
        model.renderAntenna(poseStack, antennaVC, packedLight, overlay, toArgb(antennaColor));

        // Wings
        VertexConsumer wingVC = buffer.getBuffer(RenderType.entityCutout(wingTex));
        model.renderWings(poseStack, wingVC, packedLight, overlay, toArgb(wingColor));

        // Stinger
        VertexConsumer stingerVC = buffer.getBuffer(RenderType.entityCutout(stingerTex));
        model.renderStinger(poseStack, stingerVC, packedLight, overlay, toArgb(stingerColor));

        poseStack.popPose();

        // ===== Render Name Tag =====
        poseStack.pushPose();

        poseStack.translate(0.5, 1.6, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.015f, -0.015f, 0.015f);

        Component speciesName = Component.translatable("species.apica." + speciesId);
        float textWidth = font.width(speciesName);

        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = 0.4f;
        int bgColor = (int)(bgOpacity * 255.0f) << 24;

        font.drawInBatch(
            speciesName,
            -textWidth / 2,
            0,
            0xFFFFFFFF,
            false,
            matrix,
            buffer,
            Font.DisplayMode.NORMAL,
            bgColor,
            packedLight
        );

        poseStack.popPose();
    }

    // ========== Model cache ==========

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

    /** Vide le cache des modeles (rechargement des ressources). */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
    }

    // ========== Type resolution ==========

    private static BeeBodyType resolveBodyType(String id) {
        for (BeeBodyType t : BeeBodyType.values()) if (t.getId().equals(id)) return t;
        return BeeBodyType.DEFAULT;
    }

    private static BeeWingType resolveWingType(String id) {
        for (BeeWingType t : BeeWingType.values()) if (t.getId().equals(id)) return t;
        return BeeWingType.DEFAULT;
    }

    private static BeeStingerType resolveStingerType(String id) {
        for (BeeStingerType t : BeeStingerType.values()) if (t.getId().equals(id)) return t;
        return BeeStingerType.DEFAULT;
    }

    private static BeeAntennaType resolveAntennaType(String id) {
        for (BeeAntennaType t : BeeAntennaType.values()) if (t.getId().equals(id)) return t;
        return BeeAntennaType.DEFAULT;
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    @Override
    public boolean shouldRenderOffScreen(BeeStatueBlockEntity blockEntity) {
        return true;
    }
}
