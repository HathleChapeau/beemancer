/**
 * ============================================================
 * [MagicBeeRenderer.java]
 * Description: Renderer custom pour MagicBeeEntity avec ApicaBeeModel multi-pass tinte
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entite a rendre      | Acces aux donnees (espece)     |
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass tinte         |
 * | BeeSpeciesManager   | Donnees especes      | Model types + couleurs parties |
 * | MagicBeeRenderLayer | RenderLayer multi-pass| Rendu tinte par partie         |
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
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer pour les abeilles magiques utilisant le modele ApicaBee modulaire.
 * Extends MobRenderer pour garder les features mob (shadow, damage flash, death anim,
 * smooth rotation, baby scale, name tag).
 *
 * Le rendu standard single-pass est desactive via getRenderType() → null.
 * Le multi-pass tinting est gere par MagicBeeRenderLayer.
 */
public class MagicBeeRenderer extends MobRenderer<MagicBeeEntity, ApicaBeeModel<MagicBeeEntity>> {

    /** Cache des modeles par cle de combinaison "body_wing_stinger_antenna". */
    private static final Map<String, ApicaBeeModel<MagicBeeEntity>> MODEL_CACHE = new HashMap<>();

    /** Donnees espece du frame courant, definies dans render() pour le layer. */
    BeeSpeciesManager.BeeSpeciesData currentData;
    BeeBodyType currentBodyType = BeeBodyType.DEFAULT;
    BeeWingType currentWingType = BeeWingType.DEFAULT;
    BeeStingerType currentStingerType = BeeStingerType.DEFAULT;
    BeeAntennaType currentAntennaType = BeeAntennaType.DEFAULT;

    /** Animator pour les animations de l'abeille. */
    private final BeeModelAnimator animator = BeeModelAnimator.createFlying();

    public MagicBeeRenderer(EntityRendererProvider.Context context) {
        super(context, buildDefaultModel(context), 0.4f);
        this.addLayer(new MagicBeeRenderLayer(this));
    }

    @Override
    public void render(@NotNull MagicBeeEntity bee, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        BeeSpeciesManager.ensureClientLoaded();
        currentData = BeeSpeciesManager.getSpecies(bee.getSpeciesId());

        currentBodyType = resolveBodyType(currentData != null ? currentData.modelBody : "default");
        currentWingType = resolveWingType(currentData != null ? currentData.modelWing : "default");
        currentStingerType = resolveStingerType(currentData != null ? currentData.modelStinger : "default");
        currentAntennaType = resolveAntennaType(currentData != null ? currentData.modelAntenna : "default");

        ApicaBeeModel<MagicBeeEntity> speciesModel = getOrBuildModel(
                currentBodyType, currentWingType, currentStingerType, currentAntennaType);
        if (speciesModel != null) {
            this.model = speciesModel;
        }

        // Determine animation state based on entity behavior
        float ageInTicks = bee.tickCount + partialTick;
        BeeAnimationState targetState = determineAnimationState(bee);
        animator.setState(targetState, ageInTicks);

        // Apply animations to model before render
        animator.animate(model, ageInTicks, partialTick);
        model.setBodyPitch(animator.getCurrentBodyPitch());

        super.render(bee, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Determine l'etat d'animation selon le comportement de l'abeille.
     */
    private BeeAnimationState determineAnimationState(MagicBeeEntity bee) {
        // Enraged = vol rapide
        if (bee.isEnraged()) {
            return BeeAnimationState.FLYING_FAST;
        }
        // Au sol = idle (rare pour les abeilles)
        if (bee.onGround() && bee.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
            return BeeAnimationState.IDLE;
        }
        // En mouvement = vol
        if (bee.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            return BeeAnimationState.FLYING;
        }
        // Stationnaire = hovering
        return BeeAnimationState.HOVERING;
    }

    /**
     * Retourne null pour empecher le rendu standard single-pass.
     * Le multi-pass est gere par MagicBeeRenderLayer.
     * Conserve le rendu outline pour les entites glowing (fleche spectrale).
     */
    @Override
    protected @Nullable RenderType getRenderType(@NotNull MagicBeeEntity entity,
                                                  boolean bodyVisible, boolean translucent, boolean glowing) {
        if (glowing) {
            return RenderType.outline(getTextureLocation(entity));
        }
        return null;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MagicBeeEntity bee) {
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(bee.getSpeciesId());
        String bodyId = data != null ? data.modelBody : "default";
        return ApicaBeeModel.getBodyTexture(resolveBodyType(bodyId));
    }

    // ========== Model cache ==========

    private static ApicaBeeModel<MagicBeeEntity> buildDefaultModel(EntityRendererProvider.Context context) {
        ModelPart bodyRoot = context.bakeLayer(ApicaBeeModel.getBodyLayer(BeeBodyType.DEFAULT));
        ModelPart wingRoot = context.bakeLayer(ApicaBeeModel.getWingLayer(BeeWingType.DEFAULT));
        ModelPart stingerRoot = context.bakeLayer(ApicaBeeModel.getStingerLayer(BeeStingerType.DEFAULT));
        ModelPart antennaRoot = context.bakeLayer(ApicaBeeModel.getAntennaLayer(BeeAntennaType.DEFAULT));
        return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, BeeBodyType.DEFAULT);
    }

    static ApicaBeeModel<MagicBeeEntity> getOrBuildModel(BeeBodyType body, BeeWingType wing,
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

    static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
