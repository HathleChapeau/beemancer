/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du HoverBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance             | Raison                | Utilisation                    |
 * |------------------------|----------------------|--------------------------------|
 * | HoverbikePartVariants  | Registre variantes   | Selection du modele a rendre   |
 * | HoverbikePartPositions | Positions par body   | Positionnement des parties     |
 * | HoverbikePartEffects   | Effets visuels       | Lightning, ring, connector     |
 * | HoverbikeEditModeHandler| Mode edition        | Animations ecartement/retour   |
 * | ApicaBeeModel          | Modele parent        | Body type + rotation ailes     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelB;
import com.chapeau.apica.client.renderer.entity.hoverbike.HoverbikeEditModeHandler;
import com.chapeau.apica.client.renderer.entity.hoverbike.HoverbikePartEffects;
import com.chapeau.apica.client.renderer.entity.hoverbike.HoverbikePartPositions;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
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
 * a l'origine dans son modele et positionnee selon le body type courant.
 * En edit mode, chaque partie s'ecarte du centre avec une animation smooth.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    /** Tous les modeles bakes, par partie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partVariants = new EnumMap<>(HoverbikePart.class);

    /** Etat par entite (entity ID → state). */
    private final Map<Integer, PerEntityState> entityStates = new HashMap<>();

    /** Etat specifique a une entite. */
    private static class PerEntityState {
        final HoverbikeEditModeHandler.EditModeState editState = new HoverbikeEditModeHandler.EditModeState();
        final HoverbikePartEffects.LightningState lightningState = new HoverbikePartEffects.LightningState();
    }

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> parent,
                              EntityRendererProvider.Context context) {
        super(parent);
        initPartVariants(context);
    }

    private void initPartVariants(EntityRendererProvider.Context context) {
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

        PerEntityState state = entityStates.computeIfAbsent(entity.getId(), k -> new PerEntityState());
        state.editState.tick(ageInTicks);

        boolean isEdit = entity.isEditMode();
        HoverbikeEditModeHandler.handleTransition(state.editState, isEdit, partVariants);

        ApicaBeeModel<HoverbikeEntity> beeModel = getParentModel();
        BeeBodyType bodyType = beeModel.getBodyType();

        for (HoverbikePart partType : HoverbikePart.values()) {
            renderPart(poseStack, bufferSource, packedLight, ageInTicks,
                    entity, state, partType, bodyType, beeModel);
        }

        cleanupStaleEntities(entity);
    }

    private void renderPart(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                            float ageInTicks, HoverbikeEntity entity, PerEntityState state,
                            HoverbikePart partType, BeeBodyType bodyType, ApicaBeeModel<HoverbikeEntity> beeModel) {

        List<HoverbikePartModel> models = partVariants.get(partType);
        if (models == null || models.isEmpty()) return;

        int variantIndex = entity.getPartVariant(partType);
        int clampedIndex = Math.floorMod(variantIndex, models.size());
        HoverbikePartModel part = models.get(clampedIndex);

        part.setupAnim(entity, 0, 0, ageInTicks, 0, 0);

        // Synchronise rotation des protections d'aile
        if (partType == HoverbikePart.WING_PROTECTOR) {
            applyWingRotation(part, beeModel.getRightWing(), beeModel.getLeftWing());
        }

        poseStack.pushPose();

        // Position selon body type
        Vec3 pos = HoverbikePartPositions.getPosition(partType, bodyType);
        poseStack.translate(pos.x / 16.0, pos.y / 16.0, pos.z / 16.0);

        // Flip horizontal pour controle droit
        boolean isRightControl = partType == HoverbikePart.CONTROL_RIGHT;
        if (isRightControl) {
            poseStack.scale(-1.0f, 1.0f, 1.0f);
        }

        // Animation edit mode
        HoverbikeEditModeHandler.applyOffset(poseStack, part, partType, state.editState, isRightControl);

        // Saddle B: cacher connector pour rendu principal
        SaddlePartModelB saddleB = null;
        if (partType == HoverbikePart.SADDLE && clampedIndex == 1 && part instanceof SaddlePartModelB sb) {
            saddleB = sb;
            saddleB.getConnector().visible = false;
        }

        // Rendu principal du modele
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(part.getTextureLocation()));
        part.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        // Effets speciaux par variante
        renderPartEffects(poseStack, bufferSource, packedLight, ageInTicks,
                partType, clampedIndex, saddleB, state);

        poseStack.popPose();
    }

    private void renderPartEffects(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                    float ageInTicks, HoverbikePart partType, int variantIndex,
                                    SaddlePartModelB saddleB, PerEntityState state) {

        // Saddle B: connector + lightning
        if (saddleB != null) {
            saddleB.getConnector().visible = true;
            HoverbikePartEffects.renderSaddleConnector(poseStack, bufferSource, packedLight, saddleB);
            HoverbikePartEffects.renderElectrodeLightning(poseStack, bufferSource, state.lightningState);
        }

        // Saddle C: ring
        if (partType == HoverbikePart.SADDLE && variantIndex == 2) {
            HoverbikePartEffects.renderSaddleRing(poseStack, bufferSource, packedLight, ageInTicks);
        }

        // Control C: ring
        if ((partType == HoverbikePart.CONTROL_LEFT || partType == HoverbikePart.CONTROL_RIGHT)
                && variantIndex == 2) {
            HoverbikePartEffects.renderControlRing(poseStack, bufferSource, packedLight, ageInTicks);
        }
    }

    /**
     * Applique la rotation des ailes du modele parent aux protections d'aile.
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
            // Variantes sans ces enfants: pas de synchro
        }
    }

    /**
     * Nettoie les etats des entites qui n'existent plus.
     */
    private void cleanupStaleEntities(HoverbikeEntity currentEntity) {
        if (currentEntity.tickCount % 200 != 0) return;
        entityStates.entrySet().removeIf(entry -> {
            if (entry.getKey() == currentEntity.getId()) return false;
            return currentEntity.level().getEntity(entry.getKey()) == null;
        });
    }
}
