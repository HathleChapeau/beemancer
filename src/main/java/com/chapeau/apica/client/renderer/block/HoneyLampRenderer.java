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
 * Le halo consiste en 2 cross planes diagonales avec epaisseur 0.02.
 * La texture change selon le fluide: honey, royal_jelly ou nectar.
 * Pas de halo quand la lampe est eteinte (OFF).
 */
public class HoneyLampRenderer implements BlockEntityRenderer<HoneyLampBlockEntity> {

    private static final ResourceLocation HALO_HONEY =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/artifacts/honey_lamp_halo_honey.png");
    private static final ResourceLocation HALO_ROYAL_JELLY =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/artifacts/honey_lamp_halo_royal_jelly.png");
    private static final ResourceLocation HALO_NECTAR =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/alchemy/artifacts/honey_lamp_halo_nectar.png");

    // Cross geometry centered on the glass body [5,2,5]-[11,12,11]
    private static final float CX = 0.5f;
    private static final float CZ = 0.5f;
    private static final float Y_MIN = 0f;
    private static final float Y_MAX = 14f / 16f;
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
        crossPlane(vc, pose, CX - R, CZ - R, CX + R, CZ + R, overlay);

        // Plane 2: NW-SE diagonal
        crossPlane(vc, pose, CX + R, CZ - R, CX - R, CZ + R, overlay);
    }

    /**
     * Rend un cross plane single-face (entityTranslucent a NO_CULL, visible des 2 cotes).
     */
    private static void crossPlane(VertexConsumer vc, PoseStack.Pose pose,
                                    float x0, float z0, float x1, float z1, int overlay) {
        vertex(vc, pose, x0, Y_MIN, z0, 0, 1, overlay);
        vertex(vc, pose, x1, Y_MIN, z1, 1, 1, overlay);
        vertex(vc, pose, x1, Y_MAX, z1, 1, 0, overlay);
        vertex(vc, pose, x0, Y_MAX, z0, 0, 0, overlay);
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
