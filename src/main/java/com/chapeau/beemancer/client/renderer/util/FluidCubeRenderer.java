/**
 * ============================================================
 * [FluidCubeRenderer.java]
 * Description: Utilitaire partagé pour le rendu de cubes de fluide avec texture atlas
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation               |
 * |----------------------------|----------------------|---------------------------|
 * | PoseStack.Pose             | Matrice de rendu     | Transformation vertices   |
 * | VertexConsumer             | Pipeline de rendu    | Émission des vertices     |
 * | TextureAtlasSprite         | Texture fluide       | Coordonnées UV            |
 * | LightTexture               | Éclairage            | FULL_BRIGHT               |
 * | OverlayTexture             | Overlay              | NO_OVERLAY                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyTankRenderer.java
 * - HoneyReservoirRenderer.java
 * - MultiblockTankRenderer.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Utilitaire statique pour rendre un cube de fluide texturé avec 6 faces.
 * Rendu fullbright (pas d'ombrage), couleur blanche semi-transparente (pas de tint).
 * Utilise la texture atlas sprite animée du fluide.
 */
public final class FluidCubeRenderer {

    private FluidCubeRenderer() {
    }

    /**
     * Rend un cube de fluide avec les 6 faces (Y+, Y-, Z-, Z+, X-, X+).
     */
    public static void renderFluidCube(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ) {
        renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ,
            true, true, true, true, true, true);
    }

    /**
     * Rend un cube de fluide avec sélection des faces à afficher.
     * Chaque booléen contrôle si la face correspondante est rendue.
     *
     * @param renderUp    rendre la face Y+
     * @param renderDown  rendre la face Y-
     * @param renderNorth rendre la face Z-
     * @param renderSouth rendre la face Z+
     * @param renderWest  rendre la face X-
     * @param renderEast  rendre la face X+
     */
    public static void renderFluidCube(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       boolean renderUp, boolean renderDown,
                                       boolean renderNorth, boolean renderSouth,
                                       boolean renderWest, boolean renderEast) {

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.9f;

        int light = LightTexture.FULL_BRIGHT;

        // Face dessus (Y+)
        if (renderUp) {
            consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        }

        // Face dessous (Y-)
        if (renderDown) {
            consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
            consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
            consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
            consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        }

        // Face nord (Z-)
        if (renderNorth) {
            consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
            consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
            consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
            consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        }

        // Face sud (Z+)
        if (renderSouth) {
            consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        }

        // Face ouest (X-)
        if (renderWest) {
            consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
            consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
            consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
            consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        }

        // Face est (X+)
        if (renderEast) {
            consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
            consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
            consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
            consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        }
    }
}
