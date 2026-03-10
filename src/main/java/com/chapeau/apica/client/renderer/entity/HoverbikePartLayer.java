/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du HoverBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartVariants| Registre variantes  | Selection du modele a rendre   |
 * | AnimationController | Animations edit mode | Ecartement/retour des pieces   |
 * | MoveAnimation       | Translation animee   | Mouvement des pieces           |
 * | ApicaBeeModel       | Modele parent        | Body type + rotation ailes     |
 * | BeeBodyType         | Type de corps        | Positionnement par body type   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.MoveAnimation;
import com.chapeau.apica.client.animation.TimingType;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelB;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer de rendu qui itere sur toutes les parties du hoverbike et rend
 * la variante selectionnee de chaque partie. Chaque partie est centree
 * a l'origine dans son modele et positionnee ici selon le body type courant.
 * En edit mode, chaque partie s'ecarte du centre avec une animation smooth.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    private static final float EDIT_ANIM_DURATION = 15f;
    private static final String ANIM_PREFIX = "edit_";

    /** Tous les modeles bakes, par partie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partVariants = new EnumMap<>(HoverbikePart.class);

    /** Etat d'animation par entite (entity ID → state). */
    private final Map<Integer, PerEntityState> entityStates = new HashMap<>();

    /** Etat d'animation specifique a une entite. */
    private static class PerEntityState {
        final AnimationController controller = new AnimationController();
        boolean wasEditMode = false;
        boolean editExpanded = false;
    }

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> parent,
                              EntityRendererProvider.Context context) {
        super(parent);

        for (HoverbikePart part : HoverbikePart.values()) {
            List<HoverbikePartVariants.VariantEntry> variants = HoverbikePartVariants.getVariants(part);
            List<HoverbikePartModel> models = new ArrayList<>();
            for (HoverbikePartVariants.VariantEntry entry : variants) {
                models.add(entry.factory().constructor().apply(
                        context.bakeLayer(entry.factory().layerLocation())));
            }
            partVariants.put(part, models);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        int entityId = entity.getId();
        PerEntityState state = entityStates.computeIfAbsent(entityId, k -> new PerEntityState());

        state.controller.tick(ageInTicks);

        boolean isEdit = entity.isEditMode();
        handleEditModeTransition(state, isEdit);

        ApicaBeeModel<HoverbikeEntity> beeModel = getParentModel();
        BeeBodyType bodyType = beeModel.getBodyType();
        ModelPart rightWing = beeModel.getRightWing();
        ModelPart leftWing = beeModel.getLeftWing();

        for (HoverbikePart partType : HoverbikePart.values()) {
            List<HoverbikePartModel> models = partVariants.get(partType);
            if (models == null || models.isEmpty()) continue;

            int variantIndex = entity.getPartVariant(partType);
            int clampedIndex = Math.floorMod(variantIndex, models.size());
            HoverbikePartModel part = models.get(clampedIndex);

            part.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            // Synchronise la rotation des protections d'aile avec les ailes du modele parent
            if (partType == HoverbikePart.WING_PROTECTOR) {
                applyWingRotation(part, rightWing, leftWing);
            }

            String animName = ANIM_PREFIX + partType.name().toLowerCase();

            poseStack.pushPose();

            // Positionne la partie selon le body type (les modeles sont centres a l'origine)
            Vec3 pos = getPartPosition(partType, bodyType);
            poseStack.translate(pos.x / 16.0, pos.y / 16.0, pos.z / 16.0);

            applyEditOffset(poseStack, part, animName, state);

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);

            // Spawn lightning particles for saddle variant B
            if (partType == HoverbikePart.SADDLE && clampedIndex == 1) {
                spawnElectrodeParticles(entity, bodyType, partialTick);
            }

            poseStack.popPose();
        }

        cleanupStaleEntities(entity);
    }

    // ========== Per-body-type positioning ==========

    /**
     * Position en coordonnees modele (1 unite = 1/16 bloc) pour chaque partie
     * selon le body type. Les modeles de partie sont centres a l'origine,
     * cette methode fournit la position absolue dans l'espace du modele.
     */
    private static Vec3 getPartPosition(HoverbikePart partType, BeeBodyType bodyType) {
        return switch (partType) {
            case SADDLE -> getSaddlePosition(bodyType);
            case WING_PROTECTOR -> getWingProtectorPosition(bodyType);
            case CONTROL_LEFT -> getControlLeftPosition(bodyType);
            case CONTROL_RIGHT -> getControlRightPosition(bodyType);
        };
    }

    /** Selle: sur le dessus du corps, zone arriere (ou le rider s'assoit). */
    private static Vec3 getSaddlePosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(0, 14.5, 2.0);
            case ROYAL -> new Vec3(0, 14.0, -2.0);
            case SEGMENTED -> new Vec3(0, 14.5, -0.5);
            case ARMORED, PUFFY -> new Vec3(0, 14.5, 1.0);
        };
    }

    /**
     * Protecteurs d'aile: position du centre entre les deux ailes.
     * Les offsets X de ±1.5 dans le modele servent de pivot pour la rotation.
     * Y et Z correspondent aux points d'attache des ailes par body type.
     */
    private static Vec3 getWingProtectorPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(0, 15.0, -3.0);
            case ROYAL -> new Vec3(0, 14.5, -3.0);
            case SEGMENTED -> new Vec3(0, 15.0, -1.0);
            case ARMORED, PUFFY -> new Vec3(0, 14.5, -2.0);
        };
    }

    /** Controle gauche: flanc gauche du corps, mi-hauteur, zone arriere. */
    private static Vec3 getControlLeftPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(-4.0, 16.0, 1.0);
            case ROYAL -> new Vec3(-4.5, 16.0, -3.0);
            case SEGMENTED -> new Vec3(-4.0, 16.0, 0.5);
            case ARMORED, PUFFY -> new Vec3(-4.5, 16.0, 1.0);
        };
    }

    /** Controle droit: flanc droit du corps, mi-hauteur, zone arriere. */
    private static Vec3 getControlRightPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(4.0, 16.0, 1.0);
            case ROYAL -> new Vec3(4.5, 16.0, -3.0);
            case SEGMENTED -> new Vec3(4.0, 16.0, 0.5);
            case ARMORED, PUFFY -> new Vec3(4.5, 16.0, 1.0);
        };
    }

    // ========== Wing rotation sync ==========

    /**
     * Applique la rotation des ailes du modele parent aux protections d'aile.
     * Les ModelParts "protector_right" et "protector_left" suivent le yRot (angle)
     * et le zRot (battement) de l'aile correspondante.
     */
    private void applyWingRotation(HoverbikePartModel part, ModelPart rightWing, ModelPart leftWing) {
        try {
            ModelPart protRight = part.root().getChild("protector_right");
            ModelPart protLeft = part.root().getChild("protector_left");
            protRight.yRot = rightWing.yRot;
            protRight.zRot = rightWing.zRot;
            protLeft.yRot = leftWing.yRot;
            protLeft.zRot = leftWing.zRot;
        } catch (Exception ignored) {
            // Variantes sans ces enfants : pas de synchro
        }
    }

    // ========== Edit mode animations ==========

    /**
     * Detecte les transitions d'edit mode et cree les animations d'ecartement/retour.
     * Etat stocke par entite pour eviter les interferences multi-entites.
     */
    private void handleEditModeTransition(PerEntityState state, boolean isEdit) {
        if (isEdit && !state.wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                state.controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(Vec3.ZERO).to(offset)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(false)
                        .build());
            }
            state.editExpanded = true;
        } else if (!isEdit && state.wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                state.controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(offset).to(Vec3.ZERO)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(true)
                        .build());
            }
            state.editExpanded = false;
        }
        state.wasEditMode = isEdit;
    }

    private void applyEditOffset(PoseStack poseStack, HoverbikePartModel part,
                                  String animName, PerEntityState state) {
        if (state.editExpanded && !state.controller.isAnimationPlaying(animName)) {
            Vec3 offset = part.getEditModeOffset();
            poseStack.translate(offset.x, offset.y, offset.z);
        } else {
            state.controller.applyAnimation(animName, poseStack);
        }
    }

    /**
     * Nettoie les etats des entites qui n'existent plus.
     * Appele periodiquement pour eviter les fuites memoire.
     */
    private void cleanupStaleEntities(HoverbikeEntity currentEntity) {
        if (currentEntity.tickCount % 200 != 0) return;
        entityStates.entrySet().removeIf(entry -> {
            if (entry.getKey() == currentEntity.getId()) return false;
            return currentEntity.level().getEntity(entry.getKey()) == null;
        });
    }

    // ========== Electrode lightning particles (Saddle B) ==========

    /**
     * Spawn des particules electriques entre les deux electrodes de la selle B.
     * Les particules forment un arc entre l'electrode gauche et droite.
     */
    private void spawnElectrodeParticles(HoverbikeEntity entity, BeeBodyType bodyType, float partialTick) {
        Level level = entity.level();
        if (!level.isClientSide()) return;

        // Throttle: spawn toutes les 2 ticks
        if (entity.tickCount % 2 != 0) return;

        // Position de base de la selle dans l'espace monde
        Vec3 saddlePos = getSaddlePosition(bodyType);

        // Positions des electrodes en coordonnees modele (1/16 bloc)
        Vec3 leftElectrode = SaddlePartModelB.LEFT_ELECTRODE;
        Vec3 rightElectrode = SaddlePartModelB.RIGHT_ELECTRODE;

        // Convertir en position monde
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(-yaw);
        float sin = Mth.sin(-yaw);

        // Position monde de l'entite
        double ex = entity.getX();
        double ey = entity.getY();
        double ez = entity.getZ();

        // Calculer positions monde des electrodes
        Vec3 leftWorld = transformToWorld(saddlePos, leftElectrode, ex, ey, ez, cos, sin);
        Vec3 rightWorld = transformToWorld(saddlePos, rightElectrode, ex, ey, ez, cos, sin);

        // Spawn 2-3 particules le long de l'arc
        int particleCount = 2 + level.random.nextInt(2);
        for (int i = 0; i < particleCount; i++) {
            double t = level.random.nextDouble();
            double px = Mth.lerp(t, leftWorld.x, rightWorld.x);
            double py = Mth.lerp(t, leftWorld.y, rightWorld.y) + (level.random.nextDouble() - 0.5) * 0.05;
            double pz = Mth.lerp(t, leftWorld.z, rightWorld.z);

            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0, 0);
        }
    }

    /**
     * Transforme une position locale (modele) en position monde.
     */
    private Vec3 transformToWorld(Vec3 partPos, Vec3 localOffset,
                                   double ex, double ey, double ez,
                                   float cos, float sin) {
        // Combiner position de la partie + offset local (en unites modele, 1/16 bloc)
        double lx = (partPos.x + localOffset.x) / 16.0;
        double ly = (partPos.y + localOffset.y) / 16.0;
        double lz = (partPos.z + localOffset.z) / 16.0;

        // Rotation autour de Y (yaw de l'entite)
        double rx = lx * cos - lz * sin;
        double rz = lx * sin + lz * cos;

        return new Vec3(ex + rx, ey + ly, ez + rz);
    }
}
