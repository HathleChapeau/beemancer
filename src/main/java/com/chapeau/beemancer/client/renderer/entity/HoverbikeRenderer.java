/**
 * ============================================================
 * [HoverbikeRenderer.java]
 * Description: Renderer pour l'entite Hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite a rendre      | Source des donnees             |
 * | HoverbikeModel      | Modele 3D            | Geometrie                      |
 * | Beemancer           | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.model.HoverbikeModel;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer simple pour le Hoverbike.
 * Utilise le modele test a deux cubes.
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, HoverbikeModel> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "hoverbike"), "main");

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/block/iron_block.png");

    public HoverbikeRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverbikeModel(context.bakeLayer(LAYER_LOCATION)), 0.8f);
    }

    @Override
    public ResourceLocation getTextureLocation(HoverbikeEntity entity) {
        return TEXTURE;
    }
}
