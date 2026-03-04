/**
 * ============================================================
 * [MiningLaserItemRenderer.java]
 * Description: BEWLR pour le Mining Laser — rendu 3D avec charge bars et halo sprite
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimationTimer          | Temps client         | Tracking frame animation       |
 * | BakedModel              | Modèle 3D body       | Rendu statique via putBulkData |
 * | MiningLaserItem         | Détection item       | Lecture chargeLevel            |
 * | MiningLaserRingOverlay  | Géométrie ring       | Anneaux autour des barres      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement BEWLR + modèle additionnel)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.renderer.LightningArcRenderer;
import com.chapeau.apica.common.item.tool.MiningLaserItem;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * BEWLR pour le Mining Laser. Rendu hybride en 5 couches :
 * 1. Body statique (baked model via putBulkData)
 * 2. Beam core animé (14 frames, 2px/frame, texture 16x28 : côtés col 0-12, bouts col 14-15)
 * 3. Charge bars (3 barres : off/on selon chargeLevel stocké dans CustomData)
 * 4. Ring effects (anneaux géométriques rotatifs autour des barres actives)
 * 5. Halo sprite (quad billboard au bout du canon, rotation continue, toujours visible en main)
 */
@OnlyIn(Dist.CLIENT)
public class MiningLaserItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Modèle baked du body */
    public static final ModelResourceLocation BODY_MODEL_LOC =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    Apica.MOD_ID, "item/mining_laser_body"));

    /** Texture du beam core (animation de chargement 14 frames, 16x28, 2px/frame) */
    private static final ResourceLocation BEAM_CORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/mining_laser_beam_core.png");

    /** Texture des barres indicatrices (atlas 3 colonnes × 2 états) */
    private static final ResourceLocation CHARGING2_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/mining_laser_charging2.png");

    /** Texture halo pour le sprite au bout du canon (tourne en continu) */
    private static final ResourceLocation HALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/halo.png");

    /** Texture ring pour les anneaux géométriques autour des barres actives */
    private static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/ring.png");

    // --- Overlay box (beam barrel charge inner) ---
    private static final float OVL_MIN_X = 6.25f / 16f;
    private static final float OVL_MIN_Y = -0.75f / 16f;
    private static final float OVL_MIN_Z = -12.4f / 16f;
    private static final float OVL_MAX_X = 7.75f / 16f;
    private static final float OVL_MAX_Y = 0.75f / 16f;
    private static final float OVL_MAX_Z = 0.5f / 16f;

    // --- 3 Bar boxes (charge indicators on barrel) ---
    private static final float BAR_MIN_X = 5.5f / 16f;
    private static final float BAR_MIN_Y = -1.5f / 16f;
    private static final float BAR_MAX_X = 8.5f / 16f;
    private static final float BAR_MAX_Y = 1.5f / 16f;
    private static final float[] BAR_Z_MIN = {-10f / 16f, -8f / 16f, -6f / 16f};
    private static final float[] BAR_Z_MAX = {-9f / 16f, -7f / 16f, -5f / 16f};
    private static final float[] BAR_U0 = {0f, 1f / 3f, 2f / 3f};
    private static final float[] BAR_U1 = {1f / 3f, 2f / 3f, 1f};

    // Atlas 3x12: 4 états de 3 rows
    private static final float BAR_STATE_V_SIZE = 0.25f;
    private static final float BAR_PIXEL_V = 1f / 12f;

    private static final int TOTAL_FRAMES = 14;

    // Beam core texture layout: 16 wide × 28 tall, 2px par frame
    // Colonnes 0-12: côtés (east/west/up/down), colonnes 14-15: bouts (north/south)
    private static final float TEX_HEIGHT = 28.0f;
    private static final float FRAME_HEIGHT = 2.0f;
    private static final float SIDE_U0 = 0.0f;
    private static final float SIDE_U1 = 13.0f / 16.0f;
    private static final float END_U0 = 14.0f / 16.0f;
    private static final float END_U1 = 1.0f;

    // Halo position (devant le canon, pointe du barrel)
    private static final float HALO_X = 7f / 16f;
    private static final float HALO_Y = 0f / 16f;
    private static final float HALO_Z = -13f / 16f;

    // --- Lightning arcs (entre ring_front et ring_back) ---
    private static final Vec3 ARC_START = new Vec3(7.0 / 16.0, 0, 6.0 / 16.0);
    private static final Vec3 ARC_END = new Vec3(7.0 / 16.0, 0, 11.0 / 16.0);
    private static final float ARC_AMPLITUDE = 2.0f / 16.0f;
    private static final int ARC_NODES = 2;
    private static final int ARC_REFRESH_TICKS = 10;
    private static final float ARC_HALF_WIDTH = 0.02f;

    // Animation state
    private int currentFrame = 0;
    private int lastTick = -1;

    // Lightning state
    private final LightningArcRenderer.LightningArc[] lightningArcs = new LightningArcRenderer.LightningArc[2];
    private int lastArcTick = -1;

    public MiningLaserItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                              PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay) {
        renderBodyModel(poseStack, buffer, packedLight, packedOverlay, stack);

        boolean inHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        int chargeLevel;
        if (inHand) {
            updateAnimation();
            chargeLevel = getItemChargeLevel();
            renderChargingOverlay(poseStack, buffer, packedLight);
            renderChargeBars(poseStack, buffer, packedLight, chargeLevel);
            renderRingEffects(poseStack, buffer, packedLight, chargeLevel);
            renderHaloSprite(poseStack, buffer);
            renderLightningArcs(poseStack, buffer);
        } else {
            chargeLevel = MiningLaserItem.getChargeLevel(stack);
            renderChargingOverlay(poseStack, buffer, packedLight, 0);
            renderChargeBars(poseStack, buffer, packedLight, chargeLevel);
        }
    }

    // =========================================================================
    // Body model (baked)
    // =========================================================================

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

    // =========================================================================
    // Beam core overlay (14 frames, 2px/frame, côtés + bouts séparés)
    // =========================================================================

    private void updateAnimation() {
        int tick = AnimationTimer.getTicks();
        if (tick == lastTick) return;
        lastTick = tick;

        Minecraft mc = Minecraft.getInstance();
        boolean isCharging = mc.player != null && mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() instanceof MiningLaserItem;

        if (isCharging) {
            if (currentFrame < TOTAL_FRAMES - 1) currentFrame++;
        } else {
            if (currentFrame > 0) currentFrame--;
        }
    }

    private void renderChargingOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderChargingOverlay(poseStack, buffer, packedLight, currentFrame);
    }

    /**
     * Rendu du beam core animé. Texture 16x28 :
     * - Colonnes 0-12 (SIDE) : faces latérales (east/west/up/down), le long du barrel
     * - Colonnes 14-15 (END) : faces bout du canon (north/south)
     * - 14 frames de 2 rows chacune, chargement progressif gauche→droite
     */
    private void renderChargingOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int frame) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(BEAM_CORE_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        float v0 = (frame * FRAME_HEIGHT) / TEX_HEIGHT;
        float v1 = v0 + FRAME_HEIGHT / TEX_HEIGHT;

        // --- Faces bout (north/south) : colonnes 14-15 ---

        // North (Z-) — bout avant du canon
        quad(vc, pose, OVL_MIN_X, OVL_MIN_Y, OVL_MIN_Z, OVL_MAX_X, OVL_MAX_Y, OVL_MIN_Z,
                END_U0, v1, END_U0, v0, END_U1, v0, END_U1, v1, 0, 0, -1, packedLight, overlay);
        // South (Z+) — bout arrière du canon
        quad(vc, pose, OVL_MAX_X, OVL_MIN_Y, OVL_MAX_Z, OVL_MIN_X, OVL_MAX_Y, OVL_MAX_Z,
                END_U0, v1, END_U0, v0, END_U1, v0, END_U1, v1, 0, 0, 1, packedLight, overlay);

        // --- Faces latérales (east/west/up/down) : colonnes 0-12 ---
        // U : pixel 0 = arrière (Z+), pixel 12 = avant (Z-)

        // West (X-) — back(MAX_Z) vers front(MIN_Z)
        quad(vc, pose, OVL_MIN_X, OVL_MIN_Y, OVL_MAX_Z, OVL_MIN_X, OVL_MAX_Y, OVL_MIN_Z,
                SIDE_U0, v1, SIDE_U0, v0, SIDE_U1, v0, SIDE_U1, v1, -1, 0, 0, packedLight, overlay);
        // East (X+) — front(MIN_Z) vers back(MAX_Z), rotation 180
        quad(vc, pose, OVL_MAX_X, OVL_MIN_Y, OVL_MIN_Z, OVL_MAX_X, OVL_MAX_Y, OVL_MAX_Z,
                SIDE_U1, v0, SIDE_U1, v1, SIDE_U0, v1, SIDE_U0, v0, 1, 0, 0, packedLight, overlay);
        // Up (Y+)
        quad4(vc, pose,
                OVL_MIN_X, OVL_MAX_Y, OVL_MIN_Z,
                OVL_MIN_X, OVL_MAX_Y, OVL_MAX_Z,
                OVL_MAX_X, OVL_MAX_Y, OVL_MAX_Z,
                OVL_MAX_X, OVL_MAX_Y, OVL_MIN_Z,
                SIDE_U1, v1, SIDE_U0, v1, SIDE_U0, v0, SIDE_U1, v0, 0, 1, 0, packedLight, overlay);
        // Down (Y-)
        quad4(vc, pose,
                OVL_MIN_X, OVL_MIN_Y, OVL_MAX_Z,
                OVL_MIN_X, OVL_MIN_Y, OVL_MIN_Z,
                OVL_MAX_X, OVL_MIN_Y, OVL_MIN_Z,
                OVL_MAX_X, OVL_MIN_Y, OVL_MAX_Z,
                SIDE_U0, v0, SIDE_U1, v0, SIDE_U1, v1, SIDE_U0, v1, 0, -1, 0, packedLight, overlay);
    }

    // =========================================================================
    // Charge bars (3 barres, identique leaf blower)
    // =========================================================================

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
        // Up (Y+)
        float pxV = v0 + BAR_PIXEL_V * 0.5f;
        quad4(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                u0, v0, u0, pxV, u1, pxV, u1, v0, 0, 1, 0, light, overlay);
        // Down (Y-)
        quad4(vc, pose, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
                u0, pxV, u0, v0, u1, v0, u1, pxV, 0, -1, 0, light, overlay);
    }

    // =========================================================================
    // Ring effects (anneaux géométriques autour des barres actives)
    // =========================================================================

    private void renderRingEffects(PoseStack poseStack, MultiBufferSource buffer,
                                    int packedLight, int chargeLevel) {
        if (chargeLevel <= 0) return;

        float time = AnimationTimer.getRenderTime(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));

        float centerX = 7f / 16f;
        float centerY = 0f / 16f;

        for (int i = 0; i < Math.min(chargeLevel, 3); i++) {
            float centerZ = (BAR_Z_MIN[i] + BAR_Z_MAX[i]) / 2f;
            float rotation = time * (1.2f + i * 0.3f);
            MiningLaserRingOverlay.renderRing(poseStack, buffer, packedLight,
                    centerX, centerY, centerZ, rotation, RING_TEXTURE);
        }
    }

    // =========================================================================
    // Halo sprite (toujours visible en main, tourne sur lui-même au bout du canon)
    // =========================================================================

    private void renderHaloSprite(PoseStack poseStack, MultiBufferSource buffer) {
        float time = AnimationTimer.getRenderTime(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        float rotation = time * 1.5f;
        float size = 0.30f + 0.04f * (float) Math.sin(time * 0.5);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(HALO_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(HALO_X, HALO_Y, HALO_Z);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rotation));

        PoseStack.Pose pose = poseStack.last();
        vc.addVertex(pose, -size, -size, 0).setColor(1f, 1f, 0.8f, 0.7f)
                .setUv(0, 1).setOverlay(overlay).setLight(15728880).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, -size, size, 0).setColor(1f, 1f, 0.8f, 0.7f)
                .setUv(0, 0).setOverlay(overlay).setLight(15728880).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, size, size, 0).setColor(1f, 1f, 0.8f, 0.7f)
                .setUv(1, 0).setOverlay(overlay).setLight(15728880).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, size, -size, 0).setColor(1f, 1f, 0.8f, 0.7f)
                .setUv(1, 1).setOverlay(overlay).setLight(15728880).setNormal(pose, 0, 0, 1);

        poseStack.popPose();
    }

    // =========================================================================
    // Lightning arcs (2 arcs permanents entre ring_front et ring_back)
    // =========================================================================

    private void renderLightningArcs(PoseStack poseStack, MultiBufferSource buffer) {
        int currentTick = AnimationTimer.getTicks();

        if (lastArcTick < 0 || (currentTick - lastArcTick) >= ARC_REFRESH_TICKS) {
            RandomSource random = RandomSource.create(currentTick * 31L);
            for (int i = 0; i < 2; i++) {
                lightningArcs[i] = LightningArcRenderer.generateArc(
                        ARC_START, ARC_END, ARC_NODES, ARC_AMPLITUDE,
                        ARC_REFRESH_TICKS, false, false, random);
            }
            lastArcTick = currentTick;
        }

        for (LightningArcRenderer.LightningArc arc : lightningArcs) {
            if (arc != null) {
                LightningArcRenderer.renderArc(poseStack, buffer, arc,
                        ARC_HALF_WIDTH, 0.7f, 0.9f, 1.0f, 0.9f);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private int getItemChargeLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof MiningLaserItem) {
            return MiningLaserItem.getChargeLevel(mainHand);
        }
        ItemStack offHand = mc.player.getOffhandItem();
        if (offHand.getItem() instanceof MiningLaserItem) {
            return MiningLaserItem.getChargeLevel(offHand);
        }
        return 0;
    }

    // =========================================================================
    // Quad helpers (identiques au LeafBlowerItemRenderer)
    // =========================================================================

    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float u_bl, float v_bl, float u_tl, float v_tl,
                              float u_tr, float v_tr, float u_br, float v_br,
                              float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(pose, x0, y0, z0).setColor(1f, 1f, 1f, 1f)
                .setUv(u_bl, v_bl).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x0, y1, z0).setColor(1f, 1f, 1f, 1f)
                .setUv(u_tl, v_tl).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x1, y1, z1).setColor(1f, 1f, 1f, 1f)
                .setUv(u_tr, v_tr).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x1, y0, z1).setColor(1f, 1f, 1f, 1f)
                .setUv(u_br, v_br).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

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
