/**
 * ============================================================
 * [CompanionBeeRenderer.java]
 * Description: Renderer pour CompanionBeeEntity avec ApicaBeeModel multi-pass tinte
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CompanionBeeEntity  | Entite compagnon     | Acces aux donnees (type, species)|
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass tinte         |
 * | BeeSpeciesManager   | Donnees especes      | Couleurs et modeles            |
 * | CompanionBeeRenderLayer | RenderLayer     | Rendu tinte par partie         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.animation.bee.BeeAnimationState;
import com.chapeau.apica.client.animation.bee.BeeModelAnimator;
import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer pour les abeilles compagnon utilisant le modele ApicaBee modulaire.
 * Scale 0.4f (mini-abeille).
 * Utilise BeeSpeciesManager pour les couleurs et types de modele.
 */
public class CompanionBeeRenderer extends MobRenderer<CompanionBeeEntity, ApicaBeeModel<CompanionBeeEntity>> {

    private static final float COMPANION_BEE_SCALE = 0.4f;
    private static final ItemStack CHEST_ICON = new ItemStack(Items.CHEST);

    /** Cache des modeles par cle de combinaison "body_wing_stinger_antenna". */
    private static final Map<String, ApicaBeeModel<CompanionBeeEntity>> MODEL_CACHE = new HashMap<>();

    /** Donnees espece du frame courant, definies dans render() pour le layer. */
    BeeSpeciesManager.BeeSpeciesData currentData;
    BeeBodyType currentBodyType = BeeBodyType.DEFAULT;
    BeeWingType currentWingType = BeeWingType.DEFAULT;
    BeeStingerType currentStingerType = BeeStingerType.DEFAULT;
    BeeAntennaType currentAntennaType = BeeAntennaType.DEFAULT;

    private final ItemRenderer itemRenderer;

    /** Animator pour les animations du companion bee. */
    private final BeeModelAnimator animator;

    public CompanionBeeRenderer(EntityRendererProvider.Context context) {
        super(context, buildDefaultModel(context), 0.15f);
        this.itemRenderer = context.getItemRenderer();
        this.addLayer(new CompanionBeeRenderLayer(this));

        // Companion bees start in HOVERING state
        this.animator = new BeeModelAnimator();
        this.animator.setStateImmediate(BeeAnimationState.HOVERING);
    }

    @Override
    public void render(@NotNull CompanionBeeEntity bee, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        BeeSpeciesManager.ensureClientLoaded();
        currentData = BeeSpeciesManager.getSpecies(bee.getSpeciesId());

        currentBodyType = resolveBodyType(currentData != null ? currentData.modelBody : "default");
        currentWingType = resolveWingType(currentData != null ? currentData.modelWing : "default");
        currentStingerType = resolveStingerType(currentData != null ? currentData.modelStinger : "default");
        currentAntennaType = resolveAntennaType(currentData != null ? currentData.modelAntenna : "default");

        ApicaBeeModel<CompanionBeeEntity> speciesModel = getOrBuildModel(
                currentBodyType, currentWingType, currentStingerType, currentAntennaType);
        if (speciesModel != null) {
            this.model = speciesModel;
        }

        // Determine animation state and apply
        float ageInTicks = bee.tickCount + partialTick;
        BeeAnimationState targetState = determineAnimationState(bee);
        animator.setState(targetState, ageInTicks);
        animator.animate(model, ageInTicks, partialTick);
        model.setBodyPitch(animator.getCurrentBodyPitch());

        super.render(bee, entityYaw, partialTick, poseStack, buffer, packedLight);

        // Rendu additionnel (item porte ou coffre)
        CompanionBeeEntity.CompanionType type = bee.getCompanionType();
        if (type == CompanionBeeEntity.CompanionType.BACKPACK) {
            renderChest(poseStack, buffer, packedLight, entityYaw);
        } else if (type == CompanionBeeEntity.CompanionType.MAGNET) {
            ItemStack carried = bee.getCarriedItem();
            if (!carried.isEmpty()) {
                renderCarriedItem(carried, poseStack, buffer, packedLight, entityYaw);
            }
        }
    }

    /**
     * Determine l'etat d'animation selon le comportement du companion.
     */
    private BeeAnimationState determineAnimationState(CompanionBeeEntity bee) {
        // En mouvement = vol
        if (bee.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            return BeeAnimationState.FLYING;
        }
        // Stationnaire = hovering
        return BeeAnimationState.HOVERING;
    }

    @Override
    protected void scale(@NotNull CompanionBeeEntity bee, @NotNull PoseStack poseStack, float partialTick) {
        poseStack.scale(COMPANION_BEE_SCALE, COMPANION_BEE_SCALE, COMPANION_BEE_SCALE);
    }

    /**
     * Retourne null pour empecher le rendu standard single-pass.
     * Le multi-pass est gere par CompanionBeeRenderLayer.
     */
    @Override
    protected @Nullable RenderType getRenderType(@NotNull CompanionBeeEntity entity,
                                                  boolean bodyVisible, boolean translucent, boolean glowing) {
        if (glowing) {
            return RenderType.outline(getTextureLocation(entity));
        }
        return null;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CompanionBeeEntity bee) {
        return ApicaBeeModel.getBodyTexture(currentBodyType);
    }

    // ========== Type resolution ==========

    static BeeBodyType resolveBodyType(String id) {
        for (BeeBodyType t : BeeBodyType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeBodyType.DEFAULT;
    }

    static BeeWingType resolveWingType(String id) {
        for (BeeWingType t : BeeWingType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeWingType.DEFAULT;
    }

    static BeeStingerType resolveStingerType(String id) {
        for (BeeStingerType t : BeeStingerType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeStingerType.DEFAULT;
    }

    static BeeAntennaType resolveAntennaType(String id) {
        for (BeeAntennaType t : BeeAntennaType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return BeeAntennaType.DEFAULT;
    }

    // ========== Model cache ==========

    private static ApicaBeeModel<CompanionBeeEntity> buildDefaultModel(EntityRendererProvider.Context context) {
        ModelPart bodyRoot = context.bakeLayer(ApicaBeeModel.getBodyLayer(BeeBodyType.DEFAULT));
        ModelPart wingRoot = context.bakeLayer(ApicaBeeModel.getWingLayer(BeeWingType.DEFAULT));
        ModelPart stingerRoot = context.bakeLayer(ApicaBeeModel.getStingerLayer(BeeStingerType.DEFAULT));
        ModelPart antennaRoot = context.bakeLayer(ApicaBeeModel.getAntennaLayer(BeeAntennaType.DEFAULT));
        return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, BeeBodyType.DEFAULT);
    }

    private static ApicaBeeModel<CompanionBeeEntity> getOrBuildModel(BeeBodyType body, BeeWingType wing,
                                                                      BeeStingerType stinger, BeeAntennaType antenna) {
        String key = body.getId() + "_" + wing.getId() + "_" + stinger.getId() + "_" + antenna.getId();
        return MODEL_CACHE.computeIfAbsent(key, k -> {
            try {
                var entityModels = Minecraft.getInstance().getEntityModels();
                ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(body));
                ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.getWingLayer(wing));
                ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.getStingerLayer(stinger));
                ModelPart antennaRoot = entityModels.bakeLayer(ApicaBeeModel.getAntennaLayer(antenna));
                return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, body);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** Vide le cache des modeles (utile pour le rechargement des ressources). */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
    }

    // ========== Rendu additionnels ==========

    /** Rend un item porte sous le ventre (abeille magnet). */
    private void renderCarriedItem(ItemStack stack, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, float entityYaw) {
        poseStack.pushPose();
        poseStack.translate(0.0, -0.15, 0.0);
        poseStack.scale(0.3f, 0.3f, 0.3f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }

    /** Rend un coffre sous le ventre (abeille backpack). */
    private void renderChest(PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, float entityYaw) {
        poseStack.pushPose();
        poseStack.translate(0.0, -0.25, 0.0);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        itemRenderer.renderStatic(CHEST_ICON, ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }
}
