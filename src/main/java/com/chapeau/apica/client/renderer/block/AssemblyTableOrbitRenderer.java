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
 * | AnimationTimer              | Temps de rendu       | Source de temps fluide          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AssemblyTableRenderer.java: Appel depuis render()
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
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
 * Chaque cube se deplace a une position aleatoire dans une zone 1x0.1x1
 * 1.5 blocs au-dessus de la table, pointe vers le centre, tire un beam vert,
 * puis change de position.
 * Etat gere par table (client-side, Map<BlockPos, OrbitState>).
 */
@OnlyIn(Dist.CLIENT)
public class AssemblyTableOrbitRenderer {

    private static final Map<BlockPos, OrbitState> STATES = new HashMap<>();

    private static final float CUBE_SIZE = 0.03125f;
    private static final float MOVE_DURATION = 40.0f;
    private static final float PRE_BEAM_DURATION = 10.0f;
    private static final float BEAM_DURATION = 20.0f;
    private static final float POST_BEAM_DURATION = 10.0f;
    private static final float ORBIT_RADIUS = 0.5f;
    private static final float ORBIT_HEIGHT_VARIATION = 0.1f;
    private static final float ORBIT_HEIGHT = 1.5f;
    private static final float BEAM_TARGET_OFFSET = -0.5f;

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
        float lastRenderTime = -1f;

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
                        phase = Phase.PRE_BEAM;
                    }
                    break;
                case PRE_BEAM:
                    if (timer >= PRE_BEAM_DURATION) {
                        timer = 0;
                        phase = Phase.BEAMING;
                    }
                    break;
                case BEAMING:
                    if (timer >= BEAM_DURATION) {
                        timer = 0;
                        phase = Phase.POST_BEAM;
                    }
                    break;
                case POST_BEAM:
                    if (timer >= POST_BEAM_DURATION) {
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
                float progress = Math.min(1.0f, (timer + partialTick) / MOVE_DURATION);
                return new Vec3(
                        currentPos.x + (targetPos.x - currentPos.x) * progress,
                        currentPos.y + (targetPos.y - currentPos.y) * progress,
                        currentPos.z + (targetPos.z - currentPos.z) * progress
                );
            }
            return currentPos;
        }

        boolean isBeaming() {
            return phase == Phase.BEAMING;
        }
    }

    enum Phase { MOVING, PRE_BEAM, BEAMING, POST_BEAM }

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

        float currentTime = AnimationTimer.getRenderTime(partialTick);
        if (state.lastRenderTime < 0) {
            state.lastRenderTime = currentTime;
        }
        float deltaTime = currentTime - state.lastRenderTime;
        state.lastRenderTime = currentTime;

        for (CubeState cube : state.cubes) {
            cube.tick(deltaTime);
        }

        Vec3 beamTarget = new Vec3(0.5, ORBIT_HEIGHT + BEAM_TARGET_OFFSET, 0.5);

        for (CubeState cube : state.cubes) {
            Vec3 cubePos = cube.getInterpolatedPos(partialTick);
            Vec3 relPos = cubePos.subtract(Vec3.atCenterOf(tablePos)).add(0.5, 0, 0.5);

            renderCube(poseStack, buffer, relPos, beamTarget, packedLight);

            if (cube.isBeaming()) {
                renderBeam(poseStack, buffer, relPos, beamTarget, packedLight);
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

        Vec3 dir = lookTarget.subtract(pos).normalize();
        float yaw = (float) Math.atan2(dir.x, dir.z);
        poseStack.mulPose(new org.joml.Quaternionf().rotationY(yaw));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png")));

        float s = CUBE_SIZE;
        Matrix4f mat = poseStack.last().pose();

        // 6 faces du cube
        vertex(vc, mat, -s, -s, s, 0, 0, 0, 0, 1, packedLight);
        vertex(vc, mat, s, -s, s, 1, 0, 0, 0, 1, packedLight);
        vertex(vc, mat, s, s, s, 1, 1, 0, 0, 1, packedLight);
        vertex(vc, mat, -s, s, s, 0, 1, 0, 0, 1, packedLight);

        vertex(vc, mat, s, -s, -s, 0, 0, 0, 0, -1, packedLight);
        vertex(vc, mat, -s, -s, -s, 1, 0, 0, 0, -1, packedLight);
        vertex(vc, mat, -s, s, -s, 1, 1, 0, 0, -1, packedLight);
        vertex(vc, mat, s, s, -s, 0, 1, 0, 0, -1, packedLight);

        vertex(vc, mat, -s, s, -s, 0, 0, 0, 1, 0, packedLight);
        vertex(vc, mat, -s, s, s, 0, 1, 0, 1, 0, packedLight);
        vertex(vc, mat, s, s, s, 1, 1, 0, 1, 0, packedLight);
        vertex(vc, mat, s, s, -s, 1, 0, 0, 1, 0, packedLight);

        vertex(vc, mat, -s, -s, s, 0, 0, 0, -1, 0, packedLight);
        vertex(vc, mat, -s, -s, -s, 0, 1, 0, -1, 0, packedLight);
        vertex(vc, mat, s, -s, -s, 1, 1, 0, -1, 0, packedLight);
        vertex(vc, mat, s, -s, s, 1, 0, 0, -1, 0, packedLight);

        vertex(vc, mat, s, -s, s, 0, 0, 1, 0, 0, packedLight);
        vertex(vc, mat, s, -s, -s, 1, 0, 1, 0, 0, packedLight);
        vertex(vc, mat, s, s, -s, 1, 1, 1, 0, 0, packedLight);
        vertex(vc, mat, s, s, s, 0, 1, 1, 0, 0, packedLight);

        vertex(vc, mat, -s, -s, -s, 0, 0, -1, 0, 0, packedLight);
        vertex(vc, mat, -s, -s, s, 1, 0, -1, 0, 0, packedLight);
        vertex(vc, mat, -s, s, s, 1, 1, -1, 0, 0, packedLight);
        vertex(vc, mat, -s, s, -s, 0, 1, -1, 0, 0, packedLight);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
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

        float beamWidth = 0.03f;
        float fx = (float) from.x, fy = (float) from.y, fz = (float) from.z;
        float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;

        beamVertex(vc, mat, fx, fy - beamWidth, fz, 0, 0, packedLight);
        beamVertex(vc, mat, fx, fy + beamWidth, fz, 0, 1, packedLight);
        beamVertex(vc, mat, tx, ty + beamWidth, tz, 1, 1, packedLight);
        beamVertex(vc, mat, tx, ty - beamWidth, tz, 1, 0, packedLight);

        beamVertex(vc, mat, fx - beamWidth, fy, fz, 0, 0, packedLight);
        beamVertex(vc, mat, fx + beamWidth, fy, fz, 0, 1, packedLight);
        beamVertex(vc, mat, tx + beamWidth, ty, tz, 1, 1, packedLight);
        beamVertex(vc, mat, tx - beamWidth, ty, tz, 1, 0, packedLight);

        poseStack.popPose();
    }

    private static void beamVertex(VertexConsumer vc, Matrix4f mat,
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
        double oy = (rng.nextDouble() - 0.5) * ORBIT_HEIGHT_VARIATION;
        double oz = (rng.nextDouble() - 0.5) * ORBIT_RADIUS * 2;
        return center.add(ox, oy, oz);
    }
}
