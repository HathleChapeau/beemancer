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
import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer pour l'abeille chevauchable géante.
 * Utilise le modèle vanilla de Bee avec une texture custom.
 */
public class RideableBeeRenderer extends MobRenderer<RideableBeeEntity, BeeModel<RideableBeeEntity>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/entity/mount/rideable_bee.png");

    // Texture fallback vanilla si la custom n'existe pas
    private static final ResourceLocation VANILLA_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/bee/bee.png");

    public RideableBeeRenderer(EntityRendererProvider.Context context) {
        // Shadow radius 0.6f pour correspondre à la taille de l'abeille (scale 1.5x)
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(RideableBeeEntity entity) {
        // Pour l'instant, utiliser la texture vanilla jusqu'à ce qu'on crée une texture custom
        return VANILLA_TEXTURE;
    }

    @Override
    protected void scale(RideableBeeEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        // Abeille chevauchable: scale 1.5x
        poseStack.scale(1.5f, 1.5f, 1.5f);
    }
}
