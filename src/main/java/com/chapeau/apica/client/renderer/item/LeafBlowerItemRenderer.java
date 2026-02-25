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
 * 2. Charging overlay (quads manuels avec UV dynamique par face, 13 frames scrolling)
 * 3. Charge bars (3 barres toujours rendues, bleu=inactif / jaune=actif)
 *
 * L'animation de charge joue forward pendant le chargement (+1 frame/tick)
 * et reverse au relachement (-1 frame/tick).
 * Les barres s'activent de l'avant vers l'arriere (ring 1 → 2 → 3).
 */
@OnlyIn(Dist.CLIENT)
public class LeafBlowerItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Modele bake du body (elements 1-6, 10, 11) */
    public static final ModelResourceLocation BODY_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/leaf_blower_body"));

    /** Texture de l'overlay interieur (13 frames scrolling, atlas 13x78) */
    private static final ResourceLocation CHARGING1_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/leaf_blower_charging1.png");

    /** Texture des barres indicatrices (atlas 3x12: bleu en haut, jaune en bas) */
    private static final ResourceLocation CHARGING2_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/leaf_blower_charging2.png");

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
    // Bar 0: z=3-4 (front, ring 1, 1er pixel column, s'active a charge>=1)
    // Bar 1: z=5-6 (milieu, ring 2, 2e pixel column, s'active a charge>=2)
    // Bar 2: z=7-8 (proche handle, ring 3, 3e pixel column, s'active a charge>=3)
    private static final float[] BAR_Z_MIN = { 3f / 16f, 5f / 16f, 7f / 16f };
    private static final float[] BAR_Z_MAX = { 4f / 16f, 6f / 16f, 8f / 16f };
    // UV U ranges per bar (1 pixel column par ring dans l'atlas 3px wide)
    private static final float[] BAR_U0 = { 0f, 1f / 3f, 2f / 3f };
    private static final float[] BAR_U1 = { 1f / 3f, 2f / 3f, 1f };

    // Atlas 3x12: 4 etats de 3 rows chacun (0.25 en V par etat)
    // Etat 0: rows 0-2, Etat 1: rows 3-5, Etat 2: rows 6-8, Etat 3: rows 9-11
    private static final float BAR_STATE_V_SIZE = 0.25f;
    private static final float BAR_PIXEL_V = 1f / 12f;

    private static final int TOTAL_FRAMES = 13;
    private static final int CHARGE_TIER2_TICKS = 20;
    private static final int CHARGE_TIER3_TICKS = 40;

    // Animation state (client-side)
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
        renderBodyModel(poseStack, buffer, packedLight, packedOverlay, stack);
        updateAnimation();

        renderChargingOverlay(poseStack, buffer, packedLight);

        renderChargeBars(poseStack, buffer, packedLight, getChargeLevel());
    }

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

    private void updateAnimation() {
        int tick = AnimationTimer.getTicks();
        if (tick == lastTick) return;
        lastTick = tick;

        Minecraft mc = Minecraft.getInstance();
        boolean isCharging = mc.player != null && mc.player.isUsingItem()
            && mc.player.getUseItem().getItem() instanceof com.chapeau.apica.common.item.tool.LeafBlowerItem;

        if (isCharging) {
            if (currentFrame < TOTAL_FRAMES - 1) currentFrame++;
        } else {
            if (currentFrame > 0) currentFrame--;
        }
    }

    /**
     * Rend l'overlay interieur avec UV rotations par face (matching Blockbench export).
     * North/South/West: pas de rotation UV.
     * East: rotation 180 (flip U et V).
     * Up: rotation 90.
     * Down: rotation 270.
     */
    private void renderChargingOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CHARGING1_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        float u0 = 0f, u1 = 1f;
        float v0 = (float) currentFrame / TOTAL_FRAMES;
        float v1 = v0 + 1f / TOTAL_FRAMES;

        // North (Z-) — pas de rotation UV
        quad(vc, pose, OVL_MIN_X, OVL_MIN_Y, OVL_MIN_Z, OVL_MAX_X, OVL_MAX_Y, OVL_MIN_Z,
            u0, v1, u0, v0, u1, v0, u1, v1, 0, 0, -1, packedLight, overlay);

        // South (Z+) — pas de rotation UV
        quad(vc, pose, OVL_MAX_X, OVL_MIN_Y, OVL_MAX_Z, OVL_MIN_X, OVL_MAX_Y, OVL_MAX_Z,
            u0, v1, u0, v0, u1, v0, u1, v1, 0, 0, 1, packedLight, overlay);

        // West (X-) — pas de rotation UV
        quad(vc, pose, OVL_MIN_X, OVL_MIN_Y, OVL_MAX_Z, OVL_MIN_X, OVL_MAX_Y, OVL_MIN_Z,
            u0, v1, u0, v0, u1, v0, u1, v1, -1, 0, 0, packedLight, overlay);

        // East (X+) — rotation 180 (flip U et V)
        quad(vc, pose, OVL_MAX_X, OVL_MIN_Y, OVL_MIN_Z, OVL_MAX_X, OVL_MAX_Y, OVL_MAX_Z,
            u1, v0, u1, v1, u0, v1, u0, v0, 1, 0, 0, packedLight, overlay);

        // Up (Y+) — rotation 90, V inverse pour matcher la direction de charge
        quad4(vc, pose,
            OVL_MIN_X, OVL_MAX_Y, OVL_MIN_Z,
            OVL_MIN_X, OVL_MAX_Y, OVL_MAX_Z,
            OVL_MAX_X, OVL_MAX_Y, OVL_MAX_Z,
            OVL_MAX_X, OVL_MAX_Y, OVL_MIN_Z,
            u1, v1, u0, v1, u0, v0, u1, v0, 0, 1, 0, packedLight, overlay);

        // Down (Y-) — rotation 180
        quad4(vc, pose,
            OVL_MIN_X, OVL_MIN_Y, OVL_MAX_Z,
            OVL_MIN_X, OVL_MIN_Y, OVL_MIN_Z,
            OVL_MAX_X, OVL_MIN_Y, OVL_MIN_Z,
            OVL_MAX_X, OVL_MIN_Y, OVL_MAX_Z,
            u0, v0, u1, v0, u1, v1, u0, v1, 0, -1, 0, packedLight, overlay);
    }

    /**
     * Rend les 3 barres indicatrices de charge.
     * 4 etats dans l'atlas (0=rien, 1=charge1, 2=charge2, 3=charge3).
     * Les 3 anneaux affichent le meme etat simultanément.
     */
    private void renderChargeBars(PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int chargeLevel) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CHARGING2_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        int state = Math.min(chargeLevel, 3);
        float bv0 = state * BAR_STATE_V_SIZE;
        float bv1 = bv0 + BAR_STATE_V_SIZE;

        for (int i = 0; i < 3; i++) {
            renderBarBox(vc, pose,
                BAR_MIN_X, BAR_MIN_Y, BAR_Z_MIN[i],
                BAR_MAX_X, BAR_MAX_Y, BAR_Z_MAX[i],
                BAR_U0[i], bv0, BAR_U1[i], bv1, packedLight, overlay);
        }
    }

    /** Rend une barre: sides avec UV column, up/down avec 1er pixel haut-gauche. */
    private void renderBarBox(VertexConsumer vc, PoseStack.Pose pose,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float u0, float v0, float u1, float v1, int light, int overlay) {
        // North (Z-)
        quad(vc, pose, x0, y0, z0, x1, y1, z0, u0, v1, u0, v0, u1, v0, u1, v1, 0, 0, -1, light, overlay);
        // South (Z+)
        quad(vc, pose, x1, y0, z1, x0, y1, z1, u0, v1, u0, v0, u1, v0, u1, v1, 0, 0, 1, light, overlay);
        // West (X-)
        quad(vc, pose, x0, y0, z1, x0, y1, z0, u0, v1, u0, v0, u1, v0, u1, v1, -1, 0, 0, light, overlay);
        // East (X+)
        quad(vc, pose, x1, y0, z0, x1, y1, z1, u0, v1, u0, v0, u1, v0, u1, v1, 1, 0, 0, light, overlay);
        // Up (Y+) — demi-pixel de la colonne de cet anneau
        float pxV = v0 + BAR_PIXEL_V * 0.5f;
        quad4(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
            u0, v0, u0, pxV, u1, pxV, u1, v0, 0, 1, 0, light, overlay);
        // Down (Y-) — premier pixel de la colonne de cet anneau
        quad4(vc, pose, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
            u0, pxV, u0, v0, u1, v0, u1, pxV, 0, -1, 0, light, overlay);
    }

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

    // =========================================================================
    // Quad helpers — emission de vertices pour une face rectangulaire
    // =========================================================================

    /**
     * Emet un quad vertical (2 coins: bottom-left et top-right sur le meme plan).
     * Les 4 vertices forment un rectangle sur le plan defini par la normale.
     */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float u_bl, float v_bl, float u_tl, float v_tl,
                              float u_tr, float v_tr, float u_br, float v_br,
                              float nx, float ny, float nz, int light, int overlay) {
        // bottom-left
        vc.addVertex(pose, x0, y0, z0).setColor(1f, 1f, 1f, 1f)
            .setUv(u_bl, v_bl).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        // top-left
        vc.addVertex(pose, x0, y1, z0).setColor(1f, 1f, 1f, 1f)
            .setUv(u_tl, v_tl).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        // top-right
        vc.addVertex(pose, x1, y1, z1).setColor(1f, 1f, 1f, 1f)
            .setUv(u_tr, v_tr).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        // bottom-right
        vc.addVertex(pose, x1, y0, z1).setColor(1f, 1f, 1f, 1f)
            .setUv(u_br, v_br).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** Emet un quad avec 4 positions arbitraires (pour faces up/down). */
    private static void quad4(VertexConsumer vc, PoseStack.Pose pose,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float u0, float v0, float u1, float v1,
                               float u2, float v2, float u3, float v3,
                               float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(pose, x0, y0, z0).setColor(1f, 1f, 1f, 1f)
            .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x1, y1, z1).setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x2, y2, z2).setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x3, y3, z3).setColor(1f, 1f, 1f, 1f)
            .setUv(u3, v3).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
