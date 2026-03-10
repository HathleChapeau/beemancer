/**
 * ============================================================
 * [RailgunItemRenderer.java]
 * Description: BEWLR pour le Railgun avec animation de chargement sur le Loader
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BakedModel              | Modele 3D body       | Rendu via putBulkData          |
 * | AnimationTimer          | Temps client         | Tracking frame animation       |
 * | RailgunItem             | Detection item       | Constantes charge              |
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
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.tool.RailgunItem;
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
 * BEWLR pour le Railgun. Rendu hybride:
 * 1. Body statique (baked model via putBulkData)
 * 2. Charging overlay sur le Loader (east/west faces, 26 frames scrolling vertical)
 *
 * Texture 25x26: chaque frame = 1 row de 25px, UV descend de 1px par step.
 * Forward pendant le chargement, reverse au relachement.
 */
@OnlyIn(Dist.CLIENT)
public class RailgunItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final ModelResourceLocation BODY_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "item/railgun_body"));

    /** Texture animation du Loader (25x26: 26 frames de 25x1) */
    private static final ResourceLocation CHARGING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/railgun_charging.png");

    private static final int TOTAL_FRAMES = 26;

    // Loader box en unites bloc (model coords / 16)
    private static final float LDR_MIN_X = 6.75f / 16f;
    private static final float LDR_MAX_X = 10.25f / 16f;
    private static final float LDR_MIN_Y = 4.5f / 16f;
    private static final float LDR_MAX_Y = 13f / 16f;
    private static final float LDR_MIN_Z = 5.5f / 16f;
    private static final float LDR_MAX_Z = 30.5f / 16f;
    private static final float FACE_OFFSET = 0.001f;

    private float currentFrame = 0;
    private int lastTick = -1;

    public RailgunItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        renderBodyModel(poseStack, buffer, packedLight, packedOverlay, stack);

        boolean inHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        int tint = getLoaderTint(stack);
        if (inHand) {
            updateAnimation();
            renderChargingOverlay(poseStack, buffer, packedLight, (int) currentFrame, tint);
        } else {
            renderChargingOverlay(poseStack, buffer, packedLight, 0, tint);
        }
    }

    private void renderBodyModel(PoseStack poseStack, MultiBufferSource buffer,
                                  int packedLight, int packedOverlay, ItemStack stack) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(BODY_MODEL_LOC);
        if (model == null) return;

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
        boolean holdingRailgun = mc.player != null
            && mc.player.getMainHandItem().getItem() instanceof RailgunItem;
        if (!holdingRailgun) {
            currentFrame = 0;
            return;
        }

        boolean isCharging = mc.player.isUsingItem()
            && mc.player.getUseItem().getItem() instanceof RailgunItem;

        if (isCharging) {
            float speedMult = RailgunItem.getChargeSpeedMultiplier(mc.player.getUseItem());
            float framesPerTick = (TOTAL_FRAMES - 1) * speedMult / RailgunItem.CHARGE_THRESHOLD;
            currentFrame = Math.min(currentFrame + framesPerTick, TOTAL_FRAMES - 1);
        } else {
            if (currentFrame > 0) currentFrame = Math.max(0, currentFrame - 1);
        }
    }

    /** Retourne la couleur de tinte du Loader en fonction du fluide du magazine. */
    private static int getLoaderTint(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("honey")) return 0xFADE29;
        if (fluidId.contains("royal_jelly")) return 0xFFFAD0;
        if (fluidId.contains("nectar")) return 0xE855FF;
        return 0xFFFFFF;
    }

    /**
     * Rend l'overlay d'animation sur les faces east/west/south du Loader.
     * Texture 25x26: chaque frame = 1 row de 25 pixels.
     * - East/West: full width (pixels 1-25, U 0-1), 1 row height (V frame/26 to (frame+1)/26)
     * - South: only pixel 25 (U 24/25 to 1), same V
     * Tinte par la couleur du fluide du magazine equipe.
     */
    private void renderChargingOverlay(PoseStack poseStack, MultiBufferSource buffer,
                                        int packedLight, int frame, int tint) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CHARGING_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int ol = OverlayTexture.NO_OVERLAY;

        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        // V coordinates for current frame row (1 row = 1/26 of texture height)
        float v0 = (float) frame / TOTAL_FRAMES;
        float v1 = v0 + 1f / TOTAL_FRAMES;

        // === WEST FACE (X-) ===
        // U: 0 at minZ (front), 1 at maxZ (back)
        float wx = LDR_MIN_X - FACE_OFFSET;
        vc.addVertex(pose, wx, LDR_MIN_Y, LDR_MIN_Z).setColor(r, g, b, 1f).setUv(0f, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, wx, LDR_MAX_Y, LDR_MIN_Z).setColor(r, g, b, 1f).setUv(0f, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, wx, LDR_MAX_Y, LDR_MAX_Z).setColor(r, g, b, 1f).setUv(1f, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, wx, LDR_MIN_Y, LDR_MAX_Z).setColor(r, g, b, 1f).setUv(1f, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, -1, 0, 0);

        // === EAST FACE (X+) ===
        // U: 1 at maxZ (back), 0 at minZ (front) — flipped to match world orientation
        float ex = LDR_MAX_X + FACE_OFFSET;
        vc.addVertex(pose, ex, LDR_MIN_Y, LDR_MAX_Z).setColor(r, g, b, 1f).setUv(1f, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, ex, LDR_MAX_Y, LDR_MAX_Z).setColor(r, g, b, 1f).setUv(1f, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, ex, LDR_MAX_Y, LDR_MIN_Z).setColor(r, g, b, 1f).setUv(0f, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, ex, LDR_MIN_Y, LDR_MIN_Z).setColor(r, g, b, 1f).setUv(0f, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, 1, 0, 0);

        // === SOUTH FACE (Z+) ===
        // U: only pixel 25 (last column) = 24/25 to 1
        float sU0 = 24f / 25f;
        float sU1 = 1f;
        float sz = LDR_MAX_Z + FACE_OFFSET;
        vc.addVertex(pose, LDR_MIN_X, LDR_MIN_Y, sz).setColor(r, g, b, 1f).setUv(sU0, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, LDR_MIN_X, LDR_MAX_Y, sz).setColor(r, g, b, 1f).setUv(sU0, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, LDR_MAX_X, LDR_MAX_Y, sz).setColor(r, g, b, 1f).setUv(sU1, v0).setOverlay(ol).setLight(packedLight).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, LDR_MAX_X, LDR_MIN_Y, sz).setColor(r, g, b, 1f).setUv(sU1, v1).setOverlay(ol).setLight(packedLight).setNormal(pose, 0, 0, 1);
    }
}
