/**
 * ============================================================
 * [HoneyReservoirRenderer.java]
 * Description: Renderer pour le fluide du Honey Reservoir avec texture atlas animée
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation           |
 * |----------------------------|----------------------|-----------------------|
 * | HoneyReservoirBlockEntity  | Données fluide       | getFluidAmount()      |
 * | HoneyReservoirBlock        | État formé           | FORMED                |
 * | BlockEntityRenderer        | Interface renderer   | Rendu fluide          |
 * | IClientFluidTypeExtensions | Texture atlas        | getStillTexture()     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renderer pour le fluide à l'intérieur du Honey Reservoir.
 * Utilise les textures atlas animées des fluides pour un rendu dynamique.
 * Rendu fullbright sans tint pour afficher la texture telle quelle.
 * Gère aussi le rendu du modèle formé avec spread offset pour le multibloc Storage Controller.
 */
public class HoneyReservoirRenderer implements BlockEntityRenderer<HoneyReservoirBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation FORMED_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/altar/honey_reservoir_formed_render"));

    public HoneyReservoirRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(HoneyReservoirBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        boolean formed = state.getValue(HoneyReservoirBlock.FORMED);
        float spreadX = blockEntity.getFormedSpreadX();
        float spreadZ = blockEntity.getFormedSpreadZ();
        boolean hasSpread = spreadX != 0.0f || spreadZ != 0.0f;

        // Rendre le modèle formé avec spread via renderer (le blockstate formed model est vide)
        if (formed && hasSpread) {
            renderFormedModel(blockEntity, poseStack, buffer, packedLight, packedOverlay, spreadX, spreadZ);
        }

        // Rendre le fluide
        FluidStack fluidStack = blockEntity.getFluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        float fillRatio = (float) blockEntity.getFluidAmount() / HoneyReservoirBlockEntity.CAPACITY;
        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        poseStack.pushPose();

        // Appliquer le spread au fluide aussi
        if (hasSpread) {
            poseStack.translate(spreadX, 0, spreadZ);
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        if (formed) {
            renderFormedFluid(poseStack, consumer, sprite, fillRatio);
        } else {
            renderUnformedFluid(poseStack, consumer, sprite, fillRatio);
        }

        poseStack.popPose();
    }

    /**
     * Rend le modèle formé du réservoir avec le spread offset.
     */
    private void renderFormedModel(HoneyReservoirBlockEntity blockEntity, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                    float spreadX, float spreadZ) {
        BakedModel formedModel = Minecraft.getInstance().getModelManager()
            .getModel(FORMED_MODEL_LOC);

        BlockState state = blockEntity.getBlockState();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();
        poseStack.translate(spreadX, 0, spreadZ);

        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            formedModel,
            state,
            blockEntity.getBlockPos(),
            poseStack,
            vertexConsumer,
            false,
            random,
            packedLight,
            packedOverlay,
            ModelData.EMPTY,
            RenderType.solid()
        );

        poseStack.popPose();
    }

    /**
     * Rend le fluide pour le réservoir non-formé.
     */
    private void renderUnformedFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                      float fillRatio) {
        var pose = poseStack.last();

        float fluidHeight = 6.0f * fillRatio;
        float minX = 4f / 16f;
        float maxX = 12f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        renderFluidCubeWithSprite(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Rend le fluide pour le réservoir formé.
     */
    private void renderFormedFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    float fillRatio) {
        var pose = poseStack.last();

        float fluidHeight = 6.0f * fillRatio;
        float minX = 3f / 16f;
        float maxX = 13f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        renderFluidCubeWithSprite(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Rend un cube de fluide avec texture atlas sprite animée.
     * Fullbright (pas d'ombrage), couleur blanche (pas de tint).
     */
    private void renderFluidCubeWithSprite(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                            float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ) {

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.9f;

        int light = LightTexture.FULL_BRIGHT;

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
    }
}
