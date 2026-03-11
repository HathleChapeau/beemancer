/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du HoverBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                 | Raison                | Utilisation                    |
 * |----------------------------|----------------------|--------------------------------|
 * | HoverbikePartVariants      | Registre variantes   | Selection du modele a rendre   |
 * | HoverbikePartPositions     | Positions par body   | Positionnement des parties     |
 * | HoverbikeEditModeHandler   | Mode edition         | Animations ecartement/retour   |
 * | SaddlePartRenderer         | Rendu selle          | Effets selle (lightning, ring) |
 * | ControlPartRenderer        | Rendu controle       | Effets controle (ring, flip)   |
 * | WingProtectorPartRenderer  | Rendu protecteurs    | Sync rotation ailes            |
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
import com.chapeau.apica.client.renderer.entity.hoverbike.ControlPartRenderer;
import com.chapeau.apica.client.renderer.entity.hoverbike.HoverbikeEditModeHandler;
import com.chapeau.apica.client.renderer.entity.hoverbike.HoverbikePartPositions;
import com.chapeau.apica.client.renderer.entity.hoverbike.SaddlePartRenderer;
import com.chapeau.apica.client.renderer.entity.hoverbike.WingProtectorPartRenderer;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * la variante selectionnee de chaque partie. Delegue le rendu specifique
 * a chaque renderer de partie (SaddlePartRenderer, ControlPartRenderer, etc.)
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    /** Tous les modeles bakes, par partie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partVariants = new EnumMap<>(HoverbikePart.class);

    /** Etat par entite (entity ID → state). */
    private final Map<Integer, PerEntityState> entityStates = new HashMap<>();

    /** Etat specifique a une entite. */
    private static class PerEntityState {
        final HoverbikeEditModeHandler.EditModeState editState = new HoverbikeEditModeHandler.EditModeState();
        final SaddlePartRenderer.LightningState lightningState = new SaddlePartRenderer.LightningState();
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

        // Wing Protector: sync rotation avec ailes
        if (partType == HoverbikePart.WING_PROTECTOR) {
            WingProtectorPartRenderer.syncWingRotation(part, beeModel.getRightWing(), beeModel.getLeftWing());
        }

        poseStack.pushPose();

        // Position selon body type
        Vec3 pos = HoverbikePartPositions.getPosition(partType, bodyType);
        poseStack.translate(pos.x / 16.0, pos.y / 16.0, pos.z / 16.0);

        // Control: flip horizontal pour controle droit
        boolean isRightControl = ControlPartRenderer.isRightControl(partType);
        if (isRightControl) {
            ControlPartRenderer.applyFlipIfNeeded(poseStack, partType);
        }

        // Animation edit mode
        HoverbikeEditModeHandler.applyOffset(poseStack, part, partType, state.editState, isRightControl);

        // Saddle B: prepare render (cache connector)
        SaddlePartModelB saddleB = null;
        if (partType == HoverbikePart.SADDLE) {
            saddleB = SaddlePartRenderer.prepareRender(part, clampedIndex);
        }

        // Rendu principal du modele
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(part.getTextureLocation()));
        part.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        // Effets speciaux par type de partie
        switch (partType) {
            case SADDLE -> SaddlePartRenderer.renderEffects(
                    poseStack, bufferSource, packedLight, ageInTicks,
                    clampedIndex, saddleB, state.lightningState);

            case CONTROL_LEFT, CONTROL_RIGHT -> ControlPartRenderer.renderEffects(
                    poseStack, bufferSource, packedLight, ageInTicks, clampedIndex);

            default -> { /* Pas d'effets speciaux */ }
        }

        poseStack.popPose();
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
