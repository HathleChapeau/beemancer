/**
 * ============================================================
 * [UncraftingTableRenderer.java]
 * Description: Renderer pour l'Uncrafting Table — item flottant, laser beam, items sur la slab
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|----------------------|--------------------------------|
 * | UncraftingTableBlockEntity      | Donnees item/output  | getInputSlot(), getOutputSlots |
 * | UncraftingTableBlock            | Etat WORKING         | Blockstate                     |
 * | FloatingItemHelper              | Rendu item flottant  | renderFloatingItem()           |
 * | AnimationTimer                  | Temps fluide         | Cycle cible laser              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.renderer.util.FloatingItemHelper;
import com.chapeau.apica.common.block.artifacts.UncraftingTableBlock;
import com.chapeau.apica.common.blockentity.artifacts.UncraftingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Renderer pour l'Uncrafting Table.
 * - Item d'input flottant dans le cadre au-dessus du bloc
 * - Laser beam vert (meme style que l'Assembly Table) de l'item vers la slab
 * - Items output affiches sur la slab une fois le craft termine
 */
public class UncraftingTableRenderer implements BlockEntityRenderer<UncraftingTableBlockEntity> {

    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** Couleur du beam (vert, meme que l'Assembly Table). */
    private static final int BEAM_R = 0;
    private static final int BEAM_G = 255;
    private static final int BEAM_B = 100;
    private static final int BEAM_A = 180;

    /** Position de l'item d'input (dans le cadre au-dessus). */
    private static final double INPUT_X = 0.5;
    private static final double INPUT_Y = 0.97;
    private static final double INPUT_Z = 0.5;

    private static final double GRID_X = 3.5f / 16f;
    private static final double GRID_Y = 1f - GRID_X;
    private static final double GRID_HEIGHT = 0.51f;

    /** Positions 3x3 sur la slab (Y juste au-dessus de la surface y=0.5). */
    private static final double[][] GRID = {
            {GRID_X, GRID_HEIGHT, GRID_X}, {0.50, GRID_HEIGHT, GRID_X}, {GRID_Y, GRID_HEIGHT, GRID_X},
            {GRID_X, GRID_HEIGHT, 0.50}, {0.50, GRID_HEIGHT, 0.50}, {GRID_Y, GRID_HEIGHT, 0.50},
            {GRID_X, GRID_HEIGHT, GRID_Y}, {0.50, GRID_HEIGHT, GRID_Y}, {GRID_Y, GRID_HEIGHT, GRID_Y}
    };

    /** Duree d'un cycle laser par slot (20 ticks = 1 seconde). */
    private static final int LASER_CYCLE_TICKS = 20;

    private final ItemRenderer itemRenderer;

    public UncraftingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(UncraftingTableBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Level level = be.getLevel();
        if (level == null) return;

        ItemStack inputItem = be.getInputSlot().getStackInSlot(0);
        boolean isWorking = be.getBlockState().getValue(UncraftingTableBlock.WORKING);

        // Item d'input flottant dans le cadre
        if (!inputItem.isEmpty()) {
            FloatingItemHelper.renderFloatingItem(itemRenderer, inputItem, level,
                    partialTick, poseStack, buffer, packedLight, packedOverlay,
                    INPUT_X, INPUT_Y, INPUT_Z, 0.2f, 0.01f, 1.0f);

            // Laser beam pendant le processing
            if (isWorking) {
                int tick = AnimationTimer.getTicks();
                int targetSlot = (tick / LASER_CYCLE_TICKS) % 9;
                double[] target = GRID[targetSlot];

                renderBeam(poseStack, buffer,
                        (float) INPUT_X, (float) INPUT_Y, (float) INPUT_Z,
                        (float) target[0], (float) target[1], (float) target[2]);
            }
        }

        // Items output sur la slab (apres completion)
        renderOutputItems(be, level, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les items output en 3x3 sur la surface de la slab.
     */
    private void renderOutputItems(UncraftingTableBlockEntity be, Level level,
                                    PoseStack poseStack, MultiBufferSource buffer,
                                    int packedLight, int packedOverlay) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = be.getOutputSlots().getStackInSlot(i);
            if (stack.isEmpty()) continue;

            double[] pos = GRID[i];
            poseStack.pushPose();
            poseStack.translate(pos[0], pos[1], pos[2]);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.scale(0.2f, 0.2f, 0.2f);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, buffer, level, 0);
            poseStack.popPose();
        }
    }

    /**
     * Rend un beam vert (2 quads en X) de la source vers la cible.
     * Meme technique que AssemblyTableOrbitRenderer.
     */
    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffer,
                                    float fx, float fy, float fz,
                                    float tx, float ty, float tz) {
        poseStack.pushPose();

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        float w = 0.03f;

        // Quad vertical (variation Y)
        beamVertex(vc, mat, fx, fy - w, fz, 0, 0);
        beamVertex(vc, mat, fx, fy + w, fz, 0, 1);
        beamVertex(vc, mat, tx, ty + w, tz, 1, 1);
        beamVertex(vc, mat, tx, ty - w, tz, 1, 0);

        // Quad horizontal (variation X)
        beamVertex(vc, mat, fx - w, fy, fz, 0, 0);
        beamVertex(vc, mat, fx + w, fy, fz, 0, 1);
        beamVertex(vc, mat, tx + w, ty, tz, 1, 1);
        beamVertex(vc, mat, tx - w, ty, tz, 1, 0);

        poseStack.popPose();
    }

    private static void beamVertex(VertexConsumer vc, Matrix4f mat,
                                    float x, float y, float z, float u, float v) {
        vc.addVertex(mat, x, y, z)
                .setColor(BEAM_R, BEAM_G, BEAM_B, BEAM_A)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0, 1, 0);
    }
}
