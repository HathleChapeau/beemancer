/**
 * ============================================================
 * [ApiRenderer.java]
 * Description: Renderer pour le bloc Api avec scale dynamique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApiBlockEntity      | Données de scale     | getVisualScale(), customName   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

/**
 * Render le modèle Api avec scale dynamique basé sur le level.
 * Le modèle est un cube 10x10x10 centré (from 3,0,3 to 13,10,13).
 * Le scale est appliqué autour du centre du bloc (X=0.5, Z=0.5, Y=0).
 */
public class ApiRenderer implements BlockEntityRenderer<ApiBlockEntity> {

    public static final ModelResourceLocation API_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api"));

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public ApiRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(ApiBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float scale = blockEntity.getVisualScale(partialTick);

        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(API_MODEL_LOC);

        if (model == null || model == modelManager.getMissingModel()) return;

        poseStack.pushPose();

        // Scale autour du centre X/Z du bloc, base Y=0
        poseStack.translate(0.5, 0, 0.5);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);

        // tesselateBlock pour avoir l'ambient occlusion (ombres)
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), model, blockEntity.getBlockState(),
            blockEntity.getBlockPos(), poseStack, consumer, false,
            random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout()
        );

        poseStack.popPose();

        // Afficher le nom custom si le joueur regarde Api
        if (blockEntity.getCustomName() != null) {
            renderNamePlate(blockEntity, scale, poseStack, buffer, packedLight);
        }
    }

    /**
     * Affiche le nom custom au-dessus d'Api (comme un name tag d'entité).
     * Visible uniquement quand le joueur passe le curseur dessus.
     */
    private void renderNamePlate(ApiBlockEntity blockEntity, float scale, PoseStack poseStack,
                                  MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)) return;
        if (!blockHit.getBlockPos().equals(blockEntity.getBlockPos())) return;

        Component name = blockEntity.getCustomName();
        Font font = mc.font;
        float textWidth = font.width(name);

        float nameY = 5.0f / 16.0f * scale + 0.15f;

        poseStack.pushPose();
        poseStack.translate(0.5, nameY, 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int bgColor = (int)(bgOpacity * 255.0f) << 24;
        float x = -textWidth / 2;

        font.drawInBatch(name, x, 0, 0x20FFFFFF, false, matrix, buffer,
            Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        font.drawInBatch(name, x, 0, 0xFFFFFFFF, false, matrix, buffer,
            Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ApiBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ApiBlockEntity blockEntity) {
        float scale = blockEntity.getCompletedScale();
        var pos = blockEntity.getBlockPos();
        double halfExtent = scale * 0.3125;
        return new AABB(
            pos.getX() + 0.5 - halfExtent, pos.getY(), pos.getZ() + 0.5 - halfExtent,
            pos.getX() + 0.5 + halfExtent, pos.getY() + scale * 0.3125, pos.getZ() + 0.5 + halfExtent
        );
    }
}
