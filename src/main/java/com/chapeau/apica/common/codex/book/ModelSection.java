/**
 * ============================================================
 * [ModelSection.java]
 * Description: Module modele 3D du Codex Book - affiche un ApicaBeeModel tinte par espece
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | ApicaBeeModel       | Modele modulaire     | Rendu 3D de l'abeille tintee   |
 * | BeeSpeciesManager   | Donnees especes      | Model types + couleurs parties |
 * | BeeBodyType etc.    | Enum types           | Resolution modele par espece   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (sections avec modele 3D)
 * - CodexBookScreen (rendu du modele sur la page)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ModelSection extends CodexBookSection {

    private static final int PADDING_BOTTOM = 6;

    /** Cache des modeles par cle de combinaison de types. */
    private static final Map<String, ApicaBeeModel<?>> MODEL_CACHE = new HashMap<>();

    private final String species;
    private final int displayHeight;
    private final float scale;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float rotX;
    private final float rotY;

    public ModelSection(String species, int displayHeight, float scale,
                        float offsetX, float offsetY, float offsetZ,
                        float rotX, float rotY) {
        this.species = species;
        this.displayHeight = displayHeight;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotX = rotX;
        this.rotY = rotY;
    }

    @Override
    public SectionType getType() {
        return SectionType.MODEL;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return displayHeight + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(species);

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

        float centerX = x + pageWidth / 2.0f + offsetX;
        float centerY = y + displayHeight / 2.0f + offsetY;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY + 54, offsetZ);
        poseStack.scale(scale + 40, scale + 40, scale + 40);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
                .renderBuffers().bufferSource();

        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        // Multi-pass body render
        VertexConsumer bodyVC = bufferSource.getBuffer(RenderType.entityCutout(bodyTex));

        model.showCorpusOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(bodyColor));

        model.showStripeOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(stripeColor));

        model.showEyesOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(eyeColor));

        model.showPupilsOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(pupilColor));

        model.showUntintedOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(stripeColor));

        // Antenna
        VertexConsumer antennaVC = bufferSource.getBuffer(RenderType.entityCutout(antennaTex));
        model.renderAntenna(poseStack, antennaVC, light, overlay, toArgb(antennaColor));

        // Wings
        VertexConsumer wingVC = bufferSource.getBuffer(RenderType.entityCutout(wingTex));
        model.renderWings(poseStack, wingVC, light, overlay, toArgb(wingColor));

        // Stinger
        VertexConsumer stingerVC = bufferSource.getBuffer(RenderType.entityCutout(stingerTex));
        model.renderStinger(poseStack, stingerVC, light, overlay, toArgb(stingerColor));

        bufferSource.endBatch();
        poseStack.popPose();
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

    /** Vide le cache des modeles (utile lors du rechargement des ressources). */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
    }

    public static ModelSection fromJson(JsonObject json) {
        String species = json.has("species") ? json.get("species").getAsString() : "meadow";
        int height = json.has("height") ? json.get("height").getAsInt() : 80;
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : -30f;
        float x = json.has("x") ? json.get("x").getAsFloat() : 0f;
        float y = json.has("y") ? json.get("y").getAsFloat() : -40f;
        float z = json.has("z") ? json.get("z").getAsFloat() : 100f;
        float rotX = json.has("rot_x") ? json.get("rot_x").getAsFloat() : 160f;
        float rotY = json.has("rot_y") ? json.get("rot_y").getAsFloat() : 144f;
        return new ModelSection(species, height, scale, x, y, z, rotX, rotY);
    }
}
