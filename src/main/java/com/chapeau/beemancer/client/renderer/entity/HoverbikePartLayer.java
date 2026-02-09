/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Modeles des parties  | Rendu geometrie                |
 * | ChassisPartModel    | Partie chassis       | Instanciation modele           |
 * | CoeurPartModel      | Partie coeur         | Instanciation modele           |
 * | PropulseurPartModel | Partie propulseur    | Instanciation modele           |
 * | RadiateurPartModel  | Partie radiateur     | Instanciation modele           |
 * | HoverbikeModel      | Modele parent        | Type generique du renderer     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.client.model.HoverbikeModel;
import com.chapeau.beemancer.client.model.hoverbike.ChassisPartModel;
import com.chapeau.beemancer.client.model.hoverbike.CoeurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.beemancer.client.model.hoverbike.PropulseurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.RadiateurPartModel;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;

/**
 * Layer de rendu qui itere sur toutes les parties du hoverbike et les rend
 * par-dessus le modele de base. Chaque partie a son propre modele et sa propre texture.
 * Le systeme est extensible : ajouter une partie = ajouter une ligne dans la liste.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, HoverbikeModel> {

    private final List<HoverbikePartModel> parts;

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, HoverbikeModel> parent,
                              EntityRendererProvider.Context context) {
        super(parent);

        this.parts = List.of(
                new ChassisPartModel(context.bakeLayer(ChassisPartModel.LAYER_LOCATION)),
                new CoeurPartModel(context.bakeLayer(CoeurPartModel.LAYER_LOCATION)),
                new PropulseurPartModel(context.bakeLayer(PropulseurPartModel.LAYER_LOCATION)),
                new RadiateurPartModel(context.bakeLayer(RadiateurPartModel.LAYER_LOCATION))
        );
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        for (HoverbikePartModel part : parts) {
            part.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
