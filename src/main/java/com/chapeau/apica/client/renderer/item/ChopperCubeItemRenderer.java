/**
 * ============================================================
 * [ChopperCubeItemRenderer.java]
 * Description: BEWLR pour le Chopper Cube — animation first person avec slabs, bees et bobbing
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimationController     | Systeme d'animation  | Gestion animations nommees     |
 * | MoveAnimation           | Translations animees | Ouverture slabs + bobbing      |
 * | AnimationTimer          | Temps client         | Tracking animation ticks       |
 * | BakedModel              | Modeles 3D           | Rendu statique via putBulkData |
 * | BeeModel                | Modele abeille       | Rendu mini-abeilles orbitantes |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement BEWLR + modeles additionnels)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.animation.IAnimatable;
import com.chapeau.apica.client.animation.MoveAnimation;
import com.chapeau.apica.client.animation.TimingEffect;
import com.chapeau.apica.client.animation.TimingType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * BEWLR pour le Chopper Cube.
 * En first person: slabs s'ecartent, 2 abeilles orbitent, cube central bobbing.
 * Tous les autres contextes: rendu statique des 3 parties.
 *
 * Utilise le systeme d'animation Apica (AnimationController + MoveAnimation)
 * pour les translations avec easing. Les abeilles restent procedurales (orbite circulaire).
 */
@OnlyIn(Dist.CLIENT)
public class ChopperCubeItemRenderer extends BlockEntityWithoutLevelRenderer implements IAnimatable {

    /** Modeles standalone pour les 3 parties. */
    public static final ModelResourceLocation BOTTOM_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/chopper_cube_bottom"));

    public static final ModelResourceLocation CENTER_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/chopper_cube_center"));

    public static final ModelResourceLocation TOP_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/chopper_cube_top"));

    /** Texture vanilla de l'abeille. */
    private static final ResourceLocation BEE_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    /** Delay avant le debut de l'animation. */
    private static final int ANIM_DELAY_TICKS = 10;

    /** Duree de la phase d'ouverture en ticks. */
    private static final float OPENING_TICKS = 15f;

    /** Separation maximale des slabs (3 pixels = 3/16 bloc). */
    private static final float MAX_SEPARATION = 2f / 16f;

    /** Amplitude du bobbing du centre (0.5 pixels). */
    private static final float BOB_AMPLITUDE = 0.5f / 16f;

    /** Duree d'un demi-cycle de bobbing (equivalent a sin(t*0.053) period/2 ≈ 60 ticks). */
    private static final float BOB_DURATION = 60f;

    /** Rayon d'orbite des abeilles (en unites bloc item). */
    private static final float BEE_ORBIT_RADIUS = 0.35f;

    private static final float BEE_ORBIT_SPEED = 0.15f;

    /** Echelle des abeilles. */
    private static final float BEE_SCALE = 0.20f;

    /** Centre Y pour l'orbite des abeilles (compense le flip 180 du BeeModel). */
    private static final float CENTER_Y = 12f / 16f;

    /** Centre XZ du modele (8/16 = 0.5). */
    private static final float CENTER_XZ = 8f / 16f;

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Noms des animations enregistrees dans le controller. */
    private static final String ANIM_BOTTOM_OPEN = "bottom_open";
    private static final String ANIM_TOP_OPEN = "top_open";
    private static final String ANIM_CENTER_BOB = "center_bob";

    /** Controller d'animation Apica. */
    private final AnimationController animController = new AnimationController();

    /** BeeModel lazy-init. */
    private BeeModel<?> beeModel;

    /** Animation state: tick de debut de l'animation (-1 = pas en cours). */
    private int animStartTick = -1;

    /** Dernier tick ou on a rendu en first person (pour detecter la fin du FP). */
    private int lastFpRenderTick = -1;

    /** Flags pour eviter de relancer les animations chaque frame. */
    private boolean openingStarted = false;
    private boolean bobbingStarted = false;

    public ChopperCubeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
        initAnimations();
    }

    @Override
    public AnimationController getAnimationController() {
        return animController;
    }

    /**
     * Enregistre les 3 animations nommees dans le controller.
     */
    private void initAnimations() {
        animController.createAnimation(ANIM_BOTTOM_OPEN,
            MoveAnimation.builder()
                .from(0, MAX_SEPARATION, 0)
                .to(0, 0, 0)
                .duration(OPENING_TICKS)
                .timingType(TimingType.EASE_OUT)
                .resetAfterAnimation(false)
                .build());

        animController.createAnimation(ANIM_TOP_OPEN,
            MoveAnimation.builder()
                .from(0, -MAX_SEPARATION, 0)
                .to(0, 0, 0)
                .duration(OPENING_TICKS)
                .timingType(TimingType.EASE_OUT)
                .resetAfterAnimation(false)
                .build());

        animController.createAnimation(ANIM_CENTER_BOB,
            MoveAnimation.builder()
                .from(0, -BOB_AMPLITUDE, 0)
                .to(0, BOB_AMPLITUDE, 0)
                .duration(BOB_DURATION)
                .timingType(TimingType.SLOW_IN_SLOW_OUT)
                .timingEffect(TimingEffect.BOOMERANG)
                .build());
    }

    /**
     * Reset complet de l'etat d'animation (quand on quitte le first person).
     */
    private void resetAnimations() {
        animStartTick = -1;
        openingStarted = false;
        bobbingStarted = false;
        animController.stopAnimation(ANIM_BOTTOM_OPEN);
        animController.stopAnimation(ANIM_TOP_OPEN);
        animController.stopAnimation(ANIM_CENTER_BOB);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        boolean isFirstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                             || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        int currentTick = AnimationTimer.getTicks();

        // Reset animation si on n'a pas rendu en FP depuis plus de 2 ticks
        if (lastFpRenderTick >= 0 && currentTick - lastFpRenderTick > 2) {
            resetAnimations();
        }

        animController.tick((float) currentTick);

        if (isFirstPerson) {
            if (animStartTick < 0) {
                animStartTick = currentTick;
            }
            lastFpRenderTick = currentTick;
            renderAnimated(stack, poseStack, buffer, packedLight, packedOverlay);
        } else {
            renderStatic(stack, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    /**
     * Rendu statique: les 3 parties a leurs positions par defaut.
     */
    private void renderStatic(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
                               int packedLight, int packedOverlay) {
        renderBakedModel(BOTTOM_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        renderBakedModel(CENTER_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        renderBakedModel(TOP_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rendu anime en first person via AnimationController.
     */
    private void renderAnimated(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight, int packedOverlay) {
        int currentTick = AnimationTimer.getTicks();
        int rawElapsed = currentTick - animStartTick;
        if (rawElapsed < 0) rawElapsed = 0;

        float time = (float) currentTick;
        int elapsed = rawElapsed - ANIM_DELAY_TICKS;

        // Lancer les animations d'ouverture apres le delay
        if (elapsed >= 0 && !openingStarted) {
            animController.playAnimation(ANIM_BOTTOM_OPEN);
            animController.playAnimation(ANIM_TOP_OPEN);
            openingStarted = true;
        }

        // Lancer le bobbing apres l'ouverture complete
        if (elapsed >= (int) OPENING_TICKS && !bobbingStarted) {
            animController.playAnimation(ANIM_CENTER_BOB);
            bobbingStarted = true;
        }

        // Bottom slab
        poseStack.pushPose();
        if (elapsed < 0) {
            poseStack.translate(0, MAX_SEPARATION, 0);
        } else {
            animController.applyAnimation(ANIM_BOTTOM_OPEN, poseStack);
        }
        renderBakedModel(BOTTOM_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Top slab
        poseStack.pushPose();
        if (elapsed < 0) {
            poseStack.translate(0, -MAX_SEPARATION, 0);
        } else {
            animController.applyAnimation(ANIM_TOP_OPEN, poseStack);
        }
        renderBakedModel(TOP_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Centre cube avec bobbing
        poseStack.pushPose();
        animController.applyAnimation(ANIM_CENTER_BOB, poseStack);
        renderBakedModel(CENTER_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Abeilles orbitantes (procedurales, apparaissent apres l'ouverture)
        if (elapsed >= (int) OPENING_TICKS) {
            int beeElapsed = elapsed - (int) OPENING_TICKS;
            float beeAlpha = Math.min(1.0f, beeElapsed / 10.0f);
            renderOrbitingBees(poseStack, buffer, time, beeAlpha);
        }
    }

    /**
     * Rend 2 petites abeilles orbitant autour du centre du cube (procedural).
     */
    private void renderOrbitingBees(PoseStack poseStack, MultiBufferSource buffer,
                                     float time, float alpha) {
        BeeModel<?> model = getOrCreateBeeModel();
        RenderType beeRenderType = RenderType.entityCutout(BEE_TEXTURE);
        VertexConsumer vc = buffer.getBuffer(beeRenderType);

        float radius = BEE_ORBIT_RADIUS * alpha;

        for (int i = 0; i < 2; i++) {
            double angle = time * BEE_ORBIT_SPEED + i * Math.PI;

            float beeX = CENTER_XZ + (float) Math.cos(angle) * radius;
            float beeZ = CENTER_XZ + (float) Math.sin(angle) * radius;
            float beeY = CENTER_Y;

            poseStack.pushPose();
            poseStack.translate(beeX, beeY, beeZ);

            float yRot = (float) Math.toDegrees(angle) + 0;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            poseStack.scale(BEE_SCALE, BEE_SCALE, BEE_SCALE);

            model.renderToBuffer(poseStack, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    (int) (alpha * 255) << 24 | 0xFFFFFF);

            poseStack.popPose();
        }
    }

    /**
     * Rend un BakedModel standalone (pattern LeafBlowerItemRenderer).
     */
    private void renderBakedModel(ModelResourceLocation modelLoc, ItemStack stack,
                                   PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLoc);
        if (model == null) return;

        @SuppressWarnings("deprecation")
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffer,
            RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS), true, stack.hasFoil());

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
     * Cree ou retourne le BeeModel en cache.
     */
    private BeeModel<?> getOrCreateBeeModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BEE)
            );
        }
        return beeModel;
    }
}
