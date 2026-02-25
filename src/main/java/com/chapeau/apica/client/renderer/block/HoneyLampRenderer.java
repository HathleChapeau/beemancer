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
 * | HoneyLampBlock          | LampState enum       | Choix texture halo             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.alchemy.HoneyLampBlock;
import com.chapeau.apica.common.block.alchemy.HoneyLampBlock.LampState;
import com.chapeau.apica.common.blockentity.alchemy.HoneyLampBlockEntity;
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
 * Le halo consiste en 2 cross planes diagonales (comme le modele vanilla cross).
 * La texture change selon le fluide: honey, royal_jelly ou nectar.
 * Pas de halo quand la lampe est eteinte (OFF).
 */
public class HoneyLampRenderer implements BlockEntityRenderer<HoneyLampBlockEntity> {

    private static final ResourceLocation HALO_HONEY =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/honey_lamp_halo_honey.png");
    private static final ResourceLocation HALO_ROYAL_JELLY =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/honey_lamp_halo_royal_jelly.png");
    private static final ResourceLocation HALO_NECTAR =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/honey_lamp_halo_nectar.png");

    // Cross geometry: 2 diagonal planes centered on the glass body
    // Glass body: [5,2,5] to [11,12,11], halo extends to full block
    private static final float CX = 0.5f;
    private static final float CZ = 0.5f;
    private static final float R = 0.5f;
    private static final float Y_MIN = 0f;
    private static final float Y_MAX = 14f / 16f;

    private static final int FULLBRIGHT = 0xF000F0;

    public HoneyLampRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HoneyLampBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        LampState state = be.getBlockState().getValue(HoneyLampBlock.LAMP_STATE);
        if (state == LampState.OFF) return;

        ResourceLocation texture = switch (state) {
            case HONEY -> HALO_HONEY;
            case ROYAL_JELLY -> HALO_ROYAL_JELLY;
            case NECTAR -> HALO_NECTAR;
            default -> null;
        };
        if (texture == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        // Plane 1: SW-NE diagonal
        float x0 = CX - R, z0 = CZ - R;
        float x1 = CX + R, z1 = CZ + R;
        crossPlane(vc, pose, x0, z0, x1, z1, overlay);

        // Plane 2: NW-SE diagonal
        float x2 = CX + R, z2 = CZ - R;
        float x3 = CX - R, z3 = CZ + R;
        crossPlane(vc, pose, x2, z2, x3, z3, overlay);
    }

    /** Rend un plan double-face (front + back) entre deux coins au sol. */
    private static void crossPlane(VertexConsumer vc, PoseStack.Pose pose,
                                    float x0, float z0, float x1, float z1, int overlay) {
        // Front face
        vertex(vc, pose, x0, Y_MIN, z0, 0, 1, overlay);
        vertex(vc, pose, x1, Y_MIN, z1, 1, 1, overlay);
        vertex(vc, pose, x1, Y_MAX, z1, 1, 0, overlay);
        vertex(vc, pose, x0, Y_MAX, z0, 0, 0, overlay);

        // Back face
        vertex(vc, pose, x1, Y_MIN, z1, 0, 1, overlay);
        vertex(vc, pose, x0, Y_MIN, z0, 1, 1, overlay);
        vertex(vc, pose, x0, Y_MAX, z0, 1, 0, overlay);
        vertex(vc, pose, x1, Y_MAX, z1, 0, 0, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                                float x, float y, float z, float u, float v, int overlay) {
        vc.addVertex(pose, x, y, z)
            .setColor(1f, 1f, 1f, 1f)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(FULLBRIGHT)
            .setNormal(pose, 0, 1, 0);
    }
}
