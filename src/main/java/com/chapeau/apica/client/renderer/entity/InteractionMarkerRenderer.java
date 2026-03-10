/**
 * ============================================================
 * [InteractionMarkerRenderer.java]
 * Description: Renderer pour InteractionMarkerEntity avec wireframe conditionnel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | InteractionMarkerEntity     | Entite rendue        | getMarkerType()                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.core.entity.InteractionMarkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Renderer pour les InteractionMarkerEntity.
 * Les marqueurs de type "hoverbee_part_*" affichent un wireframe blanc.
 * Les autres types restent invisibles.
 */
@OnlyIn(Dist.CLIENT)
public class InteractionMarkerRenderer extends EntityRenderer<InteractionMarkerEntity> {

    private static final String HOVERBIKE_PREFIX = "hoverbee_part_";
    private static final float HALF_SIZE = 0.25f;
    private static final int COLOR_R = 255;
    private static final int COLOR_G = 255;
    private static final int COLOR_B = 255;
    private static final int COLOR_A = 180;

    public InteractionMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(InteractionMarkerEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!entity.getMarkerType().startsWith(HOVERBIKE_PREFIX)) return;

        poseStack.pushPose();

        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();

        float s = HALF_SIZE;
        float nx = 0, ny = 1, nz = 0;

        // 12 aretes du cube
        // Bottom face
        line(vc, mat, -s, -s, -s, s, -s, -s, nx, ny, nz);
        line(vc, mat, s, -s, -s, s, -s, s, nx, ny, nz);
        line(vc, mat, s, -s, s, -s, -s, s, nx, ny, nz);
        line(vc, mat, -s, -s, s, -s, -s, -s, nx, ny, nz);
        // Top face
        line(vc, mat, -s, s, -s, s, s, -s, nx, ny, nz);
        line(vc, mat, s, s, -s, s, s, s, nx, ny, nz);
        line(vc, mat, s, s, s, -s, s, s, nx, ny, nz);
        line(vc, mat, -s, s, s, -s, s, -s, nx, ny, nz);
        // Vertical edges
        line(vc, mat, -s, -s, -s, -s, s, -s, nx, ny, nz);
        line(vc, mat, s, -s, -s, s, s, -s, nx, ny, nz);
        line(vc, mat, s, -s, s, s, s, s, nx, ny, nz);
        line(vc, mat, -s, -s, s, -s, s, s, nx, ny, nz);

        poseStack.popPose();
    }

    private void line(VertexConsumer vc, Matrix4f mat,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float nx, float ny, float nz) {
        vc.addVertex(mat, x1, y1, z1).setColor(COLOR_R, COLOR_G, COLOR_B, COLOR_A).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(COLOR_R, COLOR_G, COLOR_B, COLOR_A).setNormal(nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(InteractionMarkerEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
