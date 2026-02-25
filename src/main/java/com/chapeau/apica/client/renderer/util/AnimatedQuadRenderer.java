/**
 * ============================================================
 * [AnimatedQuadRenderer.java]
 * Description: Utilitaire pour rendre des boites texturees avec UV dynamiques (animations frame-by-frame)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PoseStack.Pose      | Matrice de rendu     | Transformation vertices        |
 * | VertexConsumer      | Pipeline de rendu    | Emission des vertices          |
 * | OverlayTexture      | Overlay              | NO_OVERLAY                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LeafBlowerItemRenderer.java (overlay charging + barres)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Rend une boite 3D texturee avec des coordonnees UV parametrables.
 * Permet de selectionner une frame dans un atlas en ajustant les UV V (vertical).
 * Pattern inspire de FluidCubeRenderer mais avec UV absolues (espace 0-1) pour textures standalone.
 */
public final class AnimatedQuadRenderer {

    private AnimatedQuadRenderer() {
    }

    /**
     * Rend une boite 3D avec les 6 faces, en utilisant des UV parametrables.
     *
     * @param consumer  vertex consumer (entity render type)
     * @param pose      matrice de transformation courante
     * @param minX      coin minimum X (en unites bloc, 0-1)
     * @param minY      coin minimum Y
     * @param minZ      coin minimum Z
     * @param maxX      coin maximum X
     * @param maxY      coin maximum Y
     * @param maxZ      coin maximum Z
     * @param u0        coordonnee U minimum (0-1)
     * @param v0        coordonnee V minimum (0-1)
     * @param u1        coordonnee U maximum (0-1)
     * @param v1        coordonnee V maximum (0-1)
     * @param light     packed light
     */
    public static void renderBox(VertexConsumer consumer, PoseStack.Pose pose,
                                 float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ,
                                 float u0, float v0, float u1, float v1,
                                 int light) {
        int overlay = OverlayTexture.NO_OVERLAY;

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }
}
