/**
 * ============================================================
 * [ModelSection.java]
 * Description: Module modèle 3D du Codex Book - affiche un modèle d'abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Système de sections modulaires |
 * | BeeModel            | Modèle vanilla       | Rendu 3D de l'abeille          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (sections avec modèle 3D)
 * - CodexBookScreen (rendu du modèle sur la page)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ModelSection extends CodexBookSection {

    private static final ResourceLocation VANILLA_BEE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/bee/bee.png");
    private static final int PADDING_BOTTOM = 6;

    private static BeeModel<?> beeModel;

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
        ResourceLocation texture = getTexture();
        float centerX = x + pageWidth / 2.0f + offsetX;
        float centerY = y + displayHeight / 2.0f + offsetY;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY, offsetZ);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
                .renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityCutout(texture));

        BeeModel<?> model = getOrCreateModel();
        model.renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private ResourceLocation getTexture() {
        ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath(
                Beemancer.MOD_ID, "textures/entity/bee/" + species + "_bee.png");
        if (Minecraft.getInstance().getResourceManager().getResource(customTexture).isPresent()) {
            return customTexture;
        }
        return VANILLA_BEE_TEXTURE;
    }

    private static BeeModel<?> getOrCreateModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(Minecraft.getInstance()
                    .getEntityModels().bakeLayer(ModelLayers.BEE));
        }
        return beeModel;
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
