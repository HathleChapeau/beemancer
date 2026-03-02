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
import com.chapeau.apica.client.animation.AnimationTimer;
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
 */
@OnlyIn(Dist.CLIENT)
public class ChopperCubeItemRenderer extends BlockEntityWithoutLevelRenderer {

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
    private static final int OPENING_TICKS = 15;

    /** Separation maximale des slabs (3 pixels = 3/16 bloc). */
    private static final float MAX_SEPARATION = 3f / 16f;

    /** Amplitude du bobbing du centre (0.5 pixels). */
    private static final float BOB_AMPLITUDE = 0.5f / 16f;

    /** Rayon d'orbite des abeilles (en unites bloc item). */
    private static final float BEE_ORBIT_RADIUS = 0.35f;

    /** Echelle des abeilles. */
    private static final float BEE_SCALE = 0.2f;

    /** Centre Y pour l'orbite des abeilles (compense le flip 180 du BeeModel). */
    private static final float CENTER_Y = 12f / 16f;

    /** Centre XZ du modele (8/16 = 0.5). */
    private static final float CENTER_XZ = 8f / 16f;

    private static final int FULL_BRIGHT = 0xF000F0;

    /** BeeModel lazy-init. */
    private BeeModel<?> beeModel;

    /** Animation state: tick de debut de l'animation (-1 = pas en cours). */
    private int animStartTick = -1;

    /** Dernier tick ou on a rendu en first person (pour detecter la fin du FP). */
    private int lastFpRenderTick = -1;

    public ChopperCubeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
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
            animStartTick = -1;
        }

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
     * Rendu anime en first person.
     */
    private void renderAnimated(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight, int packedOverlay) {
        int currentTick = AnimationTimer.getTicks();
        int rawElapsed = currentTick - animStartTick;
        if (rawElapsed < 0) rawElapsed = 0;

        float time = (float) currentTick;

        // Delay de 1 seconde avant le debut de l'animation
        int elapsed = rawElapsed - ANIM_DELAY_TICKS;

        if (elapsed < 0) {
            // Pendant le delay: rendu statique avec slabs jointes au centre
            poseStack.pushPose();
            poseStack.translate(0, MAX_SEPARATION, 0);
            renderBakedModel(BOTTOM_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0, -MAX_SEPARATION, 0);
            renderBakedModel(TOP_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();

            renderBakedModel(CENTER_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        // Phase d'ouverture: ease out (1 - (1-t)^2)
        // Les slabs commencent jointes au centre (offset = MAX) puis s'ecartent (offset = 0)
        float openProgress;
        if (elapsed >= OPENING_TICKS) {
            openProgress = 1.0f;
        } else {
            float t = (float) elapsed / OPENING_TICKS;
            openProgress = 1.0f - (1.0f - t) * (1.0f - t);
        }

        // offset diminue de MAX_SEPARATION (jointes au centre) a 0 (position du modele)
        float offset = MAX_SEPARATION * (1.0f - openProgress);

        // Bottom slab: decale vers le haut (vers le centre) au debut
        poseStack.pushPose();
        poseStack.translate(0, offset, 0);
        renderBakedModel(BOTTOM_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Top slab: decale vers le bas (vers le centre) au debut
        poseStack.pushPose();
        poseStack.translate(0, -offset, 0);
        renderBakedModel(TOP_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Centre cube: bobbing subtil (actif des l'ouverture complete)
        poseStack.pushPose();
        if (elapsed >= OPENING_TICKS) {
            float bob = (float) Math.sin(time * 0.053) * BOB_AMPLITUDE;
            poseStack.translate(0, bob, 0);
        }
        renderBakedModel(CENTER_MODEL_LOC, stack, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Abeilles orbitantes (apparaissent apres l'ouverture)
        if (elapsed >= OPENING_TICKS) {
            int beeElapsed = elapsed - OPENING_TICKS;
            float beeAlpha = Math.min(1.0f, beeElapsed / 10.0f);
            renderOrbitingBees(poseStack, buffer, time, beeAlpha);
        }
    }

    /**
     * Rend 2 petites abeilles orbitant autour du centre du cube.
     */
    private void renderOrbitingBees(PoseStack poseStack, MultiBufferSource buffer,
                                     float time, float alpha) {
        BeeModel<?> model = getOrCreateBeeModel();
        RenderType beeRenderType = RenderType.entityCutout(BEE_TEXTURE);
        VertexConsumer vc = buffer.getBuffer(beeRenderType);

        // Le rayon grandit de 0 au rayon final pendant le fade-in (alpha 0→1)
        float radius = BEE_ORBIT_RADIUS * alpha;

        for (int i = 0; i < 2; i++) {
            double angle = time * 0.15 + i * Math.PI;

            float beeX = CENTER_XZ + (float) Math.cos(angle) * radius;
            float beeZ = CENTER_XZ + (float) Math.sin(angle) * radius;
            float beeY = CENTER_Y;

            poseStack.pushPose();
            poseStack.translate(beeX, beeY, beeZ);

            // Rotation face tangentielle + 90deg supplementaires
            float yRot = (float) Math.toDegrees(angle) + 0;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

            // Flip pour convention entity models
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
