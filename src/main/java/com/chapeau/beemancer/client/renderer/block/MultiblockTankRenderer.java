/**
 * ============================================================
 * [MultiblockTankRenderer.java]
 * Description: Renderer pour le multiblock tank avec modèle JSON scalé
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation               |
 * |-------------------------------|----------------------|---------------------------|
 * | MultiblockTankBlockEntity     | Données fluide/taille| getFluidTank(), getCubeSize |
 * | FluidCubeRenderer             | Rendu cube fluide    | renderFluidCube()         |
 * | IClientFluidTypeExtensions    | Texture atlas        | getStillTexture()         |
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
import com.chapeau.beemancer.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.phys.AABB;

/**
 * Renderer pour le multiblock tank.
 * Rendu uniquement sur le master (coin min X/Y/Z).
 * Charge le modèle JSON et le scale selon la taille du cube.
 */
public class MultiblockTankRenderer implements BlockEntityRenderer<MultiblockTankBlockEntity> {

    // Modèle formé (scalé par le renderer)
    public static final ModelResourceLocation FORMED_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/alchemy/multiblock_tank_formed"));

    // Modèle bloc simple (non formé)
    public static final ModelResourceLocation SINGLE_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/alchemy/multiblock_tank_single"));

    private static final float FLUID_INSET = 2f / 16f;

    public MultiblockTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MultiblockTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Cas 1: Non formé → afficher le bloc simple
        if (!blockEntity.isFormed()) {
            renderSingleBlock(poseStack, buffer, packedLight);
            return;
        }

        // Cas 2: Formé mais pas master → invisible (le master gère tout)
        if (!blockEntity.isMaster()) return;

        // Cas 3: Formé et master → afficher le modèle scalé + fluide
        int cubeSize = blockEntity.getCubeSize();
        if (cubeSize < 2) {
            // Fallback: si cubeSize pas encore synced, afficher bloc simple
            renderSingleBlock(poseStack, buffer, packedLight);
            return;
        }

        // Le modèle fait 32 pixels (2 blocs), donc scale = cubeSize / 2
        float scale = cubeSize / 2.0f;

        // Render le modèle formé (scalé)
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderFormedModel(poseStack, buffer, packedLight);
        poseStack.popPose();

        // Render le fluide (coordonnées monde, pas scalé)
        renderFluid(blockEntity, cubeSize, poseStack, buffer);
    }

    private void renderSingleBlock(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(SINGLE_MODEL_LOC);

        if (model == null || model == modelManager.getMissingModel()) return;

        var blockRenderer = minecraft.getBlockRenderer().getModelRenderer();

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        blockRenderer.renderModel(
            poseStack.last(),
            consumer,
            null,
            model,
            1f, 1f, 1f,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.solid()
        );
    }

    private void renderFormedModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(FORMED_MODEL_LOC);

        if (model == null || model == modelManager.getMissingModel()) return;

        var blockRenderer = minecraft.getBlockRenderer().getModelRenderer();

        // Le modèle est entièrement translucent (render_type dans le JSON)
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        blockRenderer.renderModel(
            poseStack.last(),
            consumer,
            null,
            model,
            1f, 1f, 1f,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.translucent()
        );
    }

    private void renderFluid(MultiblockTankBlockEntity blockEntity, int cubeSize, PoseStack poseStack, MultiBufferSource buffer) {
        FluidTank tank = blockEntity.getFluidTank();
        if (tank == null || tank.isEmpty()) return;

        FluidStack fluidStack = tank.getFluid();
        float fillRatio = (float) tank.getFluidAmount() / tank.getCapacity();
        if (fillRatio <= 0f) return;

        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        // Coordonnées en espace monde (blocs), scalées par cubeSize
        // Le modèle: glass de [3,10,3] à [29,32,29] sur 32 pixels (2 blocs de base)
        // Ratio pour convertir: cubeSize / 2
        float scale = cubeSize / 2.0f;

        // Marge de 1 pixel à l'intérieur du glass (4 pixels depuis le bord du modèle)
        float inset = (4f / 32f) * cubeSize;  // 4 pixels sur 32 = ratio * cubeSize

        float minX = inset;
        float maxX = cubeSize - inset;
        float minZ = inset;
        float maxZ = cubeSize - inset;

        // Y: base du glass à 10/32 du modèle, top à 32/32
        // Fluide commence à 11/32, finit à 31/32
        float baseY = (11f / 32f) * cubeSize;  // Juste au-dessus du bord
        float topY = (31f / 32f) * cubeSize;   // Juste en dessous du top

        float minY = baseY;
        float maxY = baseY + (topY - baseY) * fillRatio;

        var pose = poseStack.last();
        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite,
            minX, minY, minZ, maxX, maxY, maxZ,
            true, true, true, true, true, true);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockTankBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MultiblockTankBlockEntity blockEntity) {
        if (!blockEntity.isMaster() || !blockEntity.isFormed()) {
            return AABB.unitCubeFromLowerCorner(blockEntity.getBlockPos().getCenter());
        }

        int cubeSize = blockEntity.getCubeSize();
        if (cubeSize < 2) {
            return AABB.unitCubeFromLowerCorner(blockEntity.getBlockPos().getCenter());
        }

        var pos = blockEntity.getBlockPos();
        return new AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + cubeSize, pos.getY() + cubeSize, pos.getZ() + cubeSize
        );
    }
}
