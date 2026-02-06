/**
 * ============================================================
 * [ImageSection.java]
 * Description: Module image du Codex Book - texture centrée sur la page
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Système de sections modulaires |
 * | ResourceLocation    | Chemin texture       | Chargement de l'image          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (sections illustrées)
 * - CodexBookScreen (rendu des images)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ImageSection extends CodexBookSection {

    private static final int PADDING_BOTTOM = 6;

    private final ResourceLocation texture;
    private final int imgWidth;
    private final int imgHeight;

    public ImageSection(ResourceLocation texture, int imgWidth, int imgHeight) {
        this.texture = texture;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getImgWidth() {
        return imgWidth;
    }

    public int getImgHeight() {
        return imgHeight;
    }

    @Override
    public SectionType getType() {
        return SectionType.IMAGE;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return imgHeight + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        int centeredX = x + (pageWidth - imgWidth) / 2;
        graphics.blit(texture, centeredX, y, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
    }

    public static ImageSection fromJson(JsonObject json) {
        String texturePath = json.has("texture") ? json.get("texture").getAsString() : "";
        ResourceLocation texture = ResourceLocation.parse(texturePath);
        int width = json.has("width") ? json.get("width").getAsInt() : 64;
        int height = json.has("height") ? json.get("height").getAsInt() : 64;
        return new ImageSection(texture, width, height);
    }
}
