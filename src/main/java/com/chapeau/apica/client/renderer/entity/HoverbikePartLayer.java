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
 * | SaddlePartModelB    | Positions electrodes | Lightning arcs saddle B        |
 * | SaddlePartModelC    | Position ring center | Ring effect saddle C           |
 * | ControlPartModelC   | Position ring center | Ring effect control C          |
 * | LightningArcRenderer| Arcs electriques     | Rendu lightning effects        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.animation.MoveAnimation;
import com.chapeau.apica.client.animation.TimingType;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelB;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelC;
import com.chapeau.apica.client.model.hoverbike.ControlPartModelC;
import com.chapeau.apica.client.renderer.LightningArcRenderer;
import com.chapeau.apica.Apica;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

        // Lightning arcs for saddle B
        LightningArcRenderer.LightningArc[] lightningArcs = new LightningArcRenderer.LightningArc[2];
        int lastArcTick = -1;
    }

    // Lightning arc constants (same as mining laser)
    private static final int ARC_REFRESH_TICKS = 4;
    private static final int ARC_NODES = 2;
    private static final float ARC_AMPLITUDE = 0.064f;
    private static final float ARC_HALF_WIDTH = 0.010f;

    // Ring constants for Saddle C
    private static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/ring.png");
    private static final int RING_FACE_COUNT = 12;
    private static final float RING_RADIUS = 0.12f;
    private static final float RING_HALF_DEPTH = 0.015f;

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

            // Flip horizontal pour le controle droit (meme modele que gauche, miroir)
            boolean isRightControl = partType == HoverbikePart.CONTROL_RIGHT;
            if (isRightControl) {
                poseStack.scale(-1.0f, 1.0f, 1.0f);
            }

            applyEditOffset(poseStack, part, animName, state, isRightControl);

            // Saddle B: hide connector during main render
            boolean isSaddleB = partType == HoverbikePart.SADDLE && clampedIndex == 1;
            SaddlePartModelB saddleB = null;
            if (isSaddleB && part instanceof SaddlePartModelB sb) {
                saddleB = sb;
                saddleB.getConnector().visible = false;
            }

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);

            // Saddle B: render connector with pink texture, then lightning
            if (saddleB != null) {
                saddleB.getConnector().visible = true;
                renderSaddleConnector(poseStack, bufferSource, packedLight, saddleB);
                renderElectrodeLightning(poseStack, bufferSource, state);
            }

            // Render ring effect for saddle variant C
            if (partType == HoverbikePart.SADDLE && clampedIndex == 2) {
                renderSaddleRing(poseStack, bufferSource, packedLight, ageInTicks);
            }

            // Render ring effect for control variant C
            if ((partType == HoverbikePart.CONTROL_LEFT || partType == HoverbikePart.CONTROL_RIGHT)
                    && clampedIndex == 2) {
                renderControlRing(poseStack, bufferSource, packedLight, ageInTicks);
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
            case SEGMENTED -> new Vec3(0, 14.5, 5.0);  // Reculee vers la tail
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
            case DEFAULT -> new Vec3(-4.0, 18.5, 2.5);
            case ROYAL -> new Vec3(-4.5, 18.5, -1.5);
            case SEGMENTED -> new Vec3(-4.0, 18.5, 7.5);  // Au milieu de la tail, face west
            case ARMORED, PUFFY -> new Vec3(-4.5, 18.5, 2.5);
        };
    }

    /** Controle droit: flanc droit du corps, mi-hauteur, zone arriere. */
    private static Vec3 getControlRightPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(4.0, 18.5, 2.5);
            case ROYAL -> new Vec3(4.5, 18.5, -1.5);
            case SEGMENTED -> new Vec3(4.0, 18.5, 7.5);  // Au milieu de la tail, face east
            case ARMORED, PUFFY -> new Vec3(4.5, 18.5, 2.5);
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
                                  String animName, PerEntityState state, boolean flipX) {
        if (state.editExpanded && !state.controller.isAnimationPlaying(animName)) {
            Vec3 offset = part.getEditModeOffset();
            // Inverse l'offset X pour le controle droit (deja flippe par scale)
            double ox = flipX ? -offset.x : offset.x;
            poseStack.translate(ox, offset.y, offset.z);
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

    // ========== Electrode lightning arcs (Saddle B) ==========

    /**
     * Rend des arcs electriques entre les deux electrodes de la selle B.
     * Utilise le meme systeme que le mining laser (LightningArcRenderer).
     */
    private void renderElectrodeLightning(PoseStack poseStack, MultiBufferSource bufferSource,
                                           PerEntityState state) {
        // Positions des electrodes en coordonnees modele (1/16 bloc -> blocs)
        Vec3 leftElectrode = SaddlePartModelB.LEFT_ELECTRODE.scale(1.0 / 16.0);
        Vec3 rightElectrode = SaddlePartModelB.RIGHT_ELECTRODE.scale(1.0 / 16.0);

        int currentTick = AnimationTimer.getTicks();
        if (state.lastArcTick < 0 || (currentTick - state.lastArcTick) >= ARC_REFRESH_TICKS) {
            RandomSource random = RandomSource.create(currentTick * 31L);
            for (int i = 0; i < 2; i++) {
                state.lightningArcs[i] = LightningArcRenderer.generateArc(
                        leftElectrode, rightElectrode, ARC_NODES, ARC_AMPLITUDE,
                        ARC_REFRESH_TICKS, false, false, random);
            }
            state.lastArcTick = currentTick;
        }

        // Couleur cyan electrique
        float r = 0.4f, g = 0.9f, b = 1.0f;

        for (LightningArcRenderer.LightningArc arc : state.lightningArcs) {
            if (arc != null) {
                LightningArcRenderer.renderArc(poseStack, bufferSource, arc,
                        ARC_HALF_WIDTH, r, g, b, 0.9f);
            }
        }
    }

    // ========== Connector render (Saddle B) ==========

    /**
     * Rend le connecteur entre les electrodes de la selle B avec texture placeholder rose.
     * Utilise RenderType.entityCutout pour un rendu sans transparence alpha.
     */
    private void renderSaddleConnector(PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, SaddlePartModelB saddleB) {
        ModelPart connector = saddleB.getConnector();
        VertexConsumer vc = bufferSource.getBuffer(
                RenderType.entityCutout(SaddlePartModelB.CONNECTOR_TEXTURE));

        connector.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
    }

    // ========== Ring effect (Saddle C) ==========

    /**
     * Rend un anneau rotatif autour de l'axe X pour la selle C.
     * Similaire au mining laser mais avec rotation sur X au lieu de Z.
     */
    private void renderSaddleRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, float ageInTicks) {
        Vec3 center = SaddlePartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * 0.15f;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.XP.rotation(rotation));

        float angleStep = (float) (2.0 * Math.PI / RING_FACE_COUNT);

        for (int i = 0; i < RING_FACE_COUNT; i++) {
            float angle0 = i * angleStep;
            float angle1 = (i + 1) * angleStep;
            float angleMid = (angle0 + angle1) * 0.5f;

            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            // Anneau dans le plan YZ (rotation autour de X)
            float y0 = cos0 * RING_RADIUS;
            float z0 = sin0 * RING_RADIUS;
            float y1 = cos1 * RING_RADIUS;
            float z1 = sin1 * RING_RADIUS;

            float ny = (float) Math.cos(angleMid);
            float nz = (float) Math.sin(angleMid);

            PoseStack.Pose pose = poseStack.last();
            // Face exterieure du cylindre
            vc.addVertex(pose, -RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, -RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
        }

        poseStack.popPose();
    }

    // ========== Control ring effect (Control C) ==========

    /**
     * Rend un anneau rotatif autour de l'axe X pour le controle C.
     */
    private void renderControlRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, float ageInTicks) {
        Vec3 center = ControlPartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * 0.14f;  // Reduit de 30% (etait 0.2)

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.XP.rotation(rotation));

        float angleStep = (float) (2.0 * Math.PI / RING_FACE_COUNT);
        float radius = RING_RADIUS * 0.8f;

        for (int i = 0; i < RING_FACE_COUNT; i++) {
            float angle0 = i * angleStep;
            float angle1 = (i + 1) * angleStep;
            float angleMid = (angle0 + angle1) * 0.5f;

            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            float y0 = cos0 * radius;
            float z0 = sin0 * radius;
            float y1 = cos1 * radius;
            float z1 = sin1 * radius;

            float ny = (float) Math.cos(angleMid);
            float nz = (float) Math.sin(angleMid);

            PoseStack.Pose pose = poseStack.last();
            vc.addVertex(pose, -RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, -RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
        }

        poseStack.popPose();
    }
}
