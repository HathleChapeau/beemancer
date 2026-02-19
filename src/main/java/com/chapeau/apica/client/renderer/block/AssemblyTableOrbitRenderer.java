/**
 * ============================================================
 * [AssemblyTableOrbitRenderer.java]
 * Description: Rendu de 2 cubes orbitants avec beam vert au-dessus de l'Assembly Table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | AssemblyTableBlockEntity    | Presence de piece    | isEmpty()                      |
 * | AnimationTimer              | Temps de rendu       | Interpolation smooth           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AssemblyTableRenderer.java: Appel depuis render()
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Systeme de rendu des 2 cubes orbitants au-dessus de l'Assembly Table.
 * Chaque cube se deplace a une position aleatoire dans un carre de 16x1x16
 * 1.5 blocs au-dessus de la table, pointe vers le centre, tire un beam vert
 * pendant 0.5 sec, puis change de position.
 * Etat gere par table (client-side, Map<BlockPos, OrbitState>).
 */
@OnlyIn(Dist.CLIENT)
public class AssemblyTableOrbitRenderer {

    private static final Map<BlockPos, OrbitState> STATES = new HashMap<>();

    private static final float CUBE_SIZE = 0.125f;
    private static final float BEAM_DURATION = 0.5f;
    private static final float MOVE_DURATION = 1.0f;
    private static final float PAUSE_DURATION = 0.3f;
    private static final float ORBIT_RADIUS = 8.0f;
    private static final float ORBIT_HEIGHT = 1.5f;

    // Beam color (green, semi-transparent)
    private static final int BEAM_R = 0;
    private static final int BEAM_G = 255;
    private static final int BEAM_B = 100;
    private static final int BEAM_A = 180;

    // Cube color (teal glow)
    private static final int CUBE_R = 50;
    private static final int CUBE_G = 220;
    private static final int CUBE_B = 180;
    private static final int CUBE_A = 220;

    /** Etat des 2 cubes pour une table. */
    static class OrbitState {
        final CubeState[] cubes = new CubeState[2];
        OrbitState(BlockPos tablePos) {
            Random rng = new Random(tablePos.hashCode());
            for (int i = 0; i < 2; i++) {
                cubes[i] = new CubeState(tablePos, rng);
            }
        }
    }

    /** Etat d'un cube individuel. */
    static class CubeState {
        Vec3 currentPos;
        Vec3 targetPos;
        float timer;
        Phase phase;
        final BlockPos tablePos;
        final Random rng;

        CubeState(BlockPos tablePos, Random rng) {
            this.tablePos = tablePos;
            this.rng = new Random(rng.nextLong());
            Vec3 center = Vec3.atCenterOf(tablePos).add(0, ORBIT_HEIGHT, 0);
            this.currentPos = randomPosition(center, this.rng);
            this.targetPos = randomPosition(center, this.rng);
            this.timer = 0;
            this.phase = Phase.MOVING;
        }

        void tick(float deltaTime) {
            timer += deltaTime;
            switch (phase) {
                case MOVING:
                    if (timer >= MOVE_DURATION) {
                        currentPos = targetPos;
                        timer = 0;
                        phase = Phase.PAUSED;
                    }
                    break;
                case PAUSED:
                    if (timer >= PAUSE_DURATION) {
                        timer = 0;
                        phase = Phase.BEAMING;
                    }
                    break;
                case BEAMING:
                    if (timer >= BEAM_DURATION) {
                        timer = 0;
                        phase = Phase.MOVING;
                        Vec3 center = Vec3.atCenterOf(tablePos).add(0, ORBIT_HEIGHT, 0);
                        targetPos = randomPosition(center, rng);
                    }
                    break;
            }
        }

        Vec3 getInterpolatedPos(float partialTick) {
            if (phase == Phase.MOVING) {
                float progress = Math.min(1.0f, (timer + partialTick * 0.05f) / MOVE_DURATION);
                double smooth = smoothStep(progress);
                return new Vec3(
                        currentPos.x + (targetPos.x - currentPos.x) * smooth,
                        currentPos.y + (targetPos.y - currentPos.y) * smooth,
                        currentPos.z + (targetPos.z - currentPos.z) * smooth
                );
            }
            return currentPos;
        }

        boolean isBeaming() {
            return phase == Phase.BEAMING;
        }
    }

    enum Phase { MOVING, PAUSED, BEAMING }

    /**
     * Rend les 2 cubes orbitants pour une table donnee.
     * Appele depuis AssemblyTableRenderer.render().
     *
     * @param tablePos position du bloc table
     * @param partialTick tick partiel pour interpolation
     * @param poseStack pile de transformations (deja dans l'espace relatif au bloc)
     * @param buffer source de buffers
     * @param packedLight lumiere
     */
    public static void render(BlockPos tablePos, float partialTick, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight) {

        OrbitState state = STATES.computeIfAbsent(tablePos, OrbitState::new);

        // Tick les cubes (approximation via partialTick)
        float deltaTime = 0.05f;
        for (CubeState cube : state.cubes) {
            cube.tick(deltaTime);
        }

        Vec3 tableCenter = new Vec3(0.5, ORBIT_HEIGHT, 0.5);

        for (CubeState cube : state.cubes) {
            Vec3 cubePos = cube.getInterpolatedPos(partialTick);
            // Position relative au bloc (poseStack est deja en espace bloc)
            Vec3 relPos = cubePos.subtract(Vec3.atCenterOf(tablePos)).add(0.5, 0, 0.5);

            // Rendre le cube
            renderCube(poseStack, buffer, relPos, tableCenter, packedLight);

            // Rendre le beam si en phase beaming
            if (cube.isBeaming()) {
                renderBeam(poseStack, buffer, relPos, tableCenter, packedLight);
            }
        }
    }

    /** Supprime l'etat d'une table (quand la piece est retiree). */
    public static void removeState(BlockPos tablePos) {
        STATES.remove(tablePos);
    }

    /** Nettoie tous les etats (changement de monde). */
    public static void clearAll() {
        STATES.clear();
    }

    /**
     * Rend un petit cube colore a la position donnee.
     */
    private static void renderCube(PoseStack poseStack, MultiBufferSource buffer,
                                    Vec3 pos, Vec3 lookTarget, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);

        // Rotation pour pointer vers la table
        Vec3 dir = lookTarget.subtract(pos).normalize();
        float yaw = (float) Math.atan2(dir.x, dir.z);
        poseStack.mulPose(new org.joml.Quaternionf().rotationY(yaw));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png")));

        float s = CUBE_SIZE;
        Matrix4f mat = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // 6 faces du cube
        // Front (Z+)
        vertex(vc, mat, normal, -s, -s, s, 0, 0, 0, 0, 1, packedLight);
        vertex(vc, mat, normal, s, -s, s, 1, 0, 0, 0, 1, packedLight);
        vertex(vc, mat, normal, s, s, s, 1, 1, 0, 0, 1, packedLight);
        vertex(vc, mat, normal, -s, s, s, 0, 1, 0, 0, 1, packedLight);
        // Back (Z-)
        vertex(vc, mat, normal, s, -s, -s, 0, 0, 0, 0, -1, packedLight);
        vertex(vc, mat, normal, -s, -s, -s, 1, 0, 0, 0, -1, packedLight);
        vertex(vc, mat, normal, -s, s, -s, 1, 1, 0, 0, -1, packedLight);
        vertex(vc, mat, normal, s, s, -s, 0, 1, 0, 0, -1, packedLight);
        // Top (Y+)
        vertex(vc, mat, normal, -s, s, -s, 0, 0, 0, 1, 0, packedLight);
        vertex(vc, mat, normal, -s, s, s, 0, 1, 0, 1, 0, packedLight);
        vertex(vc, mat, normal, s, s, s, 1, 1, 0, 1, 0, packedLight);
        vertex(vc, mat, normal, s, s, -s, 1, 0, 0, 1, 0, packedLight);
        // Bottom (Y-)
        vertex(vc, mat, normal, -s, -s, s, 0, 0, 0, -1, 0, packedLight);
        vertex(vc, mat, normal, -s, -s, -s, 0, 1, 0, -1, 0, packedLight);
        vertex(vc, mat, normal, s, -s, -s, 1, 1, 0, -1, 0, packedLight);
        vertex(vc, mat, normal, s, -s, s, 1, 0, 0, -1, 0, packedLight);
        // Right (X+)
        vertex(vc, mat, normal, s, -s, s, 0, 0, 1, 0, 0, packedLight);
        vertex(vc, mat, normal, s, -s, -s, 1, 0, 1, 0, 0, packedLight);
        vertex(vc, mat, normal, s, s, -s, 1, 1, 1, 0, 0, packedLight);
        vertex(vc, mat, normal, s, s, s, 0, 1, 1, 0, 0, packedLight);
        // Left (X-)
        vertex(vc, mat, normal, -s, -s, -s, 0, 0, -1, 0, 0, packedLight);
        vertex(vc, mat, normal, -s, -s, s, 1, 0, -1, 0, 0, packedLight);
        vertex(vc, mat, normal, -s, s, s, 1, 1, -1, 0, 0, packedLight);
        vertex(vc, mat, normal, -s, s, -s, 0, 1, -1, 0, 0, packedLight);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                                float x, float y, float z, float u, float v,
                                float nx, float ny, float nz, int light) {
        vc.addVertex(mat, x, y, z)
                .setColor(CUBE_R, CUBE_G, CUBE_B, CUBE_A)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    /**
     * Rend un beam (ligne epaisse) du cube vers le centre de la table.
     * Utilise un quad etire semi-transparent.
     */
    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffer,
                                    Vec3 from, Vec3 to, int packedLight) {
        poseStack.pushPose();

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png")));

        Matrix4f mat = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Beam = 2 quads croises (X et Y) entre from et to
        float beamWidth = 0.03f;
        float fx = (float) from.x, fy = (float) from.y, fz = (float) from.z;
        float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;

        // Quad horizontal (XZ plane beam)
        beamVertex(vc, mat, normal, fx, fy - beamWidth, fz, 0, 0, packedLight);
        beamVertex(vc, mat, normal, fx, fy + beamWidth, fz, 0, 1, packedLight);
        beamVertex(vc, mat, normal, tx, ty + beamWidth, tz, 1, 1, packedLight);
        beamVertex(vc, mat, normal, tx, ty - beamWidth, tz, 1, 0, packedLight);

        // Quad vertical (rotated 90 degrees)
        beamVertex(vc, mat, normal, fx - beamWidth, fy, fz, 0, 0, packedLight);
        beamVertex(vc, mat, normal, fx + beamWidth, fy, fz, 0, 1, packedLight);
        beamVertex(vc, mat, normal, tx + beamWidth, ty, tz, 1, 1, packedLight);
        beamVertex(vc, mat, normal, tx - beamWidth, ty, tz, 1, 0, packedLight);

        poseStack.popPose();
    }

    private static void beamVertex(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                                    float x, float y, float z, float u, float v, int light) {
        vc.addVertex(mat, x, y, z)
                .setColor(BEAM_R, BEAM_G, BEAM_B, BEAM_A)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0, 1, 0);
    }

    private static Vec3 randomPosition(Vec3 center, Random rng) {
        double ox = (rng.nextDouble() - 0.5) * ORBIT_RADIUS * 2;
        double oz = (rng.nextDouble() - 0.5) * ORBIT_RADIUS * 2;
        return center.add(ox, 0, oz);
    }

    private static float smoothStep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
}
