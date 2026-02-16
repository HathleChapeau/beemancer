/**
 * ============================================================
 * [BeamRenderer.java]
 * Description: Rendu de beams style beacon entre deux points 3D arbitraires
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RenderType              | Beacon beam shader   | beaconBeam() inner/outer       |
 * | PoseStack               | Transforms 3D        | Rotation direction beam        |
 * | VertexConsumer          | Emission vertices    | Quads du beam                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AltarHeartRenderer.java (beam conduit vers coeur)
 * - StorageControllerRenderer.java (beams hive/terminal vers coeur)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Utilitaire de rendu de beams style beacon entre deux points 3D arbitraires.
 * Inner beam (losange opaque) + outer glow (carre translucide).
 * Texture vanilla beacon_beam.png avec scroll et rotation automatiques.
 */
public class BeamRenderer {

    private static final ResourceLocation BEAM_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    /**
     * Rend un beam style beacon entre deux points relatifs au PoseStack courant.
     *
     * @param poseStack   PoseStack courant (position du BlockEntity)
     * @param buffer      MultiBufferSource pour les vertex consumers
     * @param start       Point de depart du beam (relatif au BE)
     * @param end         Point d'arrivee du beam (relatif au BE)
     * @param partialTick Partial tick pour l'animation
     * @param gameTime    Temps de jeu pour le scroll de texture
     * @param innerRadius Rayon du beam interieur (ex: 0.05)
     * @param outerRadius Rayon du glow exterieur (ex: 0.15)
     * @param red         Composante rouge (0-1)
     * @param green       Composante verte (0-1)
     * @param blue        Composante bleue (0-1)
     */
    public static void renderBeam(PoseStack poseStack, MultiBufferSource buffer,
                                   Vec3 start, Vec3 end,
                                   float partialTick, long gameTime,
                                   float innerRadius, float outerRadius,
                                   float red, float green, float blue) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.01) return;

        // Rotation pour aligner l'axe Y du PoseStack avec la direction du beam
        double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) Math.atan2(horizDist, dir.y);

        // Animation: scroll texture + rotation beam autour de son axe
        float time = (float) Math.floorMod(gameTime, 40) + partialTick;
        float scrollOffset = Mth.frac(-time * 0.2F - (float) Mth.floor(-time * 0.1F));
        float rotation = time * 2.25F - 45.0F;

        float height = (float) length;
        float vMin = -1.0F + scrollOffset;

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Inner beam: section carree, opaque (coins CCW: SW→NW→NE→SE, comme vanilla)
        float vMaxInner = height * (0.5F / innerRadius) + vMin;
        renderPart(poseStack, buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, false)),
            red, green, blue, 1.0F, height,
            -innerRadius, -innerRadius, -innerRadius, innerRadius,
            innerRadius, innerRadius, innerRadius, -innerRadius,
            0, 1, vMaxInner, vMin);

        // Outer glow: section carree, translucide (coins CCW: SW→NW→NE→SE, comme vanilla)
        float vMaxOuter = height + vMin;
        renderPart(poseStack, buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true)),
            red, green, blue, 0.125F, height,
            -outerRadius, -outerRadius, -outerRadius, outerRadius,
            outerRadius, outerRadius, outerRadius, -outerRadius,
            0, 1, vMaxOuter, vMin);

        poseStack.popPose();
    }

    private static void renderPart(PoseStack poseStack, VertexConsumer consumer,
                                    float r, float g, float b, float a, float height,
                                    float x1, float z1, float x2, float z2,
                                    float x3, float z3, float x4, float z4,
                                    float minU, float maxU, float maxV, float minV) {
        Matrix4f matrix = poseStack.last().pose();
        renderQuad(matrix, consumer, r, g, b, a, height, x1, z1, x2, z2, minU, maxU, maxV, minV);
        renderQuad(matrix, consumer, r, g, b, a, height, x3, z3, x4, z4, minU, maxU, maxV, minV);
        renderQuad(matrix, consumer, r, g, b, a, height, x2, z2, x3, z3, minU, maxU, maxV, minV);
        renderQuad(matrix, consumer, r, g, b, a, height, x4, z4, x1, z1, minU, maxU, maxV, minV);
    }

    private static void renderQuad(Matrix4f matrix, VertexConsumer consumer,
                                    float r, float g, float b, float a, float height,
                                    float x1, float z1, float x2, float z2,
                                    float minU, float maxU, float maxV, float minV) {
        addVertex(matrix, consumer, r, g, b, a, height, x1, z1, maxU, minV);
        addVertex(matrix, consumer, r, g, b, a, 0, x1, z1, maxU, maxV);
        addVertex(matrix, consumer, r, g, b, a, 0, x2, z2, minU, maxV);
        addVertex(matrix, consumer, r, g, b, a, height, x2, z2, minU, minV);
    }

    private static void addVertex(Matrix4f matrix, VertexConsumer consumer,
                                   float r, float g, float b, float a,
                                   float y, float x, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z)
            .setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255))
            .setUv(u, v)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);
    }

    private BeamRenderer() {
    }
}
