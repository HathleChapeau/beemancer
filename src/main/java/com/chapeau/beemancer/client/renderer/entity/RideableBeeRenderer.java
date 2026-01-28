/**
 * ============================================================
 * [RideableBeeRenderer.java]
 * Description: Renderer pour l'entité RideableBee
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RideableBeeEntity       | Entité à rendre      | Source des données             |
 * | RideableBeeModel        | Modèle 3D            | Géométrie                      |
 * | Beemancer               | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.model.RideableBeeModel;
import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer pour l'abeille chevauchable.
 * Utilise un modèle cube simple pour le moment.
 */
public class RideableBeeRenderer extends MobRenderer<RideableBeeEntity, RideableBeeModel> {

    // Layer location pour le modèle
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "rideable_bee"), "main");

    // Texture placeholder (jaune/noir comme une abeille)
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/entity/mount/rideable_bee.png");

    // Texture fallback si la custom n'existe pas
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/bee/bee.png");

    public RideableBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new RideableBeeModel(context.bakeLayer(LAYER_LOCATION)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(RideableBeeEntity entity) {
        // Utiliser texture vanilla bee pour le moment
        return FALLBACK_TEXTURE;
    }

    @Override
    protected void scale(RideableBeeEntity entity, PoseStack poseStack, float partialTick) {
        // Scale 1.5x pour une abeille de taille moyenne
        poseStack.scale(1.5f, 1.5f, 1.5f);
    }
}
