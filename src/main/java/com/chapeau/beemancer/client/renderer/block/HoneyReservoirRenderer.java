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
 * | FluidCubeRenderer          | Rendu cube fluide    | renderFluidCube()     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.renderer.util.FluidCubeRenderer;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
        boolean formed = !state.getValue(HoneyReservoirBlock.MULTIBLOCK).equals(MultiblockProperty.NONE);
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
        float minX = 5f / 16f;
        float maxX = 11f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
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

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
