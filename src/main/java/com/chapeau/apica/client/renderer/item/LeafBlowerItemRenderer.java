/**
 * ============================================================
 * [LeafBlowerItemRenderer.java]
 * Description: BEWLR pour le Leaf Blower — rendu 3D avec animation de charge frame-by-frame
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimatedQuadRenderer    | Rendu quads UV       | Overlay charging + barres      |
 * | AnimationTimer          | Temps client         | Tracking frame animation       |
 * | BakedModel              | Modele 3D body       | Rendu statique via putBulkData |
 * | LeafBlowerItem          | Detection item       | Constantes charge              |
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
import com.chapeau.apica.client.renderer.util.AnimatedQuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
 * BEWLR pour le Leaf Blower. Rendu hybride en 3 couches:
 * 1. Body statique (baked model via putBulkData)
 * 2. Charging overlay (quads manuels avec UV dynamique, 13 frames)
 * 3. Charge bars (quads manuels rendus conditionnellement, 3 barres)
 *
 * L'animation de charge joue forward pendant le chargement (+1 frame/tick)
 * et reverse au relachement (-1 frame/tick).
 */
@OnlyIn(Dist.CLIENT)
public class LeafBlowerItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Modele bake du body (elements 1-6, 10, 11) */
    public static final ModelResourceLocation BODY_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/leaf_blower_body"));

    /** Texture de l'overlay interieur (13 frames, atlas 13x78) */
    private static final ResourceLocation CHARGING1_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/leaf_blower_charging1.png");

    /** Texture des barres indicatrices (4 frames, atlas 3x24) */
    private static final ResourceLocation CHARGING2_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/leaf_blower_charging2.png");

    // --- Overlay box (element 0 du Blockbench) en unites bloc (px / 16) ---
    private static final float OVL_MIN_X = 9.75f / 16f;
    private static final float OVL_MIN_Y = 11.75f / 16f;
    private static final float OVL_MIN_Z = 1.5f / 16f;
    private static final float OVL_MAX_X = 14.25f / 16f;
    private static final float OVL_MAX_Y = 16.25f / 16f;
    private static final float OVL_MAX_Z = 14.5f / 16f;

    // --- Bar boxes (elements 7-9) en unites bloc ---
    private static final float BAR_MIN_X = 9f / 16f;
    private static final float BAR_MIN_Y = 11f / 16f;
    private static final float BAR_MAX_X = 15f / 16f;
    private static final float BAR_MAX_Y = 17f / 16f;
    // Bar 0: z = 3-4, Bar 1: z = 5-6, Bar 2: z = 7-8
    private static final float[] BAR_Z_MIN = { 3f / 16f, 5f / 16f, 7f / 16f };
    private static final float[] BAR_Z_MAX = { 4f / 16f, 6f / 16f, 8f / 16f };
    // UV U ranges per bar (columns dans l'atlas 3px wide)
    private static final float[] BAR_U0 = { 0f / 3f, 1f / 3f, 2f / 3f };
    private static final float[] BAR_U1 = { 1f / 3f, 2f / 3f, 3f / 3f };

    private static final int TOTAL_FRAMES = 13;
    private static final int CHARGE_TIER2_TICKS = 20;
    private static final int CHARGE_TIER3_TICKS = 40;

    // Animation state (client-side, shared across renders since single player)
    private int currentFrame = 0;
    private int lastTick = -1;

    public LeafBlowerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        // Couche 1: body statique (baked model)
        renderBodyModel(poseStack, buffer, packedLight, packedOverlay, stack);

        // Mettre a jour l'animation
        updateAnimation();

        // Couche 2: charging overlay (si frame > 0)
        if (currentFrame > 0) {
            renderChargingOverlay(poseStack, buffer, packedLight);
        }

        // Couche 3: barres indicatrices (selon le charge level)
        int chargeLevel = getChargeLevel();
        if (chargeLevel > 0) {
            renderChargeBars(poseStack, buffer, packedLight, chargeLevel);
        }
    }

    /**
     * Rend le modele 3D bake du body en iterant sur ses quads (pattern BuildingStaff).
     */
    private void renderBodyModel(PoseStack poseStack, MultiBufferSource buffer,
                                  int packedLight, int packedOverlay, ItemStack stack) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(BODY_MODEL_LOC);
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
     * Met a jour l'animation frame-by-frame.
     * Forward (+1/tick) pendant le chargement, reverse (-1/tick) au relachement.
     */
    private void updateAnimation() {
        int tick = AnimationTimer.getTicks();
        if (tick == lastTick) return;
        lastTick = tick;

        Minecraft mc = Minecraft.getInstance();
        boolean isCharging = mc.player != null && mc.player.isUsingItem()
            && mc.player.getUseItem().getItem() instanceof com.chapeau.apica.common.item.tool.LeafBlowerItem;

        if (isCharging) {
            // Forward: increment frame
            if (currentFrame < TOTAL_FRAMES - 1) {
                currentFrame++;
            }
        } else {
            // Reverse: decrement frame
            if (currentFrame > 0) {
                currentFrame--;
            }
        }

    }

    /**
     * Rend l'overlay interieur (element 0) avec UV dynamiques selectionnant la frame courante.
     */
    private void renderChargingOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CHARGING1_TEXTURE));

        float v0 = (float) currentFrame / TOTAL_FRAMES;
        float v1 = v0 + 1f / TOTAL_FRAMES;

        AnimatedQuadRenderer.renderBox(vc, poseStack.last(),
            OVL_MIN_X, OVL_MIN_Y, OVL_MIN_Z,
            OVL_MAX_X, OVL_MAX_Y, OVL_MAX_Z,
            0f, v0, 1f, v1, packedLight);
    }

    /**
     * Rend les barres indicatrices de charge.
     * Bar 0 si level >= 1, Bar 1 si level >= 2, Bar 2 si level == 3.
     */
    private void renderChargeBars(PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int chargeLevel) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CHARGING2_TEXTURE));

        // V range: toujours premiere frame (frame 0 = barres allumees)
        float v0 = 0f;
        float v1 = 0.25f;

        for (int i = 0; i < chargeLevel && i < 3; i++) {
            AnimatedQuadRenderer.renderBox(vc, poseStack.last(),
                BAR_MIN_X, BAR_MIN_Y, BAR_Z_MIN[i],
                BAR_MAX_X, BAR_MAX_Y, BAR_Z_MAX[i],
                BAR_U0[i], v0, BAR_U1[i], v1, packedLight);
        }
    }

    /**
     * Calcule le niveau de charge (0-3) base sur les ticks d'utilisation du joueur.
     */
    private int getChargeLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isUsingItem()) return 0;
        if (!(mc.player.getUseItem().getItem() instanceof com.chapeau.apica.common.item.tool.LeafBlowerItem)) {
            return 0;
        }

        int useTicks = mc.player.getTicksUsingItem();
        if (useTicks >= CHARGE_TIER3_TICKS) return 3;
        if (useTicks >= CHARGE_TIER2_TICKS) return 2;
        return 1;
    }
}
