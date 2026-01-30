/**
 * ============================================================
 * [DebugRenderHelper.java]
 * Description: Utilitaires partagés pour le rendu de lignes et outlines de debug
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation               |
 * |---------------------|----------------------|---------------------------|
 * | VertexConsumer      | Pipeline de rendu    | Émission des vertices     |
 * | Matrix4f            | Matrice de rendu     | Transformation vertices   |
 * | RenderType          | Type de rendu        | lines()                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeDebugRenderer.java
 * - StorageControllerRenderer.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Utilitaires statiques pour le rendu de lignes et outlines de debug.
 * Fournit des primitives réutilisables pour dessiner des lignes, cubes et marqueurs.
 */
public final class DebugRenderHelper {

    private DebugRenderHelper() {
    }

    /**
     * Dessine une ligne entre deux points (coordonnées float).
     * Calcule automatiquement la normale à partir de la direction.
     */
    public static void drawLine(VertexConsumer buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001f) length = 1;
        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;

        buffer.addVertex(matrix, x1, y1, z1)
              .setColor(r, g, b, a)
              .setNormal(nx, ny, nz);
        buffer.addVertex(matrix, x2, y2, z2)
              .setColor(r, g, b, a)
              .setNormal(nx, ny, nz);
    }

    /**
     * Dessine une ligne entre deux points Vec3.
     * Convenience method pour les renderers qui travaillent avec des Vec3.
     */
    public static void drawLine(PoseStack poseStack, MultiBufferSource bufferSource,
                                Vec3 from, Vec3 to,
                                float r, float g, float b, float a) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.001) return;
        dir = dir.scale(1.0 / len);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal((float) dir.x, (float) dir.y, (float) dir.z);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal((float) dir.x, (float) dir.y, (float) dir.z);
    }

    /**
     * Dessine un outline de cube (12 arêtes) entre min et max.
     */
    public static void drawCubeOutline(VertexConsumer buffer, Matrix4f matrix,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       float r, float g, float b, float a) {
        // Face inférieure (4 arêtes)
        drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Face supérieure (4 arêtes)
        drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Arêtes verticales (4 arêtes)
        drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }
}
