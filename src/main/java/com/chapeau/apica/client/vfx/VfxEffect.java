/**
 * ============================================================
 * [VfxEffect.java]
 * Description: Effet VFX compose de plusieurs quads avec depth buffer
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | VfxQuad             | Config quad          | Liste des quads a rendre       |
 * | RenderType          | Depth buffer         | entityCutout pour depth test   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BlackHoleEffect.java (extension)
 * - RailgunItemRenderer.java (rendu)
 *
 * ============================================================
 */
package com.chapeau.apica.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Effet VFX avec plusieurs quads textures.
 * Rendu avec depth test active (pas de layer ordering).
 * Chaque quad peut etre billboard (face camera) ou fixed (orientation monde).
 */
public class VfxEffect {

    protected final List<VfxQuad> quads = new ArrayList<>();

    public VfxEffect addQuad(VfxQuad quad) {
        quads.add(quad);
        return this;
    }

    /**
     * Rend l'effet a la position donnee.
     * @param poseStack stack de transformation (deja positionne)
     * @param buffer source de buffers
     * @param camera camera pour billboard
     * @param time temps en ticks + partialTick pour animation
     * @param light packed light
     */
    public void render(PoseStack poseStack, MultiBufferSource buffer, Camera camera,
                       float time, int light) {
        for (VfxQuad quad : quads) {
            renderQuad(poseStack, buffer, camera, quad, time, light);
        }
    }

    private void renderQuad(PoseStack poseStack, MultiBufferSource buffer, Camera camera,
                            VfxQuad quad, float time, int light) {
        poseStack.pushPose();

        float rotation = quad.initialRotation() + quad.rotationSpeed() * time;

        if (quad.mode() == VfxQuad.Mode.BILLBOARD) {
            applyBillboard(poseStack, camera, rotation);
        } else {
            applyFixed(poseStack, quad.fixedAxis(), quad.fixedUp(), rotation);
        }

        float half = quad.scale() * 0.5f;
        emitQuad(poseStack, buffer, quad.texture(), -half, -half, half, half,
                 quad.r(), quad.g(), quad.b(), quad.a(), light);

        poseStack.popPose();
    }

    /**
     * Oriente le quad pour toujours faire face a la camera.
     */
    private void applyBillboard(PoseStack poseStack, Camera camera, float rotation) {
        Quaternionf cameraRot = camera.rotation();
        poseStack.mulPose(cameraRot);
        poseStack.mulPose(new Quaternionf().rotateZ(rotation));
    }

    /**
     * Oriente le quad avec un axe fixe dans l'espace monde.
     */
    private void applyFixed(PoseStack poseStack, Vector3f axis, Vector3f up, float rotation) {
        Vector3f right = new Vector3f(up).cross(axis).normalize();
        Vector3f correctedUp = new Vector3f(axis).cross(right).normalize();

        Matrix4f mat = new Matrix4f().set(
            right.x, right.y, right.z, 0,
            correctedUp.x, correctedUp.y, correctedUp.z, 0,
            axis.x, axis.y, axis.z, 0,
            0, 0, 0, 1
        );
        poseStack.mulPose(new Quaternionf().setFromNormalized(mat));
        poseStack.mulPose(new Quaternionf().rotateZ(rotation));
    }

    /**
     * Emet un quad avec depth test active.
     * Utilise entityCutout pour proper depth handling.
     */
    private void emitQuad(PoseStack poseStack, MultiBufferSource buffer,
                          ResourceLocation texture, float x0, float y0, float x1, float y1,
                          float r, float g, float b, float a, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(texture));
        PoseStack.Pose pose = poseStack.last();
        int ol = OverlayTexture.NO_OVERLAY;

        vc.addVertex(pose, x0, y0, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(ol).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x0, y1, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(ol).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y1, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(ol).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y0, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(ol).setLight(light).setNormal(pose, 0, 0, 1);
    }
}
