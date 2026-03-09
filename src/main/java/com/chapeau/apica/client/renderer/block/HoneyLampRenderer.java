/**
 * ============================================================
 * [HoneyLampRenderer.java]
 * Description: Renderer pour le halo lumineux de la Honey Lamp (2 cross planes translucent)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoneyLampBlockEntity    | Donnees a rendre     | Lecture du blockstate          |
 * | HoneyLampBlock          | LampState enum       | Choix texture halo + teinte    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.artifacts.HoneyLampBlock;
import com.chapeau.apica.common.block.artifacts.HoneyLampBlock.LampState;
import com.chapeau.apica.common.blockentity.artifacts.HoneyLampBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend un halo translucide autour de la Honey Lamp quand elle est alimentee.
 * Texture unique teintee selon le fluide: honey (ambre), royal_jelly (rose), nectar (violet).
 * Pas de halo quand la lampe est eteinte (OFF).
 */
public class HoneyLampRenderer implements BlockEntityRenderer<HoneyLampBlockEntity> {

    private static final ResourceLocation HALO_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/artifacts/honey_lamp_halo_honey.png");

    private static final ResourceLocation RING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/ring.png");

    // Teintes par etat (RGB)
    private static final float[] HONEY_TINT =       { 1.0f, 0.7f, 0.1f };
    private static final float[] ROYAL_JELLY_TINT = { 1.0f, 0.95f, 0.8f };
    private static final float[] NECTAR_TINT =      { 0.6f, 0.2f, 1.0f };

    // Cross geometry centered on the glass body
    private static final float CX = 0.5f;
    private static final float CZ = 0.5f;
    private static final float SCALE = 0.74375f;
    private static final float HALO_HEIGHT = 14f / 16f * SCALE;
    private static final float Y_CENTER = 7f / 16f;
    private static final float Y_MIN = Y_CENTER - HALO_HEIGHT / 2f;
    private static final float Y_MAX = Y_CENTER + HALO_HEIGHT / 2f;
    private static final float R = (Y_MAX - Y_MIN) / (2f * 1.4142f);

    // Ring geometry (horizontal inside the glass body)
    private static final int RING_FACE_COUNT = 12;
    private static final float RING_RADIUS = 4.84f / 16f;
    private static final float RING_HALF_DEPTH = 0.0242f;
    private static final float RING_Y = 7f / 16f;
    private static final float RING_SPEED = 0.03f;

    private static final float RAISED_OFFSET = 1f / 16f;
    private static final int FULLBRIGHT = 0xF000F0;

    public HoneyLampRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HoneyLampBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        LampState state = be.getBlockState().getValue(HoneyLampBlock.LAMP_STATE);
        if (state == LampState.OFF) return;

        float[] tint = switch (state) {
            case HONEY -> HONEY_TINT;
            case ROYAL_JELLY -> ROYAL_JELLY_TINT;
            case NECTAR -> NECTAR_TINT;
            default -> null;
        };
        if (tint == null) return;

        boolean raised = be.getBlockState().getValue(HoneyLampBlock.PIPE_UP);
        float yOffset = raised ? RAISED_OFFSET : 0f;

        poseStack.pushPose();
        if (raised) poseStack.translate(0, yOffset, 0);

        // Cross planes
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(HALO_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        crossPlane(vc, pose, CX - R, CZ - R, CX + R, CZ + R, tint, overlay);
        crossPlane(vc, pose, CX + R, CZ - R, CX - R, CZ + R, tint, overlay);

        // Ring (horizontal, rotating)
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        long cycleLength = (long) Math.ceil(2 * Math.PI / RING_SPEED);
        float rotation = ((gameTime % cycleLength) + partialTick) * RING_SPEED;
        renderHorizontalRing(poseStack, buffer, FULLBRIGHT, rotation);

        poseStack.popPose();
    }

    private static void renderHorizontalRing(PoseStack poseStack, MultiBufferSource buffer,
                                              int light, float rotation) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(0.5f, RING_Y, 0.5f);
        poseStack.mulPose(Axis.YP.rotation(rotation));

        float angleStep = (float) (2.0 * Math.PI / RING_FACE_COUNT);
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < RING_FACE_COUNT; i++) {
            float angle0 = i * angleStep;
            float angle1 = (i + 1) * angleStep;
            float angleMid = (angle0 + angle1) * 0.5f;

            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            float x0 = cos0 * RING_RADIUS;
            float z0 = sin0 * RING_RADIUS;
            float x1 = cos1 * RING_RADIUS;
            float z1 = sin1 * RING_RADIUS;

            float nx = (float) Math.cos(angleMid);
            float nz = (float) Math.sin(angleMid);

            // Face exterieure du cylindre horizontal
            vc.addVertex(pose, x0, -RING_HALF_DEPTH, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, nx, 0, nz);
            vc.addVertex(pose, x0, RING_HALF_DEPTH, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, 0, nz);
            vc.addVertex(pose, x1, RING_HALF_DEPTH, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, 0, nz);
            vc.addVertex(pose, x1, -RING_HALF_DEPTH, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, nx, 0, nz);
        }

        poseStack.popPose();
    }

    private static void crossPlane(VertexConsumer vc, PoseStack.Pose pose,
                                    float x0, float z0, float x1, float z1,
                                    float[] tint, int overlay) {
        vertex(vc, pose, x0, Y_MIN, z0, 0, 1, tint, overlay);
        vertex(vc, pose, x1, Y_MIN, z1, 1, 1, tint, overlay);
        vertex(vc, pose, x1, Y_MAX, z1, 1, 0, tint, overlay);
        vertex(vc, pose, x0, Y_MAX, z0, 0, 0, tint, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                                float x, float y, float z, float u, float v,
                                float[] tint, int overlay) {
        vc.addVertex(pose, x, y, z)
            .setColor(tint[0], tint[1], tint[2], 1f)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(FULLBRIGHT)
            .setNormal(pose, 0, 1, 0);
    }
}
