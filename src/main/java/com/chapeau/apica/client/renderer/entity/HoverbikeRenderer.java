/**
 * ============================================================
 * [HoverbikeRenderer.java]
 * Description: Renderer pour l'entite Hoverbike (abeille geante montable)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite a rendre      | Source des donnees             |
 * | HoverbikeModel      | Modele 3D abeille    | Geometrie base                 |
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

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.HoverbikeModel;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer du Hoverbike sous forme d'abeille geante.
 * Utilise la texture vanilla de l'abeille, scaleee a 2.5x pour servir de monture.
 * Les parties modulaires (futures accessoires) sont rendues par HoverbikePartLayer.
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, HoverbikeModel> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike"), "main");

    /** Texture vanilla de l'abeille */
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/bee/bee.png");

    /** Facteur d'echelle pour transformer l'abeille en monture */
    private static final float BEE_SCALE = 2.5F;

    public HoverbikeRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverbikeModel(context.bakeLayer(LAYER_LOCATION)), 1.2f);
        this.addLayer(new HoverbikePartLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(HoverbikeEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(HoverbikeEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(BEE_SCALE, BEE_SCALE, BEE_SCALE);
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
