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
 * | HoverbikeModel      | Modele 3D            | Geometrie base                 |
 * | HoverbikePartLayer  | Layer parties         | Rendu parties modulaires       |
 * | Apica           | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.HoverbikeModel;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer du Hoverbike.
 * Rend le modele de base (2 cubes) puis delegue aux parties modulaires
 * via HoverbikePartLayer (chassis, coeur, propulseur, radiateur).
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, HoverbikeModel> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike"), "main");

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/block/iron_block.png");

    public HoverbikeRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverbikeModel(context.bakeLayer(LAYER_LOCATION)), 0.8f);
        this.addLayer(new HoverbikePartLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(HoverbikeEntity entity) {
        return TEXTURE;
    }

    /**
     * En edit mode, force le block light a 15 (fullbright).
     * L'entite est ainsi plus lumineuse que l'environnement assombri par le shader.
     */
    @Override
    protected int getBlockLightLevel(HoverbikeEntity entity, BlockPos pos) {
        return entity.isEditMode() ? 15 : super.getBlockLightLevel(entity, pos);
    }
}
