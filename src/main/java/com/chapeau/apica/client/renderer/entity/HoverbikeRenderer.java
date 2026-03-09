/**
 * ============================================================
 * [HoverbikeRenderer.java]
 * Description: Renderer pour l'entite HoverBee avec ApicaBeeModel multi-pass tinte par espece
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite a rendre      | Source des donnees             |
 * | ApicaBeeModel       | Modele modulaire     | Rendu multi-pass tinte         |
 * | BeeSpeciesManager   | Donnees especes      | Model types + couleurs         |
 * | HoverbikeRenderLayer| Layer multi-pass     | Rendu tinte par partie         |
 * | HoverbikePartLayer  | Layer parties         | Rendu parties modulaires       |
 * | Apica               | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeMode;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer du HoverBee utilisant le modele ApicaBee modulaire avec rendu multi-pass tinte.
 * Le rendu standard single-pass est desactive via getRenderType() → null.
 * Le multi-pass est gere par HoverbikeRenderLayer.
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    /** Facteur d'echelle pour transformer l'abeille en monture */
    private static final float BEE_SCALE = 1.87F;
    private static final float UP_BIAS = 0.05F;

    /** Cache des modeles par cle de combinaison "body_wing_stinger_antenna". */
    private static final Map<String, ApicaBeeModel<HoverbikeEntity>> MODEL_CACHE = new HashMap<>();

    /** Donnees espece du frame courant, accessibles par les layers. */
    BeeSpeciesManager.BeeSpeciesData currentData;
    BeeBodyType currentBodyType = BeeBodyType.DEFAULT;
    BeeWingType currentWingType = BeeWingType.DEFAULT;
    BeeStingerType currentStingerType = BeeStingerType.DEFAULT;
    BeeAntennaType currentAntennaType = BeeAntennaType.DEFAULT;

    public HoverbikeRenderer(EntityRendererProvider.Context context) {
        super(context, buildDefaultModel(context), 1.2f);
        this.addLayer(new HoverbikeRenderLayer(this));
        this.addLayer(new HoverbikePartLayer(this, context));
    }

    @Override
    public void render(@NotNull HoverbikeEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        BeeSpeciesManager.ensureClientLoaded();
        String speciesId = entity.getSpeciesId();
        currentData = BeeSpeciesManager.getSpecies(speciesId);

        currentBodyType = resolveBodyType(currentData != null ? currentData.modelBody : "default");
        currentWingType = resolveWingType(currentData != null ? currentData.modelWing : "default");
        currentStingerType = resolveStingerType(currentData != null ? currentData.modelStinger : "default");
        currentAntennaType = resolveAntennaType(currentData != null ? currentData.modelAntenna : "default");

        ApicaBeeModel<HoverbikeEntity> speciesModel = getOrBuildModel(
                currentBodyType, currentWingType, currentStingerType, currentAntennaType);
        if (speciesModel != null) {
            this.model = speciesModel;
        }

        // Animate wings and legs based on hover state (before super.render which calls setupAnim)
        animateHoverBee(entity, partialTick);

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Anime les ailes et pattes du HoverBee en fonction de l'etat de vol.
     * Appele avant super.render() car setupAnim() de ApicaBeeModel est vide.
     */
    private void animateHoverBee(HoverbikeEntity entity, float partialTick) {
        float ageInTicks = entity.tickCount + partialTick;
        float speed;
        float amplitude;
        float legTuck;

        double horizontalSpeed = entity.getDeltaMovement().horizontalDistance();
        boolean isMoving = horizontalSpeed > 0.01;

        if (entity.isJumpPressed()) {
            speed = 1.8F;
            amplitude = 0.12F;
            legTuck = 0.7854F;
        } else if (entity.getSynchedMode() == HoverbikeMode.RUN) {
            speed = 1.0F;
            amplitude = 0.08F;
            legTuck = 0.5F;
        } else if (isMoving) {
            speed = 0.6F;
            amplitude = 0.05F;
            legTuck = 0.3F;
        } else {
            speed = 0.3F;
            amplitude = 0.02F;
            legTuck = 0.15F;
        }

        this.model.animateWings(ageInTicks, speed, amplitude, UP_BIAS);
        this.model.animateLegs(legTuck);
    }

    /**
     * Retourne null pour empecher le rendu standard single-pass.
     * Le multi-pass est gere par HoverbikeRenderLayer.
     */
    @Override
    protected @Nullable RenderType getRenderType(@NotNull HoverbikeEntity entity,
                                                  boolean bodyVisible, boolean translucent, boolean glowing) {
        if (glowing) {
            return RenderType.outline(getTextureLocation(entity));
        }
        return null;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull HoverbikeEntity entity) {
        return ApicaBeeModel.getBodyTexture(currentBodyType);
    }

    @Override
    protected void scale(HoverbikeEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(BEE_SCALE, BEE_SCALE, BEE_SCALE);
        poseStack.translate(0.0F, 0.0F, -0.15F);
    }

    @Override
    protected int getBlockLightLevel(HoverbikeEntity entity, BlockPos pos) {
        return entity.isEditMode() ? 15 : super.getBlockLightLevel(entity, pos);
    }

    // ========== Model cache ==========

    private static ApicaBeeModel<HoverbikeEntity> buildDefaultModel(EntityRendererProvider.Context context) {
        ModelPart bodyRoot = context.bakeLayer(ApicaBeeModel.getBodyLayer(BeeBodyType.DEFAULT));
        ModelPart wingRoot = context.bakeLayer(ApicaBeeModel.getWingLayer(BeeWingType.DEFAULT));
        ModelPart stingerRoot = context.bakeLayer(ApicaBeeModel.getStingerLayer(BeeStingerType.DEFAULT));
        ModelPart antennaRoot = context.bakeLayer(ApicaBeeModel.getAntennaLayer(BeeAntennaType.DEFAULT));
        return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, BeeBodyType.DEFAULT);
    }

    private static ApicaBeeModel<HoverbikeEntity> getOrBuildModel(BeeBodyType body, BeeWingType wing,
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

    /** Vide le cache des modeles (rechargement ressources). */
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
}
