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
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.renderer.util.FluidCubeRenderer;
import com.chapeau.apica.client.renderer.util.RenderHelper;
import com.chapeau.apica.common.block.altar.HoneyReservoirBlock;
import com.chapeau.apica.common.blockentity.alchemy.AlembicHeartBlockEntity;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.common.blockentity.altar.HoneyReservoirBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

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
            Apica.MOD_ID, "block/multibloc/altar/honey_reservoir_altar_render"));

    public HoneyReservoirRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(HoneyReservoirBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        MultiblockProperty multiblock = state.getValue(HoneyReservoirBlock.MULTIBLOCK);
        boolean formed = !multiblock.equals(MultiblockProperty.NONE);
        float spreadX = blockEntity.getFormedSpreadX();
        float spreadZ = blockEntity.getFormedSpreadZ();
        boolean hasSpread = spreadX != 0.0f || spreadZ != 0.0f;

        // Storage: intermediaire pur (delegation vers controller, pas de rendu direct)
        if (multiblock == MultiblockProperty.STORAGE || multiblock == MultiblockProperty.STORAGE_TOP) {
            return;
        }

        // Rendre le modèle formé avec spread via renderer (altar uniquement)
        if (multiblock == MultiblockProperty.ALTAR && hasSpread) {
            renderFormedModel(blockEntity, poseStack, buffer, packedLight, packedOverlay, spreadX, spreadZ);
        }

        /**
         * ╔══════════════════════════════════════════════════════════════════════╗
         * ║   ⚠️ LE RESERVOIR NE STOCKE JAMAIS DE FLUIDE LOCALEMENT ⚠️          ║
         * ║   Toutes les données fluide viennent du CONTRÔLEUR multibloc.       ║
         * ╚══════════════════════════════════════════════════════════════════════╝
         */
        FluidStack fluidStack;
        float fillRatio;

        if (multiblock == MultiblockProperty.ALEMBIC || multiblock == MultiblockProperty.ALEMBIC_0 || multiblock == MultiblockProperty.ALEMBIC_1) {
            // Query controller for Alembic reservoirs
            AlembicFluidData alembicData = getAlembicFluidData(blockEntity, multiblock);
            if (alembicData == null || alembicData.fluidStack.isEmpty()) {
                return;
            }
            fluidStack = alembicData.fluidStack;
            fillRatio = alembicData.fillRatio;
        } else if (multiblock == MultiblockProperty.EXTRACTOR
                || multiblock == MultiblockProperty.CENTRIFUGE
                || multiblock == MultiblockProperty.INFUSER) {
            // Ces multiblocs utilisent le cache visuel synchronisé par leur contrôleur
            fluidStack = blockEntity.getVisualFluid();
            if (fluidStack.isEmpty()) {
                return;
            }
            fillRatio = blockEntity.getVisualFillRatio();
        } else if (multiblock == MultiblockProperty.ALTAR) {
            // Altar n'utilise pas de fluide dans ses recettes, pas de rendu
            return;
        } else {
            // NONE (standalone) - pas de contrôleur = pas de fluide
            return;
        }

        TextureAtlasSprite sprite = RenderHelper.getFluidSprite(fluidStack.getFluid());

        poseStack.pushPose();

        // Appliquer le spread au fluide aussi
        if (hasSpread) {
            poseStack.translate(spreadX, 0, spreadZ);
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        // Rendu spécifique selon le type de multibloc
        // ALEMBIC (lateraux) = simple colonne haute (input)
        // ALEMBIC_0 (bottom) = cube court avec tubes (output)
        switch (multiblock) {
            case ALEMBIC, ALEMBIC_1 -> renderAlembicBottomFluid(poseStack, consumer, sprite, fillRatio);
            case ALEMBIC_0 -> renderAlembicLateralFluid(poseStack, consumer, sprite, fillRatio);
            default -> {
                if (formed) {
                    renderFormedFluid(poseStack, consumer, sprite, fillRatio);
                } else {
                    renderUnformedFluid(poseStack, consumer, sprite, fillRatio);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * Data structure pour les données fluide de l'Alembic.
     */
    private record AlembicFluidData(FluidStack fluidStack, float fillRatio) {}

    /**
     * Récupère les données fluide depuis le contrôleur Alembic selon la position du réservoir.
     * - ALEMBIC_0 (Y=-1, bas) : nectar tank
     * - ALEMBIC latéraux : honey ou royal jelly selon offset X
     */
    @Nullable
    private AlembicFluidData getAlembicFluidData(HoneyReservoirBlockEntity reservoir, MultiblockProperty multiblock) {
        BlockPos controllerPos = reservoir.getControllerPos();
        if (controllerPos == null || reservoir.getLevel() == null) {
            return null;
        }

        BlockEntity be = reservoir.getLevel().getBlockEntity(controllerPos);
        if (!(be instanceof AlembicHeartBlockEntity alembic)) {
            return null;
        }

        BlockPos reservoirPos = reservoir.getBlockPos();
        int offsetX = reservoirPos.getX() - controllerPos.getX();
        int offsetY = reservoirPos.getY() - controllerPos.getY();
        int offsetZ = reservoirPos.getZ() - controllerPos.getZ();

        // Ajuster pour la rotation du multibloc
        int rotation = alembic.getRotation();
        int[] rotated = rotateOffsetInverse(offsetX, offsetZ, rotation);
        int patternX = rotated[0];

        FluidTank tank;
        if (multiblock == MultiblockProperty.ALEMBIC_0 || offsetY == -1) {
            // Réservoir du bas → nectar
            tank = alembic.getNectarTank();
        } else if (patternX < 0) {
            // Réservoir gauche (pattern X=-1) → honey
            tank = alembic.getHoneyTank();
        } else {
            // Réservoir droit (pattern X=+1) → royal jelly
            tank = alembic.getRoyalJellyTank();
        }

        if (tank.isEmpty()) {
            return null;
        }

        float ratio = (float) tank.getFluidAmount() / tank.getCapacity();
        return new AlembicFluidData(tank.getFluid(), ratio);
    }

    /**
     * Inverse la rotation pour récupérer les coordonnées pattern depuis les coordonnées monde.
     */
    private int[] rotateOffsetInverse(int x, int z, int rotation) {
        // Rotation inverse: (4 - rotation) % 4
        int invRot = (4 - rotation) & 3;
        return switch (invRot) {
            case 1 -> new int[] { z, -x };   // 90° CCW
            case 2 -> new int[] { -x, -z };  // 180°
            case 3 -> new int[] { -z, x };   // 270° CCW
            default -> new int[] { x, z };   // 0°
        };
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

        RenderHelper.tesselateModel(blockRenderer, formedModel, blockEntity.getLevel(),
            state, blockEntity.getBlockPos(), poseStack, vertexConsumer, random,
            packedLight, packedOverlay, RenderType.solid());

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

    /**
     * Rend le fluide pour le réservoir bottom de l'Alembic (ALEMBIC_0).
     * Utilise le modèle tubes: Glass [2, 2, 2] à [14, 14, 14] - fluide à l'intérieur avec marge de 1px.
     */
    private void renderAlembicLateralFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                            float fillRatio) {
        var pose = poseStack.last();

        // Fluide à l'intérieur du verre: [3, 3, 3] à [13, 3+10*fillRatio, 13]
        float fluidHeight = 10.0f * fillRatio;
        float minX = 3f / 16f;
        float maxX = 13f / 16f;
        float minZ = 3f / 16f;
        float maxZ = 13f / 16f;
        float minY = 3f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Rend le fluide pour les réservoirs latéraux de l'Alembic (ALEMBIC, ALEMBIC_1).
     * Utilise le modèle colonne: Glass [2, 4, 2] à [14, 24, 14] - colonne haute, fluide à l'intérieur.
     */
    private void renderAlembicBottomFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                           float fillRatio) {
        var pose = poseStack.last();

        // Fluide à l'intérieur du verre: [3, 5, 3] à [13, 5+18*fillRatio, 13]
        // La colonne fait 20 pixels de haut (Y=4 à Y=24), fluide sur 18 pixels
        float fluidHeight = 18.0f * fillRatio;
        float minX = 3f / 16f;
        float maxX = 13f / 16f;
        float minZ = 3f / 16f;
        float maxZ = 13f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
