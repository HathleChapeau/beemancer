/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer pour l'animation des conduits et anneaux du Honey Altar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed()            |
 * | AltarConduitAnimator    | Animation conduits   | Calcul orbite         |
 * | DebugWandItem           | Vitesse rotation     | value1                |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.animation.AltarConduitAnimator;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est formé:
 * - Rend les conduits qui orbitent autour de l'altar
 * - Rend les anneaux qui tournent (big ring sur X, small ring sur Z+X)
 * Vitesse de rotation liée à DebugWandItem.value1
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    // Modèles pour les anneaux (enregistrés via ModelEvent.RegisterAdditional)
    private static final ModelResourceLocation BIG_RING_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_heart_big_ring"));
    private static final ModelResourceLocation SMALL_RING_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_heart_small_ring"));

    // Modèle conduit formed via blockstate
    private static final ModelResourceLocation CONDUIT_FORMED_MODEL =
        new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "honey_crystal_conduit"), "facing=north,formed=true");

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Seulement rendre si le multibloc est formé
        if (!blockEntity.isFormed()) {
            return;
        }

        // Vitesses de rotation depuis debug wand
        float conduitSpeed = DebugWandItem.value1;  // Vitesse conduits
        float ringSpeed = DebugWandItem.value2;     // Vitesse anneaux

        // Calculer les angles de rotation basés sur le temps
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float ringAngle = (gameTime + partialTick) * ringSpeed;

        // Rendre les anneaux qui tournent
        renderRotatingRings(blockEntity, ringAngle, poseStack, buffer, packedLight, packedOverlay);

        // Rendre les conduits qui orbitent
        renderOrbitingConduits(blockEntity, partialTick, conduitSpeed, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les 2 anneaux qui tournent.
     * Big ring: tourne sur l'axe X
     * Small ring: tourne sur l'axe Z + hérite la rotation X du big ring
     */
    private void renderRotatingRings(AltarHeartBlockEntity blockEntity, float rotationAngle,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay) {

        // Récupérer les modèles
        BakedModel bigRingModel = Minecraft.getInstance().getModelManager()
            .getModel(BIG_RING_MODEL_LOC);
        BakedModel smallRingModel = Minecraft.getInstance().getModelManager()
            .getModel(SMALL_RING_MODEL_LOC);

        // BlockState pour le rendu (juste pour les propriétés)
        BlockState heartState = BeemancerBlocks.ALTAR_HEART.get().defaultBlockState();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

        // === BIG RING: Rotation sur X et Z (sens positif) ===
        poseStack.pushPose();

        // Centrer pour la rotation
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotation sur l'axe X
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
        // Rotation sur l'axe Z
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationAngle * 0.7f));

        // Revenir au coin
        poseStack.translate(-0.5, -0.5, -0.5);

        // Rendre le big ring
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            bigRingModel,
            heartState,
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

        // === SMALL RING: Rotation sur X et Z (sens opposé) ===
        poseStack.pushPose();

        // Centrer pour la rotation
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotation sur l'axe X (sens opposé)
        poseStack.mulPose(Axis.XP.rotationDegrees(-rotationAngle * 1.3f));
        // Rotation sur l'axe Z (sens opposé)
        poseStack.mulPose(Axis.ZP.rotationDegrees(-rotationAngle));

        // Revenir au coin
        poseStack.translate(-0.5, -0.5, -0.5);

        // Rendre le small ring
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(),
            smallRingModel,
            heartState,
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
     * Rend les 4 conduits qui orbitent autour de l'altar.
     */
    private void renderOrbitingConduits(AltarHeartBlockEntity blockEntity, float partialTick,
                                        float rotationSpeed, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Calculer l'angle avec la vitesse de la debug wand
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float rotationAngle = (gameTime + partialTick) * rotationSpeed;

        // Récupérer le BakedModel du conduit formed
        BakedModel formedModel = Minecraft.getInstance().getModelManager()
            .getModel(CONDUIT_FORMED_MODEL);

        // BlockState pour les propriétés
        BlockState conduitState = BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get().defaultBlockState()
            .setValue(HoneyCrystalConduitBlock.FORMED, true)
            .setValue(HoneyCrystalConduitBlock.FACING, Direction.NORTH);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

        // Rendre les 4 conduits en orbite
        for (int i = 0; i < AltarConduitAnimator.CONDUIT_COUNT; i++) {
            poseStack.pushPose();

            // Calculer la position orbitale du conduit
            Vec3 offset = AltarConduitAnimator.getConduitOffset(i, rotationAngle);
            poseStack.translate(offset.x, offset.y, offset.z);

            // Centrer pour la rotation de facing
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotation pour que le conduit pointe vers le centre
            float facingAngle = AltarConduitAnimator.getConduitFacingAngle(i, rotationAngle);
            poseStack.mulPose(Axis.YP.rotationDegrees(facingAngle));

            // Revenir au coin du bloc
            poseStack.translate(-0.5, -0.5, -0.5);

            // Rendre le modèle
            blockRenderer.getModelRenderer().tesselateBlock(
                blockEntity.getLevel(),
                formedModel,
                conduitState,
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
    }

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
