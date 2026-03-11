/**
 * ============================================================
 * [VfxEffect.java]
 * Description: Effet VFX compose de plusieurs quads avec transparence
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | VfxQuad             | Config quad          | Liste des quads a rendre       |
 * | RenderType          | Rendu translucent    | entityTranslucent (no cull)    |
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
 * Utilise entityTranslucentCull pour gerer lumiere et transparence.
 * Chaque quad peut etre billboard (face camera) ou fixed (orientation monde).
 */
public class VfxEffect {

    protected final List<VfxQuad> quads = new ArrayList<>();
    protected final List<Float> accumulatedRotations = new ArrayList<>();
    private float lastTime = -1;

    public VfxEffect addQuad(VfxQuad quad) {
        quads.add(quad);
        accumulatedRotations.add(quad.initialRotation());
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
        render(poseStack, buffer, camera, time, light, 1f, 1f);
    }

    /**
     * Rend l'effet avec multiplicateurs dynamiques.
     * @param scaleMult multiplicateur d'echelle (applique a tous les quads)
     * @param rotMult multiplicateur de vitesse de rotation
     */
    public void render(PoseStack poseStack, MultiBufferSource buffer, Camera camera,
                       float time, int light, float scaleMult, float rotMult) {
        render(poseStack, buffer, camera, time, light, scaleMult, rotMult, 1f, 1f, 1f);
    }

    /**
     * Rend l'effet avec multiplicateurs et teinte.
     * @param tintR teinte rouge (multiplie la couleur de chaque quad)
     * @param tintG teinte verte
     * @param tintB teinte bleue
     */
    public void render(PoseStack poseStack, MultiBufferSource buffer, Camera camera,
                       float time, int light, float scaleMult, float rotMult,
                       float tintR, float tintG, float tintB) {
        // Calcul du delta time
        float deltaTime = (lastTime < 0) ? 0 : (time - lastTime);
        lastTime = time;

        // Mise a jour et rendu de chaque quad
        for (int i = 0; i < quads.size(); i++) {
            VfxQuad quad = quads.get(i);

            // Accumule la rotation avec le multiplicateur
            float currentRot = accumulatedRotations.get(i);
            currentRot += quad.rotationSpeed() * rotMult * deltaTime;
            accumulatedRotations.set(i, currentRot);

            renderQuad(poseStack, buffer, camera, quad, currentRot, light, scaleMult, tintR, tintG, tintB);
        }
    }

    private void renderQuad(PoseStack poseStack, MultiBufferSource buffer, Camera camera,
                            VfxQuad quad, float rotation, int light, float scaleMult,
                            float tintR, float tintG, float tintB) {
        poseStack.pushPose();

        if (quad.mode() == VfxQuad.Mode.BILLBOARD) {
            applyBillboard(poseStack, camera, rotation);
        } else {
            applyFixed(poseStack, quad.fixedAxis(), quad.fixedUp(), rotation);
        }

        float half = quad.scale() * scaleMult * 0.5f;
        // Applique la teinte en multipliant avec la couleur du quad
        float r = quad.r() * tintR;
        float g = quad.g() * tintG;
        float b = quad.b() * tintB;

        // Normale vers le haut (0, 1, 0) pour eclairage neutre comme les particules
        emitQuad(poseStack, buffer, quad.texture(), -half, -half, half, half,
                 r, g, b, quad.a(), light, quad.flipU(), new Vector3f(0, 1, 0));

        poseStack.popPose();
    }

    /**
     * Applique uniquement la rotation sur l'axe Z (pas de suivi camera).
     */
    private void applyBillboard(PoseStack poseStack, Camera camera, float rotation) {
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
     * Emet un quad avec entityTranslucent (translucent + lumiere, sans backface cull).
     * La normale est passee en parametre sans transformation (comme le beam).
     */
    private void emitQuad(PoseStack poseStack, MultiBufferSource buffer,
                          ResourceLocation texture, float x0, float y0, float x1, float y1,
                          float r, float g, float b, float a, int light, boolean flipU,
                          Vector3f normal) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        int ol = OverlayTexture.NO_OVERLAY;

        float u0 = flipU ? 1 : 0;
        float u1 = flipU ? 0 : 1;

        // Normale non transformee (comme le beam qui n'a pas de rotation dans son pose)
        float nx = normal.x, ny = normal.y, nz = normal.z;
        vc.addVertex(pose, x0, y0, 0).setColor(r, g, b, a).setUv(u0, 1).setOverlay(ol).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(pose, x0, y1, 0).setColor(r, g, b, a).setUv(u0, 0).setOverlay(ol).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(pose, x1, y1, 0).setColor(r, g, b, a).setUv(u1, 0).setOverlay(ol).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(pose, x1, y0, 0).setColor(r, g, b, a).setUv(u1, 1).setOverlay(ol).setLight(light).setNormal(nx, ny, nz);
    }
}
