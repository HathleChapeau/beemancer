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

    // Teintes par etat (RGB)
    private static final float[] HONEY_TINT =       { 1.0f, 0.7f, 0.1f };
    private static final float[] ROYAL_JELLY_TINT = { 1.0f, 0.95f, 0.8f };
    private static final float[] NECTAR_TINT =      { 0.6f, 0.2f, 1.0f };

    // Cross geometry centered on the glass body [5,2,5]-[11,12,11]
    private static final float CX = 0.5f;
    private static final float CZ = 0.5f;
    private static final float SCALE = 1.75f;
    // Hauteur du halo centree sur le milieu du verre (Y=7px = 0.4375)
    private static final float HALO_HEIGHT = 14f / 16f * SCALE;
    private static final float Y_CENTER = 7f / 16f;
    private static final float Y_MIN = Y_CENTER - HALO_HEIGHT / 2f;
    private static final float Y_MAX = Y_CENTER + HALO_HEIGHT / 2f;
    // R calcule pour que la largeur diagonale (2R*sqrt2) = hauteur (Y_MAX-Y_MIN) → pixels carres
    private static final float R = (Y_MAX - Y_MIN) / (2f * 1.4142f);

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

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(HALO_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        // Plane 1: SW-NE diagonal
        crossPlane(vc, pose, CX - R, CZ - R, CX + R, CZ + R, tint, overlay);

        // Plane 2: NW-SE diagonal
        crossPlane(vc, pose, CX + R, CZ - R, CX - R, CZ + R, tint, overlay);
    }

    /**
     * Rend un cross plane single-face (entityTranslucent a NO_CULL, visible des 2 cotes).
     */
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
