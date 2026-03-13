/**
 * ============================================================
 * [BuildingStaffItemRenderer.java]
 * Description: BEWLR pour le Building Staff — rend le modele 3D + bloc dans le cristal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BuildingWandItem        | Detection item       | Verification main hand         |
 * | BakedModel              | Modele 3D staff      | Rendu des quads                |
 * | ItemRenderer            | Rendu bloc           | renderStatic pour mini-bloc    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement BEWLR + modele additionnel)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.renderer.shader.MagazineSweepShader;
import com.chapeau.apica.common.item.tool.BuildingWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

import static com.chapeau.apica.client.renderer.BuildingWandPreviewRenderer.countBlocksInInventory;

/**
 * BEWLR pour le Building Staff.
 * Rend le modele 3D du staff et, quand le joueur vise un bloc valide,
 * affiche une miniature du bloc source a l'interieur du cristal.
 */
@OnlyIn(Dist.CLIENT)
public class BuildingStaffItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static BuildingStaffItemRenderer instance;

    public static BuildingStaffItemRenderer getInstance() {
        return instance;
    }

    public static final ModelResourceLocation STAFF_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/building_staff_3d"));

    // Centre du cristal en model units (pixels / 16)
    private static final float CRYSTAL_CENTER_X = (8f / 16f) + 0.14f;
    private static final float CRYSTAL_CENTER_Y = (20.75f / 16f) + 0.17f;;
    private static final float CRYSTAL_CENTER_Z = (8f / 16f) + 0.14f;;

    // Echelle du mini-bloc dans le cristal (cristal ~3.5px, bloc = 16px, 3.5/16 * 0.8 marge)
    private static final float BLOCK_SCALE = 0.275f;

    // Reload animation (shared)
    private final MagazineReloadAnimator reloadAnimator = new MagazineReloadAnimator();

    public MagazineReloadAnimator getReloadAnimator() {
        return reloadAnimator;
    }

    public BuildingStaffItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
        instance = this;

        //reloadAnimator.HOLD_DURATION = 0f;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        boolean inHand = isHandContext(displayContext);

        float currentTime = AnimationTimer.getRenderTime(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));

        // Tick et applique l'animation de reload
        reloadAnimator.tick(currentTime);
        boolean animating = reloadAnimator.isAnimating();
        if (animating) {
            poseStack.pushPose();
            reloadAnimator.apply(poseStack, currentTime);
        }

        // Rendre le modele 3D du staff
        renderStaffModel(poseStack, buffer, packedLight, packedOverlay, stack, inHand);

        // Rendre le mini-bloc dans le cristal si on vise un bloc valide
        if (inHand) {
            renderCrystalBlock(poseStack, buffer, packedLight, packedOverlay);
        }

        if (animating) {
            poseStack.popPose();
        }
    }

    /**
     * Rend le modele 3D bake du staff en iterant sur ses quads.
     */
    private void renderStaffModel(PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int packedOverlay, ItemStack stack, boolean inHand) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(STAFF_MODEL_LOC);
        if (model == null) return;

        // Use magazine sweep shader only during sweep animation (after reload)
        float currentTime = AnimationTimer.getRenderTime(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        boolean useShader = inHand && MagazineSweepShader.isAvailable() && reloadAnimator.isSweepActive(currentTime);
        RenderType renderType;
        if (useShader) {
            renderType = MagazineSweepShader.getRenderType(TextureAtlas.LOCATION_BLOCKS, stack);
        } else {
            renderType = RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
        }

        @SuppressWarnings("deprecation")
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffer, renderType, true, stack.hasFoil());

        // Appliquer les uniforms APRES avoir obtenu le buffer (le shader est maintenant bind)
        if (useShader) {
            MagazineSweepShader.applyUniforms(stack);
        }

        RandomSource random = RandomSource.create();
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                vc.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
            vc.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
        }
    }

    /**
     * Si le joueur vise un bloc solide, rend une miniature de ce bloc au centre du cristal.
     */
    private void renderCrystalBlock(PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockState sourceState = mc.player.level().getBlockState(blockHit.getBlockPos());
        if (sourceState.isAir()) return;

        // Verifier que le bloc adjacent (ou on placerait) est remplacable
        Direction face = blockHit.getDirection();
        BlockState targetState = mc.player.level().getBlockState(blockHit.getBlockPos().relative(face));
        if (!targetState.canBeReplaced()) return;

        Block sourceBlock = sourceState.getBlock();
        ItemStack blockStack = new ItemStack(sourceBlock);
        if (blockStack.isEmpty()) return;

        // Calculer le count d'inventaire pour la clé de cache
        int inventoryCount = mc.player.isCreative() ? 99999 : countBlocksInInventory(mc.player, sourceBlock);
        if ( inventoryCount <= 0 ) return;

        // Rendre le mini-bloc au centre du cristal
        poseStack.pushPose();
        poseStack.translate(CRYSTAL_CENTER_X, CRYSTAL_CENTER_Y, CRYSTAL_CENTER_Z);
        poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel blockModel = itemRenderer.getModel(blockStack, mc.player.level(), mc.player, 0);
        itemRenderer.render(blockStack, ItemDisplayContext.FIXED, false,
            poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, blockModel);

        poseStack.popPose();
    }

    private static boolean isHandContext(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
